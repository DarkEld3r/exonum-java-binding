/*
 * Copyright 2018 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.qaservice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that it might make sense to promote the annotated element
 * to the core Exonum Java Binding library.
 */
@Target({
    ElementType.TYPE,
    ElementType.METHOD
})
public @interface PromoteToCore {
  /**
   * Why the element might be promoted to the core library.
   */
  String value() default "";
}
