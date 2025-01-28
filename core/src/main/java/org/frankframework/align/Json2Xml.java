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

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.validation.ValidatorHandler;

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
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
import org.xml.sax.SAXException;

/**
 * XML Schema guided JSON to XML converter;
 *
 * @author Gerrit van Brakel
 */
public class Json2Xml extends Tree2Xml<JsonValue,JsonValue> {

	public static final String MSG_FULL_INPUT_IN_STRICT_COMPACTING_MODE="straight json found while expecting compact arrays and strict syntax checking";
	public static final String MSG_EXPECTED_SINGLE_ELEMENT="did not expect array, but single element";
	public static final String XSD_WILDCARD_ELEMENT_TOKEN = "*";

	private boolean insertElementContainerElements;
	private boolean strictSyntax;
	private @Getter @Setter boolean readAttributes=true;
	private static final String ATTRIBUTE_PREFIX = "@";
	private static final String MIXED_CONTENT_LABEL = "#text";

	public Json2Xml(ValidatorHandler validatorHandler, List<XSModel> schemaInformation, boolean insertElementContainerElements, String rootElement) {
		this(validatorHandler, schemaInformation, insertElementContainerElements, rootElement, false);
	}

	public Json2Xml(ValidatorHandler validatorHandler, List<XSModel> schemaInformation, boolean insertElementContainerElements, String rootElement, boolean strictSyntax) {
		super(validatorHandler, schemaInformation);
		this.insertElementContainerElements=insertElementContainerElements;
		this.strictSyntax=strictSyntax;
		setRootElement(rootElement);
	}

	@Override
	public void startParse(JsonValue node) throws SAXException {
		if (node instanceof JsonObject) {
			JsonObject root = (JsonObject)node;
			List<String> potentialRootElements = new ArrayList<>(root.keySet());
			potentialRootElements.removeIf(e-> {return e.startsWith(ATTRIBUTE_PREFIX) || e.startsWith(MIXED_CONTENT_LABEL);});
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
		super.startParse(node);
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

	@Override
	public JsonValue getRootNode(JsonValue container) {
		return container;
	}

	@Override
	public void handleElementContents(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		if (node instanceof JsonObject) {
			JsonObject object = (JsonObject)node;
			if (object.containsKey(MIXED_CONTENT_LABEL)) {
				JsonValue labelValue = object.get(MIXED_CONTENT_LABEL);
				super.handleElementContents(elementDeclaration, labelValue);
				return;
			}
		}
		super.handleElementContents(elementDeclaration, node);
	}


	@Override
	public String getNodeText(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		String result;
		if (node instanceof JsonString) {
			result = ((JsonString) node).getString();
		} else if (node instanceof JsonArray) {
			throw new SAXException("Expected simple element, got instead an array-value: [" + node + "]");
		} else if (node instanceof JsonStructure) { // this happens when override key is present without a value (?? Is that so?)
			result=null;
		} else {
			result=node.toString();
		}
		if ("{}".equals(result)) {
			result="";
		}
		if (log.isTraceEnabled()) log.trace("node ["+ToStringBuilder.reflectionToString(node)+"] = ["+result+"]");
		return result;
	}

	@Override
	public boolean isNil(XSElementDeclaration elementDeclaration, JsonValue node) {
		boolean result=node==JsonValue.NULL;
		if (log.isTraceEnabled()) log.trace("node ["+node+"] = ["+result+"]");
		return result;
	}

	@Override
	public Map<String, String> getAttributes(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		if (!readAttributes) {
			return null;
		}
		if (!(node instanceof JsonObject)) {
			if (log.isTraceEnabled()) log.trace("parent node is not a JsonObject, but a ["+node.getClass().getName()+"] isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"]  value ["+node+"], returning null");
			return null;
		}
		JsonObject o = (JsonObject)node;
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
					if (log.isTraceEnabled()) log.trace("getAttributes() attribute ["+attributeName+"] = ["+value+"]");
					result.put(attributeName, value);
				}
			}
			return result;
		} catch (JsonException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public boolean hasChild(XSElementDeclaration elementDeclaration, JsonValue node, String childName) throws SAXException {
		if (isParentOfSingleMultipleOccurringChildElement() && (insertElementContainerElements || !strictSyntax)) {
			// The array element can always considered to be present; if it is not, it will be inserted
			return true;
		}
		return super.hasChild(elementDeclaration, node, childName);
	}

	@Override
	public Set<String> getAllNodeChildNames(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		if (log.isTraceEnabled()) log.trace("node isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"] ["+node.getClass().getName()+"]["+node+"]");
		try {
			if (isParentOfSingleMultipleOccurringChildElement()) {
				if ((insertElementContainerElements || !strictSyntax) && node instanceof JsonArray) {
					if (log.isTraceEnabled()) log.trace("parentOfSingleMultipleOccurringChildElement,JsonArray,(insertElementContainerElements || !strictSyntax)");
					Set<String> result = new LinkedHashSet<>(getMultipleOccurringChildElements());
					if (log.isTraceEnabled()) log.trace("isParentOfSingleMultipleOccurringChildElement, result ["+result+"]");
					return result;
				}

				if ((insertElementContainerElements && strictSyntax) && !(node instanceof JsonArray)) {
					throw new SAXException(MSG_FULL_INPUT_IN_STRICT_COMPACTING_MODE);
				}
			}
			if (!(node instanceof JsonObject)) {
				if (log.isTraceEnabled()) log.trace("parent node is not a JsonObject, but a ["+node.getClass().getName()+"] isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"]  value ["+node+"], returning null");
				return null;
			}
			JsonObject o = (JsonObject)node;
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
			if (log.isTraceEnabled()) log.trace("returning ["+result+"]");
			return result;
		} catch (JsonException e) {
			throw new SAXException(e);
		}
	}


	@Override
	public Iterable<JsonValue> getNodeChildrenByName(JsonValue node, XSElementDeclaration childElementDeclaration) throws SAXException {
		String name=childElementDeclaration.getName();
		if (log.isTraceEnabled()) log.trace("childname ["+name+"] parent isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"] isMultipleOccuringChildElement ["+isMultipleOccurringChildElement(name)+"] node ["+node+"]");
		try {
			if (!(node instanceof JsonObject)) {
				if (log.isTraceEnabled()) log.trace("parent node is not a JsonObject, but a ["+node.getClass().getName()+"]");
				return null;
			}
			JsonObject o = (JsonObject)node;
			if (!o.containsKey(name)) {
				if (log.isTraceEnabled()) log.trace("no children named ["+name+"] node ["+node+"]");
				return null;
			}
			JsonValue child = o.get(name);
			List<JsonValue> result = new LinkedList<>();
			if (child instanceof JsonArray) {
				if (log.isTraceEnabled()) log.trace("child named ["+name+"] is a JsonArray, current node insertElementContainerElements ["+insertElementContainerElements+"]");
				// if it could be necessary to insert elementContainers, we cannot return them as a list of individual elements now, because then the containing element would be duplicated
				// we also cannot use the isSingleMultipleOccurringChildElement, because it is not valid yet
				if (!isMultipleOccurringChildElement(name)) {
					if (insertElementContainerElements || !strictSyntax) {
						result.add(child);
						if (log.isTraceEnabled()) log.trace("singleMultipleOccurringChildElement ["+name+"] returning array node (insertElementContainerElements=true)");
					} else {
						throw new SAXException(MSG_EXPECTED_SINGLE_ELEMENT+" ["+name+"]");
					}
				} else {
					if (log.isTraceEnabled()) log.trace("childname ["+name+"] returning elements of array node (insertElementContainerElements=false or not singleMultipleOccurringChildElement)");
					result.addAll((JsonArray)child);
				}
				return result;
			}
			result.add(child);
			if (log.isTraceEnabled()) log.trace("name ["+name+"] returning ["+child+"]");
			return result;
		} catch (JsonException e) {
			throw new SAXException(e);
		}
	}


	@Override
	protected JsonValue getSubstitutedChild(JsonValue node, String childName) {
		if (!sp.hasSubstitutionsFor(getContext(), childName)) {
			return null;
		}
		Object substs = sp.getSubstitutionsFor(getContext(), childName);
		if (substs==null) {
			substs="{}";
		}
		if (substs instanceof List) {
			JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
			for (Object item:(List<?>)substs) {
				arrayBuilder.add(item.toString());
			}
			return arrayBuilder.build();
		}
		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
		objectBuilder.add(childName, substs.toString());
		return objectBuilder.build().getJsonString(childName);
	}

	@Override
	protected String getOverride(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		Object text = sp.getOverride(getContext());
		if (text instanceof List) {
			// if the override is a List, than it has already be substituted via getSubstitutedChild.
			// Therefore now get the node text, which is here an individual element already.
			return getNodeText(elementDeclaration, node);
		}
		if (text instanceof String) {
			return (String)text;
		}
		return text.toString();
	}

	@Override
	protected void processChildElement(JsonValue node, String parentName, XSElementDeclaration childElementDeclaration, boolean mandatory, Set<String> processedChildren) throws SAXException {
		String childElementName=childElementDeclaration.getName();
		if (log.isTraceEnabled()) log.trace("parentName ["+parentName+"] childElementName ["+childElementName+"] node ["+node+"] isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"]");
		if (isParentOfSingleMultipleOccurringChildElement()) {
			if (node instanceof JsonArray) {
				if (log.isTraceEnabled()) log.trace("array child node is JsonArray, handling each of the elements as a ["+childElementName+"]");
				JsonArray ja=(JsonArray)node;
				for (JsonValue child:ja) {
					handleElement(childElementDeclaration, child);
				}
				// mark that we have processed the array elements
				processedChildren.add(childElementName);
				return;
			}
			if (node instanceof JsonString) { // support normal (non list) parameters to supply array element values
				if (log.isTraceEnabled()) log.trace("array child node is JsonString, handling as a ["+childElementName+"]");
				handleElement(childElementDeclaration, node);
				// mark that we have processed the array element
				processedChildren.add(childElementName);
				return;
			}
		}
		super.processChildElement(node, parentName, childElementDeclaration, mandatory, processedChildren);
	}

	@Override
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
	 * Create a copy of the JSON node that contains only keys from the allowedChildren set in the top level.
	 *
	 * @param node Node to copy
	 * @param allowedChildren Names of child-nodes to keep in the copy
	 * @return Copy of the JSON node.
	 */
	@Override
	protected JsonValue filterNodeChildren(JsonValue node, List<XSParticle> allowedChildren) {
		if (node instanceof JsonArray) {
			return copyJsonArray((JsonArray)node, allowedChildren);
		} else if (node instanceof JsonObject) {
			return copyJsonObject((JsonObject)node, allowedChildren);
		} else return node;
	}

	private JsonValue copyJsonObject(JsonObject node, List<XSParticle> allowedChildren) {
		Set<String> allowedNames = allowedChildren
				.stream()
				.map(p -> p.getTerm().getName())
				.collect(Collectors.toSet());
		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
		node.forEach((key, value) -> {
			if (allowedNames.contains(key)) objectBuilder.add(key, value);
		});
		// Add in substitutions for allowed names not already in the object. This is so objects do not appear empty when
		// substitutions could fill in for absent names.
		// This is perhaps not the cleanest way to make sure the substitutions are performed but this requires the least
		// amount of code changes in other parts.
		if (sp != null) {
			allowedChildren.forEach(childParticle -> {
				String name = childParticle.getTerm().getName();
				if (!node.containsKey(name) && sp.hasSubstitutionsFor(getContext(), name)) {
					objectBuilder.add(name, getSubstitutedChild(node, name));
				} else if (hasSubstitutionForChild(childParticle)) {
					// A deeper child-node does have a substitution for this element, so add an empty object for it to further parse at later stage.
					objectBuilder.add(name, Json.createObjectBuilder().build());
				}
			});
		}
		return objectBuilder.build();
	}

	private boolean hasSubstitutionForChild(XSParticle childParticle) {
		// Find a recursive list of all child-names of this type to see if any of these names has a substitution from parameters
		Set<String> names = new HashSet<>();
		getChildElementNamesRecursive(childParticle, names, new HashSet<>());
		return names.contains(XSD_WILDCARD_ELEMENT_TOKEN) || names.stream().anyMatch(childName -> sp.hasSubstitutionsFor(getContext(), childName));
	}

	private void getChildElementNamesRecursive(XSParticle particle, Set<String> names, Set<XSParticle> visitedTypes) {
		XSTerm term = particle.getTerm();
		names.add(term.getName());
		if (visitedTypes.contains(particle)) {
			return;
		}
		visitedTypes.add(particle);
		if (term instanceof XSModelGroup) {
			XSObjectList modelGroupParticles = ((XSModelGroup)term).getParticles();
			for (Object childObject : modelGroupParticles) {
				XSParticle childParticle = (XSParticle) childObject;
				getChildElementNamesRecursive(childParticle, names, visitedTypes);
			}
		} else if (term instanceof XSElementDeclaration) {
			XSTypeDefinition typeDefinition = ((XSElementDeclaration)term).getTypeDefinition();
			if (typeDefinition.getTypeCategory()!=XSTypeDefinition.SIMPLE_TYPE) {
				XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition) typeDefinition;
				getChildElementNamesRecursive(complexTypeDefinition.getParticle(), names, visitedTypes);
			}
		} else if (term instanceof XSWildcard) {
			XSWildcard wildcard = (XSWildcard)term;
			log.debug("XSD contains wildcard element [{}], constraint [{}]/[{}]", term.getName(), wildcard.getConstraintType(), wildcard.getNsConstraintList());
			// TODO: Not sure what to do here to realistically restrict possible child-elements and I'm afraid it can balloon into a lot of unneeded code.
			names.add(XSD_WILDCARD_ELEMENT_TOKEN);
		}
	}

	private JsonValue copyJsonArray(JsonArray node, List<XSParticle> allowedChildren) {
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		node.forEach(value -> {
			arrayBuilder.add(filterNodeChildren(value, allowedChildren));
		});
		return arrayBuilder.build();
	}

	public static String translate(String json, URL schemaURL, boolean compactJsonArrays, String rootElement, String targetNamespace) throws SAXException {
		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return translate(jsonStructure, schemaURL, compactJsonArrays, rootElement, targetNamespace);
	}
	public static String translate(JsonStructure jsonStructure, URL schemaURL, boolean compactJsonArrays, String rootElement, String targetNamespace) throws SAXException {
		return translate(jsonStructure, schemaURL, compactJsonArrays, rootElement, false, false, targetNamespace, null);
	}

	public static String translate(JsonStructure json, URL schemaURL, boolean compactJsonArrays, String rootElement, boolean strictSyntax, boolean deepSearch, String targetNamespace, Map<String,Object> overrideValues) throws SAXException {
		Json2Xml j2x = create(schemaURL, compactJsonArrays, rootElement, strictSyntax, deepSearch, targetNamespace, overrideValues);
		return j2x.translate(json);
	}
	public static Json2Xml create(URL schemaURL, boolean compactJsonArrays, String rootElement, boolean strictSyntax, boolean deepSearch, String targetNamespace, Map<String,Object> overrideValues) throws SAXException {
		ValidatorHandler validatorHandler = getValidatorHandler(schemaURL);
		List<XSModel> schemaInformation = getSchemaInformation(schemaURL);

		// create the validator, setup the chain
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
}
