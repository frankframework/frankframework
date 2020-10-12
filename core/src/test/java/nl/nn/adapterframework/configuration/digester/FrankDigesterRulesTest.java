package nl.nn.adapterframework.configuration.digester;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.ObjectCreationFactory;
import org.apache.commons.digester3.binder.LinkedRuleBuilder;
import org.apache.commons.digester3.binder.RulesBinder;
import org.apache.commons.digester3.binder.RulesModule;
import org.junit.Test;
import org.mockito.Mockito;

public class FrankDigesterRulesTest extends Mockito {
	private class DummyRulesBinder implements RulesBinder {

		@Override
		public void install(RulesModule rulesModule) {
			//Package private rulesModule which we cannot implement
		}

		@Override
		public ClassLoader getContextClassLoader() {
			return null; //We don't need/use the classloader?
		}

		@Override
		public LinkedRuleBuilder forPattern(String pattern) {
			return null; //Since this is a package private final we cant stub it...
		}

		@Override
		public void addError(String messagePattern, Object... arguments) {
			//Ignore parse errors
		}

		@Override
		public void addError(Throwable t) {
			//Ignore parse errors
		}
	};

	@Test
	public void parseDigesterRulesXml() {
		Digester digester = new Digester();
		List<String> patterns = new ArrayList<>();
		FrankDigesterRules digesterRules = new FrankDigesterRules(digester) {
			@Override
			protected void createRule(RulesBinder rulesBinder, String pattern, String clazz, ObjectCreationFactory<Object> factory, String next, Class<?> parameterType) {
				patterns.add(pattern);
			}
		};

		digesterRules.configure(mock(DummyRulesBinder.class));

		assertTrue("must at least have 33 patterns", patterns.size() >= 33);
	}
}
