/*
   Copyright 2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;

/**
 * AttributesWrapper that removes all attributes that have a namespace, or are of a specific namespace
 *
 * @author Gerrit van Brakel
 *
 */
public class NamespacedContentsRemovingAttributesWrapper extends AttributesWrapper {

	public NamespacedContentsRemovingAttributesWrapper(Attributes source) {
		super(source,i->StringUtils.isEmpty(source.getURI(i)),false,null);
	}

}
