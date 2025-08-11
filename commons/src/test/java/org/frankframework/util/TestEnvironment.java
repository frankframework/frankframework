package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Properties;

import org.junit.jupiter.api.Test;

public class TestEnvironment {

	@Test
	public void getEnvVars() throws IOException {
		Properties properties = Environment.getEnvironmentVariables();
		assertNotNull(properties.getProperty("JAVA_HOME"));
	}

	@Test
	public void normalFilesystemPath() throws Exception {
		String path = "file:/content/iaf%20test.war/WEB-INF/lib/abc.jar/META-INF/maven/org.frankframework/abc/pom.properties";
		URLStreamHandler urlStreamHandler = new BytesURLStreamHandler("dummy-data".getBytes());
		URL url = new URL(null, path, urlStreamHandler);
		assertEquals("/content/iaf test.war/WEB-INF/lib/abc.jar/META-INF/maven/org.frankframework/abc/pom.properties", Environment.extractPath(url));
	}

	@Test
	public void jbossVirtualFilesystemPath() throws Exception {
		String path = "vfs:/content/iaf%20test.war/WEB-INF/lib/abc.jar/META-INF/maven/org.frankframework/abc/pom.properties";
		URLStreamHandler urlStreamHandler = new BytesURLStreamHandler("dummy-data".getBytes());
		URL url = new URL(null, path, urlStreamHandler);
		assertEquals("/content/iaf%20test.war/WEB-INF/lib/abc.jar/META-INF/maven/org.frankframework/abc/pom.properties", Environment.extractPath(url));
	}

	@Test
	public void jbossVfszipPath() throws Exception {
		String path = "vfs:jar:file:/C:/content/iaf%20test.war/WEB-INF/lib/abc.jar/META-INF/maven/org.frankframework/abc/pom.properties";
		URLStreamHandler urlStreamHandler = new BytesURLStreamHandler("dummy-data".getBytes());
		URL url = new URL(null, path, urlStreamHandler);
		assertEquals("/C:/content/iaf%20test.war/WEB-INF/lib/abc.jar/META-INF/maven/org.frankframework/abc/pom.properties", Environment.extractPath(url));
	}

	@Test
	public void windowsFilesystemPath() throws Exception {
		String path = "file:/C:/content/iaf%20test.war/WEB-INF/lib/abc.jar/META-INF/maven/org.frankframework/abc/pom.properties";
		URLStreamHandler urlStreamHandler = new BytesURLStreamHandler("dummy-data".getBytes());
		URL url = new URL(null, path, urlStreamHandler);
		assertEquals("/C:/content/iaf test.war/WEB-INF/lib/abc.jar/META-INF/maven/org.frankframework/abc/pom.properties", Environment.extractPath(url));
	}

	@Test
	public void jarFilePath() throws Exception {
		String path = "jar:file:/C:/content/iaf%20test.war/WEB-INF/lib/abc.jar!/META-INF/maven/org.frankframework/abc/pom.properties";
		URLStreamHandler urlStreamHandler = new BytesURLStreamHandler("dummy-data".getBytes());
		URL url = new URL(null, path, urlStreamHandler);
		assertEquals("/C:/content/iaf test.war/WEB-INF/lib/abc.jar!/META-INF/maven/org.frankframework/abc/pom.properties", Environment.extractPath(url));
	}

	private static class BytesURLStreamHandler extends URLStreamHandler {
		byte[] bytes;

		public BytesURLStreamHandler(byte[] bytes) {
			this.bytes = bytes;
		}

		@Override
		protected URLConnection openConnection(URL url) {
			return new BytesURLConnection(url, bytes);
		}
	}

	private static class BytesURLConnection extends URLConnection {
		byte[] bytes;

		protected BytesURLConnection(URL url, byte[] bytes) {
			super(url);
			this.bytes = bytes;
		}

		@Override
		public void connect() throws IOException {
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return new ByteArrayInputStream(bytes);
		}
	}
}
