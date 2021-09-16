package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class TestDigesterRulesConfigChild implements DigesterRulesConfigChild, DigesterRulesFrankElement {
	private @Getter(onMethod = @__({@Override})) final String roleName;
	private @Getter(onMethod = @__({@Override})) List<DigesterRulesConfigChild> configParents = new ArrayList<>();
	private @Getter(onMethod = @__({@Override})) @Setter boolean violatesDigesterRules = false;

	TestDigesterRulesConfigChild(String roleName) {
		this.roleName = roleName;
	}

	@Override
	public DigesterRulesFrankElement getOwningElement() {
		return this;
	}

	TestDigesterRulesConfigChild addParent(String roleName) {
		TestDigesterRulesConfigChild parent = new TestDigesterRulesConfigChild(roleName);
		configParents.add(parent);
		return parent;
	}

	@Override
	public String toString() {
		return roleName;
	}
}
