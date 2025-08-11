/*
   Copyright 2020-2025 WeAreFrank!

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
package org.frankframework.configuration.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StreamUtil;

/**
 * Validates if the BuildInfo.properties file is present in the configuration (jar), and if the name and version properties are set correctly
 *
 * @author Niels Meijer
 */
@Log4j2
public class BuildInfoValidator {
	private ConfigurationInfo configInfo = null;
	private byte[] jar = null;
	private String buildInfoFilename = null;
	protected static String ADDITIONAL_PROPERTIES_FILE_SUFFIX = AppConstants.getInstance().getString(AppConstants.ADDITIONAL_PROPERTIES_FILE_SUFFIX_KEY, null);

	public BuildInfoValidator(InputStream stream) throws ConfigurationException {
		String buildInfo = "BuildInfo";
		if(StringUtils.isNotEmpty(ADDITIONAL_PROPERTIES_FILE_SUFFIX)) {
			buildInfo += ADDITIONAL_PROPERTIES_FILE_SUFFIX;
		}
		this.buildInfoFilename = buildInfo + ".properties";

		try {
			jar = StreamUtil.streamToBytes(stream); // Persist Stream so it can be read multiple times.

			getConfigurationInfo();

			validate();
		} catch(IOException e) {
			throw new ConfigurationException("unable to read jarfile", e);
		}
	}

	private void getConfigurationInfo() throws IOException, ConfigurationException {
		try (JarInputStream jarInputStream = new JarInputStream(getJar())) {
			configInfo = ConfigurationInfo.fromManifest(jarInputStream.getManifest());
			if (configInfo == null) {
				configInfo = searchForBuildInfo(jarInputStream);
			}
		}

		if (configInfo == null) {
			throw new ConfigurationException("no [%s] or [%s] present in configuration".formatted(JarFile.MANIFEST_NAME, buildInfoFilename));
		}
	}

	@Nullable
	private ConfigurationInfo searchForBuildInfo(JarInputStream zipInputStream) throws IOException {
		ZipEntry zipEntry;
		while ((zipEntry = zipInputStream.getNextJarEntry()) != null) {
			if (!zipEntry.isDirectory()) {
				String entryName = zipEntry.getName();
				String fileName = FilenameUtils.getName(entryName);

				if(buildInfoFilename.equals(fileName)) {
					String configName = FilenameUtils.getPathNoEndSeparator(entryName);
					Properties props = new Properties();
					try(Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(zipInputStream)) {
						props.load(reader);
						log.info("properties loaded from archive, filename [{}]", fileName);
					}
					String configVersion = props.getProperty("configuration.version");
					String configTimestamp = props.getProperty("configuration.timestamp");

					return new ConfigurationInfo(configName, configVersion, configTimestamp);
				}
			}
		}

		return null;
	}

	private void validate() throws ConfigurationException {
		if(StringUtils.isEmpty(getName()))
			throw new ConfigurationException("unknown configuration name");
		if(StringUtils.isEmpty(getVersion()))
			throw new ConfigurationException("unknown configuration version");
	}

	public InputStream getJar() {
		return new ByteArrayInputStream(jar);
	}
	public String getName() {
		return configInfo.getName();
	}
	public String getVersion() {
		return configInfo.getLegacyVersion();
	}
}
