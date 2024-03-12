/*
Copyright 2022 WeAreFrank!

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.frankframework.doc;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If set on attribute setter or config child setter, forces attribute or config child to
 * be declared explicitly with the child. This annotation was introduced to modify the order
 * of appearance. Setting this application is only needed if the
 * inheriting setter does not update the attribute or config child in another way. For example
 * if the overriding method has a JavaDoc comment to update the description, then the element
 * child is repeated already without the need to add this annotation.
 * <p>
 * JavaDoc tag <code>@ff.reintroduce</code> has the same effect as this annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD})
@Inherited
public @interface Reintroduce {

}
