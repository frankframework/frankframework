/*
 * $Log: INamedObject.java,v $
 * Revision 1.4  2004-03-30 07:29:54  L190409
 * updated javadoc
 *
 * Revision 1.3  2004/03/26 10:42:45  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 */
package nl.nn.adapterframework.core;

/**
 * The <code>INamedObject</code> is implemented by all objects that have a name
 * 
 * @version Id
 * @author  Gerrit van Brakel
 */
public interface INamedObject {
		public static final String version="$Id: INamedObject.java,v 1.4 2004-03-30 07:29:54 L190409 Exp $";

	/**
	 * The functional name of the object implementing this interface
	 */
	public String getName();
	public void setName(String name);
}
