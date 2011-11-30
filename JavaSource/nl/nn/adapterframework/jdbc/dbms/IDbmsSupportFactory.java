/*
 * $Log: IDbmsSupportFactory.java,v $
 * Revision 1.3  2011-11-30 13:51:45  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2011/04/13 08:44:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
