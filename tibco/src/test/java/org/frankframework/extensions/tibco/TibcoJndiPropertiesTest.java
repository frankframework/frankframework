package org.frankframework.extensions.tibco;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;

import com.tibco.tibjms.TibjmsSSL;

public class TibcoJndiPropertiesTest {
	private ClassLoader classLoader = this.getClass().getClassLoader();
	private static final String JDNI_PROPERTIES = "jndi.properties";

	@Test
	public void testPropertyLoaderAsURL() throws IOException {
		URL url = classLoader.getResource(JDNI_PROPERTIES);
		assertNotNull(url, "cannot find properties file");
		TibcoEmsProperties properties = new TibcoEmsProperties(url);

		assertInstanceOf(String.class, properties.get(TibjmsSSL.VENDOR));
		assertInstanceOf(Boolean.class, properties.get(TibjmsSSL.TRACE));
		assertInstanceOf(Boolean.class, properties.get("com.tibco.tibjms.naming.ssl_debug_trace"));
		assertInstanceOf(String.class, properties.get(TibjmsSSL.TRUSTED_CERTIFICATES));
		assertInstanceOf(Boolean.class, properties.get("com.tibco.tibjms.naming.ssl_enable_verify_host"));
		assertInstanceOf(Integer.class, properties.get("com.tibco.tibjms.reconnect.attemptcount"));
	}

	@Test
	public void testPropertyLoaderasString() throws IOException {
		TibcoEmsProperties properties = new TibcoEmsProperties(null, JDNI_PROPERTIES);

		assertInstanceOf(String.class, properties.get(TibjmsSSL.VENDOR));
		assertInstanceOf(Boolean.class, properties.get(TibjmsSSL.TRACE));
		assertInstanceOf(Boolean.class, properties.get("com.tibco.tibjms.naming.ssl_debug_trace"));
		assertInstanceOf(String.class, properties.get(TibjmsSSL.TRUSTED_CERTIFICATES));
		assertInstanceOf(Boolean.class, properties.get("com.tibco.tibjms.naming.ssl_enable_verify_host"));
		assertInstanceOf(Integer.class, properties.get("com.tibco.tibjms.reconnect.attemptcount"));
	}
}
