/*
 * $Log: IQueryParameterParser.java,v $
 * Revision 1.3  2004-04-08 16:03:51  nnvznl01#l181303
 * Parse method now gives back a PreparedStatement
 *
 * Revision 1.2  2004/03/26 10:43:07  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.1  2004/03/24 13:28:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.PreparedStatement;

/**
 * Definition of classes that are able to set parameters from messages to statements.
 * @version Id
 * @author Gerrit van Brakel
 * @since  4.1
 */
public interface IQueryParameterParser {
	public static final String version = "$Id: IQueryParameterParser.java,v 1.3 2004-04-08 16:03:51 nnvznl01#l181303 Exp $";

	/**
	 * parse message and set parameters found to the statement in stmt. 
	 */
	public abstract PreparedStatement parse(PreparedStatement stmt, String correlationID, String message)
		throws JdbcException;
}