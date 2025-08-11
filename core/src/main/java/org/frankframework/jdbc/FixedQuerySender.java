/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
package org.frankframework.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.dbms.JdbcException;
import org.frankframework.stream.Message;
import org.frankframework.util.DB2XMLWriter;

/**
 * QuerySender that assumes a fixed query, possibly with attributes.
 *
 * Example of a {@code XML} result:
 * <pre>{@code
 * <result>
 *     <fielddefinition>
 *         <field name="FIELDNAME"
 *             type="columnType"
 *             columnDisplaySize=""
 *             precision=""
 *             scale=""
 *             isCurrency=""
 *             columnTypeName=""
 *             columnClassName=""/>
 *         <field ...../>
 *     </fielddefinition>
 *     <rowset>
 *         <row number="1">
 *             <field name="FIELDNAME">value</field>
 *             <field name="FIELDNAME" null="true"></field>
 *             <field name="FIELDNAME">value</field>
 *             <field name="FIELDNAME">value</field>
 *         </row>
 *     </rowset>
 * </result>
 * }</pre>
 *
 * See {@link DB2XMLWriter} for more information about the ResultSet!
 *
 * @ff.info The result {@code fieldname} and {@code columntype} are always capital case.
 * @ff.tip The default value of {@code trimSpaces} is {@literal true}.
 * @ff.tip The default value of {@code useNamedParams} is determined by the presence of <code>?&#123;...&#125;</code> in the query.
 * @ff.parameters All parameters present are applied to the query to be executed.
 *
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class FixedQuerySender extends AbstractJdbcQuerySender<QueryExecutionContext> {

	private @Getter String query=null;
	private @Getter int batchSize;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getQuery())) {
			throw new ConfigurationException("query must be specified");
		}
		if(getUseNamedParams() == null && getQuery().contains(UNP_START)) {
			setUseNamedParams(true);
		}
		super.configure();
	}

	@Override
	protected String getQuery(Message message) {
		return getQuery();
	}

	@Override
	public QueryExecutionContext openBlock(PipeLineSession session) throws SenderException, TimeoutException {
		try {
			Connection connection = getConnectionForSendMessage();
			QueryExecutionContext result;
			try {
				QueryExecutionContext result1 = getQueryExecutionContext(connection, null);
				if (getBatchSize()>0) {
					result1.getStatement().clearBatch();
				}
				result = result1;
			} catch (JdbcException | SQLException e) {
				throw new SenderException("cannot getQueryExecutionContext",e);
			}
			return result;
		} catch (JdbcException e) {
			throw new SenderException("cannot get StatementSet",e);
		}
	}

	@Override
	public void closeBlock(QueryExecutionContext blockHandle, PipeLineSession session) {
		try {
			super.closeStatementSet(blockHandle);
		} catch (Exception e) {
			log.warn("Unhandled exception closing statement-set", e);
		}
		closeConnectionForSendMessage(blockHandle.getConnection(), session);
	}

	@Override
	protected void closeStatementSet(QueryExecutionContext statementSet) {
		// postpone close to closeBlock()
	}

	@Override
	// implements IBlockEnabledSender.sendMessage()
	public SenderResult sendMessage(QueryExecutionContext blockHandle, Message message, PipeLineSession session) throws SenderException, TimeoutException {
		return executeStatementSet(blockHandle, message, session);
	}

	/** The SQL query text to be executed each time sendMessage() is called
	 * @ff.mandatory
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/** When set larger than 0 and used as a child of an IteratingPipe, then the database calls are made in batches of this size. Only for queryType=other.
	  * @ff.default 0
	  */
	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

}
