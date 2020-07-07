package nl.nn.adapterframework.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.JdbcUtil;

public class JdbcTransactionalStorageIteratorItem implements IMessageBrowsingIteratorItem {

	private Connection conn;
	private ResultSet rs;
	private boolean closeOnRelease;

	private String keyField = "messageKey";
	private String idField = "messageId";
	private String correlationIdField = "correlationId";
	private String dateField = "messageDate";
	private String commentField = "comments";
	private String expiryDateField = "expiryDate";
	private String labelField = "label";
	private String typeField="type";
	private String hostField="host";

	public JdbcTransactionalStorageIteratorItem(Connection conn, ResultSet rs, boolean closeOnRelease) {
		super();
		this.conn=conn;
		this.rs=rs;
		this.closeOnRelease=closeOnRelease;
	}
	
	@Override
	public String getId() throws ListenerException {
		try {
			return rs.getString(keyField);
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public String getOriginalId() throws ListenerException {
		try {
			return rs.getString(idField);
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public String getCorrelationId() throws ListenerException {
		try {
			return rs.getString(correlationIdField);
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public Date getInsertDate() throws ListenerException {
		try {
			return rs.getTimestamp(dateField);
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public Date getExpiryDate() throws ListenerException {
		try {
			return rs.getTimestamp(expiryDateField);
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public String getType() throws ListenerException {
		if (StringUtils.isEmpty(typeField)) {
			return null;
		}
		try {
			return rs.getString(typeField);
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}
	@Override
	public String getHost() throws ListenerException {
		if (StringUtils.isEmpty(hostField)) {
			return null;
		}
		try {
			return rs.getString(hostField);
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public String getLabel() throws ListenerException {
		if (StringUtils.isEmpty(labelField)) {
			return null;
		}
		try {
			return rs.getString(labelField);
		} catch (SQLException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public String getCommentString() throws ListenerException {
		try {
			return rs.getString(commentField);
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