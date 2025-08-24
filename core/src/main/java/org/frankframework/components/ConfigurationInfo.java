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
package org.frankframework.components;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

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
	private final VersionRange frameworkVersion;

	private final Artifact artifact;

	/**
	 * @param name Configuration Name, derived from the folder the configuration is in.
	 * @param version Derived from BuildInfo.properties -> ${configuration.version}
	 * @param timestamp Derived from BuildInfo.properties -> ${configuration.timestamp} using format 'YYYYMMdd-HHmmss'
	 */
	public ConfigurationInfo(String name, String version, String timestamp) {
		if(StringUtils.isEmpty(name))
			throw new IllegalArgumentException("unknown configuration name");
		if(StringUtils.isEmpty(version))
			throw new IllegalArgumentException("unknown configuration version");

		this.name = name;
		this.version = new DefaultArtifactVersion(version);
		this.timestamp = parseDate(timestamp);

		this.description = null;
		this.organisation = null;

		this.javaVersion = null;
		this.classpath = Collections.emptyList();
		this.frameworkVersion = null;

		this.artifact = null;
	}

	private static Instant parseDate(String timestamp) {
		if (StringUtils.isBlank(timestamp)) {
			log.info("configuration has no timestamp");
			return null;
		}

		try {
			return DateFormatUtils.parseToInstant(timestamp, BUILDINFO_PROPERTIES_FORMATTER);
		} catch (DateTimeParseException dpe1) {
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

		if(StringUtils.isEmpty(this.name))
			throw new IllegalArgumentException("no (valid) name");

		String versionStr = manifest.getMainAttributes().getValue("Implementation-Version");
		if(StringUtils.isEmpty(versionStr))
			throw new IllegalArgumentException("no (valid) version");

		this.version = new DefaultArtifactVersion(versionStr);
		this.organisation = manifest.getMainAttributes().getValue("Implementation-Vendor");
		String timestampStr = manifest.getMainAttributes().getValue("Build-Timestamp"); // yyyy-MM-dd HH:mm:ss
		this.timestamp = StringUtils.isNotBlank(timestampStr) ? DateFormatUtils.parseGenericDate(timestampStr) : null;
		this.description = manifest.getMainAttributes().getValue("Description");

		String ffVersionStr = manifest.getMainAttributes().getValue("FrankFramework-Version");
		this.frameworkVersion = parseVersionRange(ffVersionStr);

		String artifactId = manifest.getMainAttributes().getValue("Artifact-Id");
		String groupId = manifest.getMainAttributes().getValue("Group-Id");
		if (StringUtils.isNoneBlank(artifactId, groupId, versionStr)) {
			this.artifact = new DefaultArtifact(groupId, artifactId, versionStr, null, getTimestamp(timestamp), "", null);
		} else {
			this.artifact = null;
		}
	}

	@Nullable
	public static ConfigurationInfo fromManifest(Manifest manifest) {
		if (manifest != null) {
			try {
				return new ConfigurationInfo(manifest);
			} catch (IllegalArgumentException e) {
				log.debug("was not able to parse METAINF file: {}", e::getMessage);
			}
		}

		return null;
	}

	private static VersionRange parseVersionRange(String ffVersion) {
		if (StringUtils.isNotBlank(ffVersion)) {
			try {
				return VersionRange.createFromVersionSpec(ffVersion);
			} catch (InvalidVersionSpecificationException e) {
				log.error("unable to parse FrankFramework version [{}]", ffVersion);
			}
		}

		return null;
	}

	private static String getTimestamp(Instant time) {
		if (time == null) {
			return null;
		}

		// Need to think of a new format to use, either way we cannot use [BUILDINFO_PROPERTIES_FORMATTER] because that contains seconds
		DateTimeFormatter format = DateTimeFormatter
				.ofPattern("yyyyMMdd-HHmm")
				.withZone(ZoneOffset.UTC)
				.withResolverStyle(ResolverStyle.LENIENT);
		return format.format(time);
	}

	public String getLegacyVersion() {
		if (timestamp == null) {
			return version.toString();
		}

		return "%s_%s".formatted(version.toString(), getTimestamp(timestamp));
	}
}
