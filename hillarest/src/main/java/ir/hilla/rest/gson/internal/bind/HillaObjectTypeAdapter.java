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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;
import ir.hilla.rest.gson.internal.HillaLinkedTreeMap;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * Adapts types whose static type is only 'Object'. Uses getClass() on
 * serialization and a primitive/Map/List on deserialization.
 */
public final class HillaObjectTypeAdapter extends HillaTypeAdapter<Object> {
  public static final HillaTypeAdapterFactory FACTORY = new HillaTypeAdapterFactory() {
    @SuppressWarnings("unchecked")
    @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> type) {
      if (type.getRawType() == Object.class) {
        return (HillaTypeAdapter<T>) new HillaObjectTypeAdapter(gson);
      }
      return null;
    }
  };

  private final HillaGson gson;

  HillaObjectTypeAdapter(HillaGson gson) {
    this.gson = gson;
  }

  @Override public Object read(HillaJsonReader in) throws IOException {
    HillaJsonToken token = in.peek();
    switch (token) {
    case BEGIN_ARRAY:
      List<Object> list = new ArrayList<Object>();
      in.beginArray();
      while (in.hasNext()) {
        list.add(read(in));
      }
      in.endArray();
      return list;

    case BEGIN_OBJECT:
      Map<String, Object> map = new HillaLinkedTreeMap<String, Object>();
      in.beginObject();
      while (in.hasNext()) {
        map.put(in.nextName(), read(in));
      }
      in.endObject();
      return map;

    case STRING:
      return in.nextString();

    case NUMBER:
      return in.nextDouble();

    case BOOLEAN:
      return in.nextBoolean();

    case NULL:
      in.nextNull();
      return null;

    default:
      throw new IllegalStateException();
    }
  }

  @SuppressWarnings("unchecked")
  @Override public void write(HillaJsonWriter out, Object value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }

    HillaTypeAdapter<Object> typeAdapter = (HillaTypeAdapter<Object>) gson.getAdapter(value.getClass());
    if (typeAdapter instanceof HillaObjectTypeAdapter) {
      out.beginObject();
      out.endObject();
      return;
    }

    typeAdapter.write(out, value);
  }
}
