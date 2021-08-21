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
	private String schemaString;

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
			{"NormalInteger", getIntTestXml("1"), true},
			{"IntegerWithPlus", getIntTestXml("+1"), true},
			{"NegativeInteger", getIntTestXml("-12"), true},
			{"RefAsInteger", getIntTestXml("${myVariable}"), true},
			{"NonIntegerForInteger", getIntTestXml("aString"), false},
			{"InvalidRefForInteger", getIntTestXml("${myVariable"), false},
			{"True", getBoolTestXml("true"), true},
			{"False", getBoolTestXml("false"), true},
			{"RefAsBoolean", getBoolTestXml("${myVariable}"), true},
			{"StringForBoolean", getBoolTestXml("aString"), false},
			{"InvalidRefForBoolean", getBoolTestXml("${myVariable"), false},
			{"AttributeActiveTrueLowerCase", getTestXmlActive("true"), true},
			{"AttributeActiveFalseLowerCase", getTestXmlActive("false"), true},
			{"AttributeActiveMixedCaseTrue", getTestXmlActive("True"), true},
			{"AttributeActiveMixedCaseFalse", getTestXmlActive("False"), true},
			{"RefAsAttributeActive", getTestXmlActive("${myVar}"), true},
			{"AttributeActiveNotTrue", getTestXmlActive("!true"), true},
			{"NegatedRefAsAttributeActive", getTestXmlActive("!${myVar}"), true},
			{"StringForAttributeActive", getTestXmlActive("xxx"), false},
			{"AttributeActiveConcatsMultipleValidValues", getTestXmlActive("${myVar}true"), false},
			{"RestrictedAttribute", getEnumTestXml("TWO"), true},
			{"InvalidValueRestrictedAttribute", getEnumTestXml("xxx"), false}
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

	@Before
	public void setUp() {
		String packageOfEnum = "nl.nn.adapterframework.frankdoc.testtarget.attribute.type.strategy.";
		FrankClassRepository classRepository = TestUtil.getFrankClassRepositoryDoclet(packageOfEnum);
		String digesterRulesFileName = "doc/empty-digester-rules.xml";
		FrankDocModel model = FrankDocModel.populate(digesterRulesFileName, packageOfEnum + "Container", classRepository);
		FrankAttribute attribute = model.findFrankElement(packageOfEnum + "Container").getAttributes(ElementChild.ALL).get(0);
		AttributeEnum attributeEnum = model.findAttributeEnum(packageOfEnum + "Container.TestType");
		schemaString = getXsd(attributeEnum, attribute);
		System.out.println(schemaString);
	}

	private static String getXsd(AttributeEnum attributeEnum, FrankAttribute enumTypedAttribute) {
		XmlBuilder schema = getXmlSchema();
		XmlBuilder element = addElementWithType(schema, "myElement");
		XmlBuilder complexType = addComplexType(element);
		AttributeTypeStrategy.ALLOW_PROPERTY_REF.addAttribute(complexType, "boolAttr", AttributeType.BOOL);
		AttributeTypeStrategy.ALLOW_PROPERTY_REF.addAttribute(complexType, "intAttr", AttributeType.INT);
		AttributeTypeStrategy.ALLOW_PROPERTY_REF.addAttributeActive(complexType);
		AttributeTypeStrategy.ALLOW_PROPERTY_REF.addRestrictedAttribute(complexType, enumTypedAttribute);
		AttributeTypeStrategy.ALLOW_PROPERTY_REF.createHelperTypes().forEach(h -> schema.addSubElement(h));
		schema.addSubElement(AttributeTypeStrategy.ALLOW_PROPERTY_REF.createAttributeEnumType(attributeEnum));
		return schema.toXML(true);
	}

	@Test
	public void testAllowPropertyRef() {
		boolean actualAccepted = true;
		try {
			validate(testXml);
		} catch(SAXException e) {
			actualAccepted = false;
		} catch(IOException e) {
			fail(String.format("Got IOException: %s - %s", e.getMessage(), e.getStackTrace()));
		}
		assertEquals(allowPropertyRefShouldAccept, actualAccepted);
	}

	private void validate(String testXml) throws SAXException, IOException {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(new SAXSource(new InputSource(new StringReader(schemaString))));
		Validator validator = schema.newValidator();
		SAXSource source = new SAXSource(new InputSource(new StringReader(testXml)));
		validator.validate(source);
	}
}
