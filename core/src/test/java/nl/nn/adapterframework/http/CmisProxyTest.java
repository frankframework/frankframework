package nl.nn.adapterframework.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

import javax.ws.rs.HttpMethod;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sun.net.httpserver.*;

import edu.emory.mathcs.backport.java.util.Arrays;
import nl.nn.adapterframework.http.cxf.MtomCmisProxy;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.Misc;

@SuppressWarnings("restriction")
@RunWith(Parameterized.class)
public class CmisProxyTest {

	@Parameterized.Parameters(name = "{index} - {0} - {1}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][]{
			{"getRepositoriesSOAP"},
			{"getRepositoriesMTOM"}
		});
	}

	private final String lineSeparator = System.getProperty("line.separator");
	private final String BASEPATH = "/proxy/";
	private String testName;
	private URL request_in;
	private URL request_out;
	private URL response_in;
	private URL response_out;

	public CmisProxyTest(String testFileName) throws Throwable {
		this.testName = testFileName;

		String request_in_path = BASEPATH+testFileName+"_request_in.txt";
		request_in = TestFileUtils.getTestFileURL(request_in_path);
		assertNotNull("request input file ["+request_in_path+"] not found", request_in);

		String request_out_path = BASEPATH+testFileName+"_request_out.txt";
		request_out = TestFileUtils.getTestFileURL(request_out_path);
		assertNotNull("request output file ["+request_out_path+"] not found", request_out);

		String response_in_path = BASEPATH+testFileName+"_response_in.txt";
		response_in = TestFileUtils.getTestFileURL(response_in_path);
		assertNotNull("response input file ["+response_in_path+"] not found", response_in);

		String response_out_path = BASEPATH+testFileName+"_response_out.txt";
		response_out = TestFileUtils.getTestFileURL(response_out_path);
		assertNotNull("response output file ["+response_out_path+"] not found", response_out);
	}

	private String getBoundary(String contentType) {
		String boundary = contentType.substring(contentType.indexOf("boundary=")+9);
		boundary = boundary.substring(0, boundary.indexOf(";"));
		return boundary.replaceAll("\"", "");
	}

	@Test
	public void getRepositories() throws Throwable {
		InetSocketAddress address = new InetSocketAddress(80);
		HttpServer httpServer = HttpServer.create(address, 0);

		HttpHandler handler = new HttpHandler() {
			public void handle(HttpExchange exchange) throws IOException {
				StringBuilder request = new StringBuilder();
				Headers headers = exchange.getRequestHeaders();
				String boundary = null;
				for (String name : headers.keySet()) {
					switch (name.toLowerCase()) {
					case "content-type":
						String contentType = headers.getFirst(name);
						boundary = getBoundary(contentType);
						contentType = contentType.replaceAll(boundary, "IGNORE");
						request.append("Content-Type: " + contentType + lineSeparator);
						break;
					case "content-length":
						request.append("Content-length: xxxx" + lineSeparator);
						break;

					default:
						request.append(String.format("%s: %s%n", name, headers.getFirst(name)));
						break;
					}
				}
				request.append("\n");

				String content = Misc.streamToString(exchange.getRequestBody());
				content = content.replaceAll(boundary, "IGNORE");
				request.append(content);

				String expected = Misc.streamToString(request_out.openStream());
				TestAssertions.assertEqualsIgnoreCRLF(expected, request.toString());

				BufferedReader bufReader = new BufferedReader(new InputStreamReader(response_in.openStream()));
				String line = null;
				while( (line=bufReader.readLine()) != null ) {
					if(!line.isEmpty()) {
						String[] header = line.split(":");
						exchange.getResponseHeaders().add(header[0].trim(), header[1].trim());
					} else {
						break; //Stop when an empty line is found
					}
				}

				String messagebody = Misc.readerToString(bufReader, null, false); //This closes the Reader
				byte[] response = messagebody.getBytes();

				exchange.sendResponseHeaders(200, response.length);
				exchange.getResponseBody().write(response);
				exchange.close();
			}
		};

		httpServer.createContext("/webservice/"+testName, handler);
		httpServer.start();

		MtomCmisProxy proxy = new MtomCmisProxy();
		proxy.init();

		MockHttpServletRequest req = new MockHttpServletRequest(HttpMethod.POST, "/proxy/webservice/"+testName);
		BufferedReader bufReader = new BufferedReader(new InputStreamReader(request_in.openStream()));
		String line = null;
		while( (line=bufReader.readLine()) != null ) {
			if(!line.isEmpty()) {
				String[] header = line.split(":");
				req.addHeader(header[0].trim(), header[1].trim());
			} else {
				break; //Stop when an empty line is found
			}
		}

		String messagebody = Misc.readerToString(bufReader, null, false); //This closes the Reader
		req.setContent(messagebody.getBytes());

		try {
			MockHttpServletResponse res = new MockHttpServletResponse();
			proxy.service(req, res);

			StringBuilder response = new StringBuilder();
			Collection<String> headers = res.getHeaderNames();
			String boundary = null;
			for (String name : headers) {
				switch (name.toLowerCase()) {
				case "content-type":
					String contentType = res.getHeader(name);
					boundary = getBoundary(contentType);
					if(StringUtils.isNotEmpty(boundary)) {
						contentType = contentType.replaceAll(boundary, "IGNORE");
					}
					response.append("Content-Type: " + contentType + lineSeparator);
					break;
				case "content-length":
					response.append("Content-length: xxxx" + lineSeparator);
					break;

				default:
					response.append(String.format("%s: %s%n", name, res.getHeader(name)));
					break;
				}
			}
			response.append("\n");
			assertNotNull("boundary not found", boundary); //Can still be an empty string! just means we have found the Content-Type header

			String content = res.getContentAsString();
			assertNotNull("an error occured", content);
			if(StringUtils.isNotEmpty(boundary)) {
				content = content.replaceAll(boundary, "IGNORE");
			}
			response.append(content);

			TestAssertions.assertEqualsIgnoreCRLF(Misc.streamToString(response_out.openStream()), response.toString());
		} finally {
			httpServer.stop(0);
		}
	}
}
