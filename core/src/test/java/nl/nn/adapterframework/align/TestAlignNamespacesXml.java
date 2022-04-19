package nl.nn.adapterframework.align;

import java.net.URL;
import java.util.List;

import javax.xml.validation.ValidatorHandler;

import org.apache.xerces.xs.XSModel;

import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * @author Gerrit van Brakel
 */
public class TestAlignNamespacesXml extends AlignTestBase {

	@Override
	public void testFiles(String schemaFile, String namespace, String rootElement, String inputFile, boolean potentialCompactionProblems, String expectedFailureReason) throws Exception {
		
		URL schemaUrl=getSchemaURL(schemaFile);
		String xmlString=getTestFile(inputFile+".xml");
		
		String xmlNoNamespace = XmlUtils.removeNamespaces(xmlString);
		
		ValidatorHandler validatorHandler = XmlAligner.getValidatorHandler(schemaUrl);
		List<XSModel> schemaInformation = XmlAligner.getSchemaInformation(schemaUrl);
		
		XmlAligner aligner = new XmlAligner(validatorHandler, schemaInformation);
		
		NamespaceAligningFilter namespaceAligningFilter = new NamespaceAligningFilter(aligner, validatorHandler);

		XmlWriter writer = new XmlWriter();
		aligner.setContentHandler(writer);
		
		XmlUtils.parseXml(xmlNoNamespace, namespaceAligningFilter);

		MatchUtils.assertXmlEquals("", xmlString, writer.toString(), true);
	}

}
