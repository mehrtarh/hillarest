/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ir.hilla.rest.gson.internal;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import ir.hilla.rest.gson.HillaInstanceCreator;
import ir.hilla.rest.gson.HillaJsonIOException;
import ir.hilla.rest.gson.internal.reflect.HillaReflectionAccessor;
import ir.hilla.rest.gson.reflect.HillaTypeToken;

/**
 * Returns a function that can construct an instance of a requested type.
 */
public final class HillaConstructorConstructor {
  private final Map<Type, HillaInstanceCreator<?>> instanceCreators;
  private final HillaReflectionAccessor accessor = HillaReflectionAccessor.getInstance();

  public HillaConstructorConstructor(Map<Type, HillaInstanceCreator<?>> instanceCreators) {
    this.instanceCreators = instanceCreators;
  }

  public <T> HillaObjectConstructor<T> get(HillaTypeToken<T> typeToken) {
    final Type type = typeToken.getType();
    final Class<? super T> rawType = typeToken.getRawType();

    // first try an instance creator

    @SuppressWarnings("unchecked") // types must agree
    final HillaInstanceCreator<T> typeCreator = (HillaInstanceCreator<T>) instanceCreators.get(type);
    if (typeCreator != null) {
      return new HillaObjectConstructor<T>() {
        @Override public T construct() {
          return typeCreator.createInstance(type);
        }
      };
    }

    // Next try raw type match for instance creators
    @SuppressWarnings("unchecked") // types must agree
    final HillaInstanceCreator<T> rawTypeCreator =
        (HillaInstanceCreator<T>) instanceCreators.get(rawType);
    if (rawTypeCreator != null) {
      return new HillaObjectConstructor<T>() {
        @Override public T construct() {
          return rawTypeCreator.createInstance(type);
        }
      };
    }

    HillaObjectConstructor<T> defaultConstructor = newDefaultConstructor(rawType);
    if (defaultConstructor != null) {
      return defaultConstructor;
    }

    HillaObjectConstructor<T> defaultImplementation = newDefaultImplementationConstructor(type, rawType);
    if (defaultImplementation != null) {
      return defaultImplementation;
    }

    // finally try unsafe
    return newUnsafeAllocator(type, rawType);
  }

  private <T> HillaObjectConstructor<T> newDefaultConstructor(Class<? super T> rawType) {
    try {
      final Constructor<? super T> constructor = rawType.getDeclaredConstructor();
      if (!constructor.isAccessible()) {
        accessor.makeAccessible(constructor);
      }
      return new HillaObjectConstructor<T>() {
        @SuppressWarnings("unchecked") // T is the same raw type as is requested
        @Override public T construct() {
          try {
            Object[] args = null;
            return (T) constructor.newInstance(args);
          } catch (InstantiationException e) {
            // TODO: HillaJsonParseException ?
            throw new RuntimeException("Failed to invoke " + constructor + " with no args", e);
          } catch (InvocationTargetException e) {
            // TODO: don't wrap if cause is unchecked!
            // TODO: HillaJsonParseException ?
            throw new RuntimeException("Failed to invoke " + constructor + " with no args",
                e.getTargetException());
          } catch (IllegalAccessException e) {
            throw new AssertionError(e);
          }
        }
      };
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  /**
   * Constructors for common interface types like Map and List and their
   * subtypes.
   */
  @SuppressWarnings("unchecked") // use runtime checks to guarantee that 'T' is what it is
  private <T> HillaObjectConstructor<T> newDefaultImplementationConstructor(
      final Type type, Class<? super T> rawType) {
    if (Collection.class.isAssignableFrom(rawType)) {
      if (SortedSet.class.isAssignableFrom(rawType)) {
        return new HillaObjectConstructor<T>() {
          @Override public T construct() {
            return (T) new TreeSet<Object>();
          }
        };
      } else if (EnumSet.class.isAssignableFrom(rawType)) {
        return new HillaObjectConstructor<T>() {
          @SuppressWarnings("rawtypes")
          @Override public T construct() {
            if (type instanceof ParameterizedType) {
              Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
              if (elementType instanceof Class) {
                return (T) EnumSet.noneOf((Class)elementType);
              } else {
                throw new HillaJsonIOException("Invalid EnumSet type: " + type.toString());
              }
            } else {
              throw new HillaJsonIOException("Invalid EnumSet type: " + type.toString());
            }
          }
        };
      } else if (Set.class.isAssignableFrom(rawType)) {
        return new HillaObjectConstructor<T>() {
          @Override public T construct() {
            return (T) new LinkedHashSet<Object>();
          }
        };
      } else if (Queue.class.isAssignableFrom(rawType)) {
        return new HillaObjectConstructor<T>() {
          @Override public T construct() {
            return (T) new ArrayDeque<Object>();
          }
        };
      } else {
        return new HillaObjectConstructor<T>() {
          @Override public T construct() {
            return (T) new ArrayList<Object>();
          }
        };
      }
    }

    if (Map.class.isAssignableFrom(rawType)) {
      if (ConcurrentNavigableMap.class.isAssignableFrom(rawType)) {
        return new HillaObjectConstructor<T>() {
          @Override public T construct() {
            return (T) new ConcurrentSkipListMap<Object, Object>();
          }
        };
      } else if (ConcurrentMap.class.isAssignableFrom(rawType)) {
        return new HillaObjectConstructor<T>() {
          @Override public T construct() {
            return (T) new ConcurrentHashMap<Object, Object>();
          }
        };
      } else if (SortedMap.class.isAssignableFrom(rawType)) {
        return new HillaObjectConstructor<T>() {
          @Override public T construct() {
            return (T) new TreeMap<Object, Object>();
          }
        };
      } else if (type instanceof ParameterizedType && !(String.class.isAssignableFrom(
          HillaTypeToken.get(((ParameterizedType) type).getActualTypeArguments()[0]).getRawType()))) {
        return new HillaObjectConstructor<T>() {
          @Override public T construct() {
            return (T) new LinkedHashMap<Object, Object>();
          }
        };
      } else {
        return new HillaObjectConstructor<T>() {
          @Override public T construct() {
            return (T) new HillaLinkedTreeMap<String, Object>();
          }
        };
      }
    }

    return null;
  }

  private <T> HillaObjectConstructor<T> newUnsafeAllocator(
      final Type type, final Class<? super T> rawType) {
    return new HillaObjectConstructor<T>() {
      private final HillaUnsafeAllocator unsafeAllocator = HillaUnsafeAllocator.create();
      @SuppressWarnings("unchecked")
      @Override public T construct() {
        try {
          Object newInstance = unsafeAllocator.newInstance(rawType);
          return (T) newInstance;
        } catch (Exception e) {
          throw new RuntimeException(("Unable to invoke no-args constructor for " + type + ". "
              + "Registering an HillaInstanceCreator with HillaGson for this type may fix this problem."), e);
        }
      }
    };
  }

  @Override public String toString() {
    return instanceCreators.toString();
  }
}
