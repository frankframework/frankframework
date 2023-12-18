package org.frankframework.align;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.xml.transform.TransformerException;
import javax.xml.validation.ValidatorHandler;

import org.apache.xerces.xs.XSModel;
import org.xml.sax.SAXException;

import org.frankframework.testutil.MatchUtils;
import org.frankframework.util.TransformerPool;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.XmlWriter;

/**
 * @author Gerrit van Brakel
 */
public class TestAlignNamespacesXml extends AlignTestBase {

	@Override
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception {

		URL schemaUrl=getSchemaURL(schemaFile);
		String xmlString=getTestFile(inputFile+".xml");

		String xmlNoNamespace = removeNamespacesExceptAttributes(xmlString);

		ValidatorHandler validatorHandler = XmlAligner.getValidatorHandler(schemaUrl);
		List<XSModel> schemaInformation = XmlAligner.getSchemaInformation(schemaUrl);

		XmlAligner aligner = new XmlAligner(validatorHandler, schemaInformation);

		NamespaceAligningFilter namespaceAligningFilter = new NamespaceAligningFilter(aligner, validatorHandler);

		XmlWriter writer = new XmlWriter();
		aligner.setContentHandler(writer);

		XmlUtils.parseXml(xmlNoNamespace, namespaceAligningFilter);

		MatchUtils.assertXmlEquals("", xmlString, writer.toString(), true);
	}

	private String removeNamespacesExceptAttributes(String xmlString) throws TransformerException, IOException, SAXException {

		String template = "<xsl:template match=\"*\">"
				+ "<xsl:element name=\"{local-name()}\">"
				+ "<xsl:for-each select=\"@*\">"
				+ "<xsl:copy/>"
				+ "</xsl:for-each>"
				+ "<xsl:apply-templates/>"
				+ "</xsl:element>"
				+ "</xsl:template>"
				+ "<xsl:template match=\"comment() | processing-instruction() | text()\">"
				+ "<xsl:copy>"
				+ "<xsl:apply-templates/>"
				+ "</xsl:copy>"
			+ "</xsl:template>";

		String stylesheet = "<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"2.0\">"
		+ "<xsl:output method=\"xml\" />"
		+ template
		+ "</xsl:stylesheet>";

		TransformerPool tp = TransformerPool.getInstance(stylesheet, 2);
		tp.open();
		String result = tp.transform(xmlString, null);
		tp.close();
		return result;
	}
}
