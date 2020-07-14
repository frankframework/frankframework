/*
   Copyright 2020 WeAreFrank!

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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.jdbc.dbms.JdbcSession;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * JDBC implementation of {@link IMessageBrowser}.
 * 
 * 
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 * @since 4.1
 */
public class JdbcMessageBrowser<M> extends JdbcFacade implements IMessageBrowser<M> {

	public final static TransactionDefinition TXREQUIRED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
	
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
	private String expiryDateField="expiryDate";
	private String labelField="label";
	private String slotId=null;
	private String typeField="type";
	private String type = "";
	private String hostField="host";
	private boolean blobsCompressed=true;
	private String indexName="IX_IBISSTORE";

	private String prefix="";

	private String hideRegex = null;
	private String hideMethod = "all";
	
	private String order;
	private String messagesOrder=AppConstants.getInstance().getString("browse.messages.order","DESC");
	private String errorsOrder=AppConstants.getInstance().getString("browse.errors.order","ASC");
   
	protected static final int MAXIDLEN=100;		
	protected static final int MAXCIDLEN=256;		
	protected static final int MAXLABELLEN=1000;		
    // the following values are only used when the table is created. 
	private String keyFieldType="";
	private String dateFieldType="";
	private String messageFieldType="";
	private String textFieldType="";

	private PlatformTransactionManager txManager;

	protected String deleteQuery;
	protected String selectListQuery;
	protected String selectContextQuery;
	protected String selectDataQuery;
	protected String checkMessageIdQuery;
	protected String checkCorrelationIdQuery;
	protected String getMessageCountQuery;

	protected boolean selectKeyQueryIsDbmsSupported;
	
	// the following for Oracle
	private String sequenceName="seq_ibisstore";
	protected String updateBlobQuery;		
	
	private static final String CONTROL_PROPERTY_PREFIX="jdbc.storage.";
	private static final String PROPERTY_USE_INDEX_HINT=CONTROL_PROPERTY_PREFIX+"useIndexHint";
	private static final String PROPERTY_USE_FIRST_ROWS_HINT=CONTROL_PROPERTY_PREFIX+"useFirstRowsHint";
	private static final String PROPERTY_USE_PARAMETERS=CONTROL_PROPERTY_PREFIX+"useParameters";
	private static final String PROPERTY_ASSUME_PRIMARY_KEY_UNIQUE=CONTROL_PROPERTY_PREFIX+"assumePrimaryKeyUnique";
	
	private boolean useIndexHint;
	private boolean useFirstRowsHint;
	private boolean useParameters;
	private boolean assumePrimaryKeyUnique;


	public JdbcMessageBrowser() {
		super();
		setTransacted(true);
	}

	@Override
	protected String getLogPrefix() {
		return "JdbcMessageBrowser ["+getName()+"] ";
	}

	private void setOperationControls() {
		AppConstants ac = AppConstants.getInstance();
		useIndexHint = ac.getBoolean(PROPERTY_USE_INDEX_HINT, false);
		useFirstRowsHint = ac.getBoolean(PROPERTY_USE_FIRST_ROWS_HINT, true);
		useParameters = ac.getBoolean(PROPERTY_USE_PARAMETERS, true);
		assumePrimaryKeyUnique = ac.getBoolean(PROPERTY_ASSUME_PRIMARY_KEY_UNIQUE, true);
	}


	
	@Override
	/**
	 * Creates a connection, checks if the table is existing and creates it when necessary
	 */
	public void configure() throws ConfigurationException {
		super.configure();
		setOperationControls();
	}


	protected void createQueryTexts(IDbmsSupport dbmsSupport) throws ConfigurationException {
		deleteQuery = "DELETE FROM "+getPrefix()+getTableName()+ getWhereClause(getKeyField()+"=?",true);
		String listClause=getListClause();
		selectContextQuery = "SELECT "+listClause+ getWhereClause(getKeyField()+"=?",true);
		selectListQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport)+provideFirstRowsHintAfterFirstKeyword(dbmsSupport)+ listClause+ getWhereClause(null,false)+
						  " ORDER BY "+getDateField()+(StringUtils.isNotEmpty(getOrder())?" " + getOrder():"")+provideTrailingFirstRowsHint(dbmsSupport);
		selectDataQuery = "SELECT "+getMessageField()+  " FROM "+getPrefix()+getTableName()+ getWhereClause(getKeyField()+"=?",true);
		checkMessageIdQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + getIdField() +" FROM "+getPrefix()+getTableName()+ getWhereClause(getIdField() +"=?",false);
		checkCorrelationIdQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + getCorrelationIdField() +" FROM "+getPrefix()+getTableName()+ getWhereClause(getCorrelationIdField() +"=?",false);
		getMessageCountQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + "COUNT(*) FROM "+getPrefix()+getTableName()+ getWhereClause(null,false);
		if (dbmsSupport.mustInsertEmptyBlobBeforeData()) {
			updateBlobQuery = dbmsSupport.getUpdateBlobQuery(getPrefix()+getTableName(), getMessageField(), getKeyField()); 
		}
	}

	private String getListClause() {
		return getKeyField()+","+getIdField()+","+getCorrelationIdField()+","+getDateField()+","+getExpiryDateField()+
		(StringUtils.isNotEmpty(getTypeField())?","+getTypeField():"")+
		(StringUtils.isNotEmpty(getHostField())?","+getHostField():"")+
		(StringUtils.isNotEmpty(getLabelField())?","+getLabelField():"")+
		","+getCommentField()+ " FROM "+getPrefix()+getTableName();
	}
	
	private String getSelectListQuery(IDbmsSupport dbmsSupport, Date startTime, Date endTime, IMessageBrowser.SortOrder order) {
		String whereClause=null;
		if (startTime!=null) {
			whereClause=getDateField()+">=?";
		}
		if (endTime!=null) {
			whereClause=Misc.concatStrings(whereClause, " AND ", getDateField()+"<?");
		}
		if(order.equals(SortOrder.NONE)) { //If no order has been set, use the default (DESC for messages and ASC for errors)
			order = SortOrder.valueOf(getOrder());
		}

		return "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport)+provideFirstRowsHintAfterFirstKeyword(dbmsSupport)+ getListClause()+ getWhereClause(whereClause,false)+
		  " ORDER BY "+getDateField()+(" "+order.name()+" ")+provideTrailingFirstRowsHint(dbmsSupport);
	}

	private String provideIndexHintAfterFirstKeyword(IDbmsSupport dbmsSupport) {
		if (useIndexHint) {
			return dbmsSupport.provideIndexHintAfterFirstKeyword(getPrefix()+getTableName(), getPrefix()+getIndexName());
		}
		return "";
	}

	private String provideFirstRowsHintAfterFirstKeyword(IDbmsSupport dbmsSupport) {
		if (useFirstRowsHint) {
			return dbmsSupport.provideFirstRowsHintAfterFirstKeyword(10);
		}
		return "";
	}
	private String provideTrailingFirstRowsHint(IDbmsSupport dbmsSupport) {
		if (useFirstRowsHint) {
			return dbmsSupport.provideTrailingFirstRowsHint(100);
		}
		return "";
	}

	protected String getWhereClause(String clause, boolean primaryKeyIsPartOfClause) {
		if (primaryKeyIsPartOfClause && assumePrimaryKeyUnique || StringUtils.isEmpty(getSlotId())) {
			if (StringUtils.isEmpty(clause)) {
				return "";
			}  
			return " WHERE "+clause; 
		}
		String result = " WHERE "+getSelector();
		if (StringUtils.isNotEmpty(clause)) {
			result += " AND "+clause;
		}
		return result;
	}
	
	
	

	
	private String createResultString(boolean isMessageDifferent){
		String resultStringStart = "<results>";
		String resultStringEnd = "</results>";
		String messageIdExistsString = "<result>WARN_MESSAGEID_ALREADY_EXISTS</result>";
		String resultString = resultStringStart+messageIdExistsString;
		if(isMessageDifferent){
			String messageIsDifferentString = "<result>ERROR_MESSAGE_IS_DIFFERENT</result>";
			resultString = resultString+messageIsDifferentString;
		}
		resultString = resultString+resultStringEnd;
		return resultString;
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

		@Override
		public boolean hasNext() throws ListenerException {
			advance();
			return current;
		}

		@Override
		public IMessageBrowsingIteratorItem next() throws ListenerException {
			if (!current) {
				advance();
			}
			if (!current) {
				throw new ListenerException("read beyond end of resultset");
			}
			current=false;
			return new JdbcTransactionalStorageIteratorItem(conn,rs,false);
		}

		@Override
		public void close() throws ListenerException {
			try {
				rs.close();
				conn.close();
			} catch (SQLException e) {
				throw new ListenerException("error closing browser session",e);
			}
		} 
	}

	@Override
	public IMessageBrowsingIterator getIterator() throws ListenerException {
		return getIterator(null,null, SortOrder.NONE);
	}

	@Override
	public IMessageBrowsingIterator getIterator(Date startTime, Date endTime, SortOrder order) throws ListenerException {
		try (Connection conn = getConnection()) {
			String query = getSelectListQuery(getDbmsSupport(), startTime, endTime, order);
			if (log.isDebugEnabled()) {
				log.debug("preparing selectListQuery ["+query+"]");
			}
			try (PreparedStatement stmt = conn.prepareStatement(query)) {
				if (startTime==null && endTime==null) {
					applyStandardParameters(stmt, false, false);
				} else {
					int paramPos=applyStandardParameters(stmt, true, false);
					if (startTime!=null) {
						stmt.setTimestamp(paramPos++, new Timestamp(startTime.getTime()));
					}
					if (endTime!=null) {
						stmt.setTimestamp(paramPos++, new Timestamp(endTime.getTime()));
					}
				}
				ResultSet rs =  stmt.executeQuery();
				return new ResultSetIterator(conn,rs);
			}
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	protected String getSelector() {
		if (StringUtils.isEmpty(getSlotId())) {
			return null;
		}
		if (StringUtils.isEmpty(getType())) {
			return getSlotIdField()+"="+(useParameters?"?":"'"+getSlotId()+"'");
		}
		return getSlotIdField()+"="+(useParameters?"?":"'"+getSlotId()+"'")+" AND "+getTypeField()+"="+(useParameters?"?":"'"+getType()+"'");
	}

	private int applyStandardParameters(PreparedStatement stmt, boolean moreParametersFollow, boolean primaryKeyIsPartOfClause) throws SQLException {
		int position=1;
		if (!(primaryKeyIsPartOfClause && assumePrimaryKeyUnique) && useParameters && StringUtils.isNotEmpty(getSlotId())) {
			stmt.clearParameters();
			stmt.setString(position++, getSlotId());
			if (StringUtils.isNotEmpty(getType())) {
				stmt.setString(position++, getType());
			}
		} else {
			if (moreParametersFollow) {
				stmt.clearParameters();
			}
		}
		return position;
	}
	private int applyStandardParameters(PreparedStatement stmt, String paramValue, boolean primaryKeyIsPartOfClause) throws SQLException {
		int position=applyStandardParameters(stmt,true,primaryKeyIsPartOfClause);
		stmt.setString(position++,paramValue);
		return position;
	}

	
	

	@Override
	public void deleteMessage(String messageId) throws ListenerException {
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
				applyStandardParameters(stmt, messageId, true);
				stmt.execute();
			}
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	private M retrieveObject(ResultSet rs, int columnIndex, boolean compressed) throws ClassNotFoundException, JdbcException, IOException, SQLException {
		InputStream blobStream=null;
		try {
			Blob blob = rs.getBlob(columnIndex);
			if (blob==null) {
				return null;
			}
			if (compressed) {
				blobStream=new InflaterInputStream(JdbcUtil.getBlobInputStream(blob, Integer.toString(columnIndex)));
			} else {
				blobStream=JdbcUtil.getBlobInputStream(blob, Integer.toString(columnIndex));
			}
			try (ObjectInputStream ois = new ObjectInputStream(blobStream)) {
				return (M)ois.readObject();
			}
		} finally {
			if (blobStream!=null) {
				blobStream.close();
			}
		}
	}

	
	protected M retrieveObject(ResultSet rs, int columnIndex) throws ClassNotFoundException, JdbcException, IOException, SQLException {
		try {
			if (isBlobsCompressed()) {
				try {
					return retrieveObject(rs,columnIndex,true);
				} catch (ZipException e1) {
					log.warn(getLogPrefix()+"could not extract compressed blob, trying non-compressed: ("+ClassUtils.nameOf(e1)+") "+e1.getMessage());
					return retrieveObject(rs,columnIndex,false);
				}
			}
			try {
				return retrieveObject(rs,columnIndex,false);
			} catch (Exception e1) {
				log.warn(getLogPrefix()+"could not extract non-compressed blob, trying compressed: ("+ClassUtils.nameOf(e1)+") "+e1.getMessage());
				return retrieveObject(rs,columnIndex,true);
			}
		} catch (Exception e2) {
			throw new JdbcException("could not extract message", e2);
		}
	}

	@Override
	public int getMessageCount() throws ListenerException {
		try (Connection conn = getConnection()) {
			try (JdbcSession session = getDbmsSupport().prepareSessionForDirtyRead(conn)) {
				try (PreparedStatement stmt = conn.prepareStatement(getMessageCountQuery)) {
					applyStandardParameters(stmt, false, false);
					try (ResultSet rs =  stmt.executeQuery()) {
						if (!rs.next()) {
							log.warn(getLogPrefix()+"no message count found");
							return 0;
						}
						return rs.getInt(1);
					}
				}
			}
		} catch (Exception e) {
			throw new ListenerException("cannot determine message count",e);
		}
	}


	@Override
	public boolean containsMessageId(String originalMessageId) throws ListenerException {
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(checkMessageIdQuery)) {
				applyStandardParameters(stmt, originalMessageId, false);
				try (ResultSet rs =  stmt.executeQuery()) {

					if (!rs.next()) {
						return false;
					}
					
					return true;
				}
			}
		} catch (Exception e) {
			throw new ListenerException("cannot deserialize message",e);
		}
	}

	@Override
	public boolean containsCorrelationId(String correlationId) throws ListenerException {
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(checkCorrelationIdQuery)) {
				try (ResultSet rs =  stmt.executeQuery()) {

					if (!rs.next()) {
						return false;
					}
					
					return true;
				}
			}
		} catch (Exception e) {
			throw new ListenerException("cannot deserialize message",e);
		}
	}

	@Override
	public IMessageBrowsingIteratorItem getContext(String messageId) throws ListenerException {
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(selectContextQuery)) {
				applyStandardParameters(stmt, messageId, true);
				try (ResultSet rs =  stmt.executeQuery()) {
	
					if (!rs.next()) {
						throw new ListenerException("could not retrieve context for messageid ["+ messageId+"]");
					}
					return new JdbcTransactionalStorageIteratorItem(conn, rs,true);
				}
			}
		} catch (Exception e) {
			throw new ListenerException("cannot read context",e);
		}
	}

	@Override
	public M browseMessage(String messageId) throws ListenerException {
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(selectDataQuery)) {
				applyStandardParameters(stmt, messageId, true);
				try (ResultSet rs =  stmt.executeQuery()) {
	
					if (!rs.next()) {
						throw new ListenerException("could not retrieve message for messageid ["+ messageId+"]");
					}
					return retrieveObject(rs,1);
				}
			}
		} catch (ListenerException e) { //Don't catch ListenerExceptions, unnecessarily and ungly
			throw e;
		} catch (Exception e) {
			throw new ListenerException("cannot deserialize message",e);
		}
	}


	private class JdbcTransactionalStorageIteratorItem implements IMessageBrowsingIteratorItem {

		private Connection conn;
		private ResultSet rs;
		private boolean closeOnRelease;
		
		public JdbcTransactionalStorageIteratorItem(Connection conn, ResultSet rs, boolean closeOnRelease) {
			super();
			this.conn=conn;
			this.rs=rs;
			this.closeOnRelease=closeOnRelease;
		}
		
		@Override
		public String getId() throws ListenerException {
			try {
				return rs.getString(getKeyField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		@Override
		public String getOriginalId() throws ListenerException {
			try {
				return rs.getString(getIdField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		@Override
		public String getCorrelationId() throws ListenerException {
			try {
				return rs.getString(getCorrelationIdField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		@Override
		public Date getInsertDate() throws ListenerException {
			try {
				return rs.getTimestamp(getDateField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		@Override
		public Date getExpiryDate() throws ListenerException {
			try {
				return rs.getTimestamp(getExpiryDateField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		@Override
		public String getType() throws ListenerException {
			if (StringUtils.isEmpty(getTypeField())) {
				return null;
			}
			try {
				return rs.getString(getTypeField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		@Override
		public String getHost() throws ListenerException {
			if (StringUtils.isEmpty(getHostField())) {
				return null;
			}
			try {
				return rs.getString(getHostField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}

		@Override
		public String getLabel() throws ListenerException {
			if (StringUtils.isEmpty(getLabelField())) {
				return null;
			}
			try {
				return rs.getString(getLabelField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}

		@Override
		public String getCommentString() throws ListenerException {
			try {
				return rs.getString(getCommentField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}

		@Override
		public void release() {
			if (closeOnRelease) {
				JdbcUtil.fullClose(conn, rs);
			}
		}
		
		
	}




	@Override
	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" in table ["+getTableName()+"]";
	}

	@IbisDoc({"the name of the sequence used to generate the primary key (only for oracle) n.b. the default name has been changed in version 4.6", "seq_ibisstore"})
	public void setSequenceName(String string) {
		sequenceName = string;
	}
	public String getSequenceName() {
		return sequenceName;
	}


	/**
	 * Sets the name of the table messages are stored in.
	 */
	@IbisDoc({"the name of the table messages are stored in", "IBISSTORE"})
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	public String getTableName() {
		return tableName;
	}

	/**
	 * Sets the name of the column messageids are stored in.
	 */
	@IbisDoc({"the name of the column messageids are stored in", "messageid"})
	public void setIdField(String idField) {
		this.idField = idField;
	}
	public String getIdField() {
		return idField;
	}

	/**
	 * Sets the name of the column message themselves are stored in.
	 */
	@IbisDoc({"the name of the column message themselves are stored in", "message"})
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

	public String getExpiryDateField() {
		return expiryDateField;
	}

	public String getLabelField() {
		return labelField;
	}

	@IbisDoc({"the name of the column comments are stored in", "comments"})
	public void setCommentField(String string) {
		commentField = string;
	}

	@IbisDoc({"the name of the column the timestamp is stored in", "messagedate"})
	public void setDateField(String string) {
		dateField = string;
	}

	@IbisDoc({"the name of the column the timestamp for expiry is stored in", "expirydate"})
	public void setExpiryDateField(String string) {
		expiryDateField = string;
	}

	@IbisDoc({"the name of the column labels are stored in", "label"})
	public void setLabelField(String string) {
		labelField = string;
	}

	public String getCorrelationIdField() {
		return correlationIdField;
	}

	@IbisDoc({"the name of the column correlation-ids are stored in", "correlationid"})
	public void setCorrelationIdField(String string) {
		correlationIdField = string;
	}

	@IbisDoc({"the type of the column message themselves are stored in", "long binary"})
	public void setMessageFieldType(String string) {
		messageFieldType = string;
	}
	public String getMessageFieldType() {
		return messageFieldType;
	}

	@IbisDoc({"the type of the column that contains the primary key of the table", "int default autoincrement"})
	public void setKeyFieldType(String string) {
		keyFieldType = string;
	}
	public String getKeyFieldType() {
		return keyFieldType;
	}

	@IbisDoc({"the type of the column the timestamps are stored in", "timestamp"})
	public void setDateFieldType(String string) {
		dateFieldType = string;
	}
	public String getDateFieldType() {
		return dateFieldType;
	}

	@IbisDoc({"the type of the columns messageid and correlationid, slotid and comments are stored in. n.b. (100) is appended for id's, (1000) is appended for comments.", "varchar"})
	public void setTextFieldType(String string) {
		textFieldType = string;
	}
	public String getTextFieldType() {
		return textFieldType;
	}

	public String getKeyField() {
		return keyField;
	}

	@IbisDoc({"the name of the column that contains the primary key of the table", "messagekey"})
	public void setKeyField(String string) {
		keyField = string;
	}


	protected String getSlotId() {
		return slotId;
	}
	protected String getSlotIdField() {
		return slotIdField;
	}
	protected String getType() {
		return type;
	}
	protected String getTypeField() {
		return typeField;
	}
	protected String getHostField() {
		return hostField;
	}


	public void setOrder(String string) {
		order = string;
	}
	public String getOrder() {
		if (StringUtils.isNotEmpty(order)) {
			return order;
		} else {
			if (type.equalsIgnoreCase(TYPE_ERRORSTORAGE)) {
				return errorsOrder; //Defaults to ASC
			} else {
				return messagesOrder; //Defaults to DESC
			}
		}
	}

	@IbisDoc({"when set to <code>true</code>, the messages are stored compressed", "<code>true</code>"})
	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}
	public boolean isBlobsCompressed() {
		return blobsCompressed;
	}

	@IbisDoc({"the name of the index, to be used in hints for query optimizer too (only for oracle)", "ix_ibisstore"})
	public void setIndexName(String string) {
		indexName = string;
	}
	public String getIndexName() {
		return indexName;
	}

	public void setTxManager(PlatformTransactionManager manager) {
		txManager = manager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

	@IbisDoc({"prefix to be prefixed on all database objects (tables, indices, sequences), e.q. to access a different oracle schema", ""})
	public void setPrefix(String string) {
		prefix = string;
	}
	public String getPrefix() {
		return prefix;
	}



	@Override
	public void setHideRegex(String hideRegex) {
		this.hideRegex = hideRegex;
	}
	@Override
	public String getHideRegex() {
		return hideRegex;
	}

	@Override
	public void setHideMethod(String hideMethod) {
		this.hideMethod = hideMethod;
	}
	@Override
	public String getHideMethod() {
		return hideMethod;
	}

}
