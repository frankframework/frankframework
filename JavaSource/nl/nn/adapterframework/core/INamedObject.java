/**
 * $Log: INamedObject.java,v $
 * Revision 1.3  2004-03-26 10:42:45  NNVZNL01#L180564
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * The <code>INamedObject</code> is implemented by all objects that have a name
 * 
 * @version Id
 */
public interface INamedObject {
		public static final String version="$Id: INamedObject.java,v 1.3 2004-03-26 10:42:45 NNVZNL01#L180564 Exp $";

/**
 * The functional name of the object implementing this interface
 */
public String getName();
	public void setName(String name);
}
