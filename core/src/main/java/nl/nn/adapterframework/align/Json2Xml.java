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
import java.io.StringWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

/**
 * XML Schema guided JSON to XML converter;
 * 
 * @author Gerrit van Brakel
 */
public class Json2Xml extends Tree2Xml<Object> {

	public static final String MSG_FULL_INPUT_IN_STRICT_COMPACTING_MODE="straight json found while expecting compact arrays and strict syntax checking";
	public static final String MSG_EXPECTED_SINGLE_ELEMENT="did not expect array, but single element";
	
	private final boolean DEBUG=false; 
	
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
	public void startParse(Object node) throws SAXException {
		if (node instanceof JSONObject) {
			JSONObject root = (JSONObject)node;
			String names[] = JSONObject.getNames(root);
			if (StringUtils.isEmpty(getRootElement())) {
				if (names==null || names.length==0) {
					throw new SAXException("no names found");
				}
				if (names.length>1) {
					String namesList=names[0];
					for (int i=1;i<names.length; i++) {
						namesList+=","+names[i];
						if (i>5) {
							namesList+=", ...";
							break;
						}
					}
					throw new SAXException("too many names ["+namesList+"]");
				}
				setRootElement(names[0]);
			} 
			if (names.length==1 && getRootElement().equals(names[0])) {
				try {
					node=((JSONObject)node).get(getRootElement());
				} catch (JSONException e) {
					throw new SAXException(e);
				}
			}
		}
		if (node instanceof JSONArray && !insertElementContainerElements && strictSyntax) {
			throw new SAXException(MSG_EXPECTED_SINGLE_ELEMENT+" ["+getRootElement()+"] or array element container");
		}
		super.startParse(node);
	}
	

	@Override
	public String getText(Object node) {
		String result;
		if (node instanceof JSONObject) {
			result=((JSONObject)node).toString();
		} else { 
			result=node.toString();
		}
		if ("{}".equals(result)) {
			result="";
		}
		if (DEBUG) log.debug("getText() node ["+node+"] = ["+result+"]");
		return result;
	}

	@Override
	public boolean isNil(Object node) {
		boolean result=node == JSONObject.NULL;
		if (DEBUG) log.debug("isNil() node ["+node+"] = ["+result+"]");
		return result;
	}	
	
	@Override
	public Map<String, String> getAttributes(Object node) throws SAXException {
		if (!readAttributes) {
			return null;
		}
		if (!(node instanceof JSONObject)) {
			if (DEBUG) log.debug("getAttributes() parent node is not a JSONObject, but a ["+node.getClass().getName()+"] isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"]  value ["+node+"], returning null");				
			return null;
		} 
		JSONObject o = (JSONObject)node;
		JSONArray names= o.names();
		if (names==null) {
			if (DEBUG) log.debug("getAttributes() no children");
			return null;
		}
		try {
			Map<String, String> result=new HashMap<String,String>();
			for (int i=0;i<names.length();i++) {
				String name=(String)names.get(i);
				if (name.startsWith(attributePrefix)) {
					String attributeName=name.substring(attributePrefix.length());
					String value=o.getString(name);
					if (DEBUG) log.debug("getAttributes() attribute ["+attributeName+"] = ["+value+"]");
					result.put(attributeName, value);
				}
			}
			return result;
		} catch (JSONException e) {
			throw new SAXException(e);
		}
	}
	
	@Override
	public Set<String> getAllChildNames(Object node) throws SAXException {
		if (DEBUG) log.debug("getAllChildNames() node isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"] ["+node.getClass().getName()+"]["+node+"]");
		try {
			if (isParentOfSingleMultipleOccurringChildElement()) {
				if ((insertElementContainerElements || !strictSyntax) && node instanceof JSONArray) {
					if (DEBUG) log.debug("getAllChildNames() parentOfSingleMultipleOccurringChildElement,JSONArray,(insertElementContainerElements || !strictSyntax)");				
					Set<String> result = new HashSet<String>(); 
					result.addAll(getMultipleOccurringChildElements());
					if (DEBUG) log.debug("getAllChildNames() isParentOfSingleMultipleOccurringChildElement, result ["+result+"]");				
					return result;
				}
				if ((insertElementContainerElements && strictSyntax) && !(node instanceof JSONArray)) {
					throw new SAXException(MSG_FULL_INPUT_IN_STRICT_COMPACTING_MODE);
				}
			}
			if (!(node instanceof JSONObject)) {
				if (DEBUG) log.debug("getAllChildNames() parent node is not a JSONObject, but a ["+node.getClass().getName()+"] isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"]  value ["+node+"], returning null");				
				return null;
			} 
			JSONObject o = (JSONObject)node;
			JSONArray names= o.names();
			if (names==null) {
				if (DEBUG) log.debug("getAllChildNames() no children");
				return new HashSet<String>();
			}
			Set<String> result = new HashSet<String>(); 
			for (int i=0;i<names.length();i++) {
				String name=(String)names.get(i);
				if (!readAttributes || !name.startsWith(attributePrefix)) {
					result.add(name);
					if (DEBUG) log.debug("getAllChildNames() name ["+name+"] added to set");
				}
			}
			return result;
		} catch (JSONException e) {
			throw new SAXException(e);
		}
	}

	@Override
	public Iterable<Object> getChildrenByName(Object node, String name) throws SAXException {
		if (DEBUG) log.debug("getChildrenByName() childname ["+name+"] isParentOfSingleMultipleOccurringChildElement ["+isParentOfSingleMultipleOccurringChildElement()+"] isMultipleOccuringChildElement ["+isMultipleOccurringChildElement(name)+"] node ["+node+"]");
		try {
			if (!(node instanceof JSONObject)) {
				if (DEBUG) log.debug("getChildrenByName() parent node is not a JSONObject, but a ["+node.getClass().getName()+"]");
				return null;
			} 
			JSONObject o = (JSONObject)node;
			Object child = o.opt(name);
			if (child==null) {
				if (DEBUG) log.debug("getChildrenByName() no children named ["+name+"] node ["+node+"]");
				return null;
			} 
			List<Object> result = new LinkedList<Object>(); 
			if (child instanceof JSONArray) {
				if (DEBUG) log.debug("getChildrenByName() child named ["+name+"] is a JSONArray, current node insertElementContainerElements ["+insertElementContainerElements+"]");
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
					JSONArray ja = (JSONArray)child;
					for (int i=0;i<ja.length();i++) {
						result.add(ja.get(i));
						if (DEBUG) log.debug("getChildrenByName() childname ["+name+"] adding to array ["+ja.get(i)+"]");
					}
				}
				return result;
			}
			result.add(child);
			if (DEBUG) log.debug("getChildrenByName() name ["+name+"] returning ["+child+"]");
			return result;
		} catch (JSONException e) {
			throw new SAXException(e);
		}
	}
	


	@Override
	protected void processChildElement(Object node, String name, XSElementDeclaration childElementDeclaration, boolean mandatory, Set<String> unProcessedChildren, Set<String> processedChildren) throws SAXException {
		String childElementName=childElementDeclaration.getName();
		if  (node instanceof JSONArray) {
			if (DEBUG) log.debug("Json2Xml.processChildElement() node is JSONArray, handling each of the elements as a ["+name+"]");
			try {
				JSONArray ja=(JSONArray)node;
				for (int i=0;i<ja.length();i++) {
					handleNode(ja.get(i), childElementDeclaration);
				}
				// mark that we have processed the arrayElement containers
				processedChildren.add(childElementName);
				unProcessedChildren.remove(childElementName);
			} catch (JSONException e) {
				throw new SAXException(e);
			}
			return;
		}
		super.processChildElement(node, name, childElementDeclaration, mandatory, unProcessedChildren, processedChildren);
	}
	
	public static String translate(Object json, URL schemaURL, boolean compactJsonArrays, String rootElement, String targetNamespace) throws SAXException, IOException {
		return translate(json, schemaURL, compactJsonArrays, rootElement, false, targetNamespace);
	}
	public static String translate(Object json, URL schemaURL, boolean compactJsonArrays, String rootElement, boolean strictSyntax, String targetNamespace) throws SAXException, IOException {

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
		if (targetNamespace!=null) {
			j2x.setTargetNamespace(targetNamespace);
		}
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
