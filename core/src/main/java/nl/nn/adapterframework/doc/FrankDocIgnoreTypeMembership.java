package nl.nn.adapterframework.doc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * 
 * @author martijn
 *
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface FrankDocIgnoreTypeMembership {
	/**
	 * References a Java interface.
	 */
	String value();
}
