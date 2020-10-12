/*
   Copyright 2019, 2020 WeAreFrank!

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
package nl.nn.adapterframework.doc;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.digester.DigesterRule;
import nl.nn.adapterframework.configuration.digester.DigesterRulesHandler;
import nl.nn.adapterframework.doc.objects.ChildIbisBeanMapping;

public class IbisBeanMappingParser extends DigesterRulesHandler {
	private List<ChildIbisBeanMapping> childIbisBeanMappings = new ArrayList<>();

	@Override
	protected void handle(DigesterRule rule) {
		String pattern = rule.getPattern();
		StringTokenizer tokenizer = new StringTokenizer(pattern, "/");
		String currentIbisBeanName = null;
		while(tokenizer.hasMoreElements()) {
			String token = tokenizer.nextToken();
			if(!"*".equals(token)) {
				currentIbisBeanName = token;
			}
		}
		if(StringUtils.isNotEmpty(rule.getRegisterMethod())) {
			childIbisBeanMappings.add(getChildIbisBeanMapping(rule.getRegisterMethod(), currentIbisBeanName));
		}
	}

	public List<ChildIbisBeanMapping> getChildIbisBeanMappings() {
		return childIbisBeanMappings;
	}

	private static ChildIbisBeanMapping getChildIbisBeanMapping(String methodName, String childIbisBeanName) {
		ChildIbisBeanMapping result = new ChildIbisBeanMapping();
		result.setMaxOccurs(-1);
		result.setMethodName(methodName);
		result.setChildIbisBeanName(childIbisBeanName);
		if(methodName.startsWith("set")) {
			result.setMaxOccurs(1);
		} else if(!(methodName.startsWith("add") || methodName.startsWith("register"))) {
			throw new IllegalStateException("Unknow verb in method name: " + methodName);
		}
		return result;
	}
}
