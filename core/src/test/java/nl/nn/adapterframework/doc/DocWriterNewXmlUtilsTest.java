package nl.nn.adapterframework.doc;

import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addAttribute;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addComplexType;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.addElementWithType;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.createTypeFrankBoolean;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.createTypeFrankInteger;
import static nl.nn.adapterframework.doc.DocWriterNewXmlUtils.getXmlSchema;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.doc.model.AttributeType;
import nl.nn.adapterframework.util.XmlBuilder;

public class DocWriterNewXmlUtilsTest {
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

	private static String getXsd() {
		XmlBuilder schema = getXmlSchema();
		XmlBuilder element = addElementWithType(schema, "myElement");
		XmlBuilder complexType = addComplexType(element);
		addAttribute(complexType, "boolAttr", AttributeType.BOOL);
		addAttribute(complexType, "intAttr", AttributeType.INT);
		XmlBuilder boolType = createTypeFrankBoolean();
		XmlBuilder intType = createTypeFrankInteger();
		schema.addSubElement(boolType);
		schema.addSubElement(intType);
		return schema.toXML(true);
	}

	private static String getBoolTestXml(String value) {
		return String.format("<myElement boolAttr=\"%s\"/>", value);
	}

	private static String getIntTestXml(String value) {
		return String.format("<myElement intAttr=\"%s\"/>", value);
	}

	private void validate(String testXml) throws Exception {
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		Schema schema = schemaFactory.newSchema(new SAXSource(new InputSource(new StringReader(schemaString))));
		Validator validator = schema.newValidator();
		SAXSource source = new SAXSource(new InputSource(new StringReader(testXml)));
		validator.validate(source);
	}
}
