/*
 * $Log: ResultSet2FileSender.java,v $
 * Revision 1.2  2012-11-20 13:27:51  europe\m168309
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
 * <tr><td>{@link #setQuery(String) query}</td><td>query that returns a row to be processed. Must contain a message field which is written to a file and optionally a status field</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStatusFieldType(String) statusFieldType}</td><td>type of the optional status field which is set after the row is written to the file: timestamp</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFileNameSessionKey(String) fileNameSessionKey}</td><td>the session key that contains the name of the file to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAppend(boolean) append}</td><td>when set <code>true</code> and the file already exists, the resultset rows are written to the end of the file</td><td>false</td></tr>
 * </table>
 * </p>
 * 
 * @version Id
 * @author  Peter Leeuwenburgh
 */
public class ResultSet2FileSender extends FixedQuerySender {
	private String fileNameSessionKey;
	private String statusFieldType;
	private boolean append = false;

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
		FileOutputStream fos=null;
		try {
			fos = new FileOutputStream(fileName, isAppend());
			PreparedStatement statement = getStatement(connection, correlationID, null, true);
			resultset = statement.executeQuery();
			while (resultset.next()) {
				counter++;
				String rec_str = resultset.getString(1);
				if (log.isDebugEnabled()) {
					log.debug("iteration [" + counter + "] item [" + rec_str + "]");
				} 
				if ("timestamp".equalsIgnoreCase(getStatusFieldType())) {
					//TODO: statusFieldType is nu altijd een timestamp (dit moeten ook andere types kunnen zijn)
					resultset.updateTimestamp(2 , new Timestamp((new Date()).getTime()));
					resultset.updateRow();
				}
				fos.write(rec_str.getBytes());
				fos.write(eolArray);
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
}