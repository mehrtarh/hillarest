/*
 * Copyright (C) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ir.hilla.rest.gson.internal.bind;

import ir.hilla.rest.gson.HillaJsonDeserializer;
import ir.hilla.rest.gson.HillaJsonSerializer;
import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;
import ir.hilla.rest.gson.annotations.HillaJsonAdapter;
import ir.hilla.rest.gson.internal.HillaConstructorConstructor;
import ir.hilla.rest.gson.reflect.HillaTypeToken;

/**
 * Given a type T, looks for the annotation {@link HillaJsonAdapter} and uses an instance of the
 * specified class as the default type adapter.
 *
 * @since 2.3
 */
public final class HillaJsonAdapterAnnotationTypeAdapterFactory implements HillaTypeAdapterFactory {
  private final HillaConstructorConstructor constructorConstructor;

  public HillaJsonAdapterAnnotationTypeAdapterFactory(HillaConstructorConstructor constructorConstructor) {
    this.constructorConstructor = constructorConstructor;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> targetType) {
    Class<? super T> rawType = targetType.getRawType();
    HillaJsonAdapter annotation = rawType.getAnnotation(HillaJsonAdapter.class);
    if (annotation == null) {
      return null;
    }
    return (HillaTypeAdapter<T>) getTypeAdapter(constructorConstructor, gson, targetType, annotation);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" }) // Casts guarded by conditionals.
  HillaTypeAdapter<?> getTypeAdapter(HillaConstructorConstructor constructorConstructor, HillaGson gson,
                                     HillaTypeToken<?> type, HillaJsonAdapter annotation) {
    Object instance = constructorConstructor.get(HillaTypeToken.get(annotation.value())).construct();

    HillaTypeAdapter<?> typeAdapter;
    if (instance instanceof HillaTypeAdapter) {
      typeAdapter = (HillaTypeAdapter<?>) instance;
    } else if (instance instanceof HillaTypeAdapterFactory) {
      typeAdapter = ((HillaTypeAdapterFactory) instance).create(gson, type);
    } else if (instance instanceof HillaJsonSerializer || instance instanceof HillaJsonDeserializer) {
      HillaJsonSerializer<?> serializer = instance instanceof HillaJsonSerializer
          ? (HillaJsonSerializer) instance
          : null;
      HillaJsonDeserializer<?> deserializer = instance instanceof HillaJsonDeserializer
          ? (HillaJsonDeserializer) instance
          : null;
      typeAdapter = new HillaTreeTypeAdapter(serializer, deserializer, gson, type, null);
    } else {
      throw new IllegalArgumentException("Invalid attempt to bind an instance of "
          + instance.getClass().getName() + " as a @HillaJsonAdapter for " + type.toString()
          + ". @HillaJsonAdapter value must be a HillaTypeAdapter, HillaTypeAdapterFactory,"
          + " HillaJsonSerializer or HillaJsonDeserializer.");
    }

    if (typeAdapter != null && annotation.nullSafe()) {
      typeAdapter = typeAdapter.nullSafe();
    }

    return typeAdapter;
  }
}
