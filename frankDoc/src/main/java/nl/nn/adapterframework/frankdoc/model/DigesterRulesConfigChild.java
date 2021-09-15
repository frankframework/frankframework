package nl.nn.adapterframework.frankdoc.model;

interface DigesterRulesConfigChild {
	String getRoleName();
	DigesterRulesFrankElement getOwningElement();
	boolean isViolatesDigesterRules();
}
