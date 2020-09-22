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
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;

/**
 * Pipe that iterates over rows in in ResultSet.
 *
 * Each row is send passed to the sender in the same format a row is usually returned from a query.
 *
 * <p><b>Configuration </b><i>(where deviating from IteratingPipe)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.jdbc.JdbcIteratingPipeBase</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>the SQL query text to be excecuted each time sendMessage() is called</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setLockRows(boolean) lockRows}</td><td>When set <code>true</code>, exclusive row-level locks are obtained on all the rows identified by the SELECT statement (by appending ' FOR UPDATE NOWAIT SKIP LOCKED' to the end of the query)</td><td>false</td></tr>
 * <tr><td>{@link #setLockWait(int) lockWait}</td><td>when set and >=0, ' FOR UPDATE WAIT #' is used instead of ' FOR UPDATE NOWAIT SKIP LOCKED'</td><td>-1</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>can be configured from JmsRealm, too</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class ResultSetIteratingPipe extends JdbcIteratingPipeBase {

	@Override
	protected IDataIterator<String> getIterator(IDbmsSupport dbmsSupport, Connection conn, ResultSet rs) throws SenderException {
		try {
			return new ResultSetIterator(dbmsSupport, conn, rs);
		} catch (SQLException e) {
			throw new SenderException(e);
		}
	}

}
