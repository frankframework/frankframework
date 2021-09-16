package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

class DigesterRulesPattern {
	private final String originalPattern;
	private final List<String> components;

	DigesterRulesPattern(String pattern) {
		this.originalPattern = pattern;
		components = Arrays.asList(pattern.split("/"));
	}

	String getError() {
		if(components.isEmpty()) {
			return String.format("digester-rules.xml should not contain an empty pattern. The literal value was [%s]", originalPattern);
		}
		List<String> componentsThatShouldNotBeWildcard = components;
		if(components.get(0).equals("*")) {
			componentsThatShouldNotBeWildcard = components.subList(1, components.size());
		} else if(components.size() >= 2) {
			return String.format("digester-rules.xml: Cannot parse pattern that does not start with * and has multiple words. Have [%s]", originalPattern);
		}
		if(componentsThatShouldNotBeWildcard.isEmpty()) {
			return String.format("digester-rules.xml: A pattern that is only a wildcard is invalid. Encountered [%s]", originalPattern);
		}
		if(componentsThatShouldNotBeWildcard.stream().anyMatch(s -> s.equals("*"))) {
			return String.format("digester-rules.xml: Only the first pattern component can be a wildcard. Encountered [%s]", originalPattern);
		}
		return componentsThatShouldNotBeWildcard.get(componentsThatShouldNotBeWildcard.size() - 1);
	}

	String getRoleName() {
		return components.get(components.size() - 1);
	}

	ViolationChecker getViolationChecker() {
		if(! components.get(0).equals("*")) {
			return null;
		}
		List<String> nonWildcards = components.subList(1, components.size());
		if(nonWildcards.size() <= 1) {
			return null;
		}
		List<String> violationCheckWords = new ArrayList<>(nonWildcards.subList(0, nonWildcards.size() - 1));
		Collections.reverse(violationCheckWords);
		return new ViolationChecker(violationCheckWords);
	}

	static class ViolationChecker {
		private final List<String> backtrackRoleNames;

		ViolationChecker(List<String> backtrackRoleNames) {
			this.backtrackRoleNames = backtrackRoleNames;
		}

		boolean check(DigesterRulesConfigChild configChild) {
			List<DigesterRulesConfigChild> current = Arrays.asList(configChild);
			for(String backtrackRoleName: backtrackRoleNames) {
				current = current.stream()
						.map(DigesterRulesConfigChild::getOwningElement)
						.flatMap(f -> f.getConfigParents().stream())
						.filter(c -> ! c.isViolatesDigesterRules())
						.filter(c -> c.getRoleName().equals(backtrackRoleName))
						.collect(Collectors.toList());
				if(current.isEmpty()) {
					return false;
				}
			}
			return true;
		}

		@Override
		public String toString() {
			return "ViolationChecker backtracking (" + backtrackRoleNames.stream().collect(Collectors.joining(",")) + ")";
		}
	}
}
