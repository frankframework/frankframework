package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import org.apache.commons.codec.Charsets;
import org.junit.Test;

public class StreamUtilTest {

	private String UTF8_EXPECTED="ABC één euro: €1,00";
	private String OTHER_EXPECTED="ABC néé hè";
	
	public void testReader(String inputFile, String expected) throws IOException {
		testReader(inputFile, expected, null);
	}

	public void testReader(String inputFile, String expected, String defaultCharset) throws IOException {
		URL input = ClassUtils.getResourceURL(this, inputFile);
		
		System.out.println(inputFile+":");
		int i;
		InputStream inputStream = input.openStream();
		while( (i=inputStream.read())>=0) {
			System.out.print(Integer.toHexString(i)+" ");
		}
		System.out.println();
		
		Reader reader;
		if (defaultCharset==null) {
	 		reader = StreamUtil.getCharsetDetectingInputStreamReader(input.openStream());
		} else {
	 		reader = StreamUtil.getCharsetDetectingInputStreamReader(input.openStream(), defaultCharset);
		}
		
		String actual = StreamUtil.readerToString(reader, null);
		
		assertEquals(expected, actual);
		reader.close();
	}
	
	@Test
	public void testInputStreamReaderPlainUTF8() throws IOException {
		testReader("/StreamUtil/inUTF8noBOM.bin", UTF8_EXPECTED);
	}
	
	@Test
	public void testInputStreamReaderUTF8withBOM() throws IOException {
		testReader("/StreamUtil/inUTF8withBOM.bin", UTF8_EXPECTED);
	}
	
	@Test
	public void testInputStreamReaderUTF16LEwithBOM() throws IOException {
		testReader("/StreamUtil/inUTF16LEwithBOM.bin", UTF8_EXPECTED);
	}
	
	@Test
	public void testInputStreamReaderUTF16BEwithBOM() throws IOException {
		testReader("/StreamUtil/inUTF16BEwithBOM.bin", UTF8_EXPECTED);
	}

	@Test
	public void testInputStreamReaderAnsi() throws IOException {
		testReader("/StreamUtil/inISO8859-1.bin", OTHER_EXPECTED, "ISO8859-1");
	}

	@Test
	public void testInputStreamReaderUTF8withBOMWrongDefaultCharset() throws IOException {
		testReader("/StreamUtil/inUTF8withBOM.bin", UTF8_EXPECTED, "ISO8859-1");
	}
	

	public void testStreamToByteArray(String inputFile, boolean skipBOM, String expected, boolean expectBOM) throws IOException {
		URL input = ClassUtils.getResourceURL(this, inputFile);
		
		byte[] byteArray = StreamUtil.streamToByteArray(input.openStream(), skipBOM);
		
		String actual;
		if (expectBOM) {
			assertEquals((byte)0xEF,byteArray[0]);
			assertEquals((byte)0xBB,byteArray[1]);
			assertEquals((byte)0xBF,byteArray[2]);
			actual = new String(byteArray,3, byteArray.length-3, Charsets.UTF_8);
		} else {
			actual = new String(byteArray,Charsets.UTF_8);
		}
		
		assertEquals(expected, actual);
	}
	
	@Test
	public void testStreamToByteArrayUTF8noBOM() throws IOException {
		testStreamToByteArray("/StreamUtil/inUTF8noBOM.bin", true, UTF8_EXPECTED, false);
		testStreamToByteArray("/StreamUtil/inUTF8noBOM.bin", false, UTF8_EXPECTED, false);
	}

	@Test
	public void testStreamToByteArrayUTF8withBOM() throws IOException {
		testStreamToByteArray("/StreamUtil/inUTF8withBOM.bin", true, UTF8_EXPECTED, false);
		testStreamToByteArray("/StreamUtil/inUTF8withBOM.bin", false, UTF8_EXPECTED, true);
	}

}
