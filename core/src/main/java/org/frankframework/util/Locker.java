/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
package org.frankframework.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.HasTransactionAttribute;
import org.frankframework.core.IbisTransaction;
import org.frankframework.core.TransactionAttribute;
import org.frankframework.core.TransactionAttributes;
import org.frankframework.dbms.JdbcException;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.doc.Mandatory;
import org.frankframework.jdbc.JdbcFacade;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;

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
@FrankDocGroup(FrankDocGroupValue.OTHER)
public class Locker extends JdbcFacade implements HasTransactionAttribute {
	private static final String LOCK_IGNORED="%null%";
	private static final String LOCK_OBJECT_QUERY = "INSERT INTO IBISLOCK (objectId, type, host, creationDate, expiryDate) VALUES (?, ?, ?, ?, ?)";
	private static final String UNLOCK_OBJECT_QUERY = "DELETE FROM IBISLOCK WHERE objectId=?";
	private static final String CHECK_OBJECT_LOCK_QUERY = "SELECT type, host, creationDate, expiryDate FROM IBISLOCK WHERE objectId=?";

	private @Getter String objectId;
	private @Getter LockType type = LockType.T;
	private @Getter String dateFormatSuffix;
	private @Getter int retention = -1;
	private DateTimeFormatter formatter;
	private @Getter int numRetries = 0;
	private @Getter int firstDelay = 0;
	private @Getter int retryDelay = 10000;
	private @Getter boolean ignoreTableNotExist = false;

	private @Getter @Setter TransactionAttribute transactionAttribute=TransactionAttribute.SUPPORTS;
	private @Getter @Setter int transactionTimeout = 0;
	private @Getter int lockWaitTimeout = 0;

	private @Getter @Setter PlatformTransactionManager txManager;
	private @Getter TransactionDefinition txDef = null;

	public enum LockType {
		/** Temporary */
		T,
		/** Permanent */
		P
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		txDef = TransactionAttributes.configureTransactionAttributes(log, getTransactionAttribute(), getTransactionTimeout());
		if (StringUtils.isEmpty(getObjectId())) {
			throw new ConfigurationException(getLogPrefix()+ "an objectId must be specified");
		}
		if (StringUtils.isNotEmpty(getDateFormatSuffix())) {
			try {
				formatter = DateTimeFormatter.ofPattern(getDateFormatSuffix());
			} catch (IllegalArgumentException ex){
				throw new ConfigurationException(getLogPrefix()+"has an illegal value for dateFormat", ex);
			}
		}
		if (retention<0) {
			if (getType()==LockType.T) {
				retention = 4;
			} else {
				retention = 30;
			}
		}
	}

	public String acquire() throws JdbcException, SQLException, InterruptedException {
		return acquire(null);
	}

	/**
	 * Obtain the lock. If successful, the non-null lockId is returned.
	 * If the lock cannot be obtained within the lock-timeout because it is held by another party, null is returned.
	 * The lock wait timeout of the database can be overridden by setting lockWaitTimeout.
	 * A wait timeout beyond the basic lockWaitTimeout and transactionTimeout can be set using numRetries in combination with retryDelay.
	 *
	 */
	public String acquire(MessageKeeper messageKeeper) throws JdbcException, SQLException, InterruptedException {

		try (Connection conn = getConnection()) {
			if (!getDbmsSupport().isTablePresent(conn, "IBISLOCK")) {
				if (isIgnoreTableNotExist()) {
					log.info("table [IBISLOCK] does not exist, ignoring lock");
					return LOCK_IGNORED;
				}
				throw new JdbcException("table [IBISLOCK] does not exist");
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
			IbisTransaction itx = new IbisTransaction(getTxManager(), getTxDef(), "locker [" + getName() + "]");
			try {
				Instant instant = TimeProvider.now();

				objectIdWithSuffix = getObjectId();
				if (StringUtils.isNotEmpty(getDateFormatSuffix())) {
					String formattedDate = formatter.format(instant);
					objectIdWithSuffix = objectIdWithSuffix.concat(formattedDate);
				}

				boolean timeout = false;
				log.debug("preparing to set lock [{}]", objectIdWithSuffix);
				try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(LOCK_OBJECT_QUERY)) {
					stmt.clearParameters();
					stmt.setString(1,objectIdWithSuffix);
					stmt.setString(2,getType().name());
					stmt.setString(3,Misc.getHostname());
					stmt.setTimestamp(4, Timestamp.from(instant));

					if (getType() == LockType.T) {
						instant.plus(getRetention(), ChronoUnit.HOURS);
					} else {
						instant.plus(getRetention(), ChronoUnit.DAYS);
					}

					stmt.setTimestamp(5, Timestamp.from(instant));
					TimeoutGuard timeoutGuard = null;
					if (lockWaitTimeout > 0) {
						timeoutGuard = new TimeoutGuard(lockWaitTimeout, "lockWaitTimeout") {

							@Override
							protected void abort() {
								try {
									stmt.cancel();
								} catch (SQLException e) {
									log.warn("Could not cancel statement",e);
								}
							}
						};
					}
					try {
						log.debug("lock [{}] inserting...", objectIdWithSuffix);
						stmt.executeUpdate();
						log.debug("lock [{}] inserted executed", objectIdWithSuffix);
					} finally {
						if (timeoutGuard!=null && timeoutGuard.cancel()) {
							log.warn("Timeout obtaining lock [{}]", objectId);
							itx.setRollbackOnly();
							timeout=true;
							return null;
						}
					}
				} catch (Exception e) {
					itx.setRollbackOnly();
					log.debug("{}error executing insert query (as part of locker): ", getLogPrefix(), e);
					if (numRetries == -1 || r < numRetries) {
						log.debug("{}will try again", getLogPrefix());
						objectIdWithSuffix = null;
					} else {
						log.debug("{}will not try again", getLogPrefix());

						if (timeout || e instanceof SQLTimeoutException || e instanceof SQLException exception && getDbmsSupport().isConstraintViolation(exception)) {
							String msg = "could not obtain lock "+getLockerInfo(objectIdWithSuffix)+" ("+e.getClass().getTypeName()+"): " + e.getMessage();
							if(messageKeeper != null) {
								messageKeeper.add(msg, MessageKeeperLevel.INFO);
							}
							log.info("{}{}", getLogPrefix(), msg);
							return null;
						} else {
							throw e;
						}
					}
				}
			} finally {
				itx.complete();
			}
		}
		return objectIdWithSuffix;
	}

	public void release(String objectIdWithSuffix) throws JdbcException, SQLException {
		if (LOCK_IGNORED.equals(objectIdWithSuffix)) {
			log.info("lock not set, ignoring unlock");
		} else {
			if (getType()==LockType.T) {
				log.debug("preparing to remove lock [{}]", objectIdWithSuffix);
				IbisTransaction itx = new IbisTransaction(getTxManager(), getTxDef(), "locker [" + getName() + "]");

				try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(UNLOCK_OBJECT_QUERY)) {
					stmt.clearParameters();
					stmt.setString(1,objectIdWithSuffix);
					stmt.executeUpdate();
					log.debug("lock [{}] removed", objectIdWithSuffix);
				} catch(JdbcException | SQLException e) {
					itx.setRollbackOnly();
					throw e;
				} finally {
					itx.complete();
				}
			}
		}
	}

	public String getLockerInfo(String objectIdWithSuffix) {
		try {
			String query = getDbmsSupport().prepareQueryTextForNonLockingRead(CHECK_OBJECT_LOCK_QUERY);
			try (Connection conn = getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
				stmt.clearParameters();
				stmt.setString(1,objectIdWithSuffix);

				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return "objectId ["+objectId+"] of type ["+rs.getString(1)+"]. Process locked by host ["+rs.getString(2)+"] at ["+ DateFormatUtils.format(rs.getTimestamp(3))+"] with expiry date ["+ DateFormatUtils.format(rs.getTimestamp(4))+"]";
					}
					return "(no locker info found)";
				}
			}
		} catch (Exception e) {
			return "(cannot get locker info: (" + ClassUtils.nameOf(e) + ") " + e.getMessage() + ")";
		}
	}

	@Override
	protected String getLogPrefix() {
		return getName()+" ";
	}

	@Override
	public String toString() {
		return getLogPrefix()+" type ["+getType()+"] objectId ["+getObjectId()+"] transactionAttribute ["+getTransactionAttribute()+"]";
	}


	/** Identifier for this lock */
	@Mandatory
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}

	/**
	 * Type for this lock: P(ermanent) or T(emporary). A temporary lock is released after the job has completed
	 * @ff.default T
	 */
	public void setType(LockType type) {
		this.type = type;
	}

	/** Format for date which is added after <code>objectid</code> (e.g. yyyyMMdd to be sure the job is executed only once a day) */
	public void setDateFormatSuffix(String dateFormatSuffix) {
		this.dateFormatSuffix = dateFormatSuffix;
	}

	/**
	 * The time (for type=P in days and for type=T in hours) to keep the record in the database before making it eligible for deletion by a cleanup process
	 * @ff.default 30 days (type=P), 4 hours (type=T)
	 */
	public void setRetention(int retention) {
		this.retention = retention;
	}

	/**
	 * The number of times an attempt should be made to acquire a lock, after this many times an exception is thrown when no lock could be acquired, when -1 the number of retries is unlimited
	 * @ff.default 0
	 */
	public void setNumRetries(int numRetries) {
		this.numRetries = numRetries;
	}

	/**
	 * The time in ms to wait before the first attempt to acquire a lock is made
	 * @ff.default 0
	 */
	public void setFirstDelay(int firstDelay) {
		this.firstDelay = firstDelay;
	}

	/**
	 * The time in ms to wait before another attempt to acquire a lock is made
	 * @ff.default 10000
	 */
	public void setRetryDelay(int retryDelay) {
		this.retryDelay = retryDelay;
	}

	/**
	 * If > 0: The time in s to wait before the INSERT statement to obtain the lock is canceled. N.B. On Oracle hitting this lockWaitTimeout may cause the error: (SQLRecoverableException) SQLState [08003], errorCode [17008] connection closed
	 * @ff.default 0
	 */
	public void setLockWaitTimeout(int i) {
		lockWaitTimeout = i;
	}

	/** If set <code>true</code> and the IBISLOCK table does not exist in the database, the process continues as if the lock was obtained */
	public void setIgnoreTableNotExist(boolean b) {
		ignoreTableNotExist = b;
	}

}
