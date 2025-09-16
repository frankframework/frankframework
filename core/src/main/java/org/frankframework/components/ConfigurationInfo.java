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
import java.util.jar.Manifest;

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.DateFormatUtils;

@Log4j2
public class ConfigurationInfo extends ComponentInfo {
	private static final DateTimeFormatter BUILDINFO_PROPERTIES_FORMATTER = DateTimeFormatter
			.ofPattern("yyyyMMdd-HHmm[ss]")
			.withZone(ZoneOffset.UTC)
			.withResolverStyle(ResolverStyle.LENIENT);


	public ConfigurationInfo(Manifest manifest) {
		super(manifest);
	}

	/**
	 * @param name Configuration Name, derived from the folder the configuration is in.
	 * @param version Derived from BuildInfo.properties -> ${configuration.version}
	 * @param timestamp Derived from BuildInfo.properties -> ${configuration.timestamp} using format 'YYYYMMdd-HHmmss'
	 */
	public ConfigurationInfo(String name, String version, String timestamp) {
		super(name, version, parseBuildInfoDate(timestamp));
	}

	@Override
	public String getVersion() {
		if (getTimestamp() == null) {
			return super.getVersion();
		}

		return "%s_%s".formatted(super.getVersion(), getFormattedTimestamp());
	}

	@Nullable
	public static ConfigurationInfo fromManifest(Manifest manifest) {
		if (manifest != null) {
			try {
				return new ConfigurationInfo(manifest);
			} catch (IllegalArgumentException e) {
				log.debug("was not able to parse MANIFEST file: {}", e::getMessage);
			}
		}

		return null;
	}

	private static Instant parseBuildInfoDate(String timestamp) {
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
}
