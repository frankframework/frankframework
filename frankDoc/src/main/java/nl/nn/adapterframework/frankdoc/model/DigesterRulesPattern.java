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

import lombok.Getter;
import lombok.Setter;

class DigesterRulesPattern {
	private final String originalPattern;
	private List<String> components;
	private @Getter String error;
	private @Getter boolean root = false;
	private @Getter ViolationChecker violationChecker;

	DigesterRulesPattern(String pattern) {
		this.originalPattern = pattern;
		boolean matchesOnlyRoot = false;
		if(StringUtils.isBlank(pattern)) {
			components = new ArrayList<>();
			error = String.format("digester-rules.xml: Pattern cannot be null and cannot be blank");
			return;
		}
		components = Arrays.asList(pattern.split("/"));
		if(components.isEmpty()) {
			error = String.format("digester-rules.xml should not contain an empty pattern. The literal value was [%s]", originalPattern);
			return;
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
			error = String.format("digester-rules.xml: A pattern that is only a wildcard is invalid. Encountered [%s]", originalPattern);
			return;
		}
		if(componentsThatShouldNotBeWildcard.stream().anyMatch(s -> s.equals("*"))) {
			error = String.format("digester-rules.xml: Only the first pattern component can be a wildcard. Encountered [%s]", originalPattern);
		}
		if(componentsThatShouldNotBeWildcard.size() >= 2) {
			List<String> violationCheckWords = new ArrayList<>(componentsThatShouldNotBeWildcard.subList(0, componentsThatShouldNotBeWildcard.size() - 1));
			Collections.reverse(violationCheckWords);
			violationChecker = new ViolationChecker(violationCheckWords, pattern);
			violationChecker.setPatternOnlyMatchesRoot(matchesOnlyRoot);
		}
	}

	String getRoleName() {
		return components.get(components.size() - 1);
	}

	@Override
	public String toString() {
		return originalPattern;
	}

	static class ViolationChecker {
		private @Getter @Setter boolean patternOnlyMatchesRoot = false;
		private final List<String> backtrackRoleNames;
		private @Getter final String originalPattern;

		ViolationChecker(List<String> backtrackRoleNames, String originalPattern) {
			this.backtrackRoleNames = backtrackRoleNames;
			this.originalPattern = originalPattern;
		}

		boolean matches(DigesterRulesFrankElement frankElement) {
			return checkOwners(Arrays.asList(frankElement), backtrackRoleNames);
		}

		boolean checkChildren(List<DigesterRulesConfigChild> configChildren, List<String> remainingBacktrackRoleNames) {
			List<DigesterRulesFrankElement> owners = configChildren.stream().map(DigesterRulesConfigChild::getOwningElement).collect(Collectors.toList());
			return checkOwners(owners, remainingBacktrackRoleNames);
		}

		boolean checkOwners(List<DigesterRulesFrankElement> owners, List<String> remainingBacktrackRoleNames) {
			boolean haveMatchForRoot = owners.stream()
					.filter(f -> f instanceof DigesterRulesRootFrankElement)
					.map(f -> (DigesterRulesRootFrankElement) f)
					.anyMatch(f -> f.getRoleName().equals(remainingBacktrackRoleNames.get(0)));
			if(remainingBacktrackRoleNames.size() == 1) {
				if(haveMatchForRoot) {
					return true;
				} else if(patternOnlyMatchesRoot) {
					return false;
				}
			}
			List<DigesterRulesConfigChild> parents = owners.stream()
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
			String result = "ViolationChecker backtracking(" + backtrackRoleNames.stream().collect(Collectors.joining(", ")) + ")";
			if(patternOnlyMatchesRoot) {
				result += " at root";
			}
			return result;
		}
	}
}
