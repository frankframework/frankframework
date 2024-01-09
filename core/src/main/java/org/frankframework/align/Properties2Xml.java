/*
   Copyright 2017 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.validation.ValidatorHandler;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.SAXException;

import org.frankframework.align.Properties2Xml.PropertyNode;

/**
 * XML Schema guided JSON to XML converter;
 *
 * @author Gerrit van Brakel
 */
public class Properties2Xml extends Map2Xml<String,String,PropertyNode,Map<String,String>> {

	private final String attributeSeparator = ".";
//	private String indexSeparator=".";
	private final String valueSeparator = ",";

	private Map<String,String> data;

	protected static class PropertyNode {
		String value;
		Map<String,String> attributes;
	}

	public Properties2Xml(ValidatorHandler validatorHandler, List<XSModel> schemaInformation, String rootElement) {
		super(validatorHandler, schemaInformation);
		setRootElement(rootElement);
	}

	@Override
	public void startParse(Map<String, String> root) throws SAXException {
		data=root;
		super.startParse(root);
	}


	@Override
	public boolean hasChild(XSElementDeclaration elementDeclaration, PropertyNode node, String childName) {
		if (data.containsKey(childName) || node !=null && node.attributes!=null && node.attributes.containsKey(childName)) {
			return true;
		}
		for (String key:data.keySet()) {
			if (key.startsWith(childName+attributeSeparator)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Map<String, String> getAttributes(XSElementDeclaration elementDeclaration, PropertyNode node) throws SAXException {
		return node!=null ? node.attributes: null;
	}

	@Override
	public Iterable<PropertyNode> getChildrenByName(PropertyNode node, XSElementDeclaration childElementDeclaration) {
		String name = childElementDeclaration.getName();
		List<PropertyNode> result=new LinkedList<>();
		if (data.containsKey(name)) {
			String[] values=data.get(name).split(valueSeparator);
			for (String value:values) {
				PropertyNode childNode = new PropertyNode();
				childNode.value = value;
				result.add(childNode);
			}
		}
		String prefix = name+attributeSeparator;
		for (String key:data.keySet()) {
			if (key.startsWith(prefix)) {
				String attributeName = key.substring(prefix.length());
				String[] attributeValues = data.get(key).split(valueSeparator);
				for (int i=0; i< attributeValues.length; i++) {
					if (i>=result.size()) {
						result.add(new PropertyNode());
					}
					PropertyNode childNode = result.get(i);
					if (childNode.attributes == null) {
						childNode.attributes = new LinkedHashMap<>();
					}
					childNode.attributes.put(attributeName, attributeValues[i]);
				}
			}
		}
		if (log.isDebugEnabled() && result.size()>0) {
			String elems="";
			for (PropertyNode elem:result) {
				elems+=", ["+elem.value+"]";
			}
			log.debug("getChildrenByName returning: "+elems.substring(1));
		}
//		for (int i=1;data.containsKey(name+indexSeparator+i);i++) {
//			result.add(data.get(name+indexSeparator+i));
//		}
		return result;
	}

	@Override
	public String getText(XSElementDeclaration elementDeclaration, PropertyNode node) {
		return node.value;
	}

	@Override
	public PropertyNode getRootNode(Map<String, String> container) {
		if(getRootElement() == null) {
			return super.getRootNode(container);
		}

		Map<String, String> rootAttributes = new HashMap<>();
		int offset = getRootElement().length()+1;
		for(Map.Entry<String, String> entry : container.entrySet()) {
			String key = entry.getKey();
			if(key.startsWith(getRootElement()+attributeSeparator)) {
				rootAttributes.put(key.substring(offset), entry.getValue());
			}
		}
		PropertyNode rootNode = new PropertyNode();
		rootNode.attributes = rootAttributes;
		return rootNode;
	}

	public static String translate(Map<String,String> data, URL schemaURL, String rootElement, String targetNamespace) throws SAXException {
		ValidatorHandler validatorHandler = getValidatorHandler(schemaURL);
		List<XSModel> schemaInformation = getSchemaInformation(schemaURL);

		// create the validator, setup the chain
		Properties2Xml p2x = new Properties2Xml(validatorHandler,schemaInformation,rootElement);
		if (targetNamespace!=null) {
			p2x.setTargetNamespace(targetNamespace);
		}

		return p2x.translate(data);
	}

}
