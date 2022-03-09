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
package nl.nn.adapterframework.align;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.validation.ValidatorHandler;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.xml.sax.SAXException;

/**
 * XML Schema guided JSON to XML converter;
 * 
 * @author Gerrit van Brakel
 */
public class Properties2Xml extends Map2Xml<String,String,Map<String,String>> {

	private String attributeSeparator=".";
//	private String indexSeparator=".";
	private String valueSeparator=",";

	private Map<String,String> data;

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
	public boolean hasChild(XSElementDeclaration elementDeclaration, String node, String childName) throws SAXException {
		if (data.containsKey(childName)) {
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
	public Map<String, String> getAttributes(XSElementDeclaration elementDeclaration, String node) throws SAXException {
		Map<String, String> result=new LinkedHashMap<>();
		String name = elementDeclaration.getName();
		String prefix = name+attributeSeparator;
		for (String key:data.keySet()) {
			if (key.startsWith(prefix)) {
				String attributeKey = key.substring(prefix.length());
				result.put(attributeKey, data.get(key));
			}
		}
		return result;
	}

	@Override
	public Iterable<String> getChildrenByName(String node, XSElementDeclaration childElementDeclaration) throws SAXException {
		String name = childElementDeclaration.getName();
		List<String> result=new LinkedList<String>();
		if (data.containsKey(name)) {
			String value=data.get(name);
			result.addAll(Arrays.asList(value.split(valueSeparator)));
			if (log.isDebugEnabled()) {
				String elems="";
				for (String elem:result) {
					elems+=", ["+elem+"]";
				}
				log.debug("getChildrenByName returning: "+elems.substring(1));
			}
			return result;
		}
//		for (int i=1;data.containsKey(name+indexSeparator+i);i++) {
//			result.add(data.get(name+indexSeparator+i));
//		}
		return result;
	}

	@Override
	public String getText(XSElementDeclaration elementDeclaration, String node) {
		return node;
	}

	public static String translate(Map<String,String> data, URL schemaURL, String rootElement, String targetNamespace) throws SAXException, IOException {
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
