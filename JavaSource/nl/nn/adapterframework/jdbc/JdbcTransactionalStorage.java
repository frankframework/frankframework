/*
 * $Log: JdbcTransactionalStorage.java,v $
 * Revision 1.1  2004-03-24 13:28:20  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ITransactionalStorage;
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
 * <tr><td>{@link #setIdField(String) idField}</td><td>the name of the column messageids are stored in</td><td>messageid</td></tr>
 * <tr><td>{@link #setMessageField(String) messageField}</td><td>the name of the column message themselves are stored in</td><td>message</td></tr>
 * </table>
 * </p>
 * 
 * <p>$Id: JdbcTransactionalStorage.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $</p>
 * @author  Gerrit van Brakel
 * @since 	4.1
 */
public class JdbcTransactionalStorage extends DirectQuerySender implements ITransactionalStorage {
	public static final String version="$Id: JdbcTransactionalStorage.java,v 1.1 2004-03-24 13:28:20 L190409 Exp $";
	
    private String tableName="inprocstore";
    private String idField="messageid";
    private String messageField="message";
	
	public JdbcTransactionalStorage() {
		super();
		setTransacted(true);
	}
	    
	protected String getLogPrefix() {
		return "JdbcTransactionalStorage ["+getName()+"] ";
	}
	
	
	public void createTable(Connection conn) throws SQLException {
		String query=null;
		Statement stmt = conn.createStatement();
		query="CREATE TABLE "+getTableName()+" ("+getIdField()+" VARCHAR NOT NULL PRIMARY KEY, "+getMessageField()+" VARCHAR)";
		log.debug(getLogPrefix()+"creating table ["+getTableName()+"] using query ["+query+"]");
		stmt.execute(query);
	}	
	
	public void configure() throws ConfigurationException {
		super.configure();
		try {
			Connection conn = getConnection();
			if (!JdbcUtil.tableExists(conn, getTableName())) {
				log.info(getLogPrefix()+"creating table ["+getTableName()+"] for transactional storage");
				createTable(conn);
			}
			conn.close();
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix()+"exception creating table ["+getTableName()+"]",e);
		} 
	}


	public String getPhysicalDestinationName() {
		return super.getPhysicalDestinationName()+" in table ["+getTableName()+"]";
	}

	public void storeMessage(String messageId, String message) throws SenderException {
		sendMessage(messageId,
					"INSERT INTO "+tableName+" ("+idField+","+messageField+") VALUES ('"+messageId+"','"+message+"')");
	}

	public void deleteMessage(String messageId) throws SenderException {
		sendMessage(messageId,
					"DELETE FROM "+tableName+" WHERE "+idField+"='"+messageId+"'");
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

}
