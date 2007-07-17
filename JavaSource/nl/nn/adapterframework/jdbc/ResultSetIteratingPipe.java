/*
 * $Log: ResultSetIteratingPipe.java,v $
 * Revision 1.1  2007-07-17 11:16:49  europe\L190409
 * added iterating classes
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.SenderException;

/**
 * Pipe that iterates of rows in in ResultSet.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class ResultSetIteratingPipe extends IteratingPipeBase {

	protected IDataIterator getIterator(ResultSet rs) throws SenderException {
		try {
			return new ResultSetIterator(rs);
		} catch (SQLException e) {
			throw new SenderException(e);
		}
	}

}
