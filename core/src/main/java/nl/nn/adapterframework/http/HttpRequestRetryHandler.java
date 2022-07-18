package nl.nn.adapterframework.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

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
