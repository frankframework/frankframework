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
