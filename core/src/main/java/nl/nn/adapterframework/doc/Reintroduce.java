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
package nl.nn.adapterframework.doc;

import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If set on attribute setter or config child setter, forces attribute or config child to
 * be declared explicitly with the child. This annotation was introduced to modify the sort
 * order of config children. Setting this application is only needed if the
 * inheriting config child setter does not update the config child in another way. For example
 * if the overriding method has a JavaDoc comment to update the description, then the config
 * child is repeated already without the need to add this annotation.
 *
 * Although it is technically possible to apply this annotation on attributes, doing so does
 * not make sense because XML schema does not enforce the order in which attributes appear
 * in a Frank configuration.
 * 
 * This annotation has no impact on the Frank!Doc webapp. WeAreFrank! decided so because
 * repeated config children would look confusing. If an overriding config child setter
 * modifies the config child in another way then with this annotation (e.g other documentation),
 * then the config child is repeated in the Frank!Doc webapp with the overriding class.
 * 
 * JavaDoc tag <code>@ff.reintroduce</code> has the same effect as this annotation.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD})
@Inherited
public @interface Reintroduce {

}
