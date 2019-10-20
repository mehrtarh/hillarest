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

/**
 * Context for deserialization that is passed to a custom deserializer during invocation of its
 * {@link HillaJsonDeserializer#deserialize(HillaJsonElement, Type, HillaJsonDeserializationContext)}
 * method.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public interface HillaJsonDeserializationContext {

  /**
   * Invokes default deserialization on the specified object. It should never be invoked on
   * the element received as a parameter of the
   * {@link HillaJsonDeserializer#deserialize(HillaJsonElement, Type, HillaJsonDeserializationContext)} method. Doing
   * so will result in an infinite loop since HillaGson will in-turn call the custom deserializer again.
   *
   * @param json the parse tree.
   * @param typeOfT type of the expected return value.
   * @param <T> The type of the deserialized object.
   * @return An object of type typeOfT.
   * @throws HillaJsonParseException if the parse tree does not contain expected data.
   */
  public <T> T deserialize(HillaJsonElement json, Type typeOfT) throws HillaJsonParseException;
}