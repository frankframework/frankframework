/*
   Copyright 2013, 2015 Nationale-Nederlanden

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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.JsonPipe;

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
public class RestListener extends PushingListenerAdapter implements HasPhysicalDestination, HasSpecialDefaultValues {

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
			throw new ListenerException("illegal restPath value [" + requestRestPath + "], must be '" + getRestPath() + "'");
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
		PipeRunResult pipeResult = pipe.doPipe(message, new PipeLineSessionBase());
		return (String) pipeResult.getResult();
	}

	public String transformToXml(String message) throws PipeRunException {
		JsonPipe pipe = new JsonPipe();
		PipeRunResult pipeResult = pipe.doPipe(message, new PipeLineSessionBase());
		return (String) pipeResult.getResult();
	}

	public String getPhysicalDestinationName() {
		return "uriPattern: "+(getUriPattern()==null?"-any-":getUriPattern())+"; method: "+(getMethod()==null?"all":getMethod());
	}

	public String getRestUriPattern() {
		return getRestPath().substring(1) + "/" + getUriPattern();
	}
	
	public String getUriPattern() {
		return uriPattern;
	}

	@IbisDoc({"uri pattern to match. ", ""})
	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	public String getMethod() {
		return method;
	}

	@IbisDoc({"method (e.g. get or post) to match", ""})
	public void setMethod(String method) {
		this.method = method;
	}

	public String getEtagSessionKey() {
		return etagSessionKey;
	}

	@IbisDoc({"key of session variable to store etag", ""})
	public void setEtagSessionKey(String etagSessionKey) {
		this.etagSessionKey = etagSessionKey;
	}

	public String getContentTypeSessionKey() {
		return contentTypeSessionKey;
	}

	@IbisDoc({"key of Session variable to requested content type, overwrites {@link #setProduces(String) produces}", ""})
	public void setContentTypeSessionKey(String contentTypeSessionKey) {
		this.contentTypeSessionKey = contentTypeSessionKey;
	}

	public String getRestPath() {
		return restPath;
	}
	public void setRestPath(String restPath) {
		this.restPath = restPath;
	}

	@IbisDoc({"indicates whether this listener supports a view (and a link should be put in the ibis console)", "if <code>method=get</code> then <code>true</code>, else <code>false</code>"})
	public void setView(boolean b) {
		view = b;
	}
	public Boolean isView() {
		return view;
	}

	public Object getSpecialDefaultValue(String attributeName,
			Object defaultValue, Map<String, String> attributes) {
		if ("view".equals(attributeName)) {
			if (attributes.get("method").equalsIgnoreCase("GET")) {
				return true;
			} else {
				return false;
			}
		}
		return defaultValue;
	}

	@IbisDoc({"comma separated list of authorization roles which are granted for this rest service", "ibisadmin,ibisdataadmin,ibistester,ibisobserver,ibiswebservice"})
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

	@IbisDoc({"indicates whether the parts of a multipart entity should be retrieved and put in session keys. this can only be done once!", "true"})
	public void setRetrieveMultipart(boolean b) {
		retrieveMultipart = b;
	}
	public boolean isRetrieveMultipart() {
		return retrieveMultipart;
	}

	@IbisDoc({"mediatype (e.g. xml, json, text) the {@link nl.nn.adapterframework.http.restservicedispatcher restservicedispatcher} receives as input", "xml"})
	public void setConsumes(String consumes) throws ConfigurationException {
		if(mediaTypes.contains(consumes))
			this.consumes = consumes;
		else
			throw new ConfigurationException("Unknown mediatype ["+consumes+"]");
	}

	public String getConsumes() {
		return consumes;
	}

	@IbisDoc({"mediatype (e.g. xml, json, text) the {@link nl.nn.adapterframework.http.restservicedispatcher restservicedispatcher} sends as output, if set to json the ibis will automatically try to convert the xml message", "xml"})
	public void setProduces(String produces) throws ConfigurationException {
		if(mediaTypes.contains(produces))
			this.produces = produces;
		else
			throw new ConfigurationException("Unknown mediatype ["+produces+"]");
	}

	public String getProduces() {
		return produces;
	}

	@IbisDoc({"when set to true the ibis will automatically validate and process etags", "false"})
	public void setValidateEtag(boolean b) {
		this.validateEtag = b;
	}

	public boolean getValidateEtag() {
		return validateEtag;
	}

	@IbisDoc({"when set to true the ibis will automatically create an etag", "false"})
	public void setGenerateEtag(boolean b) {
		this.generateEtag = b;
	}

	public boolean getGenerateEtag() {
		return generateEtag;
	}
}
