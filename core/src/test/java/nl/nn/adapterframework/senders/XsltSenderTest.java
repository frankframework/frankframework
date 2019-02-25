package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

import org.junit.Test;

public class XsltSenderTest extends SenderTestBase<XsltSender> {

	@Override
	public XsltSender createSender() {
		return new XsltSender();
	}

	@Test
	public void basic() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		sender.configure();
		sender.open();
		String input=getFile("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=getFile("/Xslt/duplicateImport/out.xml");

		ParameterResolutionContext prc = new ParameterResolutionContext(input, session);
		String result = sender.sendMessage(null, input, prc);

		assertEquals(expected.replaceAll("\\s",""), result.replaceAll("\\s",""));
	}
}
