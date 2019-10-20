/*
 * Copyright (C) 2011 Google Inc.
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

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;
import ir.hilla.rest.gson.internal.$HillaGson$Types;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * Adapt an array of objects.
 */
public final class HillaArrayTypeAdapter<E> extends HillaTypeAdapter<Object> {
  public static final HillaTypeAdapterFactory FACTORY = new HillaTypeAdapterFactory() {
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
      Type type = typeToken.getType();
      if (!(type instanceof GenericArrayType || type instanceof Class && ((Class<?>) type).isArray())) {
        return null;
      }

      Type componentType = $HillaGson$Types.getArrayComponentType(type);
      HillaTypeAdapter<?> componentTypeAdapter = gson.getAdapter(HillaTypeToken.get(componentType));
      return new HillaArrayTypeAdapter(
              gson, componentTypeAdapter, $HillaGson$Types.getRawType(componentType));
    }
  };

  private final Class<E> componentType;
  private final HillaTypeAdapter<E> componentTypeAdapter;

  public HillaArrayTypeAdapter(HillaGson context, HillaTypeAdapter<E> componentTypeAdapter, Class<E> componentType) {
    this.componentTypeAdapter =
      new HillaTypeAdapterRuntimeTypeWrapper<E>(context, componentTypeAdapter, componentType);
    this.componentType = componentType;
  }

  @Override public Object read(HillaJsonReader in) throws IOException {
    if (in.peek() == HillaJsonToken.NULL) {
      in.nextNull();
      return null;
    }

    List<E> list = new ArrayList<E>();
    in.beginArray();
    while (in.hasNext()) {
      E instance = componentTypeAdapter.read(in);
      list.add(instance);
    }
    in.endArray();

    int size = list.size();
    Object array = Array.newInstance(componentType, size);
    for (int i = 0; i < size; i++) {
      Array.set(array, i, list.get(i));
    }
    return array;
  }

  @SuppressWarnings("unchecked")
  @Override public void write(HillaJsonWriter out, Object array) throws IOException {
    if (array == null) {
      out.nullValue();
      return;
    }

    out.beginArray();
    for (int i = 0, length = Array.getLength(array); i < length; i++) {
      E value = (E) Array.get(array, i);
      componentTypeAdapter.write(out, value);
    }
    out.endArray();
  }
}
