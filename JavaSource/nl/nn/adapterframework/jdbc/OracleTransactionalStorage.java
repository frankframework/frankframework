/*
 * $Log: OracleTransactionalStorage.java,v $
 * Revision 1.3  2005-09-07 15:37:07  europe\L190409
 * updated javadoc
 *
 * Revision 1.2  2005/08/24 15:49:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use getObject from superclass
 *
 * Revision 1.1  2005/08/18 13:31:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * JdbcTransactionalStorate descender-class for Oracle
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import nl.nn.adapterframework.core.SenderException;

import oracle.sql.BLOB;

import org.apache.commons.lang.StringUtils;

/**
 * Oracle implementation of {@link ITransactionalStorage}.
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
 * <tr><td>{@link #setTableName(String) tableName}</td><td>the name of the table messages are stored in</td><td>inprocstore</td></tr>
 * <tr><td>{@link #setKeyField(String) keyField}</td><td>the name of the column that contains the primary key of the table</td><td>messageKey</td></tr>
 * <tr><td>{@link #setIdField(String) idField}</td><td>the name of the column messageids are stored in</td><td>messageId</td></tr>
 * <tr><td>{@link #setCorrelationIdField(String) correlationIdField}</td><td>the name of the column correlation-ids are stored in</td><td>correlationId</td></tr>
 * <tr><td>{@link #setDateField(String) dateField}</td><td>the name of the column the timestamp is stored in</td><td>messageDate</td></tr>
 * <tr><td>{@link #setMessageField(String) messageField}</td><td>the name of the column message themselves are stored in</td><td>message</td></tr>
 * <tr><td>{@link #setSlotIdField(String) slotIdField}</td><td>the name of the column slotIds are stored in</td><td>slotId</td></tr>
 * <tr><td>{@link #setKeyFieldType(String) keyFieldType}</td><td>the type of the column that contains the primary key of the table</td><td>NUMBER(10)</td></tr>
 * <tr><td>{@link #setDateFieldType(String) dateFieldType}</td><td>the type of the column the timestamp is stored in</td><td>TIMESTAMP</td></tr>
 * <tr><td>{@link #setTextFieldType(String) textFieldType}</td><td>the type of the columns messageId and correlationId, slotId and comments are stored in. N.B. (100) is appended for id's, (1000) is appended for comments.</td><td>VARCHAR2</td></tr>
 * <tr><td>{@link #setMessageFieldType(String) messageFieldType}</td><td>the type of the column message themselves are stored in</td><td>BLOB</td></tr>
 * <tr><td>{@link #setSequenceName(String) sequenceName}</td><td>the name of the sequence used to generate the primary key</td><td>ibisstore_seq</td></tr>
 * </table>
 * </p>
 * 
 * The default uses the following objects:
 *  <pre>
 	CREATE TABLE ibisstore (
	  messageKey NUMBER(10) CONSTRAINT ibisstore_pk PRIMARY KEY,
	  slotId VARCHAR2(100), 
	  messageId VARCHAR2(100), 
	  correlationId VARCHAR2(100), 
	  messageDate TIMESTAMP, 
	  comments VARCHAR2(1000), 
	  message BLOB);

	CREATE INDEX ibisstore_idx ON ibisstore (slotId, messageDate);

	CREATE SEQUENCE ibisstore_seq START WITH 1 INCREMENT BY 1;
 *  </pre>
 * If these objects do not exist, Ibis tries to create them.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.3
 */
public class OracleTransactionalStorage extends JdbcTransactionalStorage {
	public static final String version = "$RCSfile: OracleTransactionalStorage.java,v $ $Revision: 1.3 $ $Date: 2005-09-07 15:37:07 $";

	private String sequenceName="ibisstore_seq";
		
	protected String updateBlobQuery;		
		
	public OracleTransactionalStorage() {
		super();
		setKeyFieldType("NUMBER(10)");
		setDateFieldType("TIMESTAMP");
		setTextFieldType("VARCHAR2");
		setMessageFieldType("BLOB");
	}
	    
	protected String getLogPrefix() {
		return "OracleTransactionalStorage ["+getName()+"] ";
	}
	

	protected void createStorage(Connection conn, Statement stmt) throws JdbcException {
		super.createStorage(conn,stmt);
		String query=null;
		try {
			query="CREATE SEQUENCE "+getSequenceName()+" START WITH 1 INCREMENT BY 1";
			log.debug(getLogPrefix()+"creating sequence for table ["+getTableName()+"] using query ["+query+"]");
			stmt.execute(query);
		} catch (SQLException e) {
			throw new JdbcException(getLogPrefix()+" executing query ["+query+"]", e);
		}
	}	

	
	protected void createQueryTexts() {
		super.createQueryTexts(); 
		insertQuery = "INSERT INTO "+getTableName()+" ("+
						getKeyField()+","+
						(StringUtils.isNotEmpty(getSlotId())?getSlotIdField()+",":"")+
						getIdField()+","+getCorrelationIdField()+","+getDateField()+","+getCommentField()+","+getMessageField()+
						") VALUES ("+getSequenceName()+".NEXTVAL,"+
						(StringUtils.isNotEmpty(getSlotId())?"'"+
						getSlotId()+"',":"")+"?,?,?,?,empty_blob())";
		updateBlobQuery = "SELECT "+getKeyField()+","+getMessageField()+
						  " FROM "+getTableName()+
						  getWhereClause(getIdField()+"=?"+
						  " AND " +getCorrelationIdField()+"=?"+
						  " AND "+getDateField()+"=?")+
						  " FOR UPDATE";
	}

	protected String storeMessageInDatabase(Connection conn, String messageId, String correlationId, Timestamp receivedDateTime, String comments, Serializable message) throws IOException, SQLException, SenderException {
		PreparedStatement stmt = null;
		try { 
			log.debug("preparing insert statement ["+insertQuery+"]");
			stmt = conn.prepareStatement(insertQuery);			
			stmt.clearParameters();
			stmt.setString(1,messageId);
			stmt.setString(2,correlationId);
			stmt.setTimestamp(3, receivedDateTime);
			stmt.setString(4, comments);
			stmt.execute();

			// now retrieve the key and update the blob
			log.debug("preparing update statement ["+updateBlobQuery+"]");
			stmt = conn.prepareStatement(updateBlobQuery);			
			stmt.clearParameters();
			stmt.setString(1,messageId);
			stmt.setString(2,correlationId);
			stmt.setTimestamp(3, receivedDateTime);

			ResultSet rs = null;
			try {
				rs = stmt.executeQuery();
				if (!rs.next()) {
					throw new SenderException("could not retrieve row for stored message ["+ messageId+"]");
				}
				String newKey = rs.getString(1);
				BLOB blob = (BLOB)rs.getBlob(2);
				OutputStream outstream = blob.setBinaryStream(1L);
				ObjectOutputStream oos = new ObjectOutputStream(outstream);
				oos.writeObject(message);
				oos.close();
				outstream.close();
				return newKey;
				
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


	public void setSequenceName(String string) {
		sequenceName = string;
	}
	public String getSequenceName() {
		return sequenceName;
	}



}
