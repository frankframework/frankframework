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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.credentialprovider.CredentialFactory;
import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.AppConstants;

public class AppConstantsValidatorTest {

	private AppConstants appConstants;

	@BeforeEach
	public void setUp() {
		appConstants = AppConstants.getInstance();
	}

	@AfterEach
	public void tearDown() {
		appConstants.remove("jdbc.convertFieldnamesToUppercase");
		appConstants.remove(AppConstants.ADDITIONAL_PROPERTIES_FILE_SUFFIX_KEY);
		appConstants.remove("configurations.autoDatabaseClassLoader");
		appConstants.remove("instance.name");

		CredentialConstants.getInstance().remove(CredentialFactory.CREDENTIAL_FACTORY_KEY);
		ApplicationWarnings.removeInstance();
		AppConstants.removeInstance();
	}

	@Test
	public void testDeprecatedJdbcConvertFieldnamesToUppercaseFalseTriggersWarning() {
		// Arrange
		appConstants.setProperty("jdbc.convertFieldnamesToUppercase", "false");

		// Act
		AppConstantsValidator.validate();

		// Assert
		List<String> warnings = ApplicationWarnings.getWarningsList();
		assertEquals(1, warnings.size());
		assertThat(warnings.getFirst(), containsString("jdbc.convertFieldnamesToUppercase"));
		assertThat(warnings.getFirst(), containsString("false"));
	}

	@Test
	public void testDeprecatedJdbcConvertFieldnamesToUppercaseTrueDoesNotTriggerWarning() {
		// Arrange
		appConstants.setProperty("jdbc.convertFieldnamesToUppercase", "true");

		// Act
		AppConstantsValidator.validate();

		// Assert
		assertEquals(0, ApplicationWarnings.getWarningsList().size());
	}

	@Test
	public void testDeprecatedAdditionalPropertiesFileSuffixTriggersWarningWhenSet() {
		// Arrange
		appConstants.setProperty(AppConstants.ADDITIONAL_PROPERTIES_FILE_SUFFIX_KEY, ".properties");

		// Act
		AppConstantsValidator.validate();

		// Assert
		List<String> warnings = ApplicationWarnings.getWarningsList();
		assertEquals(1, warnings.size());
		assertThat(warnings.getFirst(), containsString(AppConstants.ADDITIONAL_PROPERTIES_FILE_SUFFIX_KEY));
	}

	@Test
	public void testDeprecatedConfigurationsAutoDatabaseClassLoaderTriggersWarningWhenSet() {
		// Arrange
		appConstants.setProperty("configurations.autoDatabaseClassLoader", "someValue");

		// Act
		AppConstantsValidator.validate();

		// Assert
		List<String> warnings = ApplicationWarnings.getWarningsList();
		assertEquals(1, warnings.size());
		assertThat(warnings.getFirst(), containsString("configurations.autoDatabaseClassLoader"));
	}

	@Test
	public void testInstanceNameNotMatchingRegex() {
		// Arrange
		appConstants.setProperty("instance.name", "invalid instance name");

		// Act
		// Assert
		assertThrows(IllegalStateException.class, AppConstantsValidator::validate);
	}

	@Test
	public void testLegacyCredentialFactoryPackageNameTriggersWarning() {
		// Arrange
		CredentialConstants.getInstance().setProperty(CredentialFactory.CREDENTIAL_FACTORY_KEY, "nl.nn.credentialprovider.SomeFactory");

		// Act
		AppConstantsValidator.validate();

		// Assert
		List<String> warnings = ApplicationWarnings.getWarningsList();
		assertEquals(1, warnings.size());
		assertThat(warnings.getFirst(), containsString("legacy classnames"));
		assertThat(warnings.getFirst(), containsString("nl.nn.credentialprovider."));
	}

	@Test
	public void testNonLegacyCredentialFactoryPackageNameDoesNotTriggerWarning() {
		// Arrange
		CredentialConstants.getInstance().setProperty(CredentialFactory.CREDENTIAL_FACTORY_KEY, "org.frankframework.credentialprovider.SomeFactory");

		// Act
		AppConstantsValidator.validate();

		// Assert
		assertEquals(0, ApplicationWarnings.getWarningsList().size());
	}

	@Test
	public void testDeprecationChecksAllDeps() {
		// Arrange
		appConstants.setProperty("jdbc.convertFieldnamesToUppercase", false);
		appConstants.setProperty(AppConstants.ADDITIONAL_PROPERTIES_FILE_SUFFIX_KEY, ".propmap");
		appConstants.setProperty("configurations.autoDatabaseClassLoader", "not-empty");

		CredentialConstants credentialConstants = CredentialConstants.getInstance();
		Object credentialFactoryOriginalValue = credentialConstants.setProperty(CredentialFactory.CREDENTIAL_FACTORY_KEY, "nl.nn.credentialprovider.PropertyFileCredentialFactory");

		// Act
		AppConstantsValidator.validate();

		// Assert
		try {
			List<String> warnings = ApplicationWarnings.getWarningsList();

			assertEquals(4, warnings.size());
			assertThat(warnings, containsInAnyOrder(
					containsString("DEPRECATED: legacy classnames from package [" + CredentialFactory.LEGACY_PACKAGE_NAME + "] used for creating CredentialProviders"),
					containsString("DEPRECATED: [configurations.autoDatabaseClassLoader] is set to [not-empty]"),
					containsString("DEPRECATED: [ADDITIONAL.PROPERTIES.FILE.SUFFIX] is set to [.propmap]"),
					containsString("DEPRECATED: [jdbc.convertFieldnamesToUppercase] is set to [false]")
					));
		} finally {
			// Clean up properties set for this test to make sure we do not mess up any other tests
			AppConstants.removeInstance();
			ApplicationWarnings.removeInstance();

			if (credentialFactoryOriginalValue == null) {
				credentialConstants.remove(CredentialFactory.CREDENTIAL_FACTORY_KEY);
			} else {
				credentialConstants.setProperty(CredentialFactory.CREDENTIAL_FACTORY_KEY, credentialFactoryOriginalValue.toString());
			}
		}
	}
}
