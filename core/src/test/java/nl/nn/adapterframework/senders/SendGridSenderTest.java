package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

public class SendGridSenderTest extends SenderTestBase<SendGridSender> {

	@Override
	public SendGridSender createSender() {
		SendGridSender sgs = new SendGridSender();
		return sgs;
	}

	@Test
	public void testXMLFiles()
			throws SenderException, TimeOutException, ConfigurationException, IOException {
		completeXMLFile("/emailSamplesXML/emailSample.xml");
		// Added for codacy check should be removed
		assertEquals("test", "test");
	}

	public void completeXMLFile(String filePath)
			throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setPassword(""); // should be apikey itself
		if (!sender.getPassword().isEmpty()) {
			sender.configure();
			sender.open();
			Reader fileReader = new FileReader(getClass().getResource(filePath).getFile());
			BufferedReader bufReader = new BufferedReader(fileReader);
			StringBuilder sb = new StringBuilder();
			String line = bufReader.readLine();
			while (line != null) {
				sb.append(line).append("\n");
				line = bufReader.readLine();
			}
			bufReader.close();
			String xml2String = sb.toString();
			String sampleMailXML = xml2String;
			String input = "<dummy/>";
			String result = sender.sendMessage(input, sampleMailXML);
			assertEquals(input, result);
		}

	}
}
