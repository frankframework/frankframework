/*
   Copyright 2021-2022 WeAreFrank!

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
package org.frankframework.scheduler.job;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import lombok.Getter;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.IbisManager;
import org.frankframework.core.Adapter;
import org.frankframework.core.IMessageBrowser;
import org.frankframework.core.IPipe;
import org.frankframework.core.ITransactionalStorage;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.dbms.Dbms;
import org.frankframework.jdbc.AbstractJdbcQuerySender;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jdbc.JdbcTransactionalStorage;
import org.frankframework.parameters.DateParameter;
import org.frankframework.parameters.DateParameter.DateFormatType;
import org.frankframework.pipes.MessageSendingPipe;
import org.frankframework.receivers.Receiver;
import org.frankframework.scheduler.AbstractJobDef;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;
import org.frankframework.util.SpringUtils;

/**
 * Frank!Framework job to cleanup the {@code IBISSTORE} and {@code IBISLOCK} tables.
 * Find all MessageLogs and Lockers in the current configuration and removes database
 * entries which have surpassed their corresponding {@link JdbcTransactionalStorage#getExpiryDateField() MessageLog's ExpiryDateField}. 
 * 
 * {@inheritClassDoc}
 * 
 * @ff.info This is a default job that can be controlled with the property {@literal cleanup.database.active} and {@literal cleanup.database.cron}.
 */
public class CleanupDatabaseJob extends AbstractJobDef {
	private @Getter int queryTimeout;

	private static class MessageLogObject {
		private final String datasourceName;
		private final String tableName;
		private final String expiryDateField;
		private final String keyField;
		private final String typeField;

		public MessageLogObject(String datasourceName, String tableName, String expiryDateField, String keyField, String typeField) {
			this.datasourceName = datasourceName;
			this.tableName = tableName;
			this.expiryDateField = expiryDateField;
			this.keyField = keyField;
			this.typeField = typeField;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof MessageLogObject)) return false;

			MessageLogObject mlo = (MessageLogObject) o;
			return mlo.getDatasourceName().equals(datasourceName) &&
					mlo.getTableName().equals(tableName) &&
					mlo.expiryDateField.equals(expiryDateField);
		}

		public String getDatasourceName() {
			return datasourceName;
		}

		public String getTableName() {
			return tableName;
		}

		public String getExpiryDateField() {
			return expiryDateField;
		}

		public String getKeyField() {
			return keyField;
		}

		public String getTypeField() {
			return typeField;
		}
	}

	@Override
	public boolean beforeExecuteJob() {
		Set<String> datasourceNames = getAllLockerDatasourceNames();

		for (String datasourceName : datasourceNames) {
			FixedQuerySender qs = null;
			try(PipeLineSession session = new PipeLineSession()) {
				qs = SpringUtils.createBean(getApplicationContext());
				qs.setDatasourceName(datasourceName);
				qs.setName("cleanupDatabase-IBISLOCK");
				qs.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
				qs.setTimeout(getQueryTimeout());
				qs.setScalar(true);
				qs.setQuery("DELETE FROM IBISLOCK WHERE EXPIRYDATE < ?");
				DateParameter param = new DateParameter();
				param.setName("now");
				param.setValue(DateFormatUtils.now());
				param.setFormatType(DateFormatType.TIMESTAMP);
				qs.addParameter(param);
				qs.configure();
				qs.start();

				int numberOfRowsAffected;
				try (Message result = qs.sendMessageOrThrow(Message.nullMessage(), session)) {
					numberOfRowsAffected = Integer.parseInt(Objects.requireNonNull(result.asString()));
				}
				if (numberOfRowsAffected > 0) {
					getMessageKeeper().add("deleted [" + numberOfRowsAffected + "] row(s) from [IBISLOCK] table. It implies that there have been process(es) that finished unexpectedly or failed to complete. Please investigate the log files!", MessageKeeperLevel.WARN);
				}
			} catch (Exception e) {
				String msg = "error while cleaning IBISLOCK table (as part of scheduled job execution): " + e.getMessage();
				getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
				log.error(msg, e);
			} finally {
				if (qs != null) {
					qs.stop();
				}
			}
		}
		return true;
	}

	@Override
	public void execute() {
		Instant instant = Instant.now();

		int maxRows = AppConstants.getInstance().getInt("cleanup.database.maxrows", 25000);

		List<MessageLogObject> messageLogs = getAllMessageLogs();

		for (MessageLogObject mlo : messageLogs) {
			FixedQuerySender qs = null;
			try {
				qs = SpringUtils.createBean(getApplicationContext());
				qs.setDatasourceName(mlo.getDatasourceName());
				qs.setName("cleanupDatabase-" + mlo.getTableName());
				qs.setQueryType(AbstractJdbcQuerySender.QueryType.OTHER);
				qs.setTimeout(getQueryTimeout());
				qs.setScalar(true);

				DateParameter param = new DateParameter();
				param.setName("now");
				param.setValue(DateFormatUtils.format(instant));
				param.setFormatType(DateFormatType.TIMESTAMP);
				qs.addParameter(param);

				String query = this.getCleanUpIbisstoreQuery(mlo.getTableName(), mlo.getKeyField(), mlo.getTypeField(), mlo.getExpiryDateField(), maxRows, qs.getDbmsSupport().getDbms());
				qs.setQuery(query);
				qs.configure();
				qs.start();

				boolean deletedAllRecords = false;
				while (!deletedAllRecords) {
					int numberOfRowsAffected;
					try(PipeLineSession session = new PipeLineSession();
							Message result = qs.sendMessageOrThrow(Message.nullMessage(), session)) {
						String resultString = result.asString();
						log.info("deleted [{}] rows", resultString);
						if (!NumberUtils.isDigits(resultString)) {
							throw new SenderException("Sent message result did not result in a number, found: " + resultString);
						}
						numberOfRowsAffected = Integer.parseInt(resultString);
					}
					if (maxRows <= 0 || numberOfRowsAffected < maxRows) {
						deletedAllRecords = true;
					} else {
						log.info("executing the query again for job [cleanupDatabase]!");
					}
				}
			} catch (Exception e) {
				String msg = "error while deleting expired records from table [" + mlo.getTableName() + "] (as part of scheduled job execution): " + e.getMessage();
				getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
				log.error(msg, e);
			} finally {
				if (qs != null) {
					qs.stop();
				}
			}
		}
	}

	/**
	 * Locate all Lockers, and find out which datasources are used.
	 *
	 * @return distinct list of all datasourceNames used by lockers
	 */
	protected Set<String> getAllLockerDatasourceNames() {
		Set<String> datasourceNames = new HashSet<>();
		IbisManager ibisManager = getIbisManager();
		for (Configuration configuration : ibisManager.getConfigurations()) {
			if (!configuration.isActive()) {
				continue;
			}
			for (IJob jobdef : configuration.getScheduledJobs()) {
				if (jobdef.getLocker() != null) {
					String datasourceName = jobdef.getLocker().getDatasourceName();
					if (StringUtils.isNotEmpty(datasourceName)) {
						datasourceNames.add(datasourceName);
					}
				}
			}

			for (Adapter adapter : configuration.getRegisteredAdapters()) {
				PipeLine pipeLine = adapter.getPipeLine();
				if (pipeLine != null) {
					for (IPipe pipe : pipeLine.getPipes()) {
						if (pipe.getLocker() != null) {
							String datasourceName = pipe.getLocker().getDatasourceName();
							if (StringUtils.isNotEmpty(datasourceName)) {
								datasourceNames.add(datasourceName);
							}
						}
					}
				}
			}
		}

		return datasourceNames;
	}

	private void collectMessageLogs(List<MessageLogObject> messageLogs, ITransactionalStorage<?> transactionalStorage) {
		if (transactionalStorage instanceof JdbcTransactionalStorage messageLog) {
			String datasourceName = messageLog.getDatasourceName();
			String expiryDateField = messageLog.getExpiryDateField();
			String tableName = messageLog.getTableName();
			String keyField = messageLog.getKeyField();
			String typeField = messageLog.getTypeField();
			MessageLogObject mlo = new MessageLogObject(datasourceName, tableName, expiryDateField, keyField, typeField);
			if (!messageLogs.contains(mlo)) {
				messageLogs.add(mlo);
			}
		}
	}

	protected List<MessageLogObject> getAllMessageLogs() {
		List<MessageLogObject> messageLogs = new ArrayList<>();
		IbisManager ibisManager = getIbisManager();
		for (Configuration configuration : ibisManager.getConfigurations()) {
			if (!configuration.isActive()) {
				continue;
			}
			for (Adapter adapter : configuration.getRegisteredAdapters()) {
				for (Receiver<?> receiver : adapter.getReceivers()) {
					collectMessageLogs(messageLogs, receiver.getMessageLog());
				}
				PipeLine pipeline = adapter.getPipeLine();
				for (int i = 0; i < pipeline.getPipes().size(); i++) {
					IPipe pipe = pipeline.getPipe(i);
					if (pipe instanceof MessageSendingPipe msp) {
						collectMessageLogs(messageLogs, msp.getMessageLog());
					}
				}
			}
		}
		return messageLogs;
	}

	/**
	 * The number of seconds the database driver will wait for a statement to execute. If the limit is exceeded, a TimeoutException is thrown. 0 means no timeout
	 *
	 * @ff.default 0
	 */
	public void setQueryTimeout(int i) {
		queryTimeout = i;
	}

	public String getCleanUpIbisstoreQuery(String tableName, String keyField, String typeField, String expiryDateField, int maxRows, Dbms dbmsName) {
		switch (dbmsName) {
			case MSSQL:
				return "DELETE " + (maxRows > 0 ? "TOP(" + maxRows + ") " : "")
						+ "FROM " + tableName
						+ " WHERE " + typeField + " IN ('" + IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode() + "','" + IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode()
						+ "') AND " + expiryDateField + " < ?";
			case MARIADB:
				return "DELETE FROM " + tableName + " WHERE " + typeField + " IN ('" + IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode() + "','" + IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode()
						+ "') AND " + expiryDateField + "< ?" + (maxRows > 0 ? " LIMIT " + maxRows : "");
			case MYSQL:
				return "DELETE FROM " + tableName + " WHERE " + typeField + " IN ('" + IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode() + "','" + IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode()
						+ "') AND " + expiryDateField + " < ?" + (maxRows > 0 ? " LIMIT " + maxRows : "");
			default:
				if (log.isDebugEnabled()) log.warn("Not sure how to clean up for dialect: {} just trying something", dbmsName);
				return "DELETE FROM " + tableName + " WHERE " + keyField + " IN (SELECT " + keyField + " FROM " + tableName
						+ " WHERE " + typeField + " IN ('" + IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode() + "','" + IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode()
						+ "') AND " + expiryDateField + " < ?" + (maxRows > 0 ? " FETCH FIRST " + maxRows + " ROWS ONLY" : "") + ")";
		}
	}
}
