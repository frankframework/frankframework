/*
  Copyright 2026 WeAreFrank!

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
package org.frankframework.configuration;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import lombok.experimental.UtilityClass;
import lombok.extern.log4j.Log4j2;

import org.frankframework.credentialprovider.CredentialFactory;
import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.AppConstants;

/**
 * Contains validation logic for AppConstants properties, including checks for deprecated properties and regex validation for specific properties.
 */
@Log4j2
@UtilityClass
public class AppConstantsValidator {

	private static final String DEPRECATED_PROPERTY_MESSAGE = "DEPRECATED: [%s] is set to [%s]";

	private static final Map<String, Function<String, Boolean>> deprecatedProperties = Map.of(
			"jdbc.convertFieldnamesToUppercase", "false"::equalsIgnoreCase,
			AppConstants.ADDITIONAL_PROPERTIES_FILE_SUFFIX_KEY, StringUtils::isNotEmpty,
			"configurations.autoDatabaseClassLoader", StringUtils::isNotEmpty
	);

	private static final Map<String, String> propertyRegexMatchers = Map.of(
			// Validates that the instance name is between 1 and 253 characters, starts and ends with an alphanumeric character, and may contain dots and hyphens in between
			"instance.name", "(?=.{1,253}\\.?$)[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?\\.?"
	);

	public static void validate() {
		AppConstants appConstants = AppConstants.getInstance();

		validateAppConstants(appConstants);
		checkForDeprecations(appConstants);
	}

	/**
	 * Validates the properties in AppConstants against predefined regex patterns.
	 */
	private static void validateAppConstants(AppConstants appConstants) {
		propertyRegexMatchers.forEach((key, regex) -> {
			String propertyValue = appConstants.getProperty(key);

			if (propertyValue != null && !propertyValue.matches(regex)) {
				String error = String.format("Property [%s] with value [%s] does not match the expected pattern [%s]", key, propertyValue, regex);
				log.error(error);

				throw new IllegalStateException(error);
			}
		});
	}

	/**
	 * Checks for deprecated properties and/or values in AppConstants and logs warnings if present
	 */
	private static void checkForDeprecations(AppConstants appConstants) {
		deprecatedProperties.forEach((key, value) -> {
			String deprecatedProperty = appConstants.getProperty(key);

			if (Boolean.TRUE.equals(value.apply(deprecatedProperty))) {
				ApplicationWarnings.add(log, String.format(DEPRECATED_PROPERTY_MESSAGE, key, deprecatedProperty));
			}
		});

		String credentialFactoryClassNames = CredentialConstants.getInstance().getProperty(CredentialFactory.CREDENTIAL_FACTORY_KEY);
		// Legacy support for old package names; to be removed in Frank!Framework 8.1 or later
		if (StringUtils.isNotEmpty(credentialFactoryClassNames) && credentialFactoryClassNames.contains(CredentialFactory.LEGACY_PACKAGE_NAME)) {
			ApplicationWarnings.add(log, "DEPRECATED: legacy classnames from package [" +
					CredentialFactory.LEGACY_PACKAGE_NAME + "] used for creating CredentialProviders, please update to use new classnames starting with [" +
					CredentialFactory.ORG_FRANKFRAMEWORK_PACKAGE_NAME + "]: [" + credentialFactoryClassNames + "]");
		}
	}

}
