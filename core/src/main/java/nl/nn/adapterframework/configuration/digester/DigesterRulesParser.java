/*
   Copyright 2020-2021 WeAreFrank!

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
import org.springframework.context.ApplicationContext;

import lombok.Setter;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.SpringUtils;

public class DigesterRulesParser extends DigesterRulesHandler {
	private Digester digester;
	private RulesBinder rulesBinder;
	private @Setter ApplicationContext applicationContext; //Autowired ByType
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
			// Duplicate patterns are used to tell FrankDoc parser about changed multiplicity. 
			// Original method will still be available to be used by digester, so second instance of rule can be ignored here.
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
				Class<?> clazz = ClassUtils.loadClass(factory);
				object = autoWireAndInitializeBean(clazz); //Wire the factory through Spring
			} catch (Exception e) {
				throw new IllegalArgumentException("factory ["+factory+"] not found", e);
			}
			if(object instanceof ObjectCreationFactory) {
				return (ObjectCreationFactory) object;
			} 
			throw new IllegalArgumentException("factory type must implement ObjectCreationFactory");
		}
		if(log.isTraceEnabled()) log.trace("no factory specified, returing default ["+GenericFactory.class.getCanonicalName()+"]");
		return autoWireAndInitializeBean(GenericFactory.class); //Wire the factory through Spring
	}

	protected <T> T autoWireAndInitializeBean(Class<T> clazz) {
		if(applicationContext != null) {
			return SpringUtils.createBean(applicationContext, clazz); //Wire the factory through Spring
		} else {
			throw new IllegalStateException("ApplicationContext not set");
		}
	}
}
