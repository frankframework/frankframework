/*
   Copyright 2019, 2022 WeAreFrank!

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
package org.frankframework.xml;

import org.xml.sax.Attributes;

/**
 * AttributesWrapper that removes all namespaces, retaining only local names.
 *
 * @author Gerrit van Brakel
 *
 */
public class NamespaceRemovingAttributesWrapper extends AttributesWrapper {

	public NamespaceRemovingAttributesWrapper(Attributes source) {
		super(source, "xmlns");
	}

	public int findIndexByLocalName(String localName) {
		for(int i=0;i<getLength();i++) {
			if (localName.equals(getLocalName(i))) {
				return i;
			}
		}
		return -1;
	}

	@Override
	public int getIndex(String uri, String localName) {
		return findIndexByLocalName(localName);
	}


	@Override
	public String getType(String uri, String localName) {
		return getType(findIndexByLocalName(localName));
	}

	@Override
	public String getURI(int i) {
		return "";
	}

	@Override
	public String getValue(String uri, String localName) {
		return getValue(findIndexByLocalName(localName));
	}

	@Override
	public String getQName(int i) {
		return super.getLocalName(i);
	}

}
