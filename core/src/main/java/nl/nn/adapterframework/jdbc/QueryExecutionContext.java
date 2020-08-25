/*
   Copyright 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.sql.PreparedStatement;

import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.parameters.ParameterList;

public class QueryExecutionContext {

	private String query;
	private String queryType;
	private ParameterList parameterList;
	private Connection connection; 
	private PreparedStatement statement;
	private PreparedStatement resultQueryStatement;
	private JdbcSession jdbcSession;
	protected int iteration;

	public QueryExecutionContext(String query, String queryType, ParameterList parameterList) {
		this.query = query;
		this.queryType = queryType;
		this.parameterList = parameterList;
	}

	public String getQuery() {
		return query;
	}
	public void setQuery(String query) {
		this.query = query;
	}

	public String getQueryType() {
		return queryType;
	}
	public void setQueryType(String queryType) {
		this.queryType = queryType;
	}

	public ParameterList getParameterList() {
		return parameterList;
	}

	
	public Connection getConnection() {
		return connection;
	}
	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public PreparedStatement getStatement() {
		return statement;
	}
	public void setStatement(PreparedStatement statement) {
		this.statement = statement;
	}

	public PreparedStatement getResultQueryStatement() {
		return resultQueryStatement;
	}
	public void setResultQueryStatement(PreparedStatement resultQueryStatement) {
		this.resultQueryStatement = resultQueryStatement;
	}

	public void setJdbcSession(JdbcSession jdbcSession) {
		this.jdbcSession = jdbcSession;
	}
	public JdbcSession getJdbcSession() {
		return jdbcSession;
	}
}