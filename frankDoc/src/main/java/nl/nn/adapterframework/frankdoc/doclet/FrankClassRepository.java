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

import com.sun.javadoc.ClassDoc;

public interface FrankClassRepository {
	FrankClass findClass(String fullName) throws FrankDocException;

	public static FrankClassRepository getDocletInstance(
			ClassDoc[] classDocs, Set<String> includeFilters, Set<String> excludeFilters, Set<String> excludeFiltersForSuperclass) {
		return new FrankClassRepositoryDoclet(classDocs, includeFilters, excludeFilters, excludeFiltersForSuperclass);
	}

	/**
	 * Removes a trailing dot from a package name. It is handy to refer to class names like
	 * <code>PACKAGE + simpleName</code> but for this to work the variable <code>PACKAGE</code>
	 * should end with a dot. This function removes that dot to arrive at a plain package name
	 * again.
	 */
	static String removeTrailingDot(String s) {
		if(s.endsWith(".")) {
			return s.substring(0, s.length() - 1);
		} else {
			return s;
		}
	}
}
