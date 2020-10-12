/*
   Copyright 2020 WeAreFrank!

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

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.ObjectCreationFactory;
import org.apache.commons.digester3.Rule;
import org.apache.commons.digester3.binder.LinkedRuleBuilder;
import org.apache.commons.digester3.binder.RulesBinder;
import org.apache.commons.digester3.binder.RulesModule;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.AttributeCheckingRule;
import nl.nn.adapterframework.configuration.GenericFactory;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.NonResolvingExternalEntityResolver;

/**
 * Custom implementation that replaces the old digester-rules.xml file.
 * Where previously you had to specify a 'create', 'set-properties' and a 'set-next-rule'
 * In this implementation you only have to call 'createRule(rulesBinder, PATTERN, NEXT-RULE')
 *
 */
public class FrankDigesterRules implements RulesModule {
	public static final String DIGESTER_RULES_FILE = "digester-rules.xml";

	private final Logger log = LogUtil.getLogger(this);
	private GenericFactory factory = new GenericFactory();
	private Rule attributeChecker = new AttributeCheckingRule();
	private Digester digester;
	private Resource digesterRules = null;

	public FrankDigesterRules(Digester digester) {
		this(digester, null);
	}

	public FrankDigesterRules(Digester digester, Resource digesterRules) {
		this.digester = digester;
		this.digesterRules = digesterRules;
	}

	@Override
	public void configure(RulesBinder rulesBinder) {
		if(digester == null) {
			throw new IllegalStateException("Digester not set, unable to initialize ["+this.getClass().getSimpleName()+"]");
		}
		if(digesterRules == null) {
			digesterRules = Resource.getResource(digester.getClassLoader(), DIGESTER_RULES_FILE);
		}

		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(DigesterRule.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			DocumentBuilderFactory documentFactory = XmlUtils.getDocumentBuilderFactory();
			documentFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			DocumentBuilder builder = documentFactory.newDocumentBuilder();
			builder.setEntityResolver(new NonResolvingExternalEntityResolver());

			Document document = builder.parse(digesterRules.asInputSource());
			Node rules = document.getFirstChild(); //get the <digester-rules/> tag
			for(Node ruleNode : XmlUtils.getChildTags((Element)rules, "rule")) {
				DigesterRule rule = (DigesterRule) jaxbUnmarshaller.unmarshal(ruleNode);
				createRule(rulesBinder, rule.getPattern(), rule.getObject(), rule.getFactory(), rule.getNext(), rule.getParameterType());
			}
		} catch (JAXBException e) {
			throw new IllegalStateException("unable to unmarshal a xml node", e);
		} catch (IOException e) {
			throw new IllegalStateException("digesterRules file cannot be found", e);
		} catch (ParserConfigurationException e) {
			throw new IllegalStateException("documentBuilder cannot be initialized", e);
		} catch (SAXException e) {
			throw new IllegalStateException("unable to parse digesterRules file", e);
		}
	}

	/**
	 * Create a generic parser to create the object, set the properties (set-properties-rule), 
	 * set the register method (set-next-rule) add the attributeCheckerRule on a per pattern basis.
	 */
	protected void createRule(RulesBinder rulesBinder, String pattern, String clazz, ObjectCreationFactory<Object> factory, String next, Class<?> parameterType) {
		if(log.isTraceEnabled()) log.trace(String.format("adding digesterRule pattern [%s] class [%s] factory [%s] next-rule [%s] parameterType [%s]", pattern, clazz, factory, next, parameterType));

		LinkedRuleBuilder ruleBuilder = rulesBinder.forPattern(pattern);
		if(clazz != null) { //If a class is specified, load the class through the digester create-object-rule
			ruleBuilder.createObject().ofType(clazz);
		} else {
			if(factory == null) {
				factory = this.factory;
			}
			factory.setDigester(digester); //When using a custom factory you have to inject the digester manually... Sigh
			ruleBuilder.factoryCreate().usingFactory(factory); //If a factory is specified, use the factory to create the object
		}
		ruleBuilder.setProperties(); //set the set-properties-rule
		if(next != null) { //set the register method (set-next-rule)
			if(parameterType != null) {
				ruleBuilder.setNext(next).withParameterType(parameterType);
			} else {
				ruleBuilder.setNext(next);
			}
		}
		ruleBuilder.addRule(attributeChecker); //Add the attribute checker
	}
}
