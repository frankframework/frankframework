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
/*
 * $Log: DomainTransformerPipe.java,v $
 * Revision 1.6  2012-06-01 10:52:50  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.5  2012/02/17 18:04:59  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Close connection too
 * Use proxiedDataSources
 *
 * Revision 1.4  2011/11/30 13:51:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2008/10/24 06:39:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * dummy query required
 *
 * Revision 1.1  2008/08/27 16:18:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */

package nl.nn.adapterframework.pipes;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * Pipe that performs domain transforming on the basis of a database table.
 *
 * Every string which equals "%![DT{<code>label</code>,<code>valueIn</code>,<code>type</code>}]" will be replaced by <code>valueOut</code>. 
 * 
 * The field <code>type</code>, which is optional, indicates the format of <code>valueIn</code>. Currently the following types are supported:
 * 
 * <ul>
 *   <li><code>string</code> (default): the methode setString() is used</li>
 *   <li><code>number</code>: the method setDouble() is used </li>
 * </ul>
 *  
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTableName(String) tableName}</td><td>the name of the table that contains the mapping</td><td>mapping</td></tr>
 * <tr><td>{@link #setLabelField(String) labelField}</td><td>the name of the column labels are stored in</td><td>label</td></tr>
 * <tr><td>{@link #setValueInField(String) valueInField}</td><td>the name of the column source values are stored in</td><td>valueIn</td></tr>
 * <tr><td>{@link #setValueOutField(String) valueOutField}</td><td>the name of the column destination values are stored in</td><td>valueOut</td></tr>
 * </table>
 * </p>
 * 
 * @author  Peter Leeuwenburgh
 * @since   4.9
 * @version $Id$
 */

public class DomainTransformerPipe extends FixedForwardPipe {

	private final static String DT_START = "%![DT{";
	private final static String DT_SEPARATOR = ",";
	private final static String DT_END = "}]";
	private final static String TYPE_NUMBER = "number";
	private final static String TYPE_STRING = "string";

	private String tableName = "mapping";
	private String labelField = "label";
	private String valueInField = "valueIn";
	private String valueOutField = "valueOut";

	private FixedQuerySender qs = new FixedQuerySender();
	private String query;

	public void configure() throws ConfigurationException {
		super.configure();

		//dummy query required
		qs.setQuery("SELECT count(*) FROM ALL_TABLES");
		qs.configure();

		try {
			Connection conn = qs.getConnection();
			if (!JdbcUtil.columnExists(conn, tableName, "*")) {
				throw new ConfigurationException("The table [" + tableName + "] doesn't exist");
			}
			if (!JdbcUtil.columnExists(conn, tableName, labelField)) {
				throw new ConfigurationException("The column [" + labelField + "] doesn't exist");
			}
			if (!JdbcUtil.columnExists(conn, tableName, valueInField)) {
				throw new ConfigurationException("The column [" + valueInField + "] doesn't exist");
			}
			if (!JdbcUtil.columnExists(conn, tableName, valueOutField)) {
				throw new ConfigurationException("The column [" + valueOutField + "] doesn't exist");
			}

			query = "SELECT " + valueOutField + " FROM " + tableName + 
					" WHERE " + labelField+ "=? AND " + valueInField + "=?";
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		} catch (SQLException e) {
			throw new ConfigurationException(e);
		}
	}

	public PipeRunResult doPipe(Object invoer, IPipeLineSession session)
		throws PipeRunException {
		Connection conn = null;
		PreparedStatement stmt = null;
		StringBuffer buffer = new StringBuffer();

		try {
			conn = qs.getConnection();
			stmt = conn.prepareStatement(query);

			String invoerString = invoer.toString();
			int startPos = invoerString.indexOf(DT_START);
			if (startPos == -1)
				return new PipeRunResult(getForward(), invoerString);
			char[] invoerChars = invoerString.toCharArray();
			int copyFrom = 0;
			while (startPos != -1) {
				buffer.append(invoerChars, copyFrom, startPos - copyFrom);
				int nextStartPos =
					invoerString.indexOf(
						DT_START,
						startPos + DT_START.length());
				if (nextStartPos == -1) {
					nextStartPos = invoerString.length();
				}
				int endPos =
					invoerString.indexOf(DT_END, startPos + DT_START.length());
				if (endPos == -1 || endPos > nextStartPos) {
					log.warn(
						getLogPrefix(session)
							+ "Found a start delimiter without an end delimiter at position ["
							+ startPos + "] in ["+ invoerString+ "]");
					buffer.append(invoerChars, startPos, nextStartPos - startPos);
					copyFrom = nextStartPos;
				} else {
					String invoerSubstring = invoerString.substring(startPos + DT_START.length(),endPos);
					StringTokenizer st = new StringTokenizer(invoerSubstring, DT_SEPARATOR);
					int aantalTokens = st.countTokens();
					if (aantalTokens < 2 || aantalTokens > 3) {
						log.warn(getLogPrefix(session)	+ "Only 2 or 3 tokens are allowed in [" + invoerSubstring + "]");
						buffer.append(invoerChars, startPos, endPos - startPos + DT_END.length());
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
							log.warn(
								getLogPrefix(session)
									+ "Only types ["+ TYPE_STRING+ ","+ TYPE_NUMBER+ "] are allowed in ["+ invoerSubstring+ "]");
							buffer.append(invoerChars, startPos, endPos - startPos + DT_END.length());
							copyFrom = endPos + DT_END.length();
						} else {
							String valueOut = null;
							valueOut = getValueOut(label, valueIn, type, stmt);
							if (valueOut != null) {
								buffer.append(valueOut);
							}
							copyFrom = endPos + DT_END.length();
						}
					}
				}
				startPos = invoerString.indexOf(DT_START, copyFrom);
			}
			buffer.append(invoerChars, copyFrom, invoerChars.length - copyFrom);

		} catch (Throwable t) {
			throw new PipeRunException(
				this,
				getLogPrefix(session) + " Exception on transforming domain",
				t);

		} finally {
			if (stmt != null) {
				try {
					stmt.close();
				} catch (SQLException e) {
					log.warn(
						getLogPrefix(null) + "exception closing statement",
						e);
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					log.warn(
						getLogPrefix(null) + "exception closing connection",
						e);
				}
			}
		}

		return new PipeRunResult(getForward(), buffer.toString());
	}

	public String getValueOut(
		String label,
		String valueIn,
		String type,
		PreparedStatement stmt)
		throws JdbcException, SQLException {
		ResultSet rs = null;
		try {
			stmt.setString(1, label);
			if (type.equals(TYPE_NUMBER)) {
				double d = Double.valueOf(valueIn.toString()).doubleValue();
				stmt.setDouble(2, d);
			} else {
				stmt.setString(2, valueIn);
			}
			rs = stmt.executeQuery();
			String result = null;
			if (rs.next()) {
				result = rs.getString(1);
			}
			return result;
		} finally {
			if (rs != null) {
				rs.close();
			}
		}
	}

	public void start() throws PipeStartException {
		try {
			qs.open();
		} catch (Throwable t) {
			PipeStartException pse =
				new PipeStartException(
					getLogPrefix(null) + "could not start",
					t);
			pse.setPipeNameInError(getName());
			throw pse;
		}
	}
	public void stop() {
		log.info(getLogPrefix(null) + "is closing");
		try {
			qs.close();
		} catch (SenderException e) {
			log.warn(getLogPrefix(null) + "exception closing sender", e);
		}
	}

	public void setProxiedDataSources(Map proxiedDataSources) {
		qs.setProxiedDataSources(proxiedDataSources);
	}

	public void setJmsRealm(String jmsRealm) {
		qs.setJmsRealm(jmsRealm);
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setLabelField(String labelField) {
		this.labelField = labelField;
	}

	public String getLabelField() {
		return labelField;
	}

	public void setValueInField(String valueInField) {
		this.valueInField = valueInField;
	}

	public String getValueInField() {
		return valueInField;
	}

	public void setValueOutField(String valueOutField) {
		this.valueOutField = valueOutField;
	}

	public String getValueOutField() {
		return valueOutField;
	}
}
