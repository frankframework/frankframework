/*
   Copyright 2017,2018 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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

import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.SAXException;

/**
 * XML Schema guided JSON to XML converter;
 * 
 * @author Gerrit van Brakel
 */
public class Json2Xml extends Tree2Xml<JsonValue,JsonValue> {

	public static final String MSG_FULL_INPUT_IN_STRICT_COMPACTING_MODE="straight json found while expecting compact arrays and strict syntax checking";
	public static final String MSG_EXPECTED_SINGLE_ELEMENT="did not expect array, but single element";
	
	private boolean insertElementContainerElements;
	private boolean strictSyntax;
	private boolean readAttributes=true;
	private String attributePrefix="@";
	private String mixedContentLabel="#text";

	public Json2Xml(ValidatorHandler validatorHandler, boolean insertElementContainerElements, String rootElement) {
		this(validatorHandler, insertElementContainerElements, rootElement, false);
	}

	public Json2Xml(ValidatorHandler validatorHandler, boolean insertElementContainerElements, String rootElement, boolean strictSyntax) {
		super(validatorHandler);
		this.insertElementContainerElements=insertElementContainerElements;
		this.strictSyntax=strictSyntax;
		setRootElement(rootElement);
	}

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
			if (StringUtils.isEmpty(getRootElement())) {
				if (root.isEmpty()) {
					throw new SAXException("no names found");
				}
				if (root.size()>1) {
					String namesList=null;
					int i=0;
					for (String name:root.keySet()) {
						if (namesList==null) {
							namesList=name;
						} else {
							namesList+=","+name;
						}
						if (i++>5) {
							namesList+=", ...";
							break;
						}
					}
					throw new SAXException("too many names ["+namesList+"]");
				}
				setRootElement((String)root.keySet().toArray()[0]);
			} 
			// determine somewhat heuristically whether the json contains a 'root' node:
			// if the outermost JsonObject contains only one key, that has the name of the root element, 
			// then we'll assume that that is the root element...
			if (root.size()==1 && getRootElement().equals(root.keySet().toArray()[0])) {
				node=root.get(getRootElement());
			}
		}
		if (node instanceof JsonArray && !insertElementContainerElements && strictSyntax) {
			throw new SAXException(MSG_EXPECTED_SINGLE_ELEMENT+" ["+getRootElement()+"] or array element container");
		}
		super.startParse(node);
	}
	
	@Override
	public JsonValue getRootNode(JsonValue container) {
		return container;
	}

	@Override
	public void handleElementContents(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		if (node instanceof JsonObject) {
			JsonObject object = (JsonObject)node;
			if (object.containsKey(mixedContentLabel)) {
				JsonValue labelValue = object.get(mixedContentLabel);
				super.handleElementContents(elementDeclaration, labelValue);
				return;
			}
		} 
		super.handleElementContents(elementDeclaration, node);
	}


	@Override
	public String getNodeText(XSElementDeclaration elementDeclaration, JsonValue node) {
		String result;
		if (node instanceof JsonString) {
			result=((JsonString)node).getString();
		} else if (node instanceof JsonStructure) { // this happens when override key is present without a value
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
			Map<String, String> result=new HashMap<String,String>();
			for (String key:o.keySet()) {
				if (key.startsWith(attributePrefix)) {
					String attributeName=key.substring(attributePrefix.length());
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
					Set<String> result = new HashSet<String>();
					result.addAll(getMultipleOccurringChildElements());
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
				return new HashSet<String>();
			}
			Set<String> result = new HashSet<String>(); 
			for (String key:o.keySet()) {
				if (!readAttributes || !key.startsWith(attributePrefix)) {
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
			List<JsonValue> result = new LinkedList<JsonValue>(); 
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
		Object substs = sp.getSubstitutionsFor(getContext(), childName);
		if (substs==null) {
			return null;
		}
		if (substs instanceof List) {
			JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
			for (Object item:(List)substs) {
				arrayBuilder.add(item.toString());
			}
			return arrayBuilder.build();
		}
		JsonObjectBuilder objectBuilder = Json.createObjectBuilder();
		objectBuilder.add(childName, substs.toString());
		return objectBuilder.build().getJsonString(childName);
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
	
	public static String translate(String json, URL schemaURL, boolean compactJsonArrays, String rootElement, String targetNamespace) throws SAXException {
		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return translate(jsonStructure, schemaURL, compactJsonArrays, rootElement, targetNamespace);
	}
	public static String translate(JsonStructure jsonStructure, URL schemaURL, boolean compactJsonArrays, String rootElement, String targetNamespace) throws SAXException {
		return translate(jsonStructure, schemaURL, compactJsonArrays, rootElement, false, false, targetNamespace, null);
	}
	
	public static String translate(JsonStructure json, URL schemaURL, boolean compactJsonArrays, String rootElement, boolean strictSyntax, boolean deepSearch, String targetNamespace, Map<String,Object> overrideValues) throws SAXException {
		ValidatorHandler validatorHandler = getValidatorHandler(schemaURL);
		List<XSModel> schemaInformation = getSchemaInformation(schemaURL);

		// create the validator, setup the chain
		Json2Xml j2x = new Json2Xml(validatorHandler,schemaInformation,compactJsonArrays,rootElement,strictSyntax);
		if (overrideValues!=null) {
			j2x.setOverrideValues(overrideValues);
		}
		if (targetNamespace!=null) {
			//if (DEBUG) System.out.println("setting targetNamespace ["+targetNamespace+"]");
			j2x.setTargetNamespace(targetNamespace);
		}
		j2x.setDeepSearch(deepSearch);

		return j2x.translate(json);
	}

	public boolean isReadAttributes() {
		return readAttributes;
	}
	public void setReadAttributes(boolean readAttributes) {
		this.readAttributes = readAttributes;
	}

}
