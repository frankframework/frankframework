package nl.nn.adapterframework.core;

/**
 * The <code>INamedObject</code> is implemented by all objects that have a name
 * 
 * <p>$Id: INamedObject.java,v 1.2 2004-02-04 10:01:58 a1909356#db2admin Exp $</p>
 */
public interface INamedObject {
		public static final String version="$Id: INamedObject.java,v 1.2 2004-02-04 10:01:58 a1909356#db2admin Exp $";

/**
 * The functional name of the object implementing this interface
 */
public String getName();
	public void setName(String name);
}
