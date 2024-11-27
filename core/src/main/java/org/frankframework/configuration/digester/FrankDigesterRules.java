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
package org.frankframework.configuration.digester;

import java.io.IOException;

import org.apache.commons.digester3.Digester;
import org.apache.commons.digester3.binder.RulesBinder;
import org.apache.commons.digester3.binder.RulesModule;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.SAXException;

import lombok.Setter;

import org.frankframework.core.Resource;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.XmlUtils;

/**
 * Custom implementation that replaces the old digester-rules.xml file.
 * Where previously you had to specify a 'create', 'set-properties' and a 'set-next-rule'
 * In this implementation you only have to call 'createRule(rulesBinder, PATTERN, NEXT-RULE')
 *
 * @author Niels Meijer
 */
public class FrankDigesterRules implements RulesModule, ApplicationContextAware {
	public static final String DIGESTER_RULES_FILE = "digester-rules.xml";
	private @Setter ApplicationContext applicationContext;

	private Digester digester;
	private Resource digesterRules = null;

	/**
	 * We need to parse the Digester in case a factory create rule is used
	 */
	public FrankDigesterRules(Digester digester) {
		this(digester, null);
	}

	public FrankDigesterRules(Digester digester, Resource digesterRules) {
		this.digester = digester;
		this.digesterRules = digesterRules;
	}

	@Override
	public void configure(RulesBinder rulesBinder) {
		if(applicationContext == null) {
			throw new IllegalStateException("ApplicationContext not set, unable to initialize ["+ClassUtils.nameOf(this)+"]");
		}
		if(digester == null) {
			throw new IllegalStateException("Digester not set, unable to initialize ["+ClassUtils.nameOf(this)+"]");
		}
		if(digesterRules == null) {
			digesterRules = Resource.getResource(DIGESTER_RULES_FILE);
		}

		AbstractDigesterRulesHandler handler = new DigesterRulesParser(digester, rulesBinder);
		SpringUtils.autowireByType(applicationContext, handler);
		try {
			XmlUtils.parseXml(digesterRules.asInputSource(), handler);
		} catch (IOException e) {
			throw new IllegalStateException("unable to open digesterRules file", e);
		} catch (SAXException e) {
			throw new IllegalStateException("unable to parse digesterRules file", e);
		}
	}
}
