package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.UrlMessage;
import nl.nn.adapterframework.testutil.TestFileUtils;

@RunWith(Parameterized.class)
public class MessageUtilsTest {

	@Parameterized.Parameter(0)
	public String testFile;

	@Parameterized.Parameter(1)
	public String fileContent;

	@Parameterized.Parameter(2)
	public Charset expectedCharset;

	@Parameters(name= "file [{0}] encoding [{2}]")
	public static List<Object[]> data() {
		return Arrays.asList(new Object[][]{
			{"cp1252.txt", "cp1252 test text ’•†™", Charset.forName("windows-1252")},
			{"iso-8859-1.txt", null, Charset.forName("iso-8859-1")},
			{"iso-8859-2.txt", "Naiveté de style, de langage, de pinceau.", Charset.forName("iso-8859-2")},
			{"utf8-with-bom.txt", "testFile with BOM —•˜›", Charset.forName("utf-8")},
			{"utf8-without-bom.txt", "testFile w/o BOM —•˜›", Charset.forName("utf-8")},
		});
	}

	@Test
	public void testCharset() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/Util/MessageUtils/"+testFile);
		assertNotNull("cannot find test file ["+testFile+"]", url);

		Message message = new UrlMessage(url);
		Charset computedCharset = MessageUtils.computeDecodingCharset(message, 40);

		assertEquals("charset mismatch", expectedCharset, computedCharset);

		if(fileContent != null) {
			String asString = message.asString(computedCharset == null ? null : computedCharset.name());

			assertEquals("fileContent mismatch", fileContent, asString);
		}
	}

	@Test
	public void testDetection() throws Exception {
		assumeTrue(expectedCharset != Charset.forName("windows-1252"));

		URL url = TestFileUtils.getTestFileURL("/Util/MessageUtils/"+testFile);
		assertNotNull("cannot find test file ["+testFile+"]", url);

		Message message = new UrlMessage(url);
		String result = message.asString("auto"); //calls asReader();

		assertEquals("charset mismatch", expectedCharset.name(), message.getCharset());

		if(fileContent != null) {
			assertEquals("fileContent mismatch", fileContent, result);
		}
	}
}
