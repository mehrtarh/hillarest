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
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import ir.hilla.rest.gson.HillaJsonArray;
import ir.hilla.rest.gson.HillaJsonElement;
import ir.hilla.rest.gson.HillaJsonNull;
import ir.hilla.rest.gson.HillaJsonObject;
import ir.hilla.rest.gson.HillaJsonPrimitive;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * This writer creates a HillaJsonElement.
 */
public final class HillaJsonTreeWriter extends HillaJsonWriter {
  private static final Writer UNWRITABLE_WRITER = new Writer() {
    @Override public void write(char[] buffer, int offset, int counter) {
      throw new AssertionError();
    }
    @Override public void flush() throws IOException {
      throw new AssertionError();
    }
    @Override public void close() throws IOException {
      throw new AssertionError();
    }
  };
  /** Added to the top of the stack when this writer is closed to cause following ops to fail. */
  private static final HillaJsonPrimitive SENTINEL_CLOSED = new HillaJsonPrimitive("closed");

  /** The JsonElements and JsonArrays under modification, outermost to innermost. */
  private final List<HillaJsonElement> stack = new ArrayList<HillaJsonElement>();

  /** The name for the next JSON object value. If non-null, the top of the stack is a HillaJsonObject. */
  private String pendingName;

  /** the JSON element constructed by this writer. */
  private HillaJsonElement product = HillaJsonNull.INSTANCE; // TODO: is this really what we want?;

  public HillaJsonTreeWriter() {
    super(UNWRITABLE_WRITER);
  }

  /**
   * Returns the top level object produced by this writer.
   */
  public HillaJsonElement get() {
    if (!stack.isEmpty()) {
      throw new IllegalStateException("Expected one JSON element but was " + stack);
    }
    return product;
  }

  private HillaJsonElement peek() {
    return stack.get(stack.size() - 1);
  }

  private void put(HillaJsonElement value) {
    if (pendingName != null) {
      if (!value.isJsonNull() || getSerializeNulls()) {
        HillaJsonObject object = (HillaJsonObject) peek();
        object.add(pendingName, value);
      }
      pendingName = null;
    } else if (stack.isEmpty()) {
      product = value;
    } else {
      HillaJsonElement element = peek();
      if (element instanceof HillaJsonArray) {
        ((HillaJsonArray) element).add(value);
      } else {
        throw new IllegalStateException();
      }
    }
  }

  @Override public HillaJsonWriter beginArray() throws IOException {
    HillaJsonArray array = new HillaJsonArray();
    put(array);
    stack.add(array);
    return this;
  }

  @Override public HillaJsonWriter endArray() throws IOException {
    if (stack.isEmpty() || pendingName != null) {
      throw new IllegalStateException();
    }
    HillaJsonElement element = peek();
    if (element instanceof HillaJsonArray) {
      stack.remove(stack.size() - 1);
      return this;
    }
    throw new IllegalStateException();
  }

  @Override public HillaJsonWriter beginObject() throws IOException {
    HillaJsonObject object = new HillaJsonObject();
    put(object);
    stack.add(object);
    return this;
  }

  @Override public HillaJsonWriter endObject() throws IOException {
    if (stack.isEmpty() || pendingName != null) {
      throw new IllegalStateException();
    }
    HillaJsonElement element = peek();
    if (element instanceof HillaJsonObject) {
      stack.remove(stack.size() - 1);
      return this;
    }
    throw new IllegalStateException();
  }

  @Override public HillaJsonWriter name(String name) throws IOException {
    if (stack.isEmpty() || pendingName != null) {
      throw new IllegalStateException();
    }
    HillaJsonElement element = peek();
    if (element instanceof HillaJsonObject) {
      pendingName = name;
      return this;
    }
    throw new IllegalStateException();
  }

  @Override public HillaJsonWriter value(String value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    put(new HillaJsonPrimitive(value));
    return this;
  }

  @Override public HillaJsonWriter nullValue() throws IOException {
    put(HillaJsonNull.INSTANCE);
    return this;
  }

  @Override public HillaJsonWriter value(boolean value) throws IOException {
    put(new HillaJsonPrimitive(value));
    return this;
  }

  @Override public HillaJsonWriter value(Boolean value) throws IOException {
    if (value == null) {
      return nullValue();
    }
    put(new HillaJsonPrimitive(value));
    return this;
  }

  @Override public HillaJsonWriter value(double value) throws IOException {
    if (!isLenient() && (Double.isNaN(value) || Double.isInfinite(value))) {
      throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
    }
    put(new HillaJsonPrimitive(value));
    return this;
  }

  @Override public HillaJsonWriter value(long value) throws IOException {
    put(new HillaJsonPrimitive(value));
    return this;
  }

  @Override public HillaJsonWriter value(Number value) throws IOException {
    if (value == null) {
      return nullValue();
    }

    if (!isLenient()) {
      double d = value.doubleValue();
      if (Double.isNaN(d) || Double.isInfinite(d)) {
        throw new IllegalArgumentException("JSON forbids NaN and infinities: " + value);
      }
    }

    put(new HillaJsonPrimitive(value));
    return this;
  }

  @Override public void flush() throws IOException {
  }

  @Override public void close() throws IOException {
    if (!stack.isEmpty()) {
      throw new IOException("Incomplete document");
    }
    stack.add(SENTINEL_CLOSED);
  }
}
