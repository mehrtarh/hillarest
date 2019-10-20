/*
 * Copyright (C) 2008 Google Inc.
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

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ir.hilla.rest.gson.HillaExclusionStrategy;
import ir.hilla.rest.gson.HillaFieldAttributes;
import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;
import ir.hilla.rest.gson.annotations.HillaExpose;
import ir.hilla.rest.gson.annotations.HillaSince;
import ir.hilla.rest.gson.annotations.HillaUntil;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * This class selects which fields and types to omit. It is configurable,
 * supporting version attributes {@link HillaSince} and {@link HillaUntil}, modifiers,
 * synthetic fields, anonymous and local classes, inner classes, and fields with
 * the {@link HillaExpose} annotation.
 *
 * <p>This class is a type adapter factory; types that are excluded will be
 * adapted to null. It may delegate to another type adapter if only one
 * direction is excluded.
 *
 * @author Joel Leitch
 * @author Jesse Wilson
 */
public final class HillaExcluder implements HillaTypeAdapterFactory, Cloneable {
  private static final double IGNORE_VERSIONS = -1.0d;
  public static final HillaExcluder DEFAULT = new HillaExcluder();

  private double version = IGNORE_VERSIONS;
  private int modifiers = Modifier.TRANSIENT | Modifier.STATIC;
  private boolean serializeInnerClasses = true;
  private boolean requireExpose;
  private List<HillaExclusionStrategy> serializationStrategies = Collections.emptyList();
  private List<HillaExclusionStrategy> deserializationStrategies = Collections.emptyList();

  @Override protected HillaExcluder clone() {
    try {
      return (HillaExcluder) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }

  public HillaExcluder withVersion(double ignoreVersionsAfter) {
    HillaExcluder result = clone();
    result.version = ignoreVersionsAfter;
    return result;
  }

  public HillaExcluder withModifiers(int... modifiers) {
    HillaExcluder result = clone();
    result.modifiers = 0;
    for (int modifier : modifiers) {
      result.modifiers |= modifier;
    }
    return result;
  }

  public HillaExcluder disableInnerClassSerialization() {
    HillaExcluder result = clone();
    result.serializeInnerClasses = false;
    return result;
  }

  public HillaExcluder excludeFieldsWithoutExposeAnnotation() {
    HillaExcluder result = clone();
    result.requireExpose = true;
    return result;
  }

  public HillaExcluder withExclusionStrategy(HillaExclusionStrategy exclusionStrategy,
                                             boolean serialization, boolean deserialization) {
    HillaExcluder result = clone();
    if (serialization) {
      result.serializationStrategies = new ArrayList<HillaExclusionStrategy>(serializationStrategies);
      result.serializationStrategies.add(exclusionStrategy);
    }
    if (deserialization) {
      result.deserializationStrategies
          = new ArrayList<HillaExclusionStrategy>(deserializationStrategies);
      result.deserializationStrategies.add(exclusionStrategy);
    }
    return result;
  }

  public <T> HillaTypeAdapter<T> create(final HillaGson gson, final HillaTypeToken<T> type) {
    Class<?> rawType = type.getRawType();
    boolean excludeClass = excludeClassChecks(rawType);

    final boolean skipSerialize = excludeClass || excludeClassInStrategy(rawType, true);
    final boolean skipDeserialize = excludeClass ||  excludeClassInStrategy(rawType, false);

    if (!skipSerialize && !skipDeserialize) {
      return null;
    }

    return new HillaTypeAdapter<T>() {
      /** The delegate is lazily created because it may not be needed, and creating it may fail. */
      private HillaTypeAdapter<T> delegate;

      @Override public T read(HillaJsonReader in) throws IOException {
        if (skipDeserialize) {
          in.skipValue();
          return null;
        }
        return delegate().read(in);
      }

      @Override public void write(HillaJsonWriter out, T value) throws IOException {
        if (skipSerialize) {
          out.nullValue();
          return;
        }
        delegate().write(out, value);
      }

      private HillaTypeAdapter<T> delegate() {
        HillaTypeAdapter<T> d = delegate;
        return d != null
            ? d
            : (delegate = gson.getDelegateAdapter(HillaExcluder.this, type));
      }
    };
  }

  public boolean excludeField(Field field, boolean serialize) {
    if ((modifiers & field.getModifiers()) != 0) {
      return true;
    }

    if (version != HillaExcluder.IGNORE_VERSIONS
        && !isValidVersion(field.getAnnotation(HillaSince.class), field.getAnnotation(HillaUntil.class))) {
      return true;
    }

    if (field.isSynthetic()) {
      return true;
    }

    if (requireExpose) {
      HillaExpose annotation = field.getAnnotation(HillaExpose.class);
      if (annotation == null || (serialize ? !annotation.serialize() : !annotation.deserialize())) {
        return true;
      }
    }

    if (!serializeInnerClasses && isInnerClass(field.getType())) {
      return true;
    }

    if (isAnonymousOrLocal(field.getType())) {
      return true;
    }

    List<HillaExclusionStrategy> list = serialize ? serializationStrategies : deserializationStrategies;
    if (!list.isEmpty()) {
      HillaFieldAttributes fieldAttributes = new HillaFieldAttributes(field);
      for (HillaExclusionStrategy exclusionStrategy : list) {
        if (exclusionStrategy.shouldSkipField(fieldAttributes)) {
          return true;
        }
      }
    }

    return false;
  }

  private boolean excludeClassChecks(Class<?> clazz) {
      if (version != HillaExcluder.IGNORE_VERSIONS && !isValidVersion(clazz.getAnnotation(HillaSince.class), clazz.getAnnotation(HillaUntil.class))) {
          return true;
      }

      if (!serializeInnerClasses && isInnerClass(clazz)) {
          return true;
      }

      if (isAnonymousOrLocal(clazz)) {
          return true;
      }

      return false;
  }

  public boolean excludeClass(Class<?> clazz, boolean serialize) {
      return excludeClassChecks(clazz) ||
              excludeClassInStrategy(clazz, serialize);
  }

  private boolean excludeClassInStrategy(Class<?> clazz, boolean serialize) {
      List<HillaExclusionStrategy> list = serialize ? serializationStrategies : deserializationStrategies;
      for (HillaExclusionStrategy exclusionStrategy : list) {
          if (exclusionStrategy.shouldSkipClass(clazz)) {
              return true;
          }
      }
      return false;
  }

  private boolean isAnonymousOrLocal(Class<?> clazz) {
    return !Enum.class.isAssignableFrom(clazz)
        && (clazz.isAnonymousClass() || clazz.isLocalClass());
  }

  private boolean isInnerClass(Class<?> clazz) {
    return clazz.isMemberClass() && !isStatic(clazz);
  }

  private boolean isStatic(Class<?> clazz) {
    return (clazz.getModifiers() & Modifier.STATIC) != 0;
  }

  private boolean isValidVersion(HillaSince since, HillaUntil until) {
    return isValidSince(since) && isValidUntil(until);
  }

  private boolean isValidSince(HillaSince annotation) {
    if (annotation != null) {
      double annotationVersion = annotation.value();
      if (annotationVersion > version) {
        return false;
      }
    }
    return true;
  }

  private boolean isValidUntil(HillaUntil annotation) {
    if (annotation != null) {
      double annotationVersion = annotation.value();
      if (annotationVersion <= version) {
        return false;
      }
    }
    return true;
  }
}
