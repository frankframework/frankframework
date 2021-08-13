package nl.nn.adapterframework.configuration.digester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.junit.Test;
import org.xml.sax.Attributes;

import lombok.Getter;
import lombok.Setter;

public class ValidateAttributeRuleTest {
	private ClassWithEnum topBean = new ClassWithEnum();

	@Test
	public void testSimpleRule() {
		ValidateAttributeRule rule = new ValidateAttributeRule() {
			@Override
			protected Object getBean() {
				return topBean;
			}
		};
		Map<String, String> attr = new HashMap<>();
		rule.begin(null, "ClassWithEnum", copyMapToAttrs(attr));
	}

	private Map<String, String> copyMapToAttrs(Attributes attrs) {
		Map<String, String> map = new HashMap<>(attrs.getLength());
		for (int i = 0; i < attrs.getLength(); ++i) {
			String name = attrs.getLocalName(i);
			if ("".equals(name)) {
				name = attrs.getQName(i);
			}
			if(name != null && !name.equals("className")) {
				String value = attrs.getValue(i);
				map.put(name, value);
			}
		}
		return map;
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

	private static class ClassWithEnum {
		public enum TestEnum {
			ONE, TWO;
		}
		private @Getter @Setter TestEnum testEnum;
	}
}
