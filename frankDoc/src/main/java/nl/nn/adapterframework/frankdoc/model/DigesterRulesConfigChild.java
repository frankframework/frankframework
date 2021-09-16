package nl.nn.adapterframework.frankdoc.model;

/**
 * Interface implemented by ConfigChild.
 * This interface is introduced to test DigesterRulesPattern. Instances of this interface can be
 * created more easily than ConfigChild instances.
 */
interface DigesterRulesConfigChild {
	String getRoleName();
	DigesterRulesFrankElement getOwningElement();
	boolean isViolatesDigesterRules();
}
