/*
   Copyright 2022-2025 WeAreFrank!

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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.core.BytesResource;
import org.frankframework.core.Resource;
import org.frankframework.dbms.JdbcException;
import org.frankframework.jdbc.migration.AbstractDatabaseMigrator;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.BinaryMessage;
import org.frankframework.management.bus.message.StringMessage;
import org.frankframework.util.StreamUtil;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.JDBC_MIGRATION)
public class DatabaseMigrator extends BusEndpointBase {

	@ActionSelector(BusAction.DOWNLOAD)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public BinaryMessage downloadMigrationScript(Message<?> message) throws IOException {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, BusMessageUtils.ALL_CONFIGS_KEY);
		if(BusMessageUtils.ALL_CONFIGS_KEY.equals(configurationName)) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try (ZipOutputStream zos = new ZipOutputStream(out)) {
				for(Configuration configuration : getIbisManager().getConfigurations()) {
					Resource resource = getMigrationResource(configuration);
					if(resource != null) {
						try(InputStream file = resource.openStream()) {
							String filename = configuration.getName() +"-"+ normalizeName(resource.getName()); // Names have to be unique in a zip archive!
							ZipEntry entry = new ZipEntry(filename);
							zos.putNextEntry(entry);
							zos.write(file.readAllBytes());
							zos.closeEntry();
						}
					}
				}
			} catch (IOException e) {
				throw new BusException("unable to create ZIP archive", e);
			}
			BinaryMessage response = new BinaryMessage(out.toByteArray());
			response.setFilename("DatabaseChangelog.zip");
			return response;
		}

		Configuration configuration = getConfigurationByName(configurationName);
		Resource changelog = getMigrationResource(configuration);
		if(changelog == null) {
			throw new BusException("unable to generate migration script, database migrations are not enabled for this configuration");
		}

		return new BinaryMessage(changelog.openStream(), getMediaTypeFromName(changelog.getName()));
	}

	@Nonnull
	private String normalizeName(@Nonnull String name) {
		if (name.startsWith(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME)) {
			return name.substring(IConfigurationClassLoader.CLASSPATH_RESOURCE_SCHEME.length());
		}
		return name;
	}

	@Nullable
	private Resource getMigrationResource(Configuration configuration) {
		if(!configuration.isActive()) {
			return null;
		}

		AbstractDatabaseMigrator databaseMigrator = configuration.getBean("jdbcMigrator", AbstractDatabaseMigrator.class);
		if(databaseMigrator.hasMigrationScript()) {
			return databaseMigrator.getChangeLog();
		}

		return null;
	}

	@ActionSelector(BusAction.UPLOAD)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public StringMessage getMigrationChanges(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		Configuration configuration = getConfigurationByName(configurationName);

		if(!(message.getPayload() instanceof String)) {
			throw new BusException("payload is not instance of String");
		}

		AbstractDatabaseMigrator databaseMigrator = configuration.getBean("jdbcMigrator", AbstractDatabaseMigrator.class);
		if(!databaseMigrator.hasMigrationScript()) {
			throw new BusException("unable to generate migration script, database migrations are not enabled for this configuration");
		}
		String filename = BusMessageUtils.getHeader(message, "filename");
		if(StringUtils.isEmpty(filename)) {
			throw new BusException("no filename provided");
		}

		if(filename.startsWith(configurationName)) { // Remove ConfigurationName if present
			filename = filename.substring(configurationName.length()+1);
		}
		String migrationFilename = normalizeName(databaseMigrator.getChangeLog().getName());
		if(!filename.equals(migrationFilename)) { // Compare if the name matches, else Liquibase will see this as a new different changelog!
			throw new BusException("provided changelog filename mismatch");
		}

		String payload = (String) message.getPayload();

		StringWriter writer = new StringWriter();
		try {
			Resource resource = new BytesResource(payload.getBytes(StreamUtil.DEFAULT_CHARSET), filename, configuration);
			databaseMigrator.update(writer, resource);
		} catch (JdbcException e) {
			throw new BusException("unable to generate database changes", e);
		}
		return new StringMessage(writer.toString());
	}
}
