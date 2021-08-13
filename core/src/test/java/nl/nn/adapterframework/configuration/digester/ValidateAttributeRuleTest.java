package nl.nn.adapterframework.configuration.digester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xml.sax.Attributes;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.INamedObject;

public class ValidateAttributeRuleTest extends Mockito {
	private ClassWithEnum topBean;
	private ValidateAttributeRule rule;

	@Before
	public void setup() {
		topBean = new ClassWithEnum();
		rule = new ValidateAttributeRule() {
			@Override
			public Object getBean() {
				return topBean;
			}
		};
	}

	@Test
	public void testSimpleAttribute() throws Exception {
		Map<String, String> attr = new HashMap<>();
		attr.put("name", "my-string-value");
		attr.put("testString", "testStringValue");
		attr.put("deprecatedString", "deprecatedValue");
		attr.put("testInteger", "3");
		attr.put("testBoolean", "true");
//		attr.put("testEnum", "two");

		rule.begin(null, "ClassWithEnum", copyMapToAttrs(attr));

		assertEquals("my-string-value", topBean.getName());
		assertEquals("testStringValue", topBean.getTestString());
		assertEquals("deprecatedValue", topBean.getDeprecatedString());
		assertEquals(3, topBean.getTestInteger());
		assertEquals(true, topBean.isTestBoolean());
//		assertEquals(TestEnum.TWO, topBean.getTestEnum());
	}

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

	@Test
	public void testEnumGetterSetter() throws Exception {
		PropertyDescriptor pd = PropertyUtils.getPropertyDescriptor(topBean, "testEnum");
		Method writeMethod = pd.getWriteMethod();
		assertNotNull(writeMethod);
		assertEquals("TestEnum", writeMethod.getParameters()[0].getType().getSimpleName());

		Method readMethod = pd.getReadMethod();
		assertNotNull(readMethod);
		assertEquals("TestEnum", readMethod.getReturnType().getSimpleName());
	}

	public static class ClassWithEnum implements INamedObject {
		public enum TestEnum {
			ONE, TWO;
		}
		private @Getter @Setter String name;
		private @Getter @Setter TestEnum testEnum = TestEnum.ONE;
		private @Getter @Setter String testString;
		private @Deprecated @Getter @Setter String deprecatedString;
		private @Getter @Setter int testInteger;
		private @Getter @Setter boolean testBoolean = false;
	}
}
