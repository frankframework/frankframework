/*
   Copyright 2018-2019 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.http;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;

import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.testutil.TestFileUtils;

public abstract class HttpSenderTestBase<S extends HttpSenderBase> extends SenderTestBase<S> {

	public S getSender() throws Exception {
		return getSender(true);
	}

	public S getSender(boolean addCustomHeader) throws Exception {
		sender = createSender();
		if(sender == null)
			fail("sender not initialized");

		CloseableHttpClient httpClient = mock(CloseableHttpClient.class);

		//Mock all requests
		when(httpClient.execute(any(HttpHost.class), any(HttpRequestBase.class), any(HttpContext.class))).thenAnswer(new HttpResponseMock());

		when(sender.getHttpClient()).thenReturn(httpClient);

		//Some default settings, url will be mocked.
		sender.setUrl("http://127.0.0.1/");
		sender.setVerifyHostname(false);
		sender.setAllowSelfSignedCertificates(true);

		if(addCustomHeader) {
			sender.setHeadersParams("custom-header");
			Parameter headerParameter = new Parameter();
			headerParameter.setName("custom-header");
			headerParameter.setValue("value");
			sender.addParameter(headerParameter);
		}

		return sender;
	}

	private final String BASEDIR = "/nl/nn/adapterframework/http/response/";
	protected String getFile(String file) throws IOException {
		String content = TestFileUtils.getTestFile(BASEDIR+file);
		assertNotNull("file ["+BASEDIR+file+"] not found", content);
		return content;
	}
}