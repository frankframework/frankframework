/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.JdbcFacade;

import org.apache.commons.lang3.StringUtils;

/**
 * Locker of scheduler jobs and pipes.
 *
 * Tries to set a lock (by inserting a record in the database table IbisLock) and only if this is done
 * successfully the job is executed.
 * 
 * For an Oracle database the following objects are used:
 *  <pre>
	CREATE TABLE &lt;schema_owner&gt;.IBISLOCK
	(
	OBJECTID VARCHAR2(100 CHAR),
	TYPE CHAR(1 CHAR),
	HOST VARCHAR2(100 CHAR),
	CREATIONDATE TIMESTAMP(6),
	EXPIRYDATE TIMESTAMP(6)
	CONSTRAINT PK_IBISLOCK PRIMARY KEY (OBJECTID)
	);

	CREATE INDEX &lt;schema_owner&gt;.IX_IBISLOCK ON &lt;schema_owner&gt;.IBISLOCK
	(EXPIRYDATE);

	GRANT DELETE, INSERT, SELECT, UPDATE ON &lt;schema_owner&gt;.IBISLOCK TO &lt;rolename&gt;;
	GRANT SELECT ON SYS.DBA_PENDING_TRANSACTIONS TO &lt;rolename&gt;;
		
	COMMIT;
 *  </pre>
 * 
 * @author  Peter Leeuwenburgh
 */
public class Locker extends JdbcFacade {
	private static final String LOCK_IGNORED="%null%";

	private String objectId;
	private String type = "T";
	private String dateFormatSuffix;
	private int retention = -1;
	private String insertQuery = "INSERT INTO ibisLock (objectId, type, host, creationDate, expiryDate) VALUES (?, ?, ?, ?, ?)";
	private String deleteQuery = "DELETE FROM ibisLock WHERE objectId=?";
	private SimpleDateFormat formatter;
	private int numRetries = 0;
	private int firstDelay = 10000;
	private int retryDelay = 10000;
	private boolean ignoreTableNotExist = false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(getObjectId())) {
			throw new ConfigurationException(getLogPrefix()+ "an objectId must be specified");
		}
		if (!getType().equalsIgnoreCase("T") && !getType().equalsIgnoreCase("P")) {
			throw new ConfigurationException(getLogPrefix()+"illegal value for type ["+getType()+"], must be 'T' or 'P'");
		}
		if (StringUtils.isNotEmpty(getDateFormatSuffix())) {
			try {
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

	public String lock() throws JdbcException, SQLException, InterruptedException {
		try (Connection conn = getConnection()) {
			if (!getDbmsSupport().isTablePresent(conn, "ibisLock")) {
				if (isIgnoreTableNotExist()) {
					log.info("table [ibisLock] does not exist, ignoring lock");
					return LOCK_IGNORED;
				} else {
					throw new JdbcException("table [ibisLock] does not exist");
				}
			}
		}

		String objectIdWithSuffix = null;
		int r = -1;
		while (objectIdWithSuffix == null && (numRetries == -1 || r < numRetries)) {
			r++;
			if (r == 0 && firstDelay > 0) {
				Thread.sleep(firstDelay);
			}
			if (r > 0) {
				Thread.sleep(retryDelay);
			}
			Date date = new Date();
			objectIdWithSuffix = getObjectId();
			if (StringUtils.isNotEmpty(getDateFormatSuffix())) {
				String formattedDate = formatter.format(date);
				objectIdWithSuffix = objectIdWithSuffix.concat(formattedDate);
			}
			log.debug("preparing to set lock [" + objectIdWithSuffix + "]");
			try (Connection conn = getConnection()) {
				try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
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
				}
			} catch (SQLException e) {
				log.debug(getLogPrefix()+"error executing insert query (as part of locker): " + e.getMessage());
				if (numRetries == -1 || r < numRetries) {
					log.debug(getLogPrefix()+"will try again");
					objectIdWithSuffix = null;
				} else {
					log.debug(getLogPrefix()+"will not try again");
					throw e;
				}
			}
		}
		return objectIdWithSuffix;
	}

	public void unlock(String objectIdWithSuffix) throws JdbcException, SQLException {
		if (LOCK_IGNORED.equals(objectIdWithSuffix)) {
			log.info("lock not set, ignoring unlock");
		} else {
			if (getType().equalsIgnoreCase("T")) {
				log.debug("preparing to remove lock [" + objectIdWithSuffix + "]");

				try (Connection conn = getConnection()) {
					try (PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {
						stmt.clearParameters();
						stmt.setString(1,objectIdWithSuffix);
						stmt.executeUpdate();
						log.debug("lock ["+objectIdWithSuffix+"] removed");
					}
				}
			}
		}
	}

	@Override
	protected String getLogPrefix() {
		return getName()+" "; 
	}

	@IbisDoc({"format for date which is added after <code>objectid</code> (e.g. yyyymmdd to be sure the job is executed only once a day)", ""})
	public void setDateFormatSuffix(String dateFormatSuffix) {
		this.dateFormatSuffix = dateFormatSuffix;
	}

	public String getDateFormatSuffix() {
		return dateFormatSuffix;
	}

	@IbisDoc({"type for this lock: p(ermanent) or t(emporary). a temporary lock is deleted after the job has completed", "t"})
	public void setType(String type) {
		this.type = type;
	}

	public String getType() {
		return type;
	}

	@IbisDoc({"identifier for this lock", ""})
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	public String getObjectId() {
		return objectId;
	}

	@IbisDoc({"the time (for type=p in days and for type=t in hours) to keep the record in the database before making it eligible for deletion by a cleanup process", "30 days (type=p), 4 hours (type=t)"})
	public void setRetention(int retention) {
		this.retention = retention;
	}

	public int getRetention() {
		return retention;
	}

	public int getNumRetries() {
		return numRetries;
	}

	@IbisDoc({"the number of times an attempt should be made to acquire a lock, after this many times an exception is thrown when no lock could be acquired, when -1 the number of retries is unlimited", "0"})
	public void setNumRetries(int numRetries) {
		this.numRetries = numRetries;
	}

	public int getFirstDelay() {
		return firstDelay;
	}

	@IbisDoc({"the time in ms to wait before the first attempt to acquire a lock is made, this may be 0 but keep in mind that the other thread or ibis instance will propably not get much change to acquire a lock when another message is already waiting for the thread having the current lock in which case it will probably acquire a new lock soon after releasing the current lock", "10000"})
	public void setFirstDelay(int firstDelay) {
		this.firstDelay = firstDelay;
	}

	public int getRetryDelay() {
		return retryDelay;
	}

	@IbisDoc({"the time in ms to wait before another attempt to acquire a lock is made", "10000"})
	public void setRetryDelay(int retryDelay) {
		this.retryDelay = retryDelay;
	}

	public void setIgnoreTableNotExist(boolean b) {
		ignoreTableNotExist = b;
	}

	public boolean isIgnoreTableNotExist() {
		return ignoreTableNotExist;
	}
}