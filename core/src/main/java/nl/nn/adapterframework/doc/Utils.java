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

package nl.nn.adapterframework.doc;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import nl.nn.adapterframework.doc.objects.SpringBean;

public final class Utils {
	private Utils() {
	}

	/**
	 * @param interfaceName The interface for which we want SpringBean objects.
	 * @return All classes implementing interfaceName, ordered by their full class name.
	 */
	public static List<SpringBean> getSpringBeans(final String interfaceName) throws ReflectiveOperationException {
		Class<?> interfaze = getClass(interfaceName);
		if(interfaze == null) {
			throw new ReflectiveOperationException("Class or interface is not available on the classpath: " + interfaceName);
		}
		if(!interfaze.isInterface()) {
			throw new ReflectiveOperationException("This exists on the classpath but is not an interface: " + interfaceName);
		}
		Set<SpringBean> unfiltered = InfoBuilderSource.getSpringBeans(interfaze);
		List<SpringBean> result = new ArrayList<SpringBean>();
		for(SpringBean b: unfiltered) {
			if(interfaze.isAssignableFrom(b.getClazz())) {
				result.add(b);
			}
		}
		return result;
	}

	public static Class<?> getClass(final String name) {
		return InfoBuilderSource.getClass(name);
	}

	public static boolean isAttributeGetterOrSetter(Method method) {
		return InfoBuilderSource.isGetterOrSetter(method);
	}

	public static boolean isConfigChildSetter(Method method) {
		return InfoBuilderSource.isConfigChildSetter(method);
	}

	public static String toUpperCamelCase(String arg) {
		return InfoBuilderSource.toUpperCamelCase(arg);
	}
}
