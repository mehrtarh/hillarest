/*
 * Copyright (C) 2008 Google Inc.
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

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

import ir.hilla.rest.gson.annotations.HillaExpose;
import ir.hilla.rest.gson.annotations.HillaSince;
import ir.hilla.rest.gson.internal.HillaConstructorConstructor;
import ir.hilla.rest.gson.internal.HillaExcluder;
import ir.hilla.rest.gson.internal.HillaGsonBuildConfig;
import ir.hilla.rest.gson.internal.HillaPrimitives;
import ir.hilla.rest.gson.internal.HillaStreams;
import ir.hilla.rest.gson.internal.bind.HillaArrayTypeAdapter;
import ir.hilla.rest.gson.internal.bind.HillaCollectionTypeAdapterFactory;
import ir.hilla.rest.gson.internal.bind.HillaDateTypeAdapter;
import ir.hilla.rest.gson.internal.bind.HillaJsonAdapterAnnotationTypeAdapterFactory;
import ir.hilla.rest.gson.internal.bind.HillaJsonTreeReader;
import ir.hilla.rest.gson.internal.bind.HillaJsonTreeWriter;
import ir.hilla.rest.gson.internal.bind.HillaMapTypeAdapterFactory;
import ir.hilla.rest.gson.internal.bind.HillaObjectTypeAdapter;
import ir.hilla.rest.gson.internal.bind.HillaReflectiveTypeAdapterFactory;
import ir.hilla.rest.gson.internal.bind.HillaSqlDateTypeAdapter;
import ir.hilla.rest.gson.internal.bind.HillaTimeTypeAdapter;
import ir.hilla.rest.gson.internal.bind.HillaTypeAdapters;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaJsonWriter;
import ir.hilla.rest.gson.stream.HillaMalformedJsonException;

/**
 * This is the main class for using HillaGson. HillaGson is typically used by first constructing a
 * HillaGson instance and then invoking {@link #toJson(Object)} or {@link #fromJson(String, Class)}
 * methods on it. HillaGson instances are Thread-safe so you can reuse them freely across multiple
 * threads.
 *
 * <p>You can create a HillaGson instance by invoking {@code new HillaGson()} if the default configuration
 * is all you need. You can also use {@link HillaGsonBuilder} to build a HillaGson instance with various
 * configuration options such as versioning support, pretty printing, custom
 * {@link HillaJsonSerializer}s, {@link HillaJsonDeserializer}s, and {@link HillaInstanceCreator}s.</p>
 *
 * <p>Here is an example of how HillaGson is used for a simple Class:
 *
 * <pre>
 * HillaGson gson = new HillaGson(); // Or use new HillaGsonBuilder().create();
 * MyType target = new MyType();
 * String json = gson.toJson(target); // serializes target to Json
 * MyType target2 = gson.fromJson(json, MyType.class); // deserializes json into target2
 * </pre></p>
 *
 * <p>If the object that your are serializing/deserializing is a {@code ParameterizedType}
 * (i.e. contains at least one type parameter and may be an array) then you must use the
 * {@link #toJson(Object, Type)} or {@link #fromJson(String, Type)} method. Here is an
 * example for serializing and deserializing a {@code ParameterizedType}:
 *
 * <pre>
 * Type listType = new HillaTypeToken&lt;List&lt;String&gt;&gt;() {}.getType();
 * List&lt;String&gt; target = new LinkedList&lt;String&gt;();
 * target.add("blah");
 *
 * HillaGson gson = new HillaGson();
 * String json = gson.toJson(target, listType);
 * List&lt;String&gt; target2 = gson.fromJson(json, listType);
 * </pre></p>
 *
 * <p>See the <a href="https://sites.google.com/site/gson/gson-user-guide">HillaGson User Guide</a>
 * for a more complete set of examples.</p>
 *
 * @see HillaTypeToken
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @author Jesse Wilson
 */
public final class HillaGson {
  static final boolean DEFAULT_JSON_NON_EXECUTABLE = false;
  static final boolean DEFAULT_LENIENT = false;
  static final boolean DEFAULT_PRETTY_PRINT = false;
  static final boolean DEFAULT_ESCAPE_HTML = true;
  static final boolean DEFAULT_SERIALIZE_NULLS = false;
  static final boolean DEFAULT_COMPLEX_MAP_KEYS = false;
  static final boolean DEFAULT_SPECIALIZE_FLOAT_VALUES = false;

  private static final HillaTypeToken<?> NULL_KEY_SURROGATE = HillaTypeToken.get(Object.class);
  private static final String JSON_NON_EXECUTABLE_PREFIX = ")]}'\n";

  /**
   * This thread local guards against reentrant calls to getAdapter(). In
   * certain object graphs, creating an adapter for a type may recursively
   * require an adapter for the same type! Without intervention, the recursive
   * lookup would stack overflow. We cheat by returning a proxy type adapter.
   * The proxy is wired up once the initial adapter has been created.
   */
  private final ThreadLocal<Map<HillaTypeToken<?>, FutureTypeAdapter<?>>> calls
      = new ThreadLocal<Map<HillaTypeToken<?>, FutureTypeAdapter<?>>>();

  private final Map<HillaTypeToken<?>, HillaTypeAdapter<?>> typeTokenCache = new ConcurrentHashMap<HillaTypeToken<?>, HillaTypeAdapter<?>>();

  private final HillaConstructorConstructor constructorConstructor;
  private final HillaJsonAdapterAnnotationTypeAdapterFactory jsonAdapterFactory;

  final List<HillaTypeAdapterFactory> factories;

  final HillaExcluder excluder;
  final HillaFieldNamingStrategy fieldNamingStrategy;
  final Map<Type, HillaInstanceCreator<?>> instanceCreators;
  final boolean serializeNulls;
  final boolean complexMapKeySerialization;
  final boolean generateNonExecutableJson;
  final boolean htmlSafe;
  final boolean prettyPrinting;
  final boolean lenient;
  final boolean serializeSpecialFloatingPointValues;
  final String datePattern;
  final int dateStyle;
  final int timeStyle;
  final HillaLongSerializationPolicy longSerializationPolicy;
  final List<HillaTypeAdapterFactory> builderFactories;
  final List<HillaTypeAdapterFactory> builderHierarchyFactories;

  /**
   * Constructs a HillaGson object with default configuration. The default configuration has the
   * following settings:
   * <ul>
   *   <li>The JSON generated by <code>toJson</code> methods is in compact representation. This
   *   means that all the unneeded white-space is removed. You can change this behavior with
   *   {@link HillaGsonBuilder#setPrettyPrinting()}. </li>
   *   <li>The generated JSON omits all the fields that are null. Note that nulls in arrays are
   *   kept as is since an array is an ordered list. Moreover, if a field is not null, but its
   *   generated JSON is empty, the field is kept. You can configure HillaGson to serialize null values
   *   by setting {@link HillaGsonBuilder#serializeNulls()}.</li>
   *   <li>HillaGson provides default serialization and deserialization for Enums, {@link Map},
   *   {@link java.net.URL}, {@link java.net.URI}, {@link java.util.Locale}, {@link java.util.Date},
   *   {@link BigDecimal}, and {@link BigInteger} classes. If you would prefer
   *   to change the default representation, you can do so by registering a type adapter through
   *   {@link HillaGsonBuilder#registerTypeAdapter(Type, Object)}. </li>
   *   <li>The default Date format is same as {@link DateFormat#DEFAULT}. This format
   *   ignores the millisecond portion of the date during serialization. You can change
   *   this by invoking {@link HillaGsonBuilder#setDateFormat(int)} or
   *   {@link HillaGsonBuilder#setDateFormat(String)}. </li>
   *   <li>By default, HillaGson ignores the {@link HillaExpose} annotation.
   *   You can enable HillaGson to serialize/deserialize only those fields marked with this annotation
   *   through {@link HillaGsonBuilder#excludeFieldsWithoutExposeAnnotation()}. </li>
   *   <li>By default, HillaGson ignores the {@link HillaSince} annotation. You
   *   can enable HillaGson to use this annotation through {@link HillaGsonBuilder#setVersion(double)}.</li>
   *   <li>The default field naming policy for the output Json is same as in Java. So, a Java class
   *   field <code>versionNumber</code> will be output as <code>&quot;versionNumber&quot;</code> in
   *   Json. The same rules are applied for mapping incoming Json to the Java classes. You can
   *   change this policy through {@link HillaGsonBuilder#setFieldNamingPolicy(HillaFieldNamingPolicy)}.</li>
   *   <li>By default, HillaGson excludes <code>transient</code> or <code>static</code> fields from
   *   consideration for serialization and deserialization. You can change this behavior through
   *   {@link HillaGsonBuilder#excludeFieldsWithModifiers(int...)}.</li>
   * </ul>
   */
  public HillaGson() {
    this(HillaExcluder.DEFAULT, HillaFieldNamingPolicy.IDENTITY,
        Collections.<Type, HillaInstanceCreator<?>>emptyMap(), DEFAULT_SERIALIZE_NULLS,
        DEFAULT_COMPLEX_MAP_KEYS, DEFAULT_JSON_NON_EXECUTABLE, DEFAULT_ESCAPE_HTML,
        DEFAULT_PRETTY_PRINT, DEFAULT_LENIENT, DEFAULT_SPECIALIZE_FLOAT_VALUES,
        HillaLongSerializationPolicy.DEFAULT, null, DateFormat.DEFAULT, DateFormat.DEFAULT,
        Collections.<HillaTypeAdapterFactory>emptyList(), Collections.<HillaTypeAdapterFactory>emptyList(),
        Collections.<HillaTypeAdapterFactory>emptyList());
  }

  HillaGson(HillaExcluder excluder, HillaFieldNamingStrategy fieldNamingStrategy,
            Map<Type, HillaInstanceCreator<?>> instanceCreators, boolean serializeNulls,
            boolean complexMapKeySerialization, boolean generateNonExecutableGson, boolean htmlSafe,
            boolean prettyPrinting, boolean lenient, boolean serializeSpecialFloatingPointValues,
            HillaLongSerializationPolicy longSerializationPolicy, String datePattern, int dateStyle,
            int timeStyle, List<HillaTypeAdapterFactory> builderFactories,
            List<HillaTypeAdapterFactory> builderHierarchyFactories,
            List<HillaTypeAdapterFactory> factoriesToBeAdded) {
    this.excluder = excluder;
    this.fieldNamingStrategy = fieldNamingStrategy;
    this.instanceCreators = instanceCreators;
    this.constructorConstructor = new HillaConstructorConstructor(instanceCreators);
    this.serializeNulls = serializeNulls;
    this.complexMapKeySerialization = complexMapKeySerialization;
    this.generateNonExecutableJson = generateNonExecutableGson;
    this.htmlSafe = htmlSafe;
    this.prettyPrinting = prettyPrinting;
    this.lenient = lenient;
    this.serializeSpecialFloatingPointValues = serializeSpecialFloatingPointValues;
    this.longSerializationPolicy = longSerializationPolicy;
    this.datePattern = datePattern;
    this.dateStyle = dateStyle;
    this.timeStyle = timeStyle;
    this.builderFactories = builderFactories;
    this.builderHierarchyFactories = builderHierarchyFactories;

    List<HillaTypeAdapterFactory> factories = new ArrayList<HillaTypeAdapterFactory>();

    // built-in type adapters that cannot be overridden
    factories.add(HillaTypeAdapters.JSON_ELEMENT_FACTORY);
    factories.add(HillaObjectTypeAdapter.FACTORY);

    // the excluder must precede all adapters that handle user-defined types
    factories.add(excluder);

    // users' type adapters
    factories.addAll(factoriesToBeAdded);

    // type adapters for basic platform types
    factories.add(HillaTypeAdapters.STRING_FACTORY);
    factories.add(HillaTypeAdapters.INTEGER_FACTORY);
    factories.add(HillaTypeAdapters.BOOLEAN_FACTORY);
    factories.add(HillaTypeAdapters.BYTE_FACTORY);
    factories.add(HillaTypeAdapters.SHORT_FACTORY);
    HillaTypeAdapter<Number> longAdapter = longAdapter(longSerializationPolicy);
    factories.add(HillaTypeAdapters.newFactory(long.class, Long.class, longAdapter));
    factories.add(HillaTypeAdapters.newFactory(double.class, Double.class,
            doubleAdapter(serializeSpecialFloatingPointValues)));
    factories.add(HillaTypeAdapters.newFactory(float.class, Float.class,
            floatAdapter(serializeSpecialFloatingPointValues)));
    factories.add(HillaTypeAdapters.NUMBER_FACTORY);
    factories.add(HillaTypeAdapters.ATOMIC_INTEGER_FACTORY);
    factories.add(HillaTypeAdapters.ATOMIC_BOOLEAN_FACTORY);
    factories.add(HillaTypeAdapters.newFactory(AtomicLong.class, atomicLongAdapter(longAdapter)));
    factories.add(HillaTypeAdapters.newFactory(AtomicLongArray.class, atomicLongArrayAdapter(longAdapter)));
    factories.add(HillaTypeAdapters.ATOMIC_INTEGER_ARRAY_FACTORY);
    factories.add(HillaTypeAdapters.CHARACTER_FACTORY);
    factories.add(HillaTypeAdapters.STRING_BUILDER_FACTORY);
    factories.add(HillaTypeAdapters.STRING_BUFFER_FACTORY);
    factories.add(HillaTypeAdapters.newFactory(BigDecimal.class, HillaTypeAdapters.BIG_DECIMAL));
    factories.add(HillaTypeAdapters.newFactory(BigInteger.class, HillaTypeAdapters.BIG_INTEGER));
    factories.add(HillaTypeAdapters.URL_FACTORY);
    factories.add(HillaTypeAdapters.URI_FACTORY);
    factories.add(HillaTypeAdapters.UUID_FACTORY);
    factories.add(HillaTypeAdapters.CURRENCY_FACTORY);
    factories.add(HillaTypeAdapters.LOCALE_FACTORY);
    factories.add(HillaTypeAdapters.INET_ADDRESS_FACTORY);
    factories.add(HillaTypeAdapters.BIT_SET_FACTORY);
    factories.add(HillaDateTypeAdapter.FACTORY);
    factories.add(HillaTypeAdapters.CALENDAR_FACTORY);
    factories.add(HillaTimeTypeAdapter.FACTORY);
    factories.add(HillaSqlDateTypeAdapter.FACTORY);
    factories.add(HillaTypeAdapters.TIMESTAMP_FACTORY);
    factories.add(HillaArrayTypeAdapter.FACTORY);
    factories.add(HillaTypeAdapters.CLASS_FACTORY);

    // type adapters for composite and user-defined types
    factories.add(new HillaCollectionTypeAdapterFactory(constructorConstructor));
    factories.add(new HillaMapTypeAdapterFactory(constructorConstructor, complexMapKeySerialization));
    this.jsonAdapterFactory = new HillaJsonAdapterAnnotationTypeAdapterFactory(constructorConstructor);
    factories.add(jsonAdapterFactory);
    factories.add(HillaTypeAdapters.ENUM_FACTORY);
    factories.add(new HillaReflectiveTypeAdapterFactory(
        constructorConstructor, fieldNamingStrategy, excluder, jsonAdapterFactory));

    this.factories = Collections.unmodifiableList(factories);
  }

  /**
   * Returns a new HillaGsonBuilder containing all custom factories and configuration used by the current
   * instance.
   *
   * @return a HillaGsonBuilder instance.
   */
  public HillaGsonBuilder newBuilder() {
    return new HillaGsonBuilder(this);
  }

  public HillaExcluder excluder() {
    return excluder;
  }

  public HillaFieldNamingStrategy fieldNamingStrategy() {
    return fieldNamingStrategy;
  }

  public boolean serializeNulls() {
    return serializeNulls;
  }

  public boolean htmlSafe() {
    return htmlSafe;
  }

  private HillaTypeAdapter<Number> doubleAdapter(boolean serializeSpecialFloatingPointValues) {
    if (serializeSpecialFloatingPointValues) {
      return HillaTypeAdapters.DOUBLE;
    }
    return new HillaTypeAdapter<Number>() {
      @Override public Double read(HillaJsonReader in) throws IOException {
        if (in.peek() == HillaJsonToken.NULL) {
          in.nextNull();
          return null;
        }
        return in.nextDouble();
      }
      @Override public void write(HillaJsonWriter out, Number value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }
        double doubleValue = value.doubleValue();
        checkValidFloatingPoint(doubleValue);
        out.value(value);
      }
    };
  }

  private HillaTypeAdapter<Number> floatAdapter(boolean serializeSpecialFloatingPointValues) {
    if (serializeSpecialFloatingPointValues) {
      return HillaTypeAdapters.FLOAT;
    }
    return new HillaTypeAdapter<Number>() {
      @Override public Float read(HillaJsonReader in) throws IOException {
        if (in.peek() == HillaJsonToken.NULL) {
          in.nextNull();
          return null;
        }
        return (float) in.nextDouble();
      }
      @Override public void write(HillaJsonWriter out, Number value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }
        float floatValue = value.floatValue();
        checkValidFloatingPoint(floatValue);
        out.value(value);
      }
    };
  }

  static void checkValidFloatingPoint(double value) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      throw new IllegalArgumentException(value
          + " is not a valid double value as per JSON specification. To override this"
          + " behavior, use HillaGsonBuilder.serializeSpecialFloatingPointValues() method.");
    }
  }

  private static HillaTypeAdapter<Number> longAdapter(HillaLongSerializationPolicy longSerializationPolicy) {
    if (longSerializationPolicy == HillaLongSerializationPolicy.DEFAULT) {
      return HillaTypeAdapters.LONG;
    }
    return new HillaTypeAdapter<Number>() {
      @Override public Number read(HillaJsonReader in) throws IOException {
        if (in.peek() == HillaJsonToken.NULL) {
          in.nextNull();
          return null;
        }
        return in.nextLong();
      }
      @Override public void write(HillaJsonWriter out, Number value) throws IOException {
        if (value == null) {
          out.nullValue();
          return;
        }
        out.value(value.toString());
      }
    };
  }

  private static HillaTypeAdapter<AtomicLong> atomicLongAdapter(final HillaTypeAdapter<Number> longAdapter) {
    return new HillaTypeAdapter<AtomicLong>() {
      @Override public void write(HillaJsonWriter out, AtomicLong value) throws IOException {
        longAdapter.write(out, value.get());
      }
      @Override public AtomicLong read(HillaJsonReader in) throws IOException {
        Number value = longAdapter.read(in);
        return new AtomicLong(value.longValue());
      }
    }.nullSafe();
  }

  private static HillaTypeAdapter<AtomicLongArray> atomicLongArrayAdapter(final HillaTypeAdapter<Number> longAdapter) {
    return new HillaTypeAdapter<AtomicLongArray>() {
      @Override public void write(HillaJsonWriter out, AtomicLongArray value) throws IOException {
        out.beginArray();
        for (int i = 0, length = value.length(); i < length; i++) {
          longAdapter.write(out, value.get(i));
        }
        out.endArray();
      }
      @Override public AtomicLongArray read(HillaJsonReader in) throws IOException {
        List<Long> list = new ArrayList<Long>();
        in.beginArray();
        while (in.hasNext()) {
            long value = longAdapter.read(in).longValue();
            list.add(value);
        }
        in.endArray();
        int length = list.size();
        AtomicLongArray array = new AtomicLongArray(length);
        for (int i = 0; i < length; ++i) {
          array.set(i, list.get(i));
        }
        return array;
      }
    }.nullSafe();
  }

  /**
   * Returns the type adapter for {@code} type.
   *
   * @throws IllegalArgumentException if this GSON cannot serialize and
   *     deserialize {@code type}.
   */
  @SuppressWarnings("unchecked")
  public <T> HillaTypeAdapter<T> getAdapter(HillaTypeToken<T> type) {
    HillaTypeAdapter<?> cached = typeTokenCache.get(type == null ? NULL_KEY_SURROGATE : type);
    if (cached != null) {
      return (HillaTypeAdapter<T>) cached;
    }

    Map<HillaTypeToken<?>, FutureTypeAdapter<?>> threadCalls = calls.get();
    boolean requiresThreadLocalCleanup = false;
    if (threadCalls == null) {
      threadCalls = new HashMap<HillaTypeToken<?>, FutureTypeAdapter<?>>();
      calls.set(threadCalls);
      requiresThreadLocalCleanup = true;
    }

    // the key and value type parameters always agree
    FutureTypeAdapter<T> ongoingCall = (FutureTypeAdapter<T>) threadCalls.get(type);
    if (ongoingCall != null) {
      return ongoingCall;
    }

    try {
      FutureTypeAdapter<T> call = new FutureTypeAdapter<T>();
      threadCalls.put(type, call);

      for (HillaTypeAdapterFactory factory : factories) {
        HillaTypeAdapter<T> candidate = factory.create(this, type);
        if (candidate != null) {
          call.setDelegate(candidate);
          typeTokenCache.put(type, candidate);
          return candidate;
        }
      }
      throw new IllegalArgumentException("GSON (" + HillaGsonBuildConfig.VERSION + ") cannot handle " + type);
    } finally {
      threadCalls.remove(type);

      if (requiresThreadLocalCleanup) {
        calls.remove();
      }
    }
  }

  /**
   * This method is used to get an alternate type adapter for the specified type. This is used
   * to access a type adapter that is overridden by a {@link HillaTypeAdapterFactory} that you
   * may have registered. This features is typically used when you want to register a type
   * adapter that does a little bit of work but then delegates further processing to the HillaGson
   * default type adapter. Here is an example:
   * <p>Let's say we want to write a type adapter that counts the number of objects being read
   *  from or written to JSON. We can achieve this by writing a type adapter factory that uses
   *  the <code>getDelegateAdapter</code> method:
   *  <pre> {@code
   *  class StatsTypeAdapterFactory implements HillaTypeAdapterFactory {
   *    public int numReads = 0;
   *    public int numWrites = 0;
   *    public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> type) {
   *      final HillaTypeAdapter<T> delegate = gson.getDelegateAdapter(this, type);
   *      return new HillaTypeAdapter<T>() {
   *        public void write(HillaJsonWriter out, T value) throws IOException {
   *          ++numWrites;
   *          delegate.write(out, value);
   *        }
   *        public T read(HillaJsonReader in) throws IOException {
   *          ++numReads;
   *          return delegate.read(in);
   *        }
   *      };
   *    }
   *  }
   *  } </pre>
   *  This factory can now be used like this:
   *  <pre> {@code
   *  StatsTypeAdapterFactory stats = new StatsTypeAdapterFactory();
   *  HillaGson gson = new HillaGsonBuilder().registerTypeAdapterFactory(stats).create();
   *  // HillaRestCall gson.toJson() and fromJson methods on objects
   *  System.out.println("Num JSON reads" + stats.numReads);
   *  System.out.println("Num JSON writes" + stats.numWrites);
   *  }</pre>
   *  Note that this call will skip all factories registered before {@code skipPast}. In case of
   *  multiple TypeAdapterFactories registered it is up to the caller of this function to insure
   *  that the order of registration does not prevent this method from reaching a factory they
   *  would expect to reply from this call.
   *  Note that since you can not override type adapter factories for String and Java primitive
   *  types, our stats factory will not count the number of String or primitives that will be
   *  read or written.
   * @param skipPast The type adapter factory that needs to be skipped while searching for
   *   a matching type adapter. In most cases, you should just pass <i>this</i> (the type adapter
   *   factory from where {@link #getDelegateAdapter} method is being invoked).
   * @param type Type for which the delegate adapter is being searched for.
   *
   * @since 2.2
   */
  public <T> HillaTypeAdapter<T> getDelegateAdapter(HillaTypeAdapterFactory skipPast, HillaTypeToken<T> type) {
    // Hack. If the skipPast factory isn't registered, assume the factory is being requested via
    // our @HillaJsonAdapter annotation.
    if (!factories.contains(skipPast)) {
      skipPast = jsonAdapterFactory;
    }

    boolean skipPastFound = false;
    for (HillaTypeAdapterFactory factory : factories) {
      if (!skipPastFound) {
        if (factory == skipPast) {
          skipPastFound = true;
        }
        continue;
      }

      HillaTypeAdapter<T> candidate = factory.create(this, type);
      if (candidate != null) {
        return candidate;
      }
    }
    throw new IllegalArgumentException("GSON cannot serialize " + type);
  }

  /**
   * Returns the type adapter for {@code} type.
   *
   * @throws IllegalArgumentException if this GSON cannot serialize and
   *     deserialize {@code type}.
   */
  public <T> HillaTypeAdapter<T> getAdapter(Class<T> type) {
    return getAdapter(HillaTypeToken.get(type));
  }

  /**
   * This method serializes the specified object into its equivalent representation as a tree of
   * {@link HillaJsonElement}s. This method should be used when the specified object is not a generic
   * type. This method uses {@link Class#getClass()} to get the type for the specified object, but
   * the {@code getClass()} loses the generic type information because of the Type Erasure feature
   * of Java. Note that this method works fine if the any of the object fields are of generic type,
   * just the object itself should not be of a generic type. If the object is of generic type, use
   * {@link #toJsonTree(Object, Type)} instead.
   *
   * @param src the object for which Json representation is to be created setting for HillaGson
   * @return Json representation of {@code src}.
   * @since 1.4
   */
  public HillaJsonElement toJsonTree(Object src) {
    if (src == null) {
      return HillaJsonNull.INSTANCE;
    }
    return toJsonTree(src, src.getClass());
  }

  /**
   * This method serializes the specified object, including those of generic types, into its
   * equivalent representation as a tree of {@link HillaJsonElement}s. This method must be used if the
   * specified object is a generic type. For non-generic objects, use {@link #toJsonTree(Object)}
   * instead.
   *
   * @param src the object for which JSON representation is to be created
   * @param typeOfSrc The specific genericized type of src. You can obtain
   * this type by using the {@link HillaTypeToken} class. For example,
   * to get the type for {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfSrc = new HillaTypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @return Json representation of {@code src}
   * @since 1.4
   */
  public HillaJsonElement toJsonTree(Object src, Type typeOfSrc) {
    HillaJsonTreeWriter writer = new HillaJsonTreeWriter();
    toJson(src, typeOfSrc, writer);
    return writer.get();
  }

  /**
   * This method serializes the specified object into its equivalent Json representation.
   * This method should be used when the specified object is not a generic type. This method uses
   * {@link Class#getClass()} to get the type for the specified object, but the
   * {@code getClass()} loses the generic type information because of the Type Erasure feature
   * of Java. Note that this method works fine if the any of the object fields are of generic type,
   * just the object itself should not be of a generic type. If the object is of generic type, use
   * {@link #toJson(Object, Type)} instead. If you want to write out the object to a
   * {@link Writer}, use {@link #toJson(Object, Appendable)} instead.
   *
   * @param src the object for which Json representation is to be created setting for HillaGson
   * @return Json representation of {@code src}.
   */
  public String toJson(Object src) {
    if (src == null) {
      return toJson(HillaJsonNull.INSTANCE);
    }
    return toJson(src, src.getClass());
  }

  /**
   * This method serializes the specified object, including those of generic types, into its
   * equivalent Json representation. This method must be used if the specified object is a generic
   * type. For non-generic objects, use {@link #toJson(Object)} instead. If you want to write out
   * the object to a {@link Appendable}, use {@link #toJson(Object, Type, Appendable)} instead.
   *
   * @param src the object for which JSON representation is to be created
   * @param typeOfSrc The specific genericized type of src. You can obtain
   * this type by using the {@link HillaTypeToken} class. For example,
   * to get the type for {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfSrc = new HillaTypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @return Json representation of {@code src}
   */
  public String toJson(Object src, Type typeOfSrc) {
    StringWriter writer = new StringWriter();
    toJson(src, typeOfSrc, writer);
    return writer.toString();
  }

  /**
   * This method serializes the specified object into its equivalent Json representation.
   * This method should be used when the specified object is not a generic type. This method uses
   * {@link Class#getClass()} to get the type for the specified object, but the
   * {@code getClass()} loses the generic type information because of the Type Erasure feature
   * of Java. Note that this method works fine if the any of the object fields are of generic type,
   * just the object itself should not be of a generic type. If the object is of generic type, use
   * {@link #toJson(Object, Type, Appendable)} instead.
   *
   * @param src the object for which Json representation is to be created setting for HillaGson
   * @param writer Writer to which the Json representation needs to be written
   * @throws HillaJsonIOException if there was a problem writing to the writer
   * @since 1.2
   */
  public void toJson(Object src, Appendable writer) throws HillaJsonIOException {
    if (src != null) {
      toJson(src, src.getClass(), writer);
    } else {
      toJson(HillaJsonNull.INSTANCE, writer);
    }
  }

  /**
   * This method serializes the specified object, including those of generic types, into its
   * equivalent Json representation. This method must be used if the specified object is a generic
   * type. For non-generic objects, use {@link #toJson(Object, Appendable)} instead.
   *
   * @param src the object for which JSON representation is to be created
   * @param typeOfSrc The specific genericized type of src. You can obtain
   * this type by using the {@link HillaTypeToken} class. For example,
   * to get the type for {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfSrc = new HillaTypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @param writer Writer to which the Json representation of src needs to be written.
   * @throws HillaJsonIOException if there was a problem writing to the writer
   * @since 1.2
   */
  public void toJson(Object src, Type typeOfSrc, Appendable writer) throws HillaJsonIOException {
    try {
      HillaJsonWriter jsonWriter = newJsonWriter(HillaStreams.writerForAppendable(writer));
      toJson(src, typeOfSrc, jsonWriter);
    } catch (IOException e) {
      throw new HillaJsonIOException(e);
    }
  }

  /**
   * Writes the JSON representation of {@code src} of type {@code typeOfSrc} to
   * {@code writer}.
   * @throws HillaJsonIOException if there was a problem writing to the writer
   */
  @SuppressWarnings("unchecked")
  public void toJson(Object src, Type typeOfSrc, HillaJsonWriter writer) throws HillaJsonIOException {
    HillaTypeAdapter<?> adapter = getAdapter(HillaTypeToken.get(typeOfSrc));
    boolean oldLenient = writer.isLenient();
    writer.setLenient(true);
    boolean oldHtmlSafe = writer.isHtmlSafe();
    writer.setHtmlSafe(htmlSafe);
    boolean oldSerializeNulls = writer.getSerializeNulls();
    writer.setSerializeNulls(serializeNulls);
    try {
      ((HillaTypeAdapter<Object>) adapter).write(writer, src);
    } catch (IOException e) {
      throw new HillaJsonIOException(e);
    } catch (AssertionError e) {
      AssertionError error = new AssertionError("AssertionError (GSON " + HillaGsonBuildConfig.VERSION + "): " + e.getMessage());
      error.initCause(e);
      throw error;
    } finally {
      writer.setLenient(oldLenient);
      writer.setHtmlSafe(oldHtmlSafe);
      writer.setSerializeNulls(oldSerializeNulls);
    }
  }

  /**
   * Converts a tree of {@link HillaJsonElement}s into its equivalent JSON representation.
   *
   * @param jsonElement root of a tree of {@link HillaJsonElement}s
   * @return JSON String representation of the tree
   * @since 1.4
   */
  public String toJson(HillaJsonElement jsonElement) {
    StringWriter writer = new StringWriter();
    toJson(jsonElement, writer);
    return writer.toString();
  }

  /**
   * Writes out the equivalent JSON for a tree of {@link HillaJsonElement}s.
   *
   * @param jsonElement root of a tree of {@link HillaJsonElement}s
   * @param writer Writer to which the Json representation needs to be written
   * @throws HillaJsonIOException if there was a problem writing to the writer
   * @since 1.4
   */
  public void toJson(HillaJsonElement jsonElement, Appendable writer) throws HillaJsonIOException {
    try {
      HillaJsonWriter jsonWriter = newJsonWriter(HillaStreams.writerForAppendable(writer));
      toJson(jsonElement, jsonWriter);
    } catch (IOException e) {
      throw new HillaJsonIOException(e);
    }
  }

  /**
   * Returns a new JSON writer configured for the settings on this HillaGson instance.
   */
  public HillaJsonWriter newJsonWriter(Writer writer) throws IOException {
    if (generateNonExecutableJson) {
      writer.write(JSON_NON_EXECUTABLE_PREFIX);
    }
    HillaJsonWriter jsonWriter = new HillaJsonWriter(writer);
    if (prettyPrinting) {
      jsonWriter.setIndent("  ");
    }
    jsonWriter.setSerializeNulls(serializeNulls);
    return jsonWriter;
  }

  /**
   * Returns a new JSON reader configured for the settings on this HillaGson instance.
   */
  public HillaJsonReader newJsonReader(Reader reader) {
    HillaJsonReader jsonReader = new HillaJsonReader(reader);
    jsonReader.setLenient(lenient);
    return jsonReader;
  }

  /**
   * Writes the JSON for {@code jsonElement} to {@code writer}.
   * @throws HillaJsonIOException if there was a problem writing to the writer
   */
  public void toJson(HillaJsonElement jsonElement, HillaJsonWriter writer) throws HillaJsonIOException {
    boolean oldLenient = writer.isLenient();
    writer.setLenient(true);
    boolean oldHtmlSafe = writer.isHtmlSafe();
    writer.setHtmlSafe(htmlSafe);
    boolean oldSerializeNulls = writer.getSerializeNulls();
    writer.setSerializeNulls(serializeNulls);
    try {
      HillaStreams.write(jsonElement, writer);
    } catch (IOException e) {
      throw new HillaJsonIOException(e);
    } catch (AssertionError e) {
      AssertionError error = new AssertionError("AssertionError (GSON " + HillaGsonBuildConfig.VERSION + "): " + e.getMessage());
      error.initCause(e);
      throw error;
    } finally {
      writer.setLenient(oldLenient);
      writer.setHtmlSafe(oldHtmlSafe);
      writer.setSerializeNulls(oldSerializeNulls);
    }
  }

  /**
   * This method deserializes the specified Json into an object of the specified class. It is not
   * suitable to use if the specified class is a generic type since it will not have the generic
   * type information because of the Type Erasure feature of Java. Therefore, this method should not
   * be used if the desired type is a generic type. Note that this method works fine if the any of
   * the fields of the specified object are generics, just the object itself should not be a
   * generic type. For the cases when the object is of generic type, invoke
   * {@link #fromJson(String, Type)}. If you have the Json in a {@link Reader} instead of
   * a String, use {@link #fromJson(Reader, Class)} instead.
   *
   * @param <T> the type of the desired object
   * @param json the string from which the object is to be deserialized
   * @param classOfT the class of T
   * @return an object of type T from the string. Returns {@code null} if {@code json} is {@code null}
   * or if {@code json} is empty.
   * @throws HillaJsonSyntaxException if json is not a valid representation for an object of type
   * classOfT
   */
  public <T> T fromJson(String json, Class<T> classOfT) throws HillaJsonSyntaxException {
    Object object = fromJson(json, (Type) classOfT);
    return HillaPrimitives.wrap(classOfT).cast(object);
  }

  /**
   * This method deserializes the specified Json into an object of the specified type. This method
   * is useful if the specified object is a generic type. For non-generic objects, use
   * {@link #fromJson(String, Class)} instead. If you have the Json in a {@link Reader} instead of
   * a String, use {@link #fromJson(Reader, Type)} instead.
   *
   * @param <T> the type of the desired object
   * @param json the string from which the object is to be deserialized
   * @param typeOfT The specific genericized type of src. You can obtain this type by using the
   * {@link HillaTypeToken} class. For example, to get the type for
   * {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfT = new HillaTypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @return an object of type T from the string. Returns {@code null} if {@code json} is {@code null}
   * or if {@code json} is empty.
   * @throws HillaJsonParseException if json is not a valid representation for an object of type typeOfT
   * @throws HillaJsonSyntaxException if json is not a valid representation for an object of type
   */
  @SuppressWarnings("unchecked")
  public <T> T fromJson(String json, Type typeOfT) throws HillaJsonSyntaxException {
    if (json == null) {
      return null;
    }
    StringReader reader = new StringReader(json);
    T target = (T) fromJson(reader, typeOfT);
    return target;
  }

  /**
   * This method deserializes the Json read from the specified reader into an object of the
   * specified class. It is not suitable to use if the specified class is a generic type since it
   * will not have the generic type information because of the Type Erasure feature of Java.
   * Therefore, this method should not be used if the desired type is a generic type. Note that
   * this method works fine if the any of the fields of the specified object are generics, just the
   * object itself should not be a generic type. For the cases when the object is of generic type,
   * invoke {@link #fromJson(Reader, Type)}. If you have the Json in a String form instead of a
   * {@link Reader}, use {@link #fromJson(String, Class)} instead.
   *
   * @param <T> the type of the desired object
   * @param json the reader producing the Json from which the object is to be deserialized.
   * @param classOfT the class of T
   * @return an object of type T from the string. Returns {@code null} if {@code json} is at EOF.
   * @throws HillaJsonIOException if there was a problem reading from the Reader
   * @throws HillaJsonSyntaxException if json is not a valid representation for an object of type
   * @since 1.2
   */
  public <T> T fromJson(Reader json, Class<T> classOfT) throws HillaJsonSyntaxException, HillaJsonIOException {
    HillaJsonReader jsonReader = newJsonReader(json);
    Object object = fromJson(jsonReader, classOfT);
    assertFullConsumption(object, jsonReader);
    return HillaPrimitives.wrap(classOfT).cast(object);
  }

  /**
   * This method deserializes the Json read from the specified reader into an object of the
   * specified type. This method is useful if the specified object is a generic type. For
   * non-generic objects, use {@link #fromJson(Reader, Class)} instead. If you have the Json in a
   * String form instead of a {@link Reader}, use {@link #fromJson(String, Type)} instead.
   *
   * @param <T> the type of the desired object
   * @param json the reader producing Json from which the object is to be deserialized
   * @param typeOfT The specific genericized type of src. You can obtain this type by using the
   * {@link HillaTypeToken} class. For example, to get the type for
   * {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfT = new HillaTypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @return an object of type T from the json. Returns {@code null} if {@code json} is at EOF.
   * @throws HillaJsonIOException if there was a problem reading from the Reader
   * @throws HillaJsonSyntaxException if json is not a valid representation for an object of type
   * @since 1.2
   */
  @SuppressWarnings("unchecked")
  public <T> T fromJson(Reader json, Type typeOfT) throws HillaJsonIOException, HillaJsonSyntaxException {
    HillaJsonReader jsonReader = newJsonReader(json);
    T object = (T) fromJson(jsonReader, typeOfT);
    assertFullConsumption(object, jsonReader);
    return object;
  }

  private static void assertFullConsumption(Object obj, HillaJsonReader reader) {
    try {
      if (obj != null && reader.peek() != HillaJsonToken.END_DOCUMENT) {
        throw new HillaJsonIOException("JSON document was not fully consumed.");
      }
    } catch (HillaMalformedJsonException e) {
      throw new HillaJsonSyntaxException(e);
    } catch (IOException e) {
      throw new HillaJsonIOException(e);
    }
  }

  /**
   * Reads the next JSON value from {@code reader} and convert it to an object
   * of type {@code typeOfT}. Returns {@code null}, if the {@code reader} is at EOF.
   * HillaSince Type is not parameterized by T, this method is type unsafe and should be used carefully
   *
   * @throws HillaJsonIOException if there was a problem writing to the Reader
   * @throws HillaJsonSyntaxException if json is not a valid representation for an object of type
   */
  @SuppressWarnings("unchecked")
  public <T> T fromJson(HillaJsonReader reader, Type typeOfT) throws HillaJsonIOException, HillaJsonSyntaxException {
    boolean isEmpty = true;
    boolean oldLenient = reader.isLenient();
    reader.setLenient(true);
    try {
      reader.peek();
      isEmpty = false;
      HillaTypeToken<T> typeToken = (HillaTypeToken<T>) HillaTypeToken.get(typeOfT);
      HillaTypeAdapter<T> typeAdapter = getAdapter(typeToken);
      T object = typeAdapter.read(reader);
      return object;
    } catch (EOFException e) {
      /*
       * For compatibility with JSON 1.5 and earlier, we return null for empty
       * documents instead of throwing.
       */
      if (isEmpty) {
        return null;
      }
      throw new HillaJsonSyntaxException(e);
    } catch (IllegalStateException e) {
      throw new HillaJsonSyntaxException(e);
    } catch (IOException e) {
      // TODO(inder): Figure out whether it is indeed right to rethrow this as HillaJsonSyntaxException
      throw new HillaJsonSyntaxException(e);
    } catch (AssertionError e) {
      AssertionError error = new AssertionError("AssertionError (GSON " + HillaGsonBuildConfig.VERSION + "): " + e.getMessage());
      error.initCause(e);
      throw error;
    } finally {
      reader.setLenient(oldLenient);
    }
  }

  /**
   * This method deserializes the Json read from the specified parse tree into an object of the
   * specified type. It is not suitable to use if the specified class is a generic type since it
   * will not have the generic type information because of the Type Erasure feature of Java.
   * Therefore, this method should not be used if the desired type is a generic type. Note that
   * this method works fine if the any of the fields of the specified object are generics, just the
   * object itself should not be a generic type. For the cases when the object is of generic type,
   * invoke {@link #fromJson(HillaJsonElement, Type)}.
   * @param <T> the type of the desired object
   * @param json the root of the parse tree of {@link HillaJsonElement}s from which the object is to
   * be deserialized
   * @param classOfT The class of T
   * @return an object of type T from the json. Returns {@code null} if {@code json} is {@code null}
   * or if {@code json} is empty.
   * @throws HillaJsonSyntaxException if json is not a valid representation for an object of type typeOfT
   * @since 1.3
   */
  public <T> T fromJson(HillaJsonElement json, Class<T> classOfT) throws HillaJsonSyntaxException {
    Object object = fromJson(json, (Type) classOfT);
    return HillaPrimitives.wrap(classOfT).cast(object);
  }

  /**
   * This method deserializes the Json read from the specified parse tree into an object of the
   * specified type. This method is useful if the specified object is a generic type. For
   * non-generic objects, use {@link #fromJson(HillaJsonElement, Class)} instead.
   *
   * @param <T> the type of the desired object
   * @param json the root of the parse tree of {@link HillaJsonElement}s from which the object is to
   * be deserialized
   * @param typeOfT The specific genericized type of src. You can obtain this type by using the
   * {@link HillaTypeToken} class. For example, to get the type for
   * {@code Collection<Foo>}, you should use:
   * <pre>
   * Type typeOfT = new HillaTypeToken&lt;Collection&lt;Foo&gt;&gt;(){}.getType();
   * </pre>
   * @return an object of type T from the json. Returns {@code null} if {@code json} is {@code null}
   * or if {@code json} is empty.
   * @throws HillaJsonSyntaxException if json is not a valid representation for an object of type typeOfT
   * @since 1.3
   */
  @SuppressWarnings("unchecked")
  public <T> T fromJson(HillaJsonElement json, Type typeOfT) throws HillaJsonSyntaxException {
    if (json == null) {
      return null;
    }
    return (T) fromJson(new HillaJsonTreeReader(json), typeOfT);
  }

  static class FutureTypeAdapter<T> extends HillaTypeAdapter<T> {
    private HillaTypeAdapter<T> delegate;

    public void setDelegate(HillaTypeAdapter<T> typeAdapter) {
      if (delegate != null) {
        throw new AssertionError();
      }
      delegate = typeAdapter;
    }

    @Override public T read(HillaJsonReader in) throws IOException {
      if (delegate == null) {
        throw new IllegalStateException();
      }
      return delegate.read(in);
    }

    @Override public void write(HillaJsonWriter out, T value) throws IOException {
      if (delegate == null) {
        throw new IllegalStateException();
      }
      delegate.write(out, value);
    }
  }

  @Override
  public String toString() {
    return new StringBuilder("{serializeNulls:")
        .append(serializeNulls)
        .append(",factories:").append(factories)
        .append(",instanceCreators:").append(constructorConstructor)
        .append("}")
        .toString();
  }
}
