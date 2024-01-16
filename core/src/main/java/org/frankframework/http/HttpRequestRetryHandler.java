/*
   Copyright 2022 WeAreFrank!

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
package org.frankframework.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.Logger;

import org.frankframework.util.LogUtil;

/**
 * Only retries if no HttpEntity is present, or if the HttpEntity is repeatable.
 * This avoids a NonRepeatableRequestException and returns the original exception.
 *
 * @author Niels Meijer
 */
public class HttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {
	Logger log = LogUtil.getLogger(this);

	public HttpRequestRetryHandler(int retryCount) {
		super(retryCount, true);
	}

	@Override
	public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
		final HttpClientContext clientContext = HttpClientContext.adapt(context);
		final HttpRequest request = clientContext.getRequest();
		if(isRepeatable(request)) {
			log.info("attempt to retry message to [{}] count [{}]", request.getRequestLine(), executionCount);
			return super.retryRequest(exception, executionCount, context);
		}

		log.info("unable to retry message to [{}] message is not repeatable!", request::getRequestLine);
		return false;
	}

	/**
	 * Only attempt to retry the request if the request supports it!
	 *
	 * See org.apache.http.impl.execchain.RequestEntityProxy#isRepeatable(HttpRequest)
	 */
	public boolean isRepeatable(HttpRequest request) {
		if(request instanceof HttpEntityEnclosingRequest) {
			final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			return entity.isRepeatable();
		}
		return true;
	}
}
