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

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ir.hilla.rest.gson.annotations.HillaExpose;
import ir.hilla.rest.gson.internal.$HillaGson$Preconditions;
import ir.hilla.rest.gson.internal.HillaExcluder;
import ir.hilla.rest.gson.internal.bind.HillaTreeTypeAdapter;
import ir.hilla.rest.gson.internal.bind.HillaTypeAdapters;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;

import static ir.hilla.rest.gson.HillaGson.DEFAULT_COMPLEX_MAP_KEYS;
import static ir.hilla.rest.gson.HillaGson.DEFAULT_ESCAPE_HTML;
import static ir.hilla.rest.gson.HillaGson.DEFAULT_JSON_NON_EXECUTABLE;
import static ir.hilla.rest.gson.HillaGson.DEFAULT_LENIENT;
import static ir.hilla.rest.gson.HillaGson.DEFAULT_PRETTY_PRINT;
import static ir.hilla.rest.gson.HillaGson.DEFAULT_SERIALIZE_NULLS;
import static ir.hilla.rest.gson.HillaGson.DEFAULT_SPECIALIZE_FLOAT_VALUES;

/**
 * <p>Use this builder to construct a {@link HillaGson} instance when you need to set configuration
 * options other than the default. For {@link HillaGson} with default configuration, it is simpler to
 * use {@code new HillaGson()}. {@code HillaGsonBuilder} is best used by creating it, and then invoking its
 * various configuration methods, and finally calling create.</p>
 *
 * <p>The following is an example shows how to use the {@code HillaGsonBuilder} to construct a HillaGson
 * instance:
 *
 * <pre>
 * HillaGson gson = new HillaGsonBuilder()
 *     .registerTypeAdapter(Id.class, new IdTypeAdapter())
 *     .enableComplexMapKeySerialization()
 *     .serializeNulls()
 *     .setDateFormat(DateFormat.LONG)
 *     .setFieldNamingPolicy(HillaFieldNamingPolicy.UPPER_CAMEL_CASE)
 *     .setPrettyPrinting()
 *     .setVersion(1.0)
 *     .create();
 * </pre></p>
 *
 * <p>NOTES:
 * <ul>
 * <li> the order of invocation of configuration methods does not matter.</li>
 * <li> The default serialization of {@link Date} and its subclasses in HillaGson does
 *  not contain time-zone information. So, if you are using date/time instances,
 *  use {@code HillaGsonBuilder} and its {@code setDateFormat} methods.</li>
 *  </ul>
 * </p>
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @author Jesse Wilson
 */
public final class HillaGsonBuilder {
  private HillaExcluder excluder = HillaExcluder.DEFAULT;
  private HillaLongSerializationPolicy longSerializationPolicy = HillaLongSerializationPolicy.DEFAULT;
  private HillaFieldNamingStrategy fieldNamingPolicy = HillaFieldNamingPolicy.IDENTITY;
  private final Map<Type, HillaInstanceCreator<?>> instanceCreators
      = new HashMap<Type, HillaInstanceCreator<?>>();
  private final List<HillaTypeAdapterFactory> factories = new ArrayList<HillaTypeAdapterFactory>();
  /** tree-style hierarchy factories. These come after factories for backwards compatibility. */
  private final List<HillaTypeAdapterFactory> hierarchyFactories = new ArrayList<HillaTypeAdapterFactory>();
  private boolean serializeNulls = DEFAULT_SERIALIZE_NULLS;
  private String datePattern;
  private int dateStyle = DateFormat.DEFAULT;
  private int timeStyle = DateFormat.DEFAULT;
  private boolean complexMapKeySerialization = DEFAULT_COMPLEX_MAP_KEYS;
  private boolean serializeSpecialFloatingPointValues = DEFAULT_SPECIALIZE_FLOAT_VALUES;
  private boolean escapeHtmlChars = DEFAULT_ESCAPE_HTML;
  private boolean prettyPrinting = DEFAULT_PRETTY_PRINT;
  private boolean generateNonExecutableJson = DEFAULT_JSON_NON_EXECUTABLE;
  private boolean lenient = DEFAULT_LENIENT;

  /**
   * Creates a HillaGsonBuilder instance that can be used to build HillaGson with various configuration
   * settings. HillaGsonBuilder follows the builder pattern, and it is typically used by first
   * invoking various configuration methods to set desired options, and finally calling
   * {@link #create()}.
   */
  public HillaGsonBuilder() {
  }

  /**
   * Constructs a HillaGsonBuilder instance from a HillaGson instance. The newly constructed HillaGsonBuilder
   * has the same configuration as the previously built HillaGson instance.
   *
   * @param gson the gson instance whose configuration should by applied to a new HillaGsonBuilder.
   */
  HillaGsonBuilder(HillaGson gson) {
    this.excluder = gson.excluder;
    this.fieldNamingPolicy = gson.fieldNamingStrategy;
    this.instanceCreators.putAll(gson.instanceCreators);
    this.serializeNulls = gson.serializeNulls;
    this.complexMapKeySerialization = gson.complexMapKeySerialization;
    this.generateNonExecutableJson = gson.generateNonExecutableJson;
    this.escapeHtmlChars = gson.htmlSafe;
    this.prettyPrinting = gson.prettyPrinting;
    this.lenient = gson.lenient;
    this.serializeSpecialFloatingPointValues = gson.serializeSpecialFloatingPointValues;
    this.longSerializationPolicy = gson.longSerializationPolicy;
    this.datePattern = gson.datePattern;
    this.dateStyle = gson.dateStyle;
    this.timeStyle = gson.timeStyle;
    this.factories.addAll(gson.builderFactories);
    this.hierarchyFactories.addAll(gson.builderHierarchyFactories);
  }

  /**
   * Configures HillaGson to enable versioning support.
   *
   * @param ignoreVersionsAfter any field or type marked with a version higher than this value
   * are ignored during serialization or deserialization.
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   */
  public HillaGsonBuilder setVersion(double ignoreVersionsAfter) {
    excluder = excluder.withVersion(ignoreVersionsAfter);
    return this;
  }

  /**
   * Configures HillaGson to excludes all class fields that have the specified modifiers. By default,
   * HillaGson will exclude all fields marked transient or static. This method will override that
   * behavior.
   *
   * @param modifiers the field modifiers. You must use the modifiers specified in the
   * {@link java.lang.reflect.Modifier} class. For example,
   * {@link java.lang.reflect.Modifier#TRANSIENT},
   * {@link java.lang.reflect.Modifier#STATIC}.
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   */
  public HillaGsonBuilder excludeFieldsWithModifiers(int... modifiers) {
    excluder = excluder.withModifiers(modifiers);
    return this;
  }

  /**
   * Makes the output JSON non-executable in Javascript by prefixing the generated JSON with some
   * special text. This prevents attacks from third-party sites through script sourcing. See
   * <a href="http://code.google.com/p/google-gson/issues/detail?id=42">HillaGson Issue 42</a>
   * for details.
   *
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.3
   */
  public HillaGsonBuilder generateNonExecutableJson() {
    this.generateNonExecutableJson = true;
    return this;
  }

  /**
   * Configures HillaGson to exclude all fields from consideration for serialization or deserialization
   * that do not have the {@link HillaExpose} annotation.
   *
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   */
  public HillaGsonBuilder excludeFieldsWithoutExposeAnnotation() {
    excluder = excluder.excludeFieldsWithoutExposeAnnotation();
    return this;
  }

  /**
   * Configure HillaGson to serialize null fields. By default, HillaGson omits all fields that are null
   * during serialization.
   *
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.2
   */
  public HillaGsonBuilder serializeNulls() {
    this.serializeNulls = true;
    return this;
  }

  /**
   * Enabling this feature will only change the serialized form if the map key is
   * a complex type (i.e. non-primitive) in its <strong>serialized</strong> JSON
   * form. The default implementation of map serialization uses {@code toString()}
   * on the key; however, when this is called then one of the following cases
   * apply:
   *
   * <h3>Maps as JSON objects</h3>
   * For this case, assume that a type adapter is registered to serialize and
   * deserialize some {@code Point} class, which contains an x and y coordinate,
   * to/from the JSON Primitive string value {@code "(x,y)"}. The Java map would
   * then be serialized as a {@link HillaJsonObject}.
   *
   * <p>Below is an example:
   * <pre>  {@code
   *   HillaGson gson = new HillaGsonBuilder()
   *       .register(Point.class, new MyPointTypeAdapter())
   *       .enableComplexMapKeySerialization()
   *       .create();
   *
   *   Map<Point, String> original = new LinkedHashMap<Point, String>();
   *   original.put(new Point(5, 6), "a");
   *   original.put(new Point(8, 8), "b");
   *   System.out.println(gson.toJson(original, type));
   * }</pre>
   * The above code prints this JSON object:<pre>  {@code
   *   {
   *     "(5,6)": "a",
   *     "(8,8)": "b"
   *   }
   * }</pre>
   *
   * <h3>Maps as JSON arrays</h3>
   * For this case, assume that a type adapter was NOT registered for some
   * {@code Point} class, but rather the default HillaGson serialization is applied.
   * In this case, some {@code new Point(2,3)} would serialize as {@code
   * {"x":2,"y":5}}.
   *
   * <p>Given the assumption above, a {@code Map<Point, String>} will be
   * serialize as an array of arrays (can be viewed as an entry set of pairs).
   *
   * <p>Below is an example of serializing complex types as JSON arrays:
   * <pre> {@code
   *   HillaGson gson = new HillaGsonBuilder()
   *       .enableComplexMapKeySerialization()
   *       .create();
   *
   *   Map<Point, String> original = new LinkedHashMap<Point, String>();
   *   original.put(new Point(5, 6), "a");
   *   original.put(new Point(8, 8), "b");
   *   System.out.println(gson.toJson(original, type));
   * }
   *
   * The JSON output would look as follows:
   * <pre>   {@code
   *   [
   *     [
   *       {
   *         "x": 5,
   *         "y": 6
   *       },
   *       "a"
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
   *
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.7
   */
  public HillaGsonBuilder enableComplexMapKeySerialization() {
    complexMapKeySerialization = true;
    return this;
  }

  /**
   * Configures HillaGson to exclude inner classes during serialization.
   *
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.3
   */
  public HillaGsonBuilder disableInnerClassSerialization() {
    excluder = excluder.disableInnerClassSerialization();
    return this;
  }

  /**
   * Configures HillaGson to apply a specific serialization policy for {@code Long} and {@code long}
   * objects.
   *
   * @param serializationPolicy the particular policy to use for serializing longs.
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.3
   */
  public HillaGsonBuilder setLongSerializationPolicy(HillaLongSerializationPolicy serializationPolicy) {
    this.longSerializationPolicy = serializationPolicy;
    return this;
  }

  /**
   * Configures HillaGson to apply a specific naming policy to an object's field during serialization
   * and deserialization.
   *
   * @param namingConvention the JSON field naming convention to use for serialization and
   * deserialization.
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   */
  public HillaGsonBuilder setFieldNamingPolicy(HillaFieldNamingPolicy namingConvention) {
    this.fieldNamingPolicy = namingConvention;
    return this;
  }

  /**
   * Configures HillaGson to apply a specific naming policy strategy to an object's field during
   * serialization and deserialization.
   *
   * @param fieldNamingStrategy the actual naming strategy to apply to the fields
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.3
   */
  public HillaGsonBuilder setFieldNamingStrategy(HillaFieldNamingStrategy fieldNamingStrategy) {
    this.fieldNamingPolicy = fieldNamingStrategy;
    return this;
  }

  /**
   * Configures HillaGson to apply a set of exclusion strategies during both serialization and
   * deserialization. Each of the {@code strategies} will be applied as a disjunction rule.
   * This means that if one of the {@code strategies} suggests that a field (or class) should be
   * skipped then that field (or object) is skipped during serialization/deserialization.
   *
   * @param strategies the set of strategy object to apply during object (de)serialization.
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.4
   */
  public HillaGsonBuilder setExclusionStrategies(HillaExclusionStrategy... strategies) {
    for (HillaExclusionStrategy strategy : strategies) {
      excluder = excluder.withExclusionStrategy(strategy, true, true);
    }
    return this;
  }

  /**
   * Configures HillaGson to apply the passed in exclusion strategy during serialization.
   * If this method is invoked numerous times with different exclusion strategy objects
   * then the exclusion strategies that were added will be applied as a disjunction rule.
   * This means that if one of the added exclusion strategies suggests that a field (or
   * class) should be skipped then that field (or object) is skipped during its
   * serialization.
   *
   * @param strategy an exclusion strategy to apply during serialization.
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.7
   */
  public HillaGsonBuilder addSerializationExclusionStrategy(HillaExclusionStrategy strategy) {
    excluder = excluder.withExclusionStrategy(strategy, true, false);
    return this;
  }

  /**
   * Configures HillaGson to apply the passed in exclusion strategy during deserialization.
   * If this method is invoked numerous times with different exclusion strategy objects
   * then the exclusion strategies that were added will be applied as a disjunction rule.
   * This means that if one of the added exclusion strategies suggests that a field (or
   * class) should be skipped then that field (or object) is skipped during its
   * deserialization.
   *
   * @param strategy an exclusion strategy to apply during deserialization.
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.7
   */
  public HillaGsonBuilder addDeserializationExclusionStrategy(HillaExclusionStrategy strategy) {
    excluder = excluder.withExclusionStrategy(strategy, false, true);
    return this;
  }

  /**
   * Configures HillaGson to output Json that fits in a page for pretty printing. This option only
   * affects Json serialization.
   *
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   */
  public HillaGsonBuilder setPrettyPrinting() {
    prettyPrinting = true;
    return this;
  }

  /**
   * By default, HillaGson is strict and only accepts JSON as specified by
   * <a href="http://www.ietf.org/rfc/rfc4627.txt">RFC 4627</a>. This option makes the parser
   * liberal in what it accepts.
   *
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @see HillaJsonReader#setLenient(boolean)
   */
  public HillaGsonBuilder setLenient() {
    lenient = true;
    return this;
  }

  /**
   * By default, HillaGson escapes HTML characters such as &lt; &gt; etc. Use this option to configure
   * HillaGson to pass-through HTML characters as is.
   *
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.3
   */
  public HillaGsonBuilder disableHtmlEscaping() {
    this.escapeHtmlChars = false;
    return this;
  }

  /**
   * Configures HillaGson to serialize {@code Date} objects according to the pattern provided. You can
   * call this method or {@link #setDateFormat(int)} multiple times, but only the last invocation
   * will be used to decide the serialization format.
   *
   * <p>The date format will be used to serialize and deserialize {@link Date}, {@link
   * Timestamp} and {@link java.sql.Date}.
   *
   * <p>Note that this pattern must abide by the convention provided by {@code SimpleDateFormat}
   * class. See the documentation in {@link java.text.SimpleDateFormat} for more information on
   * valid date and time patterns.</p>
   *
   * @param pattern the pattern that dates will be serialized/deserialized to/from
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.2
   */
  public HillaGsonBuilder setDateFormat(String pattern) {
    // TODO(Joel): Make this fail fast if it is an invalid date format
    this.datePattern = pattern;
    return this;
  }

  /**
   * Configures HillaGson to to serialize {@code Date} objects according to the style value provided.
   * You can call this method or {@link #setDateFormat(String)} multiple times, but only the last
   * invocation will be used to decide the serialization format.
   *
   * <p>Note that this style value should be one of the predefined constants in the
   * {@code DateFormat} class. See the documentation in {@link DateFormat} for more
   * information on the valid style constants.</p>
   *
   * @param style the predefined date style that date objects will be serialized/deserialized
   * to/from
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.2
   */
  public HillaGsonBuilder setDateFormat(int style) {
    this.dateStyle = style;
    this.datePattern = null;
    return this;
  }

  /**
   * Configures HillaGson to to serialize {@code Date} objects according to the style value provided.
   * You can call this method or {@link #setDateFormat(String)} multiple times, but only the last
   * invocation will be used to decide the serialization format.
   *
   * <p>Note that this style value should be one of the predefined constants in the
   * {@code DateFormat} class. See the documentation in {@link DateFormat} for more
   * information on the valid style constants.</p>
   *
   * @param dateStyle the predefined date style that date objects will be serialized/deserialized
   * to/from
   * @param timeStyle the predefined style for the time portion of the date objects
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.2
   */
  public HillaGsonBuilder setDateFormat(int dateStyle, int timeStyle) {
    this.dateStyle = dateStyle;
    this.timeStyle = timeStyle;
    this.datePattern = null;
    return this;
  }

  /**
   * Configures HillaGson for custom serialization or deserialization. This method combines the
   * registration of an {@link HillaTypeAdapter}, {@link HillaInstanceCreator}, {@link HillaJsonSerializer}, and a
   * {@link HillaJsonDeserializer}. It is best used when a single object {@code typeAdapter} implements
   * all the required interfaces for custom serialization with HillaGson. If a type adapter was
   * previously registered for the specified {@code type}, it is overwritten.
   *
   * <p>This registers the type specified and no other types: you must manually register related
   * types! For example, applications registering {@code boolean.class} should also register {@code
   * Boolean.class}.
   *
   * @param type the type definition for the type adapter being registered
   * @param typeAdapter This object must implement at least one of the {@link HillaTypeAdapter},
   * {@link HillaInstanceCreator}, {@link HillaJsonSerializer}, and a {@link HillaJsonDeserializer} interfaces.
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public HillaGsonBuilder registerTypeAdapter(Type type, Object typeAdapter) {
    $HillaGson$Preconditions.checkArgument(typeAdapter instanceof HillaJsonSerializer<?>
        || typeAdapter instanceof HillaJsonDeserializer<?>
        || typeAdapter instanceof HillaInstanceCreator<?>
        || typeAdapter instanceof HillaTypeAdapter<?>);
    if (typeAdapter instanceof HillaInstanceCreator<?>) {
      instanceCreators.put(type, (HillaInstanceCreator) typeAdapter);
    }
    if (typeAdapter instanceof HillaJsonSerializer<?> || typeAdapter instanceof HillaJsonDeserializer<?>) {
      HillaTypeToken<?> typeToken = HillaTypeToken.get(type);
      factories.add(HillaTreeTypeAdapter.newFactoryWithMatchRawType(typeToken, typeAdapter));
    }
    if (typeAdapter instanceof HillaTypeAdapter<?>) {
      factories.add(HillaTypeAdapters.newFactory(HillaTypeToken.get(type), (HillaTypeAdapter)typeAdapter));
    }
    return this;
  }

  /**
   * Register a factory for type adapters. Registering a factory is useful when the type
   * adapter needs to be configured based on the type of the field being processed. HillaGson
   * is designed to handle a large number of factories, so you should consider registering
   * them to be at par with registering an individual type adapter.
   *
   * @since 2.1
   */
  public HillaGsonBuilder registerTypeAdapterFactory(HillaTypeAdapterFactory factory) {
    factories.add(factory);
    return this;
  }

  /**
   * Configures HillaGson for custom serialization or deserialization for an inheritance type hierarchy.
   * This method combines the registration of a {@link HillaTypeAdapter}, {@link HillaJsonSerializer} and
   * a {@link HillaJsonDeserializer}. If a type adapter was previously registered for the specified
   * type hierarchy, it is overridden. If a type adapter is registered for a specific type in
   * the type hierarchy, it will be invoked instead of the one registered for the type hierarchy.
   *
   * @param baseType the class definition for the type adapter being registered for the base class
   *        or interface
   * @param typeAdapter This object must implement at least one of {@link HillaTypeAdapter},
   *        {@link HillaJsonSerializer} or {@link HillaJsonDeserializer} interfaces.
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.7
   */
  @SuppressWarnings({"unchecked", "rawtypes"})
  public HillaGsonBuilder registerTypeHierarchyAdapter(Class<?> baseType, Object typeAdapter) {
    $HillaGson$Preconditions.checkArgument(typeAdapter instanceof HillaJsonSerializer<?>
        || typeAdapter instanceof HillaJsonDeserializer<?>
        || typeAdapter instanceof HillaTypeAdapter<?>);
    if (typeAdapter instanceof HillaJsonDeserializer || typeAdapter instanceof HillaJsonSerializer) {
      hierarchyFactories.add(HillaTreeTypeAdapter.newTypeHierarchyFactory(baseType, typeAdapter));
    }
    if (typeAdapter instanceof HillaTypeAdapter<?>) {
      factories.add(HillaTypeAdapters.newTypeHierarchyFactory(baseType, (HillaTypeAdapter)typeAdapter));
    }
    return this;
  }

  /**
   * Section 2.4 of <a href="http://www.ietf.org/rfc/rfc4627.txt">JSON specification</a> disallows
   * special double values (NaN, Infinity, -Infinity). However,
   * <a href="http://www.ecma-international.org/publications/files/ECMA-ST/Ecma-262.pdf">Javascript
   * specification</a> (see section 4.3.20, 4.3.22, 4.3.23) allows these values as valid Javascript
   * values. Moreover, most JavaScript engines will accept these special values in JSON without
   * problem. So, at a practical level, it makes sense to accept these values as valid JSON even
   * though JSON specification disallows them.
   *
   * <p>HillaGson always accepts these special values during deserialization. However, it outputs
   * strictly compliant JSON. Hence, if it encounters a float value {@link Float#NaN},
   * {@link Float#POSITIVE_INFINITY}, {@link Float#NEGATIVE_INFINITY}, or a double value
   * {@link Double#NaN}, {@link Double#POSITIVE_INFINITY}, {@link Double#NEGATIVE_INFINITY}, it
   * will throw an {@link IllegalArgumentException}. This method provides a way to override the
   * default behavior when you know that the JSON receiver will be able to handle these special
   * values.
   *
   * @return a reference to this {@code HillaGsonBuilder} object to fulfill the "Builder" pattern
   * @since 1.3
   */
  public HillaGsonBuilder serializeSpecialFloatingPointValues() {
    this.serializeSpecialFloatingPointValues = true;
    return this;
  }

  /**
   * Creates a {@link HillaGson} instance based on the current configuration. This method is free of
   * side-effects to this {@code HillaGsonBuilder} instance and hence can be called multiple times.
   *
   * @return an instance of HillaGson configured with the options currently set in this builder
   */
  public HillaGson create() {
    List<HillaTypeAdapterFactory> factories = new ArrayList<HillaTypeAdapterFactory>(this.factories.size() + this.hierarchyFactories.size() + 3);
    factories.addAll(this.factories);
    Collections.reverse(factories);

    List<HillaTypeAdapterFactory> hierarchyFactories = new ArrayList<HillaTypeAdapterFactory>(this.hierarchyFactories);
    Collections.reverse(hierarchyFactories);
    factories.addAll(hierarchyFactories);

    addTypeAdaptersForDate(datePattern, dateStyle, timeStyle, factories);

    return new HillaGson(excluder, fieldNamingPolicy, instanceCreators,
        serializeNulls, complexMapKeySerialization,
        generateNonExecutableJson, escapeHtmlChars, prettyPrinting, lenient,
        serializeSpecialFloatingPointValues, longSerializationPolicy,
        datePattern, dateStyle, timeStyle,
        this.factories, this.hierarchyFactories, factories);
  }

  @SuppressWarnings("unchecked")
  private void addTypeAdaptersForDate(String datePattern, int dateStyle, int timeStyle,
      List<HillaTypeAdapterFactory> factories) {
    HillaDefaultDateTypeAdapter dateTypeAdapter;
    HillaTypeAdapter<Timestamp> timestampTypeAdapter;
    HillaTypeAdapter<java.sql.Date> javaSqlDateTypeAdapter;
    if (datePattern != null && !"".equals(datePattern.trim())) {
      dateTypeAdapter = new HillaDefaultDateTypeAdapter(Date.class, datePattern);
      timestampTypeAdapter = (HillaTypeAdapter) new HillaDefaultDateTypeAdapter(Timestamp.class, datePattern);
      javaSqlDateTypeAdapter = (HillaTypeAdapter) new HillaDefaultDateTypeAdapter(java.sql.Date.class, datePattern);
    } else if (dateStyle != DateFormat.DEFAULT && timeStyle != DateFormat.DEFAULT) {
      dateTypeAdapter = new HillaDefaultDateTypeAdapter(Date.class, dateStyle, timeStyle);
      timestampTypeAdapter = (HillaTypeAdapter) new HillaDefaultDateTypeAdapter(Timestamp.class, dateStyle, timeStyle);
      javaSqlDateTypeAdapter = (HillaTypeAdapter) new HillaDefaultDateTypeAdapter(java.sql.Date.class, dateStyle, timeStyle);
    } else {
      return;
    }

    factories.add(HillaTypeAdapters.newFactory(Date.class, dateTypeAdapter));
    factories.add(HillaTypeAdapters.newFactory(Timestamp.class, timestampTypeAdapter));
    factories.add(HillaTypeAdapters.newFactory(java.sql.Date.class, javaSqlDateTypeAdapter));
  }
}
