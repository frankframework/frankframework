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

import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.builder.ToStringBuilder;
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

	private final boolean DEBUG=false; 
	
	private boolean insertElementContainerElements;

	public Json2Xml(String targetNamespace, ValidatorHandler validatorHandler, boolean insertElementContainerElements) {
		super(targetNamespace, validatorHandler);
		this.insertElementContainerElements=insertElementContainerElements;
	}

	@Override
	public void startParse(Object node) throws SAXException {
		JSONObject root = (JSONObject)node;
		String names[] = JSONObject.getNames(root);
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
		try {
			super.startParse(((JSONObject)node).get(names[0]));
		} catch (JSONException e) {
			throw new SAXException(e);
		}
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
		if (DEBUG && log.isDebugEnabled()) log.debug("getText() node ["+ToStringBuilder.reflectionToString(node)+"] = ["+result+"]");
		return result;
	}

	@Override
	public Iterable<Object> getChildrenByName(Object node, String name) throws SAXException {
		if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() name ["+name+"] node ["+ToStringBuilder.reflectionToString(node)+"]");
		try {
			if (node instanceof LinkedList) {
				if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() child ["+name+"], found LinkedList, straight json in compact mode");
				return (LinkedList)node;
			}
			if (node instanceof JSONArray && insertElementContainerElements) {
				if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() child ["+name+"] not found, but current node is array, assuming child needs to be inserted");
				List<Object> result = new LinkedList<Object>(); 
				JSONArray ja = (JSONArray)node;
				for (int i=0;i<ja.length();i++) {
					result.add(ja.get(i));
					if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() name ["+name+"] adding to array ["+ToStringBuilder.reflectionToString(ja.get(i))+"]");
				}
				return result;
			}
			if (!(node instanceof JSONObject)) {
				if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() parent node is not a JSONObject, but a ["+node.getClass().getName()+"]");
				return null;
			} 
			JSONObject o = (JSONObject)node;
			Object child = o.opt(name);
			if (child==null) {
				if (node instanceof JSONArray && insertElementContainerElements) {
					if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() child ["+name+"] not found, but current node is array");
					List<Object> result = new LinkedList<Object>(); 
					result.add(node);
					return result;
				}
				if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() no children named ["+name+"] node ["+ToStringBuilder.reflectionToString(node)+"]");
				return null;
			}
			List<Object> result = new LinkedList<Object>(); 
			if (child instanceof JSONArray) {
				if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() child named ["+name+"] is a JSONArray");
				if (insertElementContainerElements) {
					result.add(child);
					if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() name ["+name+"] returning array node");
				} else {
					JSONArray ja = (JSONArray)child;
					for (int i=0;i<ja.length();i++) {
						result.add(ja.get(i));
						if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() name ["+name+"] adding to array ["+ToStringBuilder.reflectionToString(ja.get(i))+"]");
					}
				}
				return result;
			}
			result.add(child);
			if (DEBUG && log.isDebugEnabled()) log.debug("getChildrenByName() name ["+name+"] returning ["+ToStringBuilder.reflectionToString(child)+"]");
			return result;
		} catch (JSONException e) {
			throw new SAXException(e);
		}
	}
	
	@Override
	public List<String> getAllChildNames(Object node) throws SAXException {
		if (DEBUG && log.isDebugEnabled()) log.debug("getAllChildNames() node ["+ToStringBuilder.reflectionToString(node)+"]");
		try {
			if (!(node instanceof JSONObject)) {
				if (DEBUG && log.isDebugEnabled()) log.debug("getAllChildNames() parent node is not a JSONObject, but a ["+node.getClass().getName()+"]");
				return new LinkedList<String>();
			} 
			JSONObject o = (JSONObject)node;
			JSONArray names= o.names();
			if (names==null) {
				if (DEBUG && log.isDebugEnabled()) log.debug("getAllChildNames() no children");
				return new LinkedList<String>();
			}
			List<String> result = new LinkedList<String>(); 
			for (int i=0;i<names.length();i++) {
				String name=(String)names.get(i);
				result.add(name);
				if (DEBUG && log.isDebugEnabled()) log.debug("getAllChildNames() name ["+name+"] added to array");
			}
			return result;
		} catch (JSONException e) {
			throw new SAXException(e);
		}
	}
	@Override
	protected void processArray(Object node, String name, String childElementName, String childElementNameSpace, boolean mandatory, List<String> unProcessedChildren, List<String> processedChildren) throws SAXException {
		if (insertElementContainerElements) {
			if (unProcessedChildren.isEmpty()) {
				if  (node instanceof JSONArray) {
					if (DEBUG && log.isDebugEnabled()) log.debug("processArray() name ["+name+"] node is JSONArray, inserting array elment container element, node ["+node+"]");
					unProcessedChildren.add(childElementName);
				}
				super.processArray(node, name, childElementName, childElementNameSpace, mandatory, unProcessedChildren, processedChildren);
			} else {
				// straight json compatibility
				// when we arrive here, the insertion of a container element needs to be compensated.
				try {
					List<Object> helper=new LinkedList<Object>(); // We'll hack this by returning a LinkedList. getChildElementsByName() will recognize this.
					JSONObject obj=(JSONObject)node;
					Object childObject=obj.get(childElementName);
					if (childObject instanceof JSONArray) {
						JSONArray ja=(JSONArray)childObject;
						for (int i=0;i<ja.length();i++) {
							helper.add(ja.get(i));
						}
					} else {
						helper.add(childObject);
					}
					processChildElement(helper, name, childElementName, childElementNameSpace, mandatory, unProcessedChildren, processedChildren);
				} catch (JSONException e) {
					throw new SAXException(e);
				}
			}
		} else {
			super.processArray(node, name, childElementName, childElementNameSpace, mandatory, unProcessedChildren, processedChildren);
		}
	}

}
