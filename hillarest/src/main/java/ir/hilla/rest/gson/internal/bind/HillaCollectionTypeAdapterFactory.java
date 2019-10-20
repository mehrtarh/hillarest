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
import java.lang.reflect.Type;
import java.util.Collection;

import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;
import ir.hilla.rest.gson.internal.$HillaGson$Types;
import ir.hilla.rest.gson.internal.HillaConstructorConstructor;
import ir.hilla.rest.gson.internal.HillaObjectConstructor;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * Adapt a homogeneous collection of objects.
 */
public final class HillaCollectionTypeAdapterFactory implements HillaTypeAdapterFactory {
  private final HillaConstructorConstructor constructorConstructor;

  public HillaCollectionTypeAdapterFactory(HillaConstructorConstructor constructorConstructor) {
    this.constructorConstructor = constructorConstructor;
  }

  @Override
  public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
    Type type = typeToken.getType();

    Class<? super T> rawType = typeToken.getRawType();
    if (!Collection.class.isAssignableFrom(rawType)) {
      return null;
    }

    Type elementType = $HillaGson$Types.getCollectionElementType(type, rawType);
    HillaTypeAdapter<?> elementTypeAdapter = gson.getAdapter(HillaTypeToken.get(elementType));
    HillaObjectConstructor<T> constructor = constructorConstructor.get(typeToken);

    @SuppressWarnings({"unchecked", "rawtypes"}) // create() doesn't define a type parameter
            HillaTypeAdapter<T> result = new Adapter(gson, elementType, elementTypeAdapter, constructor);
    return result;
  }

  private static final class Adapter<E> extends HillaTypeAdapter<Collection<E>> {
    private final HillaTypeAdapter<E> elementTypeAdapter;
    private final HillaObjectConstructor<? extends Collection<E>> constructor;

    public Adapter(HillaGson context, Type elementType,
                   HillaTypeAdapter<E> elementTypeAdapter,
                   HillaObjectConstructor<? extends Collection<E>> constructor) {
      this.elementTypeAdapter =
          new HillaTypeAdapterRuntimeTypeWrapper<E>(context, elementTypeAdapter, elementType);
      this.constructor = constructor;
    }

    @Override public Collection<E> read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }

      Collection<E> collection = constructor.construct();
      in.beginArray();
      while (in.hasNext()) {
        E instance = elementTypeAdapter.read(in);
        collection.add(instance);
      }
      in.endArray();
      return collection;
    }

    @Override public void write(HillaJsonWriter out, Collection<E> collection) throws IOException {
      if (collection == null) {
        out.nullValue();
        return;
      }

      out.beginArray();
      for (E element : collection) {
        elementTypeAdapter.write(out, element);
      }
      out.endArray();
    }
  }
}
