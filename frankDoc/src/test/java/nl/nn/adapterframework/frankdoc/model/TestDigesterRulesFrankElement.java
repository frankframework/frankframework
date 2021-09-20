package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

public class TestDigesterRulesFrankElement implements DigesterRulesFrankElement {
	private @Getter(onMethod = @__({@Override})) List<DigesterRulesConfigChild> configParents = new ArrayList<>();

	TestDigesterRulesConfigChild addParent(String roleName) {
		TestDigesterRulesConfigChild parent = TestDigesterRulesConfigChild.getInstance(roleName);
		configParents.add(parent);
		return parent;
	}

	TestDigesterRulesConfigChild addRootOwnedParent(String roleName, String rootRoleName) {
		TestDigesterRulesConfigChild parent = TestDigesterRulesConfigChild.getRootOwnedInstance(roleName, rootRoleName);
		configParents.add(parent);
		return parent;
	}
}
