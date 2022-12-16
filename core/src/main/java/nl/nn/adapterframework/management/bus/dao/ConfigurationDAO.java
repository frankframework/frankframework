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
package nl.nn.adapterframework.management.bus.dao;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Getter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.jdbc.migration.DatabaseMigratorBase;
import nl.nn.adapterframework.lifecycle.ConfigurableLifecycle.BootState;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.NameComparatorBase;

@JsonInclude(Include.NON_NULL)
public class ConfigurationDAO {
	private static final String DEFAULT_FF_CONFIGURATION_PREFIX = "IAF_";

	private final @Getter String name;
	private final @Getter String version;
	private @Getter Boolean stubbed;
	private @Getter Boolean loaded = null;
	private @Getter BootState state;
	private @Getter String type;
	private @Getter String exception = null;

	private @Getter String filename;
	private @Getter String created;
	private @Getter String user;
	private @Getter Boolean active = null;
	private @Getter Boolean autoreload = null;
	private @Getter boolean jdbcMigrator = false;

	private @Getter String parent;

	public ConfigurationDAO(String name, String version) {
		this.name = name;
		this.version = version;
	}

	public ConfigurationDAO(Configuration configuration) {
		name = configuration.getName();
		version = configuration.getVersion();
		stubbed = configuration.isStubbed();
		state = configuration.getState();
		type = configuration.getClassLoaderType();
		if(configuration.getConfigurationException() != null) {
			exception = configuration.getConfigurationException().getMessage();
		}

		ClassLoader classLoader = configuration.getClassLoader();
		if(classLoader instanceof DatabaseClassLoader) {
			setDatabaseAttributes((DatabaseClassLoader) classLoader);
		}

		DatabaseMigratorBase databaseMigrator = configuration.getBean("jdbcMigrator", DatabaseMigratorBase.class);
		if(databaseMigrator.hasMigrationScript()) {
			jdbcMigrator = true;
		}

		String parentConfig = AppConstants.getInstance().getString("configurations." + configuration.getName() + ".parentConfig", null);
		if(parentConfig != null) {
			parent = parentConfig;
		}
	}

	private void setDatabaseAttributes(DatabaseClassLoader classLoader) {
		this.filename = classLoader.getFileName();
		this.created = classLoader.getCreationDate();
		this.user = classLoader.getUser();
	}

	public void setDatabaseAttributes(String filename, Date creationDate, String user, Boolean active, Boolean autoReload) {
		this.filename = filename;
		this.created = DateUtils.format(creationDate, DateUtils.FORMAT_GENERICDATETIME);
		this.user = user;
		this.active = active;
		this.autoreload = autoReload;
	}

	public void setLoaded(boolean loaded) {
		this.loaded = loaded;
	}

	public static class VersionComparator extends NameComparatorBase<ConfigurationDAO> {
		@Override
		public int compare(ConfigurationDAO lhs, ConfigurationDAO rhs) {
			String version1 = lhs.getVersion();
			String version2 = rhs.getVersion();

			return -compareNames(version1, version2); //invert the results as we want the latest version first
		}
	}

	public static class NameComparator extends NameComparatorBase<ConfigurationDAO> {
		@Override
		public int compare(ConfigurationDAO lhs, ConfigurationDAO rhs) {
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
