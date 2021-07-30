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

package nl.nn.adapterframework.frankdoc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;
import nl.nn.adapterframework.frankdoc.doclet.FrankType;

/**
 * Utility methods for the Frank!Doc.
 * @author martijn
 *
 */
public final class Utils {
	private static final String JAVA_STRING = "java.lang.String";
	private static final String JAVA_INTEGER = "java.lang.Integer";
	private static final String JAVA_BOOLEAN = "java.lang.Boolean";
	private static final String JAVA_LONG = "java.lang.Long";
	private static final String JAVA_BYTE = "java.lang.Byte";
	private static final String JAVA_SHORT = "java.lang.Short";

	private static Map<String, String> primitiveToBoxed = new HashMap<>();
	static {
		primitiveToBoxed.put("int", JAVA_INTEGER);
		primitiveToBoxed.put("boolean", JAVA_BOOLEAN);
		primitiveToBoxed.put("long", JAVA_LONG);
		primitiveToBoxed.put("byte", JAVA_BYTE);
		primitiveToBoxed.put("short", JAVA_SHORT);
	}

	private static final Set<String> JAVA_BOXED = new HashSet<String>(Arrays.asList(new String[] {
			JAVA_STRING, JAVA_INTEGER, JAVA_BOOLEAN, JAVA_LONG, JAVA_BYTE, JAVA_SHORT}));

	// All types that are accepted by method isGetterOrSetter() 
	public static final Set<String> ALLOWED_SETTER_TYPES = new HashSet<>();
	static {
		ALLOWED_SETTER_TYPES.addAll(primitiveToBoxed.keySet());
		ALLOWED_SETTER_TYPES.addAll(JAVA_BOXED);
	}

	private Utils() {
	}

	public static boolean isAttributeGetterOrSetter(FrankMethod method) {
		boolean isSetter = method.getReturnType().isPrimitive()
				&& method.getReturnType().getName().equals("void")
				&& (method.getParameterTypes().length == 1)
				&& (! method.isVarargs())
				&& (method.getParameterTypes()[0].isPrimitive()
						|| JAVA_BOXED.contains(method.getParameterTypes()[0].getName()));
		boolean isGetter = (
					(method.getReturnType().isPrimitive()
							&& !method.getReturnType().getName().equals("void"))
					|| JAVA_BOXED.contains(method.getReturnType().getName())
				) && (method.getParameterTypes().length == 0);
		return isSetter || isGetter;
	}

	public static boolean isConfigChildSetter(FrankMethod method) {
		return (method.getParameterTypes().length == 1)
				&& configChildSetter(method.getName(), method.getParameterTypes()[0])
				&& (method.getReturnType().isPrimitive())
				&& (method.getReturnType().getName().equals("void"));
	}

	private static boolean configChildSetter(String methodName, FrankType parameterType) {
		boolean objectConfigChild = (! parameterType.isPrimitive())
				&& (! JAVA_BOXED.contains(parameterType.getName()));
		// A ConfigChildSetterDescriptor for a TextConfigChild should not start with "set".
		// If that would be allowed then we would have confusing with attribute setters.
		boolean textConfigChild = (! methodName.startsWith("set")) && parameterType.getName().equals(JAVA_STRING);
		return objectConfigChild || textConfigChild;
	}
	
	public static String promoteIfPrimitive(String typeName) {
		if(primitiveToBoxed.containsKey(typeName)) {
			return primitiveToBoxed.get(typeName);
		} else {
			return typeName;
		}
	}

	public static String toUpperCamelCase(String arg) {
		return arg.substring(0,  1).toUpperCase() + arg.substring(1);
	}
}
