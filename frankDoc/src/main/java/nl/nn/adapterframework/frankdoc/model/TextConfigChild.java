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

import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;

public class TextConfigChild extends ConfigChild {
	private final String roleName;

	TextConfigChild(FrankElement owningElement, FrankMethod method, String roleName) {
		super(owningElement, method);
		this.roleName = roleName;
	}

	@Override
	public ConfigChildKey getKey() {
		return new ConfigChildKey(roleName, null);
	}

	// Avoid complicated Lombok syntax to add the @Override tag. Just
	// coding the getter is simpler in this case.
	@Override
	public String getRoleName() {
		return roleName;
	}

	@Override
	public String toString() {
		return String.format("%s(roleName = %s)", getClass().getSimpleName(), roleName);
	}
}
