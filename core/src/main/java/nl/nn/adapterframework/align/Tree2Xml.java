/*
   Copyright 2017,2018 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.SAXException;

/**
 * Base class for XML Schema guided Tree to XML conversion;
 *
 * @author Gerrit van Brakel
 *
 * @param <C> Container of the root of the tree
 * @param <N> The tree node type
 */
public abstract class Tree2Xml<C,N> extends ToXml<C,N> {

	SubstitutionProvider<?> sp;

	public Tree2Xml(ValidatorHandler validatorHandler, List<XSModel> schemaInformation) {
		super(validatorHandler,schemaInformation);
	}

	public abstract Set<String> getAllNodeChildNames(XSElementDeclaration elementDeclaration, N node) throws SAXException; // returns null when no children present, otherwise a _copy_ of the Set (it will be modified)
	public abstract Iterable<N> getNodeChildrenByName(N node, XSElementDeclaration childElementDeclaration) throws SAXException;
	public abstract String getNodeText(XSElementDeclaration elementDeclaration, N node) throws SAXException;

	@Override
	public boolean hasChild(XSElementDeclaration elementDeclaration, N node, String childName) throws SAXException {
		// should check for complex or simple type.
		// for complex, any path of a substitution is valid
		// for simple, only when a valid substitution value is found, a hit should be present.
		if (sp!=null && sp.hasSubstitutionsFor(getContext(), childName)) {
			return true;
		}
		Set<String> allChildNames=getAllNodeChildNames(elementDeclaration, node);
		return allChildNames!=null && allChildNames.contains(childName);
	}


	/**
	 * Allows subclasses to provide a special way of substituting.
	 * This is used by Json2Xml to insert a List of values as a JsonArray.
	 */
	protected N getSubstitutedChild(N node, String childName) {
		return node;
	}
	protected String getOverride(XSElementDeclaration elementDeclaration, N node) throws SAXException {
		Object text = sp.getOverride(getContext());
		if (text instanceof String) {
			return (String)text;
		}
		return text.toString();
	}

	@Override
	public final Iterable<N> getChildrenByName(N node, XSElementDeclaration childElementDeclaration) throws SAXException {
		String childName=childElementDeclaration.getName();
		Iterable<N> children = getNodeChildrenByName(node, childElementDeclaration);
		if (children==null && sp!=null && sp.hasSubstitutionsFor(getContext(), childName)) {
			List<N> result=new LinkedList<>();
			result.add(getSubstitutedChild(node, childName));
			return result;
		}
		return children;
	}

	@Override
	public final String getText(XSElementDeclaration elementDeclaration, N node) throws SAXException {
		String nodeName=elementDeclaration.getName();
		Object text;
		if (log.isTraceEnabled()) log.trace("node ["+nodeName+"] currently parsed element ["+getContext().getLocalName()+"]");
		if (sp!=null && sp.hasOverride(getContext())) {
			String result = getOverride(elementDeclaration, node);
			if (log.isTraceEnabled()) log.trace("node ["+nodeName+"] override found ["+result+"]");
			return result;
		}
		String result=getNodeText(elementDeclaration, node);
		if (sp!=null && StringUtils.isEmpty(result) && (text=sp.getDefault(getContext()))!=null) {
			if (log.isTraceEnabled()) log.trace("node ["+nodeName+"] default found ["+text+"]");
			result = text.toString();
		}
		if (log.isTraceEnabled()) log.trace("node ["+nodeName+"] returning value ["+result+"]");
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

	public void setSubstitutionProvider(SubstitutionProvider<?> substitutions) {
		this.sp = substitutions;
	}

	public void setOverrideValues(Map<String, Object> overrideValues) {
		OverridesMap<Object> overrides=new OverridesMap<>();
		overrides.registerSubstitutes(overrideValues);
		setSubstitutionProvider(overrides);
	}

}
