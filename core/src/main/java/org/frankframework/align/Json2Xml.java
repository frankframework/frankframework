/*
   Copyright 2017,2018 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.ValidatorHandler;

import jakarta.annotation.Nonnull;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.xerces.impl.xs.XSElementDecl;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.frankframework.xml.XmlWriter;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * XML Schema guided JSON to XML converter;
 *
 * @author Gerrit van Brakel
 */
public class Json2Xml extends XmlAligner {

	public static final String MSG_FULL_INPUT_IN_STRICT_COMPACTING_MODE="straight json found while expecting compact arrays and strict syntax checking";
	public static final String MSG_EXPECTED_SINGLE_ELEMENT="did not expect array, but single element";
	public static final String XSI_PREFIX_MAPPING="xsi";
	public static final String MSG_EXPECTED_ELEMENT="expected element";
	public static final String MSG_CANNOT_NOT_FIND_ELEMENT_DECLARATION="Cannot find the declaration of element";
	private static final String NAMESPACE_PREFIX = "ns";

	private final boolean insertElementContainerElements;
	private final boolean strictSyntax;
	private final Map<String, String> prefixMap = new HashMap<>();
	SubstitutionProvider<?> sp;
	private @Getter @Setter boolean readAttributes=true;
	private static final String ATTRIBUTE_PREFIX = "@";
	private static final String MIXED_CONTENT_LABEL = "#text";
	private @Getter @Setter String rootElement;
	private @Getter @Setter String targetNamespace;
	private @Getter @Setter boolean deepSearch=false;
	private @Getter @Setter boolean failOnWildcards=false;
	private int prefixPrefixCounter=1;

	public Json2Xml(ValidatorHandler validatorHandler, List<XSModel> schemaInformation, boolean insertElementContainerElements, String rootElement, boolean strictSyntax) {
		super(validatorHandler, schemaInformation);
		this.insertElementContainerElements=insertElementContainerElements;
		this.strictSyntax=strictSyntax;
		setRootElement(rootElement);
	}

	private static Set<String> getNamesOfXsdChildElements(XSComplexTypeDefinition complexTypeDefinition) {
		XSTerm term = complexTypeDefinition.getParticle().getTerm();
		if (!(term instanceof XSModelGroup modelGroup)) {
			return Collections.emptySet();
		}
		@SuppressWarnings("unchecked")
		List<XSParticle> particles = modelGroup.getParticles();
		return particles.stream()
				.map(XSParticle::getTerm)
				.map(XSObject::getName)
				.collect(Collectors.toSet());
	}

	public void startParse(JsonValue node) throws SAXException {
		if (node instanceof JsonObject root) {
			List<String> potentialRootElements = new ArrayList<>(root.keySet());
			potentialRootElements.removeIf(e-> e.startsWith(ATTRIBUTE_PREFIX) || e.startsWith(MIXED_CONTENT_LABEL));
			if(StringUtils.isEmpty(getRootElement())) {
				determineRootElement(potentialRootElements);
			}

			// determine somewhat heuristically whether the json contains a 'root' node:
			// if the outermost JsonObject contains only one key, that has the name of the root element,
			// then we'll assume that that is the root element...
			if (potentialRootElements.size()==1 && getRootElement().equals(potentialRootElements.get(0))) {
				node = root.get(getRootElement());
			}
		}
		if (node instanceof JsonArray && !insertElementContainerElements && strictSyntax) {
			throw new SAXException(MSG_EXPECTED_SINGLE_ELEMENT+" ["+getRootElement()+"] or array element container");
		}
		//if (log.isTraceEnabled()) log.trace("startParse() rootNode ["+node.toString()+"]"); // result of node.toString() is confusing. Do not log this.
		try {
			validatorHandler.startDocument();
			handleRootNode(node, getRootElement(), getTargetNamespace());
			validatorHandler.endDocument();
		} catch (SAXException e) {
			handleError(e);
		}
	}

	private void determineRootElement(List<String> potentialRootElements) throws SAXException {
		if (potentialRootElements.isEmpty()) {
			throw new SAXException("Cannot determine XML root element, neither from attribute rootElement, nor from JSON node");
		}
		if(potentialRootElements.size() == 1) {
			setRootElement(potentialRootElements.get(0));
		} else {
			String namesList = potentialRootElements.stream().limit(5).collect(Collectors.joining(","));
			if(potentialRootElements.size() > 5) {
				namesList+=", ...";
			}
			throw new SAXException("Cannot determine XML root element, too many names ["+namesList+"] in JSON");
		}
	}

	public void handleElementContents(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		XSTypeDefinition typeDefinition = elementDeclaration.getTypeDefinition();
		JsonValue nodeToHandle;
		if (node instanceof JsonObject object) {
			nodeToHandle = object.getOrDefault(MIXED_CONTENT_LABEL, node);
		} else {
			nodeToHandle = node;
		}
		if (typeDefinition==null) {
			log.warn("handleElementContents typeDefinition is null");
			handleSimpleTypedElement(elementDeclaration, nodeToHandle);
			return;
		}
		switch (typeDefinition.getTypeCategory()) {
		case XSTypeDefinition.SIMPLE_TYPE:
			if (log.isTraceEnabled()) log.trace("handleElementContents typeDefinition.typeCategory is SimpleType, no child elements");
			handleSimpleTypedElement(elementDeclaration, nodeToHandle);
			return;
		case XSTypeDefinition.COMPLEX_TYPE:
			XSComplexTypeDefinition complexTypeDefinition=(XSComplexTypeDefinition)typeDefinition;
			switch (complexTypeDefinition.getContentType()) {
			case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
				if (log.isTraceEnabled()) log.trace("handleElementContents complexTypeDefinition.contentType is Empty, no child elements");
				return;
			case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
				if (log.isTraceEnabled()) log.trace("handleElementContents complexTypeDefinition.contentType is Simple, no child elements (only characters)");
				handleSimpleTypedElement(elementDeclaration, nodeToHandle);
				return;
			case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
			case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
				handleComplexTypedElement(elementDeclaration, nodeToHandle);
				return;
			default:
				throw new IllegalStateException("handleElementContents complexTypeDefinition.contentType is not Empty,Simple,Element or Mixed, but ["+complexTypeDefinition.getContentType()+"]");
			}
		default:
			throw new IllegalStateException("handleElementContents typeDefinition.typeCategory is not SimpleType or ComplexType, but ["+typeDefinition.getTypeCategory()+"] class ["+typeDefinition.getClass().getName()+"]");
		}
	}


	public String getNodeText(JsonValue node) {
		String result;
		if (node instanceof JsonString string) {
			result=string.getString();
		} else if (node instanceof JsonStructure) { // this happens when override key is present without a value
			result=null;
		} else {
			result=node.toString();
		}
		if ("{}".equals(result)) {
			result="";
		}
		if (log.isTraceEnabled()) log.trace("node [{}] = [{}]", ToStringBuilder.reflectionToString(node), result);
		return result;
	}

	public boolean isNil(JsonValue node) {
		boolean result=node==JsonValue.NULL;
		if (log.isTraceEnabled()) log.trace("node [{}] = [{}]", node, result);
		return result;
	}

	public Map<String, String> getAttributes(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		if (!readAttributes) {
			return null;
		}
		if (!(node instanceof JsonObject o)) {
			if (log.isTraceEnabled())
				log.trace("parent node is not a JsonObject, but a [{}] isParentOfSingleMultipleOccurringChildElement [{}]  value [{}], returning null", node.getClass()
						.getName(), isParentOfSingleMultipleOccurringChildElement(), node);
			return null;
		}
		if (o.isEmpty()) {
			if (log.isTraceEnabled()) log.trace("getAttributes() no children");
			return null;
		}
		try {
			Map<String, String> result=new LinkedHashMap<>(); // it is not really necessary to preserve the order, but often the results look nicer, and it is easier for testing ...
			for (String key:o.keySet()) {
				if (key.startsWith(ATTRIBUTE_PREFIX)) {
					String attributeName=key.substring(ATTRIBUTE_PREFIX.length());
					String value=getText(elementDeclaration, o.get(key));
					if (log.isTraceEnabled()) log.trace("getAttributes() attribute [{}] = [{}]", attributeName, value);
					result.put(attributeName, value);
				}
			}
			return result;
		} catch (JsonException e) {
			throw new SAXException(e);
		}
	}

	public boolean hasChild(JsonValue node, String childName) throws SAXException {
		if (isParentOfSingleMultipleOccurringChildElement() && (insertElementContainerElements || !strictSyntax)) {
			// The array element can always considered to be present; if it is not, it will be inserted
			return true;
		}
		// should check for complex or simple type.
		// for complex, any path of a substitution is valid
		// for simple, only when a valid substitution value is found, a hit should be present.
		if (sp!=null && sp.hasSubstitutionsFor(getContext(), childName)) {
			return true;
		}
		Set<String> allChildNames=getAllNodeChildNames(node);
		return allChildNames!=null && allChildNames.contains(childName);
	}

	public Set<String> getAllNodeChildNames(JsonValue node) throws SAXException {
		if (log.isTraceEnabled())
			log.trace("node isParentOfSingleMultipleOccurringChildElement [{}] [{}][{}]", isParentOfSingleMultipleOccurringChildElement(), node.getClass()
					.getName(), node);
		try {
			if (isParentOfSingleMultipleOccurringChildElement()) {
				if ((insertElementContainerElements || !strictSyntax) && node instanceof JsonArray) {
					if (log.isTraceEnabled()) log.trace("parentOfSingleMultipleOccurringChildElement,JsonArray,(insertElementContainerElements || !strictSyntax)");
					Set<String> result = new LinkedHashSet<>(getMultipleOccurringChildElements());
					if (log.isTraceEnabled()) log.trace("isParentOfSingleMultipleOccurringChildElement, result [{}]", result);
					return result;
				}

				if ((insertElementContainerElements && strictSyntax) && !(node instanceof JsonArray)) {
					throw new SAXException(MSG_FULL_INPUT_IN_STRICT_COMPACTING_MODE);
				}
			}
			if (!(node instanceof JsonObject o)) {
				if (log.isTraceEnabled())
					log.trace("parent node is not a JsonObject, but a [{}] isParentOfSingleMultipleOccurringChildElement [{}]  value [{}], returning null", node.getClass()
							.getName(), isParentOfSingleMultipleOccurringChildElement(), node);
				return null;
			}
			if (o.isEmpty()) {
				if (log.isTraceEnabled()) log.trace("no children");
				return null;
			}
			Set<String> result = new LinkedHashSet<>();
			for (String key:o.keySet()) {
				if (!readAttributes || !key.startsWith(ATTRIBUTE_PREFIX)) {
					result.add(key);
				}
			}
			if (log.isTraceEnabled()) log.trace("returning [{}]", result);
			return result;
		} catch (JsonException e) {
			throw new SAXException(e);
		}
	}

	public Iterable<JsonValue> getNodeChildrenByName(JsonValue node, XSElementDeclaration childElementDeclaration) throws SAXException {
		String name=childElementDeclaration.getName();
		if (log.isTraceEnabled())
			log.trace("childname [{}] parent isParentOfSingleMultipleOccurringChildElement [{}] isMultipleOccuringChildElement [{}] node [{}]", name, isParentOfSingleMultipleOccurringChildElement(), isMultipleOccurringChildElement(name), node);
		try {
			if (!(node instanceof JsonObject o)) {
				if (log.isTraceEnabled()) log.trace("parent node is not a JsonObject, but a [{}]", node.getClass().getName());
				return null;
			}
			if (!o.containsKey(name)) {
				if (log.isTraceEnabled()) log.trace("no children named [{}] node [{}]", name, node);
				return null;
			}
			JsonValue child = o.get(name);
			List<JsonValue> result = new ArrayList<>();
			if (child instanceof JsonArray array) {
				if (log.isTraceEnabled())
					log.trace("child named [{}] is a JsonArray, current node insertElementContainerElements [{}]", name, insertElementContainerElements);
				// if it could be necessary to insert elementContainers, we cannot return them as a list of individual elements now, because then the containing element would be duplicated
				// we also cannot use the isSingleMultipleOccurringChildElement, because it is not valid yet
				if (!isMultipleOccurringChildElement(name)) {
					if (insertElementContainerElements || !strictSyntax) {
						result.add(child);
						if (log.isTraceEnabled())
							log.trace("singleMultipleOccurringChildElement [{}] returning array node (insertElementContainerElements=true)", name);
					} else {
						throw new SAXException(MSG_EXPECTED_SINGLE_ELEMENT+" ["+name+"]");
					}
				} else {
					if (log.isTraceEnabled())
						log.trace("childname [{}] returning elements of array node (insertElementContainerElements=false or not singleMultipleOccurringChildElement)", name);
					result.addAll(array);
				}
				return result;
			}
			result.add(child);
			if (log.isTraceEnabled()) log.trace("name [{}] returning [{}]", name, child);
			return result;
		} catch (JsonException e) {
			throw new SAXException(e);
		}
	}

	protected JsonValue getSubstitutedChild(String childName) {
		if (!sp.hasSubstitutionsFor(getContext(), childName)) {
			return null;
		}
		Object substs = sp.getSubstitutionsFor(getContext(), childName);
		if (substs==null) {
			substs="{}";
		}
		if (substs instanceof List<?> list) {
			JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
			for (Object item : list) {
				arrayBuilder.add(item.toString());
			}
			return arrayBuilder.build();
		}
		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
		objectBuilder.add(childName, substs.toString());
		return objectBuilder.build().getJsonString(childName);
	}

	protected String getOverride(JsonValue node) {
		Object text = sp.getOverride(getContext());
		if (text instanceof List) {
			// if the override is a List, then it has already been substituted via getSubstitutedChild.
			// Therefore now get the node text, which is here an individual element already.
			return getNodeText(node);
		}
		if (text instanceof String string) {
			return string;
		}
		return text.toString();
	}

	protected void processChildElement(JsonValue node, String parentName, XSElementDeclaration childElementDeclaration, boolean mandatory, Set<String> processedChildren) throws SAXException {
		String childElementName=childElementDeclaration.getName();
		if (log.isTraceEnabled())
			log.trace("parentName [{}] childElementName [{}] node [{}] isParentOfSingleMultipleOccurringChildElement [{}]", parentName, childElementName, node, isParentOfSingleMultipleOccurringChildElement());
		if (isParentOfSingleMultipleOccurringChildElement()) {
			if (node instanceof JsonArray ja) {
				if (log.isTraceEnabled()) log.trace("array child node is JsonArray, handling each of the elements as a [{}]", childElementName);
				for (JsonValue child:ja) {
					handleElement(childElementDeclaration, child);
				}
				// mark that we have processed the array elements
				processedChildren.add(childElementName);
				return;
			}
			if (node instanceof JsonString) { // support normal (non list) parameters to supply array element values
				if (log.isTraceEnabled()) log.trace("array child node is JsonString, handling as a [{}]", childElementName);
				handleElement(childElementDeclaration, node);
				// mark that we have processed the array element
				processedChildren.add(childElementName);
				return;
			}
		}
		String childElementName1 = childElementDeclaration.getName();
		if (log.isTraceEnabled()) log.trace("ToXml.processChildElement() parent name [{}] childElementName [{}]", parentName, childElementName1);
		Iterable<JsonValue> childNodes = getChildrenByName(node, childElementDeclaration);
		boolean childSeen=false;
		if (childNodes!=null) {
			childSeen = true;
			int i = 0;
			for (JsonValue childNode:childNodes) {
				i++;
				handleElement(childElementDeclaration,childNode);
			}
			if (log.isTraceEnabled()) log.trace("processed [{}] children found by name [{}] in [{}]", i, childElementName1, parentName);
			if (i==0 && isDeepSearch() && childElementDeclaration.getTypeDefinition().getTypeCategory()!=XSTypeDefinition.SIMPLE_TYPE) {
				if (log.isTraceEnabled())
					log.trace("no children processed, and deepSearch, not a simple type therefore handle node [{}] in [{}]", childElementName1, parentName);
				handleElement(childElementDeclaration, node);
				childSeen = true;
			}
		} else {
			if (log.isTraceEnabled()) log.trace("no children found by name [{}] in [{}]", childElementName1, parentName);
			if (isDeepSearch() && childElementDeclaration.getTypeDefinition().getTypeCategory()!=XSTypeDefinition.SIMPLE_TYPE) {
				if (log.isTraceEnabled())
					log.trace("no children found, and deepSearch, not a simple type therefore handle node [{}] in [{}]", childElementName1, parentName);
				if (tryDeepSearchForChildElement(childElementDeclaration, mandatory, node, processedChildren)) {
					childSeen = true;
				}
			}
		}
		if (childSeen) {
			if (processedChildren.contains(childElementName1)) {
				throw new IllegalStateException("child element ["+ childElementName1 +"] already processed for node ["+ parentName +"]");
			}
			processedChildren.add(childElementName1);
		}
	}

	protected boolean isEmptyNode(JsonValue node) {
		if (node instanceof JsonArray) {
			return ((JsonArray)node).isEmpty();
		} else if (node instanceof JsonObject) {
			return ((JsonObject)node).isEmpty();
		} else {
			return true;
		}
	}

	/**
	 * Create a copy of the JSON node that contains only keys from the allowedNames set in the top level.
	 *
	 * @param node Node to copy
	 * @param allowedNames Names of child-nodes to keep in the copy
	 * @return Copy of the JSON node.
	 */
	protected JsonValue filterNodeChildren(JsonValue node, Set<String> allowedNames) {
		if (node instanceof JsonArray) {
			return copyJsonArray((JsonArray)node, allowedNames);
		} else if (node instanceof JsonObject) {
			return copyJsonObject((JsonObject)node, allowedNames);
		} else return node;
	}

	private JsonValue copyJsonObject(JsonObject node, Set<String> allowedNames) {
		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
		node.forEach((key, value) -> {
			if (allowedNames.contains(key)) objectBuilder.add(key, value);
		});
		// Add in substitutions for allowed names not already in the object. This is so objects do not appear empty when
		// substitutions could fill in for absent names.
		// This is perhaps not the cleanest way to make sure the substitutions are performed but this requires the least
		// amount of code changes in other parts.
		if (sp != null) {
			allowedNames.forEach(name -> {
				if (!node.containsKey(name) && sp.hasSubstitutionsFor(getContext(), name)) {
					objectBuilder.add(name, getSubstitutedChild(name));
				}
			});
		}
		return objectBuilder.build();
	}

	private JsonValue copyJsonArray(JsonArray node, Set<String> allowedNames) {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		node.forEach(value -> {
			arrayBuilder.add(filterNodeChildren(value, allowedNames));
		});
		return arrayBuilder.build();
	}

	/**
	 * Helper method for tests
	 */
	public static String translate(JsonStructure jsonStructure, URL schemaURL, boolean compactJsonArrays, String rootElement, String targetNamespace) throws SAXException {
		Json2Xml j2x = create(schemaURL, compactJsonArrays, rootElement, false, false, targetNamespace, null);
		return j2x.translate(jsonStructure);
	}

	/**
	 * Helper method for tests
	 */
	public static Json2Xml create(URL schemaURL, boolean compactJsonArrays, String rootElement, boolean strictSyntax, boolean deepSearch, String targetNamespace, Map<String,Object> overrideValues) throws SAXException {
		ValidatorHandler validatorHandler = getValidatorHandler(schemaURL);
		List<XSModel> schemaInformation = getSchemaInformation(schemaURL);

		// create the validator, set up the chain
		Json2Xml j2x = new Json2Xml(validatorHandler,schemaInformation,compactJsonArrays,rootElement,strictSyntax);
		if (overrideValues!=null) {
			j2x.setOverrideValues(overrideValues);
		}
		if (targetNamespace!=null) {
			j2x.setTargetNamespace(targetNamespace);
		}
		j2x.setDeepSearch(deepSearch);

		return j2x;
	}

	public final Iterable<JsonValue> getChildrenByName(JsonValue node, XSElementDeclaration childElementDeclaration) throws SAXException {
		String childName=childElementDeclaration.getName();
		Iterable<JsonValue> children = getNodeChildrenByName(node, childElementDeclaration);
		if (children==null && sp!=null && sp.hasSubstitutionsFor(getContext(), childName)) {
			List<JsonValue> result=new ArrayList<>();
			result.add(getSubstitutedChild(childName));
			return result;
		}
		return children;
	}

	public final String getText(XSElementDeclaration elementDeclaration, JsonValue node) {
		String nodeName=elementDeclaration.getName();
		Object text;
		if (log.isTraceEnabled()) log.trace("node [{}] currently parsed element [{}]", nodeName, getContext().getLocalName());
		if (sp!=null && sp.hasOverride(getContext())) {
			String result = getOverride(node);
			if (log.isTraceEnabled()) log.trace("node [{}] override found [{}]", nodeName, result);
			return result;
		}
		String result=getNodeText(node);
		if (sp!=null && StringUtils.isEmpty(result) && (text=sp.getDefault(getContext()))!=null) {
			if (log.isTraceEnabled()) log.trace("node [{}] default found [{}]", nodeName, text);
			result = text.toString();
		}
		if (log.isTraceEnabled()) log.trace("node [{}] returning value [{}]", nodeName, result);
		return result;
	}

	protected Set<String> getUnprocessedChildElementNames(JsonValue node, Set<String> processedChildren) throws SAXException {
		Set<String> unProcessedChildren = getAllNodeChildNames(node);
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

	/**
	 * Obtain the XmlAligner as a {@link Source} that can be used as input of a {@link Transformer}.
	 */
	public Source asSource(JsonValue container) {
		return new SAXSource(this,container==null?null: new XmlAlignerInputSource(container));
	}

	/**
	 * Start the parse, obtain the container to parse from the InputSource when set by {@link #asSource(JsonValue)}.
	 * Normally, the parse is started via {#startParse(C container)}, but this implementation allows {@link #asSource(JsonValue)} to function.
	 */
	@Override
	public void parse(InputSource input) throws SAXException, IOException {
		JsonValue container;
		if (input instanceof XmlAlignerInputSource xmlAlignerInputSource) {
			container = xmlAlignerInputSource.container;
		} else {
			container = null;
		}
		if (log.isTraceEnabled()) log.trace("parse(InputSource) container [{}]", container);
		startParse(container);
	}

	/**
	 * Pushes node through validator.
	 *
	 * Must push all nodes through validatorhandler, recursively, respecting the alignment request.
	 * Must set current=node before calling validatorHandler.startElement(), in order to get the right argument for the onStartElement / performAlignment callbacks.
	 */
	public void handleRootNode(JsonValue container, String name, String nodeNamespace) throws SAXException {
		if (log.isTraceEnabled()) log.trace("handleNode() name [{}] namespace [{}]", name, nodeNamespace);
		if (StringUtils.isEmpty(nodeNamespace)) {
			nodeNamespace=null;
		}
		XSElementDeclaration elementDeclaration=findElementDeclarationForName(nodeNamespace,name);
		if (elementDeclaration==null) {
			throw new SAXException(MSG_CANNOT_NOT_FIND_ELEMENT_DECLARATION+" for ["+name+"] in namespace ["+nodeNamespace+"]");
		}
		handleElement(elementDeclaration, container);
	}

	public void handleElement(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		String name = elementDeclaration.getName();
		String elementNamespace=elementDeclaration.getNamespace();
		String qname=getQName(elementNamespace, name);
		if (log.isTraceEnabled()) log.trace("handleNode() name [{}] elementNamespace [{}]", name, elementNamespace);
		newLine();
		AttributesImpl attributes=new AttributesImpl();
		Map<String,String> nodeAttributes = getAttributes(elementDeclaration, node);
		if (log.isTraceEnabled()) log.trace("node [{}] search for attributeDeclaration", name);
		XSTypeDefinition typeDefinition=elementDeclaration.getTypeDefinition();
		XSObjectList attributeUses=getAttributeUses(typeDefinition);
		XSWildcard wildcard = typeDefinition instanceof XSComplexTypeDefinition xsctd ? xsctd.getAttributeWildcard():null;
		if ((attributeUses==null || attributeUses.getLength()==0) && wildcard==null) {
			if (nodeAttributes!=null && !nodeAttributes.isEmpty()) {
				log.warn("node [{}] found [{}] attributes, but no declared AttributeUses or wildcard", name, nodeAttributes.size());
			} else {
				if (log.isTraceEnabled()) log.trace("node [{}] no attributeUses or wildcard, no attributes", name);
			}
		} else {
			if (nodeAttributes==null || nodeAttributes.isEmpty()) {
				log.warn("node [{}] declared [{}] attributes, but no attributes found", name, attributeUses != null ? attributeUses.getLength() : 0);
			} else if (attributeUses != null) {
				for (int i=0;i<attributeUses.getLength(); i++) {
					XSAttributeUse attributeUse=(XSAttributeUse)attributeUses.item(i);
					XSAttributeDeclaration attributeDeclaration=attributeUse.getAttrDeclaration();
					String attName=attributeDeclaration.getName();
					if (nodeAttributes.containsKey(attName)) {
						String value=nodeAttributes.remove(attName);
						String uri=attributeDeclaration.getNamespace();
						String attqname=getQName(uri,attName);
						String type=null;
						if (log.isTraceEnabled()) log.trace("node [{}] adding attribute [{}] value [{}]", name, attName, value);
						attributes.addAttribute(uri, attName, attqname, type, value);
					}
				}
				if (wildcard!=null) {
					nodeAttributes.forEach((attName,value)-> {
						if (log.isTraceEnabled()) log.trace("node [{}] adding attribute [{}] value [{}] via wildcard", name, attName, value);
						attributes.addAttribute("", attName, attName, null, value);
					});
					nodeAttributes.clear();
				}
			}
		}
		if (isNil(node)) {
			validatorHandler.startPrefixMapping(XSI_PREFIX_MAPPING, XML_SCHEMA_INSTANCE_NAMESPACE);
			attributes.addAttribute(XML_SCHEMA_INSTANCE_NAMESPACE, XML_SCHEMA_NIL_ATTRIBUTE, XSI_PREFIX_MAPPING+":"+XML_SCHEMA_NIL_ATTRIBUTE, "xs:boolean", "true");
			validatorHandler.startElement(elementNamespace, name, qname, attributes);
			validatorHandler.endElement(elementNamespace, name, qname);
			validatorHandler.endPrefixMapping(XSI_PREFIX_MAPPING);
		} else {
			if (isMultipleOccurringChildElement(name) && node instanceof List<?>) {
				//noinspection unchecked
				for(JsonValue n:(List<JsonValue>)node) {
					doHandleElement(elementDeclaration, n, elementNamespace, name, qname, attributes);
				}
			} else {
				doHandleElement(elementDeclaration, node, elementNamespace, name, qname, attributes);
			}
		}
	}

	private void doHandleElement(XSElementDeclaration elementDeclaration, JsonValue node, String elementNamespace, String name, String qname, Attributes attributes) throws SAXException {
		validatorHandler.startElement(elementNamespace, name, qname, attributes);
		handleElementContents(elementDeclaration, node);
		validatorHandler.endElement(elementNamespace, name, qname);
	}

	protected void handleComplexTypedElement(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		String name = elementDeclaration.getName();
		if (log.isTraceEnabled()) log.trace("ToXml.handleComplexTypedElement() search for best path for available children of element [{}]", name);
		List<XSParticle> childParticles = getBestChildElementPath(elementDeclaration, node, false);
		if (log.isTraceEnabled()) {
			if (childParticles.isEmpty()) {
				log.trace("Examined node [{}] deepSearch [{}] path found is empty", name, isDeepSearch());
			} else {
				log.trace("Examined node [{}] deepSearch [{}] found path length [{}]: {}", ()->name, this::isDeepSearch, childParticles::size, () -> childParticles.stream()
						.map(XSParticle::getTerm)
						.map(XSObject::getName)
						.collect(Collectors.joining(", ")));
			}
		}
		Set<String> processedChildren = new HashSet<>();

		for (int i = 0; i < childParticles.size(); i++) {
			XSParticle childParticle = childParticles.get(i);
			XSElementDeclaration childElementDeclaration = (XSElementDeclaration) childParticle.getTerm();
			if (log.isTraceEnabled()) log.trace("ToXml.handleComplexTypedElement() processing child [{}], name [{}]", i, childElementDeclaration.getName());
			processChildElement(node, name, childElementDeclaration, childParticle.getMinOccurs() > 0, processedChildren);
		}

		Set<String> unProcessedChildren = getUnprocessedChildElementNames(node, processedChildren);

		if (unProcessedChildren!=null && !unProcessedChildren.isEmpty()) {
			Set<String> unProcessedChildrenWorkingCopy=new LinkedHashSet<>(unProcessedChildren);
			log.warn("processing [{}] unprocessed child elements{}", unProcessedChildren.size(), !unProcessedChildren.isEmpty() ? ", first [" + unProcessedChildren.iterator()
					.next() + "]" : "");
			// this loop is required to handle for mixed content element containing globally defined elements
			for (String childName:unProcessedChildrenWorkingCopy) {
				log.warn("processing unprocessed child element [{}]", childName);
				XSElementDeclaration childElementDeclaration = findElementDeclarationForName(null,childName);
				if (childElementDeclaration==null) {
					// this clause is hit for mixed content element containing elements that are not defined
					if (isTypeContainsWildcard()) {
						XSElementDecl elementDeclarationStub = new XSElementDecl();
						elementDeclarationStub.fName=childName;
						childElementDeclaration = elementDeclarationStub;
					} else {
						handleRecoverableError(MSG_CANNOT_NOT_FIND_ELEMENT_DECLARATION+" ["+childName+"] in the definition of type [" + name + "]", isIgnoreUndeclaredElements());
						continue;
					}
				}
				processChildElement(node, name, childElementDeclaration, false, processedChildren);
			}
		}
		// the below is used for mixed content nodes containing text
		if (processedChildren.isEmpty()) {
			if (log.isTraceEnabled()) log.trace("ToXml.handleComplexTypedElement() handle element [{}] as simple, because no children processed", name);
			handleSimpleTypedElement(elementDeclaration, node);
		}

	}

	protected void handleSimpleTypedElement(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		String text = getText(elementDeclaration, node);
		if (log.isTraceEnabled()) log.trace("textnode name [{}] text [{}]", elementDeclaration.getName(), text);
		if (StringUtils.isNotEmpty(text)) {
			sendString(text);
		}
	}

	private boolean tryDeepSearchForChildElement(XSElementDeclaration childElementDeclaration, boolean mandatory, JsonValue node, Set<String> processedChildren) throws SAXException {
		// Steps for deep search:
		//  - Create copy of node N that only contains child node that are allowed in the XSD declaration for the childElement which we
		//    are trying to instantiate from the "deep search", so that there are no errors from unprocessed elements.
		//
		//  - Do not copy any elements that are already processed.
		//    This is so that elements that can be placed in multiple places in the XML are not inserted multiple times, when the
		//    input contains them only a single time.
		//
		//  - To be able to handle substitutions from parameters or session variables being inserted, we should add to the copy node
		//    also any substitutions with same name as any of the names that are also in the XSD for this type
		//
		//  - If the copy of the node is not empty, then call handleElement for the copy and return true
		//  - else return false
		XSTypeDefinition typeDefinition = childElementDeclaration.getTypeDefinition();
		if (!(typeDefinition instanceof XSComplexTypeDefinition complexTypeDefinition)) {
			return false;
		}
		Set<String> allowedNames = getNamesOfXsdChildElements(complexTypeDefinition);
		allowedNames.removeAll(processedChildren);

		JsonValue copy = filterNodeChildren(node, allowedNames);

		if (isEmptyNode(copy) && !mandatory) {
			return false;
		}
		handleElement(childElementDeclaration, copy);
		return true;
	}

	public @Nonnull List<XSParticle> getBestChildElementPath(XSElementDeclaration elementDeclaration, JsonValue node, boolean silent) throws SAXException {
		XSTypeDefinition typeDefinition = elementDeclaration.getTypeDefinition();
		if (typeDefinition==null) {
			log.warn("getBestChildElementPath typeDefinition is null");
			return Collections.emptyList();
		}
		switch (typeDefinition.getTypeCategory()) {
		case XSTypeDefinition.SIMPLE_TYPE:
			if (log.isTraceEnabled()) log.trace("getBestChildElementPath typeDefinition.typeCategory is SimpleType, no child elements");
			return Collections.emptyList();
		case XSTypeDefinition.COMPLEX_TYPE:
			XSComplexTypeDefinition complexTypeDefinition=(XSComplexTypeDefinition)typeDefinition;
			switch (complexTypeDefinition.getContentType()) {
			case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
				if (log.isTraceEnabled()) log.trace("getBestChildElementPath complexTypeDefinition.contentType is Empty, no child elements");
				return Collections.emptyList();
			case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
				if (log.isTraceEnabled()) log.trace("getBestChildElementPath complexTypeDefinition.contentType is Simple, no child elements (only characters)");
				return Collections.emptyList();
			case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
			case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
				XSParticle particle = complexTypeDefinition.getParticle();
				if (particle==null) {
					throw new IllegalStateException("getBestChildElementPath complexTypeDefinition.particle is null for Element or Mixed contentType");
				}
				if (log.isTraceEnabled())
					log.trace("typeDefinition particle [{}]", ToStringBuilder.reflectionToString(particle, ToStringStyle.MULTI_LINE_STYLE));
				List<XSParticle> result=new ArrayList<>();
				List<String> failureReasons=new ArrayList<>();
				if (getBestMatchingElementPath(elementDeclaration, node, particle, result, failureReasons)) {
					return result;
				}
				if (!silent) {
					handleError("Cannot find path:" + String.join("\n", failureReasons));
				}
				return Collections.emptyList();
			default:
				throw new IllegalStateException("getBestChildElementPath complexTypeDefinition.contentType is not Empty,Simple,Element or Mixed, but ["+complexTypeDefinition.getContentType()+"]");
			}
		default:
			throw new IllegalStateException("getBestChildElementPath typeDefinition.typeCategory is not SimpleType or ComplexType, but ["+typeDefinition.getTypeCategory()+"] class ["+typeDefinition.getClass().getName()+"]");
		}
	}

	/**
	 *
	 * @param baseElementDeclaration XSD Type Declaration of the base element
	 * @param baseNode Node from which to search for path   
	 * @param particle XSD Particle for which to search for path
	 * @param failureReasons returns the reasons why no match was found
	 * @param path in this list the longest list of child elements, that matches the available, is maintained. Null if no matching.
	 * @return true when a matching path is found. if false, failureReasons will contain reasons why.
	 * @throws SAXException If there was any exception
 	 */
	public boolean getBestMatchingElementPath(XSElementDeclaration baseElementDeclaration, JsonValue baseNode, XSParticle particle, List<XSParticle> path, List<String> failureReasons) throws SAXException {
		if (particle==null) {
			throw new NullPointerException("getBestMatchingElementPath particle is null");
		}
		XSTerm term = particle.getTerm();
		if (term == null) {
			throw new NullPointerException("getBestMatchingElementPath particle.term is null");
		}
		if (term instanceof XSModelGroup xsModelGroup) {
			return handleModelGroupTerm(baseElementDeclaration, baseNode, path, failureReasons, xsModelGroup);
		}
		if (term instanceof XSElementDeclaration xsElementDeclaration) {
			return handleElementDeclarationTerm(baseNode, particle, path, failureReasons, xsElementDeclaration);
		}
		if (term instanceof XSWildcard xsWildcard) {
			return handleWildcardTerm(baseElementDeclaration, xsWildcard);
		}
		throw new IllegalStateException("getBestMatchingElementPath unknown Term type ["+term.getClass().getName()+"]");
	}

	private boolean handleElementDeclarationTerm(JsonValue baseNode, XSParticle particle, List<XSParticle> path, List<String> failureReasons, XSElementDeclaration elementDeclaration) throws SAXException {
		String elementName=elementDeclaration.getName();
		if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration name [{}]", elementName);
		if (!hasChild(baseNode, elementName)) {
			if (isDeepSearch()) {
				if (log.isTraceEnabled())
					log.trace("getBestMatchingElementPath().XSElementDeclaration element [{}] not found, perform deep search", elementName);
				try {
					List<XSParticle> subList=getBestChildElementPath(elementDeclaration, baseNode, true);
					if (!subList.isEmpty()) {
						path.add(particle);
						if (log.isTraceEnabled())
							log.trace("getBestMatchingElementPath().XSElementDeclaration element [{}] not found, nested elements found in deep search", elementName);
						return true;
					}
					if (log.isTraceEnabled())
						log.trace("getBestMatchingElementPath().XSElementDeclaration element [{}] not found, no nested elements found in deep search", elementName);
				} catch (Exception e) {
					if (log.isTraceEnabled())
						log.trace("getBestMatchingElementPath().XSElementDeclaration element [{}] not found, no nested elements found in deep search: {}", elementName, e.getMessage());
					return false;
				}
			}
			if (particle.getMinOccurs()>0) {
				failureReasons.add(MSG_EXPECTED_ELEMENT+" ["+elementName+"]");
				return false;
			}
			if (log.isTraceEnabled())
				log.trace("getBestMatchingElementPath().XSElementDeclaration optional element [{}] not found, path continues", elementName);
			return true;
		}
		for (XSParticle resultParticle: path) {
			if (elementName.equals(resultParticle.getTerm().getName())) {
				if (log.isTraceEnabled())
					log.trace("getBestMatchingElementPath().XSElementDeclaration element [{}] found but required multiple times", elementName);
				failureReasons.add("element ["+elementName+"] required multiple times");
				return false;
			}
		}
		if (log.isTraceEnabled())
			log.trace("getBestMatchingElementPath().XSElementDeclaration element [{}] found", elementName);
		path.add(particle);
		return true;
	}

	private boolean handleModelGroupTerm(XSElementDeclaration baseElementDeclaration, JsonValue baseNode, List<XSParticle> path, List<String> failureReasons, XSModelGroup modelGroup) throws SAXException {
		short compositor = modelGroup.getCompositor();
		XSObjectList particles = modelGroup.getParticles();
		if (log.isTraceEnabled())
			log.trace("getBestMatchingElementPath() modelGroup particles [{}]", ToStringBuilder.reflectionToString(particles, ToStringStyle.MULTI_LINE_STYLE));
		switch (compositor) {
		case XSModelGroup.COMPOSITOR_SEQUENCE:
		case XSModelGroup.COMPOSITOR_ALL:
			for (int i=0;i<particles.getLength();i++) {
				XSParticle childParticle = (XSParticle)particles.item(i);
				if (!getBestMatchingElementPath(baseElementDeclaration, baseNode, childParticle, path, failureReasons)) {
					return false;
				}
			}
			return true;
		case XSModelGroup.COMPOSITOR_CHOICE:
			List<XSParticle> bestPath=null;

			List<String> choiceFailureReasons = new ArrayList<>();
			for (int i=0;i<particles.getLength();i++) {
				XSParticle childParticle = (XSParticle)particles.item(i);
				List<XSParticle> optionPath=new ArrayList<>(path);

				if (getBestMatchingElementPath(baseElementDeclaration, baseNode, childParticle, optionPath, choiceFailureReasons)
						&& (bestPath == null || bestPath.size() < optionPath.size())) {
					bestPath = optionPath;
				}
			}
			if (bestPath==null) {
				failureReasons.addAll(choiceFailureReasons);
				return false;
			}
			if (log.isTraceEnabled()) log.trace("Replace path with best path of Choice Compositor, size [{}]", bestPath.size());
			path.clear();
			path.addAll(bestPath);
			return true;
		default:
			throw new IllegalStateException("getBestMatchingElementPath modelGroup.compositor is not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE, but ["+compositor+"]");
		}
	}

	private boolean handleWildcardTerm(XSElementDeclaration baseElementDeclaration, XSWildcard wildcard) {
		String processContents = switch (wildcard.getProcessContents()) {
			case XSWildcard.PC_LAX -> "LAX";
			case XSWildcard.PC_SKIP -> "SKIP";
			case XSWildcard.PC_STRICT -> "STRICT";
			default ->
					throw new IllegalStateException("getBestMatchingElementPath wildcard.processContents is not PC_LAX, PC_SKIP or PC_STRICT, but [" + wildcard.getProcessContents() + "]");
		};
		String namespaceConstraint = switch (wildcard.getConstraintType()) {
			case XSWildcard.NSCONSTRAINT_ANY -> "ANY";
			case XSWildcard.NSCONSTRAINT_LIST -> "SKIP " + wildcard.getNsConstraintList();
			case XSWildcard.NSCONSTRAINT_NOT -> "NOT " + wildcard.getNsConstraintList();
			default ->
					throw new IllegalStateException("getBestMatchingElementPath wildcard.namespaceConstraint is not ANY, LIST or NOT, but [" + wildcard.getConstraintType() + "]");
		};
		String msg="term for element ["+ baseElementDeclaration.getName()+"] is WILDCARD; namespaceConstraint ["+namespaceConstraint+"] processContents ["+processContents+"]. Please check if the element typed properly in the schema";
		if (isFailOnWildcards()) {
			throw new IllegalStateException(msg+", or set failOnWildcards=\"false\"");
		} else {
			log.warn(msg);
		}
		return true;
	}

	protected void sendString(String string) throws SAXException {
		validatorHandler.characters(string.toCharArray(), 0, string.length());
	}

	public void handleError(String msg) throws SAXException {
		ErrorHandler errorHandler=validatorHandler.getErrorHandler();
		if (errorHandler!=null) {
			errorHandler.error(new SAXParseException(msg,null));
		} else {
			throw new SAXException(msg);
		}
	}

	public void handleError(SAXException e) throws SAXException {
		ErrorHandler errorHandler=validatorHandler.getErrorHandler();
		if (errorHandler!=null) {
			errorHandler.error(new SAXParseException(e.getMessage(),null));
		} else {
			throw e;
		}
	}

	public String getQName(String namespace, String name) throws SAXException {
		if (StringUtils.isNotEmpty(namespace)) {
			String prefix=getNamespacePrefix(namespace);
			return prefix+":"+name;
		}
		return name;
	}

	public String getNamespacePrefix(String uri) throws SAXException {
		String prefix=prefixMap.get(uri);
		if (prefix==null) {
			prefix = NAMESPACE_PREFIX + prefixPrefixCounter++;
			prefixMap.put(uri, prefix);
			validatorHandler.startPrefixMapping(prefix, uri);
		}
		return prefix;
	}

	public void translate(JsonValue data, ContentHandler handler) throws SAXException {
		setContentHandler(handler);
		startParse(data);
	}

	public String translate(JsonValue data) throws SAXException {
		XmlWriter xmlWriter = new XmlWriter();
		translate(data, xmlWriter);
		return xmlWriter.toString();
	}

	private static class XmlAlignerInputSource extends InputSource {
		JsonValue container;
		XmlAlignerInputSource(JsonValue container) {
			super();
			this.container=container;
		}
	}
}
