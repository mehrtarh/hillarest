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

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ir.hilla.rest.gson.internal.HillaJavaVersion;
import ir.hilla.rest.gson.internal.HillaPreJava9DateFormatProvider;
import ir.hilla.rest.gson.internal.bind.util.HillaISO8601Utils;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * This type adapter supports three subclasses of date: Date, Timestamp, and
 * java.sql.Date.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
final class HillaDefaultDateTypeAdapter extends HillaTypeAdapter<Date> {

  private static final String SIMPLE_NAME = "HillaDefaultDateTypeAdapter";

  private final Class<? extends Date> dateType;

  /**
   * List of 1 or more different date formats used for de-serialization attempts.
   * The first of them is used for serialization as well.
   */
  private final List<DateFormat> dateFormats = new ArrayList<DateFormat>();

  HillaDefaultDateTypeAdapter(Class<? extends Date> dateType) {
    this.dateType = verifyDateType(dateType);
    dateFormats.add(DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US));
    if (!Locale.getDefault().equals(Locale.US)) {
      dateFormats.add(DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT));
    }
    if (HillaJavaVersion.isJava9OrLater()) {
      dateFormats.add(HillaPreJava9DateFormatProvider.getUSDateTimeFormat(DateFormat.DEFAULT, DateFormat.DEFAULT));
    }
  }

  HillaDefaultDateTypeAdapter(Class<? extends Date> dateType, String datePattern) {
    this.dateType = verifyDateType(dateType);
    dateFormats.add(new SimpleDateFormat(datePattern, Locale.US));
    if (!Locale.getDefault().equals(Locale.US)) {
      dateFormats.add(new SimpleDateFormat(datePattern));
    }
  }

  HillaDefaultDateTypeAdapter(Class<? extends Date> dateType, int style) {
    this.dateType = verifyDateType(dateType);
    dateFormats.add(DateFormat.getDateInstance(style, Locale.US));
    if (!Locale.getDefault().equals(Locale.US)) {
      dateFormats.add(DateFormat.getDateInstance(style));
    }
    if (HillaJavaVersion.isJava9OrLater()) {
      dateFormats.add(HillaPreJava9DateFormatProvider.getUSDateFormat(style));
    }
  }

  public HillaDefaultDateTypeAdapter(int dateStyle, int timeStyle) {
    this(Date.class, dateStyle, timeStyle);
  }

  public HillaDefaultDateTypeAdapter(Class<? extends Date> dateType, int dateStyle, int timeStyle) {
    this.dateType = verifyDateType(dateType);
    dateFormats.add(DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.US));
    if (!Locale.getDefault().equals(Locale.US)) {
      dateFormats.add(DateFormat.getDateTimeInstance(dateStyle, timeStyle));
    }
    if (HillaJavaVersion.isJava9OrLater()) {
      dateFormats.add(HillaPreJava9DateFormatProvider.getUSDateTimeFormat(dateStyle, timeStyle));
    }
  }

  private static Class<? extends Date> verifyDateType(Class<? extends Date> dateType) {
    if ( dateType != Date.class && dateType != java.sql.Date.class && dateType != Timestamp.class ) {
      throw new IllegalArgumentException("Date type must be one of " + Date.class + ", " + Timestamp.class + ", or " + java.sql.Date.class + " but was " + dateType);
    }
    return dateType;
  }

  // These methods need to be synchronized since JDK DateFormat classes are not thread-safe
  // See issue 162
  @Override
  public void write(HillaJsonWriter out, Date value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    synchronized(dateFormats) {
      String dateFormatAsString = dateFormats.get(0).format(value);
      out.value(dateFormatAsString);
    }
  }

  @Override
  public Date read(HillaJsonReader in) throws IOException {
    if (in.peek() == HillaJsonToken.NULL) {
      in.nextNull();
      return null;
    }
    Date date = deserializeToDate(in.nextString());
    if (dateType == Date.class) {
      return date;
    } else if (dateType == Timestamp.class) {
      return new Timestamp(date.getTime());
    } else if (dateType == java.sql.Date.class) {
      return new java.sql.Date(date.getTime());
    } else {
      // This must never happen: dateType is guarded in the primary constructor
      throw new AssertionError();
    }
  }

  private Date deserializeToDate(String s) {
    synchronized (dateFormats) {
      for (DateFormat dateFormat : dateFormats) {
        try {
          return dateFormat.parse(s);
        } catch (ParseException ignored) {}
      }
      try {
        return HillaISO8601Utils.parse(s, new ParsePosition(0));
      } catch (ParseException e) {
        throw new HillaJsonSyntaxException(s, e);
      }
    }
  }

  @Override
  public String toString() {
    DateFormat defaultFormat = dateFormats.get(0);
    if (defaultFormat instanceof SimpleDateFormat) {
      return SIMPLE_NAME + '(' + ((SimpleDateFormat) defaultFormat).toPattern() + ')';
    } else {
      return SIMPLE_NAME + '(' + defaultFormat.getClass().getSimpleName() + ')';
    }
  }
}
