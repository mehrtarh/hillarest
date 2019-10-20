/*
 * Copyright (C) 2014 Google Inc.
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

package ir.hilla.rest.gson.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ir.hilla.rest.gson.HillaJsonDeserializer;
import ir.hilla.rest.gson.HillaJsonSerializer;
import ir.hilla.rest.gson.HillaGsonBuilder;
import ir.hilla.rest.gson.HillaTypeAdapter;
import ir.hilla.rest.gson.HillaTypeAdapterFactory;

/**
 * An annotation that indicates the HillaGson {@link HillaTypeAdapter} to use with a class
 * or field.
 *
 * <p>Here is an example of how this annotation is used:</p>
 * <pre>
 * &#64JsonAdapter(UserJsonAdapter.class)
 * public class User {
 *   public final String firstName, lastName;
 *   private User(String firstName, String lastName) {
 *     this.firstName = firstName;
 *     this.lastName = lastName;
 *   }
 * }
 * public class UserJsonAdapter extends HillaTypeAdapter&lt;User&gt; {
 *   &#64Override public void write(HillaJsonWriter out, User user) throws IOException {
 *     // implement write: combine firstName and lastName into name
 *     out.beginObject();
 *     out.name("name");
 *     out.value(user.firstName + " " + user.lastName);
 *     out.endObject();
 *     // implement the write method
 *   }
 *   &#64Override public User read(HillaJsonReader in) throws IOException {
 *     // implement read: split name into firstName and lastName
 *     in.beginObject();
 *     in.nextName();
 *     String[] nameParts = in.nextString().split(" ");
 *     in.endObject();
 *     return new User(nameParts[0], nameParts[1]);
 *   }
 * }
 * </pre>
 *
 * HillaSince User class specified UserJsonAdapter.class in &#64JsonAdapter annotation, it
 * will automatically be invoked to serialize/deserialize User instances. <br>
 *
 * <p> Here is an example of how to apply this annotation to a field.
 * <pre>
 * private static final class Gadget {
 *   &#64JsonAdapter(UserJsonAdapter2.class)
 *   final User user;
 *   Gadget(User user) {
 *     this.user = user;
 *   }
 * }
 * </pre>
 *
 * It's possible to specify different type adapters on a field, that
 * field's type, and in the {@link HillaGsonBuilder}. Field
 * annotations take precedence over {@code HillaGsonBuilder}-registered type
 * adapters, which in turn take precedence over annotated types.
 *
 * <p>The class referenced by this annotation must be either a {@link
 * HillaTypeAdapter} or a {@link HillaTypeAdapterFactory}, or must implement one
 * or both of {@link HillaJsonDeserializer} or {@link HillaJsonSerializer}.
 * Using {@link HillaTypeAdapterFactory} makes it possible to delegate
 * to the enclosing {@code HillaGson} instance.
 *
 * @since 2.3
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 * @author Jesse Wilson
 */
// Note that the above example is taken from AdaptAnnotationTest.
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface HillaJsonAdapter {

  /** Either a {@link HillaTypeAdapter} or {@link HillaTypeAdapterFactory}, or one or both of {@link HillaJsonDeserializer} or {@link HillaJsonSerializer}. */
  Class<?> value();

  /** false, to be able to handle {@code null} values within the adapter, default value is true. */
  boolean nullSafe() default true;

}
