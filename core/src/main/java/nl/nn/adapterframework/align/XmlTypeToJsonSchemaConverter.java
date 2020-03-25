/*
   Copyright 2020 WeAreFrank!

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

import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.log4j.Logger;
import org.apache.xerces.xs.StringList;
import org.apache.xerces.xs.XSAttributeDeclaration;
import org.apache.xerces.xs.XSAttributeUse;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSConstants;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSModelGroup;
import org.apache.xerces.xs.XSObjectList;
import org.apache.xerces.xs.XSObject;
import org.apache.xerces.xs.XSParticle;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTerm;
import org.apache.xerces.xs.XSTypeDefinition;
import org.apache.xerces.xs.XSWildcard;

import nl.nn.adapterframework.util.LogUtil;

public class XmlTypeToJsonSchemaConverter  {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private List<XSModel> models;
	private boolean skipArrayElementContainers;
	private boolean skipRootElement;

	protected final boolean DEBUG=false; 

	public XmlTypeToJsonSchemaConverter(List<XSModel> models, boolean skipArrayElementContainers, boolean skipRootElement) {
		this.models=models;
		this.skipArrayElementContainers=skipArrayElementContainers;
		this.skipRootElement=skipRootElement;
	}

	public JsonStructure createJsonSchema(String elementName, String namespace) {
		XSElementDeclaration elementDecl=findElementDeclaration(elementName, namespace);
		if (elementDecl==null && namespace!=null) {
			elementDecl=findElementDeclaration(elementName, null);
		}
		if (elementDecl==null) {
			log.warn("Cannot find declaration for element ["+elementName+"]");
			if (DEBUG) 
				for (XSModel model:models) {
					log.debug("model ["+ToStringBuilder.reflectionToString(model,ToStringStyle.MULTI_LINE_STYLE)+"]");
				}
			return null;
		}
		return createJsonSchema(elementName, elementDecl);
	}

	public XSElementDeclaration findElementDeclaration(String elementName, String namespace) {
		for (XSModel model:models) {
			if (log.isDebugEnabled()) log.debug("search for element ["+elementName+"] in namespace ["+namespace+"]");
			XSElementDeclaration elementDecl = model.getElementDeclaration(elementName, namespace);
			if (elementDecl!=null) {
				if (DEBUG) log.debug("findTypeDefinition found elementDeclaration ["+ToStringBuilder.reflectionToString(elementDecl,ToStringStyle.MULTI_LINE_STYLE)+"]");
				return elementDecl;
			}
		}
		if (namespace==null) {
			for (XSModel model:models) {
				StringList namespaces = model.getNamespaces();
				for (int i=0;i<namespaces.getLength();i++) {
					namespace = (String)namespaces.item(i);
					if (log.isDebugEnabled()) log.debug("search for element ["+elementName+"] in namespace ["+namespace+"]");
					XSElementDeclaration elementDecl = model.getElementDeclaration(elementName, namespace);
					if (elementDecl!=null) {
						if (DEBUG) log.debug("findTypeDefinition found elementDeclaration ["+ToStringBuilder.reflectionToString(elementDecl,ToStringStyle.MULTI_LINE_STYLE)+"]");
						return elementDecl;
					}
				}
			}
			
		}
		
		return null;
	}
	
	public JsonStructure createJsonSchema(String elementName, XSElementDeclaration elementDecl) {
		if (skipRootElement) {
			return getDefinition(elementDecl.getTypeDefinition());
		} else {
			JsonObjectBuilder builder = Json.createObjectBuilder();
			buildTerm(builder,elementDecl,null, false);
			return builder.build();
		}
	}
	

	public JsonStructure getDefinition(XSTypeDefinition typeDefinition) {
		if (typeDefinition instanceof XSComplexTypeDefinition) {
			XSComplexTypeDefinition complexTypeDefinition = (XSComplexTypeDefinition)typeDefinition;
			JsonObjectBuilder builder = Json.createObjectBuilder();
			switch (complexTypeDefinition.getContentType()) {
			case XSComplexTypeDefinition.CONTENTTYPE_EMPTY:
				if (DEBUG) log.debug("handleElementContents complexTypeDefinition.contentType is Empty, no child elements");
				break;
			case XSComplexTypeDefinition.CONTENTTYPE_SIMPLE:
				if (DEBUG) log.debug("handleElementContents complexTypeDefinition.contentType is Simple, no child elements (only characters)");
				break;
			case XSComplexTypeDefinition.CONTENTTYPE_ELEMENT:
				if (DEBUG) log.debug("handleElementContents complexTypeDefinition.contentType is Element, complexTypeDefinition ["+ToStringBuilder.reflectionToString(complexTypeDefinition,ToStringStyle.MULTI_LINE_STYLE)+"]");
				XSObjectList attributeUses = complexTypeDefinition.getAttributeUses();
				if (attributeUses.getLength()>0) {
					for (int i=0; i<attributeUses.getLength(); i++) {
						XSAttributeUse attributeUse = (XSAttributeUse)attributeUses.get(i);
						if (DEBUG) log.debug("handleElementContents complexTypeDefinition.contentType is Element, attribute ["+ToStringBuilder.reflectionToString(attributeUse.getAttrDeclaration(),ToStringStyle.MULTI_LINE_STYLE)+"]");
					}
				}
				XSParticle particle = complexTypeDefinition.getParticle();
				buildParticle(builder, particle, attributeUses);
				break;
			case XSComplexTypeDefinition.CONTENTTYPE_MIXED:
				if (DEBUG) log.debug("handleElementContents complexTypeDefinition.contentType is Mixed");
				break;
			default:
				throw new IllegalStateException("handleElementContents complexTypeDefinition.contentType is not Empty,Simple,Element or Mixed, but ["+complexTypeDefinition.getContentType()+"]");
			}
			if (DEBUG) log.debug(ToStringBuilder.reflectionToString(complexTypeDefinition,ToStringStyle.MULTI_LINE_STYLE));
			return builder.build();
		} else {
			XSSimpleTypeDefinition simpleTypeDefinition = (XSSimpleTypeDefinition)typeDefinition;
			if (DEBUG) log.debug("typeDefinition.name ["+typeDefinition.getName()+"]");;
			if (DEBUG) log.debug("simpleTypeDefinition.getBuiltInKind ["+simpleTypeDefinition.getBuiltInKind()+"]");
			if (DEBUG) log.debug(ToStringBuilder.reflectionToString(typeDefinition,ToStringStyle.MULTI_LINE_STYLE));

 			JsonObjectBuilder builder = Json.createObjectBuilder();
			short builtInKind = simpleTypeDefinition.getBuiltInKind();
			String dataType = getJsonDataType(builtInKind);
			
			if (dataType.equalsIgnoreCase("integer") || dataType.equalsIgnoreCase("number")) {
				builder.add("type", dataType.toLowerCase());

				applyFacet(simpleTypeDefinition, builder, "maximum", XSSimpleTypeDefinition.FACET_MAXINCLUSIVE);
				applyFacet(simpleTypeDefinition, builder, "minimum", XSSimpleTypeDefinition.FACET_MININCLUSIVE);
				applyFacet(simpleTypeDefinition, builder, "exclusiveMaximum", XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE);
				applyFacet(simpleTypeDefinition, builder, "exclusiveMinimum", XSSimpleTypeDefinition.FACET_MINEXCLUSIVE);
				applyFacet(simpleTypeDefinition, builder, "enumeration", XSSimpleTypeDefinition.FACET_ENUMERATION);
			} else if (dataType.equalsIgnoreCase("boolean")) {
				builder.add("type", "boolean");
			} else if (dataType.equalsIgnoreCase("string")) {	
				builder.add("type", "string");
			
				applyFacet(simpleTypeDefinition, builder, "maxLength", XSSimpleTypeDefinition.FACET_MAXLENGTH);
				applyFacet(simpleTypeDefinition, builder, "minLength", XSSimpleTypeDefinition.FACET_MINLENGTH);
				applyFacet(simpleTypeDefinition, builder, "pattern", XSSimpleTypeDefinition.FACET_PATTERN);
				applyFacet(simpleTypeDefinition, builder, "enumeration", XSSimpleTypeDefinition.FACET_ENUMERATION);
			} else if (dataType.equalsIgnoreCase("date") || dataType.equalsIgnoreCase("date-time") || dataType.equalsIgnoreCase("time")) {		
				builder.add("type", dataType);

				applyFacet(simpleTypeDefinition, builder, "pattern", XSSimpleTypeDefinition.FACET_PATTERN);
				applyFacet(simpleTypeDefinition, builder, "enumeration", XSSimpleTypeDefinition.FACET_ENUMERATION);
			}
			
//			attributeDecl.getTypeDefinition();
//			String type;
//			switch(attributeDecl.getType()) {
//			case XSConstants.BOOLEAN_DT:
//				type="boolean";
//				break;
//			case XSConstants.LONG_DT:
//			case XSConstants.SHORT_DT:
//				type="integer";
//				break;
//			case XSConstants.DECIMAL_DT:
//			case XSConstants.FLOAT_DT:
//			case XSConstants.DOUBLE_DT:
//				type="number";
//				break;
//			default:
//				type="string";
//				break;
//			}
			return builder.build();
		}
		//return null;
	}
	
	public void buildParticle(JsonObjectBuilder builder, XSParticle particle, XSObjectList attributeUses) {
		if (particle==null) {
			throw new NullPointerException("particle is null");
		} 
		XSTerm term = particle.getTerm();
		if (term==null) {
			throw new NullPointerException("particle.term is null");
		} 
		buildTerm(builder,term,attributeUses, particle.getMaxOccursUnbounded() || particle.getMaxOccurs()>1);
	}
	public void buildTerm(JsonObjectBuilder builder, XSTerm term, XSObjectList attributeUses, boolean multiOccurring) {
		if (term instanceof XSModelGroup) {
			XSModelGroup modelGroup = (XSModelGroup)term;
			short compositor = modelGroup.getCompositor();			
			XSObjectList particles = modelGroup.getParticles();
			if (DEBUG) log.debug("modelGroup particles ["+ToStringBuilder.reflectionToString(particles,ToStringStyle.MULTI_LINE_STYLE)+"]");
			switch (compositor) {
			case XSModelGroup.COMPOSITOR_SEQUENCE:
			case XSModelGroup.COMPOSITOR_ALL:
				if (DEBUG) log.debug("modelGroup COMPOSITOR_SEQUENCE or COMPOSITOR_ALL");
				if (skipArrayElementContainers && particles.getLength()==1) {
					XSParticle childParticle = (XSParticle)particles.item(0);
					if (childParticle.getMaxOccursUnbounded() || childParticle.getMaxOccurs()>1) {
						if (DEBUG) log.debug("skippable array element childParticle ["+ToStringBuilder.reflectionToString(particles.item(0),ToStringStyle.MULTI_LINE_STYLE)+"]");
						buildParticle(builder,childParticle,null);
						return;
					}
				}
				builder.add("type", "object");
				builder.add("additionalProperties", false);
				JsonObjectBuilder propertiesBuilder = Json.createObjectBuilder();
				if (attributeUses!=null) {
					for (int i=0; i< attributeUses.getLength(); i++) {
						XSAttributeUse attributeUse = (XSAttributeUse)attributeUses.get(i);
						XSAttributeDeclaration attributeDecl = attributeUse.getAttrDeclaration();
						propertiesBuilder.add("@"+attributeDecl.getName(), getDefinition(attributeDecl.getTypeDefinition()));
					}
				}
				for (int i=0;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					if (DEBUG) log.debug("childParticle ["+i+"]["+ToStringBuilder.reflectionToString(childParticle,ToStringStyle.MULTI_LINE_STYLE)+"]");
					buildParticle(propertiesBuilder, childParticle, null);
//					if (!getBestMatchingElementPath(baseElementDeclaration, baseNode, childParticle,path,failureReasons)) {
//						return false;
//					}
				}
				builder.add("properties", propertiesBuilder.build());
				return;
			case XSModelGroup.COMPOSITOR_CHOICE:
				if (DEBUG) log.debug("modelGroup COMPOSITOR_CHOICE");
				JsonArrayBuilder oneOfBuilder = Json.createArrayBuilder();
				for (int i=0;i<particles.getLength();i++) {
					XSParticle childParticle = (XSParticle)particles.item(i);
					if (DEBUG) log.debug("childParticle ["+i+"]["+ToStringBuilder.reflectionToString(childParticle,ToStringStyle.MULTI_LINE_STYLE)+"]");
					JsonObjectBuilder typeBuilder = Json.createObjectBuilder();
					buildParticle(typeBuilder,childParticle,null);
					oneOfBuilder.add(typeBuilder.build());
				}
				builder.add("oneOf", oneOfBuilder.build());
				return;
			default:
				throw new IllegalStateException("getTerm modelGroup.compositor is not COMPOSITOR_SEQUENCE, COMPOSITOR_ALL or COMPOSITOR_CHOICE, but ["+compositor+"]");
			} 
		} 
		if (term instanceof XSElementDeclaration) {
			XSElementDeclaration elementDeclaration=(XSElementDeclaration)term;
			String elementName=elementDeclaration.getName();
			//if (DEBUG) log.debug("XSElementDeclaration name ["+elementName+"]");
			if (DEBUG) log.debug("XSElementDeclaration element ["+elementName+"]["+ToStringBuilder.reflectionToString(elementDeclaration,ToStringStyle.MULTI_LINE_STYLE)+"]");
			XSTypeDefinition elementTypeDefinition = elementDeclaration.getTypeDefinition();
			JsonStructure definition =getDefinition(elementTypeDefinition);
			if (elementDeclaration.getNillable()) {
				definition=nillable(definition);
			}
			if (multiOccurring) {
				JsonObjectBuilder arrayBuilder = Json.createObjectBuilder();
				arrayBuilder.add("type", "array");
				arrayBuilder.add("items", definition);
				builder.add(elementName, arrayBuilder.build());
			} else {
				if (definition!=null) {
		 			builder.add(elementName, definition);
				}
			}
//			if (!hasChild(baseElementDeclaration, baseNode, elementName)) {
//				if (isDeepSearch()) {
//					if (DEBUG) log.debug("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] not found, perform deep search");
//					try {
//						List<XSParticle> subList=getBestChildElementPath(elementDeclaration,baseNode, true);
//						if (subList!=null && !subList.isEmpty()) {
//							path.add(particle);
//							if (DEBUG) log.debug("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] not found, nested elements found in deep search");
//							return true;
//						}
//						if (DEBUG) log.debug("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] not found, no nested elements found in deep search");
//					} catch (Exception e) {
//						if (DEBUG) log.debug("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] not found, no nested elements found in deep search: "+e.getMessage());
//						return false;
//					}
//				}
//				if (particle.getMinOccurs()>0) {
////					if (DEBUG) log.debug("getBestMatchingElementPath().XSElementDeclaration mandatory element ["+elementName+"] not found, path fails, autoInsertMandatory ["+isAutoInsertMandatory()+"]");
////					if (isAutoInsertMandatory()) {
////						path.add(particle);
////						if (DEBUG) log.debug("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] not found, nested elements found in deep search");
////						return true;
////					}
//					failureReasons.add(MSG_EXPECTED_ELEMENT+" ["+elementName+"]");
//					return false;
//				}
//				if (DEBUG) log.debug("getBestMatchingElementPath().XSElementDeclaration optional element ["+elementName+"] not found, path continues");
//				return true;
//			}
//			for (XSParticle resultParticle:path) {
//				if (elementName.equals(resultParticle.getTerm().getName())) {
//					if (DEBUG) log.debug("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] found but required multiple times");
//					failureReasons.add("element ["+elementName+"] required multiple times");
//					return false;
//				}
//			}
//			if (DEBUG) log.debug("getBestMatchingElementPath().XSElementDeclaration element ["+elementName+"] found");
//			path.add(particle);
			return;
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
//			String msg="term for element ["+baseElementDeclaration.getName()+"] is WILDCARD; namespaceConstraint ["+namespaceConstraint+"] processContents ["+processContents+"]. Please check if the element typed properly in the schema";
//			if (isFailOnWildcards()) {
//				throw new IllegalStateException(msg+", or set failOnWildcards=\"false\"");
//			}
//			log.warn(msg);
			return;
		} 
		throw new IllegalStateException("getBestMatchingElementPath unknown Term type ["+term.getClass().getName()+"]");
		
	}

	public JsonStructure nillable(JsonStructure type) {
		JsonObjectBuilder typeBuilder = Json.createObjectBuilder();
		JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
		arrayBuilder.add(type);
		arrayBuilder.add(Json.createObjectBuilder().add("type", "null"));
		
		typeBuilder.add("anyOf", arrayBuilder.build());
		return typeBuilder.build();
	}

	private void applyFacet(XSSimpleTypeDefinition simpleTypeDefinition, JsonObjectBuilder builder, String key, short facet){
		if(simpleTypeDefinition.getFacet(facet) != null){
			if(simpleTypeDefinition.getLexicalFacetValue(facet) != null){
				switch(facet){
					case XSSimpleTypeDefinition.FACET_MAXINCLUSIVE:
					case XSSimpleTypeDefinition.FACET_MININCLUSIVE:
					case XSSimpleTypeDefinition.FACET_MAXEXCLUSIVE:
					case XSSimpleTypeDefinition.FACET_MINEXCLUSIVE:
					case XSSimpleTypeDefinition.FACET_MAXLENGTH:
					case XSSimpleTypeDefinition.FACET_MINLENGTH:
						if(simpleTypeDefinition.getBuiltInKind() == XSConstants.LONG_DT){
							builder.add(key, Long.parseLong(simpleTypeDefinition.getLexicalFacetValue(facet)));
						}
						else {
							builder.add(key, Integer.parseInt(simpleTypeDefinition.getLexicalFacetValue(facet)));
						}
						break;
					default:
						builder.add(key, simpleTypeDefinition.getLexicalFacetValue(facet));
						break;
				}
			}
		}
	}

	private String getJsonDataType(short builtInKind){
		String type;
		switch(builtInKind) {
			case XSConstants.BOOLEAN_DT:
				type="boolean";
				break;
			case XSConstants.LONG_DT:
			case XSConstants.SHORT_DT:
			case XSConstants.INT_DT:
			case XSConstants.INTEGER_DT:
			case XSConstants.NEGATIVEINTEGER_DT:
			case XSConstants.NONNEGATIVEINTEGER_DT:
			case XSConstants.NONPOSITIVEINTEGER_DT:
			case XSConstants.POSITIVEINTEGER_DT:
				type="integer";
				break;
			case XSConstants.DECIMAL_DT:
			case XSConstants.FLOAT_DT:
			case XSConstants.DOUBLE_DT:
				type="number";
				break;
			case XSConstants.DATE_DT:
				type="date";
				break;
			case XSConstants.DATETIME_DT:
				type="date-time";
				break;
			default:
				type="string";
				break;
		}
		return type;
	}

}