/*
 * $Log: JdbcTransactionalStorage.java,v $
 * Revision 1.17  2006-12-12 09:57:37  europe\L190409
 * restore jdbc package
 *
 * Revision 1.15  2005/12/28 08:43:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.14  2005/10/26 13:34:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.13  2005/10/18 07:15:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reduced number of exceptions thrown
 *
 * Revision 1.12  2005/09/22 16:06:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added createTable attribute, to create table only when desired
 *
 * Revision 1.11  2005/09/07 15:37:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.10  2005/08/24 15:48:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * retrieve object using generic getBlobInputStream()
 *
 * Revision 1.9  2005/08/18 13:30:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made descender-class for Oracle
 *
 * Revision 1.8  2005/08/17 16:15:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Oracle compatiblity, attribuut dbms, (oracle or cloudscape)
 *
 * Revision 1.7  2005/08/04 15:39:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * quotes around slotId at insert
 *
 * Revision 1.6  2005/07/28 07:36:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added slotId attribute
 *
 * Revision 1.5  2005/07/19 14:59:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * JDBC implementation of {@link ITransactionalStorage}.
 * 
 * The default implementation works for Cloudscape databases.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jdbc.JdbcTransactionalStorage</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSlotId(String) slotId}</td><td>optional identifier for this storage, to be able to share the physical table between a number of receivers</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDatasourceNameXA(String) datasourceNameXA}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTableName(String) tableName}</td><td>the name of the table messages are stored in</td><td>ibisstore</td></tr>
 * <tr><td>{@link #setCreateTable(boolean) createTable}</td><td>when set to <code>true</code>, the table is created if it does not exist</td><td>false</td></tr>
 * <tr><td>{@link #setKeyField(String) keyField}</td><td>the name of the column that contains the primary key of the table</td><td>messageKey</td></tr>
 * <tr><td>{@link #setIdField(String) idField}</td><td>the name of the column messageids are stored in</td><td>messageId</td></tr>
 * <tr><td>{@link #setCorrelationIdField(String) correlationIdField}</td><td>the name of the column correlation-ids are stored in</td><td>correlationId</td></tr>
 * <tr><td>{@link #setDateField(String) dateField}</td><td>the name of the column the timestamp is stored in</td><td>messageDate</td></tr>
 * <tr><td>{@link #setMessageField(String) messageField}</td><td>the name of the column message themselves are stored in</td><td>message</td></tr>
 * <tr><td>{@link #setSlotIdField(String) slotIdField}</td><td>the name of the column slotIds are stored in</td><td>slotId</td></tr>
 * <tr><td>{@link #setKeyFieldType(String) keyFieldType}</td><td>the type of the column that contains the primary key of the table</td><td>INT DEFAULT AUTOINCREMENT</td></tr>
 * <tr><td>{@link #setDateFieldType(String) dateFieldType}</td><td>the type of the column the timestamp is stored in</td><td>TIMESTAMP</td></tr>
 * <tr><td>{@link #setTextFieldType(String) textFieldType}</td><td>the type of the columns messageId and correlationId, slotId and comments are stored in. N.B. (100) is appended for id's, (1000) is appended for comments.</td><td>VARCHAR</td></tr>
 * <tr><td>{@link #setMessageFieldType(String) messageFieldType}</td><td>the type of the column message themselves are stored in</td><td>LONG BINARY</td></tr>
 * </table>
 * </p>
 * 
 * The default uses the following objects:
 *  <pre>
	CREATE TABLE ibisstore (
	  messageKey INT DEFAULT AUTOINCREMENT CONSTRAINT ibisstore_pk PRIMARY KEY,
	  slotId VARCHAR(100), 
	  messageId VARCHAR(100), 
	  correlationId VARCHAR(100), 
	  messageDate TIMESTAMP, 
	  comments VARCHAR(1000), 
	  message LONG BINARY);

	CREATE INDEX ibisstore_idx ON ibisstore (slotId, messageDate);
 *  </pre>
 * If these objects do not exist, Ibis will try to create them if the attribute createTable="true".
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class JdbcTransactionalStorage extends JdbcFacade implements ITransactionalStorage {
	public static final String version = "$RCSfile: JdbcTransactionalStorage.java,v $ $Revision: 1.17 $ $Date: 2006-12-12 09:57:37 $";
	
	// the following currently only for debug.... 
	boolean checkIfTableExists=true;
	boolean forceCreateTable=false;

	boolean createTable=false;
    private String tableName="ibisstore";
	private String keyField="messageKey";
    private String idField="messageId";
	private String correlationIdField="correlationId";
	private String dateField="messageDate";
	private String commentField="comments";
	private String messageField="message";
	private String slotIdField="slotId";
	private String slotId=null;
   
	protected static final int MAXIDLEN=100;		
	protected static final int MAXCOMMENTLEN=1000;		
    // the following values are only used when the table is created. 
	private String keyFieldType="INT DEFAULT AUTOINCREMENT";
	private String dateFieldType="TIMESTAMP";
	private String messageFieldType="LONG BINARY";
	private String textFieldType="VARCHAR";
	
	protected String insertQuery;
	protected String deleteQuery;
	protected String selectKeyQuery;
	protected String selectListQuery;
	protected String selectDataQuery;
	
	public JdbcTransactionalStorage() {
		super();
		setTransacted(true);
	}
	    
	protected String getLogPrefix() {
		return "JdbcTransactionalStorage ["+getName()+"] ";
	}
	
	/**
	 * Creates a connection, checks if the table is existing and creates it when necessary
	 */
	public void configure() throws ConfigurationException {
//		super.configure();
		createQueryTexts();
	}


	
	public void open() throws SenderException {
		try {
			initialize();
		} catch (JdbcException e) {
			throw new SenderException(e);
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix()+"exception creating table ["+getTableName()+"]",e);
		} 
	}

	public void close() {
	}

	/**
	 *	Checks if table exists, and creates when necessary. 
	 */
	public void initialize() throws JdbcException, SQLException, SenderException {

		Connection conn = getConnection();
		try {
			boolean tableMustBeCreated;

			if (checkIfTableExists) {
				try {
					tableMustBeCreated = !JdbcUtil.tableExists(conn, getTableName());
					if (!isCreateTable() && tableMustBeCreated) {
						throw new SenderException("table ["+getTableName()+"] does not exist");
					}
					 log.info("table ["+getTableName()+"] does "+(tableMustBeCreated?"NOT ":"")+"exist");
				} catch (SQLException e) {
					log.warn(getLogPrefix()+"exception determining existence of table ["+getTableName()+"] for transactional storage, trying to create anyway."+ e.getMessage());
					tableMustBeCreated=true;
				}
			} else {
				log.info("did not check for existence of table ["+getTableName()+"]");
				tableMustBeCreated = false;
			}

			if (isCreateTable() && tableMustBeCreated || forceCreateTable) {
				log.info(getLogPrefix()+"creating table ["+getTableName()+"] for transactional storage");
				Statement stmt = conn.createStatement();
				try {
					createStorage(conn, stmt);
				} finally {
					stmt.close();
					conn.commit();
				}
			}
		} finally {
			conn.close();
		}
	}

	
	protected void createQueryTexts() {
		insertQuery = "INSERT INTO "+getTableName()+" ("+
						(StringUtils.isNotEmpty(getSlotId())?getSlotIdField()+",":"")+
						getIdField()+","+getCorrelationIdField()+","+getDateField()+","+getCommentField()+","+getMessageField()+
						") VALUES ("+
						(StringUtils.isNotEmpty(getSlotId())?"'"+
						getSlotId()+"',":"")+"?,?,?,?,?)";
		deleteQuery = "DELETE FROM "+getTableName()+ getWhereClause(getKeyField()+"=?");
		selectKeyQuery = "SELECT max("+getKeyField()+") FROM "+getTableName()+ 
						getWhereClause(getIdField()+"=?"+
									" AND " +getCorrelationIdField()+"=?"+
									" AND "+getDateField()+"=?");
		selectListQuery = "SELECT "+getKeyField()+","+getIdField()+","+getCorrelationIdField()+","+getDateField()+","+getCommentField()+
						  " FROM "+getTableName()+ getWhereClause(null)+
						  " ORDER BY "+getDateField();
		selectDataQuery = "SELECT "+getMessageField()+
						  " FROM "+getTableName()+ getWhereClause(getKeyField()+"=?");
	}

	
	/**
	 *	Acutaly creates storage. Can be overridden in descender classes 
	 */
	protected void createStorage(Connection conn, Statement stmt) throws JdbcException {
		String query=null;
		try {
			query="CREATE TABLE "+getTableName()+" ("+
						getKeyField()+" "+getKeyFieldType()+" CONSTRAINT " +getTableName()+ "_pk PRIMARY KEY, "+
						(StringUtils.isNotEmpty(getSlotId())? getSlotIdField()+" "+getTextFieldType()+"("+MAXIDLEN+"), ":"")+
						getIdField()+" "+getTextFieldType()+"("+MAXIDLEN+"), "+
						getCorrelationIdField()+" "+getTextFieldType()+"("+MAXIDLEN+"), "+
						getDateField()+" "+getDateFieldType()+", "+
						getCommentField()+" "+getTextFieldType()+"("+MAXCOMMENTLEN+"), "+
						getMessageField()+" "+getMessageFieldType()+
					  ")";
					  
			log.debug(getLogPrefix()+"creating table ["+getTableName()+"] using query ["+query+"]");
			stmt.execute(query);
			query = "CREATE INDEX "+getTableName()+"_idx ON "+getTableName()+"("+(StringUtils.isNotEmpty(getSlotId())?getSlotIdField()+",":"")+getDateField()+")";				
			log.debug(getLogPrefix()+"creating index ["+getTableName()+"_idx] using query ["+query+"]");
			stmt.execute(query);
		} catch (SQLException e) {
			throw new JdbcException(getLogPrefix()+" executing query ["+query+"]", e);
		}
	}	
	
	protected String getWhereClause(String clause) {
		if (StringUtils.isEmpty(getSlotId())) {
			if (StringUtils.isEmpty(clause)) {
				return "";
			} else {
				return " WHERE "+clause; 
			}
		} else {
			String result = " WHERE "+getSelector();
			if (StringUtils.isNotEmpty(clause)) {
				result += " AND "+clause;
			}
			return result;
		}
	}


	/**
	 * Retrieves the value of the primary key for the record just inserted. 
	 */
	protected String retrieveKey(Connection conn, String messageId, String correlationId, Timestamp receivedDateTime) throws SQLException, SenderException {
		PreparedStatement stmt=null;
		
		try {			
			stmt = conn.prepareStatement(selectKeyQuery);			
			stmt.clearParameters();
			stmt.setString(1,messageId);
			stmt.setString(2,correlationId);
			stmt.setTimestamp(3, receivedDateTime);
	
			ResultSet rs = null;
			try {
				rs = stmt.executeQuery();
				if (!rs.next()) {
					throw new SenderException("could not retrieve key for stored message ["+ messageId+"]");
				}
				return rs.getString(1);
			} finally {
				if (rs!=null) {
					rs.close();
				}
			}
		} finally {
			if (stmt!=null) {
				stmt.close();
			}
		}
	}

	protected String storeMessageInDatabase(Connection conn, String messageId, String correlationId, Timestamp receivedDateTime, String comments, Serializable message) throws IOException, SQLException, JdbcException, SenderException {
		PreparedStatement stmt = null;
		try { 
			log.debug("preparing insert statement ["+insertQuery+"]");
			stmt = conn.prepareStatement(insertQuery);			
			stmt.clearParameters();
			stmt.setString(1,messageId);
			stmt.setString(2,correlationId);
			stmt.setTimestamp(3, receivedDateTime);
			stmt.setString(4, comments);
	
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(out);
			oos.writeObject(message);
			stmt.setBytes(5, out.toByteArray());
	
			stmt.execute();
			return null;
		} finally {
			if (stmt!=null) {
				stmt.close();
			}
		}
	}


	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, Serializable message) throws SenderException {
		Connection conn;
		String result;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new SenderException(e);
		}
		try {
			Timestamp receivedDateTime = new Timestamp(receivedDate.getTime());
			if (messageId.length()>MAXIDLEN) {
				messageId=messageId.substring(0,MAXIDLEN);
			}
			if (correlationId.length()>MAXIDLEN) {
				correlationId=correlationId.substring(0,MAXIDLEN);
			}
			if (comments.length()>MAXCOMMENTLEN) {
				comments=comments.substring(0,MAXCOMMENTLEN);
			}
			result = storeMessageInDatabase(conn, messageId, correlationId, receivedDateTime, comments, message);
			if (result==null) {
				result=retrieveKey(conn,messageId,correlationId,receivedDateTime);
			}
			return result;
			
		} catch (Exception e) {
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

	protected String getSelector() {
		if (StringUtils.isEmpty(getSlotId())) {
			return null;
		}
		return getSlotIdField()+"='"+getSlotId()+"'";
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


	protected Object retrieveObject(ResultSet rs, int columnIndex) throws ClassNotFoundException, JdbcException, IOException, SQLException {
		ObjectInputStream ois = new ObjectInputStream(JdbcUtil.getBlobInputStream(rs, columnIndex));
		Object result = ois.readObject();
		ois.close();
		return result;
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
			
			Object result = retrieveObject(rs,1);
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

	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" in table ["+getTableName()+"]";
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

	public void setMessageFieldType(String string) {
		messageFieldType = string;
	}
	public String getMessageFieldType() {
		return messageFieldType;
	}

	public void setKeyFieldType(String string) {
		keyFieldType = string;
	}
	public String getKeyFieldType() {
		return keyFieldType;
	}

	public void setDateFieldType(String string) {
		dateFieldType = string;
	}
	public String getDateFieldType() {
		return dateFieldType;
	}

	public void setTextFieldType(String string) {
		textFieldType = string;
	}
	public String getTextFieldType() {
		return textFieldType;
	}

	public String getKeyField() {
		return keyField;
	}

	public void setKeyField(String string) {
		keyField = string;
	}

	public String getSlotId() {
		return slotId;
	}

	public void setSlotId(String string) {
		slotId = string;
	}

	public String getSlotIdField() {
		return slotIdField;
	}

	public void setSlotIdField(String string) {
		slotIdField = string;
	}



	public void setCreateTable(boolean b) {
		createTable = b;
	}
	public boolean isCreateTable() {
		return createTable;
	}

}
