package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.junit.Test;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.util.ClassUtils;

public class StreamPipeTest {

	private IPipeLineSession session = new PipeLineSessionBase();

	@Test
	public void doPipeHttpRequestTest() throws Exception {
		StreamPipe streamPipe = new StreamPipe();
		streamPipe.registerForward(createPipeSuccessForward());
		MockMultipartHttpServletRequest request = createMultipartHttpRequest();
		streamPipe.addParameter(createHttpRequestParameter(request, session));
		streamPipe.configure();
		PipeRunResult pipeRunResult = streamPipe.doPipe("", session);
		assertEquals("success", pipeRunResult.getPipeForward().getName());
		String expectedResult = "<parts>"
				+ "<part type=\"string\" name=\"string1\" sessionKey=\"part_string\" size=\"19\"/>"
				+ "<part type=\"file\" name=\"doc001.pdf\" sessionKey=\"part_file\" size=\"26358\" mimeType=\"application/octet-stream; charset=ISO-8859-1\"/>"
				+ "<part type=\"file\" name=\"doc002.pdf\" sessionKey=\"part_file2\" size=\"25879\" mimeType=\"application/octet-stream; charset=ISO-8859-1\"/>"
				+ "</parts>";
		assertEquals(expectedResult, pipeRunResult.getResult());
	}

	@Test
	public void doPipeHttpRequestAntiVirusTest() throws Exception {
		StreamPipe streamPipe = new StreamPipe();
		streamPipe.registerForward(createPipeSuccessForward());
		MockMultipartHttpServletRequest request = createMultipartHttpRequest(
				streamPipe, true);
		streamPipe.addParameter(createHttpRequestParameter(request, session));
		streamPipe.configure();
		PipeRunResult pipeRunResult = streamPipe.doPipe("", session);
		assertEquals("success", pipeRunResult.getPipeForward().getName());
		String expectedResult = "<parts>"
				+ "<part type=\"string\" name=\"string1\" sessionKey=\"part_string\" size=\"19\"/>"
				+ "<part type=\"file\" name=\"doc001.pdf\" sessionKey=\"part_file\" size=\"26358\" mimeType=\"application/octet-stream; charset=ISO-8859-1\"/>"
				+ "<part type=\"string\" name=\"antivirus_rc\" sessionKey=\"part_string2\" size=\"4\"/>"
				+ "<part type=\"file\" name=\"doc002.pdf\" sessionKey=\"part_file2\" size=\"25879\" mimeType=\"application/octet-stream; charset=ISO-8859-1\"/>"
				+ "<part type=\"string\" name=\"antivirus_rc\" sessionKey=\"part_string3\" size=\"4\"/>"
				+ "</parts>";
		assertEquals(expectedResult, pipeRunResult.getResult());
	}

	@Test
	public void doPipeHttpRequestCheckAntiVirusPassedTest() throws Exception {
		StreamPipe streamPipe = new StreamPipe();
		streamPipe.setCheckAntiVirus(true);
		streamPipe.registerForward(createPipeSuccessForward());
		MockMultipartHttpServletRequest request = createMultipartHttpRequest(
				streamPipe, true);
		streamPipe.addParameter(createHttpRequestParameter(request, session));
		streamPipe.configure();
		PipeRunResult pipeRunResult = streamPipe.doPipe("", session);
		assertEquals("success", pipeRunResult.getPipeForward().getName());
		String expectedResult = "<parts>"
				+ "<part type=\"string\" name=\"string1\" sessionKey=\"part_string\" size=\"19\"/>"
				+ "<part type=\"file\" name=\"doc001.pdf\" sessionKey=\"part_file\" size=\"26358\" mimeType=\"application/octet-stream; charset=ISO-8859-1\"/>"
				+ "<part type=\"file\" name=\"doc002.pdf\" sessionKey=\"part_file2\" size=\"25879\" mimeType=\"application/octet-stream; charset=ISO-8859-1\"/>"
				+ "</parts>";
		assertEquals(expectedResult, pipeRunResult.getResult());
	}

	@Test
	public void doPipeHttpRequestCheckAntiVirusFailedTest() throws Exception {
		StreamPipe streamPipe = new StreamPipe();
		streamPipe.setCheckAntiVirus(true);
		PipeForward pipeAntiVirusFailedForward = new PipeForward();
		pipeAntiVirusFailedForward.setName(StreamPipe.ANTIVIRUS_FAILED_FORWARD);
		streamPipe.registerForward(pipeAntiVirusFailedForward);
		streamPipe.registerForward(createPipeSuccessForward());
		MockMultipartHttpServletRequest request = createMultipartHttpRequest(
				streamPipe, true, true);
		streamPipe.addParameter(createHttpRequestParameter(request, session));
		streamPipe.configure();
		PipeRunResult pipeRunResult = streamPipe.doPipe("", session);
		assertEquals("antiVirusFailed",
				pipeRunResult.getPipeForward().getName());
		String expectedResult = "multipart contains file [doc002.pdf] with antivirus status [Fail] and message []";
		assertEquals(expectedResult, pipeRunResult.getResult());
	}

	private MockMultipartHttpServletRequest createMultipartHttpRequest()
			throws Exception {
		return createMultipartHttpRequest(null, false);
	}

	private MockMultipartHttpServletRequest createMultipartHttpRequest(
			StreamPipe streamPipe, boolean addAntiVirusParts) throws Exception {
		return createMultipartHttpRequest(streamPipe, addAntiVirusParts, false);
	}

	private PipeForward createPipeSuccessForward() {
		PipeForward pipeForward = new PipeForward();
		pipeForward.setName("success");
		return pipeForward;
	}

	private Parameter createHttpRequestParameter(
			MockMultipartHttpServletRequest request,
			IPipeLineSession session) {
		session.put("httpRequest", request);
		Parameter parameter = new Parameter();
		parameter.setName("httpRequest");
		parameter.setSessionKey("httpRequest");
		return parameter;
	}

	private MockMultipartHttpServletRequest createMultipartHttpRequest(
			StreamPipe streamPipe, boolean addAntiVirusParts,
			boolean antiVirusLastPartFailed) throws Exception {
		MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
		request.setContentType(
				"multipart/mixed;boundary=gc0p4Jq0M2Yt08jU534c0p");
		List<Part> parts = new ArrayList<Part>();
		String string = "<hello>test</hello>";
		StringPart stringPart = new StringPart("string1", string);
		parts.add(stringPart);
		URL url = ClassUtils.getResourceURL(this, "/Documents/doc001.pdf");
		File file = new File(url.toURI());
		FilePart filePart = new FilePart("file1", file.getName(), file);
		parts.add(filePart);
		if (addAntiVirusParts) {
			StringPart antiVirusPassedPart = new StringPart(
					streamPipe.getAntiVirusPartName(),
					streamPipe.getAntiVirusPassedMessage());
			parts.add(antiVirusPassedPart);
		}
		URL url2 = ClassUtils.getResourceURL(this, "/Documents/doc002.pdf");
		File file2 = new File(url2.toURI());
		FilePart filePart2 = new FilePart("file2", file2.getName(), file2);
		parts.add(filePart2);
		if (addAntiVirusParts) {
			String antiVirusLastPartMessage;
			if (antiVirusLastPartFailed) {
				antiVirusLastPartMessage = "Fail";
				if (antiVirusLastPartMessage.equalsIgnoreCase(
						streamPipe.getAntiVirusPassedMessage())) {
					throw new Exception("fail message ["
							+ antiVirusLastPartMessage
							+ "] must differ from pass message ["
							+ streamPipe.getAntiVirusPassedMessage() + "]");
				}
			} else {
				antiVirusLastPartMessage = streamPipe
						.getAntiVirusPassedMessage();
			}
			StringPart antiVirusPassedPart2 = new StringPart(
					streamPipe.getAntiVirusPartName(),
					antiVirusLastPartMessage);
			parts.add(antiVirusPassedPart2);
		}
		Part allParts[] = new Part[parts.size()];
		allParts = parts.toArray(allParts);
		MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(
				allParts, new PostMethod().getParams());
		ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
		multipartRequestEntity.writeRequest(requestContent);
		request.setContent(requestContent.toByteArray());
		request.setContentType(multipartRequestEntity.getContentType());
		return request;
	}
}
