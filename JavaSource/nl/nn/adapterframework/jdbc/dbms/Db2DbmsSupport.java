/*
 * $Log: Db2DbmsSupport.java,v $
 * Revision 1.4  2011-11-30 13:51:45  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:47  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2011/10/04 09:54:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getDbmsName()
 *
 * Revision 1.1  2011/03/16 16:47:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 */
package nl.nn.adapterframework.jdbc.dbms;

import nl.nn.adapterframework.jdbc.JdbcException;

import org.apache.commons.lang.StringUtils;

/**
 * preliminary version of Db2DbmsSupport.
 * 
 * @author  Gerrit van Brakel
 * @since  
 * @version Id
 */
public class Db2DbmsSupport extends GenericDbmsSupport {

	public int getDatabaseType() {
		return DbmsSupportFactory.DBMS_DB2;
	}
	
	public String getDbmsName() {
		return "DB2";
	}

	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		// see http://publib.boulder.ibm.com/infocenter/dzichelp/v2r2/index.jsp?topic=/com.ibm.db29.doc.sqlref/db2z_sql_skiplockeddata.htm
		return selectQuery+" FOR UPDATE SKIP LOCKED DATA";
	}

}
