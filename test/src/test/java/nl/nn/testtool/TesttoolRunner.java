package nl.nn.testtool;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.extensions.test.IbisTester;
import nl.nn.adapterframework.util.DomBuilderException;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

public class TesttoolRunner {
	private static IbisTester ibisTester;
	private static IbisContext ibisContext;
	private IPipeLineSession session;

	@Before
	public void initXMLUnit() throws IOException {
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
		session = new PipeLineSessionBase();
		session.put("method", "GET");
	}

	@BeforeClass
	public static void initTest() throws ConfigurationException, TransformerConfigurationException, IOException {
		ibisTester = new IbisTester();
//		System.setProperty("configurations.names", "${instance.name}");

		ibisTester.initTest();
		assertNull(ibisTester.testStartAdapters());
		ibisContext = ibisTester.getIbisContext();

		assertEquals(true, ibisContext != null);
	}

	@Ignore
	@Test
	public void scenarioRunner () throws ConfigurationException, PipeRunException, DomBuilderException, SAXException, IOException, TransformerException {
		System.out.println(ibisContext.getApplicationName());
		ibisTester.testLarva();
	}

	@AfterClass
	public static void closeTest() {
		ibisTester.closeTest();
	}
}
