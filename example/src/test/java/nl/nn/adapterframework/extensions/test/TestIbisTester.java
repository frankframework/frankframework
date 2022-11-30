package nl.nn.adapterframework.extensions.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.configuration.IbisContext;

public class TestIbisTester {

	@Test
	public void runIbisTester() {
		IbisTester ibisTester = new IbisTester();
		System.setProperty("junit.active", "true");
		long start = System.currentTimeMillis();

		ibisTester.initTest();
		String testResult = ibisTester.testStartAdapters();
		assertNull(testResult, testResult);

		IbisContext ibisContext = ibisTester.getIbisContext();
		assertNotNull(ibisContext);

		//Test generic startup time of an ibis with 3 adapters, should take less then 30 seconds.
		long startupTime = (System.currentTimeMillis() - start);
		assertTrue(startupTime < 30000, "Application took ["+startupTime+"] to start up!");
	}
}
