/* 
Copyright 2021 WeAreFrank! 

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

package nl.nn.adapterframework.frankdoc.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;

public enum AttributeType {
	STRING(new TypeCheckerString()),
	BOOL(new TypeCheckerBool()),
	INT(new TypeCheckerInt());

	private static final Map<String, AttributeType> JAVA_TO_TYPE = new HashMap<>();
	static {
		JAVA_TO_TYPE.put("int", INT);
		JAVA_TO_TYPE.put("boolean", BOOL);
		JAVA_TO_TYPE.put("long", INT);
		JAVA_TO_TYPE.put("byte", INT);
		JAVA_TO_TYPE.put("short", INT);
		JAVA_TO_TYPE.put("java.lang.String", STRING);
		JAVA_TO_TYPE.put("java.lang.Integer", INT);
		JAVA_TO_TYPE.put("java.lang.Boolean", BOOL);
		JAVA_TO_TYPE.put("java.lang.Long", INT);
		JAVA_TO_TYPE.put("java.lang.Byte", INT);
		JAVA_TO_TYPE.put("java.lang.Short", INT);
	}

	/**
	 * @throws IllegalArgumentException when the provided Java type is not String, boolean or int or its boxed equivalent.
	 */
	static AttributeType fromJavaType(String javaType) {
		AttributeType result = JAVA_TO_TYPE.get(javaType);
		if(result == null) {
			throw new IllegalArgumentException(String.format("Java type is not one of %s: %s",
					Arrays.asList(AttributeType.values()).stream().map(Enum::name).collect(Collectors.joining(", ")), javaType));
		}
		return result;
	}

	private final TypeChecker typeChecker;

	private AttributeType(TypeChecker typeChecker) {
		this.typeChecker = typeChecker;
	}

	private static abstract class TypeChecker {
		abstract void typeCheck(String value) throws FrankDocException;
	}

	void typeCheck(String value) throws FrankDocException {
		typeChecker.typeCheck(value);
	}

	private static class TypeCheckerString extends TypeChecker {
		@Override
		void typeCheck(String value) throws FrankDocException {
		}
	}

	private static class TypeCheckerInt extends TypeChecker {
		@Override
		void typeCheck(String value) throws FrankDocException {
			try {
				Integer.parseInt(value);
			} catch(Exception e) {
				throw new FrankDocException(String.format("Value [%s] is not integer", value), e);
			}
		}		
	}

	private static final HashSet<String> BOOLEANS = new HashSet<>(Arrays.asList("false", "true"));

	private static class TypeCheckerBool extends TypeChecker {
		@Override
		void typeCheck(String value) throws FrankDocException {
			if(! BOOLEANS.contains(value)) {
				throw new FrankDocException(String.format("Value [%s] is not Boolean", value), null);
			}
		}
	}
}
