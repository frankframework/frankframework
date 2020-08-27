/*
   Copyright 2013, 2015 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.JsonPipe;
import nl.nn.adapterframework.stream.Message;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IPushingListener IPushingListener} that enables a {@link nl.nn.adapterframework.receivers.GenericReceiver}
 * to receive REST messages.
 *
 * Note:
 * Servlets' multipart configuration expects a Content-Type of <code>multipart/form-data</code> (see http://docs.oracle.com/javaee/6/api/javax/servlet/annotation/MultipartConfig.html).
 * So do not use other multipart content types like <code>multipart/related</code>
 * </p>
 * @author  Niels Meijer
 * @author  Gerrit van Brakel
 */
public class RestListener extends PushingListenerAdapter<String> implements HasPhysicalDestination, HasSpecialDefaultValues {

	private String uriPattern;
	private String method;
	private String etagSessionKey;
	private String contentTypeSessionKey;
	private String restPath = "/rest";
	private Boolean view = null;
	private String authRoles="IbisAdmin,IbisDataAdmin,IbisTester,IbisObserver,IbisWebService";
	private boolean writeToSecLog = false;
	private boolean writeSecLogMessage = false;
	private boolean retrieveMultipart = true;

	private String consumes = "XML";
	private String produces = "XML";
	private List<String> mediaTypes = Arrays.asList("XML", "JSON", "TEXT");

	private boolean validateEtag = false;
	private boolean generateEtag = false;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (isView()==null) {
			if (StringUtils.isEmpty(getMethod()) || getMethod().equalsIgnoreCase("GET")) {
				setView(true);
			} else {
				setView(false);
			}
		}
	}

	@Override
	public void open() throws ListenerException {
		super.open();
		try {
			RestServiceDispatcher.getInstance().registerServiceClient(this, getUriPattern(), getMethod(), getEtagSessionKey(), getContentTypeSessionKey(), getValidateEtag());
		} catch (ConfigurationException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public void close() {
		super.close();
		RestServiceDispatcher.getInstance().unregisterServiceClient(getUriPattern());
	}

	public String processRequest(String correlationId, String message, IPipeLineSession requestContext) throws ListenerException {
		HttpServletRequest httpServletRequest = (HttpServletRequest) requestContext.get(IPipeLineSession.HTTP_REQUEST_KEY);
		String response;
		String contentType = (String) requestContext.get("contentType");

		//Check if valid path
		String requestRestPath = (String) requestContext.get("restPath");
		if (!getRestPath().equals(requestRestPath)) {
			throw new ListenerException("illegal restPath value [" + requestRestPath + "], must be [" + getRestPath() + "]");
		}

		//Check if consumes has been set or contentType is set to JSON
		if(getConsumes().equalsIgnoreCase("JSON") && "application/json".equalsIgnoreCase(httpServletRequest.getContentType())) {
			try {
				message = transformToXml(message);
			} catch (PipeRunException e) {
				throw new ListenerException("Failed to transform JSON to XML");
			}
		}
		int eTag = 0;

		//Check if contentType is not overwritten which disabled auto-converting and mediatype headers
		if(contentType == null || StringUtils.isEmpty(contentType) || contentType.equalsIgnoreCase("*/*")) {
			if(getProduces().equalsIgnoreCase("XML"))
				requestContext.put("contentType", "application/xml");
			if(getProduces().equalsIgnoreCase("JSON"))
				requestContext.put("contentType", "application/json");
			if(getProduces().equalsIgnoreCase("TEXT"))
				requestContext.put("contentType", "text/plain");

			response = super.processRequest(correlationId, message, requestContext);
			if(response != null && !response.isEmpty())
				eTag = response.hashCode();

			if(getProduces().equalsIgnoreCase("JSON")) {
				try {
					response = transformToJson(response);
				} catch (PipeRunException e) {
					throw new ListenerException("Failed to transform XML to JSON");
				}
			}
		}
		else {
			response = super.processRequest(correlationId, message, requestContext);
			if(response != null && !response.isEmpty())
				eTag = response.hashCode();
		}

		if(!requestContext.containsKey("etag") && getGenerateEtag() && eTag != 0) { //The etag can be a negative integer...
			requestContext.put("etag", RestListenerUtils.formatEtag(getRestPath(), getUriPattern(), eTag));
		}

		return response;
	}

	public String transformToJson(String message) throws PipeRunException {
		JsonPipe pipe = new JsonPipe();
		pipe.setDirection("xml2json");
		PipeRunResult pipeResult = pipe.doPipe(new Message(message), new PipeLineSessionBase());
		try {
			return pipeResult.getResult().asString();
		} catch (IOException e) {
			throw new PipeRunException(null,"cannot transform result",e);
		}
	}

	public String transformToXml(String message) throws PipeRunException {
		JsonPipe pipe = new JsonPipe();
		PipeRunResult pipeResult = pipe.doPipe(new Message(message), new PipeLineSessionBase());
		try {
			return pipeResult.getResult().asString();
		} catch (IOException e) {
			throw new PipeRunException(null,"cannot transform result",e);
		}
	}

	@Override
	public Object getSpecialDefaultValue(String attributeName, Object defaultValue, Map<String, String> attributes) {
		if ("view".equals(attributeName)) {
			if (attributes.get("method").equalsIgnoreCase("GET")) {
				return true;
			} else {
				return false;
			}
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
	

	@IbisDoc({"Uri pattern to match, the {uri} part in https://mydomain.com/ibis4something/rest/{uri}, where mydomain.com and ibis4something refer to 'your ibis'. ", ""})
	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}
	public String getUriPattern() {
		return uriPattern;
	}

	@IbisDoc({"Method (e.g. GET or POST) to match", ""})
	public void setMethod(String method) {
		this.method = method;
	}
	public String getMethod() {
		return method;
	}

	@IbisDoc({"Key of session variable to store etag", ""})
	public void setEtagSessionKey(String etagSessionKey) {
		this.etagSessionKey = etagSessionKey;
	}
	public String getEtagSessionKey() {
		return etagSessionKey;
	}

	@IbisDoc({"Key of Session variable that determines requested content type, overrides {@link #setProduces(String) produces}", ""})
	public void setContentTypeSessionKey(String contentTypeSessionKey) {
		this.contentTypeSessionKey = contentTypeSessionKey;
	}
	public String getContentTypeSessionKey() {
		return contentTypeSessionKey;
	}

	public void setRestPath(String restPath) {
		this.restPath = restPath;
	}
	public String getRestPath() {
		return restPath;
	}

	@IbisDoc({"Indicates whether this listener supports a view (and a link should be put in the ibis console)", "if <code>method=get</code> then <code>true</code>, else <code>false</code>"})
	public void setView(boolean b) {
		view = b;
	}
	public Boolean isView() {
		return view;
	}

	@IbisDoc({"Comma separated list of authorization roles which are granted for this rest service", "IbisAdmin,IbisDataAdmin,IbisTester,IbisObserver,IbisWebService"})
	public void setAuthRoles(String string) {
		authRoles = string;
	}
	public String getAuthRoles() {
		return authRoles;
	}

	public void setWriteToSecLog(boolean b) {
		writeToSecLog = b;
	}
	public boolean isWriteToSecLog() {
		return writeToSecLog;
	}

	public void setWriteSecLogMessage(boolean b) {
		writeSecLogMessage = b;
	}
	public boolean isWriteSecLogMessage() {
		return writeSecLogMessage;
	}

	@IbisDoc({"Indicates whether the parts of a multipart entity should be retrieved and put in session keys. This can only be done once!", "true"})
	public void setRetrieveMultipart(boolean b) {
		retrieveMultipart = b;
	}
	public boolean isRetrieveMultipart() {
		return retrieveMultipart;
	}

	@IbisDoc({"Mediatype (e.g. XML, JSON, TEXT) the {@link nl.nn.adapterframework.http.RestServiceDispatcher restServiceDispatcher} receives as input", "XML"})
	public void setConsumes(String consumes) throws ConfigurationException {
		if(!mediaTypes.contains(consumes)) {
			throw new ConfigurationException("Unknown mediatype ["+consumes+"]");
		}
		this.consumes = consumes;
	}
	public String getConsumes() {
		return consumes;
	}

	@IbisDoc({"Mediatype (e.g. XML, JSON, TEXT) the {@link nl.nn.adapterframework.http.RestServiceDispatcher restServiceDispatcher} sends as output, if set to json the ibis will automatically try to convert the xml message", "XML"})
	public void setProduces(String produces) throws ConfigurationException {
		if(!mediaTypes.contains(produces)) {
			throw new ConfigurationException("Unknown mediatype ["+produces+"]");
		}
		this.produces = produces;
	}
	public String getProduces() {
		return produces;
	}

	@IbisDoc({"If set to true the ibis will automatically validate and process etags", "false"})
	public void setValidateEtag(boolean b) {
		this.validateEtag = b;
	}
	public boolean getValidateEtag() {
		return validateEtag;
	}

	@IbisDoc({"If set to true the ibis will automatically create an etag", "false"})
	public void setGenerateEtag(boolean b) {
		this.generateEtag = b;
	}
	public boolean getGenerateEtag() {
		return generateEtag;
	}
}
