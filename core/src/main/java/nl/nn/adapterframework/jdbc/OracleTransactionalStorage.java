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
package nl.nn.adapterframework.jdbc;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

/**
 *
 * 
 * The default uses the following objects:
 *  <pre>
	CREATE TABLE IBISSTORE
	(
	MESSAGEKEY NUMBER(10),
	TYPE CHAR(1 CHAR),
	SLOTID VARCHAR2(100 CHAR),
	HOST VARCHAR2(100 CHAR),
	MESSAGEID VARCHAR2(100 CHAR),
	CORRELATIONID VARCHAR2(100 CHAR),
	MESSAGEDATE TIMESTAMP(6),
	COMMENTS VARCHAR2(1000 CHAR),
	MESSAGE BLOB,
	CONSTRAINT PK_IBISSTORE PRIMARY KEY (MESSAGEKEY)
	);
	
	CREATE INDEX IX_IBISSTORE ON IBISSTORE (TYPE, SLOTID, MESSAGEDATE);
	CREATE SEQUENCE SEQ_IBISSTORE;

	GRANT DELETE, INSERT, SELECT, UPDATE ON IBISSTORE TO <rolenaam>;
	GRANT SELECT ON SEQ_IBISSTORE TO <rolenaam>;
	GRANT SELECT ON SYS.DBA_PENDING_TRANSACTIONS TO <rolenaam>;
 *  </pre>
 * If these objects do not exist, Ibis will try to create them if the attribute createTable="true".
 * 
 * @author  Gerrit van Brakel
 * @since 	4.3
 * @deprecated The functionality of the OracleTransactionalStorage has been incorporated in de JdbcTransactionalStorage.
 */
public class OracleTransactionalStorage extends JdbcTransactionalStorage {

	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = "Class OracleTransactionalStorage is no longer maintained. Please replace with JdbcTransactionalStorage";
		configWarnings.add(log, msg);
		super.configure();
	}



//	private String sequenceName="ibisstore_seq";
//		
//	protected String updateBlobQuery;		
//		
//	public OracleTransactionalStorage() {
//		super();
//		setKeyFieldType("NUMBER(10)");
//		setDateFieldType("TIMESTAMP");
//		setTextFieldType("VARCHAR2");
//		setMessageFieldType("BLOB");
//	}
//	    
//	protected String getLogPrefix() {
//		return "OracleTransactionalStorage ["+getName()+"] ";
//	}
//	
//
//
//	
//	protected void createQueryTexts() {
//		super.createQueryTexts(); 
//		insertQuery = "INSERT INTO "+getTableName()+" ("+
//						getKeyField()+","+
//						(StringUtils.isNotEmpty(getSlotId())?getSlotIdField()+",":"")+
//						getIdField()+","+getCorrelationIdField()+","+getDateField()+","+getCommentField()+","+getMessageField()+
//						") VALUES ("+getSequenceName()+".NEXTVAL,"+
//						(StringUtils.isNotEmpty(getSlotId())?"'"+
//						getSlotId()+"',":"")+"?,?,?,?,empty_blob())";
//		updateBlobQuery = "SELECT "+getKeyField()+","+getMessageField()+
//						  " FROM "+getTableName()+
//						  getWhereClause(getIdField()+"=?"+
//						  " AND " +getCorrelationIdField()+"=?"+
//						  " AND "+getDateField()+"=?")+
//						  " FOR UPDATE";
//	}

//	protected void createStorage(Connection conn, Statement stmt) throws JdbcException {
//		super.createStorage(conn,stmt);
//		String query=null;
//		try {
//			query="CREATE SEQUENCE "+getSequenceName()+" START WITH 1 INCREMENT BY 1";
//			log.debug(getLogPrefix()+"creating sequence for table ["+getTableName()+"] using query ["+query+"]");
//			stmt.execute(query);
//		} catch (SQLException e) {
//			throw new JdbcException(getLogPrefix()+" executing query ["+query+"]", e);
//		}
//	}	

//
//	protected String storeMessageInDatabase(Connection conn, String messageId, String correlationId, Timestamp receivedDateTime, String comments, Serializable message) throws IOException, SenderException, JdbcException, SQLException {
//		PreparedStatement stmt = null;
//		try { 
//			log.debug("preparing insert statement ["+insertQuery+"]");
//			stmt = conn.prepareStatement(insertQuery);			
//			stmt.clearParameters();
//			stmt.setString(1,messageId);
//			stmt.setString(2,correlationId);
//			stmt.setTimestamp(3, receivedDateTime);
//			stmt.setString(4, comments);
//			stmt.execute();
//
//			// now retrieve the key and update the blob
//			log.debug("preparing update statement ["+updateBlobQuery+"]");
//			stmt = conn.prepareStatement(updateBlobQuery);			
//			stmt.clearParameters();
//			stmt.setString(1,messageId);
//			stmt.setString(2,correlationId);
//			stmt.setTimestamp(3, receivedDateTime);
//
//			ResultSet rs = null;
//			try {
//				rs = stmt.executeQuery();
//				if (!rs.next()) {
//					throw new SenderException("could not retrieve row for stored message ["+ messageId+"]");
//				}
//				String newKey = rs.getString(1);
////				BLOB blob = (BLOB)rs.getBlob(2);
//				OutputStream outstream = JdbcUtil.getBlobUpdateOutputStream(rs,2);
//				ObjectOutputStream oos = new ObjectOutputStream(outstream);
//				oos.writeObject(message);
//				oos.close();
//				outstream.close();
//				return newKey;
//				
//			} finally {
//				if (rs!=null) {
//					rs.close();
//				}
//			}
//		} finally {
//			if (stmt!=null) {
//				stmt.close();
//			}
//		}
//	}
//
//


}
