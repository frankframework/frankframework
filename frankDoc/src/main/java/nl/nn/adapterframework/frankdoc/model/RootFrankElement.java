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

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;

/**
 * A FrankElement that can appear as the root element in a Frank configuration. Such elements are not
 * part of a config child, but they have a role name that matches rules in digester-rules.xml.
 */
class RootFrankElement extends FrankElement {
	RootFrankElement(FrankClass clazz) {
		super(clazz);
	}

	public String getRoleName() {
		return getSimpleName().toLowerCase();
	}
}
