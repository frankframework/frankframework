package nl.nn.adapterframework.configuration.digester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.Attributes;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.doc.ProtectedAttribute;
import nl.nn.adapterframework.testutil.TestConfiguration;

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
		attr.put("testBoolean", "true");
		attr.put("testEnum", "two");

		ClassWithEnum bean = runRule(ClassWithEnum.class, attr);

		assertEquals("my-string-value", bean.getName());
		assertEquals("testStringValue", bean.getTestString());
		assertEquals("deprecatedValue", bean.getDeprecatedString());
		assertEquals(3, bean.getTestInteger());
		assertEquals(true, bean.isTestBoolean());
		assertEquals(TestEnum.TWO, bean.getTestEnum());
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
		Map<String, String> attr = new LinkedHashMap<>();
		attr.put("testString", "test");
		attr.put("testInteger", "0");
		attr.put("testBoolean", "false");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(3, configWarnings.size());
		assertEquals("ClassWithEnum attribute [testString] already has a default value [test]", configWarnings.get(0));
		assertEquals("ClassWithEnum attribute [testInteger] already has a default value [0]", configWarnings.get(1));
		assertEquals("ClassWithEnum attribute [testBoolean] already has a default value [false]", configWarnings.get(2));
	}

	@Test
	public void testAttributeWithNumberFormatException() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("testInteger", "a String");

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [testInteger] with value [a String] cannot be converted to a number: For input string: \"a String\"", configWarnings.get(0));
	}

	@Test
	public void testAttributeWithoutGetterNumberFormatException() throws Exception {
		Map<String, String> attr = new HashMap<>();

		attr.put("testStringWithoutGetter", "a String"); //Should work
		attr.put("testIntegerWithoutGetter", "a String"); //Throws Exception

		runRule(ClassWithEnum.class, attr);

		ConfigurationWarnings configWarnings = configuration.getConfigurationWarnings();
		assertEquals(1, configWarnings.size());
		assertEquals("ClassWithEnum attribute [testIntegerWithoutGetter] with value [a String] cannot be converted to a number", configWarnings.get(0));
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

	public enum TestEnum {
		ONE, TWO;
	}

	public static class ClassWithEnum implements INamedObject {

		private @Getter @Setter String name;
		private @Getter @Setter TestEnum testEnum = TestEnum.ONE;
		private @Getter @Setter String testString = "test";
		private @Deprecated @Getter @Setter String deprecatedString;
		private @Getter String configWarningString;
		private @Getter String deprecatedConfigWarningString;
		private @Getter @Setter int testInteger = 0;
		private @Getter @Setter boolean testBoolean = false;
		private @Setter String testStringWithoutGetter = "string";
		private @Setter int testIntegerWithoutGetter = 0;
		private @Setter boolean testBooleanWithoutGetter = false;

		@ConfigurationWarning("my test warning")
		public void setConfigWarningString(String str) {
			configWarningString = str;
		}

		@ConfigurationWarning("my deprecated test warning")
		@Deprecated
		public void setDeprecatedConfigWarningString(String str) {
			deprecatedConfigWarningString = str;
		}

		@ProtectedAttribute
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
