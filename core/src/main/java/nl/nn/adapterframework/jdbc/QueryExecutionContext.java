/*
   Copyright 2019 Nationale-Nederlanden, 2020-2021, 2023 WeAreFrank!

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

import lombok.Getter;
import nl.nn.adapterframework.jdbc.JdbcQuerySenderBase.QueryType;
import nl.nn.adapterframework.parameters.ParameterList;

public class QueryExecutionContext {

	@Getter private final String query;
	@Getter private final QueryType queryType;
	@Getter private final ParameterList parameterList;
	@Getter private final Connection connection;
	@Getter private final PreparedStatement statement;
	@Getter private final PreparedStatement resultQueryStatement;
	protected int iteration;

	public QueryExecutionContext(String query, QueryType queryType, ParameterList parameterList, Connection connection, PreparedStatement statement, PreparedStatement resultQueryStatement) {
		this.query = query;
		this.queryType = queryType;
		this.parameterList = parameterList;
		this.connection = connection;
		this.statement = statement;
		this.resultQueryStatement = resultQueryStatement;
	}
}
