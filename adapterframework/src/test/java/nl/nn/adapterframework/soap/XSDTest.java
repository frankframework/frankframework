package nl.nn.adapterframework.soap;

/**
 * TODO It seems that XSD class was refactored
 * @author Michiel Meeuwissen
 */
public class XSDTest {

	/*@Test
	public void xsdName() throws URISyntaxException, XMLStreamException, IOException {
		XSD xsd = new XSD("", "http://test", ClassUtils.getResourceURL("v1 test.xsd").toURI(), 0);
		assertEquals("v1 test.xsd", xsd.getName());
	}


	@Test
	public void xsdNamespace() throws URISyntaxException, XMLStreamException, IOException {
		XSD xsd = new XSD("", "http://test", ClassUtils.getResourceURL("v1 test.xsd").toURI(), 0);
		// todo assertEquals("v1 test.xsd", xsd.getRootTag());
	}

	@Test
	public void baseUrlXsd() throws URISyntaxException {
		XSD xsd = new XSD("", "http://test",
				new URI("file:/E:/nn/workspace/wsad/Ibis4EMSWEB/WebContent/WEB-INF/classes/AuthorizeUsers/xsd/AuthorizeUsersReply.xsd"), 0);
		assertEquals("AuthorizeUsers/xsd/", xsd.getBaseUrl());
	}


	@Test
	public void baseUrlXsdWebsphere() throws URISyntaxException, XMLStreamException, IOException {
		XSD xsd = new XSD("", "http://test",
				new URI("file:/data/WAS/6.1/wasap02/appl/Ibis4WUB-010_20111221-1815_wasap02.ear/Ibis4WUB.war/WEB-INF/classes/CalculateQuoteAndPolicyValuesLifeRetail/xsd/Calculation.xsd"), 0);
		assertEquals("CalculateQuoteAndPolicyValuesLifeRetail/xsd/", xsd.getBaseUrl());
	}


	@Test
	public void writeXSD() throws XMLStreamException, IOException, ParserConfigurationException, SAXException, URISyntaxException {
		XSD xsd = new XSD("", "http://test", ClassUtils.getResourceURL("XSDTest/test.xsd").toURI(), 0);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLStreamWriter writer = WsdlUtils.createWriter(out, false);
		xsd.write(writer, new HashMap<String, String>(), true, false);

		DocumentBuilder dbuilder = WsdlTest.createDocumentBuilder();
		Document result = dbuilder.parse(new ByteArrayInputStream(out.toByteArray()));
		Document expected = dbuilder.parse(getClass().getClassLoader().getResourceAsStream("XSDTest/test_expected.xsd"));
		System.out.println(new String(out.toByteArray()));
		XMLUnit.setIgnoreWhitespace(false);

		assertXMLEqual("test xml not similar to control xml", expected, result);


	}

	@Test
	public void writeXSDStripSchemaLocation() throws XMLStreamException, IOException, ParserConfigurationException, SAXException, URISyntaxException {
		XSD xsd = new XSD("", "http://test", ClassUtils.getResourceURL("XSDTest/test.xsd").toURI(), 0);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		XMLStreamWriter writer = WsdlUtils.createWriter(out, false);
		xsd.write(writer, new HashMap<String, String>(), true, true);

		DocumentBuilder dbuilder = WsdlTest.createDocumentBuilder();
		Document result = dbuilder.parse(new ByteArrayInputStream(out.toByteArray()));
		Document expected = dbuilder.parse(getClass().getClassLoader().getResourceAsStream("XSDTest/test_expected_removed_imported_namespaces.xsd"));
		System.out.println(new String(out.toByteArray()));
		XMLUnit.setIgnoreWhitespace(false);

		assertXMLEqual("test xml not similar to control xml", expected, result);


	}

	@Test
	public void importedXsds() throws URISyntaxException, XMLStreamException, IOException {
		XSD xsd = new XSD("", "http://test", ClassUtils.getResourceURL("XSDTest/test.xsd").toURI(), 0);
		assertEquals(2, xsd.getImportXSDs(null).size());
		System.out.println("" + xsd.getImportXSDs(null));
	}
*/
}
