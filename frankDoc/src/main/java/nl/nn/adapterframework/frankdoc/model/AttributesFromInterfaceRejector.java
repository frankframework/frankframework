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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;

class AttributesFromInterfaceRejector extends AbstractInterfaceRejector {
	AttributesFromInterfaceRejector(String rejectedInterface) {
		super(new HashSet<String>(Arrays.asList(rejectedInterface)));
	}

	@Override
	Set<String> getAllItems(FrankClass clazz) {
		Map<String, FrankMethod> attributesByName = FrankDocModel.getAttributeToMethodMap(clazz.getDeclaredMethods(), "set");
		return new HashSet<>(attributesByName.keySet());
	}
}
