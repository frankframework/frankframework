/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.management.bus.endpoints;

import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.jdbc.migration.DatabaseMigratorBase;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.webcontrol.api.FrankApiBase;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.JDBC_MIGRATION)
public class DatabaseMigrator extends BusEndpointBase {

	@ActionSelector(BusAction.DOWNLOAD)
	public Message<Object> downloadMigrationScript(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, FrankApiBase.HEADER_CONFIGURATION_NAME_KEY);
		Configuration configuration = getConfigurationByName(configurationName);

		DatabaseMigratorBase databaseMigrator = configuration.getBean("jdbcMigrator", DatabaseMigratorBase.class);
		if(!databaseMigrator.hasMigrationScript()) {
			throw new BusException("unable to generate migration script, database migrations are not enabled for this configuration");
		}

		Resource changelog = databaseMigrator.getChangeLog();
		return ResponseMessage.Builder.create().withPayload(changelog).withMimeType(MediaType.TEXT_PLAIN).raw();
	}
}