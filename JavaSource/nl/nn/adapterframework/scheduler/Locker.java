/*
 * $Log: Locker.java,v $
 * Revision 1.1  2009-03-13 14:36:11  m168309
 * Introduction of Locker (child element of Job)
 *
 */
package nl.nn.adapterframework.scheduler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.JdbcFacade;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Locker of scheduler jobs.
 *
 * Tries to set a lock (by inserting a record in the database table IbisLock) and only if this is done
 * successfully the job is executed.
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.scheduler.Locker</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setObjectId(String) objectId}</td><td>identifier for this lock</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setType(String) type}</td><td>type for this lock: P(ermanent) ot T(emporary). A temporary lock is deleted after the job has completed</td><td>T</td></tr>
 * <tr><td>{@link #setDateFormatSuffix(String) dateFormatSuffix}</td><td>format for date which is added after <code>objectId</code> (e.g. yyyyMMdd to be sure the job is executed only once a day)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRetention(int) retention}</td><td>the time (for type=P in days and for type=T in hours) to keep the record in the database before making it eligible for deletion by a cleanup process</td><td>30 days (type=P), 4 hours (type=T)</td></tr>
 * </table>
 * </p>
 * 
 * For an Oracle database the following objects are used:
 *  <pre>
	CREATE TABLE <schema_owner>.IBISLOCK
	(
	OBJECTID VARCHAR2(100 CHAR),
	TYPE CHAR(1 CHAR),
	HOST VARCHAR2(100 CHAR),
	CREATIONDATE TIMESTAMP(6),
	EXPIRYDATE TIMESTAMP(6)
	CONSTRAINT PK_IBISLOCK PRIMARY KEY (OBJECTID)
	);

	CREATE INDEX <schema_owner>.IX_IBISLOCK ON <schema_owner>.IBISLOCK
	(EXPIRYDATE);

	GRANT DELETE, INSERT, SELECT, UPDATE ON <schema_owner>.IBISLOCK TO <rolenaam>;
	GRANT SELECT ON SYS.DBA_PENDING_TRANSACTIONS TO <rolenaam>;
		
	COMMIT;
 *  </pre>
 * 
 * @author  Peter Leeuwenburgh
 * @version Id
 */
public class Locker extends JdbcFacade {
	protected Logger log=LogUtil.getLogger(this);

	private String name;
	private String objectId;
	private String type = "T";
	private String dateFormatSuffix;
	private int retention = -1;
	private String insertQuery = "INSERT INTO ibisLock (objectId, type, host, creationDate, expiryDate) VALUES (?, ?, ?, ?, ?)";
	private String deleteQuery = "DELETE FROM ibisLock WHERE objectId=?";
	private SimpleDateFormat formatter;

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getObjectId())) {
			throw new ConfigurationException(getLogPrefix()+ "an objectId must be specified");
		}
		if (!getType().equalsIgnoreCase("T") && !getType().equalsIgnoreCase("P")) {
			throw new ConfigurationException(getLogPrefix()+"illegal value for type ["+getType()+"], must be 'T' or 'P'");
		}
		if (StringUtils.isNotEmpty(getDateFormatSuffix())) {
			try {
				Date currentDate = new Date();
				formatter = new SimpleDateFormat(getDateFormatSuffix());
			} catch (IllegalArgumentException ex){
				throw new ConfigurationException(getLogPrefix()+"has an illegal value for dateFormat", ex);
			}
		}
		if (retention<0) {
			if (getType().equalsIgnoreCase("T")) {
				retention = 4;
			} else {
				retention = 30;
			}
		}
	}

	protected String lock() throws SenderException{
		Date date = new Date();
		String objectIdWithSuffix = getObjectId();
		if (StringUtils.isNotEmpty(getDateFormatSuffix())) {
			String formattedDate = formatter.format(date);
			objectIdWithSuffix = objectIdWithSuffix.concat(formattedDate);
		}

		log.debug("preparing to set lock [" + objectIdWithSuffix + "]");

		Connection conn;
		String result;
		try {
			conn = getConnection();
		} catch (JdbcException e) {
			throw new SenderException(e);
		}
		try {
			PreparedStatement stmt = conn.prepareStatement(insertQuery);			
			stmt.clearParameters();
			stmt.setString(1,objectIdWithSuffix);
			stmt.setString(2,getType());
			stmt.setString(3,Misc.getHostname());
			stmt.setTimestamp(4, new Timestamp(date.getTime()));
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			if (getType().equalsIgnoreCase("T")) {
				cal.add(Calendar.HOUR_OF_DAY, getRetention());
			} else {
				cal.add(Calendar.DAY_OF_MONTH, getRetention());
			}
			stmt.setTimestamp(5, new Timestamp(cal.getTime().getTime()));
			stmt.executeUpdate();
			log.debug("lock ["+objectIdWithSuffix+"] set");
			return objectIdWithSuffix;
		} catch (SQLException e) {
			log.debug(getLogPrefix()+"error executing query ["+insertQuery+"] (as part of locker)", e);
			return null;
		} finally {
			try {
				conn.close();
			} catch (SQLException e) {
				log.error("error closing JdbcConnection", e);
			}
		}
	}

	protected void unlock(String objectIdWithSuffix) throws SenderException{
		if (getType().equalsIgnoreCase("T")) {
			log.debug("preparing to remove lock [" + objectIdWithSuffix + "]");

			Connection conn;
			String result;
			try {
				conn = getConnection();
			} catch (JdbcException e) {
				throw new SenderException(e);
			}
			try {
				PreparedStatement stmt = conn.prepareStatement(deleteQuery);			
				stmt.clearParameters();
				stmt.setString(1,objectIdWithSuffix);
				stmt.executeUpdate();
				log.debug("lock ["+objectIdWithSuffix+"] removed");
			} catch (SQLException e) {
				log.debug(getLogPrefix()+"error executing query ["+deleteQuery+"] (as part of locker)", e);
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					log.error("error closing JdbcConnection", e);
				}
			}
		}
	}

	protected String getLogPrefix() {
		return getName()+" "; 
	}	

	public void setName(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}

	public void setDateFormatSuffix(String dateFormatSuffix) {
		this.dateFormatSuffix = dateFormatSuffix;
	}

	public String getDateFormatSuffix() {
		return dateFormatSuffix;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setRetention(int retention) {
		this.retention = retention;
	}

	public int getRetention() {
		return retention;
	}
}