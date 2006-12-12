/*
 * $Log: JdbcException.java,v $
 * Revision 1.4  2006-12-12 09:57:35  europe\L190409
 * restore jdbc package
 *
 * Revision 1.2  2004/03/26 10:43:07  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.1  2004/03/24 13:28:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import nl.nn.adapterframework.core.IbisException;

/**
 * Wrapper for JDBC related exceptions.
 * 
 * @version Id
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class JdbcException extends IbisException {
	public static final String version="$Id: JdbcException.java,v 1.4 2006-12-12 09:57:35 europe\L190409 Exp $";

	public JdbcException() {
		super();
	}

	public JdbcException(String arg1) {
		super(arg1);
	}

	public JdbcException(String arg1, Throwable arg2) {
		super(arg1, arg2);
	}

	public JdbcException(Throwable arg1) {
		super(arg1);
	}

}
