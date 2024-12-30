/*
   Copyright 2020-2024 WeAreFrank!

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

import java.util.HashMap;

import org.xml.sax.SAXException;

import lombok.Getter;

/**
 * Custom implementation that replaces the old digester-rules.xml file.
 * Where previously you had to specify a 'create', 'set-properties' and a 'set-next-rule'
 * In this implementation you only have to call 'createRule(rulesBinder, PATTERN, NEXT-RULE')
 * 
 * Since the removal of the Apache Digester this class now only compiles a list of available 'rules'.
 *
 * @author Niels Meijer
 */
public class FrankDigesterRules extends AbstractDigesterRulesHandler {
	public static final String DIGESTER_RULES_FILE = "digester-rules.xml";

	@Getter
	private final HashMap<String, DigesterRule> parsedPatterns = new HashMap<>();

	@Override
	protected void handle(DigesterRule rule) throws SAXException {
		if(log.isTraceEnabled()) log.trace("adding digesterRule {}", rule.toString());

		String pattern = rule.getPattern();

		if (parsedPatterns.containsKey(pattern)) {
			// Duplicate patterns are used to tell FrankDoc parser about changed multiplicity.
			// Original method will still be available to be used by digester, so second instance of rule can be ignored here.
			log.warn("pattern [{}] already parsed", pattern);
			return;
		}

		parsedPatterns.put(pattern, rule);
	}
}