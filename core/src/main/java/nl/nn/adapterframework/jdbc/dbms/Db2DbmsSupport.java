/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.jdbc.dbms;

import java.sql.Connection;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;

import org.apache.commons.lang3.StringUtils;

/**
 * Support for DB2.
 * 
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 */
public class Db2DbmsSupport extends GenericDbmsSupport {

	@Override
	public Dbms getDbms() {
		return Dbms.DB2;
	}

	@Override
	public String prepareQueryTextForWorkQueueReading(int batchSize, String selectQuery, int wait) throws JdbcException {
		if (StringUtils.isEmpty(selectQuery) || !selectQuery.toLowerCase().startsWith(KEYWORD_SELECT)) {
			throw new JdbcException("query ["+selectQuery+"] must start with keyword ["+KEYWORD_SELECT+"]");
		}
		// Tried FOR UPDATE SKIP LOCKED (DATA) with DB2 version 10.5 on Linux
		// but generates a SqlSyntaxErrorException. 
		// http://publib.boulder.ibm.com/infocenter/dzichelp/v2r2/topic/com.ibm.db2z10.doc/src/alltoc/db2z_10_prodhome.htm
		// http://publib.boulder.ibm.com/infocenter/dzichelp/v2r2/topic/com.ibm.db2z10.doc.sqlref/src/tpc/db2z_sql_selectstatement.htm#db2z_sql_selectstatement
		// http://publib.boulder.ibm.com/infocenter/dzichelp/v2r2/topic/com.ibm.db2z10.doc.sqlref/src/tpc/db2z_sql_updateclause.htm
		// http://publib.boulder.ibm.com/infocenter/dzichelp/v2r2/topic/com.ibm.db2z10.doc.sqlref/src/tpc/db2z_sql_skiplockeddata.htm
		// http://www.ibm.com/developerworks/data/library/techarticle/dm-0907oracleappsondb2/#code-hd
		return selectQuery+" FOR UPDATE"; // TODO: test to add SKIP LOCKED DATA";
	}

	@Override
	public String prepareQueryTextForWorkQueuePeeking(int batchSize, String selectQuery, int wait) throws JdbcException {
		return selectQuery; // + " SKIP LOCKED DATA";
	}

	@Override
	public String getFirstRecordQuery(String tableName) throws JdbcException {
		String query="select * from "+tableName+" fetch first 1 rows only";
		return query;
	}

	@Override
	public String getSchema(Connection conn) throws JdbcException {
		return JdbcUtil.executeStringQuery(conn, "SELECT CURRENT SERVER FROM SYSIBM.SYSDUMMY1");
	}

}
