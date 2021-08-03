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
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addPattern;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addSimpleType;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addUnion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.frankdoc.model.AttributeType;
import nl.nn.adapterframework.frankdoc.model.AttributeEnum;
import nl.nn.adapterframework.frankdoc.model.FrankAttribute;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

public enum AttributeTypeStrategy {
	ALLOW_PROPERTY_REF(new DelegateAllowPropertyRef()),
	VALUES_ONLY(new DelegateValuesOnly());

	private static Logger log = LogUtil.getLogger(AttributeTypeStrategy.class);

	private static final String ATTRIBUTE_ACTIVE_NAME = "active";

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

	void addAttributeActive(XmlBuilder context) {
		delegate.addAttributeActive(context);
	}

	List<XmlBuilder> createHelperTypes() {
		return delegate.createHelperTypes();
	}

	private static abstract class Delegate {
		abstract XmlBuilder addAttribute(XmlBuilder context, String name, AttributeType modelAttributeType);
		abstract XmlBuilder addRestrictedAttribute(XmlBuilder context, FrankAttribute attribute);
		abstract void addAttributeActive(XmlBuilder context);
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
			AttributeEnum attributeEnum = attribute.getAttributeEnum();
			XmlBuilder attributeBuilder = addAttributeWithType(context, attribute.getName());
			XmlBuilder simpleType = addSimpleType(attributeBuilder);
			return addUnion(simpleType, attributeEnum.getUniqueName(ATTRIBUTE_VALUES_TYPE), VARIABLE_REFERENCE);
		}

		@Override
		void addAttributeActive(XmlBuilder context) {
			DocWriterNewXmlUtils.addAttributeRef(context, ATTRIBUTE_ACTIVE_NAME);
		}

		@Override
		List<XmlBuilder> createHelperTypes() {
			log.trace("Adding helper types for boolean and integer attributes, allowing ${...} references");
			List<XmlBuilder> result = new ArrayList<>();
			result.add(createTypeFrankBoolean());
			result.add(createTypeFrankInteger());
			result.add(createAttributeForAttributeActive());
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
			addPattern(restriction, pattern);
			return simpleType;
		}

		private XmlBuilder createAttributeForAttributeActive() {
			XmlBuilder attribute = new XmlBuilder("attribute", "xs", XML_SCHEMA_URI);
			attribute.addAttribute("name", ATTRIBUTE_ACTIVE_NAME);
			DocWriterNewXmlUtils.addDocumentation(attribute, "If defined and empty or false, then this element and all its children are ignored");
			XmlBuilder simpleType = DocWriterNewXmlUtils.addSimpleType(attribute);
			XmlBuilder restriction = DocWriterNewXmlUtils.addRestriction(simpleType, "xs:string");
			DocWriterNewXmlUtils.addPattern(restriction, getPattern());
			return attribute;
		}

		private String getPattern() {
			return "\\!?" + "(" + getPatternThatMightBeNegated() + ")";
		}

		private String getPatternThatMightBeNegated() {
			String patternTrue = getCaseInsensitivePattern(Boolean.valueOf(true).toString());
			String patternFalse = getCaseInsensitivePattern(Boolean.valueOf(false).toString());
			return Arrays.asList(PATTERN_REF, patternTrue, patternFalse).stream()
					.map(s -> "(" + s + ")")
					.collect(Collectors.joining("|"));
		}

		private String getCaseInsensitivePattern(final String word) {
			return IntStream.range(0, word.length()).mapToObj(i -> Character.valueOf(word.charAt(i)))
				.map(c -> "[" + Character.toLowerCase(c) + Character.toUpperCase(c) + "]")
				.collect(Collectors.joining(""));
		}
	}

	private static class DelegateValuesOnly extends Delegate {
		@Override
		XmlBuilder addAttribute(XmlBuilder context, String name, AttributeType modelAttributeType) {
			return addAttribute(context, name, modelAttributeType, "xs:boolean", "xs:integer");
		}

		@Override
		XmlBuilder addRestrictedAttribute(XmlBuilder context, FrankAttribute attribute) {
			return DocWriterNewXmlUtils.addAttribute(context, attribute.getName(), attribute.getAttributeEnum().getUniqueName(ATTRIBUTE_VALUES_TYPE));
		}

		@Override
		void addAttributeActive(XmlBuilder context) {
		}

		@Override
		List<XmlBuilder> createHelperTypes() {
			return new ArrayList<>();
		}
	}
}
