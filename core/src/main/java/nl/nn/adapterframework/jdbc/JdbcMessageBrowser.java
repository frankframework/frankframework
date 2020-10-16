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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
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
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * JDBC implementation of {@link IMessageBrowser}.
 * 
 * @author Gerrit van Brakel
 */
public abstract class JdbcMessageBrowser<M> extends JdbcFacade implements IMessageBrowser<M> {

	public final static TransactionDefinition TXREQUIRED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
	
	private String keyField=null;
	private String idField=null;
	private String correlationIdField=null;
	private String dateField=null;
	private String commentField=null;
	private String messageField=null;
	protected String slotIdField=null;
	private String expiryDateField=null;
	private String labelField=null;
	protected String slotId=null;
	protected String typeField=null;
	private String type = "";
	private String hostField=null;

	private String prefix="";

	private String hideRegex = null;
	private String hideMethod = "all";

	private SortOrder sortOrder = null;
	private String messagesOrder = AppConstants.getInstance().getString("browse.messages.order", "DESC");
	private String errorsOrder = AppConstants.getInstance().getString("browse.errors.order", "ASC");


	protected PlatformTransactionManager txManager;

	protected String deleteQuery;
	protected String selectContextQuery;
	protected String selectDataQuery;
	protected String checkMessageIdQuery;
	protected String checkCorrelationIdQuery;
	protected String getMessageCountQuery;
	
	private String selector;

	protected boolean selectKeyQueryIsDbmsSupported;
	
	
	protected static final String CONTROL_PROPERTY_PREFIX="jdbc.storage.";
	protected static final String PROPERTY_USE_PARAMETERS=CONTROL_PROPERTY_PREFIX+"useParameters";
	protected static final String PROPERTY_ASSUME_PRIMARY_KEY_UNIQUE=CONTROL_PROPERTY_PREFIX+"assumePrimaryKeyUnique";
	
	private boolean useParameters;
	private boolean assumePrimaryKeyUnique;

	private DataSource datasource = null;

	public JdbcMessageBrowser() {
		super();
		setTransacted(true);
	}

	@Override
	protected String getLogPrefix() {
		return "JdbcMessageBrowser ["+getName()+"] ";
	}

	protected void setOperationControls() {
		AppConstants ac = AppConstants.getInstance();
		useParameters = ac.getBoolean(PROPERTY_USE_PARAMETERS, true);
		assumePrimaryKeyUnique = ac.getBoolean(PROPERTY_ASSUME_PRIMARY_KEY_UNIQUE, true);
	}

	public void copyFacadeSettings(JdbcFacade facade) throws JdbcException {
		if (facade!=null) {
			datasource=facade.getDatasource();
			setAuthAlias(facade.getAuthAlias());
			setUsername(facade.getUsername());
			setPassword(facade.getPassword());
		}
	}

	@Override
	protected DataSource getDatasource() throws JdbcException {
		return datasource!=null ? datasource : super.getDatasource();
	}

	
	@Override
	/**
	 * Creates a connection, checks if the table is existing and creates it when necessary
	 */
	public void configure() throws ConfigurationException {
		super.configure();
		setOperationControls();
		selector = createSelector();
	}



	protected abstract String getSelectListQuery(IDbmsSupport dbmsSupport, Date startTime, Date endTime, IMessageBrowser.SortOrder order);


	protected String getWhereClause(String clause, boolean primaryKeyIsPartOfClause) {
		if (primaryKeyIsPartOfClause && assumePrimaryKeyUnique || StringUtils.isEmpty(selector)) {
			if (StringUtils.isEmpty(clause)) {
				return "";
			}  
			return " WHERE "+clause; 
		}
		return Misc.concatStrings(" WHERE "+selector," AND ", clause);
	}
	
	protected String createSelector() {
		return Misc.concatStrings(
				(StringUtils.isNotEmpty(getSlotIdField()) ? getSlotIdField()+"="+(useParameters?"?":"'"+getSlotId()+"'") : ""), 
				" AND ", 
				(StringUtils.isNotEmpty(getType()) && StringUtils.isNotEmpty(getTypeField()) ? getTypeField()+"="+(useParameters?"?":"'"+getTypeField()+"'") : ""));
	}

	protected int applyStandardParameters(PreparedStatement stmt, boolean moreParametersFollow, boolean primaryKeyIsPartOfClause) throws SQLException {
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
	protected int applyStandardParameters(PreparedStatement stmt, String paramValue, boolean primaryKeyIsPartOfClause) throws SQLException {
		int position=applyStandardParameters(stmt,true,primaryKeyIsPartOfClause);
		JdbcUtil.setParameter(stmt, position++, paramValue, getDbmsSupport().isParameterTypeMatchRequired());
		return position;
	}

	
	

	private class ResultSetIterator implements IMessageBrowsingIterator {
		
		private Connection conn;
		private ResultSet  rs;
		private boolean current;
		private boolean eof;
		
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
			return new JdbcMessageBrowserIteratorItem(conn,rs,false);
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
		Connection conn;
		PreparedStatement stmt=null;
		IMessageBrowsingIterator result=null;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new ListenerException(e);
		}
		try {
			String query = getSelectListQuery(getDbmsSupport(), startTime, endTime, order);
			if (log.isDebugEnabled()) {
				log.debug("preparing selectListQuery ["+query+"]");
			}
			stmt = conn.prepareStatement(query);
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
			result = new ResultSetIterator(conn,rs);
			return result;
		} catch (SQLException e) {
			throw new ListenerException(e);
		} finally {
			if (result==null) {
				// in happy flow IMessageBrowsingIterator will close resources.
				// If it has not been created, we'll have to close them here.
				JdbcUtil.fullClose(conn, stmt);
			}
		}
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

	protected M retrieveObject(ResultSet rs, int columnIndex) throws ClassNotFoundException, JdbcException, IOException, SQLException {
		return (M)rs.getString(columnIndex); //TODO shouldn't this be getObject(columnIndex, M)?
	}

	@Override
	public int getMessageCount() throws ListenerException {
		try (Connection conn = getConnection()) {
			try (JdbcSession session = getDbmsSupport().prepareSessionForNonLockingRead(conn)) {
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
					return rs.next();
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
					return rs.next();
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
					return new JdbcMessageBrowserIteratorItem(conn, rs,true);
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
		} catch (ListenerException e) { //Don't catch ListenerExceptions, unnecessarily and ugly
			throw e;
		} catch (Exception e) {
			throw new ListenerException("cannot deserialize message",e);
		}
	}


	private class JdbcMessageBrowserIteratorItem implements IMessageBrowsingIteratorItem {

		private Connection conn;
		private ResultSet rs;
		private boolean closeOnRelease;
		
		public JdbcMessageBrowserIteratorItem(Connection conn, ResultSet rs, boolean closeOnRelease) {
			super();
			this.conn=conn;
			this.rs=rs;
			this.closeOnRelease=closeOnRelease;
		}
		
		public String fieldValue(String field) throws ListenerException {
			try {
				return StringUtils.isNotEmpty(field)?rs.getString(field):null;
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		public Date dateFieldValue(String field) throws ListenerException {
			try {
				return StringUtils.isNotEmpty(field)?rs.getTimestamp(field):null;
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		
		@Override
		public String getId() throws ListenerException {
			return fieldValue(getKeyField());
		}
		@Override
		public String getOriginalId() throws ListenerException {
			return fieldValue(getIdField());
		}
		@Override
		public String getCorrelationId() throws ListenerException {
			return fieldValue(getCorrelationIdField());
		}
		@Override
		public Date getInsertDate() throws ListenerException {
			return dateFieldValue(getDateField());
		}
		@Override
		public Date getExpiryDate() throws ListenerException {
			return dateFieldValue(getExpiryDateField());
		}
		@Override
		public String getType() throws ListenerException {
			return fieldValue(getTypeField());
		}
		@Override
		public String getHost() throws ListenerException {
			return fieldValue(getHostField());
		}

		@Override
		public String getLabel() throws ListenerException {
			return fieldValue(getLabelField());
		}

		@Override
		public String getCommentString() throws ListenerException {
			return fieldValue(getCommentField());
		}

		@Override
		public void release() {
			if (closeOnRelease) {
				JdbcUtil.fullClose(conn, rs);
			}
		}
		
		
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
	protected void setType(String type) {
		this.type = type;
	}
	protected String getType() {
		return type;
	}
	public String getTypeField() {
		return typeField;
	}

	protected void setHostField(String hostField) {
		this.hostField = hostField;
	}
	protected String getHostField() {
		return hostField;
	}


	public void setOrder(String string) {
		if(StringUtils.isNotEmpty(string)) {
			sortOrder = SortOrder.valueOf(string.trim());
		}
	}
	public String getOrder() {
		return getOrderEnum().name();
	}

	public SortOrder getOrderEnum() {
		if(sortOrder == null) {
			if (type.equalsIgnoreCase(StorageType.ERRORSTORAGE.getCode())) {
				setOrder(errorsOrder); //Defaults to ASC
			} else {
				setOrder(messagesOrder); //Defaults to DESC
			}
		}
		return sortOrder;
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
