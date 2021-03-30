/*
   Copyright 2017,2018 Nationale-Nederlanden, 2020 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
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

import nl.nn.adapterframework.xml.XmlWriter;

/**
 * Base class for XML Schema guided Object to XML conversion;
 * 
 * @author Gerrit van Brakel
 *
 * @param <C> container
 * @param <N> node
 */
public abstract class ToXml<C,N> extends XmlAligner {

	public final String XSI_PREFIX_MAPPING="xsi";

	public static final String MSG_EXPECTED_ELEMENT="expected element";
	public static final String MSG_INVALID_CONTENT="Invalid content";
	public static final String MSG_CANNOT_NOT_FIND_ELEMENT_DECLARATION="Cannot find the declaration of element";

	private String rootElement;
	private String targetNamespace;
	protected ValidatorHandler validatorHandler;
	private List<XSModel> schemaInformation; 

//	private boolean autoInsertMandatory=false;   // TODO: behaviour needs to be tested.
	private boolean deepSearch=false;
	private boolean failOnWildcards=false;

	private String prefixPrefix="ns";
	private int prefixPrefixCounter=1;
	private Map<String,String>prefixMap=new HashMap<String,String>();


	public ToXml() {
		super();
	}

	public ToXml(ValidatorHandler validatorHandler) {
		super(validatorHandler);
		this.validatorHandler=validatorHandler;
	}

	public ToXml(ValidatorHandler validatorHandler, List<XSModel> schemaInformation) {
		this(validatorHandler);
		this.schemaInformation=schemaInformation;
	}

	/**
	 * return namespace of node, if known. If not, it will be determined from the schema.
	 */
	public String getNodeNamespaceURI(N node) {
		return null; 
	}

	
	private class XmlAlignerInputSource extends InputSource {
		C container;
		XmlAlignerInputSource(C container) {
			super();
			this.container=container;
		}
	}
	
	/**
	 * Obtain the XmlAligner as a {@link Source} that can be used as input of a {@link Transformer}.
	 */
	public Source asSource(C container) {
		return new SAXSource(this,container==null?null:new XmlAlignerInputSource(container));
	}

	/**
	 * Start the parse, obtain the container to parse from the InputSource when set by {@link #asSource(Object)}. 
	 * Normally, the parse is started via {#startParse(C container)}, but this implementation allows {@link #asSource(Object)} to function.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void parse(InputSource input) throws SAXException, IOException {
		C container=null;
		if (input!=null && (input instanceof ToXml.XmlAlignerInputSource)) {
			container= ((XmlAlignerInputSource)input).container;
		}
		if (log.isTraceEnabled()) log.trace("parse(InputSource) container ["+container+"]");
		startParse(container);
	}

	/**
	 * Align the XML according to the schema. 
	 */
	public void startParse(C container) throws SAXException {
		//if (log.isTraceEnabled()) log.trace("startParse() rootNode ["+node.toString()+"]"); // result of node.toString() is confusing. Do not log this.
		try {
			validatorHandler.startDocument();
			handleNode(container, getRootElement(), getTargetNamespace());
			validatorHandler.endDocument();
		} catch (SAXException e) {
			handleError(e);
		}
	}

	public abstract N getRootNode(C container);
	public abstract Map<String,String> getAttributes(XSElementDeclaration elementDeclaration, N node) throws SAXException; // returns null when no attributes present, otherwise a _copy_ of the Map (it is allowed to be modified)
	public abstract boolean hasChild(XSElementDeclaration elementDeclaration, N node, String childName) throws SAXException; 
	public abstract Iterable<N> getChildrenByName(N node, XSElementDeclaration childElementDeclaration) throws SAXException; // returns null when no children present
	public abstract boolean isNil(XSElementDeclaration elementDeclaration, N node); // returns true when the node is a nil
	public abstract String getText(XSElementDeclaration elementDeclaration, N node); // returns null when no text present, will only be called when node has no children

	@SuppressWarnings("unused")
	protected Set<String> getUnprocessedChildElementNames(XSElementDeclaration elementDeclaration, N node, Set<String> processedChildren) throws SAXException {
		return null;
	}


	/**
	 * Pushes node through validator.
	 * 
	 * Must push all nodes through validatorhandler, recursively, respecting the alignment request.
	 * Must set current=node before calling validatorHandler.startElement(), in order to get the right argument for the onStartElement / performAlignment callbacks.
	 */
	public void handleNode(C container, String name, String nodeNamespace) throws SAXException {
		if (log.isTraceEnabled()) log.trace("handleNode() name ["+name+"] namespace ["+nodeNamespace+"]");
		N rootNode=getRootNode(container);
		if (StringUtils.isEmpty(nodeNamespace)) {
			nodeNamespace=getNodeNamespaceURI(rootNode);
		}
		XSElementDeclaration elementDeclaration=findElementDeclarationForName(nodeNamespace,name);
		if (elementDeclaration==null) {
			throw new SAXException(MSG_CANNOT_NOT_FIND_ELEMENT_DECLARATION+" for ["+name+"] in namespace ["+nodeNamespace+"]");
//			if (log.isTraceEnabled()) log.trace("node ["+name+"] did not find elementDeclaration, assigning targetNamespace ["+getTargetNamespace()+"]");
//			nodeNamespace=getTargetNamespace();
		}
		handleElement(elementDeclaration,rootNode);
	}

//	@Override
	public void handleElement(XSElementDeclaration elementDeclaration, N node) throws SAXException {
		String name = elementDeclaration.getName();
		String elementNamespace=elementDeclaration.getNamespace();
		String qname=getQName(elementNamespace, name);
		if (log.isTraceEnabled()) log.trace("handleNode() name ["+name+"] elementNamespace ["+elementNamespace+"]");
		newLine();
		AttributesImpl attributes=new AttributesImpl();
		Map<String,String> nodeAttributes = getAttributes(elementDeclaration, node);
		if (log.isTraceEnabled()) log.trace("node ["+name+"] search for attributeDeclaration");
		XSTypeDefinition typeDefinition=elementDeclaration.getTypeDefinition();
		XSObjectList attributeUses=getAttributeUses(typeDefinition);
		if (attributeUses==null || attributeUses.getLength()==0) {
			if (nodeAttributes!=null && nodeAttributes.size()>0) {
				log.warn("node ["+name+"] found ["+nodeAttributes.size()+"] attributes, but no declared AttributeUses");
			} else {
				if (log.isTraceEnabled()) log.trace("node ["+name+"] no attributeUses, no attributes");
			}
		} else {
			if (nodeAttributes==null || nodeAttributes.isEmpty()) {
				log.warn("node ["+name+"] declared ["+attributeUses.getLength()+"] attributes, but no attributes found");
			} else {
				for (int i=0;i<attributeUses.getLength(); i++) {
					XSAttributeUse attributeUse=(XSAttributeUse)attributeUses.item(i);
					XSAttributeDeclaration attributeDeclaration=attributeUse.getAttrDeclaration();
					//XSSimpleTypeDefinition attTypeDefinition=attributeDeclaration.getTypeDefinition();
					//if (log.isTraceEnabled()) log.trace("node ["+name+"] attTypeDefinition ["+ToStringBuilder.reflectionToString(attTypeDefinition)+"]");
					String attName=attributeDeclaration.getName();
					if (nodeAttributes.containsKey(attName)) {
						String value=nodeAttributes.remove(attName);
						String uri=attributeDeclaration.getNamespace();
						String attqname=getQName(uri,attName);
						String type=null;
						if (log.isTraceEnabled()) log.trace("node ["+name+"] adding attribute ["+attName+"] value ["+value+"]");
						attributes.addAttribute(uri, attName, attqname, type, value);
					}
				}
			}
		}
		if (isNil(elementDeclaration, node)) {
			validatorHandler.startPrefixMapping(XSI_PREFIX_MAPPING, XML_SCHEMA_INSTANCE_NAMESPACE);
			attributes.addAttribute(XML_SCHEMA_INSTANCE_NAMESPACE, XML_SCHEMA_NIL_ATTRIBUTE, XSI_PREFIX_MAPPING+":"+XML_SCHEMA_NIL_ATTRIBUTE, "xs:boolean", "true");
			validatorHandler.startElement(elementNamespace, name, qname, attributes);
			validatorHandler.endElement(elementNamespace, name, qname);
			validatorHandler.endPrefixMapping(XSI_PREFIX_MAPPING);
		} else {
			validatorHandler.startElement(elementNamespace, name, qname, attributes);
			handleElementContents(elementDeclaration, node);
			validatorHandler.endElement(elementNamespace, name, qname);
		}
//		if (createdPrefix!=null) {
//			validatorHandler.endPrefixMapping(createdPrefix);
//		}
	}
	
	public void handleElementContents(XSElementDeclaration elementDeclaration, N node) throws SAXException {
		XSTypeDefinition typeDefinition = elementDeclaration.getTypeDefinition();
		if (typeDefinition==null) {
			log.warn("handleElementContents typeDefinition is null");
			handleSimpleTypedElement(elementDeclaration, null, node);
			return;
		}
		switch (typeDefinition.getTypeCategory()) {
		case XSTypeDefinition.SIMPLE_TYPE:
			if (log.isTraceEnabled()) log.trace("handleElementContents typeDefinition.typeCategory is SimpleType, no child elements");
			handleSimpleTypedElement(elementDeclaration, (XSSimpleTypeDefinition)typeDefinition, node);
			return;
		case XSTypeDefinition.COMPLEX_TYPE:
			XSComplexTypeDefinition complexTypeDefinition=(XSComplexTypeDefinition)typeDefinition;
			switch (complexTypeDefinition.getContentType()) {
			case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
				if (log.isTraceEnabled()) log.trace("handleElementContents complexTypeDefinition.contentType is Empty, no child elements");
				return;
			case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
				if (log.isTraceEnabled()) log.trace("handleElementContents complexTypeDefinition.contentType is Simple, no child elements (only characters)");
				handleSimpleTypedElement(elementDeclaration, null, node);
				return;
			case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
			case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
				handleComplexTypedElement(elementDeclaration,node);
				return;
			default:
				throw new IllegalStateException("handleElementContents complexTypeDefinition.contentType is not Empty,Simple,Element or Mixed, but ["+complexTypeDefinition.getContentType()+"]");
			}
		default:
			throw new IllegalStateException("handleElementContents typeDefinition.typeCategory is not SimpleType or ComplexType, but ["+typeDefinition.getTypeCategory()+"] class ["+typeDefinition.getClass().getName()+"]");
		}
	}

	protected void handleComplexTypedElement(XSElementDeclaration elementDeclaration, N node) throws SAXException {
		String name = elementDeclaration.getName();
		//List<XSParticle> childParticles = getChildElementDeclarations(typeDefinition);
		if (log.isTraceEnabled()) log.trace("ToXml.handleComplexTypedElement() search for best path for available children of element ["+name+"]"); 
		List<XSParticle> childParticles = getBestChildElementPath(elementDeclaration, node, false);
		if (log.isTraceEnabled()) {
			if (childParticles==null) {
				log.trace("Examined node ["+name+"] deepSearch ["+isDeepSearch()+"] path found is null");
			} else {
				String msg="Examined node ["+name+"] deepSearch ["+isDeepSearch()+"] found path length ["+childParticles.size()+"]: ";
				boolean tail=false;
				for(XSParticle particle:childParticles) {
					if (tail) {
						msg+=", ";
					} else {
						tail=true;
					}
					msg+=particle.getTerm().getName();
				}
				log.trace(msg);
			}
		}
		Set<String> processedChildren = new HashSet<String>();
		
		if (childParticles!=null) {
//			if (log.isTraceEnabled()) log.trace("ToXml.handleComplexTypedElement() iterating over childParticles, size ["+childParticles.size()+"]"); 
//			if (DEBUG) {
//				for (int i=0;i<childParticles.size();i++) {
//					XSParticle childParticle=childParticles.get(i);
//					XSElementDeclaration childElementDeclaration = (XSElementDeclaration)childParticle.getTerm();
//					if (log.isTraceEnabled()) log.trace("ToXml.handleComplexTypedElement() list children ["+i+"], name ["+childElementDeclaration.getName()+"]");
//				}
//			}
			for (int i=0;i<childParticles.size();i++) {
				XSParticle childParticle=childParticles.get(i);
				XSElementDeclaration childElementDeclaration = (XSElementDeclaration)childParticle.getTerm();
				if (log.isTraceEnabled()) log.trace("ToXml.handleComplexTypedElement() processing child ["+i+"], name ["+childElementDeclaration.getName()+"]"); 
				processChildElement(node, name, childElementDeclaration, childParticle.getMinOccurs()>0, processedChildren);
			}
		}
		
		Set<String> unProcessedChildren = getUnprocessedChildElementNames(elementDeclaration, node, processedChildren);
		
		if (unProcessedChildren!=null && !unProcessedChildren.isEmpty()) {
			Set<String> unProcessedChildrenWorkingCopy=new HashSet<String>(unProcessedChildren);
			log.warn("processing ["+unProcessedChildren.size()+"] unprocessed child elements"+(unProcessedChildren.size()>0?", first ["+unProcessedChildren.iterator().next()+"]":""));
			// this loop is required to handle for mixed content element containing globally defined elements
			for (String childName:unProcessedChildrenWorkingCopy) {
				log.warn("processing unprocessed child element ["+childName+"]");
				XSElementDeclaration childElementDeclaration = findElementDeclarationForName(null,childName);
				if (childElementDeclaration==null) {
					// this clause is hit for mixed content element containing elements that are not defined
					throw new SAXException(MSG_CANNOT_NOT_FIND_ELEMENT_DECLARATION+" ["+childName+"]");
				}
				processChildElement(node, name, childElementDeclaration, false, processedChildren);
			}
		}
		// the below is used for mixed content nodes containing text
		if (processedChildren.isEmpty()) {  
			if (log.isTraceEnabled()) log.trace("ToXml.handleComplexTypedElement() handle element ["+name+"] as simple, because none processed"); 
			handleSimpleTypedElement(elementDeclaration, null, node);
		}
		
	}

	

	
	protected void handleSimpleTypedElement(XSElementDeclaration elementDeclaration, @SuppressWarnings("unused") XSSimpleTypeDefinition simpleTypeDefinition, N node) throws SAXException {
		String text = getText(elementDeclaration, node);
		if (log.isTraceEnabled()) log.trace("textnode name ["+elementDeclaration.getName()+"] text ["+text+"]");
		if (StringUtils.isNotEmpty(text)) {
			sendString(text);
		}
	}

	protected void processChildElement(N node, String parentName, XSElementDeclaration childElementDeclaration, boolean mandatory, Set<String> processedChildren) throws SAXException {
		String childElementName = childElementDeclaration.getName(); 
		if (log.isTraceEnabled()) log.trace("To2Xml.processChildElement() parent name ["+parentName+"] childElementName ["+childElementName+"]");
		Iterable<N> childNodes = getChildrenByName(node,childElementDeclaration);
		boolean childSeen=false;
		if (childNodes!=null) {
			childSeen=true;
			int i=0;
			for (N childNode:childNodes) {
				i++;
				handleElement(childElementDeclaration,childNode);
			}
			if (log.isTraceEnabled()) log.trace("processed ["+i+"] children found by name ["+childElementName+"] in ["+parentName+"]");
			if (i==0 && isDeepSearch() && childElementDeclaration.getTypeDefinition().getTypeCategory()!=XSTypeDefinition.SIMPLE_TYPE) {
				if (log.isTraceEnabled()) log.trace("no children processed, and deepSearch, not a simple type therefore handle node ["+childElementName+"] in ["+parentName+"]");
				handleElement(childElementDeclaration,node);
				childSeen=true;
			}
		} else {
			if (log.isTraceEnabled()) log.trace("no children found by name ["+childElementName+"] in ["+parentName+"]");
			if (isDeepSearch() && childElementDeclaration.getTypeDefinition().getTypeCategory()!=XSTypeDefinition.SIMPLE_TYPE) {
				if (log.isTraceEnabled()) log.trace("no children found, and deepSearch, not a simple type therefore handle node ["+childElementName+"] in ["+parentName+"]");
				handleElement(childElementDeclaration,node);
				childSeen=true;
			}
		}
		if (childSeen) {
			if (processedChildren.contains(childElementName)) {
				throw new IllegalStateException("child element ["+childElementName+"] already processed for node ["+parentName+"]");
			}
			processedChildren.add(childElementName);
		}
//		if (!childSeen && mandatory && isAutoInsertMandatory()) {
//			if (log.isDebugEnabled()) log.debug("inserting mandatory element ["+childElementName+"]");
//			handleElement(childElementDeclaration,node); // insert node when minOccurs > 0, and no node is present
//		}
	}

	public List<XSParticle> getBestChildElementPath(XSElementDeclaration elementDeclaration, N node, boolean silent) throws SAXException {
		XSTypeDefinition typeDefinition = elementDeclaration.getTypeDefinition();
		if (typeDefinition==null) {
			log.warn("getBestChildElementPath typeDefinition is null");
			return null;
		}
		switch (typeDefinition.getTypeCategory()) {
		case XSTypeDefinition.SIMPLE_TYPE:
			if (log.isTraceEnabled()) log.trace("getBestChildElementPath typeDefinition.typeCategory is SimpleType, no child elements");
			return null;
		case XSTypeDefinition.COMPLEX_TYPE:
			XSComplexTypeDefinition complexTypeDefinition=(XSComplexTypeDefinition)typeDefinition;
			switch (complexTypeDefinition.getContentType()) {
			case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
				if (log.isTraceEnabled()) log.trace("getBestChildElementPath complexTypeDefinition.contentType is Empty, no child elements");
				return null;
			case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
				if (log.isTraceEnabled()) log.trace("getBestChildElementPath complexTypeDefinition.contentType is Simple, no child elements (only characters)");
				return null;
			case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
			case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
				XSParticle particle = complexTypeDefinition.getParticle();
				if (particle==null) {
					throw new IllegalStateException("getBestChildElementPath complexTypeDefinition.particle is null for Element or Mixed contentType");
//					log.warn("typeDefinition particle is null, is this a problem?");
//					return null;
				} 
				if (log.isTraceEnabled()) log.trace("typeDefinition particle ["+ToStringBuilder.reflectionToString(particle,ToStringStyle.MULTI_LINE_STYLE)+"]");
				List<XSParticle> result=new LinkedList<XSParticle>();
				List<String> failureReasons=new LinkedList<String>();
				if (getBestMatchingElementPath(elementDeclaration, node, particle, result, failureReasons)) {
					return result;
				}
				String msg="Cannot find path:";
				for (String reason:failureReasons) {
					msg+='\n'+reason;
				}
				if (!silent) {
					handleError(msg);
				}
				return null;
			default:
				throw new IllegalStateException("getBestChildElementPath complexTypeDefinition.contentType is not Empty,Simple,Element or Mixed, but ["+complexTypeDefinition.getContentType()+"]");
			}
		default:
			throw new IllegalStateException("getBestChildElementPath typeDefinition.typeCategory is not SimpleType or ComplexType, but ["+typeDefinition.getTypeCategory()+"] class ["+typeDefinition.getClass().getName()+"]");
		}
	}

	/**
	 * 
	 * @param baseElementDeclaration TODO
	 * @param particle
	 * @param failureReasons returns the reasons why no match was found
	 * @param path in this list the longest list of child elements, that matches the available, is maintained. Null if no matching.
	 * @return true when a matching path is found. if false, failureReasons will contain reasons why.
	 * @throws SAXException 
 	 */
	public boolean getBestMatchingElementPath(XSElementDeclaration baseElementDeclaration, N baseNode, XSParticle particle, List<XSParticle> path, List<String> failureReasons) throws SAXException {
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
			if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath() modelGroup particles ["+ToStringBuilder.reflectionToString(particles,ToStringStyle.MULTI_LINE_STYLE)+"]");
			switch (compositor) {
			case XSModelGroup.COMPOSITOR_SEQUENCE:
			case XSModelGroup.COMPOSITOR_ALL:
				for (int i=0;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					if (!getBestMatchingElementPath(baseElementDeclaration, baseNode, childParticle,path,failureReasons)) {
						return false;
					}
				}
				return true;
			case XSModelGroup.COMPOSITOR_CHOICE:
				List<XSParticle> bestPath=null;
				
				List<String> choiceFailureReasons = new LinkedList<String>();
				for (int i=0;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					List<XSParticle> optionPath=new LinkedList<XSParticle>(path); 
					
					if (getBestMatchingElementPath(baseElementDeclaration, baseNode, childParticle, optionPath, choiceFailureReasons)) {
						if (bestPath==null || bestPath.size()<optionPath.size()) {
							bestPath=optionPath;
						}
					}
				}
				if (bestPath==null) {
					failureReasons.addAll(choiceFailureReasons);
					return false;
				}
				if (log.isTraceEnabled()) log.trace("Replace path with best path of Choice Compositor, size ["+bestPath.size()+"]");
				path.clear();
				path.addAll(bestPath);
				return true;
			default:
				throw new IllegalStateException("getBestMatchingElementPath modelGroup.compositor is not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE, but ["+compositor+"]");
			} 
		} 
		if (term instanceof XSElementDeclaration) {
			XSElementDeclaration elementDeclaration=(XSElementDeclaration)term;
			String elementName=elementDeclaration.getName();
			if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration name ["+elementName+"]");
			if (!hasChild(baseElementDeclaration, baseNode, elementName)) {
				if (isDeepSearch()) {
					if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] not found, perform deep search");
					try {
						List<XSParticle> subList=getBestChildElementPath(elementDeclaration,baseNode, true);
						if (subList!=null && !subList.isEmpty()) {
							path.add(particle);
							if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] not found, nested elements found in deep search");
							return true;
						}
						if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] not found, no nested elements found in deep search");
					} catch (Exception e) {
						if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] not found, no nested elements found in deep search: "+e.getMessage());
						return false;
					}
				}
				if (particle.getMinOccurs()>0) {
//					if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration mandatory element ["+elementName+"] not found, path fails, autoInsertMandatory ["+isAutoInsertMandatory()+"]");
//					if (isAutoInsertMandatory()) {
//						path.add(particle);
//						if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] not found, nested elements found in deep search");
//						return true;
//					}
					failureReasons.add(MSG_EXPECTED_ELEMENT+" ["+elementName+"]");
					return false;
				}
				if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration optional element ["+elementName+"] not found, path continues");
				return true;
			}
			for (XSParticle resultParticle:path) {
				if (elementName.equals(resultParticle.getTerm().getName())) {
					if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] found but required multiple times");
					failureReasons.add("element ["+elementName+"] required multiple times");
					return false;
				}
			}
			if (log.isTraceEnabled()) log.trace("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] found");
			path.add(particle);
			return true;
		}
		if (term instanceof XSWildcard) {
			XSWildcard wildcard=(XSWildcard)term;
			String processContents;
			switch (wildcard.getProcessContents()) {
			case XSWildcard.PC_LAX: processContents="LAX"; break;
			case XSWildcard.PC_SKIP: processContents="SKIP"; break;
			case XSWildcard.PC_STRICT: processContents="STRICT"; break;
			default: 
					throw new IllegalStateException("getBestMatchingElementPath wildcard.processContents is not PC_LAX, PC_SKIP or PC_STRICT, but ["+wildcard.getProcessContents()+"]");
			}
			String namespaceConstraint;
			switch (wildcard.getConstraintType()) {
			case XSWildcard.NSCONSTRAINT_ANY : namespaceConstraint="ANY"; break;
			case XSWildcard.NSCONSTRAINT_LIST : namespaceConstraint="SKIP "+wildcard.getNsConstraintList(); break;
			case XSWildcard.NSCONSTRAINT_NOT : namespaceConstraint="NOT "+wildcard.getNsConstraintList(); break;
			default: 
					throw new IllegalStateException("getBestMatchingElementPath wildcard.namespaceConstraint is not ANY, LIST or NOT, but ["+wildcard.getConstraintType()+"]");
			}
			String msg="term for element ["+baseElementDeclaration.getName()+"] is WILDCARD; namespaceConstraint ["+namespaceConstraint+"] processContents ["+processContents+"]. Please check if the element typed properly in the schema";
			if (isFailOnWildcards()) {
				throw new IllegalStateException(msg+", or set failOnWildcards=\"false\"");
			}
			log.warn(msg);
			return true;
		} 
		throw new IllegalStateException("getBestMatchingElementPath unknown Term type ["+term.getClass().getName()+"]");
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

	public void handleError(SAXException e) throws SAXException {
		ErrorHandler errorHandler=validatorHandler.getErrorHandler();
		if (errorHandler!=null) {
			errorHandler.error(new SAXParseException(e.getMessage(),null));
		} else {
			throw e;
		}		
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

	
	
	

	public String findNamespaceForName(String name) throws SAXException {
		XSElementDeclaration elementDeclaration=findElementDeclarationForName(null,name);
		if (elementDeclaration==null) {
			return null;
		}
		return elementDeclaration.getNamespace();
	}

	public XSElementDeclaration findElementDeclarationForName(String namespace, String name) throws SAXException {
		Set<XSElementDeclaration> elementDeclarations=findElementDeclarationsForName(namespace, name);
		if (elementDeclarations==null) {
			log.warn("No element declarations found for ["+namespace+"]:["+name+"]");
			return null;
		}
		if (elementDeclarations.size()>1) {
			XSElementDeclaration[] XSElementDeclarationArray=elementDeclarations.toArray(new XSElementDeclaration[0]);
			throw new SAXException("multiple ["+elementDeclarations.size()+"] elementDeclarations found for ["+namespace+"]:["+name+"]: first two ["+XSElementDeclarationArray[0].getNamespace()+":"+XSElementDeclarationArray[0].getName()+"]["+XSElementDeclarationArray[1].getNamespace()+":"+XSElementDeclarationArray[1].getName()+"]");
		}
		if (elementDeclarations.size()==1) {
			return (XSElementDeclaration)elementDeclarations.toArray()[0];
		}
		return null;
	}
	public Set<XSElementDeclaration> findElementDeclarationsForName(String namespace, String name) {
		Set<XSElementDeclaration> result=new LinkedHashSet<XSElementDeclaration>();
		if (schemaInformation==null) {
			log.warn("No SchemaInformation specified, cannot find namespaces for ["+namespace+"]:["+name+"]");
			return null;
		}
		for (XSModel model:schemaInformation) {
			XSNamedMap components = model.getComponents(XSConstants.ELEMENT_DECLARATION);
			for (int i=0;i<components.getLength();i++) {
				XSElementDeclaration item=(XSElementDeclaration)components.item(i);
				if ((namespace==null || namespace.equals(item.getNamespace())) && (name==null || name.equals(item.getName()))) {
					if (log.isTraceEnabled()) log.trace("name ["+item.getName()+"] found in namespace ["+item.getNamespace()+"]");
					result.add(item);
				}
			}
		}
		return result;
	}

	public String translate(C data) throws SAXException {
		XmlWriter xmlWriter = new XmlWriter();
		setContentHandler(xmlWriter);
		startParse(data);
		return xmlWriter.toString();
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

	public List<XSModel> getSchemaInformation() {
		return schemaInformation;
	}
	public void setSchemaInformation(List<XSModel> schemaInformation) {
		this.schemaInformation = schemaInformation;
	}

//	public boolean isAutoInsertMandatory() {
//		return autoInsertMandatory;
//	}
//	public void setAutoInsertMandatory(boolean autoInsertMandatory) {
//		this.autoInsertMandatory = autoInsertMandatory;
//	}

	public boolean isDeepSearch() {
		return deepSearch;
	}

	public void setDeepSearch(boolean deepSearch) {
		this.deepSearch = deepSearch;
	}

	public boolean isFailOnWildcards() {
		return failOnWildcards;
	}

	public void setFailOnWildcards(boolean failOnWildcards) {
		this.failOnWildcards = failOnWildcards;
	}

}
