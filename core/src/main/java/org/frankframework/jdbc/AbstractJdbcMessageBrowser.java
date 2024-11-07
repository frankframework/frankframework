/*
   Copyright 2020-2023 WeAreFrank!

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IMessageBrowsingIterator;
import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.util.AppConstants;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringUtil;

/**
 * JDBC implementation of {@link IMessageBrowser}.
 *
 * @author Gerrit van Brakel
 */
public abstract class AbstractJdbcMessageBrowser<M> extends JdbcFacade implements IMessageBrowser<M> {

	private @Getter String keyField=null;
	private @Getter String idField=null;
	private @Getter String correlationIdField=null;
	private @Getter String dateField=null;
	private @Getter String commentField=null;
	private @Getter String messageField=null;
	private @Getter String slotIdField=null;
	private @Getter String expiryDateField=null;
	private @Getter String labelField=null;
	private @Getter String prefix="";
	private @Getter String slotId=null;
	private @Getter String type = "";
	private @Getter String typeField=null;
	private @Getter String hostField=null;

	private @Getter @Setter String hideRegex = null;
	private @Getter @Setter HideMethod hideMethod = HideMethod.ALL;

	private @Getter SortOrder order = null;
	private final String messagesOrder = AppConstants.getInstance().getString("browse.messages.order", "DESC");
	private final String errorsOrder = AppConstants.getInstance().getString("browse.errors.order", "ASC");


	protected String deleteQuery;
	protected String selectContextQuery;
	protected String selectDataQuery;
	protected String checkMessageIdQuery;
	protected String checkCorrelationIdQuery;
	protected String getMessageCountQuery;

	private String selector;

	protected static final String CONTROL_PROPERTY_PREFIX="jdbc.storage.";
	protected static final String PROPERTY_USE_PARAMETERS=CONTROL_PROPERTY_PREFIX+"useParameters";
	protected static final String PROPERTY_ASSUME_PRIMARY_KEY_UNIQUE=CONTROL_PROPERTY_PREFIX+"assumePrimaryKeyUnique";

	private boolean useParameters;
	private boolean assumePrimaryKeyUnique;

	private DataSource datasource = null;

	public AbstractJdbcMessageBrowser() {
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
			SpringUtils.autowireByName(facade.getApplicationContext(), this);
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
	public void configure() throws ConfigurationException {
		super.configure();
		setOperationControls();
		selector = createSelector();
		if(getOrder() == null) {
			if (type.equalsIgnoreCase(StorageType.ERRORSTORAGE.getCode())) {
				setOrder(EnumUtils.parse(SortOrder.class, errorsOrder)); //Defaults to ASC
			} else {
				setOrder(EnumUtils.parse(SortOrder.class, messagesOrder)); //Defaults to DESC
			}
		}
	}

	protected abstract String getSelectListQuery(IDbmsSupport dbmsSupport, Date startTime, Date endTime, IMessageBrowser.SortOrder order);

	protected String getWhereClause(String clause, boolean primaryKeyIsPartOfClause) {
		if (primaryKeyIsPartOfClause && assumePrimaryKeyUnique || StringUtils.isEmpty(selector)) {
			if (StringUtils.isEmpty(clause)) {
				return "";
			}
			return " WHERE "+clause;
		}
		return StringUtil.concatStrings(" WHERE "+selector," AND ", clause);
	}

	protected String createSelector() {
		return StringUtil.concatStrings(
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

		private final Connection conn;
		private final ResultSet  rs;
		private boolean current;
		private boolean eof;

		ResultSetIterator(Connection conn, ResultSet rs) {
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
				log.debug("preparing selectListQuery [{}]", query);
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
	public void deleteMessage(String storageKey) throws ListenerException {
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
				applyStandardParameters(stmt, storageKey, true);
				stmt.execute();
			}
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	protected abstract RawMessageWrapper<M> retrieveObject(String storageKey, ResultSet rs, int columnIndex) throws SQLException, JdbcException;

	@Override
	public int getMessageCount() throws ListenerException {
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(getMessageCountQuery)) {
				applyStandardParameters(stmt, false, false);
				try (ResultSet rs =  stmt.executeQuery()) {
					if (!rs.next()) {
						log.warn("{}no message count found", getLogPrefix());
						return 0;
					}
					return rs.getInt(1);
				}
			}
		} catch (Exception e) {
			throw new ListenerException("cannot determine message count", e);
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
				applyStandardParameters(stmt, correlationId, false);
				try (ResultSet rs =  stmt.executeQuery()) {
					return rs.next();
				}
			}
		} catch (Exception e) {
			throw new ListenerException("cannot deserialize message",e);
		}
	}

	@Override
	public IMessageBrowsingIteratorItem getContext(String storageKey) throws ListenerException {
		// result set needs to stay open to access the fields of a record
		// The caller may use try-with-resources to call the close method of IMessageBrowsingIteratorItem to close the open resources
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement(selectContextQuery);
			applyStandardParameters(stmt, storageKey, true);
			ResultSet rs = stmt.executeQuery();
			if (!rs.next()) {
				throw new ListenerException("could not retrieve context for storageKey ["+ storageKey+"]");
			}

			return new JdbcMessageBrowserIteratorItem(conn, rs, true);
		} catch (Exception e) {
			throw new ListenerException("cannot read context",e);
		}
	}

	@Override
	public RawMessageWrapper<M> browseMessage(String storageKey) throws ListenerException {
		try (Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(selectDataQuery)) {
				applyStandardParameters(stmt, storageKey, true);
				try (ResultSet rs =  stmt.executeQuery()) {
					if (!rs.next()) {
						throw new ListenerException("could not retrieve message for storageKey ["+ storageKey+"]");
					}
					return retrieveObject(storageKey, rs, 2);
				}
			}
		} catch (ListenerException e) { // Don't catch ListenerExceptions, unnecessarily and ugly
			throw e;
		} catch (Exception e) {
			throw new ListenerException("cannot deserialize message",e);
		}
	}

	private class JdbcMessageBrowserIteratorItem implements IMessageBrowsingIteratorItem {

		private final Connection conn;
		private final ResultSet rs;
		private final boolean closeOnRelease;

		public JdbcMessageBrowserIteratorItem(Connection conn, ResultSet rs, boolean closeOnRelease) {
			super();
			this.conn = conn;
			this.rs = rs;
			this.closeOnRelease = closeOnRelease;
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
		public void close() {
			if (closeOnRelease) {
				JdbcUtil.fullClose(conn, rs);
			}
		}


	}

	/**
	 * The name of the column that contains the primary key of the table
	 * @ff.default MESSAGEKEY
	 */
	public void setKeyField(String string) {
		keyField = string;
	}

	/**
	 * The name of the column messageIds are stored in
	 * @ff.default MESSAGEID
	 */
	public void setIdField(String idField) {
		this.idField = idField;
	}

	/**
	 * The name of the column correlation-ids are stored in
	 * @ff.default CORRELATIONID
	 */
	public void setCorrelationIdField(String string) {
		correlationIdField = string;
	}

	/**
	 * The name of the column message themselves are stored in
	 * @ff.default MESSAGE
	 */
	public void setMessageField(String messageField) {
		this.messageField = messageField;
	}

	/**
	 * The name of the column the timestamp is stored in
	 * @ff.default MESSAGEDATE
	 */
	public void setDateField(String string) {
		dateField = string;
	}

	/**
	 * The name of the column comments are stored in
	 * @ff.default COMMENTS
	 */
	public void setCommentField(String string) {
		commentField = string;
	}

	/**
	 * The name of the column the timestamp for expiry is stored in
	 * @ff.default EXPIRYDATE
	 */
	public void setExpiryDateField(String string) {
		expiryDateField = string;
	}

	/**
	 * The name of the column labels are stored in
	 * @ff.default LABEL
	 */
	public void setLabelField(String string) {
		labelField = string;
	}

	protected void setSlotIdField(String string) {
		slotIdField = string;
	}
	protected void setTypeField(String typeField) {
		this.typeField = typeField;
	}

	protected void setHostField(String hostField) {
		this.hostField = hostField;
	}

	/** Prefix to be prefixed on all database objects (tables, indices, sequences), e.g. to access a different Oracle schema */
	public void setPrefix(String string) {
		prefix = string;
	}

	protected void setSlotId(String string) {
		slotId = string;
	}

	protected void setType(String type) {
		this.type = type;
	}

	public void setOrder(SortOrder value) {
		order = value;
	}

}
