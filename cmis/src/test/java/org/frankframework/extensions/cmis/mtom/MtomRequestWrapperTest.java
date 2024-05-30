package org.frankframework.extensions.cmis.mtom;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.StreamUtil;

public class MtomRequestWrapperTest {
	private static final String BASEPATH = "/proxy/";

	private String getInputFile(String testFileName) throws Throwable {
		String request_in_path = BASEPATH+testFileName+"_request_in.txt";
		URL request_in = TestFileUtils.getTestFileURL(request_in_path);
		assertNotNull(request_in, "request input file ["+request_in_path+"] not found");
		return TestFileUtils.getTestFile(request_in, "UTF-8");
	}

	private String getOutputFile(String testFileName) throws Throwable {
		String request_out_path = BASEPATH+testFileName+"_request_out.txt";
		URL request_out = TestFileUtils.getTestFileURL(request_out_path);
		assertNotNull(request_out, "request output file ["+request_out_path+"] not found");
		return TestFileUtils.getTestFile(request_out, "UTF-8");
	}

	private static String getBoundary(String contentType) {
		String boundary = contentType.substring(contentType.indexOf("=_Part_")+7);
		boundary = boundary.substring(0, boundary.indexOf(";")-1);
		return boundary.trim();
	}

	@ParameterizedTest
	@ValueSource(strings = {"getRepositoriesSOAP", "getRepositoriesMTOM"})
	public void testMtomRequest(String testFileName) throws Throwable {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cmis/webservices/RepositoryService");
		String[] filePart = getInputFile(testFileName).split("\\R", 2);
		String type = filePart[0].substring("content-type: ".length());
		String content = filePart[1];
		request.setContentType(type);
		request.setContent(content.getBytes());

		MtomRequestWrapper wrapper = new MtomRequestWrapper(request);

		String contentType = wrapper.getContentType();
		assertTrue(contentType.contains("type=\"application/xop+xml\";"));
		String result = StreamUtil.streamToString(wrapper.getInputStream());

		String boundary = getBoundary(contentType);
		assertNotNull(boundary, "no boundary found");
		result = result.replace(boundary, "IGNORE"); //Replace the multipart boundary with IGNORE

		TestAssertions.assertEqualsIgnoreCRLF(getOutputFile(testFileName), result);
	}
}
