package nl.nn.adapterframework.frankdoc;

import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addComplexType;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.addElementWithType;
import static nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.getXmlSchema;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.frankdoc.model.AttributeType;
import nl.nn.adapterframework.util.XmlBuilder;

public class AttributeTypeStrategyTest {
	private String schemaString = getXsd();

	@Test
	public void whenNormalIntegerSuppliedThenAccepted() throws Exception {
		validate(getIntTestXml("1"));
	}

	@Test
	public void whenIntegerWithPlusSuppliedThenAccepted() throws Exception {
		validate(getIntTestXml("+1"));
	}

	@Test
	public void whenNegativeIntegerSuppliedThenAccepted() throws Exception {
		validate(getIntTestXml("-12"));
	}

	@Test
	public void whenRefSuppliedAsIntegerThenAccepted() throws Exception {
		validate(getIntTestXml("${myVariable}"));
	}

	@Test(expected = SAXException.class)
	public void whenNonIntegerSuppliedThenRejected() throws Exception {
		validate(getIntTestXml("aString"));
	}

	@Test(expected = SAXException.class)
	public void whenInvalidRefSuppliedAsIntegerThenRejected() throws Exception {
		validate(getIntTestXml("${myVariable"));
	}

	@Test
	public void whenTrueSuppliedAsBooleanThenAccepted() throws Exception {
		validate(getBoolTestXml("true"));
	}

	@Test
	public void whenFalseSuppliedAsBooleanThenAccepted() throws Exception {
		validate(getBoolTestXml("false"));
	}

	@Test
	public void whenRefSuppliedAsBooleanThenAccepted() throws Exception {
		validate(getBoolTestXml("${myVariable}"));
	}

	@Test(expected = SAXException.class)
	public void whenStringSuppliedAsBooleanThenRejected() throws Exception {
		validate(getBoolTestXml("aString"));
	}

	@Test(expected = SAXException.class)
	public void whenInvalidRefSuppliedAsBooleanThenRejected() throws Exception {
		validate(getBoolTestXml("${myVariable"));
	}

	@Test
	public void whenAttributeActiveIsLowerCaseTrueThenAccepted() throws Exception {
		validate(getTestXmlActive("true"));
	}

	@Test
	public void whenAttributeActiveIsLowerCaseFalseThenAccepted() throws Exception {
		validate(getTestXmlActive("false"));		
	}

	@Test
	public void whenAttributeActiveIsMixedCaseTrueThenAccepted() throws Exception {
		validate(getTestXmlActive("True"));
	}

	@Test
	public void whenAttributeActiveIsMixedCaseFalseThenAccepted() throws Exception {
		validate(getTestXmlActive("False"));		
	}

	@Test
	public void whenAttributeActiveIsVarRefThenAccepted() throws Exception {
		validate(getTestXmlActive("${myVar}"));
	}

	@Test
	public void whenAttributeActiveIsNegatedLiteralThenAccepted() throws Exception {
		validate(getTestXmlActive("!true"));
	}

	@Test
	public void whenAttributeActiveIsNegatedVarRefThenAccepted() throws Exception {
		validate(getTestXmlActive("!${myVar}"));
	}

	@Test(expected = SAXException.class)
	public void whenAttributeActiveIsSimpleTextThenRejected() throws Exception {
		validate(getTestXmlActive("xxx"));
	}

	@Test(expected = SAXException.class)
	public void whenAttributeActiveConcatsMultipleValidElementsThenCombinationRejected() throws Exception {
		validate(getTestXmlActive("${myVar}true"));
	}

	private static String getXsd() {
		XmlBuilder schema = getXmlSchema();
		XmlBuilder element = addElementWithType(schema, "myElement");
		XmlBuilder complexType = addComplexType(element);
		AttributeTypeStrategy.ALLOW_PROPERTY_REF.addAttribute(complexType, "boolAttr", AttributeType.BOOL);
		AttributeTypeStrategy.ALLOW_PROPERTY_REF.addAttribute(complexType, "intAttr", AttributeType.INT);
		AttributeTypeStrategy.ALLOW_PROPERTY_REF.addAttributeActive(complexType);
		AttributeTypeStrategy.ALLOW_PROPERTY_REF.createHelperTypes().forEach(h -> schema.addSubElement(h));
		return schema.toXML(true);
	}

	private static String getBoolTestXml(String value) {
		return String.format("<myElement boolAttr=\"%s\"/>", value);
	}

	private static String getIntTestXml(String value) {
		return String.format("<myElement intAttr=\"%s\"/>", value);
	}

	private static String getTestXmlActive(String value) {
		return String.format("<myElement active=\"%s\"/>", value);
	}

	private void validate(String testXml) throws Exception {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(new SAXSource(new InputSource(new StringReader(schemaString))));
		Validator validator = schema.newValidator();
		SAXSource source = new SAXSource(new InputSource(new StringReader(testXml)));
		validator.validate(source);
	}
}
