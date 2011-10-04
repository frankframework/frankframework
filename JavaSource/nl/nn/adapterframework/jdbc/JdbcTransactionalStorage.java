/*
 * $Log: JdbcTransactionalStorage.java,v $
 * Revision 1.53  2011-10-04 14:26:03  l190409
 * added prefix in sequence select query
 *
 * Revision 1.52  2011/08/09 09:53:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use isTablePresent() and isTableColumnPresent() from dbmsSupport
 *
 * Revision 1.51  2011/04/13 08:38:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Blob and Clob support using DbmsSupport
 *
 * Revision 1.50  2011/03/16 16:42:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of DbmsSupport, including support for MS SQL Server
 *
 * Revision 1.49  2011/01/27 12:57:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in store without full message
 *
 * Revision 1.48  2011/01/26 16:25:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute storeFullMessage
 *
 * Revision 1.47  2010/12/20 10:44:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * check indices on columns but not by specifying the index name
 *
 * Revision 1.46  2010/12/13 13:28:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made JdbcTransactionalStorabe query generation configurable
 *
 * Revision 1.45  2010/07/12 12:37:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE when connection is null
 *
 * Revision 1.44  2010/03/10 11:05:09  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * increased length of CORRELATIONID
 *
 * Revision 1.43  2010/02/11 14:27:00  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * verify database configuration
 *
 * Revision 1.42  2010/02/05 08:03:35  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * disable 'verify index configuration'
 *
 * Revision 1.41  2010/02/03 14:26:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * verify index configuration
 *
 * Revision 1.40  2010/01/06 15:18:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added type to index ix_ibisstore
 *
 * Revision 1.39  2009/12/31 08:16:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added MAXLABELLEN
 *
 * Revision 1.38  2009/12/29 14:54:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * - increased IBISSTORE with the field LABEL for adding user data
 * - added attribute labelField
 *
 * Revision 1.37  2009/12/23 17:09:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified MessageBrowsing interface to reenable and improve export of messages
 *
 * Revision 1.36  2009/10/26 14:06:24  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * - added MessageLog facility to receivers
 * - added facility to disable records for deletion by the cleanup process
 *
 * Revision 1.35  2009/03/19 11:11:54  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * corrected comments create index for ibisstore
 *
 * Revision 1.34  2009/03/13 14:30:35  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added attributes expiryDateField, dateFieldType and retention
 *
 * Revision 1.33  2008/08/07 11:21:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added schema owners to create script
 *
 * Revision 1.32  2008/08/06 16:29:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected insertQuery for use of prefix
 *
 * Revision 1.31  2008/07/24 12:17:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added messageCount
 * added prefix attribute
 *
 * Revision 1.30  2008/06/26 16:06:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update database always in transaction
 *
 * Revision 1.29  2008/06/24 07:58:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use hint where appropriate
 *
 * Revision 1.28  2008/06/03 15:44:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * compress messages in blobs
 *
 * Revision 1.27  2008/01/11 14:51:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added getTypeString() and getHostString()
 *
 * Revision 1.26  2007/12/27 16:02:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * add grant to SYS.PENDING_TRANSACTIONS to create DDL-comments
 *
 * Revision 1.25  2007/12/10 10:06:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified SQL creation scripts
 *
 * Revision 1.24  2007/11/15 12:30:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * configurable order for browsing
 *
 * Revision 1.23  2007/11/13 14:12:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected checkMessageIdQuery
 *
 * Revision 1.22  2007/10/09 15:34:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * copy changes from Ibis-EJB:
 * added containsMessageId()
 *
 * Revision 1.21  2007/09/10 11:18:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.20  2007/06/12 11:21:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * adapted to new functionality
 *
 * Revision 1.19  2007/06/07 12:27:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * check on not-null for messageid and correlationid
 *
 * Revision 1.18  2007/05/23 09:13:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * incorporated functionality of OracleTransactionalStorage
 * in basic JdbcTransactionalStorage
 *
 * Revision 1.17  2006/12/12 09:57:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

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
 * <tr><td>{@link #setCreateTable(boolean) createTable}</td><td>when set to <code>true</code>, the table is created if it does not exist</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setKeyField(String) keyField}</td><td>the name of the column that contains the primary key of the table</td><td>messageKey</td></tr>
 * <tr><td>{@link #setTypeField(String) typeField}</td><td>the name of the column types are stored in</td><td>type</td></tr>
 * <tr><td>{@link #setHostField(String) hostField}</td><td>the name of the column that stores the hostname of the server</td><td>host</td></tr>
 * <tr><td>{@link #setIdField(String) idField}</td><td>the name of the column messageids are stored in</td><td>messageId</td></tr>
 * <tr><td>{@link #setCorrelationIdField(String) correlationIdField}</td><td>the name of the column correlation-ids are stored in</td><td>correlationId</td></tr>
 * <tr><td>{@link #setDateField(String) dateField}</td><td>the name of the column the timestamp is stored in</td><td>messageDate</td></tr>
 * <tr><td>{@link #setCommentField(String) commentField}</td><td>the name of the column comments are stored in</td><td>comments</td></tr>
 * <tr><td>{@link #setMessageField(String) messageField}</td><td>the name of the column message themselves are stored in</td><td>message</td></tr>
 * <tr><td>{@link #setSlotIdField(String) slotIdField}</td><td>the name of the column slotIds are stored in</td><td>slotId</td></tr>
 * <tr><td>{@link #setExpiryDateField(String) expiryDateField}</td><td>the name of the column the timestamp for expiry is stored in</td><td>expiryDate</td></tr>
 * <tr><td>{@link #setLabelField(String) labelField}</td><td>the name of the column labels are stored in</td><td>label</td></tr>
 * <tr><td>{@link #setKeyFieldType(String) keyFieldType}</td><td>the type of the column that contains the primary key of the table</td><td>INT DEFAULT AUTOINCREMENT</td></tr>
 * <tr><td>{@link #setDateFieldType(String) dateFieldType}</td><td>the type of the column the timestamps are stored in</td><td>TIMESTAMP</td></tr>
 * <tr><td>{@link #setTextFieldType(String) textFieldType}</td><td>the type of the columns messageId and correlationId, slotId and comments are stored in. N.B. (100) is appended for id's, (1000) is appended for comments.</td><td>VARCHAR</td></tr>
 * <tr><td>{@link #setMessageFieldType(String) messageFieldType}</td><td>the type of the column message themselves are stored in</td><td>LONG BINARY</td></tr>
 * <tr><td>{@link #setStoreFullMessage(boolean) storeFullMessage}</td><td>when set to <code>true</code>, the messages are stored compressed</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setBlobsCompressed(boolean) blobsCompressed}</td><td>when set to <code>true</code>, the full message is stored with the log. Can be set to <code>false</code> to reduce table size, by avoiding to store the full message</td><td><code>true</code></td></tr>
 * <tr><td>{@link #setSequenceName(String) sequenceName}</td><td>the name of the sequence used to generate the primary key (only for Oracle)<br>N.B. the default name has been changed in version 4.6</td><td>seq_ibisstore</td></tr>
 * <tr><td>{@link #setIndexName(String) indexName}</td><td>the name of the index, to be used in hints for query optimizer too (only for Oracle)</td><td>IX_IBISSTORE</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td>prefix to be prefixed on all database objects (tables, indices, sequences), e.q. to access a different Oracle Schema</td><td></td></tr>
 * <tr><td>{@link #setRetention(int) retention}</td><td>the time (in days) to keep the record in the database before making it eligible for deletion by a cleanup process. When set to -1, the record will live on forever</td><td>30</td></tr>
 * <tr><td>{@link #setSchemaOwner4Check(String) schemaOwner4Check}</td><td>schema owner to be used to check the database</td><td>&lt;current_schema&gt; (only for Oracle)</td></tr>
 * </table>
 * </p>
 * 
 * For an Oracle database the following objects are used by default:
 *  <pre>
	CREATE TABLE <schema_owner>.IBISSTORE
	(
	MESSAGEKEY NUMBER(10),
	TYPE CHAR(1 CHAR),
	SLOTID VARCHAR2(100 CHAR),
	HOST VARCHAR2(100 CHAR),
	MESSAGEID VARCHAR2(100 CHAR),
	CORRELATIONID VARCHAR2(256 CHAR),
	MESSAGEDATE TIMESTAMP(6),
	COMMENTS VARCHAR2(1000 CHAR),
	MESSAGE BLOB,
	EXPIRYDATE TIMESTAMP(6),
	LABEL VARCHAR2(100 CHAR),
	CONSTRAINT PK_IBISSTORE PRIMARY KEY (MESSAGEKEY)
	);
	
	CREATE INDEX <schema_owner>.IX_IBISSTORE ON <schema_owner>.IBISSTORE (TYPE, SLOTID, MESSAGEDATE);
	CREATE INDEX <schema_owner>.IX_IBISSTORE_02 ON <schema_owner>.IBISSTORE (EXPIRYDATE);
	CREATE SEQUENCE <schema_owner>.SEQ_IBISSTORE;

	GRANT DELETE, INSERT, SELECT, UPDATE ON <schema_owner>.IBISSTORE TO <rolenaam>;
	GRANT SELECT ON <schema_owner>.SEQ_IBISSTORE TO <rolenaam>;
	GRANT SELECT ON SYS.DBA_PENDING_TRANSACTIONS TO <rolenaam>;
	
	COMMIT;
 *  </pre>
 * For an MS SQL Server database the following objects are used by default:
 *  <pre>
	CREATE TABLE IBISSTORE
	(
	MESSAGEKEY int identity,
	TYPE CHAR(1),
	SLOTID VARCHAR(100),
	HOST VARCHAR(100),
	MESSAGEID VARCHAR(100),
	CORRELATIONID VARCHAR(256),
	MESSAGEDATE datetime,
	COMMENTS VARCHAR(1000),
	MESSAGE varbinary(max),
	EXPIRYDATE datetime,
	LABEL VARCHAR(100),
	CONSTRAINT PK_IBISSTORE PRIMARY KEY (MESSAGEKEY)
	);
	
	CREATE INDEX IX_IBISSTORE ON IBISSTORE (TYPE, SLOTID, MESSAGEDATE);
	CREATE INDEX IX_IBISSTORE_02 ON IBISSTORE (EXPIRYDATE);

	COMMIT;
 *  </pre>
 * 
 * For a generic database the following objects are used by default:
 *  <pre>
	CREATE TABLE ibisstore (
	  messageKey INT DEFAULT AUTOINCREMENT CONSTRAINT ibisstore_pk PRIMARY KEY,
	  type CHAR(1), 
	  slotId VARCHAR(100), 
	  host VARCHAR(100),
	  messageId VARCHAR(100), 
	  correlationId VARCHAR(256), 
	  messageDate TIMESTAMP, 
	  comments VARCHAR(1000), 
	  message LONG BINARY),
	  expiryDate TIMESTAMP, 
	  label VARCHAR(100); 

	CREATE INDEX ibisstore_idx ON ibisstore (slotId, messageDate, expiryDate);
 *  </pre>
 * If these objects do not exist, Ibis will try to create them if the attribute createTable="true".
 * 
 * <br/>
 * N.B. Note on using XA transactions:
 * If transactions are used, make sure that the database user can access the table SYS.DBA_PENDING_TRANSACTIONS.
 * If not, transactions present when the server goes down cannot be properly recovered, resulting in exceptions like:
 * <pre>
   The error code was XAER_RMERR. The exception stack trace follows: javax.transaction.xa.XAException
	at oracle.jdbc.xa.OracleXAResource.recover(OracleXAResource.java:508)
   </pre>
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class JdbcTransactionalStorage extends JdbcFacade implements ITransactionalStorage {

	public final static String TYPE_ERRORSTORAGE="E";
	public final static String TYPE_MESSAGELOG_PIPE="L";
	public final static String TYPE_MESSAGELOG_RECEIVER="A";

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
	private String host;
	private boolean active=true;
	private boolean blobsCompressed=true;
	private boolean storeFullMessage=true;
	private String indexName="IX_IBISSTORE";
//	private String index2Name="IX_IBISSTORE_02";
	private String prefix="";
	private int retention = 30;
	private String schemaOwner4Check=null;
	
	private String order=AppConstants.getInstance().getString("browse.messages.order","");
   
	protected static final int MAXIDLEN=100;		
	protected static final int MAXCIDLEN=256;		
	protected static final int MAXCOMMENTLEN=1000;		
	protected static final int MAXLABELLEN=1000;		
    // the following values are only used when the table is created. 
	private String keyFieldType="";
	private String dateFieldType="";
	private String messageFieldType="";
	private String textFieldType="";

	private PlatformTransactionManager txManager;

	protected String insertQuery;
	protected String deleteQuery;
	protected String selectKeyQuery;
	protected String selectListQuery;
	protected String selectContextQuery;
	protected String selectDataQuery;
    protected String checkMessageIdQuery;
	protected String getMessageCountQuery;
    
	protected boolean selectKeyQueryIsDbmsSupported;
	
	// the following for Oracle
	private String sequenceName="seq_ibisstore";
	protected String updateBlobQuery;		
	
	private final String CONTROL_PROPERTY_PREFIX="jdbc.storage.";
	private final String PROPERTY_USE_INDEX_HINT=CONTROL_PROPERTY_PREFIX+"useIndexHint";
	private final String PROPERTY_USE_FIRST_ROWS_HINT=CONTROL_PROPERTY_PREFIX+"useFirstRowsHint";
	private final String PROPERTY_USE_PARAMETERS=CONTROL_PROPERTY_PREFIX+"useParameters";
	private final String PROPERTY_ASSUME_PRIMARY_KEY_UNIQUE=CONTROL_PROPERTY_PREFIX+"assumePrimaryKeyUnique";
	private final String PROPERTY_CHECK_TABLE=CONTROL_PROPERTY_PREFIX+"checkTable";
	private final String PROPERTY_CHECK_INDICES=CONTROL_PROPERTY_PREFIX+"checkIndices";
//	private final String PROPERTY_CHECK_INDEXNAMES=CONTROL_PROPERTY_PREFIX+"checkIndexNames";
	
	
	private final boolean documentQueries=false;
	private boolean useIndexHint;
	private boolean useFirstRowsHint;
	private boolean useParameters;
	private boolean assumePrimaryKeyUnique;
	private boolean checkTable;
	private boolean checkIndices;
//	private boolean checkIndexNames;
	
	
	public JdbcTransactionalStorage() {
		super();
		setTransacted(true);
	}
	    
	protected String getLogPrefix() {
		return "JdbcTransactionalStorage ["+getName()+"] ";
	}

	private void setOperationControls() {
		AppConstants ac = AppConstants.getInstance();
		useIndexHint = ac.getBoolean(PROPERTY_USE_INDEX_HINT, false);
		useFirstRowsHint = ac.getBoolean(PROPERTY_USE_FIRST_ROWS_HINT, true);
		useParameters = ac.getBoolean(PROPERTY_USE_PARAMETERS, true);
		assumePrimaryKeyUnique = ac.getBoolean(PROPERTY_ASSUME_PRIMARY_KEY_UNIQUE, true);
		checkTable = ac.getBoolean(PROPERTY_CHECK_TABLE, false);
		checkIndices = ac.getBoolean(PROPERTY_CHECK_INDICES, true);
//		checkIndexNames = ac.getBoolean(PROPERTY_CHECK_INDEXNAMES, true);
	}
	
	private void checkTableColumnPresent(Connection connection, IDbmsSupport dbms, String columnName) throws JdbcException {
		if (StringUtils.isNotEmpty(columnName)) {
			if (!dbms.isTableColumnPresent(connection, getSchemaOwner4Check(), getTableName(), columnName)) {
				String msg="Table ["+getTableName()+"] has no column ["+columnName+"]";
				ConfigurationWarnings.getInstance().add(getLogPrefix()+msg);
			}
		}
	}

//	private void checkIndexColumnPresent(Connection connection, String indexName, String columnName, int position) {
//		if (StringUtils.isNotEmpty(columnName)) {
//			if (!JdbcUtil.isIndexColumnPresent(connection, getDatabaseType(), getSchemaOwner4Check(), getTableName(), indexName, columnName)) {
//				String msg="Index ["+indexName+"] on table ["+getTableName()+"] has no column ["+columnName+"]";
//				ConfigurationWarnings.getInstance().add(getLogPrefix()+msg);
//			} else {
//				int columnPos=JdbcUtil.getIndexColumnPosition(connection, getDatabaseType(), getSchemaOwner4Check(), getTableName(), indexName, columnName);
//				if (columnPos!=position) {
//					String msg="Index ["+indexName+"] on table ["+getTableName()+"] column ["+columnName+"] has position ["+columnPos+"] instead of ["+position+"]";
//					ConfigurationWarnings.getInstance().add(getLogPrefix()+msg);
//				}
//			}
//		}
//	}


	
	private void checkTable(Connection connection) throws JdbcException {
		IDbmsSupport dbms=getDbmsSupport();
		String schemaOwner=getSchemaOwner4Check();
		log.debug("checking for presence of table ["+getTableName()+"] in schema/catalog ["+schemaOwner+"]");
		if (dbms.isTablePresent(connection, getSchemaOwner4Check(), getTableName())) {
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
			String msg="Table ["+getTableName()+"] not present";
			ConfigurationWarnings.getInstance().add(getLogPrefix()+msg);
		}
	}

	private void checkIndices(Connection connection) {
		checkIndexOnColumnPresent(connection, getKeyField());
		
		ArrayList columnList= new ArrayList();
		if (StringUtils.isNotEmpty(getTypeField())) {
			columnList.add(getTypeField());
		}
		if (StringUtils.isNotEmpty(getSlotIdField())) {
			columnList.add(getSlotIdField());
		}
		if (StringUtils.isNotEmpty(getDateField())) {
			columnList.add(getDateField());
		}
		checkIndexOnColumnsPresent(connection, columnList);

		if (StringUtils.isNotEmpty(getExpiryDateField())) {
			checkIndexOnColumnPresent(connection, getExpiryDateField());
		}
	}

	private void checkIndexOnColumnPresent(Connection connection, String column) {
		if (!JdbcUtil.hasIndexOnColumn(connection, getDatabaseType(), getSchemaOwner4Check(), getTableName(), column)) {
			String msg="table ["+getTableName()+"] has no index on column ["+column+"]";
			ConfigurationWarnings.getInstance().add(getLogPrefix()+msg);
		}
	}

	private void checkIndexOnColumnsPresent(Connection connection, List columns) {
		if (columns!=null && columns.size()>0) {
			if (!JdbcUtil.hasIndexOnColumns(connection, getDatabaseType(), getSchemaOwner4Check(), getTableName(), columns)) {
				String msg="table ["+getTableName()+"] has no index on columns ["+columns.get(0);
				for (int i=1;i<columns.size();i++) {
					msg+=","+columns.get(i);
				}
				msg+="]";
				ConfigurationWarnings.getInstance().add(getLogPrefix()+msg);
			}
		}
	}
	
//	private void checkIndex(Connection connection) {
//		if (JdbcUtil.isIndexPresent(connection, getDatabaseType(), getSchemaOwner4Check(), getTableName(), getIndexName())) {
//			int pos = 0;
//			if (StringUtils.isNotEmpty(getTypeField())) {
//				pos++;
//				checkIndexColumnPresent(connection,getIndexName(),getTypeField(),pos);
//			}
//			if (StringUtils.isNotEmpty(getSlotIdField())) {
//				pos++;
//				checkIndexColumnPresent(connection,getIndexName(),getSlotIdField(),pos);
//			}
//			if (StringUtils.isNotEmpty(getDateField())) {
//				pos++;
//				checkIndexColumnPresent(connection,getIndexName(),getDateField(),pos);
//			}
//		} else {
//			String msg="Index ["+getIndexName()+"] on table ["+getTableName()+"] not present";
//			ConfigurationWarnings.getInstance().add(getLogPrefix()+msg);
//		}
//	}

	private void checkSequence(Connection connection) {
		if (JdbcUtil.isSequencePresent(connection, getDatabaseType(), getSchemaOwner4Check(), getSequenceName())) {
			//no more checks
		} else {
			String msg="Sequence ["+getSequenceName()+"] not present";
			ConfigurationWarnings.getInstance().add(getLogPrefix()+msg);
		}
	}

//	private void checkIndex2(Connection connection) {
//		if (JdbcUtil.isIndexPresent(connection, getDatabaseType(), getSchemaOwner4Check(), getTableName(), getIndex2Name())) {
//			int pos = 0;
//			if (StringUtils.isNotEmpty(getExpiryDateField())) {
//				pos++;
//				checkIndexColumnPresent(connection,getIndex2Name(),getExpiryDateField(),pos);
//			}
//		} else {
//			String msg="Index ["+getIndex2Name()+"] on table ["+getTableName()+"] not present";
//			ConfigurationWarnings.getInstance().add(getLogPrefix()+msg);
//		}
//	}


	private void checkDatabase() throws ConfigurationException {
		Connection connection=null;
		try {
//			if (getDatabaseType()==DbmsSupportFactory.DBMS_ORACLE) {
				if (checkTable || checkIndices) {
					if (StringUtils.isNotEmpty(getSchemaOwner4Check())){
						connection=getConnection();
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
//							if (StringUtils.isNotEmpty(getIndexName())) {
//								checkIndex(connection);
//							} else {
//								throw new ConfigurationException("Attribute [indexName] is not set");
//							}
//							if (StringUtils.isNotEmpty(getIndex2Name())) {
//								checkIndex2(connection);
//							} else {
//								throw new ConfigurationException("Attribute [index2Name] is not set");
//							}
						}
					} else {
						ConfigurationWarnings.getInstance().add(getLogPrefix()+"Could not check database regarding table [" + getTableName() + "]: Schema owner is unknown");
					}
				} else {
					log.info(getLogPrefix()+"checking of table and indices is not enabled");
				}
//			} else {
//				ConfigurationWarnings.getInstance().add(getLogPrefix()+"Could not check database regarding table [" + getTableName() + "]: Not an Oracle database");
//			}
		} catch (JdbcException e) {
			ConfigurationWarnings.getInstance().add(getLogPrefix()+"Could not check database regarding table [" + getTableName() + "]: "+e.getMessage());
		} finally {
			try {
				if (connection!=null) {
					connection.close();
				}
			} catch (SQLException e1) {
				log.warn("could not close connection",e1);
			}
		}
	}
	
	/**
	 * Creates a connection, checks if the table is existing and creates it when necessary
	 */
	public void configure() throws ConfigurationException {
//		super.configure();
		setOperationControls();
		if (StringUtils.isEmpty(getTableName())) {
			throw new ConfigurationException("Attribute [tableName] is not set");
		}
		if (useIndexHint && StringUtils.isEmpty(getIndexName())) {
			throw new ConfigurationException("Attribute [indexName] is not set and useIndexHint=true");
		}
		if (StringUtils.isEmpty(getSequenceName())) {
			throw new ConfigurationException("Attribute [sequenceName] is not set");
		}
//		if (StringUtils.isEmpty(getIndex2Name())) {
//			throw new ConfigurationException("Attribute [index2Name] is not set");
//		}
		if (StringUtils.isNotEmpty(getHostField())) {
			host=Misc.getHostname();
		}
		createQueryTexts(getDbmsSupport());
		checkDatabase();
	}

	public void open() throws SenderException {
		try {
			initialize(getDbmsSupport());
		} catch (JdbcException e) {
			throw new SenderException(e);
		} catch (SQLException e) {
			throw new SenderException(getLogPrefix()+"exception creating table ["+getTableName()+"]",e);
		} 
	}

	public void close() {
	}

	/**
	 * change datatypes used for specific database vendor. 
	 */
	protected void setDataTypes(IDbmsSupport dbmsSupport) {
		if (StringUtils.isEmpty(getKeyFieldType())) setKeyFieldType(dbmsSupport.getAutoIncrementKeyFieldType());
		if (StringUtils.isEmpty(getDateFieldType())) setDateFieldType(dbmsSupport.getTimestampFieldType());
		if (StringUtils.isEmpty(getTextFieldType())) setTextFieldType(dbmsSupport.getTextFieldType());
		if (StringUtils.isEmpty(getMessageFieldType())) setMessageFieldType(dbmsSupport.getBlobFieldType());
	}

	protected void createQueryTexts(IDbmsSupport dbmsSupport) throws ConfigurationException {
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
						") VALUES ("+
						(keyFieldsNeedsInsert?dbmsSupport.autoIncrementInsertValue(getPrefix()+getSequenceName())+",":"")+
						(StringUtils.isNotEmpty(getTypeField())?"?,":"")+
						(StringUtils.isNotEmpty(getSlotId())?"?,":"")+
						(StringUtils.isNotEmpty(getHostField())?"?,":"")+
						(StringUtils.isNotEmpty(getLabelField())?"?,":"")+
						"?,?,?,?,?"+
						(isStoreFullMessage()?","+(blobFieldsNeedsEmptyBlobInsert?dbmsSupport.emptyBlobValue():"?"):"")+")";
		deleteQuery = "DELETE FROM "+getPrefix()+getTableName()+ getWhereClause(getKeyField()+"=?",true);
		selectKeyQuery = dbmsSupport.getInsertedAutoIncrementValueQuery(getPrefix()+getSequenceName());
		selectKeyQueryIsDbmsSupported=StringUtils.isNotEmpty(selectKeyQuery);
		if (!selectKeyQueryIsDbmsSupported) {
			selectKeyQuery = "SELECT max("+getKeyField()+") FROM "+getPrefix()+getTableName()+ 
							getWhereClause(getIdField()+"=?"+
										" AND " +getCorrelationIdField()+"=?"+
										" AND "+getDateField()+"=?",false);
		}
		String listClause=getListClause();
		selectContextQuery = "SELECT "+listClause+ getWhereClause(getKeyField()+"=?",true);
		selectListQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport)+provideFirstRowsHintAfterFirstKeyword(dbmsSupport)+ listClause+ getWhereClause(null,false)+
						  " ORDER BY "+getDateField()+(StringUtils.isNotEmpty(getOrder())?" " + getOrder():"")+provideTrailingFirstRowsHint(dbmsSupport);
		selectDataQuery = "SELECT "+getMessageField()+  " FROM "+getPrefix()+getTableName()+ getWhereClause(getKeyField()+"=?",true);
        checkMessageIdQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + getIdField() +" FROM "+getPrefix()+getTableName()+ getWhereClause(getIdField() +"=?",false);
		getMessageCountQuery = "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport) + "COUNT(*) FROM "+getPrefix()+getTableName()+ getWhereClause(null,false);
		if (dbmsSupport.mustInsertEmptyBlobBeforeData()) {
			updateBlobQuery = dbmsSupport.getUpdateBlobQuery(getPrefix()+getTableName(), getMessageField(), getKeyField()); 
		}
		if (documentQueries && log.isDebugEnabled()) {
			log.debug(
					documentQuery("insertQuery",insertQuery,"Voeg een regel toe aan de tabel")+
					documentQuery("deleteQuery",deleteQuery,"Verwijder een regel uit de tabel, via de primary key")+
					documentQuery("selectKeyQuery",selectKeyQuery,"Haal de huidige waarde op van de sequence voor messageKey")+
					documentQuery("selectContextQuery",selectContextQuery,"Haal de niet blob velden van een regel op, via de primary key")+
					documentQuery("selectListQuery",selectListQuery,"Haal een lijst van regels op, op volgorde van de index. Haalt niet altijd alle regels op")+
					documentQuery("selectDataQuery",selectDataQuery,"Haal de blob van een regel op, via de primary key")+
					documentQuery("checkMessageIdQuery",checkMessageIdQuery,"bekijk of een messageId bestaat, NIET via de primary key. Echter: het aantal fouten is over het algemeen relatief klein. De index selecteert dus een beperkt aantal rijen uit een groot aantal.")+
					documentQuery("getMessageCountQuery",getMessageCountQuery,"tel het aantal regels in een gedeelte van de tabel. Kan via index.")+
					documentQuery("updateBlobQuery",updateBlobQuery,"Geef de blob een waarde, via de primary key")
//					+"\n"
//					+"\n- slotId en type zou via ? kunnen"
//					+"\n- selectListQuery zou in sommige gevallen extra filters in de where clause kunnen krijgen"
//					+"\n- selectListQuery zou FIRST_ROWS(500) hint kunnen krijgen"
//					+"\n- we zouden de index hint via een custom property aan en uit kunnen zetten"
					);
		}
	}

	private String getListClause() {
		return getKeyField()+","+getIdField()+","+getCorrelationIdField()+","+getDateField()+","+getExpiryDateField()+
		(StringUtils.isNotEmpty(getTypeField())?","+getTypeField():"")+
		(StringUtils.isNotEmpty(getHostField())?","+getHostField():"")+
		(StringUtils.isNotEmpty(getLabelField())?","+getLabelField():"")+
		","+getCommentField()+ " FROM "+getPrefix()+getTableName();
	}
	
	private String getSelectListQuery(IDbmsSupport dbmsSupport, Date startTime, Date endTime, boolean forceDescending) {
		String whereClause=null;
		if (startTime!=null) {
			whereClause=getDateField()+">=?";
		}
		if (endTime!=null) {
			whereClause=Misc.concatStrings(whereClause, " AND ", getDateField()+"<?");
		}
		return "SELECT "+provideIndexHintAfterFirstKeyword(dbmsSupport)+provideFirstRowsHintAfterFirstKeyword(dbmsSupport)+ getListClause()+ getWhereClause(whereClause,false)+
		  " ORDER BY "+getDateField()+(forceDescending?" DESC ":" "+getOrder()+" ")+provideTrailingFirstRowsHint(dbmsSupport);
	}
	
	
	private String documentQuery(String name, String query, String purpose) {
		return "\n"+name+(purpose!=null?"\n"+purpose:"")+"\n"+query+"\n";
	}

	private String provideIndexHintAfterFirstKeyword(IDbmsSupport dbmsSupport) {
		if (useIndexHint) {
			return dbmsSupport.provideIndexHintAfterFirstKeyword(getPrefix()+getTableName(), getPrefix()+getIndexName());
		}
		return "";
	}

	private String provideFirstRowsHintAfterFirstKeyword(IDbmsSupport dbmsSupport) {
		if (useFirstRowsHint) {
			return dbmsSupport.provideFirstRowsHintAfterFirstKeyword(100);
		}
		return "";
	}
	private String provideTrailingFirstRowsHint(IDbmsSupport dbmsSupport) {
		if (useFirstRowsHint) {
			return dbmsSupport.provideTrailingFirstRowsHint(100);
		}
		return "";
	}

	/**
	 *	Checks if table exists, and creates when necessary. 
	 */
	public void initialize(IDbmsSupport dbmsSupport) throws JdbcException, SQLException, SenderException {
		Connection conn = getConnection();
		try {
			boolean tableMustBeCreated;

			if (checkIfTableExists) {
				try {
					tableMustBeCreated = !JdbcUtil.tableExists(conn, getPrefix()+getTableName());
					if (!isCreateTable() && tableMustBeCreated) {
						throw new SenderException("table ["+getPrefix()+getTableName()+"] does not exist");
					}
					 log.info("table ["+getPrefix()+getTableName()+"] does "+(tableMustBeCreated?"NOT ":"")+"exist");
				} catch (SQLException e) {
					log.warn(getLogPrefix()+"exception determining existence of table ["+getPrefix()+getTableName()+"] for transactional storage, trying to create anyway."+ e.getMessage());
					tableMustBeCreated=true;
				}
			} else {
				log.info("did not check for existence of table ["+getPrefix()+getTableName()+"]");
				tableMustBeCreated = false;
			}

			if (isCreateTable() && tableMustBeCreated || forceCreateTable) {
				log.info(getLogPrefix()+"creating table ["+getPrefix()+getTableName()+"] for transactional storage");
				Statement stmt = conn.createStatement();
				try {
					createStorage(conn, stmt, dbmsSupport);
				} finally {
					stmt.close();
					conn.commit();
				}
			}
		} finally {
			conn.close();
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



	/**
	 * Retrieves the value of the primary key for the record just inserted. 
	 */
	protected String retrieveKey(Connection conn, String messageId, String correlationId, Timestamp receivedDateTime) throws SQLException, SenderException {
		PreparedStatement stmt=null;
		
		try {			
			if (log.isDebugEnabled()) log.debug("preparing key retrieval statement ["+selectKeyQuery+"]");
			stmt = conn.prepareStatement(selectKeyQuery);			
			if (!selectKeyQueryIsDbmsSupported) {
				int paramPos=applyStandardParameters(stmt, true, false);
				stmt.setString(paramPos++,messageId);
				stmt.setString(paramPos++,correlationId);
				stmt.setTimestamp(paramPos++, receivedDateTime);
			}
	
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

	protected String storeMessageInDatabase(Connection conn, String messageId, String correlationId, Timestamp receivedDateTime, String comments, String label, Serializable message) throws IOException, SQLException, JdbcException, SenderException {
		PreparedStatement stmt = null;
		try { 
			IDbmsSupport dbmsSupport=getDbmsSupport();
			if (log.isDebugEnabled()) log.debug("preparing insert statement ["+insertQuery+"]");
			stmt = conn.prepareStatement(insertQuery);			
			stmt.clearParameters();
			int parPos=0;
			
			if (StringUtils.isNotEmpty(getTypeField())) {
				stmt.setString(++parPos,type);
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
			if (type.equalsIgnoreCase(TYPE_MESSAGELOG_PIPE) || type.equalsIgnoreCase(TYPE_MESSAGELOG_RECEIVER)) {
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
				stmt.execute();
				return null;
			}
			if (!dbmsSupport.mustInsertEmptyBlobBeforeData()) {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				
				if (isBlobsCompressed()) {
					DeflaterOutputStream dos = new DeflaterOutputStream(out);
					ObjectOutputStream oos = new ObjectOutputStream(dos);
					oos.writeObject(message);
					dos.close();
				} else {
					ObjectOutputStream oos = new ObjectOutputStream(out);
					oos.writeObject(message);
				}
				
				stmt.setBytes(++parPos, out.toByteArray());

				stmt.execute();
				return null;
			}
			stmt.execute();

			if (log.isDebugEnabled()) log.debug("preparing select statement ["+selectKeyQuery+"]");
			stmt = conn.prepareStatement(selectKeyQuery);			
			ResultSet rs = null;
			try {
				// retrieve the key
				rs = stmt.executeQuery();
				if (!rs.next()) {
					throw new SenderException("could not retrieve key of stored message");
				}
				String newKey = rs.getString(1);
				rs.close();

				// and update the blob
				if (log.isDebugEnabled()) log.debug("preparing update statement ["+updateBlobQuery+"]");
				stmt = conn.prepareStatement(updateBlobQuery);			
				stmt.clearParameters();
				stmt.setString(1,newKey);

				rs = stmt.executeQuery();
				if (!rs.next()) {
					throw new SenderException("could not retrieve row for stored message ["+ messageId+"]");
				}
//						String newKey = rs.getString(1);
//						BLOB blob = (BLOB)rs.getBlob(2);
				Object blobHandle=dbmsSupport.getBlobUpdateHandle(rs, 1);
				OutputStream out = dbmsSupport.getBlobOutputStream(rs, 1, blobHandle);
//					OutputStream out = JdbcUtil.getBlobUpdateOutputStream(rs,1);
				if (isBlobsCompressed()) {
					DeflaterOutputStream dos = new DeflaterOutputStream(out);
					ObjectOutputStream oos = new ObjectOutputStream(dos);
					oos.writeObject(message);
					oos.close();
					dos.close();
				} else {
					ObjectOutputStream oos = new ObjectOutputStream(out);
					oos.writeObject(message);
					oos.close();
				}
				out.close();
				dbmsSupport.updateBlob(rs, 1, blobHandle);
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



	public String storeMessage(String messageId, String correlationId, Date receivedDate, String comments, String label, Serializable message) throws SenderException {
		TransactionStatus txStatus=null;
		if (txManager!=null) {
			txStatus = txManager.getTransaction(TXREQUIRED);
		}
		try {
			Connection conn;
			String result;
			if (messageId==null) {
				throw new SenderException("messageId cannot be null");
			}
			if (correlationId==null) {
				throw new SenderException("correlationId cannot be null");
			}
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
				throw new SenderException("cannot serialize message",e);
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					log.error("error closing JdbcConnection", e);
				}
			}
		} finally {
			if (txStatus!=null) {
				txManager.commit(txStatus);
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

		public IMessageBrowsingIteratorItem next() throws ListenerException {
			if (!current) {
				advance();
			}
			if (!current) {
				throw new ListenerException("read beyond end of resultset");
			}
			current=false;
			return new JdbcTransactionalStorageIteratorItem(rs,false);
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
		return getIterator(null,null,false);
	}
	public IMessageBrowsingIterator getIterator(Date startTime, Date endTime, boolean forceDescending) throws ListenerException {
		Connection conn;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new ListenerException(e);
		}
		try {
			String query;
			if (startTime==null && endTime==null) {
				query=selectListQuery;
			} else {
				query=getSelectListQuery(getDbmsSupport(), startTime, endTime, forceDescending);
			}
			if (log.isDebugEnabled()) log.debug("preparing selectListQuery ["+query+"]");
			PreparedStatement stmt = conn.prepareStatement(query);
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
		} catch (SQLException e) {
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

	
	

	public void deleteMessage(String messageId) throws ListenerException {
		Connection conn;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new ListenerException(e);
		}
		try {
			PreparedStatement stmt = conn.prepareStatement(deleteQuery);	
			applyStandardParameters(stmt, messageId, true);
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

	private Object retrieveObject(ResultSet rs, int columnIndex, boolean compressed) throws ClassNotFoundException, JdbcException, IOException, SQLException {
		InputStream blobStream=null;
		try {
			Blob blob = rs.getBlob(columnIndex);
			if (blob==null) {
				return null;
			}
			if (compressed) {
				blobStream=new InflaterInputStream(JdbcUtil.getBlobInputStream(blob, columnIndex+""));
			} else {
				blobStream=JdbcUtil.getBlobInputStream(blob, columnIndex+"");
			}
			ObjectInputStream ois = new ObjectInputStream(blobStream);
			Object result = ois.readObject();
			ois.close();
			return result;
		} finally {
			if (blobStream!=null) {
				blobStream.close();
			}
		}
	}

	
	protected Object retrieveObject(ResultSet rs, int columnIndex) throws ClassNotFoundException, JdbcException, IOException, SQLException {
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

	public int getMessageCount() throws ListenerException {
		Connection conn;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new ListenerException(e);
		}
		try {
			PreparedStatement stmt = conn.prepareStatement(getMessageCountQuery);			
			applyStandardParameters(stmt, false, false);
			ResultSet rs =  stmt.executeQuery();

			if (!rs.next()) {
				log.warn(getLogPrefix()+"no message count found");
				return 0;
			}
			return rs.getInt(1);			
			
		} catch (Exception e) {
			throw new ListenerException("cannot determine message count",e);
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				log.error("error closing JdbcConnection", e);
			}
		}
	}


    public boolean containsMessageId(String originalMessageId) throws ListenerException {
		Connection conn;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new ListenerException(e);
		}
		try {
			PreparedStatement stmt = conn.prepareStatement(checkMessageIdQuery);			
			applyStandardParameters(stmt, originalMessageId, false);
			ResultSet rs =  stmt.executeQuery();

			if (!rs.next()) {
				return false;
			}
			
			return true;
			
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
 
	public IMessageBrowsingIteratorItem getContext(String messageId) throws ListenerException {
		Connection conn;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new ListenerException(e);
		}
		try {
			PreparedStatement stmt = conn.prepareStatement(selectContextQuery);			
			applyStandardParameters(stmt, messageId, true);
			ResultSet rs =  stmt.executeQuery();

			if (!rs.next()) {
				throw new ListenerException("could not retrieve context for messageid ["+ messageId+"]");
			}
			return new JdbcTransactionalStorageIteratorItem(rs,true);
			
		} catch (Exception e) {
			throw new ListenerException("cannot read context",e);
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
			applyStandardParameters(stmt, messageId, true);
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


	private class JdbcTransactionalStorageIteratorItem implements IMessageBrowsingIteratorItem {

		private ResultSet rs;
		private boolean closeOnRelease;
		
		public JdbcTransactionalStorageIteratorItem(ResultSet rs, boolean closeOnRelease) {
			super();
			this.rs=rs;
			this.closeOnRelease=closeOnRelease;
		}
		
		public String getId() throws ListenerException {
			try {
				return rs.getString(getKeyField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		public String getOriginalId() throws ListenerException {
			try {
				return rs.getString(getIdField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		public String getCorrelationId() throws ListenerException {
			try {
				return rs.getString(getCorrelationIdField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		public Date getInsertDate() throws ListenerException {
			try {
				return rs.getTimestamp(getDateField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
		public Date getExpiryDate() throws ListenerException {
			try {
				return rs.getTimestamp(getExpiryDateField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}
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

		public String getCommentString() throws ListenerException {
			try {
				return rs.getString(getCommentField());
			} catch (SQLException e) {
				throw new ListenerException(e);
			}
		}

		public void release() {
			if (closeOnRelease) {
				JdbcUtil.fullClose(rs);
			}
		}
		
		
	}




	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" in table ["+getTableName()+"]";
	}

	public void setSequenceName(String string) {
		sequenceName = string;
	}
	public String getSequenceName() {
		return sequenceName;
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

	public String getExpiryDateField() {
		return expiryDateField;
	}

	public String getLabelField() {
		return labelField;
	}

	public void setCommentField(String string) {
		commentField = string;
	}

	public void setDateField(String string) {
		dateField = string;
	}

	public void setExpiryDateField(String string) {
		expiryDateField = string;
	}

	public void setLabelField(String string) {
		labelField = string;
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

	public String getType() {
		return type;
	}

	public void setType(String string) {
		type = string;
	}

	public String getTypeField() {
		return typeField;
	}

	public void setTypeField(String string) {
		typeField = string;
	}


	public void setCreateTable(boolean b) {
		createTable = b;
	}
	public boolean isCreateTable() {
		return createTable;
	}

	public void setActive(boolean b) {
		active = b;
	}
	public boolean isActive() {
		return active;
	}

	public void setHostField(String string) {
		hostField = string;
	}
	public String getHostField() {
		return hostField;
	}

	public void setOrder(String string) {
		order = string;
	}
	public String getOrder() {
		return order;
	}

	public void setBlobsCompressed(boolean b) {
		blobsCompressed = b;
	}
	public boolean isBlobsCompressed() {
		return blobsCompressed;
	}

	public void setIndexName(String string) {
		indexName = string;
	}
	public String getIndexName() {
		return indexName;
	}

//	public void setIndex2Name(String string) {
//		index2Name = string;
//	}
//	public String getIndex2Name() {
//		return index2Name;
//	}

	public void setTxManager(PlatformTransactionManager manager) {
		txManager = manager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}

	public void setPrefix(String string) {
		prefix = string;
	}
	public String getPrefix() {
		return prefix;
	}

	public void setRetention(int retention) {
		this.retention = retention;
	}

	public int getRetention() {
		return retention;
	}

	public void setSchemaOwner4Check(String string) {
		schemaOwner4Check = string;
	}

	public String getSchemaOwner4Check() {
		if (schemaOwner4Check==null) {
			IDbmsSupport dbms=getDbmsSupport();
			Connection conn=null;
			try {
				conn=getConnection();
				schemaOwner4Check=dbms.getSchema(conn);
			} catch (Exception e) {
				log.warn("Exception determining current schema", e);
				return "";
			} finally {
				try {
					if (conn!=null) {
						conn.close();
					}
				} catch (SQLException e1) {
					log.warn("exception closing connection for schema owner",e1);
				}
			}
		}
		return schemaOwner4Check;
	}

	public boolean isStoreFullMessage() {
		return storeFullMessage;
	}
	public void setStoreFullMessage(boolean storeFullMessage) {
		this.storeFullMessage = storeFullMessage;
	}
}
