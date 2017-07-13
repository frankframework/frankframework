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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.validation.Schema;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSParticle;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Base class for XML Schema guided Tree to XML conversion;
 * 
 * @author Gerrit van Brakel
 *
 * @param <N>
 */
public abstract class Tree2Xml<N> extends XmlAligner<N> {

	private String rootElement;
	private String targetNamespace;
	private boolean autoInsertMandatory=false;   // TODO: behaviour needs to be tested.
	private boolean mustProcessAllElements=true; // cannot be true if array element container elements are to be inserted
	
	private String prefixPrefix="ns";
	private int prefixPrefixCounter=1;
	private Map<String,String>prefixMap=new HashMap<String,String>();

	private final boolean DEBUG=false; 
	
	public Tree2Xml() {
		super();
	}
	public Tree2Xml(String targetNamespace, Schema schema) {
		super(schema);
		this.targetNamespace=targetNamespace;
	}
	public Tree2Xml(String targetNamespace, ValidatorHandler validatorHandler) {
		super(validatorHandler);
		this.targetNamespace=targetNamespace;
	}
	public Tree2Xml(String targetNamespace, String rootElement, Schema schema) {
		this(targetNamespace, schema);
		this.rootElement=rootElement;
	}

	/**
	 * return namespace of node, if known. If not, it will be determined from the schema.
	 * @param node
	 * @return
	 */
	public String getNodeNamespaceURI(N node) {
		return null; 
	}
	
//	public abstract boolean isLeafNode(N node); // only leaf nodes can contain text, leaf nodes cannot contain children
	public abstract Iterable<N> getChildrenByName(N node, String name) throws SAXException; // returns null when no children present
	public abstract List<String> getAllChildNames(N node) throws SAXException; // returns null when no children present
	public abstract String getText(N node); // returns null when no text present
	
	@Override
	public void handleNode(N node) throws SAXException {
		handleNode(node, getTargetNamespace(), getRootElement());
	}
	public void handleNode(N node, String nodeNamespace, String name) throws SAXException {
		if (DEBUG && log.isDebugEnabled()) log.debug("handleNode() namespace ["+nodeNamespace+"] name ["+name+"]");
		AttributesImpl attributes=new AttributesImpl();
		// TODO: handle attributes
		if (nodeNamespace==null) {
			nodeNamespace=getNodeNamespaceURI(node);
		}
		if (nodeNamespace==null) {
			nodeNamespace=findNamespaceForName(name);
		}
		
		String nodeLocalname=name;
		String elementNamespace=StringUtils.isNotEmpty(nodeNamespace)?nodeNamespace:StringUtils.isNotEmpty(targetNamespace)?targetNamespace:"";
		String nodePrefix=prefixMap.get(elementNamespace);
		String createdPrefix=null;
		if (nodePrefix==null) {
			createdPrefix=createPrefix();
			nodePrefix=createdPrefix;
			prefixMap.put(elementNamespace, createdPrefix);
			validatorHandler.startPrefixMapping(nodePrefix, elementNamespace);
			//attributes.addAttribute(null, "xmlns", null, "string", elementNamespace);
		}
		String qname=nodePrefix+":"+nodeLocalname;
		if (DEBUG && log.isDebugEnabled()) log.debug("namespaceuri ["+elementNamespace+"] node name ["+nodeLocalname+"] qname ["+qname+"]");
		if (DEBUG && log.isDebugEnabled()) if (!elementNamespace.equals(nodeNamespace)) log.debug("switched namespace to ["+elementNamespace+"]");
		newLine();
		validatorHandler.startElement(elementNamespace, name, qname, attributes);
		List<XSParticle> childParticles = this.childElementDeclarations;
		List<String> unProcessedChildren = getAllChildNames(node);
		List<String> processedChildren = new LinkedList<String>();
		
		if (childParticles!=null) {
			if (childParticles.size()==1 && (childParticles.get(0).getMaxOccurs()>1 || childParticles.get(0).getMaxOccursUnbounded())) {
				XSParticle childParticle=childParticles.get(0);
				XSElementDeclaration childElementDeclaration = (XSElementDeclaration)childParticle.getTerm();
				String childElementName=childElementDeclaration.getName();
				String childElementNameSpace=childElementDeclaration.getNamespace();
				processArray(node, name, childElementName, childElementNameSpace, childParticle.getMinOccurs()>0, unProcessedChildren, processedChildren);
			} else {
				for (int i=0;i<childParticles.size();i++) {
					XSParticle childParticle=childParticles.get(i);
					XSElementDeclaration childElementDeclaration = (XSElementDeclaration)childParticle.getTerm();
					String childElementName=childElementDeclaration.getName();
					String childElementNameSpace=childElementDeclaration.getNamespace();
					processChildElement(node, name, childElementName, childElementNameSpace, childParticle.getMinOccurs()>0, unProcessedChildren, processedChildren);
				}
			}
			newLine(-1);
		} else {
			String text = getText(node);
			if (DEBUG && log.isDebugEnabled()) log.debug("textnode name ["+name+"] text ["+text+"]");
			if (StringUtils.isNotEmpty(text)) {
				sendString(text);
			}
		}
		if (unProcessedChildren!=null && !unProcessedChildren.isEmpty()) {
			List<String> unProcessedChildrenWorkingCopy=new LinkedList<String>(unProcessedChildren);
			for (String childName:unProcessedChildren) {
				String namespace = findNamespaceForName(childName);
				if (namespace!=null) {
					processChildElement(node, namespace, childName, namespace, false, unProcessedChildrenWorkingCopy, processedChildren);
				}
			}
			unProcessedChildren=unProcessedChildrenWorkingCopy;
			if (!unProcessedChildren.isEmpty()) {
				String elementList=null;
				int count=0;
				for (String childName:unProcessedChildren) {
					count++;
					if (elementList==null) {
						elementList=childName;
					} else {
						elementList+=","+childName;
					}
				}
				String msg="Invalid content was found in node ["+name+"] that has ["+count+"] unprocessed children ["+elementList+"]";
				if (mustProcessAllElements) {
					throw new SAXException(msg);
				}
				log.warn(msg);
			}
		}
		validatorHandler.endElement(elementNamespace, name, qname);
		if (createdPrefix!=null) {
			validatorHandler.endPrefixMapping(createdPrefix);
		}
	}

	protected void processChildElement(N node, String name, String childElementName, String childElementNameSpace, boolean mandatory, List<String> unProcessedChildren, List<String> processedChildren) throws SAXException {
		Iterable<N> childNodes = getChildrenByName(node,childElementName);
		boolean childSeen=false;
		if (childNodes!=null) {
			for (N childNode:childNodes) {
				handleNode(childNode,childElementNameSpace,childElementName);
				childSeen=true;
			}
		} 
		if (childSeen) {
			if (unProcessedChildren==null) {
				throw new IllegalStateException("child element ["+childElementName+"] found, but node ["+name+"] should have no children");
			}
			if (!unProcessedChildren.remove(childElementName)) {
				throw new IllegalStateException("child element ["+childElementName+"] not found in list of unprocessed children of node ["+name+"]");
			}
			if (processedChildren.contains(childElementName)) {
				throw new IllegalStateException("child element ["+childElementName+"] already processed for node ["+name+"]");
			}
			processedChildren.add(childElementName);
		}
		if (!childSeen && mandatory && isAutoInsertMandatory()) {
			if (log.isDebugEnabled()) log.debug("inserting mandatory element ["+childElementName+"]");
			handleNode(node,childElementNameSpace,childElementName); // insert node when minOccurs > 0, and no node is present
		}
	}

	protected void processArray(N node, String name, String childElementName, String childElementNameSpace, boolean mandatory, List<String> unProcessedChildren, List<String> processedChildren) throws SAXException {
		processChildElement(node, name, childElementName, childElementNameSpace, mandatory, unProcessedChildren, processedChildren);	
	}
	
	
	public String createPrefix() {
		return prefixPrefix+prefixPrefixCounter++;
	}
	
	public String getRootElement() {
		return rootElement;
	}
	public void setRootElement(String rootElement) {
		this.rootElement = rootElement;
	}
	
	public String getTargetNamespace() {
		return targetNamespace;
	}
	public void setTargetNamespace(String targetNamespace) {
		this.targetNamespace = targetNamespace;
	}
	public boolean isAutoInsertMandatory() {
		return autoInsertMandatory;
	}
	public void setAutoInsertMandatory(boolean autoInsertMandatory) {
		this.autoInsertMandatory = autoInsertMandatory;
	}
	public boolean isMustProcessAllElements() {
		return mustProcessAllElements;
	}
	public void setMustProcessAllElements(boolean mustProcessAllElements) {
		this.mustProcessAllElements = mustProcessAllElements;
	}

}
