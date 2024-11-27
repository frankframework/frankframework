package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.util.ClassLoaderUtils;

class StreamPipeTest extends PipeTestBase<StreamPipe> {


	@Override
	public StreamPipe createPipe() {
		return new StreamPipe();
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		session = new PipeLineSession();
	}

	@Test
	void doPipeHttpRequestTest() throws Exception {
		MockMultipartHttpServletRequest request = createMultipartHttpRequest();
		pipe.addParameter(createHttpRequestParameter(request, session));
		pipe.configure();
		pipe.start();
		PipeRunResult pipeRunResult = doPipe(pipe, "", session);
		assertEquals("success", pipeRunResult.getPipeForward().getName());
		String expectedResult = """
				<parts>\
				<part type="string" name="string1" sessionKey="part_string" size="19"/>\
				<part type="file" name="doc001.pdf" sessionKey="part_file" size="26358" mimeType="application/octet-stream"/>\
				<part type="file" name="doc002.pdf" sessionKey="part_file2" size="25879" mimeType="application/octet-stream"/>\
				</parts>\
				""";
		assertEquals(expectedResult, pipeRunResult.getResult().asString());
	}

	@Test
	void doPipeHttpRequestAntiVirusTest() throws Exception {
		MockMultipartHttpServletRequest request = createMultipartHttpRequest(pipe, true);
		pipe.addParameter(createHttpRequestParameter(request, session));
		pipe.configure();
		pipe.start();
		PipeRunResult pipeRunResult = doPipe(pipe, "", session);
		assertEquals("success", pipeRunResult.getPipeForward().getName());
		String expectedResult = """
				<parts>\
				<part type="string" name="string1" sessionKey="part_string" size="19"/>\
				<part type="file" name="doc001.pdf" sessionKey="part_file" size="26358" mimeType="application/octet-stream"/>\
				<part type="string" name="antivirus_rc" sessionKey="part_string2" size="4"/>\
				<part type="file" name="doc002.pdf" sessionKey="part_file2" size="25879" mimeType="application/octet-stream"/>\
				<part type="string" name="antivirus_rc" sessionKey="part_string3" size="4"/>\
				</parts>\
				""";
		assertEquals(expectedResult, pipeRunResult.getResult().asString());
	}

	@Test
	void doPipeHttpRequestCheckAntiVirusPassedTest() throws Exception {
		pipe.setCheckAntiVirus(true);
		MockMultipartHttpServletRequest request = createMultipartHttpRequest(pipe, true);
		pipe.addParameter(createHttpRequestParameter(request, session));
		pipe.configure();
		pipe.start();
		PipeRunResult pipeRunResult = doPipe(pipe, "", session);
		assertEquals("success", pipeRunResult.getPipeForward().getName());
		String expectedResult = """
				<parts>\
				<part type="string" name="string1" sessionKey="part_string" size="19"/>\
				<part type="file" name="doc001.pdf" sessionKey="part_file" size="26358" mimeType="application/octet-stream"/>\
				<part type="file" name="doc002.pdf" sessionKey="part_file2" size="25879" mimeType="application/octet-stream"/>\
				</parts>\
				""";
		assertEquals(expectedResult, pipeRunResult.getResult().asString());
	}

	@Test
	void doPipeHttpRequestCheckAntiVirusFailedTest() throws Exception {
		pipe.setCheckAntiVirus(true);
		PipeForward pipeAntiVirusFailedForward = new PipeForward();
		pipeAntiVirusFailedForward.setName(StreamPipe.ANTIVIRUS_FAILED_FORWARD);
		pipe.addForward(pipeAntiVirusFailedForward);
		MockMultipartHttpServletRequest request = createMultipartHttpRequest(pipe, true, true);
		pipe.addParameter(createHttpRequestParameter(request, session));
		pipe.configure();
		pipe.start();
		PipeRunResult pipeRunResult = doPipe(pipe, "", session);
		assertEquals("antiVirusFailed", pipeRunResult.getPipeForward().getName());
		String expectedResult = "multipart contains file [doc002.pdf] with antivirus status [Fail] and message []";
		assertEquals(expectedResult, pipeRunResult.getResult().asString());
	}

	private MockMultipartHttpServletRequest createMultipartHttpRequest() throws Exception {
		return createMultipartHttpRequest(null, false);
	}

	private MockMultipartHttpServletRequest createMultipartHttpRequest(StreamPipe pipe, boolean addAntiVirusParts) throws Exception {
		return createMultipartHttpRequest(pipe, addAntiVirusParts, false);
	}

	private Parameter createHttpRequestParameter(MockMultipartHttpServletRequest request, PipeLineSession session) {
		session.put("httpRequest", request);
		return ParameterBuilder.create().withName("httpRequest").withSessionKey("httpRequest");
	}

	private MockMultipartHttpServletRequest createMultipartHttpRequest(StreamPipe pipe, boolean addAntiVirusParts, boolean antiVirusLastPartFailed) throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setBoundary("gc0p4Jq0M2Yt08jU534c0p");
		builder.addTextBody("string1", "<hello>test</hello>");

		URL url = ClassLoaderUtils.getResourceURL("/Documents/doc001.pdf");
		File file = new File(url.toURI());
		builder.addBinaryBody("file1", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());

		if (addAntiVirusParts) {
			builder.addTextBody(pipe.getAntiVirusPartName(), pipe.getAntiVirusPassedMessage());
		}

		URL url2 = ClassLoaderUtils.getResourceURL("/Documents/doc002.pdf");
		File file2 = new File(url2.toURI());
		builder.addBinaryBody("file2", file2, ContentType.APPLICATION_OCTET_STREAM, file2.getName());

		if (addAntiVirusParts) {
			String antiVirusLastPartMessage;
			if (antiVirusLastPartFailed) {
				antiVirusLastPartMessage = "Fail";
				if (antiVirusLastPartMessage.equalsIgnoreCase(pipe.getAntiVirusPassedMessage())) {
					throw new Exception("fail message ["+antiVirusLastPartMessage+"] must differ from pass message ["+pipe.getAntiVirusPassedMessage()+"]");
				}
			} else {
				antiVirusLastPartMessage = pipe.getAntiVirusPassedMessage();
			}
			builder.addTextBody(pipe.getAntiVirusPartName(), antiVirusLastPartMessage);
		}

		ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
		HttpEntity entity = builder.build();
		entity.writeTo(requestContent);
		request.setContent(requestContent.toByteArray());
		request.setContentType(entity.getContentType().getValue());
		return request;
	}
}
