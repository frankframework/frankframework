package nl.nn.adapterframework.extensions.idin;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import net.bankid.merchant.library.Communicator;
import net.bankid.merchant.library.Configuration;
import net.bankid.merchant.library.DirectoryResponse;
import net.bankid.merchant.library.internal.DirectoryResponseBase.Issuer;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassLoaderUtils;

/**
 * Initially I thought, hey lets add some unittests...
 * Lets just skip them for now shall we? :) 
 * 
 * PS: Requires java 1.7 to compile/run!
 *
 */
public class IdinSenderTest extends Mockito {

	IdinSender sender = null;

	private List<Issuer> getIssuers() {
		return getIssuers("NL");
	}
	private List<Issuer> getIssuers(String country) {
		List<Issuer> issuers = new ArrayList<Issuer>();
		for(int i = 0; i < 10; i++) {
			Issuer issuer = mock(Issuer.class);
			issuer.setIssuerCountry(country);
			issuer.setIssuerID("id_"+i);
			issuer.setIssuerName("name_"+i);
			issuers.add(issuer);
		}
		return issuers;
	}
	private Map<String, List<Issuer>> getIssuersByCountry() {
		Map<String, List<Issuer>> countryMap = new HashMap<String, List<Issuer>>();
		List<String> countries = Arrays.asList("NL", "BE", "GB", "DE", "ES");
		for(String country : countries) {
			countryMap.put(country, getIssuers(country));
		}
		return countryMap;
	}
	private XMLGregorianCalendar getDateTimestamp() {
		GregorianCalendar c = new GregorianCalendar();
		c.setTime(new Date(0)); //EPOCH
		try {
			return DatatypeFactory.newInstance().newXMLGregorianCalendar(c);
		} catch (DatatypeConfigurationException e) {
			return null;
		}
	}

	@Before
	public void initializeIdinSender() throws ParserConfigurationException, SAXException, IOException {
		URL expectedUrl = ClassLoaderUtils.getResourceURL("bankid-config.xml");
		Configuration.defaultInstance().Load(expectedUrl.openStream());
		Communicator communicator = mock(Communicator.class);
		DirectoryResponse response = mock(DirectoryResponse.class);

		when(response.getIsError()).thenReturn(false);
//		when(response.getIssuers()).thenReturn(new ArrayList<Issuer>());
//		when(response.getIssuersByCountry()).thenReturn(new HashMap<String, List<Issuer>>());
		when(response.getDirectoryDateTimestamp()).thenReturn(getDateTimestamp());
		when(communicator.getDirectory()).thenReturn(response);

		sender = spy(new IdinSender());
		when(sender.getCommunicator()).thenReturn(communicator);

		sender.setAction("DIRECTORY");
	}

	@Ignore
	@Test
	public void randomMessage() throws SenderException, TimeoutException, IOException {
		String message = "<test><woop>1</woop></test>";
		PipeLineSession session = null;
		String result = sender.sendMessageOrThrow(new Message(message), session).asString();
		//TODO compare
	}

	@Ignore
	@Test
	public void normal() throws SenderException, TimeoutException, IOException {
		String message = "<idin/>";
		PipeLineSession session = null;
		String result = sender.sendMessageOrThrow(new Message(message), session).asString();
		//TODO assertEquals("result", result);
	}

	@Ignore
	@Test
	public void issuersByCountry() throws SenderException, TimeoutException, IOException {
		String message = "<idin><issuersByCountry>true</issuersByCountry></idin>";
		PipeLineSession session = null;
		String result = sender.sendMessageOrThrow(new Message(message), session).asString();
		//TODO assertEquals("result", result);
	}
}