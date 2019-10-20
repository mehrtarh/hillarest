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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Currency;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import ir.hilla.rest.gson.HillaJsonArray;
import ir.hilla.rest.gson.HillaJsonElement;
import ir.hilla.rest.gson.HillaJsonIOException;
import ir.hilla.rest.gson.HillaJsonNull;
import ir.hilla.rest.gson.HillaJsonObject;
import ir.hilla.rest.gson.HillaJsonPrimitive;
import ir.hilla.rest.gson.HillaJsonSyntaxException;
import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;
import ir.hilla.rest.gson.annotations.HillaSerializedName;
import ir.hilla.rest.gson.internal.HillaLazilyParsedNumber;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * Type adapters for basic types.
 */
public final class HillaTypeAdapters {
  private HillaTypeAdapters() {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("rawtypes")
  public static final HillaTypeAdapter<Class> CLASS = new HillaTypeAdapter<Class>() {
    @Override
    public void write(HillaJsonWriter out, Class value) throws IOException {
      throw new UnsupportedOperationException("Attempted to serialize java.lang.Class: "
              + value.getName() + ". Forgot to register a type adapter?");
    }
    @Override
    public Class read(HillaJsonReader in) throws IOException {
      throw new UnsupportedOperationException(
              "Attempted to deserialize a java.lang.Class. Forgot to register a type adapter?");
    }
  }.nullSafe();

  public static final HillaTypeAdapterFactory CLASS_FACTORY = newFactory(Class.class, CLASS);

  public static final HillaTypeAdapter<BitSet> BIT_SET = new HillaTypeAdapter<BitSet>() {
    @Override public BitSet read(HillaJsonReader in) throws IOException {
      BitSet bitset = new BitSet();
      in.beginArray();
      int i = 0;
      HillaJsonToken tokenType = in.peek();
      while (tokenType != HillaJsonToken.END_ARRAY) {
        boolean set;
        switch (tokenType) {
        case NUMBER:
          set = in.nextInt() != 0;
          break;
        case BOOLEAN:
          set = in.nextBoolean();
          break;
        case STRING:
          String stringValue = in.nextString();
          try {
            set = Integer.parseInt(stringValue) != 0;
          } catch (NumberFormatException e) {
            throw new HillaJsonSyntaxException(
                "Error: Expecting: bitset number value (1, 0), Found: " + stringValue);
          }
          break;
        default:
          throw new HillaJsonSyntaxException("Invalid bitset value type: " + tokenType);
        }
        if (set) {
          bitset.set(i);
        }
        ++i;
        tokenType = in.peek();
      }
      in.endArray();
      return bitset;
    }

    @Override public void write(HillaJsonWriter out, BitSet src) throws IOException {
      out.beginArray();
      for (int i = 0, length = src.length(); i < length; i++) {
        int value = (src.get(i)) ? 1 : 0;
        out.value(value);
      }
      out.endArray();
    }
  }.nullSafe();

  public static final HillaTypeAdapterFactory BIT_SET_FACTORY = newFactory(BitSet.class, BIT_SET);

  public static final HillaTypeAdapter<Boolean> BOOLEAN = new HillaTypeAdapter<Boolean>() {
    @Override
    public Boolean read(HillaJsonReader in) throws IOException {
      HillaJsonToken peek = in.peek();
      if (peek == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      } else if (peek == HillaJsonToken.STRING) {
        // support strings for compatibility with GSON 1.7
        return Boolean.parseBoolean(in.nextString());
      }
      return in.nextBoolean();
    }
    @Override
    public void write(HillaJsonWriter out, Boolean value) throws IOException {
      out.value(value);
    }
  };

  /**
   * Writes a boolean as a string. Useful for map keys, where booleans aren't
   * otherwise permitted.
   */
  public static final HillaTypeAdapter<Boolean> BOOLEAN_AS_STRING = new HillaTypeAdapter<Boolean>() {
    @Override public Boolean read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return Boolean.valueOf(in.nextString());
    }

    @Override public void write(HillaJsonWriter out, Boolean value) throws IOException {
      out.value(value == null ? "null" : value.toString());
    }
  };

  public static final HillaTypeAdapterFactory BOOLEAN_FACTORY
      = newFactory(boolean.class, Boolean.class, BOOLEAN);

  public static final HillaTypeAdapter<Number> BYTE = new HillaTypeAdapter<Number>() {
    @Override
    public Number read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      try {
        int intValue = in.nextInt();
        return (byte) intValue;
      } catch (NumberFormatException e) {
        throw new HillaJsonSyntaxException(e);
      }
    }
    @Override
    public void write(HillaJsonWriter out, Number value) throws IOException {
      out.value(value);
    }
  };

  public static final HillaTypeAdapterFactory BYTE_FACTORY
      = newFactory(byte.class, Byte.class, BYTE);

  public static final HillaTypeAdapter<Number> SHORT = new HillaTypeAdapter<Number>() {
    @Override
    public Number read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      try {
        return (short) in.nextInt();
      } catch (NumberFormatException e) {
        throw new HillaJsonSyntaxException(e);
      }
    }
    @Override
    public void write(HillaJsonWriter out, Number value) throws IOException {
      out.value(value);
    }
  };

  public static final HillaTypeAdapterFactory SHORT_FACTORY
      = newFactory(short.class, Short.class, SHORT);

  public static final HillaTypeAdapter<Number> INTEGER = new HillaTypeAdapter<Number>() {
    @Override
    public Number read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      try {
        return in.nextInt();
      } catch (NumberFormatException e) {
        throw new HillaJsonSyntaxException(e);
      }
    }
    @Override
    public void write(HillaJsonWriter out, Number value) throws IOException {
      out.value(value);
    }
  };
  public static final HillaTypeAdapterFactory INTEGER_FACTORY
      = newFactory(int.class, Integer.class, INTEGER);

  public static final HillaTypeAdapter<AtomicInteger> ATOMIC_INTEGER = new HillaTypeAdapter<AtomicInteger>() {
    @Override public AtomicInteger read(HillaJsonReader in) throws IOException {
      try {
        return new AtomicInteger(in.nextInt());
      } catch (NumberFormatException e) {
        throw new HillaJsonSyntaxException(e);
      }
    }
    @Override public void write(HillaJsonWriter out, AtomicInteger value) throws IOException {
      out.value(value.get());
    }
  }.nullSafe();
  public static final HillaTypeAdapterFactory ATOMIC_INTEGER_FACTORY =
      newFactory(AtomicInteger.class, HillaTypeAdapters.ATOMIC_INTEGER);

  public static final HillaTypeAdapter<AtomicBoolean> ATOMIC_BOOLEAN = new HillaTypeAdapter<AtomicBoolean>() {
    @Override public AtomicBoolean read(HillaJsonReader in) throws IOException {
      return new AtomicBoolean(in.nextBoolean());
    }
    @Override public void write(HillaJsonWriter out, AtomicBoolean value) throws IOException {
      out.value(value.get());
    }
  }.nullSafe();
  public static final HillaTypeAdapterFactory ATOMIC_BOOLEAN_FACTORY =
      newFactory(AtomicBoolean.class, HillaTypeAdapters.ATOMIC_BOOLEAN);

  public static final HillaTypeAdapter<AtomicIntegerArray> ATOMIC_INTEGER_ARRAY = new HillaTypeAdapter<AtomicIntegerArray>() {
    @Override public AtomicIntegerArray read(HillaJsonReader in) throws IOException {
        List<Integer> list = new ArrayList<Integer>();
        in.beginArray();
        while (in.hasNext()) {
          try {
            int integer = in.nextInt();
            list.add(integer);
          } catch (NumberFormatException e) {
            throw new HillaJsonSyntaxException(e);
          }
        }
        in.endArray();
        int length = list.size();
        AtomicIntegerArray array = new AtomicIntegerArray(length);
        for (int i = 0; i < length; ++i) {
          array.set(i, list.get(i));
        }
        return array;
    }
    @Override public void write(HillaJsonWriter out, AtomicIntegerArray value) throws IOException {
      out.beginArray();
      for (int i = 0, length = value.length(); i < length; i++) {
        out.value(value.get(i));
      }
      out.endArray();
    }
  }.nullSafe();
  public static final HillaTypeAdapterFactory ATOMIC_INTEGER_ARRAY_FACTORY =
      newFactory(AtomicIntegerArray.class, HillaTypeAdapters.ATOMIC_INTEGER_ARRAY);

  public static final HillaTypeAdapter<Number> LONG = new HillaTypeAdapter<Number>() {
    @Override
    public Number read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      try {
        return in.nextLong();
      } catch (NumberFormatException e) {
        throw new HillaJsonSyntaxException(e);
      }
    }
    @Override
    public void write(HillaJsonWriter out, Number value) throws IOException {
      out.value(value);
    }
  };

  public static final HillaTypeAdapter<Number> FLOAT = new HillaTypeAdapter<Number>() {
    @Override
    public Number read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return (float) in.nextDouble();
    }
    @Override
    public void write(HillaJsonWriter out, Number value) throws IOException {
      out.value(value);
    }
  };

  public static final HillaTypeAdapter<Number> DOUBLE = new HillaTypeAdapter<Number>() {
    @Override
    public Number read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return in.nextDouble();
    }
    @Override
    public void write(HillaJsonWriter out, Number value) throws IOException {
      out.value(value);
    }
  };

  public static final HillaTypeAdapter<Number> NUMBER = new HillaTypeAdapter<Number>() {
    @Override
    public Number read(HillaJsonReader in) throws IOException {
      HillaJsonToken jsonToken = in.peek();
      switch (jsonToken) {
      case NULL:
        in.nextNull();
        return null;
      case NUMBER:
      case STRING:
        return new HillaLazilyParsedNumber(in.nextString());
      default:
        throw new HillaJsonSyntaxException("Expecting number, got: " + jsonToken);
      }
    }
    @Override
    public void write(HillaJsonWriter out, Number value) throws IOException {
      out.value(value);
    }
  };

  public static final HillaTypeAdapterFactory NUMBER_FACTORY = newFactory(Number.class, NUMBER);

  public static final HillaTypeAdapter<Character> CHARACTER = new HillaTypeAdapter<Character>() {
    @Override
    public Character read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      String str = in.nextString();
      if (str.length() != 1) {
        throw new HillaJsonSyntaxException("Expecting character, got: " + str);
      }
      return str.charAt(0);
    }
    @Override
    public void write(HillaJsonWriter out, Character value) throws IOException {
      out.value(value == null ? null : String.valueOf(value));
    }
  };

  public static final HillaTypeAdapterFactory CHARACTER_FACTORY
      = newFactory(char.class, Character.class, CHARACTER);

  public static final HillaTypeAdapter<String> STRING = new HillaTypeAdapter<String>() {
    @Override
    public String read(HillaJsonReader in) throws IOException {
      HillaJsonToken peek = in.peek();
      if (peek == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      /* coerce booleans to strings for backwards compatibility */
      if (peek == HillaJsonToken.BOOLEAN) {
        return Boolean.toString(in.nextBoolean());
      }
      return in.nextString();
    }
    @Override
    public void write(HillaJsonWriter out, String value) throws IOException {
      out.value(value);
    }
  };
  
  public static final HillaTypeAdapter<BigDecimal> BIG_DECIMAL = new HillaTypeAdapter<BigDecimal>() {
    @Override public BigDecimal read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      try {
        return new BigDecimal(in.nextString());
      } catch (NumberFormatException e) {
        throw new HillaJsonSyntaxException(e);
      }
    }

    @Override public void write(HillaJsonWriter out, BigDecimal value) throws IOException {
      out.value(value);
    }
  };
  
  public static final HillaTypeAdapter<BigInteger> BIG_INTEGER = new HillaTypeAdapter<BigInteger>() {
    @Override public BigInteger read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      try {
        return new BigInteger(in.nextString());
      } catch (NumberFormatException e) {
        throw new HillaJsonSyntaxException(e);
      }
    }

    @Override public void write(HillaJsonWriter out, BigInteger value) throws IOException {
      out.value(value);
    }
  };

  public static final HillaTypeAdapterFactory STRING_FACTORY = newFactory(String.class, STRING);

  public static final HillaTypeAdapter<StringBuilder> STRING_BUILDER = new HillaTypeAdapter<StringBuilder>() {
    @Override
    public StringBuilder read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return new StringBuilder(in.nextString());
    }
    @Override
    public void write(HillaJsonWriter out, StringBuilder value) throws IOException {
      out.value(value == null ? null : value.toString());
    }
  };

  public static final HillaTypeAdapterFactory STRING_BUILDER_FACTORY =
    newFactory(StringBuilder.class, STRING_BUILDER);

  public static final HillaTypeAdapter<StringBuffer> STRING_BUFFER = new HillaTypeAdapter<StringBuffer>() {
    @Override
    public StringBuffer read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return new StringBuffer(in.nextString());
    }
    @Override
    public void write(HillaJsonWriter out, StringBuffer value) throws IOException {
      out.value(value == null ? null : value.toString());
    }
  };

  public static final HillaTypeAdapterFactory STRING_BUFFER_FACTORY =
    newFactory(StringBuffer.class, STRING_BUFFER);

  public static final HillaTypeAdapter<URL> URL = new HillaTypeAdapter<URL>() {
    @Override
    public URL read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      String nextString = in.nextString();
      return "null".equals(nextString) ? null : new URL(nextString);
    }
    @Override
    public void write(HillaJsonWriter out, URL value) throws IOException {
      out.value(value == null ? null : value.toExternalForm());
    }
  };

  public static final HillaTypeAdapterFactory URL_FACTORY = newFactory(URL.class, URL);

  public static final HillaTypeAdapter<URI> URI = new HillaTypeAdapter<URI>() {
    @Override
    public URI read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      try {
        String nextString = in.nextString();
        return "null".equals(nextString) ? null : new URI(nextString);
      } catch (URISyntaxException e) {
        throw new HillaJsonIOException(e);
      }
    }
    @Override
    public void write(HillaJsonWriter out, URI value) throws IOException {
      out.value(value == null ? null : value.toASCIIString());
    }
  };

  public static final HillaTypeAdapterFactory URI_FACTORY = newFactory(URI.class, URI);

  public static final HillaTypeAdapter<InetAddress> INET_ADDRESS = new HillaTypeAdapter<InetAddress>() {
    @Override
    public InetAddress read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      // regrettably, this should have included both the host name and the host address
      return InetAddress.getByName(in.nextString());
    }
    @Override
    public void write(HillaJsonWriter out, InetAddress value) throws IOException {
      out.value(value == null ? null : value.getHostAddress());
    }
  };

  public static final HillaTypeAdapterFactory INET_ADDRESS_FACTORY =
    newTypeHierarchyFactory(InetAddress.class, INET_ADDRESS);

  public static final HillaTypeAdapter<UUID> UUID = new HillaTypeAdapter<UUID>() {
    @Override
    public UUID read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return java.util.UUID.fromString(in.nextString());
    }
    @Override
    public void write(HillaJsonWriter out, UUID value) throws IOException {
      out.value(value == null ? null : value.toString());
    }
  };

  public static final HillaTypeAdapterFactory UUID_FACTORY = newFactory(UUID.class, UUID);

  public static final HillaTypeAdapter<Currency> CURRENCY = new HillaTypeAdapter<Currency>() {
    @Override
    public Currency read(HillaJsonReader in) throws IOException {
      return Currency.getInstance(in.nextString());
    }
    @Override
    public void write(HillaJsonWriter out, Currency value) throws IOException {
      out.value(value.getCurrencyCode());
    }
  }.nullSafe();
  public static final HillaTypeAdapterFactory CURRENCY_FACTORY = newFactory(Currency.class, CURRENCY);

  public static final HillaTypeAdapterFactory TIMESTAMP_FACTORY = new HillaTypeAdapterFactory() {
    @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
    @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
      if (typeToken.getRawType() != Timestamp.class) {
        return null;
      }

      final HillaTypeAdapter<Date> dateTypeAdapter = gson.getAdapter(Date.class);
      return (HillaTypeAdapter<T>) new HillaTypeAdapter<Timestamp>() {
        @Override public Timestamp read(HillaJsonReader in) throws IOException {
          Date date = dateTypeAdapter.read(in);
          return date != null ? new Timestamp(date.getTime()) : null;
        }

        @Override public void write(HillaJsonWriter out, Timestamp value) throws IOException {
          dateTypeAdapter.write(out, value);
        }
      };
    }
  };

  public static final HillaTypeAdapter<Calendar> CALENDAR = new HillaTypeAdapter<Calendar>() {
    private static final String YEAR = "year";
    private static final String MONTH = "month";
    private static final String DAY_OF_MONTH = "dayOfMonth";
    private static final String HOUR_OF_DAY = "hourOfDay";
    private static final String MINUTE = "minute";
    private static final String SECOND = "second";

    @Override
    public Calendar read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return  null;
      }
      in.beginObject();
      int year = 0;
      int month = 0;
      int dayOfMonth = 0;
      int hourOfDay = 0;
      int minute = 0;
      int second = 0;
      while (in.peek() != HillaJsonToken.END_OBJECT) {
        String name = in.nextName();
        int value = in.nextInt();
        if (YEAR.equals(name)) {
          year = value;
        } else if (MONTH.equals(name)) {
          month = value;
        } else if (DAY_OF_MONTH.equals(name)) {
          dayOfMonth = value;
        } else if (HOUR_OF_DAY.equals(name)) {
          hourOfDay = value;
        } else if (MINUTE.equals(name)) {
          minute = value;
        } else if (SECOND.equals(name)) {
          second = value;
        }
      }
      in.endObject();
      return new GregorianCalendar(year, month, dayOfMonth, hourOfDay, minute, second);
    }

    @Override
    public void write(HillaJsonWriter out, Calendar value) throws IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.beginObject();
      out.name(YEAR);
      out.value(value.get(Calendar.YEAR));
      out.name(MONTH);
      out.value(value.get(Calendar.MONTH));
      out.name(DAY_OF_MONTH);
      out.value(value.get(Calendar.DAY_OF_MONTH));
      out.name(HOUR_OF_DAY);
      out.value(value.get(Calendar.HOUR_OF_DAY));
      out.name(MINUTE);
      out.value(value.get(Calendar.MINUTE));
      out.name(SECOND);
      out.value(value.get(Calendar.SECOND));
      out.endObject();
    }
  };

  public static final HillaTypeAdapterFactory CALENDAR_FACTORY =
    newFactoryForMultipleTypes(Calendar.class, GregorianCalendar.class, CALENDAR);

  public static final HillaTypeAdapter<Locale> LOCALE = new HillaTypeAdapter<Locale>() {
    @Override
    public Locale read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      String locale = in.nextString();
      StringTokenizer tokenizer = new StringTokenizer(locale, "_");
      String language = null;
      String country = null;
      String variant = null;
      if (tokenizer.hasMoreElements()) {
        language = tokenizer.nextToken();
      }
      if (tokenizer.hasMoreElements()) {
        country = tokenizer.nextToken();
      }
      if (tokenizer.hasMoreElements()) {
        variant = tokenizer.nextToken();
      }
      if (country == null && variant == null) {
        return new Locale(language);
      } else if (variant == null) {
        return new Locale(language, country);
      } else {
        return new Locale(language, country, variant);
      }
    }
    @Override
    public void write(HillaJsonWriter out, Locale value) throws IOException {
      out.value(value == null ? null : value.toString());
    }
  };

  public static final HillaTypeAdapterFactory LOCALE_FACTORY = newFactory(Locale.class, LOCALE);

  public static final HillaTypeAdapter<HillaJsonElement> JSON_ELEMENT = new HillaTypeAdapter<HillaJsonElement>() {
    @Override public HillaJsonElement read(HillaJsonReader in) throws IOException {
      switch (in.peek()) {
      case STRING:
        return new HillaJsonPrimitive(in.nextString());
      case NUMBER:
        String number = in.nextString();
        return new HillaJsonPrimitive(new HillaLazilyParsedNumber(number));
      case BOOLEAN:
        return new HillaJsonPrimitive(in.nextBoolean());
      case NULL:
        in.nextNull();
        return HillaJsonNull.INSTANCE;
      case BEGIN_ARRAY:
        HillaJsonArray array = new HillaJsonArray();
        in.beginArray();
        while (in.hasNext()) {
          array.add(read(in));
        }
        in.endArray();
        return array;
      case BEGIN_OBJECT:
        HillaJsonObject object = new HillaJsonObject();
        in.beginObject();
        while (in.hasNext()) {
          object.add(in.nextName(), read(in));
        }
        in.endObject();
        return object;
      case END_DOCUMENT:
      case NAME:
      case END_OBJECT:
      case END_ARRAY:
      default:
        throw new IllegalArgumentException();
      }
    }

    @Override public void write(HillaJsonWriter out, HillaJsonElement value) throws IOException {
      if (value == null || value.isJsonNull()) {
        out.nullValue();
      } else if (value.isJsonPrimitive()) {
        HillaJsonPrimitive primitive = value.getAsJsonPrimitive();
        if (primitive.isNumber()) {
          out.value(primitive.getAsNumber());
        } else if (primitive.isBoolean()) {
          out.value(primitive.getAsBoolean());
        } else {
          out.value(primitive.getAsString());
        }

      } else if (value.isJsonArray()) {
        out.beginArray();
        for (HillaJsonElement e : value.getAsJsonArray()) {
          write(out, e);
        }
        out.endArray();

      } else if (value.isJsonObject()) {
        out.beginObject();
        for (Map.Entry<String, HillaJsonElement> e : value.getAsJsonObject().entrySet()) {
          out.name(e.getKey());
          write(out, e.getValue());
        }
        out.endObject();

      } else {
        throw new IllegalArgumentException("Couldn't write " + value.getClass());
      }
    }
  };

  public static final HillaTypeAdapterFactory JSON_ELEMENT_FACTORY
      = newTypeHierarchyFactory(HillaJsonElement.class, JSON_ELEMENT);

  private static final class EnumTypeAdapter<T extends Enum<T>> extends HillaTypeAdapter<T> {
    private final Map<String, T> nameToConstant = new HashMap<String, T>();
    private final Map<T, String> constantToName = new HashMap<T, String>();

    public EnumTypeAdapter(Class<T> classOfT) {
      try {
        for (T constant : classOfT.getEnumConstants()) {
          String name = constant.name();
          HillaSerializedName annotation = classOfT.getField(name).getAnnotation(HillaSerializedName.class);
          if (annotation != null) {
            name = annotation.value();
            for (String alternate : annotation.alternate()) {
              nameToConstant.put(alternate, constant);
            }
          }
          nameToConstant.put(name, constant);
          constantToName.put(constant, name);
        }
      } catch (NoSuchFieldException e) {
        throw new AssertionError(e);
      }
    }
    @Override public T read(HillaJsonReader in) throws IOException {
      if (in.peek() == HillaJsonToken.NULL) {
        in.nextNull();
        return null;
      }
      return nameToConstant.get(in.nextString());
    }

    @Override public void write(HillaJsonWriter out, T value) throws IOException {
      out.value(value == null ? null : constantToName.get(value));
    }
  }

  public static final HillaTypeAdapterFactory ENUM_FACTORY = new HillaTypeAdapterFactory() {
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
      Class<? super T> rawType = typeToken.getRawType();
      if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class) {
        return null;
      }
      if (!rawType.isEnum()) {
        rawType = rawType.getSuperclass(); // handle anonymous subclasses
      }
      return (HillaTypeAdapter<T>) new EnumTypeAdapter(rawType);
    }
  };

  public static <TT> HillaTypeAdapterFactory newFactory(
          final HillaTypeToken<TT> type, final HillaTypeAdapter<TT> typeAdapter) {
    return new HillaTypeAdapterFactory() {
      @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
      @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
        return typeToken.equals(type) ? (HillaTypeAdapter<T>) typeAdapter : null;
      }
    };
  }

  public static <TT> HillaTypeAdapterFactory newFactory(
      final Class<TT> type, final HillaTypeAdapter<TT> typeAdapter) {
    return new HillaTypeAdapterFactory() {
      @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
      @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
        return typeToken.getRawType() == type ? (HillaTypeAdapter<T>) typeAdapter : null;
      }
      @Override public String toString() {
        return "Factory[type=" + type.getName() + ",adapter=" + typeAdapter + "]";
      }
    };
  }

  public static <TT> HillaTypeAdapterFactory newFactory(
      final Class<TT> unboxed, final Class<TT> boxed, final HillaTypeAdapter<? super TT> typeAdapter) {
    return new HillaTypeAdapterFactory() {
      @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
      @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
        Class<? super T> rawType = typeToken.getRawType();
        return (rawType == unboxed || rawType == boxed) ? (HillaTypeAdapter<T>) typeAdapter : null;
      }
      @Override public String toString() {
        return "Factory[type=" + boxed.getName()
            + "+" + unboxed.getName() + ",adapter=" + typeAdapter + "]";
      }
    };
  }

  public static <TT> HillaTypeAdapterFactory newFactoryForMultipleTypes(final Class<TT> base,
                                                                        final Class<? extends TT> sub, final HillaTypeAdapter<? super TT> typeAdapter) {
    return new HillaTypeAdapterFactory() {
      @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
      @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
        Class<? super T> rawType = typeToken.getRawType();
        return (rawType == base || rawType == sub) ? (HillaTypeAdapter<T>) typeAdapter : null;
      }
      @Override public String toString() {
        return "Factory[type=" + base.getName()
            + "+" + sub.getName() + ",adapter=" + typeAdapter + "]";
      }
    };
  }

  /**
   * Returns a factory for all subtypes of {@code typeAdapter}. We do a runtime check to confirm
   * that the deserialized type matches the type requested.
   */
  public static <T1> HillaTypeAdapterFactory newTypeHierarchyFactory(
      final Class<T1> clazz, final HillaTypeAdapter<T1> typeAdapter) {
    return new HillaTypeAdapterFactory() {
      @SuppressWarnings("unchecked")
      @Override public <T2> HillaTypeAdapter<T2> create(HillaGson gson, HillaTypeToken<T2> typeToken) {
        final Class<? super T2> requestedType = typeToken.getRawType();
        if (!clazz.isAssignableFrom(requestedType)) {
          return null;
        }
        return (HillaTypeAdapter<T2>) new HillaTypeAdapter<T1>() {
          @Override public void write(HillaJsonWriter out, T1 value) throws IOException {
            typeAdapter.write(out, value);
          }

          @Override public T1 read(HillaJsonReader in) throws IOException {
            T1 result = typeAdapter.read(in);
            if (result != null && !requestedType.isInstance(result)) {
              throw new HillaJsonSyntaxException("Expected a " + requestedType.getName()
                  + " but was " + result.getClass().getName());
            }
            return result;
          }
        };
      }
      @Override public String toString() {
        return "Factory[typeHierarchy=" + clazz.getName() + ",adapter=" + typeAdapter + "]";
      }
    };
  }
}