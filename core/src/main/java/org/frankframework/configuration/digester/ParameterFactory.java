/*
   Copyright 2024 WeAreFrank!

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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterType;
import org.frankframework.util.EnumUtils;

/**
 * Factory for instantiating Parameters from the Digester framework.
 * Instantiates the parameter based on the type specified.
 *
 * @author Niels Meijer
 */
public class ParameterFactory extends GenericFactory {

	@Override
	public Object createObject(Map<String, String> attrs) throws ClassNotFoundException {
		String className = attrs.get("className");
		if(StringUtils.isEmpty(className) || className.equals(Parameter.class.getCanonicalName())) { //Default empty, filled when using new pre-parsing
			String type = attrs.get("type");

			className = determineClassNameFromCategory(type);
			attrs.put("className", className);
		}
		return super.createObject(attrs);
	}

	private String determineClassNameFromCategory(String typeName) {
		if(StringUtils.isEmpty(typeName)) { //StringParameter
			return Parameter.class.getCanonicalName();
		}

		ParameterType type = EnumUtils.parse(ParameterType.class, typeName);
		Class<?> clazz = type.getTypeClass();

		return clazz.getCanonicalName();
	}
}
