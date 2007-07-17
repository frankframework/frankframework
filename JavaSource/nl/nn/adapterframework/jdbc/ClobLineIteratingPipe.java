/*
 * $Log: ClobLineIteratingPipe.java,v $
 * Revision 1.2  2007-07-17 15:09:41  europe\L190409
 * improved error message
 *
 * Revision 1.1  2007/07/17 11:16:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added iterating classes
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.Reader;
import java.sql.ResultSet;
import java.sql.SQLException;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.ReaderLineIterator;

/**
 * Pipe that iterates over the lines in a clob.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public class ClobLineIteratingPipe extends IteratingPipeBase {


	protected IDataIterator getIterator(ResultSet rs) throws SenderException {
		try {
			if (!rs.next()) {
				throw new SenderException("query has empty resultset");
			}
			Reader clobReader;
			clobReader = rs.getCharacterStream(1);
			return new ReaderLineIterator(clobReader);
		} catch (SQLException e) {
			throw new SenderException(e);
		}
	}

}
