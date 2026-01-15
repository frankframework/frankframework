package org.frankframework.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.frankframework.logging.FrankPropertyLookupProvider.UrlLocationComparator;

public class FrankPropertyLookupProviderTest {

	@Test
	public void simpleLookup() throws IOException {
		FrankPropertyLookupProvider provider = new FrankPropertyLookupProvider();
		assertNull(provider.lookup("i-do-not-exist"));
		assertNull(provider.lookup(null, "i-do-not-exist"));
		assertEquals("-1", provider.lookup("log.lengthLogRecords"));
		assertEquals("-1", provider.lookup(null, "log.lengthLogRecords"));
	}

	@Test
	public void hasLogLevelAndDirectory() throws IOException {
		FrankPropertyLookupProvider provider = new FrankPropertyLookupProvider();
		assertNotNull(provider.lookup("log.level"));
		assertNotNull(provider.lookup(null, "log.level"));
		assertNotNull(provider.lookup("log.dir"));
		assertNotNull(provider.lookup(null, "log.dir"));
	}

	@Test
	public void returnsNullWhenCannotFindFile() throws IOException {
		assertNull(FrankPropertyLookupProvider.findResource("does-not-exist.properties"));
	}

	@Test
	public void returnsURL() throws IOException {
		assertNotNull(FrankPropertyLookupProvider.findResource("DeploymentSpecifics.properties"));
	}

	@Test
	public void dockerClassPath() throws MalformedURLException, URISyntaxException {
		List<URL> urls = new ArrayList<>();
		urls.add(new URI("jar:file:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib/ibis-adapterframework-core-7.9.6.jar!/log4j4ibis.properties").toURL());
		urls.add(new URI("file:/opt/frank/resources/log4j4ibis.properties").toURL());

		urls.sort(new UrlLocationComparator());

		assertEquals("file:/opt/frank/resources/log4j4ibis.properties", urls.getFirst().toExternalForm());
	}

	@Test
	public void dockerClassPathReverse() throws MalformedURLException, URISyntaxException {
		List<URL> urls = new ArrayList<>();
		urls.add(new URI("file:/opt/frank/resources/log4j4ibis.properties").toURL());
		urls.add(new URI("jar:file:/usr/local/tomcat/webapps/ROOT/WEB-INF/lib/ibis-adapterframework-core-7.9.6.jar!/log4j4ibis.properties").toURL());

		urls.sort(new UrlLocationComparator());

		assertEquals("file:/opt/frank/resources/log4j4ibis.properties", urls.getFirst().toExternalForm());
	}

	@Test
	public void webArchive() throws MalformedURLException, URISyntaxException {
		List<URL> urls = new ArrayList<>();
		urls.add(new URI("file:/foo/wtpwebapps/iaf-test/WEB-INF/classes/log4j4ibis.properties").toURL());
		urls.add(new URI("jar:file:/foo/wtpwebapps/iaf-test/WEB-INF/lib/frankframework-core-9.3.0-SNAPSHOT.jar!/log4j4ibis.properties").toURL());

		urls.sort(new UrlLocationComparator());

		assertEquals("file:/foo/wtpwebapps/iaf-test/WEB-INF/classes/log4j4ibis.properties", urls.getFirst().toExternalForm());
	}

	@Test
	public void webArchiveReverse() throws MalformedURLException, URISyntaxException {
		List<URL> urls = new ArrayList<>();
		urls.add(new URI("jar:file:/foo/wtpwebapps/iaf-test/WEB-INF/lib/frankframework-core-9.3.0-SNAPSHOT.jar!/log4j4ibis.properties").toURL());
		urls.add(new URI("file:/foo/wtpwebapps/iaf-test/WEB-INF/classes/log4j4ibis.properties").toURL());

		urls.sort(new UrlLocationComparator());

		assertEquals("file:/foo/wtpwebapps/iaf-test/WEB-INF/classes/log4j4ibis.properties", urls.getFirst().toExternalForm());
	}

	@Test
	public void webArchiveWithClassPathResource() throws MalformedURLException, URISyntaxException {
		List<URL> urls = new ArrayList<>();
		urls.add(new URI("jar:file:/foo/wtpwebapps/iaf-test/WEB-INF/lib/frankframework-core-9.3.0-SNAPSHOT.jar!/log4j4ibis.properties").toURL());
		urls.add(new URI("file:/opt/frank/resources/log4j4ibis.properties").toURL());
		urls.add(new URI("file:/foo/wtpwebapps/iaf-test/WEB-INF/classes/log4j4ibis.properties").toURL());

		urls.sort(new UrlLocationComparator());

		assertEquals("file:/opt/frank/resources/log4j4ibis.properties", urls.getFirst().toExternalForm());
	}
}
