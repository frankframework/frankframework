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
import java.time.format.ResolverStyle;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.StringUtil;

@Log4j2
@Getter
public class ComponentInfo {

	private final String name;
	private final String version;
	private final Instant timestamp;

	private final String description;
	private final String organisation;

	private final String javaVersion;
	private final List<String> classpath;
	private final VersionRange frameworkVersion;

	protected ComponentInfo(@Nonnull String name, @Nonnull String version, @Nullable Instant timestamp) {
		this.name = name;
		this.version = version;
		this.timestamp = timestamp;

		validate();

		this.description = null;
		this.organisation = null;

		this.javaVersion = null;
		this.classpath = Collections.emptyList();
		this.frameworkVersion = null;
	}

	protected ComponentInfo(@Nonnull Manifest manifest) {
		this.name = manifest.getMainAttributes().getValue("Implementation-Title");
		this.version = manifest.getMainAttributes().getValue("Implementation-Version");

		String timestampStr = manifest.getMainAttributes().getValue("Build-Timestamp"); // yyyy-MM-dd HH:mm:ss
		this.timestamp = StringUtils.isNotBlank(timestampStr) ? DateFormatUtils.parseGenericDate(timestampStr) : null;

		validate();

		this.javaVersion = manifest.getMainAttributes().getValue("Build-Jdk-Spec");
		String classpathStr = manifest.getMainAttributes().getValue("Class-Path");
		this.classpath = StringUtil.split(classpathStr, " ");

		this.organisation = manifest.getMainAttributes().getValue("Implementation-Vendor");
		this.description = manifest.getMainAttributes().getValue("Description");

		String ffVersionStr = manifest.getMainAttributes().getValue("FrankFramework-Version");
		this.frameworkVersion = parseVersionRange(ffVersionStr);
	}

	private void validate() {
		if(StringUtils.isEmpty(name)) {
			throw new IllegalArgumentException("no (valid) name");
		}
		if(StringUtils.isEmpty(version)) {
			throw new IllegalArgumentException("no (valid) version");
		}
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

	protected String getFormattedTimestamp() {
		if (timestamp == null) {
			return null;
		}

		// Need to think of a new format to use, either way we cannot use [BUILDINFO_PROPERTIES_FORMATTER] because that contains seconds
		DateTimeFormatter format = DateTimeFormatter
				.ofPattern("yyyyMMdd-HHmm")
				.withZone(ZoneOffset.UTC)
				.withResolverStyle(ResolverStyle.LENIENT);
		return format.format(timestamp);
	}
}
