/*
 * Copyright (C) 2017 The HillaGson authors
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
package ir.hilla.rest.gson.internal.reflect;

import java.lang.reflect.AccessibleObject;

/**
 * A basic implementation of {@link HillaReflectionAccessor} which is suitable for Java 8 and below.
 * <p>
 * This implementation just calls {@link AccessibleObject#setAccessible(boolean) setAccessible(true)}, which worked
 * fine before Java 9.
 */
final class HillaPreJava9ReflectionAccessor extends HillaReflectionAccessor {

  /** {@inheritDoc} */
  @Override
  public void makeAccessible(AccessibleObject ao) {
    ao.setAccessible(true);
  }
}
