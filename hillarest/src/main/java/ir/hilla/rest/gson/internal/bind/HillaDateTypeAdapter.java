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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ir.hilla.rest.gson.HillaJsonSyntaxException;
import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;
import ir.hilla.rest.gson.internal.HillaJavaVersion;
import ir.hilla.rest.gson.internal.HillaPreJava9DateFormatProvider;
import ir.hilla.rest.gson.internal.bind.util.HillaISO8601Utils;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * Adapter for Date. Although this class appears stateless, it is not.
 * DateFormat captures its time zone and locale when it is created, which gives
 * this class state. DateFormat isn't thread safe either, so this class has
 * to synchronize its read and write methods.
 */
public final class HillaDateTypeAdapter extends HillaTypeAdapter<Date> {
  public static final HillaTypeAdapterFactory FACTORY = new HillaTypeAdapterFactory() {
    @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
    @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
      return typeToken.getRawType() == Date.class ? (HillaTypeAdapter<T>) new HillaDateTypeAdapter() : null;
    }
  };

  /**
   * List of 1 or more different date formats used for de-serialization attempts.
   * The first of them (default US format) is used for serialization as well.
   */
  private final List<DateFormat> dateFormats = new ArrayList<DateFormat>();

  public HillaDateTypeAdapter() {
    dateFormats.add(DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, Locale.US));
    if (!Locale.getDefault().equals(Locale.US)) {
      dateFormats.add(DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT));
    }
    if (HillaJavaVersion.isJava9OrLater()) {
      dateFormats.add(HillaPreJava9DateFormatProvider.getUSDateTimeFormat(DateFormat.DEFAULT, DateFormat.DEFAULT));
    }
  }

  @Override public Date read(HillaJsonReader in) throws IOException {
    if (in.peek() == HillaJsonToken.NULL) {
      in.nextNull();
      return null;
    }
    return deserializeToDate(in.nextString());
  }

  private synchronized Date deserializeToDate(String json) {
    for (DateFormat dateFormat : dateFormats) {
      try {
        return dateFormat.parse(json);
      } catch (ParseException ignored) {}
    }
    try {
    	return HillaISO8601Utils.parse(json, new ParsePosition(0));
    } catch (ParseException e) {
      throw new HillaJsonSyntaxException(json, e);
    }
  }

  @Override public synchronized void write(HillaJsonWriter out, Date value) throws IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    String dateFormatAsString = dateFormats.get(0).format(value);
    out.value(dateFormatAsString);
  }
  
  
}
