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
import java.util.Set;

import javax.xml.validation.ValidatorHandler;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.SAXException;

/**
 * Base class for XML Schema guided Tree to XML conversion;
 * 
 * @author Gerrit van Brakel
 *
 * @param <N>
 */
public abstract class Tree2Xml<N> extends ToXml<N,N> {

	private boolean autoInsertMandatory=false;   // TODO: behaviour needs to be tested.

	private final boolean DEBUG=false; 
	
	public Tree2Xml() {
		super();
	}

	public Tree2Xml(ValidatorHandler validatorHandler) {
		super(validatorHandler);
	}

	public Tree2Xml(ValidatorHandler validatorHandler, List<XSModel> schemaInformation) {
		super(validatorHandler,schemaInformation);
	}

	
	@Override
	protected void processChildElement(N node, String name, XSElementDeclaration childElementDeclaration, boolean mandatory, Set<String> unProcessedChildren, Set<String> processedChildren) throws SAXException {
		String childElementName = childElementDeclaration.getName(); 
		if (DEBUG) log.debug("Tree2Xml.processChildElement() parent name ["+name+"] childElementName ["+childElementName+"]");
		Iterable<N> childNodes = getChildrenByName(node,childElementName);
		boolean childSeen=false;
		if (childNodes!=null) {
			childSeen=true;
			int i=0;
			for (N childNode:childNodes) {
				i++;
				handleNode(childNode,childElementDeclaration);
			}
			if (DEBUG) log.debug("processed ["+i+"] children found by name ["+childElementName+"] in ["+name+"]");
		} else {
			if (DEBUG) log.debug("no children found by name ["+childElementName+"] in ["+name+"]");
		}
		if (childSeen) {
			if (unProcessedChildren==null) {
				throw new IllegalStateException("child element ["+childElementName+"] found, but node ["+name+"] should have no children");
			}
			if (!unProcessedChildren.remove(childElementName)) {
				throw new IllegalStateException("child element ["+childElementName+"] not found in list of unprocessed children of node ["+name+"]");
			}
			if (processedChildren.contains(childElementName)) {
				throw new IllegalStateException("child element ["+childElementName+"] already processed for node ["+name+"]");
			}
			processedChildren.add(childElementName);
		}
		if (!childSeen && mandatory && isAutoInsertMandatory()) {
			if (log.isDebugEnabled()) log.debug("inserting mandatory element ["+childElementName+"]");
			handleNode(node,childElementDeclaration); // insert node when minOccurs > 0, and no node is present
		}
	}


	public boolean isAutoInsertMandatory() {
		return autoInsertMandatory;
	}
	public void setAutoInsertMandatory(boolean autoInsertMandatory) {
		this.autoInsertMandatory = autoInsertMandatory;
	}

}
