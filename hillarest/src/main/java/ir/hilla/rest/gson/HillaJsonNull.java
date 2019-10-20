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

/**
 * A class representing a Json {@code null} value.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @since 1.2
 */
public final class HillaJsonNull extends HillaJsonElement {
  /**
   * singleton for HillaJsonNull
   *
   * @since 1.8
   */
  public static final HillaJsonNull INSTANCE = new HillaJsonNull();

  /**
   * Creates a new HillaJsonNull object.
   * Deprecated since HillaGson version 1.8. Use {@link #INSTANCE} instead
   */
  @Deprecated
  public HillaJsonNull() {
    // Do nothing
  }

  /**
   * Returns the same instance since it is an immutable value
   * @since 2.8.2
   */
  @Override
  public HillaJsonNull deepCopy() {
    return INSTANCE;
  }

  /**
   * All instances of HillaJsonNull have the same hash code since they are indistinguishable
   */
  @Override
  public int hashCode() {
    return HillaJsonNull.class.hashCode();
  }

  /**
   * All instances of HillaJsonNull are the same
   */
  @Override
  public boolean equals(Object other) {
    return this == other || other instanceof HillaJsonNull;
  }
}
