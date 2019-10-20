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
import java.io.Reader;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import ir.hilla.rest.gson.HillaJsonArray;
import ir.hilla.rest.gson.HillaJsonElement;
import ir.hilla.rest.gson.HillaJsonNull;
import ir.hilla.rest.gson.HillaJsonObject;
import ir.hilla.rest.gson.HillaJsonPrimitive;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;

/**
 * This reader walks the elements of a HillaJsonElement as if it was coming from a
 * character stream.
 *
 * @author Jesse Wilson
 */
public final class HillaJsonTreeReader extends HillaJsonReader {
  private static final Reader UNREADABLE_READER = new Reader() {
    @Override public int read(char[] buffer, int offset, int count) throws IOException {
      throw new AssertionError();
    }
    @Override public void close() throws IOException {
      throw new AssertionError();
    }
  };
  private static final Object SENTINEL_CLOSED = new Object();

  /*
   * The nesting stack. Using a manual array rather than an ArrayList saves 20%.
   */
  private Object[] stack = new Object[32];
  private int stackSize = 0;

  /*
   * The path members. It corresponds directly to stack: At indices where the
   * stack contains an object (EMPTY_OBJECT, DANGLING_NAME or NONEMPTY_OBJECT),
   * pathNames contains the name at this scope. Where it contains an array
   * (EMPTY_ARRAY, NONEMPTY_ARRAY) pathIndices contains the current index in
   * that array. Otherwise the value is undefined, and we take advantage of that
   * by incrementing pathIndices when doing so isn't useful.
   */
  private String[] pathNames = new String[32];
  private int[] pathIndices = new int[32];

  public HillaJsonTreeReader(HillaJsonElement element) {
    super(UNREADABLE_READER);
    push(element);
  }

  @Override public void beginArray() throws IOException {
    expect(HillaJsonToken.BEGIN_ARRAY);
    HillaJsonArray array = (HillaJsonArray) peekStack();
    push(array.iterator());
    pathIndices[stackSize - 1] = 0;
  }

  @Override public void endArray() throws IOException {
    expect(HillaJsonToken.END_ARRAY);
    popStack(); // empty iterator
    popStack(); // array
    if (stackSize > 0) {
      pathIndices[stackSize - 1]++;
    }
  }

  @Override public void beginObject() throws IOException {
    expect(HillaJsonToken.BEGIN_OBJECT);
    HillaJsonObject object = (HillaJsonObject) peekStack();
    push(object.entrySet().iterator());
  }

  @Override public void endObject() throws IOException {
    expect(HillaJsonToken.END_OBJECT);
    popStack(); // empty iterator
    popStack(); // object
    if (stackSize > 0) {
      pathIndices[stackSize - 1]++;
    }
  }

  @Override public boolean hasNext() throws IOException {
    HillaJsonToken token = peek();
    return token != HillaJsonToken.END_OBJECT && token != HillaJsonToken.END_ARRAY;
  }

  @Override public HillaJsonToken peek() throws IOException {
    if (stackSize == 0) {
      return HillaJsonToken.END_DOCUMENT;
    }

    Object o = peekStack();
    if (o instanceof Iterator) {
      boolean isObject = stack[stackSize - 2] instanceof HillaJsonObject;
      Iterator<?> iterator = (Iterator<?>) o;
      if (iterator.hasNext()) {
        if (isObject) {
          return HillaJsonToken.NAME;
        } else {
          push(iterator.next());
          return peek();
        }
      } else {
        return isObject ? HillaJsonToken.END_OBJECT : HillaJsonToken.END_ARRAY;
      }
    } else if (o instanceof HillaJsonObject) {
      return HillaJsonToken.BEGIN_OBJECT;
    } else if (o instanceof HillaJsonArray) {
      return HillaJsonToken.BEGIN_ARRAY;
    } else if (o instanceof HillaJsonPrimitive) {
      HillaJsonPrimitive primitive = (HillaJsonPrimitive) o;
      if (primitive.isString()) {
        return HillaJsonToken.STRING;
      } else if (primitive.isBoolean()) {
        return HillaJsonToken.BOOLEAN;
      } else if (primitive.isNumber()) {
        return HillaJsonToken.NUMBER;
      } else {
        throw new AssertionError();
      }
    } else if (o instanceof HillaJsonNull) {
      return HillaJsonToken.NULL;
    } else if (o == SENTINEL_CLOSED) {
      throw new IllegalStateException("HillaJsonReader is closed");
    } else {
      throw new AssertionError();
    }
  }

  private Object peekStack() {
    return stack[stackSize - 1];
  }

  private Object popStack() {
    Object result = stack[--stackSize];
    stack[stackSize] = null;
    return result;
  }

  private void expect(HillaJsonToken expected) throws IOException {
    if (peek() != expected) {
      throw new IllegalStateException(
          "Expected " + expected + " but was " + peek() + locationString());
    }
  }

  @Override public String nextName() throws IOException {
    expect(HillaJsonToken.NAME);
    Iterator<?> i = (Iterator<?>) peekStack();
    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
    String result = (String) entry.getKey();
    pathNames[stackSize - 1] = result;
    push(entry.getValue());
    return result;
  }

  @Override public String nextString() throws IOException {
    HillaJsonToken token = peek();
    if (token != HillaJsonToken.STRING && token != HillaJsonToken.NUMBER) {
      throw new IllegalStateException(
          "Expected " + HillaJsonToken.STRING + " but was " + token + locationString());
    }
    String result = ((HillaJsonPrimitive) popStack()).getAsString();
    if (stackSize > 0) {
      pathIndices[stackSize - 1]++;
    }
    return result;
  }

  @Override public boolean nextBoolean() throws IOException {
    expect(HillaJsonToken.BOOLEAN);
    boolean result = ((HillaJsonPrimitive) popStack()).getAsBoolean();
    if (stackSize > 0) {
      pathIndices[stackSize - 1]++;
    }
    return result;
  }

  @Override public void nextNull() throws IOException {
    expect(HillaJsonToken.NULL);
    popStack();
    if (stackSize > 0) {
      pathIndices[stackSize - 1]++;
    }
  }

  @Override public double nextDouble() throws IOException {
    HillaJsonToken token = peek();
    if (token != HillaJsonToken.NUMBER && token != HillaJsonToken.STRING) {
      throw new IllegalStateException(
          "Expected " + HillaJsonToken.NUMBER + " but was " + token + locationString());
    }
    double result = ((HillaJsonPrimitive) peekStack()).getAsDouble();
    if (!isLenient() && (Double.isNaN(result) || Double.isInfinite(result))) {
      throw new NumberFormatException("JSON forbids NaN and infinities: " + result);
    }
    popStack();
    if (stackSize > 0) {
      pathIndices[stackSize - 1]++;
    }
    return result;
  }

  @Override public long nextLong() throws IOException {
    HillaJsonToken token = peek();
    if (token != HillaJsonToken.NUMBER && token != HillaJsonToken.STRING) {
      throw new IllegalStateException(
          "Expected " + HillaJsonToken.NUMBER + " but was " + token + locationString());
    }
    long result = ((HillaJsonPrimitive) peekStack()).getAsLong();
    popStack();
    if (stackSize > 0) {
      pathIndices[stackSize - 1]++;
    }
    return result;
  }

  @Override public int nextInt() throws IOException {
    HillaJsonToken token = peek();
    if (token != HillaJsonToken.NUMBER && token != HillaJsonToken.STRING) {
      throw new IllegalStateException(
          "Expected " + HillaJsonToken.NUMBER + " but was " + token + locationString());
    }
    int result = ((HillaJsonPrimitive) peekStack()).getAsInt();
    popStack();
    if (stackSize > 0) {
      pathIndices[stackSize - 1]++;
    }
    return result;
  }

  @Override public void close() throws IOException {
    stack = new Object[] { SENTINEL_CLOSED };
    stackSize = 1;
  }

  @Override public void skipValue() throws IOException {
    if (peek() == HillaJsonToken.NAME) {
      nextName();
      pathNames[stackSize - 2] = "null";
    } else {
      popStack();
      if (stackSize > 0) {
        pathNames[stackSize - 1] = "null";
      }
    }
    if (stackSize > 0) {
      pathIndices[stackSize - 1]++;
    }
  }

  @Override public String toString() {
    return getClass().getSimpleName();
  }

  public void promoteNameToValue() throws IOException {
    expect(HillaJsonToken.NAME);
    Iterator<?> i = (Iterator<?>) peekStack();
    Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();
    push(entry.getValue());
    push(new HillaJsonPrimitive((String) entry.getKey()));
  }

  private void push(Object newTop) {
    if (stackSize == stack.length) {
      int newLength = stackSize * 2;
      stack = Arrays.copyOf(stack, newLength);
      pathIndices = Arrays.copyOf(pathIndices, newLength);
      pathNames = Arrays.copyOf(pathNames, newLength);
    }
    stack[stackSize++] = newTop;
  }

  @Override public String getPath() {
    StringBuilder result = new StringBuilder().append('$');
    for (int i = 0; i < stackSize; i++) {
      if (stack[i] instanceof HillaJsonArray) {
        if (stack[++i] instanceof Iterator) {
          result.append('[').append(pathIndices[i]).append(']');
        }
      } else if (stack[i] instanceof HillaJsonObject) {
        if (stack[++i] instanceof Iterator) {
          result.append('.');
          if (pathNames[i] != null) {
            result.append(pathNames[i]);
          }
        }
      }
    }
    return result.toString();
  }

  private String locationString() {
    return " at path " + getPath();
  }
}
