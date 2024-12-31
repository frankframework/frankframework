/*
   Copyright 2023-2024 WeAreFrank!

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
package org.frankframework.lifecycle.servlets;

import jakarta.servlet.annotation.ServletSecurity.TransportGuarantee;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.PropertyResolver;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.EnumUtils;

@Log4j2
public class SecuritySettings {
	public static final String AUTH_ENABLED_KEY = "application.security.http.authentication";
	public static final String HTTPS_ENABLED_KEY = "application.security.http.transportGuarantee";

	private static Boolean webSecurityEnabled = null;
	private static TransportGuarantee defaultTransportGuarantee = TransportGuarantee.CONFIDENTIAL;

	// package private for testing purposes
	static void resetSecuritySettings() {
		webSecurityEnabled = null;
		defaultTransportGuarantee = TransportGuarantee.CONFIDENTIAL;
	}

	public static void setupDefaultSecuritySettings(PropertyResolver properties) {
		if(webSecurityEnabled != null) {
			return; // Already configured, skipping...
		}

		boolean isDtapStageLoc = "LOC".equalsIgnoreCase(properties.getProperty("dtap.stage"));
		String isAuthEnabled = properties.getProperty(AUTH_ENABLED_KEY);
		webSecurityEnabled = StringUtils.isNotEmpty(isAuthEnabled) ? Boolean.parseBoolean(isAuthEnabled) : !isDtapStageLoc;

		String constraintType = properties.getProperty(HTTPS_ENABLED_KEY);
		if (StringUtils.isNotEmpty(constraintType)) {
			try {
				defaultTransportGuarantee = EnumUtils.parse(TransportGuarantee.class, constraintType);
			} catch(IllegalArgumentException e) {
				log.error("unable to set TransportGuarantee", e);
			}
		} else if(isDtapStageLoc) {
			defaultTransportGuarantee = TransportGuarantee.NONE;
		}
	}

	public static boolean isWebSecurityEnabled() {
		if (webSecurityEnabled == null) {
			throw new IllegalStateException("SecuritySettings not yet initialized");
		}

		return webSecurityEnabled;
	}

	public static TransportGuarantee getDefaultTransportGuarantee() {
		return defaultTransportGuarantee;
	}
}
