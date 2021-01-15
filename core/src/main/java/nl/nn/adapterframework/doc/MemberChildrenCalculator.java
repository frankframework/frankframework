/* 
Copyright 2020, 2021 WeAreFrank! 

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.doc.model.ElementRole;
import nl.nn.adapterframework.doc.model.ElementType;
import nl.nn.adapterframework.doc.model.FrankDocModel;
import nl.nn.adapterframework.util.LogUtil;

class MemberChildrenCalculator {
	private static Logger log = LogUtil.getLogger(MemberChildrenCalculator.class);

	private ElementRole subject;
	private FrankDocModel model;
	private XmlSchemaVersionImpl version;

	MemberChildrenCalculator(ElementRole subject, FrankDocModel model, XmlSchemaVersionImpl version) {
		this.subject = subject;
		this.model = model;
		this.version = version;
	}

	List<ElementRole> getMemberChildOptions() {
		Map<String, List<ElementRole>> childRolesBySyntax1Name = groupElementRolesBySyntax1Name(
				model.getElementTypeMemberChildRoles(
						subject.getElementType(), version.childSelector(), version.childRejector(), version.frankElementFilter()));
		return disambiguateElementRoles(childRolesBySyntax1Name);
	}

	private Map<String, List<ElementRole>> groupElementRolesBySyntax1Name(List<ElementRole> elementRoles) {
		// This is implemented such that the sort order remains deterministic
		Map<String, List<ElementRole>> result = new TreeMap<>();
		for(ElementRole role: elementRoles) {
			result.merge(role.getSyntax1Name(), Arrays.asList(role),
					(l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toList()));
		}
		return result;
	}

	private List<ElementRole> disambiguateElementRoles(Map<String, List<ElementRole>> bySyntax1Name) {
		List<ElementRole> result = new ArrayList<>();
		for(String syntax1Name: bySyntax1Name.keySet()) {
			List<ElementRole> bucket = bySyntax1Name.get(syntax1Name);
			if(bucket.size() >= 2) {
				if(log.isTraceEnabled()) {
					log.trace(String.format("Multiple element roles for syntax 1 name [%]: [%s]",
							syntax1Name, ElementRole.collection2String(bucket)));
				}
				List<ElementRole> combination = disambiguateElementRoles(bucket);
				result.addAll(combination);
				if(log.isTraceEnabled()) {
					log.trace(String.format("Combined into the following roles: [%s]", ElementRole.collection2String(combination)));
				}
			}
			else {
				result.add(bucket.get(0));					
			}
		}
		return result;
	}

	private List<ElementRole> disambiguateElementRoles(List<ElementRole> bucket) {
		Map<ElementRole.Key, List<ElementRole>> byHighestCommonInterface = bucket.stream()
				.collect(Collectors.groupingBy(this::getHighestCommonInterface));
		List<ElementRole> result = new ArrayList<>();
		for(ElementRole.Key key: byHighestCommonInterface.keySet()) {
			ElementRole highestCommonInterface = model.findElementRole(key);
			if(highestCommonInterface == null) {
				result.addAll(byHighestCommonInterface.get(key));
			} else {
				result.add(highestCommonInterface);
			}
		}
		return result;
	}

	private ElementRole.Key getHighestCommonInterface(ElementRole role) {
		ElementType highestCommonInterface = role.getElementType().getHighestCommonInterface();
		return new ElementRole.Key(highestCommonInterface.getFullName(), role.getSyntax1Name());
	}
}
