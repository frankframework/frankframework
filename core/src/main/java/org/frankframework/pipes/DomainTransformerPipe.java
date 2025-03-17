/*
   Copyright 2013, 2018, 2020 Nationale-Nederlanden, 2022 WeAreFrank!

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
package org.frankframework.pipes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.dbms.JdbcException;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.stream.Message;
import org.frankframework.util.JdbcUtil;

/**
 * Pipe that performs domain transforming on the basis of a database table.
 *
 * Every string that equals "%![DT{<code>label</code>,<code>valueIn</code>,<code>type</code>}]" will be replaced by <code>valueOut</code>.
 *
 * The field <code>type</code>, which is optional, indicates the format of <code>valueIn</code>. Currently, the following types are supported:
 *
 * <ul>
 *   <li><code>string</code> (default): the method setString() is used.</li>
 *   <li><code>number</code>: the method setDouble() is used.</li>
 * </ul>
 *
 *
 * @author  Peter Leeuwenburgh
 * @since   4.9
 */
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class DomainTransformerPipe extends FixedForwardPipe {

	private static final String DT_START = "%![DT{";
	private static final String DT_SEPARATOR = ",";
	private static final String DT_END = "}]";
	private static final String TYPE_NUMBER = "number";
	private static final String TYPE_STRING = "string";

	private @Getter String tableName = "mapping";
	private @Getter String labelField = "label";
	private @Getter String valueInField = "valueIn";
	private @Getter String valueOutField = "valueOut";

	private FixedQuerySender qs;
	private @Getter String query;
	private @Getter String datasourceName;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		qs = createBean(FixedQuerySender.class);
		qs.setDatasourceName(getDatasourceName());

		//dummy query required
		qs.setQuery("SELECT count(*) FROM ALL_TABLES");
		qs.configure();

		Connection conn = null;
		try {
			conn = qs.getConnection(); // TODO this should not be done in configure, qs is not yet open.
			if (!qs.getDbmsSupport().isTablePresent(conn, tableName)) {
				throw new ConfigurationException("The table [" + tableName + "] doesn't exist");
			}
			if (!qs.getDbmsSupport().isColumnPresent(conn, tableName, labelField)) {
				throw new ConfigurationException("The column [" + labelField + "] doesn't exist");
			}
			if (!qs.getDbmsSupport().isColumnPresent(conn, tableName, valueInField)) {
				throw new ConfigurationException("The column [" + valueInField + "] doesn't exist");
			}
			if (!qs.getDbmsSupport().isColumnPresent(conn, tableName, valueOutField)) {
				throw new ConfigurationException("The column [" + valueOutField + "] doesn't exist");
			}
			query = "SELECT " + valueOutField + " FROM " + tableName + " WHERE " + labelField+ "=? AND " + valueInField + "=?";
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		} finally {
			JdbcUtil.close(conn);
		}
	}

	@Override
	public PipeRunResult doPipe(Message invoer, PipeLineSession session) throws PipeRunException {
		StringBuilder builder = new StringBuilder();

		try(Connection conn = qs.getConnection(); PreparedStatement stmt = conn.prepareStatement(getQuery())) {
			String invoerString = invoer.asString();
			int startPos = invoerString.indexOf(DT_START);
			if (startPos == -1)
				return new PipeRunResult(getSuccessForward(), invoerString);
			char[] invoerChars = invoerString.toCharArray();
			int copyFrom = 0;
			while (startPos != -1) {
				builder.append(invoerChars, copyFrom, startPos - copyFrom);
				int nextStartPos =
					invoerString.indexOf(DT_START, startPos + DT_START.length());
				if (nextStartPos == -1) {
					nextStartPos = invoerString.length();
				}
				int endPos =
					invoerString.indexOf(DT_END, startPos + DT_START.length());
				if (endPos == -1 || endPos > nextStartPos) {
					log.warn("Found a start delimiter without an end delimiter at position [{}] in [{}]", startPos, invoerString);
					builder.append(invoerChars, startPos, nextStartPos - startPos);
					copyFrom = nextStartPos;
				} else {
					String invoerSubstring = invoerString.substring(startPos + DT_START.length(),endPos);
					StringTokenizer st = new StringTokenizer(invoerSubstring, DT_SEPARATOR);
					int aantalTokens = st.countTokens();
					if (aantalTokens < 2 || aantalTokens > 3) {
						log.warn("Only 2 or 3 tokens are allowed in [{}]", invoerSubstring);
						builder.append(invoerChars, startPos, endPos - startPos + DT_END.length());
						copyFrom = endPos + DT_END.length();
					} else {
						String label = st.nextToken();
						String valueIn = st.nextToken();
						String type = TYPE_STRING;
						if (st.hasMoreTokens()) {
							type = st.nextToken();
						}
						if (!type.equals(TYPE_STRING)
							&& !type.equals(TYPE_NUMBER)) {
							log.warn("Only types [{},{}] are allowed in [{}]", TYPE_STRING, TYPE_NUMBER, invoerSubstring);
							builder.append(invoerChars, startPos, endPos - startPos + DT_END.length());
							copyFrom = endPos + DT_END.length();
						} else {
							String valueOut = null;
							valueOut = getValueOut(label, valueIn, type, stmt);
							if (valueOut != null) {
								builder.append(valueOut);
							}
							copyFrom = endPos + DT_END.length();
						}
					}
				}
				startPos = invoerString.indexOf(DT_START, copyFrom);
			}
			builder.append(invoerChars, copyFrom, invoerChars.length - copyFrom);

		} catch (Throwable t) {
			throw new PipeRunException(this, " Exception on transforming domain", t);
		}

		return new PipeRunResult(getSuccessForward(), builder.toString());
	}

	public String getValueOut(String label, String valueIn, String type, PreparedStatement stmt) throws SQLException {
		stmt.setString(1, label);
		if (type.equals(TYPE_NUMBER)) {
			double d = Double.parseDouble(valueIn);
			stmt.setDouble(2, d);
		} else {
			stmt.setString(2, valueIn);
		}
		try (ResultSet rs = stmt.executeQuery()) {
			String result = null;
			if (rs.next()) {
				result = rs.getString(1);
			}
			return result;
		}
	}

	@Override
	public void start() {
		qs.start();
	}

	@Override
	public void stop() {
		qs.stop();
	}

	public void setDatasourceName(String datasourceName) {
		this.datasourceName = datasourceName;
	}

	/**
	 * The name of the table that contains the mapping.
	 * @ff.default mapping
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	/**
	 * The name of the column labels are stored in. 
	 * @ff.default label
	 */
	public void setLabelField(String labelField) {
		this.labelField = labelField;
	}

	/**
	 * The name of the column source values are stored in.
	 * @ff.default valuein
	 */
	public void setValueInField(String valueInField) {
		this.valueInField = valueInField;
	}

	/**
	 * The name of the column destination values are stored in.
	 * @ff.default valueout
	 */
	public void setValueOutField(String valueOutField) {
		this.valueOutField = valueOutField;
	}
}
