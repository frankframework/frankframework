/*
   Copyright 2025 WeAreFrank!

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

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.StringUtil;

@Log4j2
@Getter
public class ConfigurationInfo {
	private static final DateTimeFormatter BUILDINFO_PROPERTIES_FORMATTER = DateTimeFormatter
			.ofPattern("yyyyMMdd-HHmm[ss]")
			.withZone(ZoneOffset.UTC)
			.withResolverStyle(ResolverStyle.LENIENT);

	private final String name;
	private final ArtifactVersion version;
	private final Instant timestamp;

	private final String description;
	private final String organisation;

	private final String javaVersion;
	private final List<String> classpath;
	private final ArtifactVersion frameworkVersion;

	private final String artifactId;
	private final String groupId;

	/**
	 * @param name Configuration Name, derived from the folder the configuration is in.
	 * @param version Derived from BuildInfo.properties -> ${configuration.version}
	 * @param timestamp Derived from BuildInfo.properties -> ${configuration.timestamp} using format 'YYYYMMdd-HHmmss'
	 */
	public ConfigurationInfo(String name, String version, String timestamp) {
		this.name = name;
		this.version = new DefaultArtifactVersion(version);
		this.timestamp = parseDate(timestamp);

		this.description = null;
		this.organisation = null;

		this.javaVersion = null;
		this.classpath = Collections.emptyList();
		this.frameworkVersion = null;
		this.artifactId = null;
		this.groupId = null;
	}

	private static Instant parseDate(String timestamp) {
		if (StringUtils.isBlank(timestamp)) {
			log.info("configuration has no timestamp");
			return null;
		}

		try {
			return DateFormatUtils.parseToInstant(timestamp, BUILDINFO_PROPERTIES_FORMATTER);
		} catch (DateTimeParseException dpe1) {
			dpe1.printStackTrace();
			try {
				return DateFormatUtils.parseGenericDate(timestamp);
			} catch (DateTimeParseException dpe2) {
				dpe1.addSuppressed(dpe2);
				log.warn("unable to parse timestamp [{}]", timestamp, dpe1);
			}
		}
		return null;
	}

	public ConfigurationInfo(Manifest manifest) {
		this.javaVersion = manifest.getMainAttributes().getValue("Build-Jdk-Spec");
		String classpathStr = manifest.getMainAttributes().getValue("Class-Path");
		this.classpath = StringUtil.split(classpathStr, " ");
		this.name = manifest.getMainAttributes().getValue("Implementation-Title");
		String versionStr = manifest.getMainAttributes().getValue("Implementation-Version");
		this.version = StringUtils.isNotBlank(versionStr) ? new DefaultArtifactVersion(versionStr) : null;
		this.organisation = manifest.getMainAttributes().getValue("Implementation-Vendor");
		this.artifactId = manifest.getMainAttributes().getValue("Artifact-Id");
		String timestampStr = manifest.getMainAttributes().getValue("Build-Timestamp"); // yyyy-MM-dd HH:mm:ss
		this.timestamp = StringUtils.isNotBlank(timestampStr) ? DateFormatUtils.parseGenericDate(timestampStr) : null;
		this.description = manifest.getMainAttributes().getValue("Description");
		String ffVersionStr = manifest.getMainAttributes().getValue("FrankFramework-Version");
		this.frameworkVersion = StringUtils.isNotBlank(ffVersionStr) ? new DefaultArtifactVersion(ffVersionStr) : null;
		this.groupId = manifest.getMainAttributes().getValue("Group-Id");
	}

	public static ConfigurationInfo fromManifest(Manifest manifest) {
		if (manifest != null) {
			ConfigurationInfo info = new ConfigurationInfo(manifest);
			if (info.getName() != null && info.getVersion() != null) {
				return info;
			}
		}

		return null;
	}

	public String getLegacyVersion() {
		DateTimeFormatter format = DateTimeFormatter
				.ofPattern("yyyyMMdd-HHmm")
				.withZone(ZoneOffset.UTC)
				.withResolverStyle(ResolverStyle.LENIENT);
		return "%s_%s".formatted(version.toString(), format.format(timestamp));
	}
}
