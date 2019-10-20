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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ir.hilla.rest.gson.HillaJsonElement;
import ir.hilla.rest.gson.HillaJsonPrimitive;
import ir.hilla.rest.gson.HillaJsonSyntaxException;
import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;
import ir.hilla.rest.gson.internal.$HillaGson$Types;
import ir.hilla.rest.gson.internal.HillaConstructorConstructor;
import ir.hilla.rest.gson.internal.HillaJsonReaderInternalAccess;
import ir.hilla.rest.gson.internal.HillaObjectConstructor;
import ir.hilla.rest.gson.internal.HillaStreams;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * Adapts maps to either JSON objects or JSON arrays.
 *
 * <h3>Maps as JSON objects</h3>
 * For primitive keys or when complex map key serialization is not enabled, this
 * converts Java {@link Map Maps} to JSON Objects. This requires that map keys
 * can be serialized as strings; this is insufficient for some key types. For
 * example, consider a map whose keys are points on a grid. The default JSON
 * form encodes reasonably: <pre>   {@code
 *   Map<Point, String> original = new LinkedHashMap<Point, String>();
 *   original.put(new Point(5, 6), "a");
 *   original.put(new Point(8, 8), "b");
 *   System.out.println(gson.toJson(original, type));
 * }</pre>
 * The above code prints this JSON object:<pre>   {@code
 *   {
 *     "(5,6)": "a",
 *     "(8,8)": "b"
 *   }
 * }</pre>
 * But GSON is unable to deserialize this value because the JSON string name is
 * just the {@link Object#toString() toString()} of the map key. Attempting to
 * convert the above JSON to an object fails with a parse exception:
 * <pre>ir.hilla.rest.converter.HillaJsonParseException: Expecting object found: "(5,6)"
 *   at ir.hilla.rest.converter.JsonObjectDeserializationVisitor.visitFieldUsingCustomHandler
 *   at ir.hilla.rest.converter.ObjectNavigator.navigateClassFields
 *   ...</pre>
 *
 * <h3>Maps as JSON arrays</h3>
 * An alternative approach taken by this type adapter when it is required and
 * complex map key serialization is enabled is to encode maps as arrays of map
 * entries. Each map entry is a two element array containing a key and a value.
 * This approach is more flexible because any type can be used as the map's key;
 * not just strings. But it's also less portable because the receiver of such
 * JSON must be aware of the map entry convention.
 *
 * <p>Register this adapter when you are creating your GSON instance.
 * <pre>   {@code
 *   HillaGson gson = new HillaGsonBuilder()
 *     .registerTypeAdapter(Map.class, new MapAsArrayTypeAdapter())
 *     .create();
 * }</pre>
 * This will change the structure of the JSON emitted by the code above. Now we
 * get an array. In this case the arrays elements are map entries:
 * <pre>   {@code
 *   [
 *     [
 *       {
 *         "x": 5,
 *         "y": 6
 *       },
 *       "a",
 *     ],
 *     [
 *       {
 *         "x": 8,
 *         "y": 8
 *       },
 *       "b"
 *     ]
 *   ]
 * }</pre>
 * This format will serialize and deserialize just fine as long as this adapter
 * is registered.
 */
public final class HillaMapTypeAdapterFactory implements HillaTypeAdapterFactory {
  private final HillaConstructorConstructor constructorConstructor;
  final boolean complexMapKeySerialization;

  public HillaMapTypeAdapterFactory(HillaConstructorConstructor constructorConstructor,
                                    boolean complexMapKeySerialization) {
    this.constructorConstructor = constructorConstructor;
    this.complexMapKeySerialization = complexMapKeySerialization;
  }

  @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
    Type type = typeToken.getType();

    Class<? super T> rawType = typeToken.getRawType();
    if (!Map.class.isAssignableFrom(rawType)) {
      return null;
    }

    Class<?> rawTypeOfSrc = $HillaGson$Types.getRawType(type);
    Type[] keyAndValueTypes = $HillaGson$Types.getMapKeyAndValueTypes(type, rawTypeOfSrc);
    HillaTypeAdapter<?> keyAdapter = getKeyAdapter(gson, keyAndValueTypes[0]);
    HillaTypeAdapter<?> valueAdapter = gson.getAdapter(HillaTypeToken.get(keyAndValueTypes[1]));
    HillaObjectConstructor<T> constructor = constructorConstructor.get(typeToken);

    @SuppressWarnings({"unchecked", "rawtypes"})
    // we don't define a type parameter for the key or value types
            HillaTypeAdapter<T> result = new Adapter(gson, keyAndValueTypes[0], keyAdapter,
        keyAndValueTypes[1], valueAdapter, constructor);
    return result;
  }

  /**
   * Returns a type adapter that writes the value as a string.
   */
  private HillaTypeAdapter<?> getKeyAdapter(HillaGson context, Type keyType) {
    return (keyType == boolean.class || keyType == Boolean.class)
        ? HillaTypeAdapters.BOOLEAN_AS_STRING
        : context.getAdapter(HillaTypeToken.get(keyType));
  }

  private final class Adapter<K, V> extends HillaTypeAdapter<Map<K, V>> {
    private final HillaTypeAdapter<K> keyTypeAdapter;
    private final HillaTypeAdapter<V> valueTypeAdapter;
    private final HillaObjectConstructor<? extends Map<K, V>> constructor;

    public Adapter(HillaGson context, Type keyType, HillaTypeAdapter<K> keyTypeAdapter,
                   Type valueType, HillaTypeAdapter<V> valueTypeAdapter,
                   HillaObjectConstructor<? extends Map<K, V>> constructor) {
      this.keyTypeAdapter =
        new HillaTypeAdapterRuntimeTypeWrapper<K>(context, keyTypeAdapter, keyType);
      this.valueTypeAdapter =
        new HillaTypeAdapterRuntimeTypeWrapper<V>(context, valueTypeAdapter, valueType);
      this.constructor = constructor;
    }

    @Override public Map<K, V> read(HillaJsonReader in) throws IOException {
      HillaJsonToken peek = in.peek();
      if (peek == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }

      Map<K, V> map = constructor.construct();

      if (peek == HillaJsonToken.BEGIN_ARRAY) {
        in.beginArray();
        while (in.hasNext()) {
          in.beginArray(); // entry array
          K key = keyTypeAdapter.read(in);
          V value = valueTypeAdapter.read(in);
          V replaced = map.put(key, value);
          if (replaced != null) {
            throw new HillaJsonSyntaxException("duplicate key: " + key);
          }
          in.endArray();
        }
        in.endArray();
      } else {
        in.beginObject();
        while (in.hasNext()) {
          HillaJsonReaderInternalAccess.INSTANCE.promoteNameToValue(in);
          K key = keyTypeAdapter.read(in);
          V value = valueTypeAdapter.read(in);
          V replaced = map.put(key, value);
          if (replaced != null) {
            throw new HillaJsonSyntaxException("duplicate key: " + key);
          }
        }
        in.endObject();
      }
      return map;
    }

    @Override public void write(HillaJsonWriter out, Map<K, V> map) throws IOException {
      if (map == null) {
        out.nullValue();
        return;
      }

      if (!complexMapKeySerialization) {
        out.beginObject();
        for (Map.Entry<K, V> entry : map.entrySet()) {
          out.name(String.valueOf(entry.getKey()));
          valueTypeAdapter.write(out, entry.getValue());
        }
        out.endObject();
        return;
      }

      boolean hasComplexKeys = false;
      List<HillaJsonElement> keys = new ArrayList<HillaJsonElement>(map.size());

      List<V> values = new ArrayList<V>(map.size());
      for (Map.Entry<K, V> entry : map.entrySet()) {
        HillaJsonElement keyElement = keyTypeAdapter.toJsonTree(entry.getKey());
        keys.add(keyElement);
        values.add(entry.getValue());
        hasComplexKeys |= keyElement.isJsonArray() || keyElement.isJsonObject();
      }

      if (hasComplexKeys) {
        out.beginArray();
        for (int i = 0, size = keys.size(); i < size; i++) {
          out.beginArray(); // entry array
          HillaStreams.write(keys.get(i), out);
          valueTypeAdapter.write(out, values.get(i));
          out.endArray();
        }
        out.endArray();
      } else {
        out.beginObject();
        for (int i = 0, size = keys.size(); i < size; i++) {
          HillaJsonElement keyElement = keys.get(i);
          out.name(keyToString(keyElement));
          valueTypeAdapter.write(out, values.get(i));
        }
        out.endObject();
      }
    }

    private String keyToString(HillaJsonElement keyElement) {
      if (keyElement.isJsonPrimitive()) {
        HillaJsonPrimitive primitive = keyElement.getAsJsonPrimitive();
        if (primitive.isNumber()) {
          return String.valueOf(primitive.getAsNumber());
        } else if (primitive.isBoolean()) {
          return Boolean.toString(primitive.getAsBoolean());
        } else if (primitive.isString()) {
          return primitive.getAsString();
        } else {
          throw new AssertionError();
        }
      } else if (keyElement.isJsonNull()) {
        return "null";
      } else {
        throw new AssertionError();
      }
    }
  }
}
