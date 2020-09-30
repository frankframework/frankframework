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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * QuerySender that writes each row in a ResultSet to a file.
 * 
 * @author  Peter Leeuwenburgh
 */
public class ResultSet2FileSender extends FixedQuerySender {
	private String fileNameSessionKey;
	private String statusFieldType;
	private boolean append = false;
	private String maxRecordsSessionKey;

	protected byte[] eolArray=null;

	@Override
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

	@Override
	public Message sendMessage(QueryExecutionContext blockHandle, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		int counter = 0;
		ResultSet resultset=null;
		String fileName = (String)session.get(getFileNameSessionKey());
		int maxRecords = -1;
		if (StringUtils.isNotEmpty(getMaxRecordsSessionKey())) {
			maxRecords = Integer.parseInt((String)session.get(getMaxRecordsSessionKey()));
		}

		FileOutputStream fos=null;
		try {
			fos = new FileOutputStream(fileName, isAppend());
			QueryExecutionContext queryExecutionContext = blockHandle;
			PreparedStatement statement=queryExecutionContext.getStatement();
			JdbcUtil.applyParameters(getDbmsSupport(), statement, queryExecutionContext.getParameterList(), message, session);
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
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix() + "got Exception resolving parameter", e);
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
		return new Message("<result><rowsprocessed>" + counter + "</rowsprocessed></result>");
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
	
	@IbisDoc({"type of the optional status field which is set after the row is written to the file: timestamp", ""})
	public void setStatusFieldType(String statusFieldType) {
		this.statusFieldType = statusFieldType;
	}
	public String getStatusFieldType() {
		return statusFieldType;
	}

	@IbisDoc({"the session key that contains the name of the file to use", ""})
	public void setFileNameSessionKey(String filenameSessionKey) {
		this.fileNameSessionKey = filenameSessionKey;
	}
	public String getFileNameSessionKey() {
		return fileNameSessionKey;
	}

	@IbisDoc({"when set <code>true</code> and the file already exists, the resultset rows are written to the end of the file", "false"})
	public void setAppend(boolean b) {
		append = b;
	}
	public boolean isAppend() {
		return append;
	}

	@IbisDoc({"when set (and &gt;=0), this session key contains the maximum number of records which are processed. if <code>query</code> contains a group field (3), then also following records with the same group field value as the last record are processed", ""})
	public void setMaxRecordsSessionKey(String maxRecordsSessionKey) {
		this.maxRecordsSessionKey = maxRecordsSessionKey;
	}
	public String getMaxRecordsSessionKey() {
		return maxRecordsSessionKey;
	}
}