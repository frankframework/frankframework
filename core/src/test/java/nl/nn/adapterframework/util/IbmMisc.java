package nl.nn.adapterframework.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import nl.nn.adapterframework.testutil.TestFileUtils;

/**
 * Mock class to Misc methods
 */
public class IbmMisc {

	public static String getApplicationDeploymentDescriptorPath() {
		return getFileLocation("META-INF"); //pretend this is the META-INF folder of an EAR file.
	}

	/**
	 * This file is used in conjunction with the {@link #getConnectionPoolProperties(String, String, String)} method.
	 * Since we stub the {@link #getConnectionPoolProperties(String, String, String)} result, there is no need to return anything
	 */
	public static String getConfigurationResourcePath() {
		return getFileLocation("resources.xml");
	}

	public static String getConfigurationServerPath() {
		return getFileLocation("server.xml");
	}

	private static String getFileLocation(String filename) {
		try {
			URL url = TestFileUtils.getTestFileURL("/Util/IbmMisc/"+filename);
			assertNotNull(url);
			return new File(url.toURI()).getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
			return null;
		}
	}

	/**
	 * Should be tested in the IBM module, we just mock the result for convenience.
	 */
	public static String getConnectionPoolProperties(String confResString, String providerType, String jndiName) throws IOException, TransformerException, SAXException {
		return String.format("mockPoolProperties type [%s] jndi [%s]", providerType, jndiName);
	}
}
