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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.xerces.xs.PSVIProvider;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;
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

	public final String XSI_PREFIX_MAPPING="xsi";
	
	private String rootElement;
	private String targetNamespace;
	private boolean autoInsertMandatory=false;   // TODO: behaviour needs to be tested.
	private boolean mustProcessAllElements=true;
	protected ValidatorHandler validatorHandler;
	protected List<XSModel> schemaInformation; 
	
	protected XSTypeDefinition upcomingTypeDefinition=null;
	
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
	
	public abstract Iterable<N> getChildrenByName(N node, String name) throws SAXException; // returns null when no children present
	public abstract Set<String> getAllChildNames(N node) throws SAXException; // returns null when no children present, otherwise a _copy_ of the Set (it will be modified)
	public abstract Map<String,String> getAttributes(N node) throws SAXException; // returns null when no attributes present, otherwise a _copy_ of the Map (it will be modified)
	public abstract String getText(N node); // returns null when no text present
	public abstract boolean isNil(N node); // returns true when the node is a nil


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
			root= ((XmlAlignerInputSource)input).root;
		}
		if (DEBUG) log.debug("parse(InputSource) root ["+root+"]");
		startParse(root);
	}

	/**
	 * Align the XML according to the schema. 
	 */
	public void startParse(N node) throws SAXException {
		//if (DEBUG) log.debug("startParse() rootNode ["+node.toString()+"]"); // result of node.toString() is confusing. Do not log this.
		validatorHandler.startDocument();
		handleNode(node);
		validatorHandler.endDocument();
	}

	protected void sendString(String string) throws SAXException {
		validatorHandler.characters(string.toCharArray(), 0, string.length());
	}
	
	public void handleError(String msg) throws SAXException {
		ErrorHandler errorHandler=validatorHandler.getErrorHandler();
		if (errorHandler!=null) {
			errorHandler.error(new SAXParseException(msg,null));
		} else {
			throw new SAXException(msg);
		}		
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
		if (DEBUG) log.debug("handleNode() name ["+name+"] namespace ["+nodeNamespace+"] isNil ["+isNil(node)+"]");
		// TODO: handle attributes
		if (nodeNamespace==null) {
			nodeNamespace=getNodeNamespaceURI(node);
		}
		if (nodeNamespace==null) {
			nodeNamespace=findNamespaceForName(name);
		}
		if (nodeNamespace==null) {
			if (DEBUG) log.debug("node ["+name+"] did not find namespace, assigning targetNamespace ["+getTargetNamespace()+"]");
			nodeNamespace=getTargetNamespace();
		}
		
		String nodeLocalname=name;
		String elementNamespace=StringUtils.isNotEmpty(nodeNamespace)?nodeNamespace:StringUtils.isNotEmpty(targetNamespace)?targetNamespace:"";
		String qname=getQName(elementNamespace, nodeLocalname);

		newLine();
		if (DEBUG) if (!elementNamespace.equals(nodeNamespace)) log.debug("switched namespace to ["+elementNamespace+"]");
		AttributesImpl attributes=new AttributesImpl();
		Map<String,String> nodeAttributes = getAttributes(node);
		if (DEBUG) log.debug("node ["+name+"] search for attributeDeclaration");
		XSObjectList attributeUses=getAttributeUses(upcomingTypeDefinition);
		if (attributeUses==null || attributeUses.getLength()==0) {
			if (nodeAttributes!=null && nodeAttributes.size()>0) {
				log.warn("node ["+name+"] found ["+nodeAttributes.size()+"] attributes, but no declared AttributeUses");
			} else {
				if (DEBUG) log.debug("node ["+name+"] no attributeUses, no attributes");
			}
		} else {
			if (nodeAttributes==null || nodeAttributes.isEmpty()) {
				log.warn("node ["+name+"] declared ["+attributeUses.getLength()+"] attributes, but no attributes found");
			} else {
				for (int i=0;i<attributeUses.getLength(); i++) {
					XSAttributeUse attributeUse=(XSAttributeUse)attributeUses.item(i);
					//if (DEBUG) log.debug("startElement ["+localName+"] attributeUse ["+ToStringBuilder.reflectionToString(attributeUse)+"]");
					XSAttributeDeclaration attributeDeclaration=attributeUse.getAttrDeclaration();
					if (DEBUG) log.debug("node ["+name+"] attributeDeclaration ["+ToStringBuilder.reflectionToString(attributeDeclaration)+"]");
					XSSimpleTypeDefinition attTypeDefinition=attributeDeclaration.getTypeDefinition();
					if (DEBUG) log.debug("node ["+name+"] attTypeDefinition ["+ToStringBuilder.reflectionToString(attTypeDefinition)+"]");
					String attName=attributeDeclaration.getName();
					if (nodeAttributes.containsKey(attName)) {
						String value=nodeAttributes.remove(attName);
						String uri=attributeDeclaration.getNamespace();
						String attqname=getQName(uri,attName);
						String type=null;
						if (DEBUG) log.debug("node ["+name+"] adding attribute ["+attName+"] value ["+value+"]");
						attributes.addAttribute(uri, attName, attqname, type, value);
					}
				}
			}
		}
		if (isNil(node)) {
			validatorHandler.startPrefixMapping(XSI_PREFIX_MAPPING, XML_SCHEMA_INSTANCE_NAMESPACE);
			attributes.addAttribute(XML_SCHEMA_INSTANCE_NAMESPACE, XML_SCHEMA_NIL_ATTRIBUTE, XSI_PREFIX_MAPPING+":"+XML_SCHEMA_NIL_ATTRIBUTE, "xs:boolean", "true");
			validatorHandler.startElement(elementNamespace, name, qname, attributes);
			validatorHandler.endElement(elementNamespace, name, qname);
			validatorHandler.endPrefixMapping(XSI_PREFIX_MAPPING);
		} else {
			validatorHandler.startElement(elementNamespace, name, qname, attributes);
			//List<XSParticle> childParticles = getChildElementDeclarations(typeDefintion);
			if (DEBUG) log.debug("handleNode() determine names of available children of element ["+name+"]"); 
			Set<String> availableChildren = getAllChildNames(node);
			Set<String> unProcessedChildren = availableChildren==null?null:new HashSet<String>(availableChildren);
			if (DEBUG) log.debug("handleNode() search for best path for available children of element ["+name+"]"); 
			List<XSParticle> childParticles = getBestChildElementPath(getTypeDefinition(), availableChildren);
			Set<String> processedChildren = new HashSet<String>();
			
			if (childParticles!=null) {
				for (int i=0;i<childParticles.size();i++) {
					XSParticle childParticle=childParticles.get(i);
					XSElementDeclaration childElementDeclaration = (XSElementDeclaration)childParticle.getTerm();
					String childElementName=childElementDeclaration.getName();
					String childElementNameSpace=childElementDeclaration.getNamespace();
					upcomingTypeDefinition=childElementDeclaration.getTypeDefinition();
					processChildElement(node, name, childElementName, childElementNameSpace, childParticle.getMinOccurs()>0, unProcessedChildren, processedChildren);
				}
			}
			if (unProcessedChildren!=null && !unProcessedChildren.isEmpty()) {
				Set<String> unProcessedChildrenWorkingCopy=new HashSet<String>(unProcessedChildren);
				log.warn("processing ["+unProcessedChildren.size()+"] unprocessed child elements"+(unProcessedChildren.size()>0?", first ["+unProcessedChildren.iterator().next()+"]":""));
				for (String childName:unProcessedChildrenWorkingCopy) {
					log.warn("processing unprocessed child element ["+childName+"]");
					String namespace=null;
					upcomingTypeDefinition=null;
					XSElementDeclaration childElementDeclaration = findElementDeclarationForName(childName);
					if (childElementDeclaration==null) {
						log.warn("Could not find ElementDeclaration for ["+childName+"]");
					} else {
						namespace = childElementDeclaration.getNamespace();
						upcomingTypeDefinition=childElementDeclaration.getTypeDefinition();
					}
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
						handleError(msg);
					} else {
						log.warn(msg);
					}
				}
			}
			if (processedChildren.isEmpty()) {
				String text = getText(node);
				if (DEBUG) log.debug("textnode name ["+name+"] text ["+text+"]");
				if (StringUtils.isNotEmpty(text)) {
					sendString(text);
				}
			}
			//if (elementType==null) newLine();
			validatorHandler.endElement(elementNamespace, name, qname);
		}
//		if (createdPrefix!=null) {
//			validatorHandler.endPrefixMapping(createdPrefix);
//		}
	}

	public String getQName(String namespace, String name) throws SAXException {
		if (StringUtils.isNotEmpty(namespace)) {
			String prefix=getNamespacePrefix(namespace);
			return prefix+":"+name;
		} 
		return name;
	}
	
	public String getNamespacePrefix(String uri) throws SAXException {
		String prefix=prefixMap.get(uri);
		if (prefix==null) {
			prefix=prefixPrefix+prefixPrefixCounter++;
			prefixMap.put(uri, prefix);
			validatorHandler.startPrefixMapping(prefix, uri);
		}
		return prefix;
	}
	
	/**
	 * 
	 * @param modelGroup
	 * @param availableElements
	 * @return the longest list of child elements, that matches the available, or null if no matching.
 	 */
	public boolean getBestMatchingElementPath(XSParticle particle, List<XSParticle> path, Set<String> availableElements, List<String> failureReasons) {
		if (particle==null) {
			throw new NullPointerException("getBestMatchingElementPath particle is null");
		} 
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new NullPointerException("getBestMatchingElementPath particle.term is null");
		} 
		if (term instanceof XSModelGroup) {
			XSModelGroup modelGroup = (XSModelGroup)term;
			short compositor = modelGroup.getCompositor();			
			XSObjectList particles = modelGroup.getParticles();
			if (DEBUG) log.debug("getBestMatchingElementPath() modelGroup particles ["+ToStringBuilder.reflectionToString(particles,ToStringStyle.MULTI_LINE_STYLE)+"]");
			switch (compositor) {
			case XSModelGroup.COMPOSITOR_SEQUENCE:
			case XSModelGroup.COMPOSITOR_ALL:
				for (int i=0;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					if (!getBestMatchingElementPath(childParticle,path,availableElements,failureReasons)) {
						return false;
					}
				}
				return true;
			case XSModelGroup.COMPOSITOR_CHOICE:
				List<XSParticle> bestPath=null;
				Set<String> bestPathElementsLeft=null;
				
				List<String> choiceFailureReasons = new LinkedList<String>();
				for (int i=0;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					List<XSParticle> optionPath=new LinkedList<XSParticle>(path); 
					Set<String> optionAvailableElements=new HashSet<String>(availableElements); 
					
					if (getBestMatchingElementPath(childParticle, optionPath, optionAvailableElements, choiceFailureReasons)) {
						if (bestPath==null || bestPath.size()<optionPath.size()) {
							bestPath=optionPath;
							bestPathElementsLeft=optionAvailableElements;
						}
					}
				}
				if (bestPath==null) {
					failureReasons.addAll(choiceFailureReasons);
					return false;
				}
				path.addAll(bestPath);
				availableElements.retainAll(bestPathElementsLeft);
				return true;
			default:
				throw new IllegalStateException("getBestMatchingElementPath modelGroup.compositor is not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE, but ["+compositor+"]");
			} 
		} 
		if (term instanceof XSElementDeclaration) {
			XSElementDeclaration elementDeclaration=(XSElementDeclaration)term;
			String elementName=elementDeclaration.getName();
			if (DEBUG) log.debug("getBestMatchingElementPath().XSElementDeclaration name ["+elementName+"]");
			if (availableElements.remove(elementName)) {
				path.add(particle);
				return true;
			}
			if (particle.getMinOccurs()>0) {
				for (XSParticle resultParticle:path) {
					if (elementName.equals(resultParticle.getTerm().getName())) {
						failureReasons.add("element ["+elementName+"] required multiple times");
						return false;
					}
				}
				failureReasons.add("expected element ["+elementName+"]");
				return false;
			}
			return true;
		}
		if (term instanceof XSWildcard) {
			log.warn("getBestMatchingElementPath SHOULD implement term instanceof XSWildcard ["+ToStringBuilder.reflectionToString(term)+"]");
			return true;
		} 
		throw new IllegalStateException("getBestMatchingElementPath unknown Term type ["+term.getClass().getName()+"]");
	}
	

	public List<XSParticle> getBestChildElementPath(XSTypeDefinition typeDefintion, Set<String> availableElements) throws SAXException {
		if (typeDefintion==null) {
			log.warn("getBestChildElementPath typeDefinition is null");
			return null;
		}
		switch (typeDefintion.getTypeCategory()) {
		case XSTypeDefinition.SIMPLE_TYPE:
			if (DEBUG) log.debug("getBestChildElementPath typeDefinition.typeCategory is SimpleType, no child elements");
			return null;
		case XSTypeDefinition.COMPLEX_TYPE:
			XSComplexTypeDefinition complexTypeDefinition=(XSComplexTypeDefinition)typeDefintion;
			switch (complexTypeDefinition.getContentType()) {
			case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
				if (DEBUG) log.debug("getBestChildElementPath complexTypeDefinition.contentType is Empty, no child elements");
				return null;
			case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
				if (DEBUG) log.debug("getBestChildElementPath complexTypeDefinition.contentType is Simple, no child elements (only characters)");
				return null;
			case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
			case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
				XSParticle particle = complexTypeDefinition.getParticle();
				if (particle==null) {
					throw new IllegalStateException("getBestChildElementPath complexTypeDefinition.particle is null for Element or Mixed contentType");
//					log.warn("typeDefinition particle is null, is this a problem?");
//					return null;
				} 
				if (DEBUG) log.debug("typeDefinition particle ["+ToStringBuilder.reflectionToString(particle,ToStringStyle.MULTI_LINE_STYLE)+"]");
				List<XSParticle> result=new LinkedList<XSParticle>();
				List<String> failureReasons=new LinkedList<String>();
				if (getBestMatchingElementPath(particle, result, availableElements, failureReasons)) {
					return result;
				}
				String msg="Cannot find path:";
				for (String reason:failureReasons) {
					msg+='\n'+reason;
				}
				handleError(msg);
				return null;
			default:
				throw new IllegalStateException("getBestChildElementPath complexTypeDefinition.contentType is not Empty,Simple,Element or Mixed, but ["+complexTypeDefinition.getContentType()+"]");
			}
		default:
			throw new IllegalStateException("getBestChildElementPath typeDefinition.typeCategory is not SimpleType or ComplexType, but ["+typeDefintion.getTypeCategory()+"] class ["+typeDefintion.getClass().getName()+"]");
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
			if (DEBUG) log.debug("processed ["+i+"] children found by name ["+childElementName+"] in ["+name+"]");
		} else {
			if (DEBUG) log.debug("no children found by name ["+childElementName+"] in ["+name+"]");
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

	public String findNamespaceForName(String name) throws SAXException {
		XSElementDeclaration elementDeclaration=findElementDeclarationForName(name);
		if (elementDeclaration==null) {
			return null;
		}
		return elementDeclaration.getNamespace();
	}

	public XSElementDeclaration findElementDeclarationForName(String name) throws SAXException {
		Set<XSElementDeclaration> elementDeclarations=findElementDeclarationsForName(name);
		if (elementDeclarations==null) {
			log.warn("No element declarations found for ["+name+"]");
			return null;
		}
		if (elementDeclarations.size()>1) {
			XSElementDeclaration[] XSElementDeclarationArray=(XSElementDeclaration[])elementDeclarations.toArray();
			throw new SAXException("multiple ["+elementDeclarations.size()+"] namespaces found for ["+name+"]: first two ["+XSElementDeclarationArray[0].getNamespace()+"]["+XSElementDeclarationArray[1].getNamespace()+"]");
		}
		if (elementDeclarations.size()==1) {
			return (XSElementDeclaration)elementDeclarations.toArray()[0];
		}
		return null;
	}
	public Set<XSElementDeclaration> findElementDeclarationsForName(String name) {
		Set<XSElementDeclaration> result=new LinkedHashSet<XSElementDeclaration>();
		if (schemaInformation==null) {
			log.warn("No SchemaInformation specified, cannot find namespaces for ["+name+"]");
			return null;
		}
		for (XSModel model:schemaInformation) {
			XSNamedMap components = model.getComponents(XSConstants.ELEMENT_DECLARATION);
			for (int i=0;i<components.getLength();i++) {
				XSElementDeclaration item=(XSElementDeclaration)components.item(i);
				if (item.getName().equals(name)) {
					if (DEBUG) log.debug("name ["+name+"] found in namespace ["+item.getNamespace()+"]");
					result.add(item);
				}
			}
		}
		return result;
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
