package nl.nn.adapterframework.util;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Michiel Meeuwissen
 */
public class ClassUtilsTest {


    @Test
    @Ignore("Only runs  @michiel")
    // TODO may actual tests...
	public void classUtilsTest() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(this, "jar:file:///Users/michiel/ibis/workspace/rsa-8/Ibis4WUBWEB/WebContent/WEB-INF/lib/AdapterFramework 5.0-a14.jarx!/xml/xsd/soap/envelope.xsd");
		System.out.println(url);
		URI uri = url.toURI();
		InputStream inputStream = uri.toURL().openStream();
		System.out.println(uri);
		System.out.println(uri.toURL());
	}
}
