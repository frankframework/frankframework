/*
   Copyright 2017 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.validation.ValidatorHandler;

import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSParticle;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class DomTreeAligner extends Tree2Xml<Document,Node> {

	public DomTreeAligner(ValidatorHandler validatorHandler, List<XSModel> schemaInformation) {
		super(validatorHandler, schemaInformation);
	}

	@Override
	public void startParse(Document dom) throws SAXException {
		setRootElement(getNodeName(getRootNode(dom)));
		super.startParse(dom);
	}

	@Override
	public Node getRootNode(Document dom) {
		return dom.getDocumentElement();
	}

	@Override
	boolean isEmptyNode(Node node) {
		return !node.hasChildNodes();
	}

	@Override
	Node filterNodeChildren(Node node, List<XSParticle> allowedChildren) {
		// Dummy implementation, since this is basically dead code / deep search is not relevant to this class.
		return node;
	}

	@Override
	public String getNodeNamespaceURI(Node node) {
		return node.getNamespaceURI();
	}

	public String getNodeName(Node node) {
		return node.getLocalName();
	}

	@Override
	public String getNodeText(XSElementDeclaration elementDeclaration, Node node) {
		Node childNode=node.getFirstChild();
		return childNode==null?null:childNode.getNodeValue();
	}

	@Override
	public boolean hasChild(XSElementDeclaration elementDeclaration, Node node, String childName) throws SAXException {
		// TODO this does not take overrideValues and defaultValues into account...
		if (log.isTraceEnabled()) log.trace("hasChild() node ["+node+"] childName ["+childName+"]");
		for (Node cur=node.getFirstChild();cur!=null;cur=cur.getNextSibling()) {
			if (cur.getNodeType()==Node.ELEMENT_NODE && childName.equals(getNodeName(cur))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<String> getAllNodeChildNames(XSElementDeclaration elementDeclaration, Node node) throws SAXException {
		if (log.isTraceEnabled()) log.trace("getAllChildNames() node ["+node+"]");
		Set<String> children = new HashSet<>();
		for (Node cur=node.getFirstChild();cur!=null;cur=cur.getNextSibling()) {
			//if (log.isTraceEnabled()) log.trace("getAllChildNames() found node ["+getNodeName(cur)+"] type ["+cur.getNodeType()+"] ["+ToStringBuilder.reflectionToString(cur)+"]");
			if (cur.getNodeType()==Node.ELEMENT_NODE) {
				if (log.isTraceEnabled()) log.trace("getAllChildNames() node ["+node+"] added node ["+getNodeName(cur)+"] type ["+cur.getNodeType()+"]");
				children.add(getNodeName(cur));
			}
		}
		return children;
	}

	@Override
	public Iterable<Node> getNodeChildrenByName(Node node, XSElementDeclaration childElementDeclaration) throws SAXException {
		String name=childElementDeclaration.getName();
		if (log.isTraceEnabled()) log.trace("getChildrenByName() node ["+node+"] name ["+name+"]");
		List<Node> children = new LinkedList<>();
		for (Node cur=node.getFirstChild();cur!=null;cur=cur.getNextSibling()) {
			if (cur.getNodeType()==Node.ELEMENT_NODE && name.equals(getNodeName(cur))) {
				if (log.isTraceEnabled()) log.trace("getChildrenByName() node ["+node+"] added node ["+getNodeName(cur)+"]");
				children.add(cur);
			}
		}
		return children;
	}

	@Override
	public boolean isNil(XSElementDeclaration elementDeclaration, Node node) {
		NamedNodeMap attrs= node.getAttributes();
		if (attrs==null) {
			return false;
		}
		Node nilAttribute=attrs.getNamedItemNS(XML_SCHEMA_INSTANCE_NAMESPACE, XML_SCHEMA_NIL_ATTRIBUTE);
		return nilAttribute!=null && "true".equals(nilAttribute.getTextContent());
	}
	@Override
	public Map<String, String> getAttributes(XSElementDeclaration elementDeclaration, Node node) {
		Map<String, String> result=new HashMap<>();
		NamedNodeMap attributes=node.getAttributes();
		if (attributes!=null) {
			for (int i=0;i<attributes.getLength();i++) {
				Node item=attributes.item(i);
				result.put(getNodeName(item), item.getNodeValue());
			}
		}
		return result;
	}

	public static String translate(Document xmlIn, URL schemaURL, boolean ignoreUndeclaredElements) throws SAXException {
		ValidatorHandler validatorHandler = getValidatorHandler(schemaURL);
		List<XSModel> schemaInformation = getSchemaInformation(schemaURL);

		// create the validator, setup the chain
		DomTreeAligner dta = new DomTreeAligner(validatorHandler,schemaInformation);
		dta.setIgnoreUndeclaredElements(ignoreUndeclaredElements);

		return dta.translate(xmlIn);
	}
}
