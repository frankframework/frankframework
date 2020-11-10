package nl.nn.adapterframework.doc;

import nl.nn.adapterframework.util.XmlBuilder;

class DocWriterNewXmlUtils {
	private static final String XML_SCHEMA_URI = "http://www.w3.org/2001/XMLSchema";

	private DocWriterNewXmlUtils() {
	}

	static XmlBuilder getXmlSchema() {
		XmlBuilder schema = new XmlBuilder("schema", "xs", XML_SCHEMA_URI);
		schema.addAttribute("xmlns:xs", XML_SCHEMA_URI);
		schema.addAttribute("elementFormDefault", "qualified");
		return schema;
	}

	static void addElement(XmlBuilder context, String elementName, String elementType) {
		XmlBuilder element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("name", elementName);
		element.addAttribute("type", elementType);
		context.addSubElement(element);
	}

	static void addElement(
			XmlBuilder context,
			String elementName,
			String elementType,
			String minOccurs,
			String maxOccurs) {
		XmlBuilder element = new XmlBuilder("element", "xs", XML_SCHEMA_URI);
		element.addAttribute("name", elementName);
		element.addAttribute("type", elementType);
		element.addAttribute("minOccurs", minOccurs);
		element.addAttribute("maxOccurs", maxOccurs);
		context.addSubElement(element);
	}

	static XmlBuilder addComplexType(XmlBuilder schema, String complexTypeName) {
		XmlBuilder complexType;
		complexType = new XmlBuilder("complexType", "xs", XML_SCHEMA_URI);
		complexType.addAttribute("name", complexTypeName);
		schema.addSubElement(complexType);
		return complexType;
	}

	static XmlBuilder addChoice(XmlBuilder context) {
		XmlBuilder choice = new XmlBuilder("choice", "xs", XML_SCHEMA_URI);
		context.addSubElement(choice);
		return choice;
	}

	static XmlBuilder addSequence(XmlBuilder context) {
		XmlBuilder sequence = new XmlBuilder("sequence", "xs", XML_SCHEMA_URI);
		context.addSubElement(sequence);
		return sequence;
	}

	static XmlBuilder addAttribute(XmlBuilder context, String name, String defaultValue) {
		XmlBuilder attribute = new XmlBuilder("attribute", "xs", XML_SCHEMA_URI);
		attribute.addAttribute("name", name);
		attribute.addAttribute("type", "xs:string");
		if(defaultValue != null) {
			attribute.addAttribute("default", defaultValue);
		}
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

	static XmlBuilder addGroupRef(XmlBuilder context, String id) {
		XmlBuilder group = new XmlBuilder("group", "xs", XML_SCHEMA_URI);
		context.addSubElement(group);
		group.addAttribute("ref", id);
		return group;
	}
}
