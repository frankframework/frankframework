/*
 * $Log: JdbcException.java,v $
 * Revision 1.1  2004-03-24 13:28:20  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import nl.nn.adapterframework.core.IbisException;

/**
 * Wrapper for JDBC related exceptions.
 * 
 * <p>$Id: JdbcException.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $</p>
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class JdbcException extends IbisException {
	public static final String version="$Id: JdbcException.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $";

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
