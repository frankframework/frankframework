/*
 * $Log: JdbcTransactionalStorage.java,v $
 * Revision 1.5  2005-07-19 14:59:02  europe\L190409
 * adapted to an implementation extending IMessageBrowser
 *
 * Revision 1.4  2004/03/31 12:04:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.3  2004/03/26 10:43:08  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.2  2004/03/25 13:48:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enhanced creation of table
 *
 * Revision 1.1  2004/03/24 13:28:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * JDBC implementation of {@link ITransactionalStorage}.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.JdbcQuerySenderBase</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceName(String) datasourceName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceNameXA(String) datasourceNameXA}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTableName(String) tableName}</td><td>the name of the table messages are stored in</td><td>inprocstore</td></tr>
 * <tr><td>{@link #setKeyField(String) keyField}</td><td>the name of the column that contains the primary key of the table</td><td>messageKey</td></tr>
 * <tr><td>{@link #setIdField(String) idField}</td><td>the name of the column messageids are stored in</td><td>messageId</td></tr>
 * <tr><td>{@link #setCorrelationIdField(String) correlationIdField}</td><td>the name of the column correlation-ids are stored in</td><td>correlationId</td></tr>
 * <tr><td>{@link #setDateField(String) dateField}</td><td>the name of the column the timestamp is stored in</td><td>messageDate</td></tr>
 * <tr><td>{@link #setMessageField(String) messageField}</td><td>the name of the column message themselves are stored in</td><td>message</td></tr>
 * <tr><td>{@link #setKeyFieldType(String) keyFieldType}</td><td>the type of the column that contains the primary key of the table</td><td>INT DEFAULT AUTOINCREMENT</td></tr>
 * <tr><td>{@link #setDataFieldType(String) dateFieldType}</td><td>the type of the column the timestamp is stored in</td><td>TIMESTAMP</td></tr>
 * <tr><td>{@link #setMessageFieldType(String) messageFieldType}</td><td>the type of the column message themselves are stored in</td><td>LONG BINARY</td></tr>
 * </table>
 * </p>
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class JdbcTransactionalStorage extends JdbcFacade implements ITransactionalStorage {
	public static final String version = "$RCSfile: JdbcTransactionalStorage.java,v $ $Revision: 1.5 $ $Date: 2005-07-19 14:59:02 $";
	
    private String tableName="ibisstore";
	private String keyField="messageKey";
    private String idField="messageId";
	private String correlationIdField="correlationId";
	private String dateField="messageDate";
	private String commentField="comment";
	private String messageField="message";
    
	protected static final int MAXIDLEN=100;		
	protected static final int MAXCOMMENTLEN=1000;		
    // the following values are only used when the table is created. 
    // TODO: handle sizes of messages and ids more intelligently
	private String keyFieldType="INT DEFAULT AUTOINCREMENT";
	private String dateFieldType="TIMESTAMP";
	private String messageFieldType="LONG BINARY";
	
	private String insertQuery;
	private String deleteQuery;
	private String selectKeyQuery;
	private String selectListQuery;
	private String selectDataQuery;
	
	public JdbcTransactionalStorage() {
		super();
		setTransacted(true);
	}
	    
	protected String getLogPrefix() {
		return "JdbcTransactionalStorage ["+getName()+"] ";
	}
	
	
	public void open() {
	}

	public void close() {
	}
	
	
	public void createTable(Connection conn) throws JdbcException {
		Statement stmt;
		try {
			stmt = conn.createStatement();
		} catch (SQLException e) {
			throw new JdbcException(getLogPrefix()+" creating statement to create table:", e);
		}
		String query="CREATE TABLE "+getTableName()+" ("+
						getKeyField()+" "+getKeyFieldType()+" PRIMARY KEY, "+
						getIdField()+" VARCHAR("+MAXIDLEN+"), "+
						getCorrelationIdField()+" VARCHAR("+MAXIDLEN+"), "+
						getDateField()+" "+getDateFieldType()+", "+
						getCommentField()+" VARCHAR("+MAXCOMMENTLEN+"), "+
						getMessageField()+" "+getMessageFieldType()+
					  ")";
		try {
			log.debug(getLogPrefix()+"creating table ["+getTableName()+"] using query ["+query+"]");
			stmt.execute(query);
			stmt.close();
		} catch (SQLException e) {
			throw new JdbcException(getLogPrefix()+" executing query ["+query+"]", e);
		}
	}	
	
	protected void createQueryTexts() {
		insertQuery = "INSERT INTO "+getTableName()+" ("+
						getIdField()+","+getCorrelationIdField()+","+getDateField()+","+getCommentField()+","+getMessageField()+
						") VALUES (?,?,?,?,?)";
		deleteQuery = "DELETE FROM "+getTableName()+ " WHERE "+getKeyField()+"=?";
		selectKeyQuery = "SELECT max("+getKeyField()+") FROM "+getTableName()+ 
						" WHERE "+getIdField()+"=?"+
						" AND " +getCorrelationIdField()+"=?"+
						" AND "+getDateField()+"=?";
		selectListQuery = "SELECT "+getKeyField()+","+getIdField()+","+getCorrelationIdField()+","+getDateField()+","+getCommentField()+
						  " FROM "+getTableName()+
						  " ORDER BY "+getDateField();
		selectDataQuery = "SELECT "+getMessageField()+
						  " FROM "+getTableName()+ " WHERE "+getKeyField()+"=?";
	}


	
	/**
	 * Creates a connection, checks if the table is existing and creates it when necessary
	 */
	public void configure() throws ConfigurationException {
//		super.configure();
		try {
			createQueryTexts();
			Connection conn = getConnection();
			boolean tableMustBeCreated;
			try {
				tableMustBeCreated = !JdbcUtil.tableExists(conn, getTableName());
			} catch (SQLException e) {
				log.warn(getLogPrefix()+"exception determining existence of table ["+getTableName()+"] for transactional storage, trying to create anyway."+ e.getMessage());
				tableMustBeCreated=true;
			}
			if (tableMustBeCreated) {
				log.info(getLogPrefix()+"creating table ["+getTableName()+"] for transactional storage");
				createTable(conn);
			}
			conn.close();
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		} catch (SQLException e) {
			throw new ConfigurationException(getLogPrefix()+"exception creating table ["+getTableName()+"]",e);
		} 
	}


	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" in table ["+getTableName()+"]";
	}



	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, Serializable message) throws SenderException {
		Connection conn;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new SenderException(e);
		}
		try {
			Timestamp receivedDateTime = new Timestamp(receivedDate.getTime());
			log.debug("preparing insert statement ["+insertQuery+"]");
			PreparedStatement stmt = conn.prepareStatement(insertQuery);			
			stmt.clearParameters();
			if (messageId.length()>MAXIDLEN) {
				messageId=messageId.substring(0,MAXIDLEN);
			}
			stmt.setString(1,messageId);
			if (correlationId.length()>MAXIDLEN) {
				correlationId=correlationId.substring(0,MAXIDLEN);
			}
			stmt.setString(2,correlationId);
			stmt.setTimestamp(3, receivedDateTime);
			if (comments.length()>MAXCOMMENTLEN) {
				comments=comments.substring(0,MAXCOMMENTLEN);
			}
			stmt.setString(4, comments);
		
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(message);
			stmt.setBytes(5, out.toByteArray());

			stmt.execute();

			stmt = conn.prepareStatement(selectKeyQuery);			
			stmt.clearParameters();
			stmt.setString(1,messageId);
			stmt.setString(2,correlationId);
			stmt.setTimestamp(3, receivedDateTime);

			ResultSet rs = stmt.executeQuery();
			
			if (!rs.next()) {
				throw new SenderException("did not retrieve key for stored message ["+ messageId+"]");
			}

			return rs.getString(1);
			
		} catch (SQLException e) {
			throw new SenderException(e);
		} catch (IOException e) {
			throw new SenderException("cannot serialize message",e);
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				log.error("error closing JdbcConnection", e);
			}
		}
		
	}

	private class ResultSetIterator implements IMessageBrowsingIterator {
		
		Connection conn;
		ResultSet  rs;
		boolean current;
		boolean eof;
		
		ResultSetIterator(Connection conn, ResultSet rs) throws SQLException {
			this.conn=conn;
			this.rs=rs;
			current=false;
			eof=false;
		}

		private void advance() throws ListenerException {
			if (!current && !eof) {
				try {
					current = rs.next();
					eof = !current;
				} catch (SQLException e) {
					throw new ListenerException(e);
				}
			}
		}

		public boolean hasNext() throws ListenerException {
			advance();
			return current;
		}

		public Object next() throws ListenerException {
			if (!current) {
				advance();
			}
			if (!current) {
				throw new ListenerException("read beyond end of resultset");
			}
			current=false;
			return rs;
		}

		public void close() throws ListenerException {
			try {
				rs.close();
				conn.close();
			} catch (SQLException e) {
				throw new ListenerException("error closing browser session",e);
			}
		} 
	}

	public IMessageBrowsingIterator getIterator() throws ListenerException {
		Connection conn;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new ListenerException(e);
		}
		try {
			log.debug("preparing selectListQuery ["+selectListQuery+"]");
			PreparedStatement stmt = conn.prepareStatement(selectListQuery);
			ResultSet rs =  stmt.executeQuery();
			return new ResultSetIterator(conn,rs);
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}



	public void deleteMessage(String messageId) throws ListenerException {
		Connection conn;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new ListenerException(e);
		}
		try {
			PreparedStatement stmt = conn.prepareStatement(deleteQuery);			
			stmt.clearParameters();
			stmt.setString(1,messageId);
			stmt.execute();
			
		} catch (SQLException e) {
			throw new ListenerException(e);
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				log.error("error closing JdbcConnection", e);
			}
		}
	}


	public Object browseMessage(String messageId) throws ListenerException {
		Connection conn;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new ListenerException(e);
		}
		try {
			PreparedStatement stmt = conn.prepareStatement(selectDataQuery);			
			stmt.clearParameters();
			stmt.setString(1,messageId);
			ResultSet rs =  stmt.executeQuery();

			if (!rs.next()) {
				throw new ListenerException("could not retrieve message for messageid ["+ messageId+"]");
			}

			ByteArrayInputStream in = new ByteArrayInputStream(rs.getBytes(1));
			ObjectInputStream ois = new ObjectInputStream(in);
			
			Object result = ois.readObject();
			return result;
			
		} catch (Exception e) {
			throw new ListenerException("cannot deserialize message",e);
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				log.error("error closing JdbcConnection", e);
			}
		}
	}

	public Object getMessage(String messageId) throws ListenerException {
		Object result = browseMessage(messageId);
		deleteMessage(messageId);
		return result;
	}



	public String getId(Object iteratorItem)  throws ListenerException{
		ResultSet row = (ResultSet) iteratorItem;
		try {
			return row.getString(getKeyField());
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}

	public String getOriginalId(Object iteratorItem) throws ListenerException {
		ResultSet row = (ResultSet) iteratorItem;
		try {
			return row.getString(getIdField());
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}

	public String getCorrelationId(Object iteratorItem) throws ListenerException {
		ResultSet row = (ResultSet) iteratorItem;
		try {
			return row.getString(getCorrelationIdField());
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}

	public Date getInsertDate(Object iteratorItem)  throws ListenerException{
		ResultSet row = (ResultSet) iteratorItem;
		try {
			return row.getTimestamp(getDateField());
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}

	public String getCommentString(Object iteratorItem) throws ListenerException {
		ResultSet row = (ResultSet) iteratorItem;
		try {
			return row.getString(getCommentField());
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}

	/**
	 * Sets the name of the table messages are stored in.
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getTableName() {
		return tableName;
	}

	/**
	 * Sets the name of the column messageids are stored in.
	 */
	public void setIdField(String idField) {
		this.idField = idField;
	}
	public String getIdField() {
		return idField;
	}

	/**
	 * Sets the name of the column message themselves are stored in.
	 */
	public void setMessageField(String messageField) {
		this.messageField = messageField;
	}
	public String getMessageField() {
		return messageField;
	}

	public String getCommentField() {
		return commentField;
	}

	public String getDateField() {
		return dateField;
	}

	public void setCommentField(String string) {
		commentField = string;
	}

	public void setDateField(String string) {
		dateField = string;
	}


	public String getCorrelationIdField() {
		return correlationIdField;
	}

	public void setCorrelationIdField(String string) {
		correlationIdField = string;
	}

	public String getMessageFieldType() {
		return messageFieldType;
	}

	public String getKeyFieldType() {
		return keyFieldType;
	}

	public void setMessageFieldType(String string) {
		messageFieldType = string;
	}

	public void setKeyFieldType(String string) {
		keyFieldType = string;
	}

	public String getKeyField() {
		return keyField;
	}

	public void setKeyField(String string) {
		keyField = string;
	}

	public String getDateFieldType() {
		return dateFieldType;
	}

	public void setDateFieldType(String string) {
		dateFieldType = string;
	}

}
