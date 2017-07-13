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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.ValidatorHandler;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.apache.xerces.xs.ElementPSVI;
import org.apache.xerces.xs.PSVIProvider;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSNamedMap;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * XMLFilter with option to get schema information about child elements to be parsed.
 * 
 * @author Gerrit van Brakel
 *
 * @param <N> Node type 
 */
public class XmlAligner<N> extends XMLFilterImpl {
	protected Logger log = Logger.getLogger(this.getClass());

	private boolean indent=true;
	private int indentLevel;
	protected ValidatorHandler validatorHandler;

	protected List<XSParticle> childElementDeclarations=null;
	protected List<XSModel> schemaInformation; 
	protected Stack<Set<String>> multipleOccurringElements=new Stack<Set<String>>();
	protected Set<String> multipleOccurringChildElements=null;

	private final char[] INDENTOR="\n                                                                                         ".toCharArray();
	private final int MAX_INDENT=INDENTOR.length/2;
	
	private final boolean DEBUG=false; 
	
	public XmlAligner() {
		super();
	}

	public XmlAligner(ValidatorHandler validatorHandler) {
		this();
		setValidatorHandler(validatorHandler);
	}
	
	public XmlAligner(Schema schema) {
		this();
		setSchema(schema);
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
		
	}

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
		if (input!=null && (input instanceof XmlAligner.XmlAlignerInputSource)) {
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

	public void newLine() throws SAXException {
		newLine(0);
	}
	public void newLine(int offset) throws SAXException {
		if (indent) {
			int level=indentLevel+offset;
			validatorHandler.ignorableWhitespace(INDENTOR, 0, (level<MAX_INDENT?level:MAX_INDENT)*2+1);
		}
	}
	
	@Override
	public void startElement(String namespaceUri, String localName, String qName, Attributes attributes) throws SAXException {
		if (DEBUG && log.isDebugEnabled()) log.debug("startElement() uri ["+namespaceUri+"] localName ["+localName+"] qName ["+qName+"]");
		childElementDeclarations=getChildElementDeclarations((PSVIProvider)validatorHandler);
		multipleOccurringElements.push(multipleOccurringChildElements);
		multipleOccurringChildElements=findMultipleOccurringChildElements();
		super.startElement(namespaceUri, localName, qName, attributes);
		indentLevel++;
	}
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (DEBUG && log.isDebugEnabled()) log.debug("endElement() uri ["+uri+"] localName ["+localName+"] qName ["+qName+"]");
		indentLevel--;
		super.endElement(uri, localName, qName);
		multipleOccurringChildElements=multipleOccurringElements.pop();
	}


	protected void sendString(String string) throws SAXException {
		validatorHandler.characters(string.toCharArray(), 0, string.length());
	}
	
	public boolean isPresentInSet(Set<String> set, String name) {
		return set!=null && set.contains(name);
	}
	public boolean isMultipleOccuringChildElement(String name) {
		return isPresentInSet(multipleOccurringChildElements,name);
	}
	public boolean isMultipleOccuringChildInParentElement(String name) {
		return isPresentInSet(multipleOccurringElements.peek(),name);
	}

	protected Set<String> findMultipleOccurringChildElements() {
		if (childElementDeclarations==null) {
			if (DEBUG && log.isDebugEnabled()) log.debug("findMultipleOccurringChildElements() no childElementDeclarations");
			return null;
		}
		Set<String> result=new HashSet<String>();
		for (XSParticle particle:childElementDeclarations) {
			XSTerm term = particle.getTerm();
			String name=term.getName();
			if (DEBUG && log.isDebugEnabled()) log.debug("findMultipleOccurringChildElements() child element ["+name+"] occurring unbounded ["+particle.getMaxOccursUnbounded()+"] max occur ["+particle.getMaxOccurs()+"]");
			if (particle.getMaxOccursUnbounded() || particle.getMaxOccurs()>1) {
				if (DEBUG && log.isDebugEnabled()) log.debug("findMultipleOccurringChildElements() multiple occurring child element ["+name+"]");
				result.add(name);
			}
		}
		return result;
	}
	public List<XSParticle> getChildElementDeclarations(PSVIProvider psviProvider) {
		ElementPSVI elementPSVI = psviProvider.getElementPSVI();
		if (DEBUG && log.isDebugEnabled()) log.debug("elementPSVI ["+ToStringBuilder.reflectionToString(elementPSVI)+"]");
		XSElementDeclaration elementDeclaration = elementPSVI.getElementDeclaration();
		if (DEBUG && log.isDebugEnabled()) log.debug("elementPSVI element declaration ["+ToStringBuilder.reflectionToString(elementDeclaration)+"]");
		
		XSTypeDefinition typeDefinition = elementDeclaration.getTypeDefinition();
		if (DEBUG && log.isDebugEnabled()) log.debug("elementDeclaration typeDefinition ["+ToStringBuilder.reflectionToString(typeDefinition)+"]");
		
		if (typeDefinition instanceof XSComplexTypeDefinition) {
			XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition)typeDefinition;
			XSParticle particle = complexTypeDefinition.getParticle();
			if (particle==null) {
				log.warn("typeDefinition particle is null, is this a problem?");					
			} else {
				if (DEBUG && log.isDebugEnabled()) log.debug("typeDefinition particle ["+ToStringBuilder.reflectionToString(particle,ToStringStyle.MULTI_LINE_STYLE)+"]");
				XSTerm term = particle.getTerm();
				if (DEBUG && log.isDebugEnabled()) log.debug("particle term ["+ToStringBuilder.reflectionToString(term,ToStringStyle.MULTI_LINE_STYLE)+"]");
				
				if (term instanceof XSModelGroup) {
					XSModelGroup modelGroup = (XSModelGroup)term;
					short compositor = modelGroup.getCompositor();
//					if (compositor==XSModelGroup.COMPOSITOR_SEQUENCE || compositor==XSModelGroup.COMPOSITOR_ALL) {
						XSObjectList particles = modelGroup.getParticles();
						if (DEBUG && log.isDebugEnabled()) log.debug("modelGroup particles ["+ToStringBuilder.reflectionToString(particles,ToStringStyle.MULTI_LINE_STYLE)+"]");
						List<XSParticle> result = new LinkedList<XSParticle>();

						for (int i=0;i<particles.getLength();i++) {
							XSParticle childParticle = (XSParticle)particles.item(i);
							XSTerm childTerm = childParticle.getTerm();
							if (childTerm instanceof XSElementDeclaration) {
								XSElementDeclaration childElementDeclaration=(XSElementDeclaration)childTerm;
//								if (DEBUG && log.isDebugEnabled()) log.debug("childElementDeclaration ["+ToStringBuilder.reflectionToString(childElementDeclaration,ToStringStyle.MULTI_LINE_STYLE)+"]");
								if (DEBUG && log.isDebugEnabled()) log.debug("childElementDeclaration name ["+childElementDeclaration.getName()+"]");
								result.add(childParticle);
							} else {
								if (log.isDebugEnabled()) log.debug("childTerm is not a XSElementDeclaration, but is ["+ToStringBuilder.reflectionToString(childTerm,ToStringStyle.MULTI_LINE_STYLE)+"]");
							}
						}
						childElementDeclarations=result;
						return result;
//					} 
//					log.warn("modelGroup compositor is not a COMPOSITOR_SEQUENCE or a COMPOSITOR_ALL, ignoring");
				} else {
					log.warn("particle term is not a XSModelGroup, ignoring (for now)");
				}
			}
		} else {
			if (DEBUG && log.isDebugEnabled()) log.debug("simple type, no children");
		}
		return null;
	}

	public String findNamespaceForName(String name) throws SAXException {
		List<String> namespaces=findNamespacesForName(name);
		if (namespaces.size()>1) {
			throw new SAXException("multiple namespaces found for ["+name+"]");
		}
		if (namespaces.size()==1) {
			return namespaces.get(0);
		}
		return null;
	}
	public List<String> findNamespacesForName(String name) {
		if (DEBUG && log.isDebugEnabled()) log.debug("schemaInformation ["+ToStringBuilder.reflectionToString(schemaInformation,ToStringStyle.MULTI_LINE_STYLE)+"]");
		List<String> result=new LinkedList<String>();
		for (XSModel model:schemaInformation) {
			XSNamedMap components = model.getComponents(XSConstants.ELEMENT_DECLARATION);
			for (int i=0;i<components.getLength();i++) {
				XSObject item=components.item(i);
				if (DEBUG && log.isDebugEnabled()) log.debug("found component name ["+item.getName()+"] in namespace ["+item.getNamespace()+"]");
				if (item.getName().equals(name)) {
					if (log.isDebugEnabled()) log.debug("name ["+name+"] found in namespace ["+item.getNamespace()+"]");
					result.add(item.getNamespace());
				}
			}
		}
		return result;
	}
	
	public void setValidatorHandler(ValidatorHandler validatorHandler) {
		this.validatorHandler=validatorHandler;
		this.validatorHandler.setContentHandler(this);
	}
	
	public void setSchema(Schema schema) {
		setValidatorHandler(schema.newValidatorHandler());
	}

	public List<XSModel> getSchemaInformation() {
		return schemaInformation;
	}

	public void setSchemaInformation(List<XSModel> schemaInformation) {
		this.schemaInformation = schemaInformation;
	}

}
