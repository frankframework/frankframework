/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IListener;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.FixedQuerySender;
import org.frankframework.jdbc.IDataSourceFactory;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.EmptyMessage;
import org.frankframework.receivers.Receiver;
import org.frankframework.scheduler.SchedulerHelper;
import org.frankframework.scheduler.job.DatabaseJob;
import org.frankframework.util.AppConstants;
import org.frankframework.util.Locker;
import org.frankframework.util.SpringUtils;


@BusAware("frank-management-bus")
public class CreateScheduledJob extends BusEndpointBase {

	private SchedulerHelper getSchedulerHelper() {
		return getBean("schedulerHelper", SchedulerHelper.class);
	}

	@TopicSelector(BusTopic.SCHEDULER)
	@ActionSelector(BusAction.UPLOAD)
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> createOrUpdateSchedule(Message<?> message) {
		String jobName = BusMessageUtils.getHeader(message, "job");
		if(StringUtils.isEmpty(jobName)) {
			throw new BusException("no job name specified");
		}
		String groupName = BusMessageUtils.getHeader(message, "group");
		if(StringUtils.isEmpty(groupName)) {
			throw new BusException("no job group specified");
		}
		boolean overwrite = BusMessageUtils.getBooleanHeader(message, "overwrite", false);

		return createOrUpdateSchedule(jobName, groupName, message, overwrite);
	}

	private Message<String> createOrUpdateSchedule(String name, String jobGroup, Message<?> message, boolean overwrite) {
		String cronExpression = BusMessageUtils.getHeader(message, "cron", null);
		int interval = BusMessageUtils.getIntHeader(message, "interval", -1);
		//Either one of the two has to be set
		if(interval == -1 && StringUtils.isEmpty(cronExpression)) {
			throw new BusException("Either 'cron' or 'interval' has to be set");
		}

		String configurationName = BusMessageUtils.getHeader(message, "configuration", null);
		String adapterName = BusMessageUtils.getHeader(message, "adapter");

		Adapter adapter = getAdapter(configurationName, adapterName);
		//Make sure the adapter exists!
		if(adapter == null) {
			throw new BusException("Adapter ["+adapterName+"] not found");
		}

		String listenerName = BusMessageUtils.getHeader(message, "listener");
		String receiverName = BusMessageUtils.getHeader(message, "receiver");
		//Make sure the receiver exists!
		if(StringUtils.isEmpty(listenerName) && StringUtils.isNotEmpty(receiverName)) {
			Receiver<?> receiver = adapter.getReceiverByName(receiverName);
			IListener<?> listener = receiver.getListener();
			if(listener != null) {
				listenerName = listener.getName();
			}
		}
		if(StringUtils.isEmpty(listenerName)) {
			throw new BusException("no listener specified");
		}

		Configuration applicationContext = adapter.getConfiguration();

		boolean hasLocker = BusMessageUtils.getBooleanHeader(message, "locker", false);
		String lockKey = BusMessageUtils.getHeader(message, "lockkey", "lock4["+name+"]");
		String jobMessage = BusMessageUtils.getHeader(message, "message");
		String description = BusMessageUtils.getHeader(message, "description");

		SchedulerHelper sh = getSchedulerHelper();

		//First try to create the schedule and run it on the local ibis before storing it in the database
		DatabaseJob jobdef = SpringUtils.createBean(applicationContext);
		jobdef.setCronExpression(cronExpression);
		jobdef.setName(name);
		jobdef.setAdapterName(adapterName);
		jobdef.setJavaListener(listenerName);
		jobdef.setJobGroup(jobGroup);
		jobdef.setMessage(jobMessage);
		jobdef.setDescription(description);
		jobdef.setInterval(interval);

		if(hasLocker) {
			Locker locker = SpringUtils.createBean(applicationContext);
			locker.setName(lockKey);
			locker.setObjectId(lockKey);
			locker.setDatasourceName(IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
			jobdef.setLocker(locker);
		}

		try {
			jobdef.configure();
			sh.scheduleJob(jobdef);
		} catch (Exception e) {
			throw new BusException("Failed to add schedule", e);
		}

		//Save the job in the database
		if(AppConstants.getInstance().getBoolean("loadDatabaseSchedules.active", false)) {
			boolean success = false;
			FixedQuerySender qs = createBean(FixedQuerySender.class);
			qs.setDatasourceName(IDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME);
			qs.setQuery("SELECT COUNT(*) FROM IBISSCHEDULES");
			try {
				qs.configure();
			} catch (ConfigurationException e) {
				throw new BusException("Error creating FixedQuerySender bean to store job in database", e);
			}

			try {
				qs.start();
				try (Connection conn = qs.getConnection()) {

					if(overwrite) {
						String deleteQuery = "DELETE FROM IBISSCHEDULES WHERE JOBNAME=? AND JOBGROUP=?";
						try (PreparedStatement deleteStatement = conn.prepareStatement(deleteQuery)) {
							deleteStatement.setString(1, name);
							deleteStatement.setString(2, jobGroup);
							deleteStatement.executeUpdate();
						}
					}

					String insertQuery = """
							INSERT INTO IBISSCHEDULES (JOBNAME, JOBGROUP, ADAPTER, RECEIVER, CRON, EXECUTIONINTERVAL, MESSAGE, DESCRIPTION, LOCKER, LOCK_KEY, CREATED_ON, BY_USER) \
							VALUES (?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,?)\
							""";
					try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
						stmt.setString(1, name);
						stmt.setString(2, jobGroup);
						stmt.setString(3, adapterName);
						stmt.setString(4, listenerName);
						stmt.setString(5, cronExpression);
						stmt.setInt(6, interval);
						stmt.setClob(7, new StringReader(jobMessage));
						stmt.setString(8, description);
						stmt.setBoolean(9, hasLocker);
						stmt.setString(10, lockKey);
						stmt.setString(11, BusMessageUtils.getUserPrincipalName());

						success = stmt.executeUpdate() > 0;
					}
				}
			} catch (LifecycleException | SQLException | JdbcException e) {
				throw new BusException("error saving job in database", e);
			} finally {
				qs.stop();
			}

			if(!success)
				throw new BusException("An error occurred while storing the job in the database");
		}

		return EmptyMessage.created();
	}

	private Adapter getAdapter(String configurationName, String adapterName) {
		Configuration configuration = null;
		if(StringUtils.isNotEmpty(configurationName)) {
			configuration = getIbisManager().getConfiguration(configurationName);
			if(configuration == null) {
				throw new BusException("configuration ["+configurationName+"] does not exists");
			}
		}
		if(configuration != null) {
			return configuration.getRegisteredAdapter(adapterName);
		}
		return findAdapter(adapterName);
	}

	private Adapter findAdapter(String adapterName) {
		for(Configuration config : getIbisManager().getConfigurations()) {
			if(config.isActive()) {
				Adapter adapter = config.getRegisteredAdapter(adapterName);
				if (adapterName.equals(adapter.getName())) {
					return adapter;
				}
			}
		}
		return null;
	}
}
