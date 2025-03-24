/*
   Copyright 2020-2022 WeAreFrank!

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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringUtil;

public class JdbcTableMessageBrowser<M> extends AbstractJdbcMessageBrowser<M> {

	private @Getter String tableName="IBISSTORE";
	private @Getter String indexName="IX_IBISSTORE";
	private String selectCondition=null;
	private String tableAlias;

	private JdbcFacade parent=null;
	private final JdbcTableListener<M> tableListener;

	private static final String PROPERTY_USE_INDEX_HINT=CONTROL_PROPERTY_PREFIX+"useIndexHint";
	private static final String PROPERTY_USE_FIRST_ROWS_HINT=CONTROL_PROPERTY_PREFIX+"useFirstRowsHint";

	protected boolean useIndexHint;
	private boolean useFirstRowsHint;

	public JdbcTableMessageBrowser(JdbcTableListener<M> tableListener) {
		this.tableListener = tableListener;
	}

	public JdbcTableMessageBrowser(JdbcTableListener<M> tableListener, String statusValue, StorageType storageType) {
		this(tableListener);
		parent=tableListener;
		setKeyField(tableListener.getKeyField());
		setIdField(tableListener.getMessageIdField());
		setCorrelationIdField(tableListener.getCorrelationIdField());
		setTableName(tableListener.getTableName());
		tableAlias = tableListener.getTableAlias();
		setMessageField(StringUtils.isNotEmpty(tableListener.getMessageField())?tableListener.getMessageField():tableListener.getKeyField());
		setDateField(tableListener.getTimestampField());
		setCommentField(tableListener.getCommentField());
		setType(storageType.getCode());
		selectCondition=tableListener.getStatusField()+ "='"+statusValue+"'";
		if (StringUtils.isNotEmpty(tableListener.getSelectCondition())) {
			selectCondition += " AND ("+tableListener.getSelectCondition()+")";
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		try {
			copyFacadeSettings(parent);
		} catch (JdbcException e) {
			throw new ConfigurationException(e);
		}
		super.configure();
		if (StringUtils.isEmpty(getTableName())) {
			throw new ConfigurationException("Attribute [tableName] is not set");
		}
		if (StringUtils.isEmpty(getKeyField())) {
			throw new ConfigurationException("Attribute [keyField] is not set");
		}
		createQueryTexts(getDbmsSupport());
	}

	@Override
	protected void setOperationControls() {
		super.setOperationControls();
		AppConstants ac = AppConstants.getInstance();
		useIndexHint = ac.getBoolean(PROPERTY_USE_INDEX_HINT, false);
		useFirstRowsHint = ac.getBoolean(PROPERTY_USE_FIRST_ROWS_HINT, true);
	}

	@Override
	protected RawMessageWrapper<M> retrieveObject(String storageKey, ResultSet rs, int columnIndex) throws JdbcException, SQLException {
		if (tableListener!=null) {
			return tableListener.extractRawMessage(rs);
		}
		//noinspection unchecked
		RawMessageWrapper<M> rawMessageWrapper = (RawMessageWrapper<M>) new RawMessageWrapper<>(rs.getString(columnIndex), storageKey, null);
		rawMessageWrapper.getContext().put(PipeLineSession.STORAGE_ID_KEY, storageKey);
		return rawMessageWrapper;
	}

	protected void createQueryTexts(IDbmsSupport dbmsSupport) throws ConfigurationException {
		deleteQuery = "DELETE" + getFromClause(true) + getWhereClause(getKeyField()+"=?",true);
		selectContextQuery = "SELECT "+getListClause(true)+ getWhereClause(getKeyField()+"=?",true);
		selectDataQuery = "SELECT "+getKeyField()+","+getMessageField()+ getFromClause(true) + getWhereClause(getKeyField()+"=?",true);
		checkMessageIdQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + getIdField() + getFromClause(false) + getWhereClause(getIdField() +"=?",false);
		checkCorrelationIdQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + getCorrelationIdField() + getFromClause(false) + getWhereClause(getCorrelationIdField() +"=?",false);
		try {
			getMessageCountQuery = dbmsSupport.prepareQueryTextForNonLockingRead("SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + "COUNT(*)"+ getFromClause(false) + getWhereClause(null,false));
		} catch (JdbcException e) {
			throw new ConfigurationException("Cannot create getMessageCountQuery", e);
		}
	}

	protected String getFromClause(boolean noAlias) {
		return " FROM "+getPrefix()+getTableName() + (!noAlias && StringUtils.isNotBlank(tableAlias)?" "+tableAlias.trim():"");
	}

	private String getListClause(boolean noAlias) {
		return getKeyField()+
		(StringUtils.isNotEmpty(getIdField())?","+getIdField():"")+
		(StringUtils.isNotEmpty(getCorrelationIdField())?","+getCorrelationIdField():"")+
		(StringUtils.isNotEmpty(getDateField())?","+getDateField():"")+
		(StringUtils.isNotEmpty(getExpiryDateField())?","+getExpiryDateField():"")+
		(StringUtils.isNotEmpty(getTypeField())?","+getTypeField():"")+
		(StringUtils.isNotEmpty(getHostField())?","+getHostField():"")+
		(StringUtils.isNotEmpty(getLabelField())?","+getLabelField():"")+
		(StringUtils.isNotEmpty(getCommentField())?","+getCommentField():"")+
		getFromClause(noAlias);
	}

	@Override
	protected String getSelectListQuery(IDbmsSupport dbmsSupport, Date startTime, Date endTime, IMessageBrowser.SortOrder order) {
		String whereClause=null;
		if (startTime!=null) {
			whereClause=getDateField()+">=?";
		}
		if (endTime!=null) {
			whereClause= StringUtil.concatStrings(whereClause, " AND ", getDateField()+"<?");
		}
		if(order == SortOrder.NONE) { //If no order has been set, use the default (DESC for messages and ASC for errors)
			order = getOrder();
		}
		if (order == null) {
			order = SortOrder.ASC;
		}
		return "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport)+provideFirstRowsHintAfterFirstKeyword(dbmsSupport)+ getListClause(false)+ getWhereClause(whereClause,false)+
				(StringUtils.isNotEmpty(getDateField())? " ORDER BY "+getDateField()+ " "+order.name():"")+provideTrailingFirstRowsHint(dbmsSupport);
	}




	@Override
	protected String createSelector() {
		if (StringUtils.isNotEmpty(selectCondition)) {
			return StringUtil.concatStrings(super.createSelector()," AND ","("+selectCondition+")");
		}
		return super.createSelector();
	}


	protected String provideIndexHintAfterFirstKeyword(IDbmsSupport dbmsSupport) {
		if (useIndexHint && StringUtils.isNotEmpty(getIndexName())) {
			return dbmsSupport.provideIndexHintAfterFirstKeyword(getPrefix()+getTableName(), getPrefix()+getIndexName());
		}
		return "";
	}

	protected String provideFirstRowsHintAfterFirstKeyword(IDbmsSupport dbmsSupport) {
		if (useFirstRowsHint) {
			return dbmsSupport.provideFirstRowsHintAfterFirstKeyword(100);
		}
		return "";
	}
	protected String provideTrailingFirstRowsHint(IDbmsSupport dbmsSupport) {
		if (useFirstRowsHint) {
			return dbmsSupport.provideTrailingFirstRowsHint(100);
		}
		return "";
	}




	@Override
	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" in table ["+getTableName()+"]";
	}


	/**
	 * Name of the table messages are stored in.
	 * @ff.default IBISSTORE
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}


	/**
	 * Name of the index, to be used in hints for query optimizer too (only for Oracle).
	 * @ff.default IX_IBISSTORE
	 */
	public void setIndexName(String string) {
		indexName = string;
	}

	@Override
	public List<String> getStorageFields() {
		return Stream.of(
			getKeyField(),
			getIdField(),
			getCorrelationIdField(),
			getDateField(),
			getCommentField(),
			getMessageField(),
			getSlotIdField(),
			getExpiryDateField(),
			getLabelField(),
			getTypeField(),
			getHostField()
		).filter(StringUtils::isNotEmpty).toList();
	}
}

