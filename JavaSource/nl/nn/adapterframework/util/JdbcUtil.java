/*
 * $Log: JdbcUtil.java,v $
 * Revision 1.1  2004-03-23 17:16:14  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.util;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database-oriented utility functions
 * <p>$Id: JdbcUtil.java,v 1.1 2004-03-23 17:16:14 L190409 Exp $</p>
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class JdbcUtil {
	public static final String version="$Id: JdbcUtil.java,v 1.1 2004-03-23 17:16:14 L190409 Exp $";
	/**
	 * @return true if tableName exists in database in this connection
	 */
	public static boolean tableExists(Connection conn, String tableName ) throws SQLException {
			DatabaseMetaData dbmeta = conn.getMetaData();
			ResultSet tableset = dbmeta.getTables(null, null, tableName, null);
			
			return !tableset.isAfterLast();
	}
}
