/*
 * $Log: IQueryParameterParser.java,v $
 * Revision 1.4  2004-10-19 08:12:32  L190409
 * made obsolete with introduction of generic parameter handling
 *
 * Revision 1.3  2004/04/08 16:03:51  Dennis van Loon <dennis.van.loon@ibissource.org>
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
 * @deprecated This class has become obsolete with the arrival of generic {@link nl.nn.adapterframework.parameters.Parameter parameters}.
 */
public interface IQueryParameterParser {
	public static final String version = "$Id: IQueryParameterParser.java,v 1.4 2004-10-19 08:12:32 L190409 Exp $";

	/**
	 * parse message and set parameters found to the statement in stmt. 
	 */
	public abstract PreparedStatement parse(PreparedStatement stmt, String correlationID, String message)
		throws JdbcException;
}