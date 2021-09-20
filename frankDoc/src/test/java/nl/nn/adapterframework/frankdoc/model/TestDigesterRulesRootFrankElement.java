package nl.nn.adapterframework.frankdoc.model;

import lombok.Getter;

public class TestDigesterRulesRootFrankElement extends TestDigesterRulesFrankElement implements DigesterRulesRootFrankElement {
	private @Getter(onMethod = @__(@Override)) String roleName;

	TestDigesterRulesRootFrankElement(String roleName) {
		this.roleName = roleName;
	}
}
