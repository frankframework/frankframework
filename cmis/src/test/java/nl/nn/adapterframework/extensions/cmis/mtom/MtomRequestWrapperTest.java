package nl.nn.adapterframework.extensions.cmis.mtom;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.mock.web.MockHttpServletRequest;

import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.StreamUtil;

@RunWith(Parameterized.class)
public class MtomRequestWrapperTest {
	private static final String BASEPATH = "/proxy/";

	private URL request_in;
	private URL request_out;

	@Parameterized.Parameters(name = "{index} - {0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
			{"getRepositoriesSOAP"},
			{"getRepositoriesMTOM"}
		});
	}

	public MtomRequestWrapperTest(String testFileName) throws Throwable {
		String request_in_path = BASEPATH+testFileName+"_request_in.txt";
		request_in = TestFileUtils.getTestFileURL(request_in_path);
		assertNotNull("request input file ["+request_in_path+"] not found", request_in);

		String request_out_path = BASEPATH+testFileName+"_request_out.txt";
		request_out = TestFileUtils.getTestFileURL(request_out_path);
		assertNotNull("request output file ["+request_out_path+"] not found", request_out);
	}

	private String getBoundary(String contentType) {
		String boundary = contentType.substring(contentType.indexOf("=_Part_")+7);
		boundary = boundary.substring(0, boundary.indexOf(";")-1);
		return boundary.trim();
	}

	@Test
	public void testMtomRequest() throws Throwable {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cmis/webservices/RepositoryService");
		String inputFile = TestFileUtils.getTestFile(request_in, "UTF-8");
		String[] filePart = inputFile.split("\\R", 2);
		String type = filePart[0].substring("content-type: ".length());
		String content = filePart[1];
		request.setContentType(type);
		request.setContent(content.getBytes());

		MtomRequestWrapper wrapper = new MtomRequestWrapper(request);

		String contentType = wrapper.getContentType();
		assertTrue(contentType.contains("type=\"application/xop+xml\";"));
		String result = StreamUtil.streamToString(wrapper.getInputStream());

		String boundary = getBoundary(contentType);
		assertNotNull("no boundary found", boundary);
		result = result.replace(boundary, "IGNORE"); //Replace the multipart boundary with IGNORE

		String expected = TestFileUtils.getTestFile(request_out, "UTF-8");
		assertNotNull("cannot find expected file", expected);
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}
}
