/* 
Copyright 2021 WeAreFrank! 

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

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.jdbc.MessageStoreSender;

/**
 * Classes annotated with this interface are treaded as follows by the Frank!Doc:
 * <ul>
 * <li> Attributes in the referenced Java interface are not included and they are rejected from
 * inheritance, unless the attribute is also defined from an interface or class that does not inherit
 * from the referenced Java interface.
 * <li> If the referenced Java interface has a FrankDocGroup annotation, then this annotation
 * influences the groups in the Frank!Doc website. The group in the FrankDocGroup annotation
 * is reduced by the annotated class and the derived classes of the annotated class. These
 * classes are supposed to belong to a group that comes from another implemented Java interface. 
 * </ul>
 * 
 * Example: Class {@link MessageStoreSender} extends {@link JdbcTransactionalStorage} which implements
 * {@link ITransactionalStorage}. {@link MessageStoreSender} also implements {@link ISender}.
 * Class {@link MessageStoreSender} should not produce configurable attributes by inheritance from
 * {@link ITransactionalStorage}, also not if there are attribute setter methods in {@link JdbcTransactionalStorage}.
 * But if attribute setters are duplicate in {@link MessageStoreSender} and {@link ISender}, then
 * we want those attributes.
 * <p>
 * Both {@link ITransactionalStorage} and {@link ISender} have a FrankDocGroup annotation to specify
 * the group in the Frank!Doc website. The Frank!Doc website works with a hierarchy of groups
 * that contain types that contain elements. This annotation removes only for the Frank!Doc
 * website {@link MessageStoreSender} and its derived classes from the type {@link ITransactionalStorage}.
 * <p>
 * Please note that you can re-introduce attributes lower in the class inheritance hierarchy if this
 * annotation is applied on a higher level to exclude an attribute. The reason is that omitting
 * attributes is only done on the class that is annotated with this annotation. A derived class that
 * is not annotated is not analyzed for attributes to be omitted.
 * 
 * @author martijn
 *
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface FrankDocIgnoreTypeMembership {
	/**
	 * References a Java interface.
	 */
	String value();
}
