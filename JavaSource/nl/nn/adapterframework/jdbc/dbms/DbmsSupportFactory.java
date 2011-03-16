/*
 * $Log: DbmsSupportFactory.java,v $
 * Revision 1.1  2011-03-16 16:47:26  L190409
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 */
package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class DbmsSupportFactory {
	protected static Logger log = LogUtil.getLogger(DbmsSupportFactory.class);

	public final static int DBMS_GENERIC=0;
	public final static int DBMS_ORACLE=1;
	public final static int DBMS_MSSQLSERVER=2;
	public final static int DBMS_DB2=3;
	
	public final static String PRODUCT_NAME_ORACLE_="Oracle";
	public final static String PRODUCT_NAME_MSSQLSERVER="Microsoft SQL Server";

	public static IDbmsSupport getDbmsSupport(Connection conn) throws SQLException {
		DatabaseMetaData md=conn.getMetaData();
		String product=md.getDatabaseProductName();
		if (PRODUCT_NAME_ORACLE_.equals(product)) {
			log.debug("Setting databasetype to ORACLE");
			return new OracleDbmsSupport();
		}
		if (PRODUCT_NAME_MSSQLSERVER.equals(product)) {
			log.debug("Setting databasetype to MSSQLSERVER");
			return new MsSqlServerDbmsSupport();
		}
		log.debug("Setting databasetype to GENERIC, productName ["+product+"]");
		return new GenericDbmsSupport();
	}

}
