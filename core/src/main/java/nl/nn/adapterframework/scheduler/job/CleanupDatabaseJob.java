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
package nl.nn.adapterframework.scheduler.job;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import nl.nn.adapterframework.core.IMessageBrowser;

import nl.nn.adapterframework.dbms.Dbms;
import nl.nn.adapterframework.dbms.MySqlDbmsSupport;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jdbc.JdbcTransactionalStorage;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.scheduler.JobDef;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.SpringUtils;

public class CleanupDatabaseJob extends JobDef {
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
			try {
				qs = SpringUtils.createBean(getApplicationContext(), FixedQuerySender.class);
				qs.setDatasourceName(datasourceName);
				qs.setName("cleanupDatabase-IBISLOCK");
				qs.setQueryType("other");
				qs.setTimeout(getQueryTimeout());
				qs.setScalar(true);
				String query = "DELETE FROM IBISLOCK WHERE EXPIRYDATE < ?";
				qs.setQuery(query);
				Parameter param = new Parameter("now", DateUtils.format(new Date()));
				param.setType(ParameterType.TIMESTAMP);
				qs.addParameter(param);
				qs.configure();
				qs.open();

				int numberOfRowsAffected;
				try (Message result = qs.sendMessageOrThrow(Message.nullMessage(), null)) {
					numberOfRowsAffected = Integer.parseInt(Objects.requireNonNull(result.asString()));
				}
				if (numberOfRowsAffected > 0) {
					getMessageKeeper().add("deleted [" + numberOfRowsAffected + "] row(s) from [IBISLOCK] table. It implies that there have been process(es) that finished unexpectedly or failed to complete. Please investigate the log files!", MessageKeeperLevel.WARN);
				}
			} catch (Exception e) {
				String msg = "error while cleaning IBISLOCK table (as part of scheduled job execution): " + e.getMessage();
				getMessageKeeper().add(msg, MessageKeeperLevel.ERROR);
				log.error(getLogPrefix() + msg, e);
			} finally {
				if (qs != null) {
					qs.close();
				}
			}
		}
		return true;
	}

	@Override
	public void execute() {
		Date date = new Date();

		int maxRows = AppConstants.getInstance().getInt("cleanup.database.maxrows", 25000);

		List<MessageLogObject> messageLogs = getAllMessageLogs();

		for (MessageLogObject mlo : messageLogs) {
			FixedQuerySender qs = null;
			try {
				qs = SpringUtils.createBean(getApplicationContext(), FixedQuerySender.class);
				qs.setDatasourceName(mlo.getDatasourceName());
				qs.setName("cleanupDatabase-" + mlo.getTableName());
				qs.setQueryType("other");
				qs.setTimeout(getQueryTimeout());
				qs.setScalar(true);

				Parameter param = new Parameter("now", DateUtils.format(date));
				param.setType(ParameterType.TIMESTAMP);
				qs.addParameter(param);

				String query = this.getCleanUpIbisstoreQuery(mlo.getTableName(), mlo.getKeyField(), mlo.getTypeField(), mlo.getExpiryDateField(), maxRows, qs.getDbmsSupport().getDbmsName());
				qs.setQuery(query);
				qs.configure();
				qs.open();

				boolean deletedAllRecords = false;
				while (!deletedAllRecords) {
					int numberOfRowsAffected;
					try (Message result = qs.sendMessageOrThrow(Message.nullMessage(), null)) {
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
				log.error("{} {}", getLogPrefix(), msg);
			} finally {
				if (qs != null) {
					qs.close();
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

			for (IAdapter adapter : configuration.getRegisteredAdapters()) {
				PipeLine pipeLine = adapter.getPipeLine();
				if (pipeLine != null) {
					for (IPipe pipe : pipeLine.getPipes()) {
						if (pipe instanceof IExtendedPipe) {
							IExtendedPipe extendedPipe = (IExtendedPipe) pipe;
							if (extendedPipe.getLocker() != null) {
								String datasourceName = extendedPipe.getLocker().getDatasourceName();
								if (StringUtils.isNotEmpty(datasourceName)) {
									datasourceNames.add(datasourceName);
								}
							}
						}
					}
				}
			}
		}

		return datasourceNames;
	}

	private void collectMessageLogs(List<MessageLogObject> messageLogs, ITransactionalStorage<?> transactionalStorage) {
		if (transactionalStorage instanceof JdbcTransactionalStorage) {
			JdbcTransactionalStorage<?> messageLog = (JdbcTransactionalStorage<?>) transactionalStorage;
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
			for (IAdapter adapter : configuration.getRegisteredAdapters()) {
				for (Receiver<?> receiver : adapter.getReceivers()) {
					collectMessageLogs(messageLogs, receiver.getMessageLog());
				}
				PipeLine pipeline = adapter.getPipeLine();
				for (int i = 0; i < pipeline.getPipes().size(); i++) {
					IPipe pipe = pipeline.getPipe(i);
					if (pipe instanceof MessageSendingPipe) {
						MessageSendingPipe msp = (MessageSendingPipe) pipe;
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

	public String getCleanUpIbisstoreQuery(String tableName, String keyField, String typeField, String expiryDateField, int maxRows, String dbmsName) {
		if (dbmsName.equalsIgnoreCase(Dbms.MSSQL.getKey())) {
			return "DELETE " + (maxRows > 0 ? "TOP(" + maxRows + ") " : "")
					+ "FROM " + tableName
					+ " WHERE " + typeField + " IN ('" + IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode() + "','" + IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode()
					+ "') AND " + expiryDateField + " < ?";
		} else if (dbmsName.equalsIgnoreCase(Dbms.MARIADB.getKey())) {
			return "DELETE FROM " + tableName + " WHERE " + typeField + " IN ('" + IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode() + "','" + IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode()
					+ "') AND " + expiryDateField + "< ?" + (maxRows > 0 ? " LIMIT " + maxRows : "");
		} else if (dbmsName.equalsIgnoreCase(Dbms.MYSQL.getKey())) {
			return "DELETE FROM " + tableName + " WHERE " + typeField + " IN ('" + IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode() + "','" + IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode()
					+ "') AND " + expiryDateField + " < ?" + (maxRows > 0 ? " LIMIT " + maxRows : "");
		} else {
			return ("DELETE FROM " + tableName + " WHERE " + keyField + " IN (SELECT " + keyField + " FROM " + tableName
					+ " WHERE " + typeField + " IN ('" + IMessageBrowser.StorageType.MESSAGELOG_PIPE.getCode() + "','" + IMessageBrowser.StorageType.MESSAGELOG_RECEIVER.getCode()
					+ "') AND " + expiryDateField + " < ?" + (maxRows > 0 ? " FETCH FIRST " + maxRows + " ROWS ONLY" : "") + ")");

		}
	}
}
