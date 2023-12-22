package org.frankframework.extensions.cmis.mtom;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.URL;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletResponse;

import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;

public class MtomResponseWrapperTest {
	private static final String BASEPATH = "/proxy/";

	private String getInputFile(String testFileName) throws Throwable {
		String request_in_path = BASEPATH+testFileName+"_response_in.txt";
		URL request_in = TestFileUtils.getTestFileURL(request_in_path);
		assertNotNull(request_in, "request input file ["+request_in_path+"] not found");
		return TestFileUtils.getTestFile(request_in, "UTF-8");
	}

	private String getOutputFile(String testFileName) throws Throwable {
		String request_out_path = BASEPATH+testFileName+"_response_out.txt";
		URL request_out = TestFileUtils.getTestFileURL(request_out_path);
		assertNotNull(request_out, "request output file ["+request_out_path+"] not found");
		return TestFileUtils.getTestFile(request_out, "UTF-8");
	}

	private static String getBoundary(String contentType) {
		int partIndex = contentType.indexOf("=_Part_");
		if(partIndex > 0) {
			String boundary = contentType.substring(partIndex+7);
			boundary = boundary.substring(0, boundary.indexOf(";")-1);
			return boundary.trim();
		} else {
			return null;
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {"getRepositoriesSOAP", "getRepositoriesMTOM"})
	public void testMtomResponse(String testFileName) throws Throwable {
		MockHttpServletResponse response = new MockHttpServletResponse();
		String[] filePart = getInputFile(testFileName).split("\\R", 2);
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
			assertTrue(contentType.contains("text/xml"), "contentType does not contain [text/xml]");
		} else {
			assertTrue(contentType.contains("type=\"application/xop+xml\";"), "contentType is not a multipart");
			result = result.replace(boundary, "IGNORE"); //Replace the multipart boundary with IGNORE
		}

		TestAssertions.assertEqualsIgnoreCRLF(getOutputFile(testFileName), result);
	}
}
