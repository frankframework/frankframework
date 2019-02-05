package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;

import org.junit.Test;
import org.mockito.Mock;
/**
 * MailSender can be replaced with MailSenderNew to test MailSenderNew
 * 
 * @author alisihab
 *
 */
public class MailSenderTest extends SenderTestBase<MailSender> {

	@Mock
	private Properties properties = new Properties();
	private String smtpHost = "localhost"; // could be smtp.sendgrid.net

	@Override
	public MailSender createSender() {
		MailSender ms = new MailSender();
		ms.setProperties(properties);
		return ms;
	}

	@Test
	public void basic() throws SenderException, TimeOutException, ConfigurationException,
			IOException {
		sender.setSmtpHost(smtpHost);
		sender.setSmtpUserid("");
		sender.setSmtpPassword(""); // should be apikey itself if smtp.sendgrid.net is the hostname
		if (!sender.getSmtpPassword().isEmpty()) {
			sender.configure();
			Reader fileReader = new FileReader(getClass().getResource(
					"/emailSamplesXML/emailSample.xml").getFile());
			BufferedReader bufReader = new BufferedReader(fileReader);
			StringBuilder sb = new StringBuilder();
			String line = bufReader.readLine();
			while (line != null) {
				sb.append(line).append("\n");
				line = bufReader.readLine();
			}
			String xml2String = sb.toString();
			String sampleMailXML = xml2String;
			String input = "<dummy/>";
			String result = sender.sendMessage(input, sampleMailXML);
			assertEquals(input, result);
		}
	}
}
