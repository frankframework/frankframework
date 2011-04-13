/*
 * $Log: IDbmsSupportFactory.java,v $
 * Revision 1.1  2011-04-13 08:44:10  L190409
 * Spring configurable DbmsSupport
 *
 */
package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;

/**
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public interface IDbmsSupportFactory {

	final int DBMS_GENERIC=0;
	final int DBMS_ORACLE=1;
	final int DBMS_MSSQLSERVER=2;
	final int DBMS_DB2=3;
	
	IDbmsSupport getDbmsSupport(Connection conn);

}
