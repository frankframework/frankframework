/*
   Copyright 2021 Nationale-Nederlanden, 2022 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.credentialprovider.rolemapping;

import org.apache.tomcat.util.digester.Digester;
import org.apache.tomcat.util.digester.RuleSet;

public class RoleGroupMappingRuleSet implements RuleSet {

// ----------------------------------------------------- Instance Variables

	/**
	 * The matching pattern prefix to use for recognizing our elements.
	 */
	protected final String prefix;

// ------------------------------------------------------------ Constructor

	/**
	 * Construct an instance of this <code>RuleSet</code> with the default matching
	 * pattern prefix.
	 */
	public RoleGroupMappingRuleSet() {
		this("security-role-mappings/");
	}

	/**
	 * Construct an instance of this <code>RuleSet</code> with the specified
	 * matching pattern prefix.
	 *
	 * @param prefix Prefix for matching pattern rules (including the trailing slash character)
	 */
	public RoleGroupMappingRuleSet(String prefix) {
		this.prefix = prefix;
	}

// --------------------------------------------------------- Public Methods

	/**
	 * <p>
	 * Add the set of Rule instances defined in this RuleSet to the specified
	 * <code>Digester</code> instance, associating them with our namespace URI (if
	 * any). This method should only be called by a Digester instance.
	 * </p>
	 *
	 * @param digester Digester instance to which the new Rule instances should be added.
	 */
	@Override
	public void addRuleInstances(Digester digester) {
		digester.addCallMethod(prefix + "security-role-mapping", "addRoleGroupMapping", 2);
		digester.addCallParam(prefix + "security-role-mapping/role-name", 0);
		digester.addCallParam(prefix + "security-role-mapping/group-name", 1);
	}
}
