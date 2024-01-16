/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.core;

import java.util.HashMap;
import java.util.Map;

import javax.wsdl.Definition;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.functional.ThrowingFunction;

/**
 * This exists because WSDL Definitions can grow dramatically in size when they include a few xsd's.
 */
public class SharedWsdlDefinitions {

	private final Map<String, Definition> resources = new HashMap<>();

	public synchronized Definition getOrCompute(String name, ThrowingFunction<String, Definition, ConfigurationException> creator) throws ConfigurationException {
		Definition result = resources.get(name);
		if (result==null) {
			result = creator.apply(name);
			resources.put(name, result);
		}
		return result;
	}

}
