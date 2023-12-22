/*
   Copyright 2018 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.align;

import org.apache.xerces.xs.XSTypeDefinition;

/**
 * Top of a stack of parsed elements, that represent the current position in the aligned document.
 */
public class AlignmentContext {

	private final AlignmentContext parent;
	private final String localName;
	private final XSTypeDefinition typeDefinition;

	public AlignmentContext() {
		this(null, null, null);
	}

	public AlignmentContext(AlignmentContext parent, String localName, XSTypeDefinition typeDefinition) {
		this.parent = parent;
		this.localName = localName;
		this.typeDefinition = typeDefinition;
	}

	public AlignmentContext getParent() {
		return parent;
	}

	public String getLocalName() {
		return localName;
	}

	public XSTypeDefinition getTypeDefinition() {
		return typeDefinition;
	}
}
