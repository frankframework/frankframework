package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.align.AlignTestBase;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;

public class FileHandlerTest {

	public static String BASEDIR="/FileHandler/";

	private FileHandler handler;
	private IPipeLineSession session=new PipeLineSessionBase();

	public String CHARSET_UTF8="UTF-8";
	public String charset=CHARSET_UTF8;

	
	@Before
	public void setup() throws ConfigurationException {
		handler = new FileHandler();
	}
	
	public URL getURL(String file) {
		return FileHandlerTest.class.getResource(BASEDIR+file);
	}
 
	protected String getTestFile(String file, String charset) throws IOException, TimeoutException {
		URL url=AlignTestBase.class.getResource(BASEDIR+file);
		if (url==null) {
			return null;
		}
		BufferedReader buf = new BufferedReader(new InputStreamReader(url.openStream(),charset));
		StringBuilder string = new StringBuilder();
		while (true) {
			int c=buf.read();
			if (c<0) {
				break;
			}
			string.append((char)c);
		}
		return string.toString();
	}

	public void testRead(String filename, String charset) throws Exception {
		URL fileURL=getURL(filename);
		String filepath=fileURL.getPath();
		
		handler.setActions("read");
		handler.setCharset(charset);
		handler.setFileName(filepath);
		handler.configure();
		
		String expectedContents=getTestFile(filename, charset);
		String actualContents = (String) handler.handle(null,session,null);
		assertEquals("file contents", expectedContents, actualContents);
	}

	@Test
	public void testReadXml() throws Exception {
		testRead("smiley.xml",charset);
	}

	@Test
	public void testReadJson() throws Exception {
		testRead("smiley.json",charset);
	}

	@Test
	public void testReadTxt() throws Exception {
		testRead("smiley.txt",charset);
	}
}
