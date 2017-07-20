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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.xs.PSVIProvider;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSParticle;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * Base class for XML Schema guided Tree to XML conversion;
 * 
 * @author Gerrit van Brakel
 *
 * @param <N>
 */
public abstract class Tree2Xml<N> extends XmlAligner {

	private String rootElement;
	private String targetNamespace;
	private boolean autoInsertMandatory=false;   // TODO: behaviour needs to be tested.
	private boolean mustProcessAllElements=true;
	protected ValidatorHandler validatorHandler;
	protected List<XSModel> schemaInformation; 
	
	private String prefixPrefix="ns";
	private int prefixPrefixCounter=1;
	private Map<String,String>prefixMap=new HashMap<String,String>();

	private final boolean DEBUG=false; 
	
	public Tree2Xml() {
		super();
	}

	public Tree2Xml(ValidatorHandler validatorHandler) {
		super((PSVIProvider)validatorHandler);
		validatorHandler.setContentHandler(this);
		this.validatorHandler=validatorHandler;
	}

	public Tree2Xml(ValidatorHandler validatorHandler, List<XSModel> schemaInformation) {
		this(validatorHandler);
		this.schemaInformation=schemaInformation;
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
	public abstract Set<String> getAllChildNames(N node) throws SAXException; // returns null when no children present
	public abstract String getText(N node); // returns null when no text present


	private class XmlAlignerInputSource extends InputSource {
		N root;
		XmlAlignerInputSource(N root) {
			super();
			this.root=root;
		}
	}
	
	/**
	 * Obtain a the XmlAligner as a {@link Source} that can be used as input of a {@link Transformer}.
	 */
	public Source asSource(N root) {
		return new SAXSource(this,root==null?null:new XmlAlignerInputSource(root));
	}

	/**
	 * start the parse, obtain the root node from the InputSource when its a {@link XmlAlignerInputSource}.
	 */
	@Override
	public void parse(InputSource input) throws SAXException, IOException {
		N root=null;
		if (input!=null && (input instanceof Tree2Xml.XmlAlignerInputSource)) {
			root= (N)((XmlAlignerInputSource)input).root;
		}
		if (log.isDebugEnabled()) log.debug("parse(InputSource) root ["+root+"]");
		startParse(root);
	}

	/**
	 * Align the XML according to the schema. 
	 */
	public void startParse(N node) throws SAXException {
		//if (log.isDebugEnabled()) log.debug("startParse() rootNode ["+node.toString()+"]"); // result of node.toString() is confusing. Do not log this.
		validatorHandler.startDocument();
		handleNode(node);
		validatorHandler.endDocument();
	}

	protected void sendString(String string) throws SAXException {
		validatorHandler.characters(string.toCharArray(), 0, string.length());
	}
	
	
	/**
	 * Pushes node through validator.
	 * 
	 * Must push all nodes through validatorhandler, recursively, respecting the alignment request.
	 * Must set current=node before calling validatorHandler.startElement(), in order to get the right argument for the onStartElement / performAlignment callbacks.
	 * @param node 
	 * @throws SAXException 
	 */
	public void handleNode(N node) throws SAXException {
		handleNode(node, getRootElement(), getTargetNamespace());
	}
	public void handleNode(N node, String name, String nodeNamespace) throws SAXException {
		if (DEBUG && log.isDebugEnabled()) log.debug("handleNode() name ["+name+"] namespace ["+nodeNamespace+"]");
		AttributesImpl attributes=new AttributesImpl();
		// TODO: handle attributes
		if (nodeNamespace==null) {
			nodeNamespace=getNodeNamespaceURI(node);
		}
		if (nodeNamespace==null) {
			nodeNamespace=findNamespaceForName(name);
		}
		if (nodeNamespace==null) {
			if (log.isDebugEnabled()) log.debug("node ["+name+"] did not find namespace, assigning targetNamespace ["+getTargetNamespace()+"]");
			nodeNamespace=getTargetNamespace();
		}
		
		String nodeLocalname=name;
		String elementNamespace=StringUtils.isNotEmpty(nodeNamespace)?nodeNamespace:StringUtils.isNotEmpty(targetNamespace)?targetNamespace:"";
		String nodePrefix=null;
		String createdPrefix=null;
		String qname=null;
		if (StringUtils.isNotEmpty(elementNamespace)) {
			nodePrefix=prefixMap.get(elementNamespace);
			if (nodePrefix==null) {
				createdPrefix=createPrefix();
				nodePrefix=createdPrefix;
				prefixMap.put(elementNamespace, createdPrefix);
				validatorHandler.startPrefixMapping(nodePrefix, elementNamespace);
				//attributes.addAttribute(null, createdPrefix, "xmlns:"+createdPrefix, "string", elementNamespace);
			}
			qname=nodePrefix+":"+nodeLocalname;
		} else {
			qname=nodeLocalname;
		}
		if (DEBUG && log.isDebugEnabled()) if (!elementNamespace.equals(nodeNamespace)) log.debug("switched namespace to ["+elementNamespace+"]");
		newLine();
		validatorHandler.startElement(elementNamespace, name, qname, attributes);
		List<XSParticle> childParticles = this.childElementDeclarations;
		Set<String> unProcessedChildren = getAllChildNames(node);
		Set<String> processedChildren = new HashSet<String>();
		
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
		}
		if (unProcessedChildren!=null && !unProcessedChildren.isEmpty()) {
			Set<String> unProcessedChildrenWorkingCopy=new HashSet<String>(unProcessedChildren);
			for (String childName:unProcessedChildrenWorkingCopy) {
				log.debug("processing unprocessed childelement ["+childName+"]");
				String namespace = findNamespaceForName(childName);
				processChildElement(node, namespace, childName, namespace, false, unProcessedChildren, processedChildren);
			}
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
					ErrorHandler errorHandler=validatorHandler.getErrorHandler();
					if (errorHandler!=null) {
						errorHandler.error(new SAXParseException(msg,null));
					} else {
						throw new SAXException(msg);
					}
				}
				log.warn(msg);
			}
		}
		if (processedChildren.isEmpty()) {
			String text = getText(node);
			if (DEBUG && log.isDebugEnabled()) log.debug("textnode name ["+name+"] text ["+text+"]");
			if (StringUtils.isNotEmpty(text)) {
				sendString(text);
			}
		}
		validatorHandler.endElement(elementNamespace, name, qname);
		if (createdPrefix!=null) {
			validatorHandler.endPrefixMapping(createdPrefix);
		}
	}

	protected void processChildElement(N node, String name, String childElementName, String childElementNameSpace, boolean mandatory, Set<String> unProcessedChildren, Set<String> processedChildren) throws SAXException {
		Iterable<N> childNodes = getChildrenByName(node,childElementName);
		boolean childSeen=false;
		if (childNodes!=null) {
			int i=0;
			for (N childNode:childNodes) {
				i++;
				handleNode(childNode,childElementName,childElementNameSpace);
				childSeen=true;
			}
			if (DEBUG && log.isDebugEnabled()) log.debug("processed ["+i+"] children found by name ["+childElementName+"] in ["+name+"]");
		} else {
			if (DEBUG && log.isDebugEnabled()) log.debug("no children found by name ["+childElementName+"] in ["+name+"]");
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
			handleNode(node,childElementName,childElementNameSpace); // insert node when minOccurs > 0, and no node is present
		}
	}

	protected void processArray(N node, String name, String childElementName, String childElementNameSpace, boolean mandatory, Set<String> unProcessedChildren, Set<String> processedChildren) throws SAXException {
		processChildElement(node, name, childElementName, childElementNameSpace, mandatory, unProcessedChildren, processedChildren);	
	}
	
	public String findNamespaceForName(String name) throws SAXException {
		Set<String> namespaces=findNamespacesForName(name);
		if (namespaces==null) {
			log.warn("No namespaces found for ["+name+"]");
			return null;
		}
		if (namespaces.size()>1) {
			String[] namespacesArray=(String[])namespaces.toArray();
			throw new SAXException("multiple ["+namespaces.size()+"] namespaces found for ["+name+"]: first two ["+namespacesArray[0]+"]["+namespacesArray[1]+"]");
		}
		if (namespaces.size()==1) {
			return (String)namespaces.toArray()[0];
		}
		return null;
	}
	public Set<String> findNamespacesForName(String name) {
		//if (DEBUG && log.isDebugEnabled()) log.debug("schemaInformation ["+ToStringBuilder.reflectionToString(schemaInformation,ToStringStyle.MULTI_LINE_STYLE)+"]");
		Set<String> result=new LinkedHashSet<String>();
		if (schemaInformation==null) {
			log.warn("No SchemaInformation specified, cannot find namespaces for ["+name+"]");
			return null;
		}
		for (XSModel model:schemaInformation) {
			XSNamedMap components = model.getComponents(XSConstants.ELEMENT_DECLARATION);
			for (int i=0;i<components.getLength();i++) {
				XSObject item=components.item(i);
				if (item.getName().equals(name)) {
					if (log.isDebugEnabled()) log.debug("name ["+name+"] found in namespace ["+item.getNamespace()+"]");
					result.add(item.getNamespace());
				}
			}
		}
		return result;
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

	public List<XSModel> getSchemaInformation() {
		return schemaInformation;
	}
	public void setSchemaInformation(List<XSModel> schemaInformation) {
		this.schemaInformation = schemaInformation;
	}

}
