/*
   Copyright 2013, 2015 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.HasSpecialDefaultValues;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.http.rest.ApiListener;
import org.frankframework.pipes.JsonPipe;
import org.frankframework.pipes.JsonPipe.Direction;
import org.frankframework.receivers.Receiver;
import org.frankframework.stream.Message;

/**
 * Listener that allows a {@link Receiver} to receive messages as a REST webservice.
 * Prepends the configured URI pattern with <code>rest/</code>. When you are writing a new Frank config, you are recommended
 * to use an {@link ApiListener} instead. You can find all serviced URI patterns
 * in the Frank!Console: main menu item Webservice, heading Available REST Services.
 * <p>
 * It's possible to use the ApiListener instead with the same path (/rest).
 * Custom pages can be added to the console (using a comma separated list, no spaces) with the following property
 * {@code customViews.names=MyApplication}.
 *
 * Specify details for each view, the url is either a relative path from the web-content folder or an external url, eq. http://google.com/
 * {@code customViews.MyApplication.name=Custom View}
 * {@code customViews.MyApplication.url=myWebapp}
 * </p>
 * <p>
 * Note:
 * Servlets' multipart configuration expects a Content-Type of <code>multipart/form-data</code> (see http://docs.oracle.com/javaee/6/api/javax/servlet/annotation/MultipartConfig.html).
 * So do not use other multipart content types like <code>multipart/related</code>
 * </p>
 * @author  Niels Meijer
 * @author  Gerrit van Brakel
 */
@Deprecated(forRemoval = true, since = "9.0")
@ConfigurationWarning("Please use the ApiListener instead")
public class RestListener extends PushingListenerAdapter implements HasPhysicalDestination, HasSpecialDefaultValues {

	private final @Getter String domain = "Http";
	private @Getter String uriPattern;
	private @Getter String method;
	private @Getter String etagSessionKey;
	private @Getter String contentTypeSessionKey;
	private @Getter String restPath = "/rest";
	private final @Getter Boolean view = null;
	private @Getter String authRoles="IbisWebService,IbisObserver,IbisDataAdmin,IbisAdmin,IbisTester";
	private @Getter boolean writeToSecLog = false;
	private @Getter boolean writeSecLogMessage = false;
	private @Getter boolean retrieveMultipart = true;
	private @Getter boolean automaticallyTransformToAndFromJson = true;

	private @Getter MediaTypes consumes = MediaTypes.XML;
	private @Getter MediaTypes produces = MediaTypes.XML;

	private @Getter boolean validateEtag = false;
	private @Getter boolean generateEtag = false;

	public enum MediaTypes {
		XML, JSON, TEXT
	}

	@Override
	public void start() {
		super.start();
		RestServiceDispatcher.getInstance().registerServiceClient(this, getUriPattern(), getMethod(), getEtagSessionKey(), getContentTypeSessionKey(), isValidateEtag());
	}

	@Override
	public void stop() {
		super.stop();
		RestServiceDispatcher.getInstance().unregisterServiceClient(getUriPattern(), getMethod());
	}

	@Override
	public Message processRequest(Message message, PipeLineSession session) throws ListenerException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
		Message response;
		String acceptHeaderThatIsSomeHowStoredUnderThisKey = (String) session.get("contentType");

		// Check if valid path
		String requestRestPath = (String) session.get("restPath");
		if (!getRestPath().equals(requestRestPath)) {
			throw new ListenerException("illegal restPath value [" + requestRestPath + "], must be [" + getRestPath() + "]");
		}

		// Check if consumes has been set or contentType is set to JSON
		if(automaticallyTransformToAndFromJson && getConsumes()== MediaTypes.JSON && "application/json".equalsIgnoreCase(httpServletRequest.getContentType())) {
			try {
				message = transformToXml(message);
			} catch (PipeRunException e) {
				throw new ListenerException("Failed to transform JSON to XML", e);
			}
		}
		int eTag = 0;

		// Check if contentType is not overwritten which disabled auto-converting and mediatype headers
		if(StringUtils.isEmpty(acceptHeaderThatIsSomeHowStoredUnderThisKey) || "*/*".equalsIgnoreCase(acceptHeaderThatIsSomeHowStoredUnderThisKey)) {
			switch(getProduces()) {
				case XML:
					session.put("contentType", "application/xml");
					break;
				case JSON:
					session.put("contentType", "application/json");
					break;
				case TEXT:
					session.put("contentType", "text/plain");
					break;
				default:
					throw new IllegalStateException("Unknown mediatype ["+getProduces()+"]");
			}

			response = super.processRequest(message, session);
			if(response != null && !response.isEmpty())
				eTag = response.hashCode();

			if(automaticallyTransformToAndFromJson && getProduces()== MediaTypes.JSON) {
				try {
					response = transformToJson(response);
				} catch (PipeRunException | ConfigurationException e) {
					throw new ListenerException("Failed to transform XML to JSON", e);
				}
			}
		}
		else {
			response = super.processRequest(message, session);
			if(response != null && !response.isEmpty())
				eTag = response.hashCode();
		}

		if(!session.containsKey("etag") && isGenerateEtag() && eTag != 0) { // The etag can be a negative integer...
			session.put("etag", "rest_"+eTag);
		}

		return response;
	}

	public Message transformToJson(Message message) throws PipeRunException, ConfigurationException {
		JsonPipe pipe = new JsonPipe();
		pipe.setDirection(Direction.XML2JSON);
		try {
			pipe.configure();
		} catch (ConfigurationException e) {
			throw new ConfigurationException("unable to configure ");
		}
		PipeRunResult pipeResult = pipe.doPipe(message, new PipeLineSession());
		return pipeResult.getResult();
	}

	public Message transformToXml(Message message) throws PipeRunException {
		JsonPipe pipe = new JsonPipe();
		PipeRunResult pipeResult = pipe.doPipe(message, new PipeLineSession());
		return pipeResult.getResult();
	}

	@Override
	public Object getSpecialDefaultValue(String attributeName, Object defaultValue, Map<String, String> attributes) {
		if ("view".equals(attributeName)) { // if attribute view is present
			if (attributes.get("method") == null || "GET".equalsIgnoreCase(attributes.get("method"))) {// if view="true" AND no method has been supplied, or it's set to GET
				return true; //Then the default is TRUE
			}
			return false;
		}
		return defaultValue;
	}

	@Override
	public String getPhysicalDestinationName() {
		return "uriPattern: "+(getUriPattern()==null?"-any-":getUriPattern())+"; method: "+(getMethod()==null?"all":getMethod());
	}

	public String getRestUriPattern() {
		return getRestPath().substring(1) + "/" + getUriPattern();
	}


	/** Uri pattern to match, the {uri} part in https://mydomain.com/ibis4something/rest/{uri}, where mydomain.com and ibis4something refer to 'your ibis'.  */
	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	/** Method (e.g. GET or POST) to match */
	public void setMethod(String method) {
		this.method = method;
	}

	/** Key of session variable to store etag */
	public void setEtagSessionKey(String etagSessionKey) {
		this.etagSessionKey = etagSessionKey;
	}

	/** Key of Session variable that determines requested content type, overrides {@link #setProduces(MediaTypes) produces} */
	public void setContentTypeSessionKey(String contentTypeSessionKey) {
		this.contentTypeSessionKey = contentTypeSessionKey;
	}

	/** Can be either <code>/rest</code> or <code>/rest-public</code> and must correspond with the available RestListenerServlet path(s). */
	public void setRestPath(String restPath) {
		this.restPath = restPath;
	}

	/**
	 * Comma separated list of authorization roles which are granted for this rest service
	 * @ff.default IbisWebService,IbisObserver,IbisDataAdmin,IbisAdmin,IbisTester
	 */
	public void setAuthRoles(String string) {
		authRoles = string;
	}

	public void setWriteToSecLog(boolean b) {
		writeToSecLog = b;
	}

	public void setWriteSecLogMessage(boolean b) {
		writeSecLogMessage = b;
	}

	/**
	 * Indicates whether the parts of a multipart entity should be retrieved and put in session keys. This can only be done once!
	 * @ff.default true
	 */
	public void setRetrieveMultipart(boolean b) {
		retrieveMultipart = b;
	}

	/**
	 * Mediatype (e.g. XML, JSON, TEXT) the {@link RestServiceDispatcher} receives as input
	 * @ff.default XML
	 */
	public void setConsumes(MediaTypes consumes) {
		this.consumes = consumes;
	}

	/**
	 * Mediatype (e.g. XML, JSON, TEXT) the {@link RestServiceDispatcher} sends as output, if set to json the ibis will automatically try to convert the xml message
	 * @ff.default XML
	 */
	public void setProduces(MediaTypes produces) {
		this.produces = produces;
	}

	/**
	 * If set to true the ibis will automatically validate and process etags
	 * @ff.default false
	 */
	public void setValidateEtag(boolean b) {
		this.validateEtag = b;
	}

	/**
	 * If set to true the ibis will automatically create an etag
	 * @ff.default false
	 */
	public void setGenerateEtag(boolean b) {
		this.generateEtag = b;
	}

	/**
	 * Uses an JsonPipe to convert the json-input to xml, and xml-output to json.
	 * Use with caution, a properly configured Input/Output-wrapper can do much more and is more robust!
	 * @ff.default true
	 */
	public void setAutomaticallyTransformToAndFromJson(boolean b) {
		automaticallyTransformToAndFromJson = b;
	}
}
