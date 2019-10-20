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

import java.util.Map;
import java.util.Set;

import ir.hilla.rest.gson.internal.HillaLinkedTreeMap;

/**
 * A class representing an object type in Json. An object consists of name-value pairs where names
 * are strings, and values are any other type of {@link HillaJsonElement}. This allows for a creating a
 * tree of JsonElements. The member elements of this object are maintained in order they were added.
 *
 * @author Inderjeet Singh
 * @author Joel Leitch
 */
public final class HillaJsonObject extends HillaJsonElement {
  private final HillaLinkedTreeMap<String, HillaJsonElement> members =
      new HillaLinkedTreeMap<String, HillaJsonElement>();

  /**
   * Creates a deep copy of this element and all its children
   * @since 2.8.2
   */
  @Override
  public HillaJsonObject deepCopy() {
    HillaJsonObject result = new HillaJsonObject();
    for (Map.Entry<String, HillaJsonElement> entry : members.entrySet()) {
      result.add(entry.getKey(), entry.getValue().deepCopy());
    }
    return result;
  }

  /**
   * Adds a member, which is a name-value pair, to self. The name must be a String, but the value
   * can be an arbitrary HillaJsonElement, thereby allowing you to build a full tree of JsonElements
   * rooted at this node.
   *
   * @param property name of the member.
   * @param value the member object.
   */
  public void add(String property, HillaJsonElement value) {
    members.put(property, value == null ? HillaJsonNull.INSTANCE : value);
  }

  /**
   * Removes the {@code property} from this {@link HillaJsonObject}.
   *
   * @param property name of the member that should be removed.
   * @return the {@link HillaJsonElement} object that is being removed.
   * @since 1.3
   */
  public HillaJsonElement remove(String property) {
    return members.remove(property);
  }

  /**
   * Convenience method to add a primitive member. The specified value is converted to a
   * HillaJsonPrimitive of String.
   *
   * @param property name of the member.
   * @param value the string value associated with the member.
   */
  public void addProperty(String property, String value) {
    add(property, value == null ? HillaJsonNull.INSTANCE : new HillaJsonPrimitive(value));
  }

  /**
   * Convenience method to add a primitive member. The specified value is converted to a
   * HillaJsonPrimitive of Number.
   *
   * @param property name of the member.
   * @param value the number value associated with the member.
   */
  public void addProperty(String property, Number value) {
    add(property, value == null ? HillaJsonNull.INSTANCE : new HillaJsonPrimitive(value));
  }

  /**
   * Convenience method to add a boolean member. The specified value is converted to a
   * HillaJsonPrimitive of Boolean.
   *
   * @param property name of the member.
   * @param value the number value associated with the member.
   */
  public void addProperty(String property, Boolean value) {
    add(property, value == null ? HillaJsonNull.INSTANCE : new HillaJsonPrimitive(value));
  }

  /**
   * Convenience method to add a char member. The specified value is converted to a
   * HillaJsonPrimitive of Character.
   *
   * @param property name of the member.
   * @param value the number value associated with the member.
   */
  public void addProperty(String property, Character value) {
    add(property, value == null ? HillaJsonNull.INSTANCE : new HillaJsonPrimitive(value));
  }

  /**
   * Returns a set of members of this object. The set is ordered, and the order is in which the
   * elements were added.
   *
   * @return a set of members of this object.
   */
  public Set<Map.Entry<String, HillaJsonElement>> entrySet() {
    return members.entrySet();
  }

  /**
   * Returns a set of members key values.
   *
   * @return a set of member keys as Strings
   * @since 2.8.1
   */
  public Set<String> keySet() {
    return members.keySet();
  }

  /**
   * Returns the number of key/value pairs in the object.
   *
   * @return the number of key/value pairs in the object.
   */
  public int size() {
    return members.size();
  }

  /**
   * Convenience method to check if a member with the specified name is present in this object.
   *
   * @param memberName name of the member that is being checked for presence.
   * @return true if there is a member with the specified name, false otherwise.
   */
  public boolean has(String memberName) {
    return members.containsKey(memberName);
  }

  /**
   * Returns the member with the specified name.
   *
   * @param memberName name of the member that is being requested.
   * @return the member matching the name. Null if no such member exists.
   */
  public HillaJsonElement get(String memberName) {
    return members.get(memberName);
  }

  /**
   * Convenience method to get the specified member as a HillaJsonPrimitive element.
   *
   * @param memberName name of the member being requested.
   * @return the HillaJsonPrimitive corresponding to the specified member.
   */
  public HillaJsonPrimitive getAsJsonPrimitive(String memberName) {
    return (HillaJsonPrimitive) members.get(memberName);
  }

  /**
   * Convenience method to get the specified member as a HillaJsonArray.
   *
   * @param memberName name of the member being requested.
   * @return the HillaJsonArray corresponding to the specified member.
   */
  public HillaJsonArray getAsJsonArray(String memberName) {
    return (HillaJsonArray) members.get(memberName);
  }

  /**
   * Convenience method to get the specified member as a HillaJsonObject.
   *
   * @param memberName name of the member being requested.
   * @return the HillaJsonObject corresponding to the specified member.
   */
  public HillaJsonObject getAsJsonObject(String memberName) {
    return (HillaJsonObject) members.get(memberName);
  }

  @Override
  public boolean equals(Object o) {
    return (o == this) || (o instanceof HillaJsonObject
        && ((HillaJsonObject) o).members.equals(members));
  }

  @Override
  public int hashCode() {
    return members.hashCode();
  }
}
