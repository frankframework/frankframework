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

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = false)
class ConfigChildKey extends ElementChild.AbstractKey {
	private final @Getter String roleName;
	private final @Getter ElementType elementType;

	public ConfigChildKey(String roleName, ElementType elementType) {
		this.roleName = roleName;
		this.elementType = elementType;
	}

	@Override
	public String toString() {
		return "(roleName=" + roleName + ", elementType=" + elementType.getFullName() + ")";
	}
}
