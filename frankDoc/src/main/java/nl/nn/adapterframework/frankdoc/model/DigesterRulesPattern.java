package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

class DigesterRulesPattern {
	private final String originalPattern;
	private List<String> components;
	private @Getter String error;
	private @Getter boolean root = false;

	DigesterRulesPattern(String pattern) {
		this.originalPattern = pattern;
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
			if(components.size() >= 2) {
				error = String.format("digester-rules.xml: Cannot parse pattern that does not start with * and has multiple words. Have [%s]", originalPattern);
				return;
			}
			root = true;
		}
		if(componentsThatShouldNotBeWildcard.isEmpty()) {
			error = String.format("digester-rules.xml: A pattern that is only a wildcard is invalid. Encountered [%s]", originalPattern);
			return;
		}
		if(componentsThatShouldNotBeWildcard.stream().anyMatch(s -> s.equals("*"))) {
			error = String.format("digester-rules.xml: Only the first pattern component can be a wildcard. Encountered [%s]", originalPattern);
		}
	}

	String getRoleName() {
		return components.get(components.size() - 1);
	}

	boolean contains(String word) {
		return components.stream().anyMatch(w -> w.equals(word));
	}

	@Override
	public String toString() {
		return originalPattern;
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
		return new ViolationChecker(violationCheckWords, originalPattern);
	}

	static class ViolationChecker {
		private final List<String> backtrackRoleNames;
		private @Getter final String originalPattern;

		ViolationChecker(List<String> backtrackRoleNames, String originalPattern) {
			this.backtrackRoleNames = backtrackRoleNames;
			this.originalPattern = originalPattern;
		}

		// TODO: If digester-rules.xml has a pattern */configuration/adapter,
		// should it accept a config Configuration.registerAdapter()?. Indeed,
		// class Adapter has config parent Configuration, but Configuration itself
		// does not have parents. At the moment this point has not been considered,
		// because we do not need patterns that include the role name of the root
		// XML element in Frank configurations. We only have a pattern "configuration"
		// without a start and without other words, but that pattern can be ignored by
		// the Frank!Doc.
		boolean checkImplemented(Set<String> rootRoleNames) {
			if(backtrackRoleNames.stream().anyMatch(w -> rootRoleNames.contains(w))) {
				return false;
			} else {
				return true;
			}
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
			return "ViolationChecker backtracking(" + backtrackRoleNames.stream().collect(Collectors.joining(", ")) + ")";
		}
	}
}
