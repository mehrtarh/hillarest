/*
 * Copyright (C) 2009 Google Inc.
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
package ir.hilla.rest.gson;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import ir.hilla.rest.gson.internal.HillaStreams;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaMalformedJsonException;

/**
 * A parser to parse Json into a parse tree of {@link HillaJsonElement}s
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @since 1.3
 */
public final class HillaJsonParser {
  /** @deprecated No need to instantiate this class, use the static methods instead. */
  @Deprecated
  public HillaJsonParser() {}

  /**
   * Parses the specified JSON string into a parse tree
   *
   * @param json JSON text
   * @return a parse tree of {@link HillaJsonElement}s corresponding to the specified JSON
   * @throws HillaJsonParseException if the specified text is not valid JSON
   */
  public static HillaJsonElement parseString(String json) throws HillaJsonSyntaxException {
    return parseReader(new StringReader(json));
  }

  /**
   * Parses the specified JSON string into a parse tree
   *
   * @param reader JSON text
   * @return a parse tree of {@link HillaJsonElement}s corresponding to the specified JSON
   * @throws HillaJsonParseException if the specified text is not valid JSON
   */
  public static HillaJsonElement parseReader(Reader reader) throws HillaJsonIOException, HillaJsonSyntaxException {
    try {
      HillaJsonReader jsonReader = new HillaJsonReader(reader);
      HillaJsonElement element = parseReader(jsonReader);
      if (!element.isJsonNull() && jsonReader.peek() != HillaJsonToken.END_DOCUMENT) {
        throw new HillaJsonSyntaxException("Did not consume the entire document.");
      }
      return element;
    } catch (HillaMalformedJsonException e) {
      throw new HillaJsonSyntaxException(e);
    } catch (IOException e) {
      throw new HillaJsonIOException(e);
    } catch (NumberFormatException e) {
      throw new HillaJsonSyntaxException(e);
    }
  }

  /**
   * Returns the next value from the JSON stream as a parse tree.
   *
   * @throws HillaJsonParseException if there is an IOException or if the specified
   *     text is not valid JSON
   */
  public static HillaJsonElement parseReader(HillaJsonReader reader)
      throws HillaJsonIOException, HillaJsonSyntaxException {
    boolean lenient = reader.isLenient();
    reader.setLenient(true);
    try {
      return HillaStreams.parse(reader);
    } catch (StackOverflowError e) {
      throw new HillaJsonParseException("Failed parsing JSON source: " + reader + " to Json", e);
    } catch (OutOfMemoryError e) {
      throw new HillaJsonParseException("Failed parsing JSON source: " + reader + " to Json", e);
    } finally {
      reader.setLenient(lenient);
    }
  }

  /** @deprecated Use {@link HillaJsonParser#parseString} */
  @Deprecated
  public HillaJsonElement parse(String json) throws HillaJsonSyntaxException {
    return parseString(json);
  }

  /** @deprecated Use {@link HillaJsonParser#parseReader(Reader)} */
  @Deprecated
  public HillaJsonElement parse(Reader json) throws HillaJsonIOException, HillaJsonSyntaxException {
    return parseReader(json);
  }

  /** @deprecated Use {@link HillaJsonParser#parseReader(HillaJsonReader)} */
  @Deprecated
  public HillaJsonElement parse(HillaJsonReader json) throws HillaJsonIOException, HillaJsonSyntaxException {
    return parseReader(json);
  }
}
