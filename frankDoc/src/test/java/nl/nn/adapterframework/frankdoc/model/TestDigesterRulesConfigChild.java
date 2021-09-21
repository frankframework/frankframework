package nl.nn.adapterframework.frankdoc.model;

import lombok.Getter;

public class TestDigesterRulesConfigChild implements DigesterRulesConfigChild {
	private final TestDigesterRulesFrankElement owner;
	private @Getter(onMethod = @__({@Override})) final String roleName;

	static TestDigesterRulesConfigChild getInstance(String roleName) {
		TestDigesterRulesFrankElement owner = new TestDigesterRulesFrankElement();
		return new TestDigesterRulesConfigChild(owner, roleName);
	}

	static TestDigesterRulesConfigChild getRootOwnedInstance(String roleName, String rootRoleName) {
		TestDigesterRulesFrankElement owner = new TestDigesterRulesRootFrankElement(rootRoleName);
		return new TestDigesterRulesConfigChild(owner, roleName);
	}

	private TestDigesterRulesConfigChild(TestDigesterRulesFrankElement owner, String roleName) {
		this.owner = owner;
		this.roleName = roleName;
	}

	@Override
	public DigesterRulesFrankElement getOwningElement() {
		return owner;
	}

	TestDigesterRulesConfigChild addParent(String roleName) {
		return owner.addParent(roleName);
	}

	TestDigesterRulesConfigChild addRootOwnedParent(String roleName, String rootRoleName) {
		return owner.addRootOwnedParent(roleName, rootRoleName);
	}

	@Override
	public String toString() {
		return roleName;
	}
}
