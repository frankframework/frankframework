/* 
Copyright 2021 WeAreFrank! 

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

import static nl.nn.adapterframework.frankdoc.DocWriterNew.ATTRIBUTE_VALUES_TYPE;
import static nl.nn.adapterframework.frankdoc.DocWriterNew.VARIABLE_REFERENCE;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.XML_SCHEMA_URI;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addAttributeWithType;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addSimpleType;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addUnion;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.frankdoc.model.AttributeType;
import nl.nn.adapterframework.frankdoc.model.AttributeValues;
import nl.nn.adapterframework.frankdoc.model.FrankAttribute;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

public enum AttributeTypeStrategy {
	ALLOW_PROPERTY_REF(new DelegateAllowPropertyRef()),
	VALUES_ONLY(new DelegateValuesOnly());

	private static Logger log = LogUtil.getLogger(AttributeTypeStrategy.class);

	// The $-sign is not escaped in the regex below. This way,
	// the regexes in the XSDs are not flagged by XMLSpy.
	private static final String PATTERN_REF = "$\\{[^\\}]+\\}";

	private static final String FRANK_BOOLEAN = "frankBoolean";
	private static final String FRANK_INT = "frankInt";
	private static final String PATTERN_FRANK_BOOLEAN = String.format("(true|false)|(%s)", PATTERN_REF);
	private static final String PATTERN_FRANK_INT = String.format("((\\+|-)?[0-9]+)|(%s)", PATTERN_REF);

	private final Delegate delegate;

	private AttributeTypeStrategy(final Delegate delegate) {
		this.delegate = delegate;
	}

	XmlBuilder addAttribute(XmlBuilder context, String name, AttributeType modelAttributeType) {
		return delegate.addAttribute(context, name, modelAttributeType);
	}

	XmlBuilder addRestrictedAttribute(XmlBuilder context, FrankAttribute attribute) {
		return delegate.addRestrictedAttribute(context, attribute);
	}

	List<XmlBuilder> createHelperTypes() {
		return delegate.createHelperTypes();
	}

	private static abstract class Delegate {
		abstract XmlBuilder addAttribute(XmlBuilder context, String name, AttributeType modelAttributeType);
		abstract XmlBuilder addRestrictedAttribute(XmlBuilder context, FrankAttribute attribute);
		abstract List<XmlBuilder> createHelperTypes();

		final XmlBuilder addAttribute(XmlBuilder context, String name, AttributeType modelAttributeType, String boolType, String intType) {
			XmlBuilder attribute = new XmlBuilder("attribute", "xs", XML_SCHEMA_URI);
			attribute.addAttribute("name", name);
			String typeName = null;
			switch(modelAttributeType) {
			case BOOL:
				typeName = boolType;
				break;
			case INT:
				typeName = intType;
				break;
			case STRING:
				typeName = "xs:string";
				break;
			}
			attribute.addAttribute("type", typeName);
			context.addSubElement(attribute);
			return attribute;						
		}
	}

	private static class DelegateAllowPropertyRef extends Delegate {
		// This method ensures that references are still allowed for integer and boolean attributes.
		// For example, an integer attribute can still be set like "${someIdentifier}".
		// This method expects that methods DocWriterNewXmlUtils.createTypeFrankBoolean() and
		// DocWriterNewXmlUtils.createTypeFrankInteger() are used to define the referenced XSD types.
		@Override
		XmlBuilder addAttribute(XmlBuilder context, String name, AttributeType modelAttributeType) {
			return addAttribute(context, name, modelAttributeType, FRANK_BOOLEAN, FRANK_INT);
		}

		@Override
		XmlBuilder addRestrictedAttribute(XmlBuilder context, FrankAttribute attribute) {
			AttributeValues attributeValues = attribute.getAttributeValues();
			XmlBuilder attributeBuilder = addAttributeWithType(context, attribute.getName());
			XmlBuilder simpleType = addSimpleType(attributeBuilder);
			return addUnion(simpleType, attributeValues.getUniqueName(ATTRIBUTE_VALUES_TYPE), VARIABLE_REFERENCE);
		}

		@Override
		List<XmlBuilder> createHelperTypes() {
			log.trace("Adding helper types for boolean and integer attributes, allowing ${...} references");
			List<XmlBuilder> result = new ArrayList<>();
			result.add(createTypeFrankBoolean());
			result.add(createTypeFrankInteger());
			// Helper type for allowing a variable reference instead of an enum value
			result.add(createTypeVariableReference(VARIABLE_REFERENCE));
			return result;
		}

		private static XmlBuilder createTypeFrankBoolean() {
			return createStringRestriction(FRANK_BOOLEAN, PATTERN_FRANK_BOOLEAN);
		}

		private static XmlBuilder createTypeFrankInteger() {
			return createStringRestriction(FRANK_INT, PATTERN_FRANK_INT);
		}

		private static XmlBuilder createTypeVariableReference(String name) {
			return createStringRestriction(name, PATTERN_REF);
		}

		private static XmlBuilder createStringRestriction(String name, String pattern) {
			XmlBuilder simpleType = new XmlBuilder("simpleType", "xs", XML_SCHEMA_URI);
			simpleType.addAttribute("name", name);
			XmlBuilder restriction = new XmlBuilder("restriction", "xs", XML_SCHEMA_URI);
			simpleType.addSubElement(restriction);
			restriction.addAttribute("base", "xs:string");
			XmlBuilder patternElement = new XmlBuilder("pattern", "xs", XML_SCHEMA_URI);
			restriction.addSubElement(patternElement);
			patternElement.addAttribute("value", pattern);
			return simpleType;
		}
	}

	private static class DelegateValuesOnly extends Delegate {
		@Override
		XmlBuilder addAttribute(XmlBuilder context, String name, AttributeType modelAttributeType) {
			return addAttribute(context, name, modelAttributeType, "xs:boolean", "xs:integer");
		}

		@Override
		XmlBuilder addRestrictedAttribute(XmlBuilder context, FrankAttribute attribute) {
			return DocWriterNewXmlUtils.addAttribute(context, attribute.getName(), attribute.getAttributeValues().getUniqueName(ATTRIBUTE_VALUES_TYPE));
		}

		@Override
		List<XmlBuilder> createHelperTypes() {
			return new ArrayList<>();
		}
	}
}
