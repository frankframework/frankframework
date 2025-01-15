package org.frankframework.configuration.digester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.HasName;
import org.frankframework.core.NameAware;
import org.frankframework.doc.Protected;
import org.frankframework.doc.Unsafe;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;

public class ValidateAttributeRuleTest {
	private TestConfiguration configuration;

	// Convenience method to create an Attribute list to be parsed
	private Attributes copyMapToAttrs(Map<String, String> map) {
		List<String[]> attList = new LinkedList<>();
		for (String key : map.keySet()) {
			attList.add(new String[]{key, map.get(key)});
		}

		Attributes attrs = spy(Attributes.class);
		when(attrs.getLocalName(anyInt())).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				int i = (int) invocation.getArguments()[0];
				return attList.get(i)[0];
			}
		});
		when(attrs.getValue(anyInt())).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				int i = (int) invocation.getArguments()[0];
				return attList.get(i)[1];
			}
		});
		when(attrs.getLength()).thenReturn(attList.size());
		return attrs;
	}

	// Run the ValidateAttributeRule, returns the beanClass to validate setters being called
	private <T> T runRule(Class<T> beanClass, Map<String, String> attributes) throws Exception {
		configuration = new TestConfiguration();
		T topBean = ClassUtils.newInstance(beanClass);
		ValidateAttributeRule rule = new ValidateAttributeRule() {
			@Override
			public Object getBean() {
				return topBean;
			}
		};
		configuration.autowireByName(rule);

		rule.begin(beanClass.getSimpleName(), copyMapToAttrs(attributes));

		// Test the bean name with and without NameAware interface
		if (topBean instanceof ConfigWarningTestClass) {
			assertEquals("ConfigWarningTestClass [name here]", rule.getObjectName());
		}
		if (topBean instanceof DeprecatedTestClass) {
			assertEquals("DeprecatedTestClass", rule.getObjectName());
		}

		return topBean;
	}

	@Test
	public void testSimpleAttribute() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("name", "my-string-value");
		attr.put("testString", "testStringValue");
		attr.put("deprecatedString", "deprecatedValue");
		attr.put("testInteger", "3");
		attr.put("testLong", "3");
		attr.put("testBoolean", "true");
		attr.put("testEnum", "two");

		ClassWithEnum bean = runRule(ClassWithEnum.class, attr);

		assertEquals("my-string-value", bean.getName());
		assertEquals("testStringValue", bean.getTestString());
		assertEquals("deprecatedValue", bean.getDeprecatedString());
		assertEquals(3, bean.getTestInteger());
		assertEquals(3L, bean.getTestLong());
		assertTrue(bean.isTestBoolean());
		assertEquals(TestEnum.TWO, bean.getTestEnum());

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(0, configWarnings.size());
	}

	@Test
	public void testAttributeFromInterfaceDefaultMethod() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("naam", "Pietje Puk");

		ClassWithEnum bean = runRule(ClassWithEnum.class, attr);

		assertEquals("Pietje Puk", bean.getName());
		assertEquals("Pietje Puk", bean.getNaam());
	}

	@Test
	public void testEmptyAttribute() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("testString", "");
		attr.put("testInteger", "");
		attr.put("testBoolean", "");

		ClassWithEnum bean = runRule(ClassWithEnum.class, attr);

		assertEquals("", bean.getTestString(), "empty string value should be empty string");
		assertEquals(1234, bean.getTestInteger(), "empty int value should be ignored"); // May trigger cannot be converted to int exception
		assertFalse(bean.isTestBoolean(), "empty bool value should be ignored"); //may trigger a default warning exception

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(0, configWarnings.size(), "there should not be any configuration warnings but got: " + configWarnings.getWarnings());
	}

	@Test
	public void testAttributeThatDoesNotExist() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("do-not-exist", "string value here");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum does not have an attribute [do-not-exist] to set to value [string value here]", configWarnings.get(0));
	}

	@Test
	public void testAttributeWithConfigWarning() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("configWarningString", "string value here");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [configWarningString]: my test warning", configWarnings.get(0));
	}

	@Test
	void testEnumAttributeWithConfigWarningAndDefaultValueWarning() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("queryType", "INSERT");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(2, configWarnings.size());
		assertEquals("ClassWithEnum attribute [queryType.INSERT] has been deprecated since v8.1.0: Use queryType 'OTHER' instead", configWarnings.get(0));
		assertEquals("ClassWithEnum attribute [queryType] already has a default value [INSERT]", configWarnings.get(1));
	}

	@Test
	void testEnumAttributeWithConfigWarning() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("queryType", "SELECT");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [queryType.SELECT]: Select might be slow", configWarnings.get(0));
	}

	@Test
	public void testDeprecatedAttributeWithConfigWarning() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("deprecatedConfigWarningString", "string value here");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [deprecatedConfigWarningString] is deprecated: my deprecated test warning", configWarnings.get(0));
	}

	@Test
	public void testDeprecatedAttributeWithConfigWarningSinceAndForRemoval() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("deprecatedConfigWarningSinceAndForRemoval", "string value here");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [deprecatedConfigWarningSinceAndForRemoval] has been deprecated since v8.0.0 and has been marked for removal: this is since and for removal", configWarnings.get(0));
	}

	@Test
	public void testDeprecatedAttributeWithConfigWarningSince() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("deprecatedConfigWarningSince", "string value here");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [deprecatedConfigWarningSince] has been deprecated since v8.0.0: this is since", configWarnings.get(0));
	}
	@Test
	public void testDeprecatedAttributeWithConfigWarningForRemoval() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("deprecatedConfigWarningForRemoval", "string value here");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [deprecatedConfigWarningForRemoval] is deprecated and has been marked for removal: this is for removal", configWarnings.get(0));
	}

	@Test
	public void testDeprecatedAttributeWithoutConfigWarning() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("deprecatedString", "string value here");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(0, configWarnings.size());
	}

	@Test
	@DisplayName("All attributes should be resolved")
	public void testAttributeWithPropertyRef() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("name", "${instance.name}");
		attr.put("testString", "${instance.name}");

		ClassWithEnum bean = runRule(ClassWithEnum.class, attr);

		assertEquals(TestConfiguration.TEST_CONFIGURATION_NAME, bean.getName());
		assertEquals(TestConfiguration.TEST_CONFIGURATION_NAME, bean.getTestString());
	}

	@Test
	public void testAttributeValueEqualToDefaultValue() throws Exception {
		// Arrange
		Map<String, String> attr = new LinkedHashMap<>();
		attr.put("testString", "test");
		attr.put("testInteger", "1234");
		attr.put("testLong", "0");
		attr.put("testBoolean", "false");
		attr.put("testEnum", "one");

		// Act
		runRule(ClassWithEnum.class, attr);

		// Assert
		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(5, configWarnings.size());
		assertEquals("ClassWithEnum attribute [testString] already has a default value [test]", configWarnings.get(0));
		assertEquals("ClassWithEnum attribute [testInteger] already has a default value [1234]", configWarnings.get(1));
		assertEquals("ClassWithEnum attribute [testLong] already has a default value [0]", configWarnings.get(2));
		assertEquals("ClassWithEnum attribute [testBoolean] already has a default value [false]", configWarnings.get(3));
		assertEquals("ClassWithEnum attribute [testEnum] already has a default value [one]", configWarnings.get(4));
	}

	@Test
	public void testAttributeValueEqualToDefaultValueWarningsSuppressed() throws Exception {
		// Arrange
		configuration = new TestConfiguration(TestConfiguration.TEST_CONFIGURATION_FILE);
		AppConstants appConstants = loadAppConstants(configuration);
		appConstants.setProperty(SuppressKeys.DEFAULT_VALUE_SUPPRESS_KEY.getKey(), true);

		Map<String, String> attr = new LinkedHashMap<>();
		attr.put("testString", "test");
		attr.put("testInteger", "0");
		attr.put("testLong", "0");
		attr.put("testBoolean", "false");
		attr.put("testEnum", "one");

		// Act
		runRule(ClassWithEnum.class, attr);

		// After
		appConstants.remove(SuppressKeys.DEFAULT_VALUE_SUPPRESS_KEY.getKey());


		// Assert
		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertTrue(configWarnings.isEmpty());
	}

	@Test
	public void testAttributeWithNumberFormatException() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("testInteger", "a String");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum cannot set field [testInteger]: value [a String] cannot be converted to a number [int]", configWarnings.get(0));
	}

	@Test
	public void testAttributeWithoutGetterNumberFormatException() throws Exception {
		Map<String, String> attr = new HashMap<>();

		attr.put("testStringWithoutGetter", "a String"); // Should work
		attr.put("testIntegerWithoutGetter", "a String"); // Throws Exception

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum cannot set field [testIntegerWithoutGetter]: value [a String] cannot be converted to a number [int]", configWarnings.get(0));
	}

	@Test
	public void testUnparsableEnum() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("testEnum", "notEnumValue");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum cannot set field [testEnum]: unparsable value [notEnumValue]. Must be one of [ONE, TWO]", configWarnings.get(0));
	}

	@Test
	public void testUnparsableEnumWithDifferentFieldName() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("enumWithDifferentName", "notEnumValue");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum cannot set field [enumWithDifferentName]: unparsable value [notEnumValue]. Must be one of [ONE, TWO]", configWarnings.get(0));
	}

	@Test
	public void testDeprecatedTestClass() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("name", "name here");

		runRule(DeprecatedTestClass.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("DeprecatedTestClass is deprecated: warning above deprecated test class", configWarnings.get(0));
	}

	@Test
	public void testConfigWarningTestClass() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("name", "name here");

		runRule(ConfigWarningTestClass.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ConfigWarningTestClass [name here] : warning above test class", configWarnings.get(0));
	}

	@Test
	public void testEnumGetterSetter() throws Exception {
		ClassWithEnum bean = new ClassWithEnum();
		PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(bean, "testEnum");
		Method writeMethod = pd.getWriteMethod();
		assertNotNull(writeMethod);
		assertEquals("TestEnum", writeMethod.getParameters()[0].getType().getSimpleName());

		Method readMethod = pd.getReadMethod();
		assertNotNull(readMethod);
		assertEquals("TestEnum", readMethod.getReturnType().getSimpleName());
	}

	@Test
	void testVarArgsIntegerSetter() throws Exception {
		ClassWithEnum bean = new ClassWithEnum();
		PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(bean, "integerVarArgs");
		Method writeMethod = pd.getWriteMethod();
		writeMethod.invoke(bean, new Object[]{new Integer[]{8, 3}});
		assertNotNull(writeMethod);
		assertEquals("Integer[]", writeMethod.getParameters()[0].getType().getSimpleName());
		assertEquals(11, bean.getTestInteger());
	}

	@Test
	void testVarArgsEnumSetter() throws Exception {
		ClassWithEnum bean = new ClassWithEnum();
		PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(bean, "enumVarArgs");
		Method writeMethod = pd.getWriteMethod();
		writeMethod.invoke(bean, new Object[]{new TestEnum[]{TestEnum.TWO}});
		assertNotNull(writeMethod);
		assertEquals("TestEnum[]", writeMethod.getParameters()[0].getType().getSimpleName());
		assertEquals(TestEnum.TWO, bean.getTestEnum());
	}

	@Test
	public void testSuppressAttribute() throws Exception {
		Map<String, String> attr = new HashMap<>();

		attr.put("testStringWithoutGetter", "text");
		attr.put("testSuppressAttribute", "text"); // Should fail

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [testSuppressAttribute] is protected, cannot be set from configuration", configWarnings.get(0));
	}

	@Test
	public void testUnsafeAttribute() throws Exception {
		Map<String, String> attr = new HashMap<>();

		attr.put("testUnsafeAttribute", "unsafe");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum [testUnsafeAttribute] is unsafe and should not be used in a production environment", configWarnings.get(0));
	}

	@Test
	public void testUnsafeAttributeWithDefault() throws Exception {
		Map<String, String> attr = new HashMap<>();

		attr.put("testUnsafeAttributeWithDefault", "default");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [testUnsafeAttributeWithDefault] already has a default value [default]", configWarnings.get(0));
	}

	@Test
	public void testUnsafeAttributeWithoutDefault() throws Exception {
		Map<String, String> attr = new HashMap<>();

		attr.put("testUnsafeAttributeWithDefault", "unsafe");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum [testUnsafeAttributeWithDefault] is unsafe and should not be used in a production environment", configWarnings.get(0));
	}

	@Test
	public void testSuppressDeprecationWarningsForSomeAdapters() throws IOException {
		// Arrange
		configuration = new TestConfiguration("testConfigurationWithDigester.xml");
		configuration.setId("TestSuppressDeprecationWarningsConfiguration");
		loadAppConstants(configuration);

		// Act
		// Refreshing the configuration will trigger the loading via digester
		configuration.refresh();

		// Assert
		ConfigurationWarnings configurationWarnings = configuration.getBean(ConfigurationWarnings.class);
		assertThat(configurationWarnings.getWarnings(), not(anyOf(
				hasItem(containsString("DeprecatedPipe1InAdapter1")),
				hasItem(containsString("DeprecatedPipe2InAdapter1")),
				hasItem(containsString("DeprecatedPipe1InAdapter3")),
				hasItem(containsString("DeprecatedPipe2InAdapter3"))
		)));
		assertThat(configurationWarnings.getWarnings(), containsInAnyOrder(
				containsString("DeprecatedPipe2InAdapter2"),
				containsString("DeprecatedPipe1InAdapter4"),
				containsString("DeprecatedPipe2InAdapter4")
		));
		assertEquals(3, configurationWarnings.getWarnings().size());
	}

	@Test
	public void testSuppressDeprecationWarningsWithLocationInfo() throws IOException {
		// Arrange
		configuration = new TestConfiguration("testConfigurationWithDigester.xml");
		configuration.setId("TestSuppressDeprecationWarningsConfiguration");
		AppConstants appConstants = loadAppConstants(configuration);
		appConstants.setProperty("configuration.warnings.linenumbers", true);

		try {
			// Act
			// Refreshing the configuration will trigger the loading via digester
			configuration.refresh();

			// Assert
			ConfigurationWarnings configurationWarnings = configuration.getBean(ConfigurationWarnings.class);
			assertThat(configurationWarnings.getWarnings(), not(anyOf(
					hasItem(containsString("[DeprecatedPipe1InAdapter1]")),
					hasItem(containsString("[DeprecatedPipe2InAdapter1]")),
					hasItem(containsString("[DeprecatedPipe1InAdapter3]")),
					hasItem(containsString("[DeprecatedPipe2InAdapter3]"))
			)));
			assertEquals(3, configurationWarnings.getWarnings().size());
			assertThat(configurationWarnings.getWarnings(), containsInAnyOrder(
					containsString("[DeprecatedPipe2InAdapter2] on line [42] column [6]"),
					containsString("[DeprecatedPipe1InAdapter4] on line [77] column [52]"),
					containsString("[DeprecatedPipe2InAdapter4] on line [82] column [6]")
			));

		} finally {
			// Cleanup so we don't crash other tests
			appConstants.setProperty("configuration.warnings.linenumbers", false);
		}
	}

	@Test
	public void testSuppressDeprecationWarningsForAllAdapters() throws IOException {
		// Arrange
		configuration = new TestConfiguration("testConfigurationWithDigester.xml");
		configuration.setId("TestSuppressDeprecationWarningsConfiguration");
		AppConstants appConstants = loadAppConstants(configuration);
		appConstants.setProperty(SuppressKeys.DEPRECATION_SUPPRESS_KEY.getKey(), true);

		try {
			// Act
			// Refreshing the configuration will trigger the loading via digester
			configuration.refresh();

			// Assert
			ConfigurationWarnings configurationWarnings = configuration.getBean(ConfigurationWarnings.class);
			assertTrue(configurationWarnings.getWarnings().isEmpty());

		} finally {
			// Cleanup so we don't crash other tests
			appConstants.setProperty(SuppressKeys.DEPRECATION_SUPPRESS_KEY.getKey(), false);
		}
	}

	private AppConstants loadAppConstants(ApplicationContext applicationContext) throws IOException {
		AppConstants appConstants = AppConstants.getInstance(applicationContext != null ? applicationContext.getClassLoader() : this.getClass().getClassLoader());
		appConstants.load(getClass().getClassLoader().getResourceAsStream("AppConstants/AppConstants_ValidateAttributeRuleTest.properties"));
		return appConstants;
	}

	public static enum TestEnum {
		ONE, TWO;
	}

	public static interface InterfaceWithDefaultMethod extends NameAware, HasName {

		default void setNaam(String naam) {
			setName(naam);
		}

		default String getNaam() {
			return getName();
		}
	}

	public static class ClassWithEnum extends ClassWithEnumBase implements NameAware, InterfaceWithDefaultMethod {
		private @Getter @Setter String name;
		private @Getter @Setter TestEnum testEnum = TestEnum.ONE;
		private @Getter @Setter String testString = "test";
		private @Deprecated @Getter @Setter String deprecatedString;
		private @Getter String configWarningString;
		private @Getter String deprecatedConfigWarningString;
		private @Getter @Setter int testInteger = 1234;
		private @Getter @Setter long testLong = 0L;
		private @Getter @Setter boolean testBoolean = false;
		private @Setter String testStringWithoutGetter = "string";
		private @Setter int testIntegerWithoutGetter = 0;
		private @Setter boolean testBooleanWithoutGetter = false;
		private @Getter String testUnsafeAttributeWithDefault = "default";

		public void setEnumWithDifferentName(TestEnum testEnum) {
			this.testEnum = testEnum;
		}

		public void setEnumVarArgs(TestEnum... testEnum) {
			this.testEnum = testEnum[0];
		}

		public void setIntegerVarArgs(Integer... integerVarArgs) {
			testInteger = integerVarArgs[0] + integerVarArgs[1];
		}

		@ConfigurationWarning("my test warning")
		public void setConfigWarningString(String str) {
			configWarningString = str;
		}

		@ConfigurationWarning("my deprecated test warning")
		@Deprecated
		public void setDeprecatedConfigWarningString(String str) {
			deprecatedConfigWarningString = str;
		}

		@ConfigurationWarning("this is for removal")
		@Deprecated(forRemoval = true)
		public void setDeprecatedConfigWarningForRemoval(String str) {
			deprecatedConfigWarningString = str;
		}

		@ConfigurationWarning("this is since")
		@Deprecated(since = "8.0.0")
		public void setDeprecatedConfigWarningSince(String str) {
			deprecatedConfigWarningString = str;
		}

		@ConfigurationWarning("this is since and for removal")
		@Deprecated(since = "8.0.0", forRemoval = true)
		public void setDeprecatedConfigWarningSinceAndForRemoval(String str) {
			deprecatedConfigWarningString = str;
		}

		@Protected
		public void setTestSuppressAttribute(String test) {
			testString = test;
		}

		@Unsafe
		public void setTestUnsafeAttribute(String test) {
			// NO OP
		}

		@Unsafe
		public void setTestUnsafeAttributeWithDefault(String testUnsafeAttributeWithDefault) {
			this.testUnsafeAttributeWithDefault = testUnsafeAttributeWithDefault;
		}
	}

	public abstract static class ClassWithEnumBase {
		private @Setter @Getter QueryType queryType = QueryType.INSERT;

		public enum QueryType {
			OTHER,
			/** Deprecated: Use OTHER instead */
			@ConfigurationWarning("Use queryType 'OTHER' instead")
			@Deprecated(since = "8.1.0") INSERT,
			@ConfigurationWarning("Select might be slow")
			SELECT
		}
	}

	@ConfigurationWarning("warning above test class")
	public static class ConfigWarningTestClass implements NameAware, HasName {
		private @Getter @Setter String name;
	}

	@Deprecated
	@ConfigurationWarning("warning above deprecated test class")
	public static class DeprecatedTestClass {
		private @Getter @Setter String name;
	}
}
