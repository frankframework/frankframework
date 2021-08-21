package nl.nn.adapterframework.frankdoc;

import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addComplexType;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addElementWithType;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.getXmlSchema;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.frankdoc.doclet.FrankClassRepository;
import nl.nn.adapterframework.frankdoc.doclet.TestUtil;
import nl.nn.adapterframework.frankdoc.model.AttributeEnum;
import nl.nn.adapterframework.frankdoc.model.AttributeType;
import nl.nn.adapterframework.frankdoc.model.ElementChild;
import nl.nn.adapterframework.frankdoc.model.FrankAttribute;
import nl.nn.adapterframework.frankdoc.model.FrankDocModel;
import nl.nn.adapterframework.util.XmlBuilder;

@RunWith(Parameterized.class)
public class AttributeTypeStrategyTest {
	private String schemaStringAllowAttributeRef;
	private String schemaStringAllowAttributeRefEnumValuesIgnoreCase;

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"NormalInteger", getIntTestXml("1"), true, true},
			{"IntegerWithPlus", getIntTestXml("+1"), true, true},
			{"NegativeInteger", getIntTestXml("-12"), true, true},
			{"RefAsInteger", getIntTestXml("${myVariable}"), true, true},
			{"NonIntegerForInteger", getIntTestXml("aString"), false, false},
			{"InvalidRefForInteger", getIntTestXml("${myVariable"), false, false},
			{"True", getBoolTestXml("true"), true, true},
			{"False", getBoolTestXml("false"), true, true},
			{"RefAsBoolean", getBoolTestXml("${myVariable}"), true, true},
			{"StringForBoolean", getBoolTestXml("aString"), false, false},
			{"InvalidRefForBoolean", getBoolTestXml("${myVariable"), false, false},
			{"AttributeActiveTrueLowerCase", getTestXmlActive("true"), true, true},
			{"AttributeActiveFalseLowerCase", getTestXmlActive("false"), true, true},
			{"AttributeActiveMixedCaseTrue", getTestXmlActive("True"), true, true},
			{"AttributeActiveMixedCaseFalse", getTestXmlActive("False"), true, true},
			{"RefAsAttributeActive", getTestXmlActive("${myVar}"), true, true},
			{"AttributeActiveNotTrue", getTestXmlActive("!true"), true, true},
			{"NegatedRefAsAttributeActive", getTestXmlActive("!${myVar}"), true, true},
			{"StringForAttributeActive", getTestXmlActive("xxx"), false, false},
			{"AttributeActiveConcatsMultipleValidValues", getTestXmlActive("${myVar}true"), false, false},
			{"RestrictedAttribute", getEnumTestXml("TWO"), true, true},
			{"RestrictedAttributeMixedCase1", getEnumTestXml("Two"), false, true},
			{"RestrictedAttributeMixedCase2", getEnumTestXml("twO"), false, true},
			{"InvalidValueRestrictedAttribute", getEnumTestXml("xxx"), false, false}
		});
	}

	private static String getBoolTestXml(String value) {
		return String.format("<myElement boolAttr=\"%s\"/>", value);
	}

	private static String getIntTestXml(String value) {
		return String.format("<myElement intAttr=\"%s\"/>", value);
	}

	private static String getEnumTestXml(String value) {
		return String.format("<myElement restrictedAttribute=\"%s\"/>", value);
	}

	private static String getTestXmlActive(String value) {
		return String.format("<myElement active=\"%s\"/>", value);
	}

	@Parameter(0)
	public String title;

	@Parameter(1)
	public String testXml;

	@Parameter(2)
	public boolean allowPropertyRefShouldAccept;

	@Parameter(3)
	public boolean allowPropertyRefEnumValuesIgnoreCaseShouldAccept;

	@Before
	public void setUp() {
		String packageOfEnum = "nl.nn.adapterframework.frankdoc.testtarget.attribute.type.strategy.";
		FrankClassRepository classRepository = TestUtil.getFrankClassRepositoryDoclet(packageOfEnum);
		String digesterRulesFileName = "doc/empty-digester-rules.xml";
		FrankDocModel model = FrankDocModel.populate(digesterRulesFileName, packageOfEnum + "Container", classRepository);
		FrankAttribute attribute = model.findFrankElement(packageOfEnum + "Container").getAttributes(ElementChild.ALL).get(0);
		AttributeEnum attributeEnum = model.findAttributeEnum(packageOfEnum + "Container.TestType");
		schemaStringAllowAttributeRef = getXsd(AttributeTypeStrategy.ALLOW_PROPERTY_REF, attributeEnum, attribute);
		schemaStringAllowAttributeRefEnumValuesIgnoreCase = getXsd(AttributeTypeStrategy.ALLOW_PROPERTY_REF_ENUM_VALUES_IGNORE_CASE, attributeEnum, attribute);
	}

	private static String getXsd(AttributeTypeStrategy attributeTypeStrategy, AttributeEnum attributeEnum, FrankAttribute enumTypedAttribute) {
		XmlBuilder schema = getXmlSchema();
		XmlBuilder element = addElementWithType(schema, "myElement");
		XmlBuilder complexType = addComplexType(element);
		attributeTypeStrategy.addAttribute(complexType, "boolAttr", AttributeType.BOOL);
		attributeTypeStrategy.addAttribute(complexType, "intAttr", AttributeType.INT);
		attributeTypeStrategy.addAttributeActive(complexType);
		attributeTypeStrategy.addRestrictedAttribute(complexType, enumTypedAttribute);
		attributeTypeStrategy.createHelperTypes().forEach(h -> schema.addSubElement(h));
		schema.addSubElement(attributeTypeStrategy.createAttributeEnumType(attributeEnum));
		return schema.toXML(true);
	}

	@Test
	public void testAllowPropertyRef() {
		doTest(allowPropertyRefShouldAccept, schemaStringAllowAttributeRef);
	}

	@Test
	public void testAllowPropertyRefEnumValuesIgnoreCase() {
		doTest(allowPropertyRefEnumValuesIgnoreCaseShouldAccept, schemaStringAllowAttributeRefEnumValuesIgnoreCase);
	}

	private void doTest(boolean expectedValue, String theXsd) {
		boolean actualAccepted = true;
		try {
			validate(testXml, theXsd);
		} catch(SAXException e) {
			actualAccepted = false;
		} catch(IOException e) {
			fail(String.format("Got IOException: %s - %s", e.getMessage(), e.getStackTrace()));
		}
		assertEquals(expectedValue, actualAccepted);
	}

	private void validate(String testXml, String theXsd) throws SAXException, IOException {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(new SAXSource(new InputSource(new StringReader(theXsd))));
		Validator validator = schema.newValidator();
		SAXSource source = new SAXSource(new InputSource(new StringReader(testXml)));
		validator.validate(source);
	}
}
