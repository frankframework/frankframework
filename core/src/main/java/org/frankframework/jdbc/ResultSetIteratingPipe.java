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
package org.frankframework.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ApplicationWarnings;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.IDataIterator;
import org.frankframework.core.SenderException;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StreamUtil;

/**
 * Pipe that iterates over rows in in ResultSet.
 *
 * Each row is send passed to the sender in the same format a row is usually returned from a query.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class ResultSetIteratingPipe extends JdbcIteratingPipeBase {

	private final boolean suppressResultSetHoldabilityWarning = AppConstants.getInstance().getBoolean(SuppressKeys.RESULT_SET_HOLDABILITY.getKey(), false);
	private String blobCharset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		try(Connection connection=querySender.getConnection()){
			DatabaseMetaData md = connection.getMetaData();
			if (!suppressResultSetHoldabilityWarning && md.getResultSetHoldability() != ResultSet.HOLD_CURSORS_OVER_COMMIT) {
				// For (some?) combinations of WebSphere and (XA) Databases this seems to be the default and result in the following exception:
				// com.ibm.websphere.ce.cm.ObjectClosedException: DSRA9110E: ResultSet is closed.
				// When a ResultSetIteratingPipe is calling next() on the ResultSet after processing the first message it will throw the exception when:
				// - the ResultSetIteratingPipe is non-transacted and the sender calls a sub-adapter that is transacted (transactionAttribute="Required")
				// - the ResultSetIteratingPipe is transacted and sender contains a non transacted sender (transactionAttribute="NotSupported")
				// Either none, or both need to be transacted.
				// See issue #2015 ((ObjectClosedException) DSRA9110E: ResultSet is closed) on www.github.com
				ApplicationWarnings.add(log, "The database's default holdability for ResultSet objects is " + md.getResultSetHoldability() + " instead of " + ResultSet.HOLD_CURSORS_OVER_COMMIT + " (ResultSet.HOLD_CURSORS_OVER_COMMIT). This may cause 'DSRA9110E: ResultSet is closed' error on WebSphere if the subadapter has a different transactionality than the main adapter.");
			}
		} catch (JdbcException | SQLException e) {
			log.warn("Exception determining databaseinfo",e);
		}

		if(StringUtils.isNotBlank(querySender.getBlobCharset())) {
			blobCharset = querySender.getBlobCharset();
		}
	}

	@Override
	protected IDataIterator<String> getIterator(IDbmsSupport dbmsSupport, Connection conn, ResultSet rs) throws SenderException {
		try {
			return new ResultSetIterator(dbmsSupport, conn, rs, blobCharset, querySender.isBlobsCompressed(), querySender.isBlobSmartGet());
		} catch (SQLException e) {
			throw new SenderException(e);
		}
	}

}
