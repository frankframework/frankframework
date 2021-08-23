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

package nl.nn.adapterframework.frankdoc;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

class DocWriterNewXmlUtils {
	private static Logger log = LogUtil.getLogger(DocWriterNewXmlUtils.class);

	static final String XML_SCHEMA_URI = "http://www.w3.org/2001/XMLSchema";

	private DocWriterNewXmlUtils() {
	}

	static XmlBuilder getXmlSchema() {
		XmlBuilder schema = new XmlBuilder("schema", "xs", XML_SCHEMA_URI);
		schema.addAttribute("xmlns:xs", XML_SCHEMA_URI);
		schema.addAttribute("elementFormDefault", "qualified");
		return schema;
	}

	static XmlBuilder addElement(XmlBuilder context, String elementName, String elementType, String minOccurs, String maxOccurs) {
		XmlBuilder element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("name", elementName);
		element.addAttribute("minOccurs", minOccurs);
		element.addAttribute("maxOccurs", maxOccurs);
		element.addAttribute("type", elementType);
		context.addSubElement(element);
		return element;
	}

	static XmlBuilder addElementRef(XmlBuilder context, String elementName, String minOccurs, String maxOccurs) {
		XmlBuilder element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("ref", elementName);
		element.addAttribute("minOccurs", minOccurs);
		element.addAttribute("maxOccurs", maxOccurs);
		context.addSubElement(element);
		return element;
	}

	static void addElementRef(XmlBuilder context, String elementName) {
		XmlBuilder element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("ref", elementName);
		context.addSubElement(element);
	}

	static XmlBuilder addElementWithType(XmlBuilder context, String name) {
		XmlBuilder element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("name", name);
		context.addSubElement(element);
		return element;
	}

	static XmlBuilder createElementWithType(String name) {
		XmlBuilder element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("name", name);
		return element;
	}

	static XmlBuilder addComplexType(XmlBuilder schema) {
		XmlBuilder complexType;
		complexType = new XmlBuilder("complexType", "xs", XML_SCHEMA_URI);
		schema.addSubElement(complexType);
		return complexType;
	}

	static XmlBuilder createComplexType(String name) {
		XmlBuilder complexType;
		complexType = new XmlBuilder("complexType", "xs", XML_SCHEMA_URI);
		complexType.addAttribute("name", name);
		return complexType;		
	}

	static XmlBuilder addSimpleType(XmlBuilder schema) {
		XmlBuilder complexType;
		complexType = new XmlBuilder("simpleType", "xs", XML_SCHEMA_URI);
		schema.addSubElement(complexType);
		return complexType;
	}

	static XmlBuilder createSimpleType(String name) {
		XmlBuilder simpleType = new XmlBuilder("simpleType", "xs", XML_SCHEMA_URI);
		simpleType.addAttribute("name", name);
		return simpleType;		
	}

	static XmlBuilder addComplexType(XmlBuilder schema, String name) {
		XmlBuilder complexType;
		complexType = new XmlBuilder("complexType", "xs", XML_SCHEMA_URI);
		complexType.addAttribute("name", name);
		schema.addSubElement(complexType);
		return complexType;		
	}

	static XmlBuilder addChoice(XmlBuilder context) {
		XmlBuilder choice = new XmlBuilder("choice", "xs", XML_SCHEMA_URI);
		context.addSubElement(choice);
		return choice;
	}

	static XmlBuilder addChoice(XmlBuilder context, String minOccurs, String maxOccurs) {
		XmlBuilder choice = new XmlBuilder("choice", "xs", XML_SCHEMA_URI);
		context.addSubElement(choice);
		choice.addAttribute("minOccurs", minOccurs);
		choice.addAttribute("maxOccurs", maxOccurs);
		return choice;
	}

	static XmlBuilder addSequence(XmlBuilder context) {
		XmlBuilder sequence = new XmlBuilder("sequence", "xs", XML_SCHEMA_URI);
		context.addSubElement(sequence);
		return sequence;
	}

	enum AttributeUse {
		OPTIONAL,
		REQUIRED,
		PROHIBITED;
	}

	enum AttributeValueStatus {
		DEFAULT("default"),
		FIXED("fixed");

		@Getter
		private final String xsdWord;

		private AttributeValueStatus(String xsdWord) {
			this.xsdWord = xsdWord;
		}
	}

	static XmlBuilder addAttribute(XmlBuilder context, String name, AttributeValueStatus valueStatus, String value, AttributeUse attributeUse) {
		XmlBuilder result = startAddingAttribute(context, name);
		try {
			addValueToAttribute(result, valueStatus, value);
		} catch(AttributeFormatException e) {
			log.warn("Error formatting attribute [{}]", name, e);
		}
		addUsageToAttribute(result, attributeUse);
		return result;
	}

	private static XmlBuilder startAddingAttribute(XmlBuilder context, String name) {
		XmlBuilder attribute = new XmlBuilder("attribute", "xs", XML_SCHEMA_URI);
		attribute.addAttribute("name", name);
		attribute.addAttribute("type", "xs:string");
		context.addSubElement(attribute);
		return attribute;
	}

	@SuppressWarnings("serial")
	private static class AttributeFormatException extends Exception {
		AttributeFormatException(String msg) {
			super(msg);
		}
	}

	private static void addValueToAttribute(XmlBuilder result, AttributeValueStatus valueStatus, String value) throws AttributeFormatException {
		if(value == null) {
			if(valueStatus == AttributeValueStatus.FIXED) {
				throw new AttributeFormatException("Attribute values can be omitted, but then they cannot be fixed");
			}
		}
		else {
			result.addAttribute(valueStatus.getXsdWord(), value);
		}
	}

	private static void addUsageToAttribute(XmlBuilder result, AttributeUse attributeUse) {
		switch(attributeUse) {
		case OPTIONAL:
			break;
		case REQUIRED:
			result.addAttribute("use", "required");
			break;
		case PROHIBITED:
			result.addAttribute("use", "prohibited");
			break;
		}
	}

	static XmlBuilder addAttribute(XmlBuilder context, String name, String typeName) {
		XmlBuilder attribute = new XmlBuilder("attribute", "xs", XML_SCHEMA_URI);
		attribute.addAttribute("name", name);
		attribute.addAttribute("type", typeName);
		context.addSubElement(attribute);
		return attribute;
	}

	static XmlBuilder addAttributeRef(XmlBuilder context, String name) {
		XmlBuilder attribute = new XmlBuilder("attribute", "xs", XML_SCHEMA_URI);
		attribute.addAttribute("ref", name);
		context.addSubElement(attribute);
		return attribute;
	}

	static XmlBuilder addAnyAttribute(XmlBuilder context) {
		XmlBuilder attribute = new XmlBuilder("anyAttribute", "xs", XML_SCHEMA_URI);
		context.addSubElement(attribute);
		attribute.addAttribute("processContents", "skip");
		return attribute;
	}

	static XmlBuilder addAttributeWithType(XmlBuilder context, String name) {
		XmlBuilder attribute = new XmlBuilder("attribute", "xs", XML_SCHEMA_URI);
		attribute.addAttribute("name", name);
		context.addSubElement(attribute);
		return attribute;
	}

	static void addDocumentation(XmlBuilder context, String description) {
		XmlBuilder annotation = new XmlBuilder("annotation", "xs", XML_SCHEMA_URI);
		context.addSubElement(annotation);
		XmlBuilder documentation = new XmlBuilder("documentation", "xs", XML_SCHEMA_URI);
		annotation.addSubElement(documentation);
		documentation.setValue(description);
	}

	static XmlBuilder addGroup(XmlBuilder context, String name) {
		XmlBuilder group = new XmlBuilder("group", "xs", XML_SCHEMA_URI);
		context.addSubElement(group);
		group.addAttribute("name", name);
		return group;
	}

	static XmlBuilder createGroup(String name) {
		XmlBuilder group = new XmlBuilder("group", "xs", XML_SCHEMA_URI);
		group.addAttribute("name", name);
		return group;
	}

	static XmlBuilder addGroupRef(XmlBuilder context, String id) {
		XmlBuilder group = new XmlBuilder("group", "xs", XML_SCHEMA_URI);
		context.addSubElement(group);
		group.addAttribute("ref", id);
		return group;
	}

	static XmlBuilder addGroupRef(XmlBuilder context, String id, String minOccurs, String maxOccurs) {
		XmlBuilder group = new XmlBuilder("group", "xs", XML_SCHEMA_URI);
		group.addAttribute("ref", id);
		group.addAttribute("minOccurs", minOccurs);
		group.addAttribute("maxOccurs", maxOccurs);
		context.addSubElement(group);
		return group;
	}

	static XmlBuilder addAttributeGroup(XmlBuilder context, String name) {
		XmlBuilder group = new XmlBuilder("attributeGroup", "xs", XML_SCHEMA_URI);
		context.addSubElement(group);
		group.addAttribute("name", name);
		return group;
	}

	static XmlBuilder createAttributeGroup(String name) {
		XmlBuilder group = new XmlBuilder("attributeGroup", "xs", XML_SCHEMA_URI);
		group.addAttribute("name", name);
		return group;
	}

	static XmlBuilder addAttributeGroupRef(XmlBuilder context, String name) {
		XmlBuilder group = new XmlBuilder("attributeGroup", "xs", XML_SCHEMA_URI);
		context.addSubElement(group);
		group.addAttribute("ref", name);
		return group;
	}

	static XmlBuilder addComplexContent(XmlBuilder context) {
		XmlBuilder complexContent = new XmlBuilder("complexContent", "xs", XML_SCHEMA_URI);
		context.addSubElement(complexContent);
		return complexContent;
	}

	static XmlBuilder addExtension(XmlBuilder context, String base) {
		XmlBuilder extension = new XmlBuilder("extension", "xs", XML_SCHEMA_URI);
		context.addSubElement(extension);
		extension.addAttribute("base", base);
		return extension;
	}

	static XmlBuilder addRestriction(XmlBuilder context, String base) {
		XmlBuilder restriction = new XmlBuilder("restriction", "xs", XML_SCHEMA_URI);
		context.addSubElement(restriction);
		restriction.addAttribute("base", base);
		return restriction;
	}

	static XmlBuilder addEnumeration(XmlBuilder context, String item) {
		XmlBuilder enumeration = new XmlBuilder("enumeration", "xs", XML_SCHEMA_URI);
		context.addSubElement(enumeration);
		enumeration.addAttribute("value", item);
		return enumeration;
	}

	static XmlBuilder addUnion(XmlBuilder context, String ...combinedTypes) {
		XmlBuilder union = new XmlBuilder("union", "xs", XML_SCHEMA_URI);
		context.addSubElement(union);
		String memberTypes = Arrays.asList(combinedTypes).stream().collect(Collectors.joining(" "));
		union.addAttribute("memberTypes", memberTypes);
		return union;
	}

	static XmlBuilder addPattern(XmlBuilder context, String pattern) {
		XmlBuilder result = new XmlBuilder("pattern", "xs", XML_SCHEMA_URI);
		context.addSubElement(result);
		result.addAttribute("value", pattern);
		return result;
	}
}
