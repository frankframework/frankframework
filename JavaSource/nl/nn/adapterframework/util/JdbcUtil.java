/*
 * $Log: JdbcUtil.java,v $
 * Revision 1.5  2005-08-18 13:37:22  europe\L190409
 * corrected version String
 *
 * Revision 1.4  2005/08/18 13:36:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework using prepared statement
 * close() finally
 *
 * Revision 1.3  2004/03/26 10:42:42  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.2  2004/03/25 13:36:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * table exists via count(*) rather then via metadata
 *
 * Revision 1.1  2004/03/23 17:16:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

/**
 * Database-oriented utility functions.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.1
 */
public class JdbcUtil {
	public static final String version = "$RCSfile: JdbcUtil.java,v $ $Revision: 1.5 $ $Date: 2005-08-18 13:37:22 $";
	protected static Logger log = Logger.getLogger(JdbcUtil.class);
	
	private static final boolean useMetaData=false;
	/**
	 * @return true if tableName exists in database in this connection
	 */
	public static boolean tableExists(Connection conn, String tableName ) throws SQLException {
		
		PreparedStatement stmt = null;
		if (useMetaData) {
			DatabaseMetaData dbmeta = conn.getMetaData();
			ResultSet tableset = dbmeta.getTables(null, null, tableName, null);
			return !tableset.isAfterLast();
		} else {
			try {
				String query="select count(*) from "+tableName;
				log.debug("create statement to check for existence of ["+tableName+"] using query ["+query+"]");
				stmt = conn.prepareStatement(query);
				log.debug("execute statement");
				ResultSet rs = stmt.executeQuery();
				log.debug("statement executed");
				rs.close();
				return true;
			} catch (SQLException e) {
				return false;
			} finally {
				if (stmt!=null) {
					stmt.close();
				}
			}
		}
	}
}
