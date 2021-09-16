package nl.nn.adapterframework.frankdoc.model;

import java.util.List;

/**
 * Interface implemented by FrankElement.
 * This interface is introduced to test DigesterRulesPattern. Instances of this interface can be
 * created more easily than FrankElement instances.
 */
interface DigesterRulesFrankElement {
	List<DigesterRulesConfigChild> getConfigParents();
}
