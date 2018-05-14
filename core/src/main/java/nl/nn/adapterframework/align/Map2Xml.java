/*
   Copyright 2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.align;

import java.util.List;
import java.util.Map;

import javax.xml.validation.ValidatorHandler;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.SAXException;

/**
 * Base class for XML Schema guided Map to XML conversion;
 * ToXml Container C: Map M<K,V>
 * ToXml Node N: V
 * @author Gerrit van Brakel
 */
public abstract class Map2Xml<K,V,M extends Map<K,V>> extends ToXml<M,V> {

	{
		setDeepSearch(true);
	}
	
	public Map2Xml() {
		super();
	}

	public Map2Xml(ValidatorHandler validatorHandler) {
		super(validatorHandler);
	}

	public Map2Xml(ValidatorHandler validatorHandler, List<XSModel> schemaInformation) {
		super(validatorHandler,schemaInformation);
	}

	@Override
	public Map<String, String> getAttributes(XSElementDeclaration elementDeclaration, V node) throws SAXException {
		return null;
	}

	@Override
	public boolean isNil(XSElementDeclaration elementDeclaration, V node) {
		return false;
	}

	@Override
	public V getRootNode(M container) {
		return null;
	}

}
