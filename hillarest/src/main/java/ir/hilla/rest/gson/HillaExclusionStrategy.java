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
 * A strategy (or policy) definition that is used to decide whether or not a field or top-level
 * class should be serialized or deserialized as part of the JSON output/input. For serialization,
 * if the {@link #shouldSkipClass(Class)} method returns true then that class or field type
 * will not be part of the JSON output. For deserialization, if {@link #shouldSkipClass(Class)}
 * returns true, then it will not be set as part of the Java object structure.
 *
 * <p>The following are a few examples that shows how you can use this exclusion mechanism.
 *
 * <p><strong>Exclude fields and objects based on a particular class type:</strong>
 * <pre class="code">
 * private static class SpecificClassExclusionStrategy implements HillaExclusionStrategy {
 *   private final Class&lt;?&gt; excludedThisClass;
 *
 *   public SpecificClassExclusionStrategy(Class&lt;?&gt; excludedThisClass) {
 *     this.excludedThisClass = excludedThisClass;
 *   }
 *
 *   public boolean shouldSkipClass(Class&lt;?&gt; clazz) {
 *     return excludedThisClass.equals(clazz);
 *   }
 *
 *   public boolean shouldSkipField(HillaFieldAttributes f) {
 *     return excludedThisClass.equals(f.getDeclaredClass());
 *   }
 * }
 * </pre>
 *
 * <p><strong>Excludes fields and objects based on a particular annotation:</strong>
 * <pre class="code">
 * public &#64interface FooAnnotation {
 *   // some implementation here
 * }
 *
 * // Excludes any field (or class) that is tagged with an "&#64FooAnnotation"
 * private static class FooAnnotationExclusionStrategy implements HillaExclusionStrategy {
 *   public boolean shouldSkipClass(Class&lt;?&gt; clazz) {
 *     return clazz.getAnnotation(FooAnnotation.class) != null;
 *   }
 *
 *   public boolean shouldSkipField(HillaFieldAttributes f) {
 *     return f.getAnnotation(FooAnnotation.class) != null;
 *   }
 * }
 * </pre>
 *
 * <p>Now if you want to configure {@code HillaGson} to use a user defined exclusion strategy, then
 * the {@code HillaGsonBuilder} is required. The following is an example of how you can use the
 * {@code HillaGsonBuilder} to configure HillaGson to use one of the above sample:
 * <pre class="code">
 * HillaExclusionStrategy excludeStrings = new UserDefinedExclusionStrategy(String.class);
 * HillaGson gson = new HillaGsonBuilder()
 *     .setExclusionStrategies(excludeStrings)
 *     .create();
 * </pre>
 *
 * <p>For certain model classes, you may only want to serialize a field, but exclude it for
 * deserialization. To do that, you can write an {@code HillaExclusionStrategy} as per normal;
 * however, you would register it with the
 * {@link HillaGsonBuilder#addDeserializationExclusionStrategy(HillaExclusionStrategy)} method.
 * For example:
 * <pre class="code">
 * HillaExclusionStrategy excludeStrings = new UserDefinedExclusionStrategy(String.class);
 * HillaGson gson = new HillaGsonBuilder()
 *     .addDeserializationExclusionStrategy(excludeStrings)
 *     .create();
 * </pre>
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 *
 * @see HillaGsonBuilder#setExclusionStrategies(HillaExclusionStrategy...)
 * @see HillaGsonBuilder#addDeserializationExclusionStrategy(HillaExclusionStrategy)
 * @see HillaGsonBuilder#addSerializationExclusionStrategy(HillaExclusionStrategy)
 *
 * @since 1.4
 */
public interface HillaExclusionStrategy {

  /**
   * @param f the field object that is under test
   * @return true if the field should be ignored; otherwise false
   */
  public boolean shouldSkipField(HillaFieldAttributes f);

  /**
   * @param clazz the class object that is under test
   * @return true if the class should be ignored; otherwise false
   */
  public boolean shouldSkipClass(Class<?> clazz);
}
