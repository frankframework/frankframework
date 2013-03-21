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
 * $Log: ResultSet2FileSender.java,v $
 * Revision 1.4  2012-12-06 11:24:08  europe\m168309
 * javadoc
 *
 * Revision 1.3  2012/11/21 10:23:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * ResultSet2FileSender: added maxRecordsSessionKey attribute
 *
 * Revision 1.2  2012/11/20 13:27:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * bugfix for updateTimestamp()
 *
 * Revision 1.1  2012/11/13 11:19:29  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * initial version
 *
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

/**
 * QuerySender that writes each row in a ResultSet to a file.
 * 
 * <p><b>Configuration </b><i>(where deviating from FixedQuerySender)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.ResultSet2FileSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setQuery(String) query}</td><td>query that returns a row to be processed. Must contain a message field (1) which is written to a file and optionally a status field (2) and a group field (3)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStatusFieldType(String) statusFieldType}</td><td>type of the optional status field which is set after the row is written to the file: timestamp</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileNameSessionKey(String) fileNameSessionKey}</td><td>the session key that contains the name of the file to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAppend(boolean) append}</td><td>when set <code>true</code> and the file already exists, the resultset rows are written to the end of the file</td><td>false</td></tr>
 * <tr><td>{@link #setMaxRecordsSessionKey(String) maxRecordsSessionKey}</td><td>when set (and &gt;=0), this session key contains the maximum number of records which are processed. If <code>query</code> contains a group field (3), then also following records with the same group field value as the last record are processed</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @version $Id$
 * @author  Peter Leeuwenburgh
 */
public class ResultSet2FileSender extends FixedQuerySender {
	private String fileNameSessionKey;
	private String statusFieldType;
	private boolean append = false;
	private String maxRecordsSessionKey;

	protected byte[] eolArray=null;

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getFileNameSessionKey())) {
			throw new ConfigurationException(getLogPrefix()+"fileNameSessionKey must be specified");
		}
		String sft = getStatusFieldType();
		if (StringUtils.isNotEmpty(sft)) {
			if (!sft.equalsIgnoreCase("timestamp")) {
				throw new ConfigurationException(getLogPrefix() + "illegal value for statusFieldType [" + sft + "], must be 'timestamp'");
			}
		}
		eolArray = System.getProperty("line.separator").getBytes();
	}

	protected String sendMessage(Connection connection, String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		int counter = 0;
		ResultSet resultset=null;
		String fileName = (String)prc.getSession().get(getFileNameSessionKey());
		int maxRecords = -1;
		if (StringUtils.isNotEmpty(getMaxRecordsSessionKey())) {
			maxRecords = Integer.parseInt((String)prc.getSession().get(getMaxRecordsSessionKey()));
		}

		FileOutputStream fos=null;
		try {
			fos = new FileOutputStream(fileName, isAppend());
			PreparedStatement statement = getStatement(connection, correlationID, null, true);
			resultset = statement.executeQuery();
			boolean eor = false;
			if (maxRecords==0) {
				eor = true;
			}
			while (resultset.next() && !eor) {
				counter++;
				processResultSet(resultset, fos, counter);
				if (maxRecords>=0 && counter>=maxRecords) {
					ResultSetMetaData rsmd = resultset.getMetaData();
					if (rsmd.getColumnCount() >= 3) {
						String group = resultset.getString(3);
						while (resultset.next() && !eor) {
							String groupNext = resultset.getString(3);
							if (groupNext.equals(group)) {
								counter++;
								processResultSet(resultset, fos, counter);
							} else {
								eor = true;
							}
						}
					} else {
						eor = true;
					}
				}
			}
		} catch (FileNotFoundException e) {
			throw new SenderException(getLogPrefix() + "could not find file [" + fileName + "]", e);
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "got IOException", e);
		} catch (SQLException sqle) {
			throw new SenderException(getLogPrefix() + "got exception executing a SQL command", sqle);
		} catch (JdbcException e) {
			throw new SenderException(getLogPrefix() + "got exception executing a SQL command", e);
		} finally {
			try {
				if (fos!=null) {
					fos.close();
				}
			} catch (IOException e) {
				log.warn(new SenderException(getLogPrefix() + "got exception closing fileoutputstream", e));
			}
			try {
				if (resultset!=null) {
					resultset.close();
				}
			} catch (SQLException e) {
				log.warn(new SenderException(getLogPrefix() + "got exception closing resultset", e));
			}
		}
		return "<result><rowsprocessed>" + counter + "</rowsprocessed></result>";
	}

	private void processResultSet (ResultSet resultset, FileOutputStream fos, int counter) throws SQLException, IOException {
		String rec_str = resultset.getString(1);
		if (log.isDebugEnabled()) {
			log.debug("iteration [" + counter + "] item [" + rec_str + "]");
		} 
		if ("timestamp".equalsIgnoreCase(getStatusFieldType())) {
			//TODO: statusFieldType is nu altijd een timestamp (dit moeten ook andere types kunnen zijn)
			resultset.updateTimestamp(2 , new Timestamp((new Date()).getTime()));
			resultset.updateRow();
		}
		if (rec_str!=null) {
			fos.write(rec_str.getBytes());
		}
		fos.write(eolArray);
	}
	
	public void setStatusFieldType(String statusFieldType) {
		this.statusFieldType = statusFieldType;
	}
	public String getStatusFieldType() {
		return statusFieldType;
	}

	public void setFileNameSessionKey(String filenameSessionKey) {
		this.fileNameSessionKey = filenameSessionKey;
	}
	public String getFileNameSessionKey() {
		return fileNameSessionKey;
	}

	public void setAppend(boolean b) {
		append = b;
	}
	public boolean isAppend() {
		return append;
	}

	public void setMaxRecordsSessionKey(String maxRecordsSessionKey) {
		this.maxRecordsSessionKey = maxRecordsSessionKey;
	}
	public String getMaxRecordsSessionKey() {
		return maxRecordsSessionKey;
	}
}