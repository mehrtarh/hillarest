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
import java.sql.Time;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ir.hilla.rest.gson.HillaJsonSyntaxException;
import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;
import ir.hilla.rest.gson.reflect.HillaTypeToken;
import ir.hilla.rest.gson.stream.HillaJsonReader;
import ir.hilla.rest.gson.stream.HillaJsonToken;
import ir.hilla.rest.gson.stream.HillaJsonWriter;

/**
 * Adapter for Time. Although this class appears stateless, it is not.
 * DateFormat captures its time zone and locale when it is created, which gives
 * this class state. DateFormat isn't thread safe either, so this class has
 * to synchronize its read and write methods.
 */
public final class HillaTimeTypeAdapter extends HillaTypeAdapter<Time> {
  public static final HillaTypeAdapterFactory FACTORY = new HillaTypeAdapterFactory() {
    @SuppressWarnings("unchecked") // we use a runtime check to make sure the 'T's equal
    @Override public <T> HillaTypeAdapter<T> create(HillaGson gson, HillaTypeToken<T> typeToken) {
      return typeToken.getRawType() == Time.class ? (HillaTypeAdapter<T>) new HillaTimeTypeAdapter() : null;
    }
  };

  private final DateFormat format = new SimpleDateFormat("hh:mm:ss a");

  @Override public synchronized Time read(HillaJsonReader in) throws IOException {
    if (in.peek() == HillaJsonToken.NULL) {
      in.nextNull();
      return null;
    }
    try {
      Date date = format.parse(in.nextString());
      return new Time(date.getTime());
    } catch (ParseException e) {
      throw new HillaJsonSyntaxException(e);
    }
  }

  @Override public synchronized void write(HillaJsonWriter out, Time value) throws IOException {
    out.value(value == null ? null : format.format(value));
  }
}
