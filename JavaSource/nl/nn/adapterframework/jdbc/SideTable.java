/*
 * $Log: SideTable.java,v $
 * Revision 1.1  2009-08-26 15:35:11  L190409
 * support for storing statistics in a database
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;

import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Utility class to populate and reference side tables.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.8
 * @version Id
 */
public class SideTable {
	protected Logger log = LogUtil.getLogger(this);
	
	private String selectQuery;
	private String selectNextValueQuery;
	private String insertQuery;
	

	public SideTable(String tableName, String keyColumn, String nameColumn, String sequence) {
		super();
		createQueries(tableName,keyColumn,nameColumn,sequence);
	}

	private void createQueries(String tableName, String keyColumn, String nameColumn, String sequence) {
		selectQuery="SELECT "+keyColumn+" FROM "+tableName+" WHERE "+nameColumn+"=?";
		selectNextValueQuery="SELECT "+sequence+".nextval FROM DUAL";
		insertQuery="INSERT INTO "+tableName+"("+keyColumn+","+nameColumn+") VALUES (?,?)";
	}

	public int findOrInsert(Connection connection, String name) throws JdbcException {
		int result;
		
		result = JdbcUtil.executeIntQuery(connection,selectQuery,name);
		if (result>=0) {
			return result;
		}
		result = JdbcUtil.executeIntQuery(connection,selectNextValueQuery);
		JdbcUtil.executeStatement(connection,insertQuery,result,name);
		return result;
	}
}
