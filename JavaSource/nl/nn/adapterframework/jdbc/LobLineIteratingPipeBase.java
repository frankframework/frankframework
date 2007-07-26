/*
 * $Log: LobLineIteratingPipeBase.java,v $
 * Revision 1.1  2007-07-26 16:15:28  europe\L190409
 * first version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.Reader;
import java.sql.ResultSet;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.ReaderLineIterator;

/**
 * abstract baseclass for Pipes that iterate over the lines in a lob.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
public abstract class LobLineIteratingPipeBase extends IteratingPipeBase {

	protected abstract Reader getReader(ResultSet rs) throws SenderException;

	protected class ResultStreamIterator extends ReaderLineIterator {
		ResultSet rs;
		
		ResultStreamIterator(ResultSet rs, Reader reader) throws SenderException {
			super(reader);
			this.rs=rs;
		}
		
		public void close() throws SenderException {
			try {
				super.close();
			} finally {
				JdbcUtil.fullClose(rs);
			}
		}

	}

	protected IDataIterator getIterator(ResultSet rs) throws SenderException {
		return new ResultStreamIterator(rs, getReader(rs));
	}

}
