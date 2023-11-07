/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Utility class to populate and reference side tables.
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.8
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
