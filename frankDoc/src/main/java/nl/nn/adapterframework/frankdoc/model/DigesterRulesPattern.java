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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;

class DigesterRulesPattern {
	private final String originalPattern;
	private List<String> components;
	private @Getter boolean root = false;
	private @Getter Matcher matcher;

	DigesterRulesPattern(String pattern) throws SAXException {
		this.originalPattern = pattern;
		boolean matchesOnlyRoot = false;
		if(StringUtils.isBlank(pattern)) {
			throw new SAXException(String.format("digester-rules.xml: Pattern cannot be null and cannot be blank"));
		}
		components = Arrays.asList(pattern.split("/"));
		if(components.isEmpty()) {
			throw new SAXException(String.format("digester-rules.xml should not contain an empty pattern. The literal value was [%s]", originalPattern));
		}
		List<String> componentsThatShouldNotBeWildcard = components;
		if(components.get(0).equals("*")) {
			componentsThatShouldNotBeWildcard = components.subList(1, components.size());
		} else {
			matchesOnlyRoot = true;
			if(components.size() == 1) {
				root = true;
			}
		}
		if(componentsThatShouldNotBeWildcard.isEmpty()) {
			throw new SAXException(String.format("digester-rules.xml: A pattern that is only a wildcard is invalid. Encountered [%s]", originalPattern));
		}
		if(componentsThatShouldNotBeWildcard.stream().anyMatch(s -> s.equals("*"))) {
			throw new SAXException(String.format("digester-rules.xml: Only the first pattern component can be a wildcard. Encountered [%s]", originalPattern));
		}
		if(componentsThatShouldNotBeWildcard.size() >= 2) {
			List<String> violationCheckWords = new ArrayList<>(componentsThatShouldNotBeWildcard.subList(0, componentsThatShouldNotBeWildcard.size() - 1));
			Collections.reverse(violationCheckWords);
			matcher = new Matcher(violationCheckWords, pattern);
			matcher.setPatternOnlyMatchesRoot(matchesOnlyRoot);
		}
	}

	String getRoleName() {
		return components.get(components.size() - 1);
	}

	@Override
	public String toString() {
		return originalPattern;
	}

	static class Matcher {
		private @Getter @Setter boolean patternOnlyMatchesRoot = false;
		private final List<String> backtrackRoleNames;
		private @Getter final String originalPattern;

		Matcher(List<String> backtrackRoleNames, String originalPattern) {
			this.backtrackRoleNames = backtrackRoleNames;
			this.originalPattern = originalPattern;
		}

		boolean matches(FrankElement frankElement) {
			return checkOwners(Arrays.asList(frankElement), backtrackRoleNames);
		}

		boolean checkChildren(List<ConfigChild> configChildren, List<String> remainingBacktrackRoleNames) {
			List<FrankElement> owners = configChildren.stream().map(ConfigChild::getOwningElement).collect(Collectors.toList());
			return checkOwners(owners, remainingBacktrackRoleNames);
		}

		boolean checkOwners(List<FrankElement> owners, List<String> remainingBacktrackRoleNames) {
			boolean haveMatchForRoot = owners.stream()
					.filter(f -> f instanceof RootFrankElement)
					.map(f -> (RootFrankElement) f)
					.anyMatch(f -> f.getRoleName().equals(remainingBacktrackRoleNames.get(0)));
			if(remainingBacktrackRoleNames.size() == 1) {
				if(haveMatchForRoot) {
					return true;
				} else if(patternOnlyMatchesRoot) {
					return false;
				}
			}
			List<ConfigChild> parents = owners.stream()
					.flatMap(f -> f.getConfigParents().stream())
					.filter(c -> c.getRoleName().equals(remainingBacktrackRoleNames.get(0)))
					.collect(Collectors.toList());
			if(parents.isEmpty()) {
				return false;
			} else if(remainingBacktrackRoleNames.size() == 1) {
				return true;
			} else {
				return checkChildren(parents, remainingBacktrackRoleNames.subList(1, remainingBacktrackRoleNames.size()));
			}
		}

		@Override
		public String toString() {
			String result = "Matcher backtracking(" + backtrackRoleNames.stream().collect(Collectors.joining(", ")) + ")";
			if(patternOnlyMatchesRoot) {
				result += " at root";
			}
			return result;
		}
	}
}
