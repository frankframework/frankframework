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

package nl.nn.adapterframework.frankdoc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.frankdoc.model.FrankDocModel;
import nl.nn.adapterframework.frankdoc.model.FrankElement;

public class FrankDocElementSummaryFactory {
	private static final int MAX_NUM_PACKAGE_COMPONENTS_EXPECTED = 100;

	private FrankDocModel model;

	public FrankDocElementSummaryFactory(FrankDocModel model) {
		this.model = model;
	}

	public String getText() {
		Map<String, Integer> numComponentsMap = new HashMap<>();
		Map<String, List<FrankElement>> elementsBySimpleName = model.getAllElements().values().stream()
				.collect(Collectors.groupingBy(FrankElement::getSimpleName));
		for(String simpleName: elementsBySimpleName.keySet()) {
			List<String> fullNames = elementsBySimpleName.get(simpleName).stream().map(FrankElement::getFullName).collect(Collectors.toList());
			numComponentsMap.put(simpleName, getNumComponentsForUnique(fullNames));
		}
		List<SummaryElement> summaryElements = new ArrayList<>();
		for(FrankElement frankElement: model.getAllElements().values()) {
			String simpleName = frankElement.getSimpleName();
			int numComponents = numComponentsMap.get(simpleName);
			String label = getAbbreviation(frankElement.getFullName(), numComponents);
			String xmlElements = frankElement.getXmlElementNames().stream().collect(Collectors.joining(", "));
			summaryElements.add(new SummaryElement(label, xmlElements, frankElement.isAbstract()));
		}
		int maxLabelWidth = summaryElements.stream().map(se -> se.label).map(String::length).collect(Collectors.maxBy(Comparator.naturalOrder())).get();
		Map<String, SummaryElement> summaryElementsByLabel = summaryElements.stream().collect(Collectors.toMap(se -> se.label, se -> se));
		List<String> sortedLabels = new ArrayList<>(summaryElementsByLabel.keySet());
		Collections.sort(sortedLabels);
		StringBuilder b = new StringBuilder();
		for(String label: sortedLabels) {
			SummaryElement e = summaryElementsByLabel.get(label);
			if(e.isAbstract) {
				b.append(String.format("%s: (abstract)\n", StringUtils.leftPad(label, maxLabelWidth)));
			} else {
				b.append(String.format("%s: %s\n", StringUtils.leftPad(label, maxLabelWidth), e.xmlElements));
			}
		}
		return b.toString();
	}

	private int getNumComponentsForUnique(List<String> fullNames) {
		int result = 0;
		while(result < MAX_NUM_PACKAGE_COMPONENTS_EXPECTED) {
			final int numComponents = result;
			int numUnique = (int) fullNames.stream().map(n -> getAbbreviation(n, numComponents)).distinct().collect(Collectors.counting()).longValue();
			if(numUnique >= fullNames.size()) {
				return result;
			}
			++result;
		}
		throw new IllegalArgumentException(String.format("Names are not unique: %s", fullNames.stream().collect(Collectors.joining(", "))));
	}

	private String getAbbreviation(String fullName, int numPackageComponents) {
		List<String> components = Arrays.asList(fullName.split("\\."));
		Collections.reverse(components);
		int stopAt = 0;
		while(Character.isUpperCase(components.get(stopAt).charAt(0))) {
			++stopAt;
		}
		stopAt = Math.min(stopAt + numPackageComponents, components.size());
		List<String> result = new ArrayList<>(components.subList(0, stopAt));
		String head = result.get(0);
		List<String> remainder = new ArrayList<>(result.subList(1, result.size()));
		Collections.reverse(remainder);
		if(remainder.isEmpty()) {
			return head;
		} else {
			return head + String.format(" (from %s)", remainder.stream().collect(Collectors.joining(", ")));
		}
	}

	static class SummaryElement {
		String label;
		String xmlElements;
		boolean isAbstract;

		SummaryElement(String label, String xmlElements, boolean isAbstract) {
			this.label = label;
			this.xmlElements = xmlElements;
			this.isAbstract = isAbstract;
		}
	}

	
}
