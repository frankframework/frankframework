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

package nl.nn.adapterframework.frankdoc.doclet;

import java.util.Set;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

class FrankClassRepositoryReflect implements FrankClassRepository {
	private @Getter @Setter(AccessLevel.PACKAGE) Set<String> excludeFilters;
	private @Getter @Setter(AccessLevel.PACKAGE) Set<String> includeFilters;
	private @Getter @Setter(AccessLevel.PACKAGE) Set<String> excludeFiltersForSuperclass;

	@Override
	public FrankClass findClass(String fullName) throws FrankDocException {
		try {
			return new FrankClassReflect(Class.forName(fullName), this);
		} catch(ClassNotFoundException e) {
			String outerClassName = getOuterClassName(fullName);
			String simpleName = getSimpleName(fullName);
			String expectedInnerClassName = outerClassName + "$" + simpleName;
			try {
				Class<?> outerClazz = Class.forName(outerClassName);
				Class<?>[] innerClasses = outerClazz.getDeclaredClasses();
				for(Class<?> innerClass: innerClasses) {
					if(innerClass.getName().equals(expectedInnerClassName)) {
						return new FrankClassReflect(innerClass, this);
					}
				}
				throw new FrankDocException(String.format("Found outer class [%s] but it does not have inner class [%s]", outerClazz.getName(), simpleName), e);
			} catch(ClassNotFoundException innerException) {
				throw new FrankDocException(String.format("Could not find class [%s]", fullName), innerException);
			}
		}
	}

	private String getOuterClassName(String innerClassFullName) {
		return innerClassFullName.substring(0, innerClassFullName.lastIndexOf("."));
	}

	private static String getSimpleName(String innerClassFullName) {
		return innerClassFullName.substring(innerClassFullName.lastIndexOf(".") + 1);
	}
}
