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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonStructure;
import javax.json.JsonValue;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
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
	
	private static final boolean DEBUG=false; 
	
	private boolean insertElementContainerElements;
	private boolean strictSyntax;
	private boolean readAttributes=true;
	private String attributePrefix="@";

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
	public String getNodeText(XSElementDeclaration elementDeclaration, JsonValue node) {
		String result;
		if (node instanceof JsonString) {
			result=((JsonString)node).getString();
		} else { 
			result=node.toString();
		}
		if ("{}".equals(result)) {
			result="";
		}
		if (DEBUG) log.debug("getText() node ["+ToStringBuilder.reflectionToString(node)+"] = ["+result+"]");
		return result;
	}

	@Override
	public boolean isNil(XSElementDeclaration elementDeclaration, JsonValue node) {
		boolean result=node==JsonValue.NULL;
		if (DEBUG) log.debug("isNil() node ["+node+"] = ["+result+"]");
		return result;
	}	
	
	@Override
	public Map<String, String> getAttributes(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		if (!readAttributes) {
			return null;
		}
		if (!(node instanceof JsonObject)) {
			if (DEBUG) log.debug("getAttributes() parent node is not a JsonObject, but a ["+node.getClass().getName()+"] isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"]  value ["+node+"], returning null");				
			return null;
		} 
		JsonObject o = (JsonObject)node;
		if (o.isEmpty()) {
			if (DEBUG) log.debug("getAttributes() no children");
			return null;
		}
		try {
			Map<String, String> result=new HashMap<String,String>();
			for (String key:o.keySet()) {
				if (key.startsWith(attributePrefix)) {
					String attributeName=key.substring(attributePrefix.length());
					String value=getText(elementDeclaration, o.get(key));
					if (DEBUG) log.debug("getAttributes() attribute ["+attributeName+"] = ["+value+"]");
					result.put(attributeName, value);
				}
			}
			return result;
		} catch (JsonException e) {
			throw new SAXException(e);
		}
	}

	
	@Override
	public Set<String> getAllNodeChildNames(XSElementDeclaration elementDeclaration, JsonValue node) throws SAXException {
		if (DEBUG) log.debug("getAllChildNames() node isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"] ["+node.getClass().getName()+"]["+node+"]");
		try {
			if (isParentOfSingleMultipleOccurringChildElement()) {
				if ((insertElementContainerElements || !strictSyntax) && node instanceof JsonArray) {
					if (DEBUG) log.debug("getAllChildNames() parentOfSingleMultipleOccurringChildElement,JsonArray,(insertElementContainerElements || !strictSyntax)");				
					Set<String> result = new HashSet<String>(); 
					result.addAll(getMultipleOccurringChildElements());
					if (DEBUG) log.debug("getAllChildNames() isParentOfSingleMultipleOccurringChildElement, result ["+result+"]");				
					return result;
				}
				if ((insertElementContainerElements && strictSyntax) && !(node instanceof JsonArray)) {
					throw new SAXException(MSG_FULL_INPUT_IN_STRICT_COMPACTING_MODE);
				}
			}
			if (!(node instanceof JsonObject)) {
				if (DEBUG) log.debug("getAllChildNames() parent node is not a JsonObject, but a ["+node.getClass().getName()+"] isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"]  value ["+node+"], returning null");				
				return null;
			} 
			JsonObject o = (JsonObject)node;
			if (o.isEmpty()) {
				if (DEBUG) log.debug("getAllChildNames() no children");
				return new HashSet<String>();
			}
			Set<String> result = new HashSet<String>(); 
			for (String key:o.keySet()) {
				if (!readAttributes || !key.startsWith(attributePrefix)) {
					result.add(key);
					if (DEBUG) log.debug("getAllChildNames() key ["+key+"] added to set");
				}
			}
			return result;
		} catch (JsonException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public Iterable<JsonValue> getNodeChildrenByName(JsonValue node, XSElementDeclaration childElementDeclaration) throws SAXException {
		String name=childElementDeclaration.getName();
		if (DEBUG) log.debug("getChildrenByName() childname ["+name+"] isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"] isMultipleOccuringChildElement ["+isMultipleOccurringChildElement(name)+"] node ["+node+"]");
		try {
			if (!(node instanceof JsonObject)) {
				if (DEBUG) log.debug("getChildrenByName() parent node is not a JsonObject, but a ["+node.getClass().getName()+"]");
				return null;
			} 
			JsonObject o = (JsonObject)node;
			if (!o.containsKey(name)) {
				if (DEBUG) log.debug("getChildrenByName() no children named ["+name+"] node ["+node+"]");
				return null;
			} 
			JsonValue child = o.get(name);
			List<JsonValue> result = new LinkedList<JsonValue>(); 
			if (child instanceof JsonArray) {
				if (DEBUG) log.debug("getChildrenByName() child named ["+name+"] is a JsonArray, current node insertElementContainerElements ["+insertElementContainerElements+"]");
				// if it could be necessary to insert elementContainers, we cannot return them as a list of individual elements now, because then the containing element would be duplicated
				// we also cannot use the isSingleMultipleOccurringChildElement, because it is not valid yet
				if (!isMultipleOccurringChildElement(name)) {
					if (insertElementContainerElements || !strictSyntax) { 
						result.add(child);
						if (DEBUG) log.debug("getChildrenByName() singleMultipleOccurringChildElement ["+name+"] returning array node (insertElementContainerElements=true)");
					} else {
						throw new SAXException(MSG_EXPECTED_SINGLE_ELEMENT+" ["+name+"]");
					}
				} else {
					if (DEBUG) log.debug("getChildrenByName() childname ["+name+"] returning elements of array node (insertElementContainerElements=false or not singleMultipleOccurringChildElement)");
					result.addAll((JsonArray)child);
				}
				return result;
			}
			result.add(child);
			if (DEBUG) log.debug("getChildrenByName() name ["+name+"] returning ["+child+"]");
			return result;
		} catch (JsonException e) {
			throw new SAXException(e);
		}
	}
	


	@Override
	protected void processChildElement(JsonValue node, String name, XSElementDeclaration childElementDeclaration, boolean mandatory, Set<String> processedChildren) throws SAXException {
		String childElementName=childElementDeclaration.getName();
		if  (node instanceof JsonArray) {
			if (DEBUG) log.debug("Json2Xml.processChildElement() node is JsonArray, handling each of the elements as a ["+name+"]");
			JsonArray ja=(JsonArray)node;
			for (JsonValue child:ja) {
				handleElement(childElementDeclaration, child);
			}
			// mark that we have processed the arrayElement containers
			processedChildren.add(childElementName);
			return;
		}
		super.processChildElement(node, name, childElementDeclaration, mandatory, processedChildren);
	}
	
	public static String translate(String json, URL schemaURL, boolean compactJsonArrays, String rootElement, String targetNamespace) throws SAXException, IOException {
		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return translate(jsonStructure, schemaURL, compactJsonArrays, rootElement, targetNamespace);
	}
	public static String translate(JsonStructure jsonStructure, URL schemaURL, boolean compactJsonArrays, String rootElement, String targetNamespace) throws SAXException, IOException {
//		JsonStructure jsonStructure = Json.createReader(new StringReader(json)).read();
		return translate(jsonStructure, schemaURL, compactJsonArrays, rootElement, false, false, targetNamespace, null);
	}
	
	public static String translate(JsonStructure json, URL schemaURL, boolean compactJsonArrays, String rootElement, boolean strictSyntax, boolean deepSearch, String targetNamespace, Map<String,Object> overrideValues) throws SAXException, IOException {

		// create the ValidatorHandler
    	SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = sf.newSchema(schemaURL); 
		ValidatorHandler validatorHandler = schema.newValidatorHandler();
 	
		// create the XSModel
		XMLSchemaLoader xsLoader = new XMLSchemaLoader();
		XSModel xsModel = xsLoader.loadURI(schemaURL.toExternalForm());
		List<XSModel> schemaInformation= new LinkedList<XSModel>();
		schemaInformation.add(xsModel);

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
    	Source source=j2x.asSource(json);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        String xml=null;
		try {
	        TransformerFactory tf = TransformerFactory.newInstance();
	        Transformer transformer = tf.newTransformer();
	        transformer.transform(source, result);
	        writer.flush();
	        xml = writer.toString();
		} catch (TransformerConfigurationException e) {
			SAXException se = new SAXException(e);
			se.initCause(e);
			throw se;
		} catch (TransformerException e) {
			SAXException se = new SAXException(e);
			se.initCause(e);
			throw se;
		}
    	return xml;
 	}

	public boolean isReadAttributes() {
		return readAttributes;
	}
	public void setReadAttributes(boolean readAttributes) {
		this.readAttributes = readAttributes;
	}

	public String getAttributePrefix() {
		return attributePrefix;
	}
	public void setAttributePrefix(String attributePrefix) {
		this.attributePrefix = attributePrefix;
	}

}
