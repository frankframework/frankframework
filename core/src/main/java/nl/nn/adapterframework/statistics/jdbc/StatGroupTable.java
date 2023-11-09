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
package nl.nn.adapterframework.statistics.jdbc;

import java.sql.Connection;

import nl.nn.adapterframework.dbms.DbmsException;
import nl.nn.adapterframework.util.DbmsUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.dbms.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Utility class to populate and reference groups used in statistics.
 *
 * @author  Gerrit van Brakel
 * @since   4.9.8
 */
public class StatGroupTable {
	protected Logger log = LogUtil.getLogger(this);

	private String selectRootByNameQuery;
	private String selectByNameQuery;
	private String selectByTypeQuery;
	private String selectByNameAndTypeQuery;
	private String selectNextValueQuery;
	private String insertQuery;
	private String insertRootQuery;


	public StatGroupTable(String tableName, String keyColumn, String parentKeyColumn, String instanceKeyColumn, String nameColumn, String typeColumn, String sequence) {
		super();
		createQueries(tableName,keyColumn,parentKeyColumn,instanceKeyColumn,nameColumn,typeColumn,sequence);
	}

	private void createQueries(String tableName, String keyColumn, String parentKeyColumn, String instanceKeyColumn, String nameColumn, String typeColumn, String sequence) {
		selectRootByNameQuery="SELECT "+keyColumn+" FROM "+tableName+" WHERE "+parentKeyColumn+" IS NULL AND "+instanceKeyColumn+"=? AND "+nameColumn+"=?";
		selectByNameQuery="SELECT "+keyColumn+" FROM "+tableName+" WHERE "+parentKeyColumn+"=? AND "+instanceKeyColumn+"=? AND "+nameColumn+"=?";
		selectByTypeQuery="SELECT "+keyColumn+" FROM "+tableName+" WHERE "+parentKeyColumn+"=? AND "+instanceKeyColumn+"=? AND "+typeColumn+"=?";
		selectByNameAndTypeQuery="SELECT "+keyColumn+" FROM "+tableName+" WHERE "+parentKeyColumn+"=? AND "+instanceKeyColumn+"=? AND "+nameColumn+"=? AND "+typeColumn+"=?";
		selectNextValueQuery="SELECT "+sequence+".nextval FROM DUAL";
		insertQuery="INSERT INTO "+tableName+"("+keyColumn+","+parentKeyColumn+","+instanceKeyColumn+","+nameColumn+","+typeColumn+") VALUES (?,?,?,?,?)";
		insertRootQuery="INSERT INTO "+tableName+"("+keyColumn+","+instanceKeyColumn+","+nameColumn+","+typeColumn+") VALUES (?,?,?,?)";
	}

	public int findOrInsert(Connection connection, int parentKey, int instanceKey, String name, String type) throws JdbcException, DbmsException {
		int result;

		if (parentKey<0) {
			result = DbmsUtil.executeIntQuery(connection,selectRootByNameQuery,instanceKey,name);
		} else if (StringUtils.isNotEmpty(name)) {
			result = DbmsUtil.executeIntQuery(connection,selectByNameAndTypeQuery,parentKey,instanceKey,name,type);
		} else {
			result = DbmsUtil.executeIntQuery(connection,selectByTypeQuery,parentKey,instanceKey,type);

		}
		if (result>=0) {
			return result;
		}
		result = DbmsUtil.executeIntQuery(connection,selectNextValueQuery);
		if (parentKey<0) {
			JdbcUtil.executeStatement(connection,insertRootQuery,instanceKey,name);
		} else {
			JdbcUtil.executeStatement(connection,insertQuery,result,parentKey,instanceKey,name==null?"":name,type);
		}
		return result;
	}
}
