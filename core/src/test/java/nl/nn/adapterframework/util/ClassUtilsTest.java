package nl.nn.adapterframework.util;

import static org.junit.Assert.*;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Niels Meijer
 */
public class ClassUtilsTest {

	private String fileName = "Configuration.xml";
	private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	private String fileContent = "<test />";

	@Test
	public void getResourceURL() throws URISyntaxException, IOException {
		URL baseUrl = classLoader.getResource(fileName);

		assertEquals(baseUrl.getFile(), ClassUtils.getResourceURL(this, fileName).getFile());
	}

	@Test
	public void getResourceURLAndValidateContentsO() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(this, fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLAndValidateContentsC1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLAndValidateContentsC2() throws URISyntaxException, IOException {
		ClassLoader classLoader = this.getClass().getClassLoader();
		URL url = ClassUtils.getResourceURL(classLoader, fileName);
		assertEquals(fileContent, Misc.streamToString(url.openStream()).trim());
	}

	@Test
	public void getResourceURLfromExternalFile() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(this, fileName);
		String fullPath = url.toURI().toString();

		URL url2 = ClassUtils.getResourceURL(classLoader, fullPath, "file");
		assertEquals(url.getFile(), url2.getFile());
	}

	@Test
	public void getResourceURLfromExternalFileError() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(this, fileName);
		String fullPath = url.toURI().toString();

		assertNull(ClassUtils.getResourceURL(classLoader, fullPath, ""));
	}

	@Test
	public void getResourceURLnoFileError() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "apple.pie.with.raisins");

		assertNull(url);
	}

	@Test
	public void getResourceURLnoExternalFileErrorC1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "apple.pie.with.raisins", "file");

		assertNull(url);
	}

	@Test
	public void getResourceURLnoExternalFileErrorC2() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "file://potato.ext", "file");

		assertEquals("", url.getFile()); //returns an empty string if one does not exist
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://potato.ext", "");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed2() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://potato.ext", "file");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed3() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://potato.ext", "file,thing");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed4() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://localhost/potato.ext", "file,thing");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolNotAllowed5() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://localhost/potato.ext", "https");

		assertNull(url);
	}

	@Test
	public void getResourceURLfromHttpProtocolAllowed1() throws URISyntaxException, IOException {
		URL url = ClassUtils.getResourceURL(classLoader, "http://localhost/potato.ext", "http");

		assertNotNull(url);
	}
}
