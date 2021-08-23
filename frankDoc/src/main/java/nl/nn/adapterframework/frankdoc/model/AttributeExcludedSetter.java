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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;
import nl.nn.adapterframework.util.LogUtil;

class AttributeExcludedSetter {
	private static Logger log = LogUtil.getLogger(AttributeExcludedSetter.class);

	private Set<String> namesExcludedDueToIgnoredInterface = new HashSet<>();

	AttributeExcludedSetter(FrankClass clazz) {
		String ignoredInterface = clazz.getJavaDocTag(FrankElement.JAVADOC_IGNORE_TYPE_MEMBERSHIP);
		if(ignoredInterface != null) {
			if(StringUtils.isBlank(ignoredInterface)) {
				log.warn("Javadoc tag {} requires an argument that refers a Java interface", FrankElement.JAVADOC_IGNORE_TYPE_MEMBERSHIP);
			} else {
				log.trace("FrankElement has Javadoc tag {} that refers to interface [{}]", () -> FrankElement.JAVADOC_IGNORE_TYPE_MEMBERSHIP, () -> ignoredInterface);
				AttributesFromInterfaceRejector rejector = new AttributesFromInterfaceRejector(ignoredInterface);
				namesExcludedDueToIgnoredInterface = rejector.getRejects(clazz);
			}
			if(log.isTraceEnabled()) {
				String namesNonAttributesStr = namesExcludedDueToIgnoredInterface.stream().collect(Collectors.joining(", "));
				log.trace("The following will be excluded as attributes: {}", namesNonAttributesStr);
			}
		}
	}

	void updateAttribute(FrankAttribute attribute, FrankMethod method) {
		if(method.getJavaDocTag(FrankAttribute.JAVADOC_NO_FRANK_ATTRIBUTE) != null) {
			log.trace("Attribute [{}] has JavaDoc tag {}, marking as excluded", () -> attribute.getName(), () -> FrankAttribute.JAVADOC_NO_FRANK_ATTRIBUTE);
			attribute.setExcluded(true);
		}
		if(namesExcludedDueToIgnoredInterface.contains(attribute.getName())) {
			log.trace("Attribute [{}] is excluded because it belongs to an excluded interface", () -> attribute.getName());
			attribute.setExcluded(true);
		}
		namesExcludedDueToIgnoredInterface.remove(attribute.getName());
	}

	List<FrankAttribute> getExcludedAttributesForRemainingNames(FrankElement attributeOwner) {
		List<FrankAttribute> result = new ArrayList<>();
		for(String name: namesExcludedDueToIgnoredInterface) {
			FrankAttribute a = new FrankAttribute(name, attributeOwner);
			a.setExcluded(true);
			result.add(a);
			log.trace("Created excluded attribute [{}]", () -> a.getName());
		}
		return result;
	}
}
