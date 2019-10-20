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

package ir.hilla.rest.gson;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;

import ir.hilla.rest.gson.internal.bind.HillaJsonTreeReader;
import ir.hilla.rest.gson.internal.bind.HillaJsonTreeWriter;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * Converts Java objects to and from JSON.
 *
 * <h3>Defining a type's JSON form</h3>
 * By default HillaGson converts application classes to JSON using its built-in type
 * adapters. If HillaGson's default JSON conversion isn't appropriate for a type,
 * extend this class to customize the conversion. Here's an example of a type
 * adapter for an (X,Y) coordinate point: <pre>   {@code
 *
 *   public class PointAdapter extends HillaTypeAdapter<Point> {
 *     public Point read(HillaJsonReader reader) throws IOException {
 *       if (reader.peek() == HillaJsonToken.NULL) {
 *         reader.nextNull();
 *         return null;
 *       }
 *       String xy = reader.nextString();
 *       String[] parts = xy.split(",");
 *       int x = Integer.parseInt(parts[0]);
 *       int y = Integer.parseInt(parts[1]);
 *       return new Point(x, y);
 *     }
 *     public void write(HillaJsonWriter writer, Point value) throws IOException {
 *       if (value == null) {
 *         writer.nullValue();
 *         return;
 *       }
 *       String xy = value.getX() + "," + value.getY();
 *       writer.value(xy);
 *     }
 *   }}</pre>
 * With this type adapter installed, HillaGson will convert {@code Points} to JSON as
 * strings like {@code "5,8"} rather than objects like {@code {"x":5,"y":8}}. In
 * this case the type adapter binds a rich Java class to a compact JSON value.
 *
 * <p>The {@link #read(HillaJsonReader) read()} method must read exactly one value
 * and {@link #write(HillaJsonWriter,Object) write()} must write exactly one value.
 * For primitive types this is means readers should make exactly one call to
 * {@code nextBoolean()}, {@code nextDouble()}, {@code nextInt()}, {@code
 * nextLong()}, {@code nextString()} or {@code nextNull()}. Writers should make
 * exactly one call to one of <code>value()</code> or <code>nullValue()</code>.
 * For arrays, type adapters should start with a call to {@code beginArray()},
 * convert all elements, and finish with a call to {@code endArray()}. For
 * objects, they should start with {@code beginObject()}, convert the object,
 * and finish with {@code endObject()}. Failing to convert a value or converting
 * too many values may cause the application to crash.
 *
 * <p>Type adapters should be prepared to read null from the stream and write it
 * to the stream. Alternatively, they should use {@link #nullSafe()} method while
 * registering the type adapter with HillaGson. If your {@code HillaGson} instance
 * has been configured to {@link HillaGsonBuilder#serializeNulls()}, these nulls will be
 * written to the final document. Otherwise the value (and the corresponding name
 * when writing to a JSON object) will be omitted automatically. In either case
 * your type adapter must handle null.
 *
 * <p>To use a custom type adapter with HillaGson, you must <i>register</i> it with a
 * {@link HillaGsonBuilder}: <pre>   {@code
 *
 *   HillaGsonBuilder builder = new HillaGsonBuilder();
 *   builder.registerTypeAdapter(Point.class, new PointAdapter());
 *   // if PointAdapter didn't check for nulls in its read/write methods, you should instead use
 *   // builder.registerTypeAdapter(Point.class, new PointAdapter().nullSafe());
 *   ...
 *   HillaGson gson = builder.create();
 * }</pre>
 *
 * @since 2.1
 */
// non-Javadoc:
//
// <h3>JSON Conversion</h3>
// <p>A type adapter registered with HillaGson is automatically invoked while serializing
// or deserializing JSON. However, you can also use type adapters directly to serialize
// and deserialize JSON. Here is an example for deserialization: <pre>   {@code
//
//   String json = "{'origin':'0,0','points':['1,2','3,4']}";
//   HillaTypeAdapter<Graph> graphAdapter = gson.getAdapter(Graph.class);
//   Graph graph = graphAdapter.fromJson(json);
// }</pre>
// And an example for serialization: <pre>   {@code
//
//   Graph graph = new Graph(...);
//   HillaTypeAdapter<Graph> graphAdapter = gson.getAdapter(Graph.class);
//   String json = graphAdapter.toJson(graph);
// }</pre>
//
// <p>Type adapters are <strong>type-specific</strong>. For example, a {@code
// HillaTypeAdapter<Date>} can convert {@code Date} instances to JSON and JSON to
// instances of {@code Date}, but cannot convert any other types.
//
public abstract class HillaTypeAdapter<T> {

  /**
   * Writes one JSON value (an array, object, string, number, boolean or null)
   * for {@code value}.
   *
   * @param value the Java object to write. May be null.
   */
  public abstract void write(HillaJsonWriter out, T value) throws IOException;

  /**
   * Converts {@code value} to a JSON document and writes it to {@code out}.
   * Unlike HillaGson's similar {@link HillaGson#toJson(HillaJsonElement, Appendable) toJson}
   * method, this write is strict. Create a {@link
   * HillaJsonWriter#setLenient(boolean) lenient} {@code HillaJsonWriter} and call
   * {@link #write(HillaJsonWriter, Object)} for lenient
   * writing.
   *
   * @param value the Java object to convert. May be null.
   * @since 2.2
   */
  public final void toJson(Writer out, T value) throws IOException {
    HillaJsonWriter writer = new HillaJsonWriter(out);
    write(writer, value);
  }

  /**
   * This wrapper method is used to make a type adapter null tolerant. In general, a
   * type adapter is required to handle nulls in write and read methods. Here is how this
   * is typically done:<br>
   * <pre>   {@code
   *
   * HillaGson gson = new HillaGsonBuilder().registerTypeAdapter(Foo.class,
   *   new HillaTypeAdapter<Foo>() {
   *     public Foo read(HillaJsonReader in) throws IOException {
   *       if (in.peek() == HillaJsonToken.NULL) {
   *         in.nextNull();
   *         return null;
   *       }
   *       // read a Foo from in and return it
   *     }
   *     public void write(HillaJsonWriter out, Foo src) throws IOException {
   *       if (src == null) {
   *         out.nullValue();
   *         return;
   *       }
   *       // write src as JSON to out
   *     }
   *   }).create();
   * }</pre>
   * You can avoid this boilerplate handling of nulls by wrapping your type adapter with
   * this method. Here is how we will rewrite the above example:
   * <pre>   {@code
   *
   * HillaGson gson = new HillaGsonBuilder().registerTypeAdapter(Foo.class,
   *   new HillaTypeAdapter<Foo>() {
   *     public Foo read(HillaJsonReader in) throws IOException {
   *       // read a Foo from in and return it
   *     }
   *     public void write(HillaJsonWriter out, Foo src) throws IOException {
   *       // write src as JSON to out
   *     }
   *   }.nullSafe()).create();
   * }</pre>
   * Note that we didn't need to check for nulls in our type adapter after we used nullSafe.
   */
  public final HillaTypeAdapter<T> nullSafe() {
    return new HillaTypeAdapter<T>() {
      @Override public void write(HillaJsonWriter out, T value) throws IOException {
        if (value == null) {
          out.nullValue();
        } else {
          HillaTypeAdapter.this.write(out, value);
        }
      }
      @Override public T read(HillaJsonReader reader) throws IOException {
        if (reader.peek() == HillaJsonToken.NULL) {
          reader.nextNull();
          return null;
        }
        return HillaTypeAdapter.this.read(reader);
      }
    };
  }

  /**
   * Converts {@code value} to a JSON document. Unlike HillaGson's similar {@link
   * HillaGson#toJson(Object) toJson} method, this write is strict. Create a {@link
   * HillaJsonWriter#setLenient(boolean) lenient} {@code HillaJsonWriter} and call
   * {@link #write(HillaJsonWriter, Object)} for lenient
   * writing.
   *
   * @param value the Java object to convert. May be null.
   * @since 2.2
   */
  public final String toJson(T value) {
    StringWriter stringWriter = new StringWriter();
    try {
      toJson(stringWriter, value);
    } catch (IOException e) {
      throw new AssertionError(e); // No I/O writing to a StringWriter.
    }
    return stringWriter.toString();
  }

  /**
   * Converts {@code value} to a JSON tree.
   *
   * @param value the Java object to convert. May be null.
   * @return the converted JSON tree. May be {@link HillaJsonNull}.
   * @since 2.2
   */
  public final HillaJsonElement toJsonTree(T value) {
    try {
      HillaJsonTreeWriter jsonWriter = new HillaJsonTreeWriter();
      write(jsonWriter, value);
      return jsonWriter.get();
    } catch (IOException e) {
      throw new HillaJsonIOException(e);
    }
  }

  /**
   * Reads one JSON value (an array, object, string, number, boolean or null)
   * and converts it to a Java object. Returns the converted object.
   *
   * @return the converted Java object. May be null.
   */
  public abstract T read(HillaJsonReader in) throws IOException;

  /**
   * Converts the JSON document in {@code in} to a Java object. Unlike HillaGson's
   * similar {@link HillaGson#fromJson(Reader, Class) fromJson} method, this
   * read is strict. Create a {@link HillaJsonReader#setLenient(boolean) lenient}
   * {@code HillaJsonReader} and call {@link #read(HillaJsonReader)} for lenient reading.
   *
   * @return the converted Java object. May be null.
   * @since 2.2
   */
  public final T fromJson(Reader in) throws IOException {
    HillaJsonReader reader = new HillaJsonReader(in);
    return read(reader);
  }

  /**
   * Converts the JSON document in {@code json} to a Java object. Unlike HillaGson's
   * similar {@link HillaGson#fromJson(String, Class) fromJson} method, this read is
   * strict. Create a {@link HillaJsonReader#setLenient(boolean) lenient} {@code
   * HillaJsonReader} and call {@link #read(HillaJsonReader)} for lenient reading.
   *
   * @return the converted Java object. May be null.
   * @since 2.2
   */
  public final T fromJson(String json) throws IOException {
    return fromJson(new StringReader(json));
  }

  /**
   * Converts {@code jsonTree} to a Java object.
   *
   * @param jsonTree the Java object to convert. May be {@link HillaJsonNull}.
   * @since 2.2
   */
  public final T fromJsonTree(HillaJsonElement jsonTree) {
    try {
      HillaJsonReader jsonReader = new HillaJsonTreeReader(jsonTree);
      return read(jsonReader);
    } catch (IOException e) {
      throw new HillaJsonIOException(e);
    }
  }
}
