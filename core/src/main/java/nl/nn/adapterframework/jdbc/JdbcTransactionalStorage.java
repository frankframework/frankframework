/*
   Copyright 2013-2018 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TransactionAttribute;
import nl.nn.adapterframework.core.TransactionAttributes;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * JDBC implementation of {@link ITransactionalStorage}.
 *
 * Storage structure is defined in /IAF_util/IAF_DatabaseChangelog.xml
 *
 * If these objects do not exist, Ibis will try to create them if the attribute createTable="true".
 *
 * <br/>
 * N.B. Note on using XA transactions:
 * If transactions are used on Oracle, make sure that the database user can access the table SYS.DBA_PENDING_TRANSACTIONS.
 * If not, transactions present when the server goes down cannot be properly recovered, resulting in exceptions like:
 * <pre>
   The error code was XAER_RMERR. The exception stack trace follows: javax.transaction.xa.XAException
	at oracle.jdbc.xa.OracleXAResource.recover(OracleXAResource.java:508)
   </pre>
 *
 * @author Gerrit van Brakel
 * @author Jaco de Groot
 * @since 4.1
 */
public class JdbcTransactionalStorage<S extends Serializable> extends JdbcTableMessageBrowser<S> implements ITransactionalStorage<S> {

	private @Getter boolean checkTable; 		// default set from appConstant jdbc.storage.checkTable
	private @Getter boolean checkIndices;		// default set from appConstant jdbc.storage.checkIndices
	private @Getter boolean createTable=false;

	private String host;
	private @Getter boolean blobsCompressed=true;
	private @Getter boolean storeFullMessage=true;

	private @Getter int retention = 30;
	private @Getter String schemaOwner4Check=null;
	private @Getter boolean onlyStoreWhenMessageIdUnique=false;


	protected static final int MAXIDLEN=100;
	protected static final int MAXCIDLEN=256;
	protected static final int MAXLABELLEN=1000;
	// the following values are only used when the table is created.
	private @Getter String keyFieldType="";
	private @Getter String dateFieldType="";
	private @Getter String messageFieldType="";
	private @Getter String textFieldType="";



	protected String insertQuery;
	protected String selectKeyQuery;
	protected String selectDataQuery2;

	// the following for Oracle
	private @Getter String sequenceName="seq_ibisstore";
	protected String updateBlobQuery;

	private static final String PROPERTY_CHECK_TABLE=CONTROL_PROPERTY_PREFIX+"checkTable";
	private static final String PROPERTY_CHECK_INDICES=CONTROL_PROPERTY_PREFIX+"checkIndices";

	private static final boolean documentQueries=false;

	protected @Getter @Setter PlatformTransactionManager txManager;

	private TransactionDefinition txDef;

	private static Set<String> checkedTables = new HashSet<>();
	private static Set<String> checkedIndices = new HashSet<>();
	private static Set<String> checkedSequences = new HashSet<>();

	public JdbcTransactionalStorage() {
		super(null);
		setTableName("IBISSTORE");
		setKeyField("MESSAGEKEY");
		setIdField("MESSAGEID");
		setCorrelationIdField("CORRELATIONID");
		setDateField("MESSAGEDATE");
		setCommentField("COMMENTS");
		setMessageField("MESSAGE");
		setSlotIdField("SLOTID");
		setExpiryDateField("EXPIRYDATE");
		setLabelField("LABEL");
		setTypeField("TYPE");
		setHostField("HOST");
		setIndexName("IX_IBISSTORE");
	}

	@Override
	protected String getLogPrefix() {
		return "JdbcTransactionalStorage ["+getName()+"] ";
	}

	@Override
	protected void setOperationControls() {
		super.setOperationControls();
		AppConstants ac = AppConstants.getInstance();
		checkTable = ac.getBoolean(PROPERTY_CHECK_TABLE, false);
		checkIndices = ac.getBoolean(PROPERTY_CHECK_INDICES, true);
	}

	private void checkTableColumnPresent(Connection connection, IDbmsSupport dbms, String columnName) throws JdbcException {
		if (StringUtils.isNotEmpty(columnName) && !dbms.isColumnPresent(connection, getSchemaOwner4Check(), getTableName(), columnName)) {
			ConfigurationWarnings.add(this, log, "table [" + getTableName() + "] has no column [" + columnName + "]");
		}
	}

	private void checkTable(Connection connection) throws JdbcException {
		String storageRefKey = getStorageRefKey();
		if (checkedTables.contains(storageRefKey)) {
			log.debug("table [{}] already checked", this::getTableName);
			return;
		}
		checkedTables.add(storageRefKey);
		IDbmsSupport dbms=getDbmsSupport();
		String schemaOwner=getSchemaOwner4Check();
		log.debug("checking for presence of table ["+getTableName()+"] in schema/catalog ["+schemaOwner+"]");
		if (dbms.isTablePresent(connection, getTableName())) {
			checkTableColumnPresent(connection,dbms,getKeyField());
			checkTableColumnPresent(connection,dbms,getTypeField());
			checkTableColumnPresent(connection,dbms,getSlotIdField());
			checkTableColumnPresent(connection,dbms,getHostField());
			checkTableColumnPresent(connection,dbms,getIdField());
			checkTableColumnPresent(connection,dbms,getCorrelationIdField());
			checkTableColumnPresent(connection,dbms,getDateField());
			checkTableColumnPresent(connection,dbms,getCommentField());
			if (isStoreFullMessage()) {
				checkTableColumnPresent(connection,dbms,getMessageField());
			}
			checkTableColumnPresent(connection,dbms,getExpiryDateField());
			checkTableColumnPresent(connection,dbms,getLabelField());
		} else {
			ConfigurationWarnings.add(this, log, "table ["+getTableName()+"] not present");
		}
	}

	private void checkIndices(Connection connection) throws JdbcException {
		String storageRefKey = getStorageRefKey();
		if (checkedIndices.contains(storageRefKey)) {
			log.debug("table [{}] already checked for indices", this::getTableName);
			return;
		}
		checkedIndices.add(storageRefKey);

		checkIndexOnColumnPresent(connection, getKeyField());

		List<String> columnListIndex01= new ArrayList<String>();
		if (StringUtils.isNotEmpty(getTypeField())) {
			columnListIndex01.add(getTypeField());
		}
		if (StringUtils.isNotEmpty(getSlotIdField())) {
			columnListIndex01.add(getSlotIdField());
		}
		if (StringUtils.isNotEmpty(getDateField())) {
			columnListIndex01.add(getDateField());
		}
		checkIndexOnColumnsPresent(connection, columnListIndex01);

		if (StringUtils.isNotEmpty(getExpiryDateField())) {
			checkIndexOnColumnPresent(connection, getExpiryDateField());
		}

		List<String> columnListIndex03= new ArrayList<String>();
		if (StringUtils.isNotEmpty(getSlotIdField())) {
			columnListIndex03.add(getSlotIdField());
		}
		if (StringUtils.isNotEmpty(getIdField())) {
			columnListIndex03.add(getIdField());
		}
		checkIndexOnColumnsPresent(connection, columnListIndex03);
	}

	private void checkIndexOnColumnPresent(Connection connection, String column) throws JdbcException {
		if (!getDbmsSupport().hasIndexOnColumn(connection, getSchemaOwner4Check(), getTableName(), column)) {
			ConfigurationWarnings.add(this, log, "table ["+getTableName()+"] has no index on column ["+column+"]");
		}
	}

	private void checkIndexOnColumnsPresent(Connection connection, List<String> columns) throws JdbcException {
		if (columns!=null && !columns.isEmpty()) {
			if (!getDbmsSupport().hasIndexOnColumns(connection, getSchemaOwner4Check(), getTableName(), columns)) {
				String msg="table ["+getTableName()+"] has no index on columns ["+columns.get(0);
				for (int i=1;i<columns.size();i++) {
					msg+=","+columns.get(i);
				}
				msg+="]";
				ConfigurationWarnings.add(this, log, msg);
			}
		}
	}

	private void checkSequence(Connection connection) {
		String storageRefKey = getStorageRefKey();
		if (checkedSequences.contains(storageRefKey)) {
			log.debug("table [{}] already checked for sequence", this::getTableName);
			return;
		}
		checkedSequences.add(storageRefKey);
		if (!getDbmsSupport().isSequencePresent(connection, getSchemaOwner4Check(), getTableName(), getSequenceName())) {
			ConfigurationWarnings.add(this, log, "sequence ["+getSequenceName()+"] not present");
		}
	}

	private void checkDatabase() throws ConfigurationException {
		if (checkTable || checkIndices) {
			try (Connection connection = getConnection()) {
				if (schemaOwner4Check == null) {
					IDbmsSupport dbms = getDbmsSupport();
					try {
						schemaOwner4Check = dbms.getSchema(connection);
					} catch (Exception e) {
						log.warn("Exception determining current schema", e);
					}
				}
				if (StringUtils.isNotEmpty(getSchemaOwner4Check())) {
					if (checkTable) {
						if (StringUtils.isNotEmpty(getTableName())) {
							checkTable(connection);
						} else {
							throw new ConfigurationException("Attribute [tableName] is not set");
						}
						if (StringUtils.isNotEmpty(getSequenceName())) {
							checkSequence(connection);
						} else {
							throw new ConfigurationException("Attribute [sequenceName] is not set");
						}
					}
					if (checkIndices) {
						checkIndices(connection);
					}
				} else {
					ConfigurationWarnings.add(this, log, "could not check database regarding table [" + getTableName() + "]: Schema owner is unknown");
				}
			} catch (JdbcException e) {
				ConfigurationWarnings.add(this, log, "could not check database regarding table [" + getTableName() + "]"+e.getMessage(), e);
			} catch (SQLException e) {
				log.warn("could check database", e);
			}
		} else {
			log.info(getLogPrefix()+"checking of table and indices is not enabled");
		}
	}

	@Override
	/**
	 * Creates a connection, checks if the table is existing and creates it when necessary
	 */
	public void configure() throws ConfigurationException {
		if (useIndexHint && StringUtils.isEmpty(getIndexName())) {
			throw new ConfigurationException("Attribute [indexName] is not set and useIndexHint=true");
		}
		if (StringUtils.isEmpty(getSequenceName())) {
			throw new ConfigurationException("Attribute [sequenceName] is not set");
		}
		if (StringUtils.isNotEmpty(getHostField())) {
			host=Misc.getHostname();
		}
		super.configure();
		checkDatabase();
		txDef = TransactionAttributes.configureTransactionAttributes(log, TransactionAttribute.REQUIRED, 0);
	}

	@Override
	public void open() throws SenderException {
		try {
			initialize(getDbmsSupport());
		} catch (JdbcException e) {
			throw new SenderException(e);
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix()+"exception creating table ["+getTableName()+"]",e);
		}
	}

	/**
	 * change datatypes used for specific database vendor.
	 */
	protected void setDataTypes(IDbmsSupport dbmsSupport) {
		if (StringUtils.isEmpty(getKeyFieldType())) {
			setKeyFieldType(dbmsSupport.getAutoIncrementKeyFieldType());
		}
		if (StringUtils.isEmpty(getDateFieldType())) {
			setDateFieldType(dbmsSupport.getTimestampFieldType());
		}
		if (StringUtils.isEmpty(getTextFieldType())) {
			setTextFieldType(dbmsSupport.getTextFieldType());
		}
		if (StringUtils.isEmpty(getMessageFieldType())) {
			setMessageFieldType(dbmsSupport.getBlobFieldType());
		}
	}

	@Override
	protected void createQueryTexts(IDbmsSupport dbmsSupport) throws ConfigurationException {
		super.createQueryTexts(dbmsSupport);
		setDataTypes(dbmsSupport);
		boolean keyFieldsNeedsInsert=dbmsSupport.autoIncrementKeyMustBeInserted();
		boolean blobFieldsNeedsEmptyBlobInsert=dbmsSupport.mustInsertEmptyBlobBeforeData();
		insertQuery = "INSERT INTO "+getPrefix()+getTableName()+" ("+
						(keyFieldsNeedsInsert?getKeyField()+",":"")+
						(StringUtils.isNotEmpty(getTypeField())?getTypeField()+",":"")+
						(StringUtils.isNotEmpty(getSlotId())?getSlotIdField()+",":"")+
						(StringUtils.isNotEmpty(getHostField())?getHostField()+",":"")+
						(StringUtils.isNotEmpty(getLabelField())?getLabelField()+",":"")+
						getIdField()+","+getCorrelationIdField()+","+getDateField()+","+getCommentField()+","+getExpiryDateField()+
						(isStoreFullMessage()?","+getMessageField():"")+
						(isOnlyStoreWhenMessageIdUnique()?") SELECT ":") VALUES (")+
						(keyFieldsNeedsInsert?dbmsSupport.autoIncrementInsertValue(getPrefix()+getSequenceName())+",":"")+
						(StringUtils.isNotEmpty(getTypeField())?"?,":"")+
						(StringUtils.isNotEmpty(getSlotId())?"?,":"")+
						(StringUtils.isNotEmpty(getHostField())?"?,":"")+
						(StringUtils.isNotEmpty(getLabelField())?"?,":"")+
						"?,?,?,?,?"+
						(isStoreFullMessage()?","+(blobFieldsNeedsEmptyBlobInsert?dbmsSupport.emptyBlobValue():"?"):"")+
						(isOnlyStoreWhenMessageIdUnique()?" "+dbmsSupport.getFromForTablelessSelect()+" WHERE NOT EXISTS (SELECT * FROM IBISSTORE WHERE "+getIdField()+" = ?"+(StringUtils.isNotEmpty(getSlotId())?" AND "+getSlotIdField()+" = ?":"")+")":")");
		selectKeyQuery = dbmsSupport.getInsertedAutoIncrementValueQuery(getPrefix()+getSequenceName());
		selectKeyQueryIsDbmsSupported=StringUtils.isNotEmpty(selectKeyQuery);
		if (!selectKeyQueryIsDbmsSupported) {
			selectKeyQuery = "SELECT max("+getKeyField()+")"+getFromClause(false)+
							getWhereClause(getIdField()+"=?"+
										" AND "+getCorrelationIdField()+"=?"+
										" AND "+getDateField()+"=?",false);
		}
		if (dbmsSupport.mustInsertEmptyBlobBeforeData()) {
			updateBlobQuery = dbmsSupport.getUpdateBlobQuery(getPrefix()+getTableName(), getMessageField(), getKeyField());
		}
		selectDataQuery2 = "SELECT " + getMessageField() + " FROM " + getPrefix() + getTableName() + " WHERE " + getIdField() + "=?";
		if (documentQueries && log.isDebugEnabled()) {
			log.debug(
					documentQuery("insertQuery",insertQuery,"Voeg een regel toe aan de tabel")+
					documentQuery("deleteQuery",deleteQuery,"Verwijder een regel uit de tabel, via de primary key")+
					documentQuery("selectKeyQuery",selectKeyQuery,"Haal de huidige waarde op van de sequence voor messageKey")+
					documentQuery("selectContextQuery",selectContextQuery,"Haal de niet blob velden van een regel op, via de primary key")+
					documentQuery("selectListQuery",getSelectListQuery(dbmsSupport, null, null, null),"Haal een lijst van regels op, op volgorde van de index. Haalt niet altijd alle regels op")+
					documentQuery("selectDataQuery",selectDataQuery,"Haal de blob van een regel op, via de primary key")+
					documentQuery("checkMessageIdQuery",checkMessageIdQuery,"bekijk of een messageId bestaat, NIET via de primary key. Echter: het aantal fouten is over het algemeen relatief klein. De index selecteert dus een beperkt aantal rijen uit een groot aantal.")+
					documentQuery("checkCorrelationIdQuery",checkCorrelationIdQuery,"bekijk of een correlationId bestaat, NIET via de primary key. Echter: het aantal fouten is over het algemeen relatief klein. De index selecteert dus een beperkt aantal rijen uit een groot aantal.")+
					documentQuery("getMessageCountQuery",getMessageCountQuery,"tel het aantal regels in een gedeelte van de tabel. Kan via index.")+
					documentQuery("updateBlobQuery",updateBlobQuery,"Geef de blob een waarde, via de primary key")+
					documentQuery("selectDataQuery2",selectDataQuery2,"Haal de blob van een regel op, via een messageId")
//					+"\n"
//					+"\n- slotId en type zou via ? kunnen"
//					+"\n- selectListQuery zou in sommige gevallen extra filters in de where clause kunnen krijgen"
//					+"\n- selectListQuery zou FIRST_ROWS(500) hint kunnen krijgen"
//					+"\n- we zouden de index hint via een custom property aan en uit kunnen zetten"
					);
		}
	}


	private String documentQuery(String name, String query, String purpose) {
		return "\n"+name+(purpose!=null?"\n"+purpose:"")+"\n"+query+"\n";
	}



	/**
	 *	Checks if table exists, and creates when necessary.
	 */
	public void initialize(IDbmsSupport dbmsSupport) throws JdbcException, SQLException, SenderException {
		try (Connection conn = getConnection()) {
			boolean tableMustBeCreated;

			if (isCheckTable()) {
				try {
					tableMustBeCreated = !getDbmsSupport().isTablePresent(conn, getPrefix()+getTableName());
					if (!isCreateTable() && tableMustBeCreated) {
						throw new SenderException("table ["+getPrefix()+getTableName()+"] does not exist");
					}
					log.info("table ["+getPrefix()+getTableName()+"] does "+(tableMustBeCreated?"NOT ":"")+"exist");
				} catch (JdbcException e) {
					log.warn(getLogPrefix()+"exception determining existence of table ["+getPrefix()+getTableName()+"] for transactional storage, trying to create anyway."+ e.getMessage());
					tableMustBeCreated=true;
				}
			} else {
				log.info("did not check for existence of table ["+getPrefix()+getTableName()+"]");
				tableMustBeCreated = false;
			}

			if (isCreateTable() && tableMustBeCreated) {
				log.info(getLogPrefix()+"creating table ["+getPrefix()+getTableName()+"] for transactional storage");
				try (Statement stmt = conn.createStatement()) {
					createStorage(conn, stmt, dbmsSupport);
				}
				conn.commit();
			}
		}
	}



	/**
	 *	Acutaly creates storage. Can be overridden in descender classes
	 */
	protected void createStorage(Connection conn, Statement stmt, IDbmsSupport dbmsSupport) throws JdbcException {
		String query=null;
		try {
			query="CREATE TABLE "+getPrefix()+getTableName()+" ("+
						getKeyField()+" "+getKeyFieldType()+" CONSTRAINT " +getPrefix()+getTableName()+ "_pk PRIMARY KEY, "+
						(StringUtils.isNotEmpty(getTypeField())?getTypeField()+" CHAR(1), ":"")+
						(StringUtils.isNotEmpty(getSlotId())? getSlotIdField()+" "+getTextFieldType()+"("+MAXIDLEN+"), ":"")+
						(StringUtils.isNotEmpty(getHostField())?getHostField()+" "+getTextFieldType()+"("+MAXIDLEN+"), ":"")+
						getIdField()+" "+getTextFieldType()+"("+MAXIDLEN+"), "+
						getCorrelationIdField()+" "+getTextFieldType()+"("+MAXCIDLEN+"), "+
						getDateField()+" "+getDateFieldType()+", "+
						getCommentField()+" "+getTextFieldType()+"("+MAXCOMMENTLEN+"), "+
						getMessageField()+" "+getMessageFieldType()+", "+
						getExpiryDateField()+" "+getDateFieldType()+
						(StringUtils.isNotEmpty(getLabelField())?getLabelField()+" "+getTextFieldType()+"("+MAXLABELLEN+"), ":"")+
					")";

			log.debug(getLogPrefix()+"creating table ["+getPrefix()+getTableName()+"] using query ["+query+"]");
			stmt.execute(query);
			if (StringUtils.isNotEmpty(getIndexName())) {
				query = "CREATE INDEX "+getPrefix()+getIndexName()+" ON "+getPrefix()+getTableName()+"("+(StringUtils.isNotEmpty(getSlotId())?getSlotIdField()+",":"")+getDateField()+","+getExpiryDateField()+")";
				log.debug(getLogPrefix()+"creating index ["+getPrefix()+getIndexName()+"] using query ["+query+"]");
				stmt.execute(query);
			}
			if (dbmsSupport.autoIncrementUsesSequenceObject()) {
				query="CREATE SEQUENCE "+getPrefix()+getSequenceName()+" START WITH 1 INCREMENT BY 1";
				log.debug(getLogPrefix()+"creating sequence for table ["+getPrefix()+getTableName()+"] using query ["+query+"]");
				stmt.execute(query);
			}
			conn.commit();
		} catch (SQLException e) {
			throw new JdbcException(getLogPrefix()+" executing query ["+query+"]", e);
		}
	}


	/**
	 * Retrieves the value of the primary key for the record just inserted.
	 */
	private String retrieveKey(Connection conn, String messageId, String correlationId, Timestamp receivedDateTime) throws SQLException, SenderException {
		if (log.isDebugEnabled()) {
			log.debug("preparing key retrieval statement ["+selectKeyQuery+"]");
		}
		try (PreparedStatement stmt = conn.prepareStatement(selectKeyQuery)) {
			if (!selectKeyQueryIsDbmsSupported) {
				int paramPos=applyStandardParameters(stmt, true, false);
				JdbcUtil.setParameter(stmt, paramPos++, messageId, getDbmsSupport().isParameterTypeMatchRequired());
				JdbcUtil.setParameter(stmt, paramPos++, correlationId, getDbmsSupport().isParameterTypeMatchRequired());
				stmt.setTimestamp(paramPos++, receivedDateTime);
			}

			try (ResultSet rs = stmt.executeQuery()) {
				if (!rs.next()) {
					throw new SenderException("could not retrieve key for stored message ["+ messageId+"]");
				}
				return "<id>" + rs.getString(1) + "</id>";
			}
		}
	}

	protected String storeMessageInDatabase(Connection conn, String messageId, String correlationId, Timestamp receivedDateTime, String comments, String label, S message) throws IOException, SQLException, JdbcException, SenderException {
		PreparedStatement stmt = null;
		try {
			IDbmsSupport dbmsSupport=getDbmsSupport();
			if (log.isDebugEnabled()) {
				log.debug("preparing insert statement ["+insertQuery+"]");
			}
			if (!dbmsSupport.mustInsertEmptyBlobBeforeData()) {
				stmt = conn.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
			} else {
				stmt = conn.prepareStatement(insertQuery);
			}
			stmt.clearParameters();
			int parPos=0;

			if (StringUtils.isNotEmpty(getTypeField())) {
				stmt.setString(++parPos,getType());
			}
			if (StringUtils.isNotEmpty(getSlotId())) {
				stmt.setString(++parPos,getSlotId());
			}
			if (StringUtils.isNotEmpty(getHostField())) {
				stmt.setString(++parPos,host);
			}
			if (StringUtils.isNotEmpty(getLabelField())) {
				stmt.setString(++parPos,label);
			}
			stmt.setString(++parPos,messageId);
			stmt.setString(++parPos,correlationId);
			stmt.setTimestamp(++parPos, receivedDateTime);
			stmt.setString(++parPos, comments);
			if (StorageType.MESSAGELOG_PIPE.getCode().equalsIgnoreCase(getType()) || StorageType.MESSAGELOG_RECEIVER.getCode().equalsIgnoreCase(getType())) {
				if (getRetention()<0) {
					stmt.setTimestamp(++parPos, null);
				} else {
					Date date = new Date();
					Calendar cal = Calendar.getInstance();
					cal.setTime(date);
					cal.add(Calendar.DAY_OF_MONTH, getRetention());
					stmt.setTimestamp(++parPos, new Timestamp(cal.getTime().getTime()));
				}
			} else {
				stmt.setTimestamp(++parPos, null);
			}

			if (!isStoreFullMessage()) {
				if (isOnlyStoreWhenMessageIdUnique()) {
					JdbcUtil.setParameter(stmt, ++parPos, messageId, getDbmsSupport().isParameterTypeMatchRequired());
					stmt.setString(++parPos, getSlotId());
				}
				stmt.execute();
				return null;
			}
			if (!dbmsSupport.mustInsertEmptyBlobBeforeData()) {
				int blobColumnIndex = ++parPos;
				Object blobHandle=dbmsSupport.getBlobHandle(stmt, blobColumnIndex);
				try (ObjectOutputStream oos = new ObjectOutputStream(JdbcUtil.getBlobOutputStream(dbmsSupport, blobHandle, stmt, blobColumnIndex, isBlobsCompressed()))) {
					oos.writeObject(message);
				}
				dbmsSupport.applyBlobParameter(stmt, blobColumnIndex, blobHandle);

				if (isOnlyStoreWhenMessageIdUnique()) {
					stmt.setString(++parPos, messageId);
					stmt.setString(++parPos, getSlotId());
				}
				stmt.execute();
				try (ResultSet rs = stmt.getGeneratedKeys()) {
					if (rs.next() && rs.getString(1) != null) {
						return "<id>" + rs.getString(1) + "</id>";
					}
				}

				boolean isMessageDifferent = isMessageDifferent(conn, messageId, message);
				String resultString = createResultString(isMessageDifferent);
				log.warn("MessageID [" + messageId + "] already exists");
				if (isMessageDifferent) {
					log.warn("Message with MessageID [" + messageId + "] is not equal");
				}
				return resultString;
			}
			if (isOnlyStoreWhenMessageIdUnique()) {
				stmt.setString(++parPos, messageId);
				stmt.setString(++parPos, getSlotId());
			}
			stmt.execute();
			int updateCount = stmt.getUpdateCount();
			if (log.isDebugEnabled()) {
				log.debug("update count for insert statement: "+updateCount);
			}
			if (updateCount > 0) {
				if (log.isDebugEnabled()) {
					log.debug("preparing select statement ["+selectKeyQuery+"]");
				}
				stmt.close();
				stmt = conn.prepareStatement(selectKeyQuery);
				// retrieve the key
				String newKey;
				try (ResultSet rs = stmt.executeQuery()) {
					if (!rs.next()) {
						throw new SenderException("could not retrieve key of stored message");
					}
					newKey = rs.getString(1);
				}

				// and update the blob
				if (log.isDebugEnabled()) {
					log.debug("preparing update statement ["+updateBlobQuery+"]");
				}
				stmt.close();
				stmt = conn.prepareStatement(updateBlobQuery);
				stmt.clearParameters();
				stmt.setString(1,newKey);

				try (ResultSet rs = stmt.executeQuery()) {
					if (!rs.next()) {
						throw new SenderException("could not retrieve row for stored message ["+ messageId+"]");
					}
					Object blobHandle=dbmsSupport.getBlobHandle(rs, 1);
					try (ObjectOutputStream oos = new ObjectOutputStream(JdbcUtil.getBlobOutputStream(dbmsSupport, blobHandle, rs, 1, isBlobsCompressed()))) {
						oos.writeObject(message);
					}
					dbmsSupport.updateBlob(rs, 1, blobHandle);
					return "<id>" + newKey+ "</id>";

				}
			} else {
				if (isOnlyStoreWhenMessageIdUnique()) {
					boolean isMessageDifferent = isMessageDifferent(conn, messageId, message);
					String resultString = createResultString(isMessageDifferent);
					log.warn("MessageID [" + messageId + "] already exists");
					if (isMessageDifferent) {
						log.warn("Message with MessageID [" + messageId + "] is not equal");
					}
					return resultString;
				} else {
					throw new SenderException("update count for update statement not greater than 0 ["+updateCount+"]");
				}
			}

		} finally {
			if (stmt!=null) {
				stmt.close();
			}
		}
	}

	private boolean isMessageDifferent(Connection conn, String messageId, S message) throws SQLException{
		int paramPosition=0;

		try (PreparedStatement stmt = conn.prepareStatement(selectDataQuery2)){
			stmt.clearParameters();
			JdbcUtil.setParameter(stmt, ++paramPosition, messageId, getDbmsSupport().isParameterTypeMatchRequired());
			// executing query, getting message as response in a result set.
			try (ResultSet rs = stmt.executeQuery()) {
				// if rs.next() needed as you can not simply call rs.
				if (rs.next()) {
					String dataBaseMessage = retrieveObject(rs, 1).toString();
					String inputMessage = message.toString();
					if (dataBaseMessage.equals(inputMessage)) {
						return false;
					}
				}
				return true;
			}
		} catch (Exception e) {
			log.warn("Exception comparing messages", e);
			return true;
		}
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

	@Override
	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, S message) throws SenderException {
		IbisTransaction itx = null;
		if (txManager!=null) {
			itx = new IbisTransaction(txManager, txDef, ClassUtils.nameOf(this));
		}
		try {
			String result;
			if (messageId==null) {
				throw new SenderException("messageId cannot be null");
			}
			if (correlationId==null) {
				throw new SenderException("correlationId cannot be null");
			}
			try (Connection conn = getConnection()) {
				Timestamp receivedDateTime = new Timestamp(receivedDate.getTime());
				if (messageId.length()>MAXIDLEN) {
					messageId=messageId.substring(0,MAXIDLEN);
				}
				if (correlationId.length()>MAXCIDLEN) {
					correlationId=correlationId.substring(0,MAXCIDLEN);
				}
				if (comments!=null && comments.length()>MAXCOMMENTLEN) {
					comments=comments.substring(0,MAXCOMMENTLEN);
				}
				if (label!=null && label.length()>MAXLABELLEN) {
					label=label.substring(0,MAXLABELLEN);
				}
				result = storeMessageInDatabase(conn, messageId, correlationId, receivedDateTime, comments, label, message);
				if (result==null) {
					result=retrieveKey(conn,messageId,correlationId,receivedDateTime);
				}
				return result;

			} catch (Exception e) {
				if (itx!=null) {
					itx.setRollbackOnly();
				}
				throw new SenderException("cannot serialize message",e);
			}
		} finally {
			if (itx!=null) {
				itx.commit();
			}
		}

	}

	public String storeMessage(Connection conn, String messageId, String correlationId, Date receivedDate, String comments, String label, S message) throws SenderException {
		String result;
		try {
			Timestamp receivedDateTime = new Timestamp(receivedDate.getTime());
			if (messageId.length()>MAXIDLEN) {
				messageId=messageId.substring(0,MAXIDLEN);
			}
			if (correlationId.length()>MAXCIDLEN) {
				correlationId=correlationId.substring(0,MAXCIDLEN);
			}
			if (comments!=null && comments.length()>MAXCOMMENTLEN) {
				comments=comments.substring(0,MAXCOMMENTLEN);
			}
			if (label!=null && label.length()>MAXLABELLEN) {
				label=label.substring(0,MAXLABELLEN);
			}
			result = storeMessageInDatabase(conn, messageId, correlationId, receivedDateTime, comments, label, message);
			if (result==null) {
				result=retrieveKey(conn,messageId,correlationId,receivedDateTime);
			}
			return result;
		} catch (IOException | JdbcException | SQLException e) {
			throw new SenderException("cannot serialize message", e);
		}
	}




	private S retrieveObject(ResultSet rs, int columnIndex, boolean compressed) throws ClassNotFoundException, JdbcException, IOException, SQLException {
		try (InputStream blobInputStream = JdbcUtil.getBlobInputStream(getDbmsSupport(), rs, columnIndex, compressed)) {
			if (blobInputStream==null) {
				return null;
			}
			try (ObjectInputStream ois = new ObjectInputStream(blobInputStream)) {
				return (S)ois.readObject();
			}
		}
	}


	@Override
	protected S retrieveObject(ResultSet rs, int columnIndex) throws JdbcException {
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
	public S getMessage(String storageKey) throws ListenerException {
		S result = browseMessage(storageKey);
		deleteMessage(storageKey);
		return result;
	}

	protected String getStorageRefKey() {
		return getDatasourceName()+"|"+getTableName();
	}



	@Override
	public void setSlotId(String string) {
		super.setSlotId(string);
	}

	@Override
	public void setType(String type) {
		super.setType(type);
	}



	@Override
	/**
	 * The name of the column slotids are stored in
	 * @ff.default SLOTID
	 */
	public void setSlotIdField(String string) {
		super.setSlotIdField(string);
	}

	@Override
	/**
	 * The name of the column types are stored in
	 * @ff.default TYPE
	 */
	public void setTypeField(String typeField) {
		super.setTypeField(typeField);
	}

	@Override
	/**
	 * The name of the column that stores the hostname of the server
	 * @ff.default HOST
	 */
	public void setHostField(String hostField) {
		super.setHostField(hostField);
	}


	/**
	 * The name of the sequence used to generate the primary key, for DBMSes that use sequences, like Oracle
	 * @ff.default seq_ibisstore
	 */
	public void setSequenceName(String string) {
		sequenceName = string;
	}

	@Deprecated
	@ConfigurationWarning("Replaced with checkTable")
	public void setCheckIfTableExists(boolean b) {
		setCheckTable(b);
	}

	/**
	 * If set to <code>true</code>, checks are performed if the table exists and is properly created
	 * @ff.default false
	 */
	public void setCheckTable(boolean b) {
		checkTable = b;
	}

	/**
	 * If set to <code>true</code>, the table is created if it does not exist
	 * @ff.default false
	 */
	public void setCreateTable(boolean b) {
		createTable = b;
	}

	/** The type of the column message themselves are stored in */
	public void setMessageFieldType(String string) {
		messageFieldType = string;
	}

	/** The type of the column that contains the primary key of the table */
	public void setKeyFieldType(String string) {
		keyFieldType = string;
	}

	/** The type of the column the timestamps are stored in */
	public void setDateFieldType(String string) {
		dateFieldType = string;
	}

	/** The type of the columns messageId and correlationId, slotId and comments are stored in. N.B. <code>(100)</code> is appended for id's, <code>(1000)</code> is appended for comments. */
	public void setTextFieldType(String string) {
		textFieldType = string;
	}





	/**
	 * If set to <code>true</code>, the messages are stored compressed
	 * @ff.default true
	 */
	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}

	/**
	 * The time (in days) to keep the record in the database before making it eligible for deletion by a cleanup process. when set to -1, the record will live on forever
	 * @ff.default 30
	 */
	public void setRetention(int retention) {
		this.retention = retention;
	}

	/**
	 * Schema owner to be used to check the database
	 * @ff.default &lt;current_schema&gt; (only for oracle)
	 */
	public void setSchemaOwner4Check(String string) {
		schemaOwner4Check = string;
	}



	/**
	 * If set to <code>true</code>, the full message is stored with the log. Can be set to <code>false</code> to reduce table size, by avoiding to store the full message
	 * @ff.default true
	 */
	public void setStoreFullMessage(boolean storeFullMessage) {
		this.storeFullMessage = storeFullMessage;
	}

	/**
	 * If set to <code>true</code>, the message is stored only if the MessageId is not present in the store yet.
	 * @ff.default false
	 */
	public void setOnlyStoreWhenMessageIdUnique(boolean onlyStoreWhenMessageIdUnique) {
		this.onlyStoreWhenMessageIdUnique = onlyStoreWhenMessageIdUnique;
	}

}
