package nl.nn.adapterframework.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

public class HttpRequestRetryHandler extends DefaultHttpRequestRetryHandler {

	public HttpRequestRetryHandler(int retryCount) {
		super(retryCount, true);
	}

	@Override
	public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
		if(super.retryRequest(exception, executionCount, context)) {
			final HttpClientContext clientContext = HttpClientContext.adapt(context);
			final HttpRequest request = clientContext.getRequest();
			return isRepeatable(request);
		}

		return false;
	}

	/**
	 * See org.apache.http.impl.execchain.RequestEntityProxy#isRepeatable(HttpRequest)
	 */
	public boolean isRepeatable(HttpRequest request) {
		if(request instanceof HttpEntityEnclosingRequest) {
			final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
			return entity.isRepeatable();
		}
		return false;
	}
}
