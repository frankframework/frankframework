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

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.ObjectCreationFactory;
import org.apache.commons.digester3.Rule;
import org.apache.commons.digester3.binder.LinkedRuleBuilder;
import org.apache.commons.digester3.binder.RulesBinder;
import org.apache.commons.digester3.binder.RulesModule;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.AttributeCheckingRule;
import nl.nn.adapterframework.configuration.ConfigurationDigesterFactory;
import nl.nn.adapterframework.configuration.GenericFactory;
import nl.nn.adapterframework.configuration.JmsRealmsFactory;
import nl.nn.adapterframework.configuration.ListenerFactory;
import nl.nn.adapterframework.configuration.RecordHandlingFlowFactory;
import nl.nn.adapterframework.util.LogUtil;

public class FrankDigesterRules implements RulesModule {

	private final Logger log = LogUtil.getLogger(this);
	private GenericFactory factory = new GenericFactory();
	private Rule attributeChecker = new AttributeCheckingRule();
	private Digester digester;

	public FrankDigesterRules(Digester digester) {
		this.digester = digester;
	}

	@Override
	public void configure(RulesBinder rulesBinder) {
		rulesBinder.forPattern("IOS-Adaptering");
		rulesBinder.forPattern("configuration");

		createRule(rulesBinder, "*/include", new ConfigurationDigesterFactory(), "include");

		createRule(rulesBinder, "*/jmsRealms", new JmsRealmsFactory());
		createRule(rulesBinder, "*/jmsRealm", "registerJmsRealm");

		createRule(rulesBinder, "*/sapSystem", "nl.nn.adapterframework.extensions.sap.SapSystem", "registerItem");

		createRule(rulesBinder, "*/adapter", "registerAdapter");
		createRule(rulesBinder, "*/pipeline", "registerPipeLine");
		createRule(rulesBinder, "*/errorMessageFormatter", "setErrorMessageFormatter");

		createRule(rulesBinder, "*/receiver", "registerReceiver");
		createRule(rulesBinder, "*/sender", "setSender");
		createRule(rulesBinder, "*/postboxSender", "setSender");
		createRule(rulesBinder, "*/listener", new ListenerFactory(), "setListener");
		createRule(rulesBinder, "*/postboxListener", "setListener");
		createRule(rulesBinder, "*/errorSender", "setErrorSender");
		createRule(rulesBinder, "*/messageLog", "setMessageLog");
		createRule(rulesBinder, "*/inProcessStorage", "setInProcessStorage");
		createRule(rulesBinder, "*/errorStorage", "setErrorStorage");
		createRule(rulesBinder, "*/inputValidator", "setInputValidator");
		createRule(rulesBinder, "*/outputValidator", "setOutputValidator");
		createRule(rulesBinder, "*/inputWrapper", "setInputWrapper");
		createRule(rulesBinder, "*/outputWrapper", "setOutputWrapper");

		createRule(rulesBinder, "*/pipe", "addPipe");
		createRule(rulesBinder, "*/forward", "registerForward");
		createRule(rulesBinder, "*/child", "registerChild");
		createRule(rulesBinder, "*/pipeline/exits/exit", "registerPipeLineExit");

		createRule(rulesBinder, "*/scheduler/job", "registerScheduledJob");
		createRule(rulesBinder, "*/locker", "setLocker");
		createRule(rulesBinder, "*/param", "addParameter");
		createRule(rulesBinder, "*/directoryCleaner", "addDirectoryCleaner");
		createRule(rulesBinder, "*/readerFactory", "setReaderFactory");

		createRule(rulesBinder, "*/manager", "registerManager");
		createRule(rulesBinder, "*/manager/flow", new RecordHandlingFlowFactory(), "addHandler");
		createRule(rulesBinder, "*/recordHandler", "registerRecordHandler");
		createRule(rulesBinder, "*/resultHandler", "registerResultHandler");

		createRule(rulesBinder, "*/statisticsHandlers", "registerStatisticsHandler");
		createRule(rulesBinder, "*/statisticsHandler", "registerStatisticsHandler");
		createRule(rulesBinder, "*/cache", "registerCache");
	}

	private LinkedRuleBuilder createRule(RulesBinder rulesBinder, String pattern, String next) {
		return createRule(rulesBinder, pattern, null, factory, next, null);
	}
	//This is here to be able to load classes (through reflection) that are not in the core module
	private LinkedRuleBuilder createRule(RulesBinder rulesBinder, String pattern, String objectClassName, String next) {
		try {
			Class<?> clazz = this.getClass().getClassLoader().loadClass(objectClassName);
			return createRule(rulesBinder, pattern, clazz, next);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("class ["+objectClassName+"] not found", e);
		}
	}
	private LinkedRuleBuilder createRule(RulesBinder rulesBinder, String pattern, Class<?> clazz, String next) {
		return createRule(rulesBinder, pattern, clazz, null, next, null);
	}
	private LinkedRuleBuilder createRule(RulesBinder rulesBinder, String pattern, ObjectCreationFactory<Object> factory) {
		return createRule(rulesBinder, pattern, factory, null);
	}
	private LinkedRuleBuilder createRule(RulesBinder rulesBinder, String pattern, ObjectCreationFactory<Object> factory, String next) {
		return createRule(rulesBinder, pattern, null, factory, next, null);
	}

	/**
	 * Create a generic parser to create the object, set the properties, add the attributeChecker on a per pattern basis.
	 */
	private LinkedRuleBuilder createRule(RulesBinder rulesBinder, String pattern, Class<?> clazz, ObjectCreationFactory<Object> factory, String next, Class<?> parameterType) {
		if(log.isTraceEnabled()) log.trace(String.format("adding digesterRule pattern [%s] class [%s] factory [%s] next-rule [%s] parameterType [%s]", pattern, clazz, factory, next, parameterType));

		LinkedRuleBuilder ruleBuilder = rulesBinder.forPattern(pattern);
		if(clazz != null) {
			ruleBuilder.createObject().ofType(clazz);
		} else {
			factory.setDigester(digester); //When using a custom factory you have to inject the digester manually... Sigh
			ruleBuilder.factoryCreate().usingFactory(factory);
		}
		ruleBuilder.setProperties();
		if(next != null) {
			if(parameterType != null) {
				ruleBuilder.setNext(next).withParameterType(parameterType);
			} else {
				ruleBuilder.setNext(next);
			}
		}
		ruleBuilder.addRule(attributeChecker);
		return ruleBuilder;
	}
}
