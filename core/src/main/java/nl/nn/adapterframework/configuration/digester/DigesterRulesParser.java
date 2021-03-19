package nl.nn.adapterframework.configuration.digester;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.ObjectCreateRule;
import org.apache.commons.digester3.ObjectCreationFactory;
import org.apache.commons.digester3.Rule;
import org.apache.commons.digester3.binder.LinkedRuleBuilder;
import org.apache.commons.digester3.binder.RulesBinder;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.AttributeCheckingRule;
import nl.nn.adapterframework.configuration.GenericFactory;
import nl.nn.adapterframework.util.ClassUtils;

public class DigesterRulesParser extends DigesterRulesHandler {
	private Digester digester;
	private RulesBinder rulesBinder;
	private Rule attributeChecker = new AttributeCheckingRule();
	private Set<String> parsedPatterns = new HashSet<String>();

	public DigesterRulesParser(Digester digester, RulesBinder rulesBinder) {
		this.digester = digester;
		this.rulesBinder = rulesBinder;
	}

	@Override
	protected void handle(DigesterRule rule) {
		if(log.isTraceEnabled()) log.trace("adding digesterRule " + rule.toString());
		
		String pattern = rule.getPattern();

		if (parsedPatterns.contains(pattern)) {
			log.warn("pattern [{}] already parsed", pattern);
			return;
		}
		parsedPatterns.add(pattern);
		
		LinkedRuleBuilder ruleBuilder = rulesBinder.forPattern(pattern);
		if(StringUtils.isNotEmpty(rule.getObject())) { //If a class is specified, load the class through the digester create-object-rule
//			ruleBuilder.createObject().ofTypeSpecifiedByAttribute(rule.getObject()); //Can't use 'ruleBuilder' as this tries to load the class at configure time and not runtime
			ruleBuilder.addRule(new ObjectCreateRule(rule.getObject()));
		} else {
			ObjectCreationFactory<Object> factory = getFactory(rule.getFactory());
			if(factory != null) {
				factory.setDigester(digester); //When using a custom factory you have to inject the digester manually... Sigh
				ruleBuilder.factoryCreate().usingFactory(factory); //If a factory is specified, use the factory to create the object
			}
		}
		ruleBuilder.setProperties(); //set the set-properties-rule
		if(rule.getRegisterMethod() != null) { //set the register method (set-next-rule)
			ruleBuilder.setNext(rule.getRegisterMethod());
		}
		if(rule.getSelfRegisterMethod() != null) { //set the register method (set-top-rule)
			ruleBuilder.setTop(rule.getSelfRegisterMethod());
		}
		ruleBuilder.addRule(attributeChecker); //Add the attribute checker
	}

	/**
	 * Return the specified factory or the default factory when empty.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ObjectCreationFactory<Object> getFactory(String factory) {
		if("null".equals(factory)) { //Check against a null-string when you don't want to use the default factory
			if(log.isTraceEnabled()) log.trace("NULL factory specified, skip factory registration");
			return null;
		} else if(StringUtils.isNotEmpty(factory)) {
			Object object;
			try {
				if(log.isTraceEnabled()) log.trace("attempting to create new factory of class ["+factory+"]");
				object = ClassUtils.newInstance(factory);
			} catch (Exception e) {
				throw new IllegalArgumentException("factory ["+factory+"] not found", e);
			}
			if(object instanceof ObjectCreationFactory) {
				return (ObjectCreationFactory) object;
			} else {
				throw new IllegalArgumentException("factory type must implement ObjectCreationFactory");
			}
		}
		if(log.isTraceEnabled()) log.trace("no factory specified, returing default ["+GenericFactory.class.getCanonicalName()+"]");
		return new GenericFactory();
	}
}
