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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
//import org.apache.commons.lang3.StringUtils;
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
public abstract class Tree2Xml<C,N> extends ToXml<C,N> {

	private Map<String,Object> overrideValues;
	private Map<String,Object> defaultValues;
	
	public Tree2Xml() {
		super();
	}

	public Tree2Xml(ValidatorHandler validatorHandler) {
		super(validatorHandler);
	}

	public Tree2Xml(ValidatorHandler validatorHandler, List<XSModel> schemaInformation) {
		super(validatorHandler,schemaInformation);
	}

	public abstract Set<String> getAllNodeChildNames(XSElementDeclaration elementDeclaration, N node) throws SAXException; // returns null when no children present, otherwise a _copy_ of the Set (it will be modified)
	public abstract Iterable<N> getNodeChildrenByName(N node, XSElementDeclaration childElementDeclaration) throws SAXException;
	public abstract String getNodeText(XSElementDeclaration elementDeclaration, N node);

	@Override
	public boolean hasChild(XSElementDeclaration elementDeclaration, N node, String childName) throws SAXException {
		if (overrideValues!=null && overrideValues.containsKey(childName) || 
			defaultValues!=null && defaultValues.containsKey(childName)) {
			return true;
		}
		Set<String> allChildNames=getAllNodeChildNames(elementDeclaration, node);
		return allChildNames!=null && allChildNames.contains(childName);
	}

	@Override
	public final Iterable<N> getChildrenByName(N node, XSElementDeclaration childElementDeclaration) throws SAXException {
		String childName=childElementDeclaration.getName();
		if (overrideValues!=null && overrideValues.containsKey(childName)) {
			List<N> result=new LinkedList<N>();
			result.add(node);
			return result;
		}
		return getNodeChildrenByName(node, childElementDeclaration);
	}

	@Override
	public final String getText(XSElementDeclaration elementDeclaration, N node) {
		String nodeName=elementDeclaration.getName();
		if (overrideValues!=null && overrideValues.containsKey(nodeName)) {
			Object text=overrideValues.get(nodeName);
			if (DEBUG) log.debug("getText() node ["+nodeName+"] override found ["+text+"]");
			if (text==null || text instanceof String) {
				return (String)text;
			}
			return text.toString();
		}
		String result=getNodeText(elementDeclaration, node);
		if (defaultValues!=null && StringUtils.isEmpty(result) && defaultValues.containsKey(nodeName)) {
			Object text=defaultValues.get(nodeName);
			if (DEBUG) log.debug("getText() node ["+nodeName+"] default found ["+text+"]");
			if (text==null || text instanceof String) {
				result = (String)text;
			}
			result = text.toString();
		}
		if (DEBUG) log.debug("getText() node ["+nodeName+"] returning value ["+result+"]");
		return result;
	}

	@Override
	protected Set<String> getUnprocessedChildElementNames(XSElementDeclaration elementDeclaration, N node, Set<String> processedChildren) throws SAXException {
		Set<String> unProcessedChildren = getAllNodeChildNames(elementDeclaration,node);
		if (unProcessedChildren!=null && !unProcessedChildren.isEmpty()) {
			unProcessedChildren.removeAll(processedChildren);
		}
		return unProcessedChildren;
	}

	public Map<String, Object> getOverrideValues() {
		return overrideValues;
	}
	public void setOverrideValues(Map<String, Object> overrideValues) {
		this.overrideValues = overrideValues;
		boolean mustDeepSearch=overrideValues!=null && !overrideValues.isEmpty();
		setDeepSearch(mustDeepSearch);
	}

	public Map<String, Object> getDefaultValues() {
		return defaultValues;
	}
	public void setDefaultValues(Map<String, Object> defaultValues) {
		this.defaultValues = defaultValues;
	}


}
