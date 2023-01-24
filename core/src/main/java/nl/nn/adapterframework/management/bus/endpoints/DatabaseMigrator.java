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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.configuration.classloaders.ClassLoaderBase;
import nl.nn.adapterframework.core.BytesResource;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jdbc.migration.DatabaseMigratorBase;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.StreamUtil;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.JDBC_MIGRATION)
public class DatabaseMigrator extends BusEndpointBase {

	@ActionSelector(BusAction.DOWNLOAD)
	public Message<Object> downloadMigrationScript(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		if(IbisManager.ALL_CONFIGS_KEY.equals(configurationName)) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			try (ZipOutputStream zos = new ZipOutputStream(out)) {
				for(Configuration configuration : getIbisManager().getConfigurations()) {
					Resource resource = getMigrationResource(configuration);
					if(resource != null) {
						try(InputStream file = resource.openStream()) {
							String filename = configuration.getName() +"-"+ normalizeName(resource.getName()); //Names have to be unique in a zip archive!
							ZipEntry entry = new ZipEntry(filename);
							zos.putNextEntry(entry);
							zos.write(StreamUtil.streamToByteArray(file, false));
							zos.closeEntry();
						}
					}
				}
			} catch (IOException e) {
				throw new BusException("unable to create ZIP archive", e);
			}
			return ResponseMessage.Builder.create().withPayload(out.toByteArray()).withMimeType(MediaType.APPLICATION_OCTET_STREAM).withFilename("DatabaseChangelog.zip").raw();
		}

		Configuration configuration = getConfigurationByName(configurationName);
		Resource changelog = getMigrationResource(configuration);
		if(changelog == null) {
			throw new BusException("unable to generate migration script, database migrations are not enabled for this configuration");
		}
		return ResponseMessage.Builder.create().withPayload(changelog).withMimeType(getMediaTypeFromName(changelog.getName())).raw();
	}

	@Nonnull
	private String normalizeName(@Nonnull String name) {
		if (name.startsWith(ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME)) {
			return name.substring(ClassLoaderBase.CLASSPATH_RESOURCE_SCHEME.length());
		}
		return name;
	}

	@Nullable
	private Resource getMigrationResource(Configuration configuration) {
		if(!configuration.isActive()) {
			return null;
		}

		DatabaseMigratorBase databaseMigrator = configuration.getBean("jdbcMigrator", DatabaseMigratorBase.class);
		if(databaseMigrator.hasMigrationScript()) {
			return databaseMigrator.getChangeLog();
		}

		return null;
	}

	@ActionSelector(BusAction.UPLOAD)
	public Message<Object> getMigrationChanges(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		Configuration configuration = getConfigurationByName(configurationName);

		if(!(message.getPayload() instanceof String)) {
			throw new BusException("payload is not instance of String");
		}

		DatabaseMigratorBase databaseMigrator = configuration.getBean("jdbcMigrator", DatabaseMigratorBase.class);
		if(!databaseMigrator.hasMigrationScript()) {
			throw new BusException("unable to generate migration script, database migrations are not enabled for this configuration");
		}
		String filename = BusMessageUtils.getHeader(message, "filename");
		if(StringUtils.isEmpty(filename)) {
			throw new BusException("no filename provided");
		}

		if(filename.startsWith(configurationName)) { //Remove ConfigurationName if present
			filename = filename.substring(configurationName.length()+1);
		}
		String migrationFilename = normalizeName(databaseMigrator.getChangeLog().getName());
		if(!filename.equals(migrationFilename)) { //Compare if the name matches, else liquibase will see this as a new different changelog!
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
		return ResponseMessage.Builder.create().withPayload(writer.toString()).withMimeType(MediaType.TEXT_PLAIN).raw();
	}
}