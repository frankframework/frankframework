/*
 * $Log: ResultSetIterator.java,v $
 * Revision 1.1  2007-07-17 11:16:50  europe\L190409
 * added iterating classes
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.log4j.Logger;

/**
 * Iterator over ResultSet.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version Id
 */
class ResultSetIterator implements IDataIterator {
	protected Logger log = LogUtil.getLogger(this);

	private ResultSet rs;
		
	private ResultSetMetaData rsmeta;
	private boolean lineChecked=false;
	private boolean lineAvailable=false;

	int rowNumber=0;

	public ResultSetIterator(ResultSet rs) throws SQLException {
		super();
		this.rs=rs;
		rsmeta=rs.getMetaData();
	}

	public boolean hasNext() throws SenderException {
		try {
			if (!lineChecked) {
				lineAvailable=rs.next();
				lineChecked=true;
			}
			return lineAvailable;
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	public Object next() throws SenderException {
		try {
			lineChecked=false;
			return DB2XMLWriter.getRowXml(rs, rowNumber++, rsmeta, Misc.DEFAULT_INPUT_STREAM_ENCODING, false, "", true).toXML();
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	public void close() {

		Statement stmt=null;			
		Connection con = null;			
		try {
			stmt = rs.getStatement();
			try {
				con = stmt.getConnection();
			} catch (SQLException e) {
				log.warn("obtaining connection", e);
			}
		} catch (SQLException e) {
			log.warn("obtaining statement", e);
		}
		try {
			rs.close();
		} catch (SQLException e) {
			log.warn("closing resultset", e);
		} finally {
			try {
				if (stmt!=null) {
					stmt.close();
				} 
			} catch (SQLException e) {
				log.warn("closing statement", e);
			} finally {
				try {
					if (con!=null) {
						con.close();
					}
				} catch (SQLException e) {
					log.warn("closing connection", e);
				}
			}
		}
	}
}
