package nl.nn.adapterframework.configuration.digester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.doc.Protected;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.util.AppConstants;

public class ValidateAttributeRuleTest extends Mockito {
	private TestConfiguration configuration;

	//Convenience method to create an Attribute list to be parsed
	private Attributes copyMapToAttrs(Map<String, String> map) {
		List<String[]> attList = new LinkedList<>();
		for(String key : map.keySet()) {
			attList.add(new String[] {key , map.get(key)});
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

	//Run the ValidateAttributeRule, returns the beanClass to validate setters being called
	private <T> T runRule(Class<T> beanClass, Map<String, String> attributes) throws Exception {
		configuration = new TestConfiguration();
		T topBean = beanClass.newInstance();
		ValidateAttributeRule rule = new ValidateAttributeRule() {
			@Override
			public Object getBean() {
				return topBean;
			}
		};
		configuration.autowireByName(rule);

		rule.begin(null, beanClass.getSimpleName(), copyMapToAttrs(attributes));

		//Test the bean name with and without INamedObject interface
		if(topBean instanceof ConfigWarningTestClass) {
			assertEquals("ConfigWarningTestClass [name here]", rule.getObjectName());
		}
		if(topBean instanceof DeprecatedTestClass) {
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
		assertEquals(true, bean.isTestBoolean());
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
	public void testAttributeThatDoesntExist() throws Exception {
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
	public void testDeprecatedAttributeWithConfigWarning() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("deprecatedConfigWarningString", "string value here");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [deprecatedConfigWarningString] is deprecated: my deprecated test warning", configWarnings.get(0));
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
	public void testAttributeWithPropertyRef() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("name", "${instance.name}");
		attr.put("testString", "${instance.name}");

		ClassWithEnum bean = runRule(ClassWithEnum.class, attr);

		assertEquals("${instance.name}", bean.getName()); //Does not resolve
		assertEquals(TestConfiguration.TEST_CONFIGURATION_NAME, bean.getTestString()); //Does resolve
	}

	@Test
	public void testAttributeValueEqualToDefaultValue() throws Exception {
		// Arrange
		Map<String, String> attr = new LinkedHashMap<>();
		attr.put("testString", "test");
		attr.put("testInteger", "0");
		attr.put("testLong", "0");
		attr.put("testBoolean", "false");
		attr.put("testEnum", "one");

		// Act
		runRule(ClassWithEnum.class, attr);

		// Assert
		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(5, configWarnings.size());
		assertEquals("ClassWithEnum attribute [testString] already has a default value [test]", configWarnings.get(0));
		assertEquals("ClassWithEnum attribute [testInteger] already has a default value [0]", configWarnings.get(1));
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
		assertEquals("ClassWithEnum cannot set field [testInteger] of type [int]: value [a String] cannot be converted to a number", configWarnings.get(0));
	}

	@Test
	public void testAttributeWithoutGetterNumberFormatException() throws Exception {
		Map<String, String> attr = new HashMap<>();

		attr.put("testStringWithoutGetter", "a String"); //Should work
		attr.put("testIntegerWithoutGetter", "a String"); //Throws Exception

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum cannot set field [testIntegerWithoutGetter] of type [int]: value [a String] cannot be converted to a number", configWarnings.get(0));
	}

	@Test
	public void testUnparsableEnum() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("testEnum", "unparsable");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum cannot set field [testEnum] to unparsable value [unparsable]. Must be one of [ONE, TWO]", configWarnings.get(0));
	}

	@Test
	public void testUnparsableEnumWithDifferentFieldName() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("enumWithDifferentName", "unparsable");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum cannot set field [enumWithDifferentName] to unparsable value [unparsable]. Must be one of [ONE, TWO]", configWarnings.get(0));
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
	public void testSuppressAttribute() throws Exception {
		Map<String, String> attr = new HashMap<>();

		attr.put("testStringWithoutGetter", "text");
		attr.put("testSuppressAttribute", "text"); //Should fail

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [testSuppressAttribute] is protected, cannot be set from configuration", configWarnings.get(0));
	}

	@SuppressWarnings("unchecked")
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
		assertEquals(4, configurationWarnings.getWarnings().size());
		assertThat(configurationWarnings.getWarnings(), not(anyOf(
			hasItem(containsString("DeprecatedPipe1InAdapter1")),
			hasItem(containsString("DeprecatedPipe2InAdapter1")),
			hasItem(containsString("DeprecatedPipe1InAdapter3")),
			hasItem(containsString("DeprecatedPipe2InAdapter3"))
		)));
		assertThat(configurationWarnings.getWarnings(), containsInAnyOrder(
			containsString("DeprecatedPipe1InAdapter2"),
			containsString("DeprecatedPipe2InAdapter2"),
			containsString("DeprecatedPipe1InAdapter4"),
			containsString("DeprecatedPipe2InAdapter4")
		));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSuppressDeprecationWarningsWithLocationInfo() throws IOException {
		// Arrange
		configuration = new TestConfiguration("testConfigurationWithDigester.xml");
		configuration.setId("TestSuppressDeprecationWarningsConfiguration");
		AppConstants appConstants = loadAppConstants(configuration);
		appConstants.setProperty("configuration.warnings.linenumbers", true);

		// Act
		// Refreshing the configuration will trigger the loading via digester
		configuration.refresh();

		// Assert
		ConfigurationWarnings configurationWarnings = configuration.getBean(ConfigurationWarnings.class);
		assertEquals(4, configurationWarnings.getWarnings().size());
		assertThat(configurationWarnings.getWarnings(), not(anyOf(
			hasItem(containsString("[DeprecatedPipe1InAdapter1]")),
			hasItem(containsString("[DeprecatedPipe2InAdapter1]")),
			hasItem(containsString("[DeprecatedPipe1InAdapter3]")),
			hasItem(containsString("[DeprecatedPipe2InAdapter3]"))
		)));
		assertThat(configurationWarnings.getWarnings(), containsInAnyOrder(
			containsString("[DeprecatedPipe1InAdapter2] on line [35] column [91]"),
			containsString("[DeprecatedPipe2InAdapter2] on line [42] column [6]"),
			containsString("[DeprecatedPipe1InAdapter4] on line [79] column [24]"),
			containsString("[DeprecatedPipe2InAdapter4] on line [84] column [6]")
		));

		// Cleanup so we don't crash other tests
		appConstants.setProperty("configuration.warnings.linenumbers", false);
	}

	@Test
	public void testSuppressDeprecationWarningsForAllAdapters() throws IOException {
		// Arrange
		configuration = new TestConfiguration("testConfigurationWithDigester.xml");
		configuration.setId("TestSuppressDeprecationWarningsConfiguration");
		AppConstants appConstants = loadAppConstants(configuration);
		appConstants.setProperty(SuppressKeys.DEPRECATION_SUPPRESS_KEY.getKey(), true);

		// Act
		// Refreshing the configuration will trigger the loading via digester
		configuration.refresh();

		// Assert
		ConfigurationWarnings configurationWarnings = configuration.getBean(ConfigurationWarnings.class);
		assertTrue(configurationWarnings.getWarnings().isEmpty());

		// Cleanup so we don't crash other tests
		appConstants.setProperty(SuppressKeys.DEPRECATION_SUPPRESS_KEY.getKey(), false);
	}

	private AppConstants loadAppConstants(ApplicationContext applicationContext) throws IOException {
		AppConstants appConstants = AppConstants.getInstance(applicationContext != null ? applicationContext.getClassLoader() : this.getClass().getClassLoader());
		appConstants.load(getClass().getClassLoader().getResourceAsStream("AppConstants/AppConstants_ValidateAttributeRuleTest.properties"));
		return appConstants;
	}

	public static enum TestEnum {
		ONE, TWO;
	}

	public static interface InterfaceWithDefaultMethod extends INamedObject {

		default void setNaam(String naam) {
			setName(naam);
		}

		default String getNaam() {
			return getName();
		}
	}

	public static class ClassWithEnum implements INamedObject, InterfaceWithDefaultMethod {

		private @Getter @Setter String name;
		private @Getter @Setter TestEnum testEnum = TestEnum.ONE;
		private @Getter @Setter String testString = "test";
		private @Deprecated @Getter @Setter String deprecatedString;
		private @Getter String configWarningString;
		private @Getter String deprecatedConfigWarningString;
		private @Getter @Setter int testInteger = 0;
		private @Getter @Setter long testLong = 0L;
		private @Getter @Setter boolean testBoolean = false;
		private @Setter String testStringWithoutGetter = "string";
		private @Setter int testIntegerWithoutGetter = 0;
		private @Setter boolean testBooleanWithoutGetter = false;

		public void setEnumWithDifferentName(TestEnum testEnum) {
			this.testEnum = testEnum;
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

		@Protected
		public void setTestSuppressAttribute(String test) {
			testString = test;
		}
	}

	@ConfigurationWarning("warning above test class")
	public static class ConfigWarningTestClass implements INamedObject {
		private @Getter @Setter String name;
	}

	@Deprecated
	@ConfigurationWarning("warning above deprecated test class")
	public static class DeprecatedTestClass {
		private @Getter @Setter String name;
	}
}
