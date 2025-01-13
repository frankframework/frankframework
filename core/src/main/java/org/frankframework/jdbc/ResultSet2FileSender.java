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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.dbms.JdbcException;
import org.frankframework.stream.Message;
import org.frankframework.util.JdbcUtil;

/**
 * QuerySender that writes each row in a ResultSet to a file.
 *
 * @ff.info Please note that the default value of {@code trimSpaces} is {@literal true}
 * @author  Peter Leeuwenburgh
 */
public class ResultSet2FileSender extends FixedQuerySender {
	private @Getter String filenameSessionKey;
	private @Getter String statusFieldType;
	private @Getter boolean append = false;
	private @Getter String maxRecordsSessionKey;

	protected byte[] eolArray = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getFilenameSessionKey())) {
			throw new ConfigurationException("filenameSessionKey must be specified");
		}
		String sft = getStatusFieldType();
		if (StringUtils.isNotEmpty(sft)) {
			if (!"timestamp".equalsIgnoreCase(sft)) {
				throw new ConfigurationException("illegal value for statusFieldType [" + sft + "], must be 'timestamp'");
			}
		}
		eolArray = System.getProperty("line.separator").getBytes();
	}

	@Override
	protected SenderResult executeStatementSet(@Nonnull QueryExecutionContext queryExecutionContext, @Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException {
		String fileName = session.getString(getFilenameSessionKey());
		if (fileName == null) {
			throw new SenderException("unable to get filename from session key ["+getFilenameSessionKey()+"]");
		}
		int maxRecords = -1;
		if (StringUtils.isNotEmpty(getMaxRecordsSessionKey())) {
			try {
				maxRecords = session.getInteger(getMaxRecordsSessionKey());
			} catch (Exception e) {
				throw new SenderException("unable to parse "+getMaxRecordsSessionKey()+" to integer", e);
			}
		}

		int counter = 0;
		try (FileOutputStream fos = new FileOutputStream(fileName, isAppend())) {
			PreparedStatement statement=queryExecutionContext.getStatement();
			JdbcUtil.applyParameters(getDbmsSupport(), statement, queryExecutionContext.getParameterList(), message, session);
			try (ResultSet resultset = statement.executeQuery()) {
				boolean eor = maxRecords == 0;
				while (!eor && resultset.next()) {
					counter++;
					processResultSet(resultset, fos, counter);
					if (maxRecords>=0 && counter>=maxRecords) {
						ResultSetMetaData rsmd = resultset.getMetaData();
						if (rsmd.getColumnCount() >= 3) {
							String group = resultset.getString(3);
							while (!eor && resultset.next()) {
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
			}
		} catch (FileNotFoundException e) {
			throw new SenderException("could not find file [" + fileName + "]", e);
		} catch (ParameterException e) {
			throw new SenderException("got Exception resolving parameter", e);
		} catch (IOException e) {
			throw new SenderException("got IOException", e);
		} catch (SQLException | JdbcException e) {
			throw new SenderException("got exception executing a SQL command", e);
		}
		return new SenderResult(new Message("<result><rowsprocessed>" + counter + "</rowsprocessed></result>"));
	}

	private void processResultSet (ResultSet resultset, FileOutputStream fos, int counter) throws SQLException, IOException {
		String rec_str = resultset.getString(1);
		if (log.isDebugEnabled()) {
			log.debug("iteration [{}] item [{}]", counter, rec_str);
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

	/** type of the optional status field which is set after the row is written to the file: timestamp */
	public void setStatusFieldType(String statusFieldType) {
		this.statusFieldType = statusFieldType;
	}

	/**
	 * Key of session variable that contains the name of the file to use.
	 * @ff.mandatory
	 */
	public void setFilenameSessionKey(String filenameSessionKey) {
		this.filenameSessionKey = filenameSessionKey;
	}

	/**
	 * If set <code>true</code> and the file already exists, the resultset rows are written to the end of the file.
	 * @ff.default false
	 */
	public void setAppend(boolean b) {
		append = b;
	}

	/**
	 * If set (and &gt;=0), this session key contains the maximum number of records which are processed.
	 * If <code>query</code> contains a group field (3), then also following records with the same group field value as the last record are processed
	 */
	public void setMaxRecordsSessionKey(String maxRecordsSessionKey) {
		this.maxRecordsSessionKey = maxRecordsSessionKey;
	}
}
