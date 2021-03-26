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

class FrankClassRepositoryReflect implements FrankClassRepository {
	private Set<String> excludeFilters;
	private String[] includeFilter;

	@Override
	public void setExcludeFilters(Set<String> excludeFilters) {
		this.excludeFilters = excludeFilters;
	}

	@Override
	public Set<String> getExcludeFilters() {
		return excludeFilters;
	}

	@Override
	public void setIncludeFilters(String ...items) {
		includeFilter = items;
	}

	@Override
	public String[] getIncludeFilter() {
		return includeFilter;
	}


	@Override
	public FrankClass findClass(String fullName) throws FrankDocException {
		try {
			return new FrankClassReflect(Class.forName(fullName), this);
		} catch(ClassNotFoundException e) {
			throw new FrankDocException(String.format("Could not find class [%s]", fullName), e);
		}
	}
}
