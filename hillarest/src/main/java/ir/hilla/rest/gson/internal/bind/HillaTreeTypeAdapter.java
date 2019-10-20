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

package ir.hilla.rest.gson.internal.bind;

import java.io.IOException;
import java.lang.reflect.Type;

import ir.hilla.rest.gson.HillaJsonDeserializationContext;
import ir.hilla.rest.gson.HillaJsonDeserializer;
import ir.hilla.rest.gson.HillaJsonElement;
import ir.hilla.rest.gson.HillaJsonParseException;
import ir.hilla.rest.gson.HillaJsonSerializationContext;
import ir.hilla.rest.gson.HillaJsonSerializer;
import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;
import ir.hilla.rest.gson.internal.$HillaGson$Preconditions;
import ir.hilla.rest.gson.internal.HillaStreams;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * Adapts a HillaGson 1.x tree-style adapter as a streaming HillaTypeAdapter. HillaSince the
 * tree adapter may be serialization-only or deserialization-only, this class
 * has a facility to lookup a delegate type adapter on demand.
 */
public final class HillaTreeTypeAdapter<T> extends HillaTypeAdapter<T> {
  private final HillaJsonSerializer<T> serializer;
  private final HillaJsonDeserializer<T> deserializer;
  final HillaGson gson;
  private final HillaTypeToken<T> typeToken;
  private final HillaTypeAdapterFactory skipPast;
  private final GsonContextImpl context = new GsonContextImpl();

  /** The delegate is lazily created because it may not be needed, and creating it may fail. */
  private HillaTypeAdapter<T> delegate;

  public HillaTreeTypeAdapter(HillaJsonSerializer<T> serializer, HillaJsonDeserializer<T> deserializer,
                              HillaGson gson, HillaTypeToken<T> typeToken, HillaTypeAdapterFactory skipPast) {
    this.serializer = serializer;
    this.deserializer = deserializer;
    this.gson = gson;
    this.typeToken = typeToken;
    this.skipPast = skipPast;
  }

  @Override public T read(HillaJsonReader in) throws IOException {
    if (deserializer == null) {
      return delegate().read(in);
    }
    HillaJsonElement value = HillaStreams.parse(in);
    if (value.isJsonNull()) {
      return null;
    }
    return deserializer.deserialize(value, typeToken.getType(), context);
  }

  @Override public void write(HillaJsonWriter out, T value) throws IOException {
    if (serializer == null) {
      delegate().write(out, value);
      return;
    }
    if (value == null) {
      out.nullValue();
      return;
    }
    HillaJsonElement tree = serializer.serialize(value, typeToken.getType(), context);
    HillaStreams.write(tree, out);
  }

  private HillaTypeAdapter<T> delegate() {
    HillaTypeAdapter<T> d = delegate;
    return d != null
        ? d
        : (delegate = gson.getDelegateAdapter(skipPast, typeToken));
  }

  /**
   * Returns a new factory that will match each type against {@code exactType}.
   */
  public static HillaTypeAdapterFactory newFactory(HillaTypeToken<?> exactType, Object typeAdapter) {
    return new SingleTypeFactory(typeAdapter, exactType, false, null);
  }

  /**
   * Returns a new factory that will match each type and its raw type against
   * {@code exactType}.
   */
  public static HillaTypeAdapterFactory newFactoryWithMatchRawType(
          HillaTypeToken<?> exactType, Object typeAdapter) {
    // only bother matching raw types if exact type is a raw type
    boolean matchRawType = exactType.getType() == exactType.getRawType();
    return new SingleTypeFactory(typeAdapter, exactType, matchRawType, null);
  }

  /**
   * Returns a new factory that will match each type's raw type for assignability
   * to {@code hierarchyType}.
   */
  public static HillaTypeAdapterFactory newTypeHierarchyFactory(
      Class<?> hierarchyType, Object typeAdapter) {
    return new SingleTypeFactory(typeAdapter, null, false, hierarchyType);
  }

  private static final class SingleTypeFactory implements HillaTypeAdapterFactory {
    private final HillaTypeToken<?> exactType;
    private final boolean matchRawType;
    private final Class<?> hierarchyType;
    private final HillaJsonSerializer<?> serializer;
    private final HillaJsonDeserializer<?> deserializer;

    SingleTypeFactory(Object typeAdapter, HillaTypeToken<?> exactType, boolean matchRawType,
                      Class<?> hierarchyType) {
      serializer = typeAdapter instanceof HillaJsonSerializer
          ? (HillaJsonSerializer<?>) typeAdapter
          : null;
      deserializer = typeAdapter instanceof HillaJsonDeserializer
          ? (HillaJsonDeserializer<?>) typeAdapter
          : null;
      $HillaGson$Preconditions.checkArgument(serializer != null || deserializer != null);
      this.exactType = exactType;
      this.matchRawType = matchRawType;
      this.hierarchyType = hierarchyType;
    }

    @SuppressWarnings("unchecked") // guarded by typeToken.equals() call
    @Override
    public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> type) {
      boolean matches = exactType != null
          ? exactType.equals(type) || matchRawType && exactType.getType() == type.getRawType()
          : hierarchyType.isAssignableFrom(type.getRawType());
      return matches
          ? new HillaTreeTypeAdapter<T>((HillaJsonSerializer<T>) serializer,
              (HillaJsonDeserializer<T>) deserializer, gson, type, this)
          : null;
    }
  }

  private final class GsonContextImpl implements HillaJsonSerializationContext, HillaJsonDeserializationContext {
    @Override public HillaJsonElement serialize(Object src) {
      return gson.toJsonTree(src);
    }
    @Override public HillaJsonElement serialize(Object src, Type typeOfSrc) {
      return gson.toJsonTree(src, typeOfSrc);
    }
    @SuppressWarnings("unchecked")
    @Override public <R> R deserialize(HillaJsonElement json, Type typeOfT) throws HillaJsonParseException {
      return (R) gson.fromJson(json, typeOfT);
    }
  };
}
