package nl.nn.adapterframework.doc.model;

import java.util.Arrays;
import java.util.stream.Collectors;

import lombok.Getter;

public class FrankElementStatistics {
	private @Getter FrankElement subject;
	private @Getter int numAncestors;
	private @Getter int numChildren;
	private @Getter int numDescendants;
	private @Getter int numConfigChildren;
	private @Getter int numAttributes;
	private @Getter int numOverriddenConfigChildren;
	private @Getter int numOverriddenAttributes;
	private @Getter int numConfigChildOverriders;
	private @Getter int numAttributeOverriders;
	private @Getter int minConfigChildOverriderDepth;
	private @Getter int maxConfigChildOverriderDepth;
	private @Getter int minAttributeOverriderDepth;
	private @Getter int maxAttributeOverriderDepth;

	FrankElementStatistics(FrankElement subject) {
		this.subject = subject;
		if(subject.getParent() == null) {
			numAncestors = 0;
		} else {
			numAncestors = subject.getParent().getStatistics().getNumAncestors() + 1;
		}
	}

	public static String header() {
		return Arrays.asList(
				"FullName",
				"numAncestors",
				"numChildren",
				"numDescendants",
				"numConfigChildren",
				"numAttributes",
				"numOverriddenConfigChildren",
				"numOverriddenAttributes",
				"numConfigChildOverriders",
				"numAttributeOverriders",
				"minConfigChildOverriderDepth",
				"maxConfigChildOverriderDepth",
				"minAttributeOverriderDepth",
				"maxAttributeOverriderDepth")
				.stream().collect(Collectors.joining(", "));
	}

	@Override
	public String toString() {
		return Arrays.asList(
				subject.getFullName(),
				new Integer(numAncestors).toString(),
				new Integer(numChildren).toString(),
				new Integer(numDescendants).toString(),
				new Integer(numConfigChildren).toString(),
				new Integer(numAttributes).toString(),
				new Integer(numOverriddenConfigChildren).toString(),
				new Integer(numOverriddenAttributes).toString(),
				new Integer(numConfigChildOverriders).toString(),
				new Integer(numAttributeOverriders).toString(),
				new Integer(minConfigChildOverriderDepth).toString(),
				new Integer(maxConfigChildOverriderDepth).toString(),
				new Integer(minAttributeOverriderDepth).toString(),
				new Integer(maxAttributeOverriderDepth).toString())
				.stream().collect(Collectors.joining(", "));
	}

	void finish() {
		finishDescendantStatistics();
		finishConfigChildOverrideStatistics();
		finishAttributeOverrideStatistics();
	}

	private void finishDescendantStatistics() {
		numConfigChildren = subject.getConfigChildren().size();
		numAttributes = subject.getAttributes().size();
		boolean isFirstAncestor = true;
		FrankElement ancestor = subject;
		while(ancestor.getParent() != null) {
			ancestor = ancestor.getParent();
			ancestor.getStatistics().numDescendants++;
			if(isFirstAncestor) {
				ancestor.getStatistics().numChildren++;
				isFirstAncestor = false;
			}
		}
	}

	private void finishConfigChildOverrideStatistics() {
		for(ConfigChild configChild: subject.getConfigChildren()) {
			if(configChild.getOverriddenFrom() != null) {
				numOverriddenConfigChildren++;
				addConfigChildOverriddenFrom(configChild.getOverriddenFrom());
			}
		}
	}

	private void addConfigChildOverriddenFrom(FrankElement overriddenFrom) {
		overriddenFrom.getStatistics().onConfigChildOverriderAtDepth(inheritanceDistance(overriddenFrom));
	}

	private int inheritanceDistance(FrankElement targetAncestor) {
		int depth = 0;
		FrankElement ancestor = subject;
		while(ancestor != targetAncestor) {
			ancestor = ancestor.getParent();
			depth++;
		}
		return depth;
	}

	private void onConfigChildOverriderAtDepth(int depth) {
		numConfigChildOverriders++;
		if(minConfigChildOverriderDepth == 0) {
			minConfigChildOverriderDepth = depth;
			maxConfigChildOverriderDepth = depth;
		}
		else {
			if(depth < minConfigChildOverriderDepth) {
				minConfigChildOverriderDepth = depth;
			}
			if(depth > maxConfigChildOverriderDepth) {
				maxConfigChildOverriderDepth = depth;
			}
		}
	}

	private void finishAttributeOverrideStatistics() {
		for(FrankAttribute attribute: subject.getAttributes()) {
			if(attribute.getOverriddenFrom() != null) {
				numOverriddenAttributes++;
				addAttributeOverriddenFrom(attribute.getOverriddenFrom());
			}
		}
	}

	private void addAttributeOverriddenFrom(FrankElement overriddenFrom) {
		overriddenFrom.getStatistics().onAttributeOverriderAtDepth(inheritanceDistance(overriddenFrom));
	}

	private void onAttributeOverriderAtDepth(int depth) {
		numAttributeOverriders++;
		if(minAttributeOverriderDepth == 0) {
			minAttributeOverriderDepth = depth;
			maxAttributeOverriderDepth = depth;
		}
		else {
			if(depth < minAttributeOverriderDepth) {
				minAttributeOverriderDepth = depth;
			}
			if(depth > maxAttributeOverriderDepth) {
				maxAttributeOverriderDepth = depth;
			}
		}		
	}
}
