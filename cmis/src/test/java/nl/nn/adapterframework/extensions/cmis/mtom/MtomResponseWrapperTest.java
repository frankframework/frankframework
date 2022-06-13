package nl.nn.adapterframework.extensions.cmis.mtom;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.mock.web.MockHttpServletResponse;

import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

@RunWith(Parameterized.class)
public class MtomResponseWrapperTest {
	private static final String BASEPATH = "/proxy/";

	private URL response_in;
	private URL response_out;

	@Parameterized.Parameters(name = "{index} - {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
			{"getRepositoriesSOAP"},
			{"getRepositoriesMTOM"}
		});
	}

	public MtomResponseWrapperTest(String testFileName) throws Throwable {
		String response_in_path = BASEPATH+testFileName+"_response_in.txt";
		response_in = TestFileUtils.getTestFileURL(response_in_path);
		assertNotNull("request input file ["+response_in_path+"] not found", response_in);

		String response_out_path = BASEPATH+testFileName+"_response_out.txt";
		response_out = TestFileUtils.getTestFileURL(response_out_path);
		assertNotNull("request output file ["+response_out_path+"] not found", response_out);
	}

	private String getBoundary(String contentType) {
		int partIndex = contentType.indexOf("=_Part_");
		if(partIndex > 0) {
			String boundary = contentType.substring(partIndex+7);
			boundary = boundary.substring(0, boundary.indexOf(";")-1);
			return boundary.trim();
		} else {
			return null;
		}
	}

	@Test
	public void testMtomResponse() throws Throwable {
		MockHttpServletResponse response = new MockHttpServletResponse();
		String inputFile = TestFileUtils.getTestFile(response_in, "UTF-8");
		String[] filePart = inputFile.split("\\R", 2);
		String type = filePart[0].substring("content-type: ".length());
		String content = filePart[1];
		response.setContentType(type);

		MtomResponseWrapper wrapper = new MtomResponseWrapper(response);

		try (OutputStream outputStream = wrapper.getOutputStream()) {
			outputStream.write(content.getBytes());
		}

		String contentType = wrapper.getContentType();
		String result = response.getContentAsString();

		String boundary = getBoundary(contentType);
		if(boundary == null) { //assume there are no parts
			assertTrue("contentType does not contain [text/xml]", contentType.contains("text/xml"));
		} else {
			assertTrue("contentType is not a multipart", contentType.contains("type=\"application/xop+xml\";"));
			result = result.replace(boundary, "IGNORE"); //Replace the multipart boundary with IGNORE
		}

		String expected = TestFileUtils.getTestFile(response_out, "UTF-8");
		assertNotNull("cannot find expected file", expected);
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}
}
