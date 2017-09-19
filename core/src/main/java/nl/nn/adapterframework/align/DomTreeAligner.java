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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.xml.validation.ValidatorHandler;

import org.apache.xerces.xs.XSModel;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class DomTreeAligner extends Tree2Xml<Node> {

	public DomTreeAligner() throws SAXException {
		super();
	}
	public DomTreeAligner(ValidatorHandler validatorHandler, List<XSModel> schemaInformation) {
		super(validatorHandler, schemaInformation);
	}

	@Override
	public void startParse(Node node) throws SAXException {
		setRootElement(node.getLocalName());
		super.startParse(node);
	}
	
	@Override
	public String getNodeNamespaceURI(Node node) { 
		return node.getNamespaceURI();
	}

	public String getNodeName(Node node) { 
		return node.getNodeName();
	}

	@Override
	public String getText(Node node) {
		Node childNode=node.getFirstChild();
		return childNode==null?null:childNode.getNodeValue();
	}
	
	@Override
	public Iterable<Node> getChildrenByName(Node node, String name) throws SAXException {
		List<Node> children = new LinkedList<Node>();
		for (Node cur=node.getFirstChild();cur!=null;cur=cur.getNextSibling()) {
			if (cur.getNodeName().equals(name)) {
				children.add(cur);
			}
		}
		return children;
	}
	
	@Override
	public Set<String> getAllChildNames(Node node) throws SAXException {
		Set<String> children = new HashSet<String>();
		for (Node cur=node.getFirstChild();cur!=null;cur=cur.getNextSibling()) {
			if (!"#text".equals(cur.getNodeName())) {
				children.add(cur.getNodeName());
			}
		}
		return children;
	}
	@Override
	public boolean isNil(Node node) {
		return "true".equals(node.getAttributes().getNamedItemNS(XML_SCHEMA_INSTANCE_NAMESPACE, XML_SCHEMA_NIL_ATTRIBUTE));
	}


}
