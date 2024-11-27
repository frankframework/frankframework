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
package org.frankframework.management.bus.dto;

import java.time.Instant;

import org.apache.commons.io.FilenameUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;

import org.frankframework.configuration.Configuration;
import org.frankframework.configuration.classloaders.DatabaseClassLoader;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.jdbc.migration.AbstractDatabaseMigrator;
import org.frankframework.util.AbstractNameComparator;
import org.frankframework.util.AppConstants;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.RunState;

@JsonInclude(Include.NON_NULL)
public class ConfigurationDTO {
	private static final String DEFAULT_FF_CONFIGURATION_PREFIX = "IAF_";

	private final @Getter String name;
	private final @Getter String version;
	private @Getter Boolean stubbed;
	private @Getter Boolean loaded = null;
	private @Getter RunState state;
	private @Getter String type;
	private @Getter String exception = null;

	private @Getter String directory;
	private @Getter String filename;
	private @Getter String created;
	private @Getter String user;
	private @Getter Boolean active = null;
	private @Getter Boolean autoreload = null;
	private @Getter boolean jdbcMigrator = false;

	private @Getter String parent;

	public ConfigurationDTO(String name, String version) {
		this.name = name;
		this.version = version;
	}

	public ConfigurationDTO(Configuration configuration) {
		name = configuration.getName();
		version = configuration.getVersion();
		stubbed = configuration.isStubbed();
		state = configuration.getState();
		type = configuration.getClassLoaderType();
		if(configuration.getConfigurationException() != null) {
			exception = configuration.getConfigurationException().getMessage();
		}

		ClassLoader classLoader = configuration.getClassLoader();
		if(classLoader instanceof DatabaseClassLoader loader) {
			setDatabaseAttributes(loader);
		} else if(classLoader instanceof DirectoryClassLoader loader) {
			setDirectoryAttributes(loader);
		}

		if(configuration.isActive()) {
			AbstractDatabaseMigrator databaseMigrator = configuration.getBean("jdbcMigrator", AbstractDatabaseMigrator.class);
			if(databaseMigrator.hasMigrationScript()) {
				jdbcMigrator = true;
			}
		}

		String parentConfig = AppConstants.getInstance().getString("configurations." + configuration.getName() + ".parentConfig", null);
		if(parentConfig != null) {
			parent = parentConfig;
		}
	}

	private void setDirectoryAttributes(DirectoryClassLoader classLoader) {
		String directory = classLoader.getDirectory().getAbsolutePath();
		this.directory = FilenameUtils.normalize(directory, true);
	}

	private void setDatabaseAttributes(DatabaseClassLoader classLoader) {
		this.filename = classLoader.getFileName();
		this.created = classLoader.getCreationDate();
		this.user = classLoader.getUser();
	}

	public void setDatabaseAttributes(String filename, Instant creationDate, String user, Boolean active, Boolean autoreload) {
		this.filename = filename;
		this.created = DateFormatUtils.format(creationDate, DateFormatUtils.GENERIC_DATETIME_FORMATTER);
		this.user = user;
		this.active = active;
		this.autoreload = autoreload;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public static class VersionComparator extends AbstractNameComparator<ConfigurationDTO> {
		@Override
		public int compare(ConfigurationDTO lhs, ConfigurationDTO rhs) {
			String version1 = lhs.getVersion();
			String version2 = rhs.getVersion();

			return -compareNames(version1, version2); //invert the results as we want the latest version first
		}
	}

	public static class NameComparator extends AbstractNameComparator<ConfigurationDTO> {
		@Override
		public int compare(ConfigurationDTO lhs, ConfigurationDTO rhs) {
			String name1 = lhs.getName();
			String name2 = rhs.getName();
			if(name1 == null || name2 == null) return 0;

			if(name1.startsWith(DEFAULT_FF_CONFIGURATION_PREFIX)) {
				return -1;
			}
			return name2.startsWith(DEFAULT_FF_CONFIGURATION_PREFIX) ? 1 : compareNames(name1, name2);
		}
	}
}
