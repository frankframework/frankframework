/*
   Copyright 2017-2019 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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
package nl.nn.adapterframework.extensions.akamai;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.akamai.NetStorageCmsSigner.SignType;
import nl.nn.adapterframework.http.HttpResponseHandler;
import nl.nn.adapterframework.http.HttpSenderBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sender for Akamai NetStorage (HTTP based).
 *
 * <p>See {@link HttpSenderBase} for more arguments and parameters!</p>
 *
 *
 * <p><b>AuthAlias:</b></p>
 * <p>If you do not want to specify the nonce and the accesstoken used to authenticate with Akamai, you can use the authalias property. The username represents the nonce and the password the accesstoken.</p>
 *
 * @ff.parameters Some actions require specific parameters to be set. Optional parameters for the <code>upload</code> action are: md5, sha1, sha256 and mtime.
 *
 * @author	Niels Meijer
 * @since	7.0-B4
 */
public class NetStorageSender extends HttpSenderBase {
	private Logger log = LogUtil.getLogger(NetStorageSender.class);
	private static final String URL_PARAM_KEY = "urlParameter";
	public static final String DESTINATION_PARAM_KEY = "destination";
	public static final String FILE_PARAM_KEY = "file";
	public static final String MTIME_PARAM_KEY = "mtime";
	public static final String HASHVALUE_PARAM_KEY = "hashValue";

	private @Getter Action action = null;
	public enum Action {
		DU, DIR, DELETE, UPLOAD, MKDIR, RMDIR, RENAME, MTIME, DOWNLOAD;
	}

	private @Getter int signVersion = 5;
	private @Getter int actionVersion = 1;
	private @Getter String rootDir = null;

	private HashAlgorithm hashAlgorithm = null;

	private @Getter String cpCode = null;
	private @Getter String authAlias = null;
	private @Getter String nonce = null;
	private @Getter String accessToken = null;
	private CredentialFactory accessTokenCf = null;

	@Override
	public void configure() throws ConfigurationException {
		//The HttpSenderBase dictates that you must use a Parameter with 'getUrlParam()' as name to use a dynamic endpoint.
		//In order to not force everyone to use the URL parameter but instead the input of the sender as 'dynamic' path, this exists.
		Parameter urlParameter = new Parameter();
		urlParameter.setName(getUrlParam());
		urlParameter.setSessionKey(URL_PARAM_KEY);
		addParameter(urlParameter);

		super.configure();

		//Safety checks
		if(getAction() == null)
			throw new ConfigurationException(getLogPrefix()+"action must be specified");

		if(getCpCode() == null)
			throw new ConfigurationException(getLogPrefix()+"cpCode must be specified");
		if(!getUrl().startsWith("http"))
			throw new ConfigurationException(getLogPrefix()+"url must be start with http(s)");

		if(getSignVersion() < 3 || getSignVersion() > 5)
			throw new ConfigurationException(getLogPrefix()+"signVersion must be either 3, 4 or 5");


		ParameterList parameterList = getParameterList();
		if(getAction() == Action.UPLOAD && parameterList.findParameter(FILE_PARAM_KEY) == null) {
			throw new ConfigurationException(getLogPrefix()+"the upload action requires a file parameter to be present");
		}
		if(getAction() == Action.RENAME && parameterList.findParameter(DESTINATION_PARAM_KEY) == null) {
			throw new ConfigurationException(getLogPrefix()+"the rename action requires a destination parameter to be present");
		}
		if(getAction() == Action.MTIME && parameterList.findParameter(MTIME_PARAM_KEY) == null) {
			throw new ConfigurationException(getLogPrefix()+"the mtime action requires a mtime parameter to be present");
		}

		//check if md5/sha1/sha256 -> geef deprecated warning + parse hashAlgorithme
		//hashValue  parameterList
		for(HashAlgorithm algorithm : HashAlgorithm.values()) {
			String simpleName = algorithm.name().toLowerCase();
			Parameter hashValue = parameterList.findParameter(simpleName);

			if(hashValue != null) {
				setHashAlgorithm(algorithm);
				ConfigurationWarnings.add(this, log, "deprecated parameter ["+simpleName+"]: please use attribute [hashAlgorithm] in combination with parameter ["+HASHVALUE_PARAM_KEY+"]");
			}
		}

		accessTokenCf = new CredentialFactory(getAuthAlias(), getNonce(), getAccessToken());
	}

	/**
	 * Builds the URI with the rootDirectory, optional CpCode and makes sure the
	 * path never ends with a slash '/'.
	 * @param path to append to the root
	 * @return full path to use as endpoint
	 */
	@Override
	protected URI getURI(String path) throws URISyntaxException {
		if (!path.startsWith("/")) path = "/" + path;
		String url = getUrl() + getCpCode();

		if(getRootDir() != null)
			url += getRootDir();

		url += path;

		if(url.endsWith("/")) //The path should never end with a '/'
			url = url.substring(0, url.length() -1);

		return new URIBuilder(url).build();
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {

		//The input of this sender is the path where to send or retrieve info from.
		String path;
		try {
			path = message.asString();
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(), e);
		}
		//Store the input in the PipeLineSession, so it can be resolved as ParameterValue.
		//See {@link HttpSenderBase#getURI getURI(..)} how this is resolved
		session.put(URL_PARAM_KEY, path);

		//We don't need to send any message to the HttpSenderBase
		return super.sendMessage(new Message(""), session);
	}

	@Override
	public HttpRequestBase getMethod(URI uri, Message message, ParameterValueList parameters, PipeLineSession session) throws SenderException {
		NetStorageRequest request = new NetStorageRequest(uri, getAction());
		request.setVersion(actionVersion);
		request.setHashAlgorithm(hashAlgorithm);

		if(parameters != null) {
			request.mapParameters(parameters);
		}

		setMethodType(request.getMethodType()); //For logging purposes
		if(log.isDebugEnabled()) log.debug("opening ["+request.getMethodType()+"] connection to ["+uri+"] with action ["+getAction()+"]");

		NetStorageCmsSigner signer = new NetStorageCmsSigner(uri, accessTokenCf, getSignType());
		request.sign(signer);

		return request.build();
	}

	@Override
	public Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws SenderException, IOException {
		int statusCode = responseHandler.getStatusLine().getStatusCode();

		boolean ok = false;
		if (StringUtils.isNotEmpty(getResultStatusCodeSessionKey())) {
			session.put(getResultStatusCodeSessionKey(), Integer.toString(statusCode));
			ok = true;
		} else {
			if (statusCode==HttpServletResponse.SC_OK) {
				ok = true;
			} else if (isFollowRedirects() &&
					statusCode == HttpServletResponse.SC_MOVED_PERMANENTLY ||
					statusCode == HttpServletResponse.SC_MOVED_TEMPORARILY ||
					statusCode == HttpServletResponse.SC_TEMPORARY_REDIRECT) {
				ok = true;
			}
		}

		if (!ok) {
			throw new SenderException(getLogPrefix() + "httpstatus "
					+ statusCode + ": " + responseHandler.getStatusLine().getReasonPhrase()
					+ " body: " + getResponseBodyAsString(responseHandler, false));
		}

		XmlBuilder result = new XmlBuilder("result");

		HttpServletResponse response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);
		if(response == null) {
			XmlBuilder statuscode = new XmlBuilder("statuscode");
			statuscode.setValue(statusCode + "");
			result.addSubElement(statuscode);

			String responseString = getResponseBodyAsString(responseHandler, true);
			responseString = XmlUtils.skipDocTypeDeclaration(responseString.trim());
			responseString = XmlUtils.skipXmlDeclaration(responseString);

			if (statusCode == HttpURLConnection.HTTP_OK) {
				XmlBuilder message = new XmlBuilder("message");
				message.setValue(responseString, false);
				result.addSubElement(message);
			}
			else {
				// Validate Server-Time drift
				String dateString = responseHandler.getHeader("Date");
				if(!StringUtils.isEmpty(dateString)) {
					Date currentDate = new Date();
					DateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
					long responseDate = 0;

					try {
						Date date = format.parse(dateString);
						responseDate = date.getTime();
					}
					catch (Exception e) {}

					if (responseDate != 0 && currentDate.getTime() - responseDate > 30*1000)
						throw new SenderException("Local server Date is more than 30s out of sync with Remote server");
				}
				XmlBuilder message = new XmlBuilder("error");
				message.setValue(responseString);
				result.addSubElement(message);

				log.warn(String.format("Unexpected Response from Server: %d %s\n%s",
						statusCode, responseString, responseHandler.getHeaderFields()));
			}
		}

		return Message.asMessage(result.toXML());
	}

	/**
	 * When an exception occurs and the response cannot be parsed, we do not want to throw a 'missing response' exception.
	 * Since this method is used when handling exceptions, silently return null, to avoid NPE's and IOExceptions
	 */
	public String getResponseBodyAsString(HttpResponseHandler responseHandler, boolean throwIOExceptionWhenParsingResponse) throws IOException {
		String charset = responseHandler.getCharset();
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"response body uses charset ["+charset+"]");

		Message response = responseHandler.getResponseMessage();

		try {
			return response.asString();
		} catch(IOException e) {
			if(throwIOExceptionWhenParsingResponse) {
				throw e;
			}
			return null;
		}
	}

	/** Only works in combination with the UPLOAD action. If set, and not specified as parameter, the sender will sign the file to be uploaded.*/
	public void setHashAlgorithm(HashAlgorithm hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm;
	}

	/** NetStorage action to be used */
	public void setAction(Action action) {
		this.action = action;
	}

	/** At the time of writing, NetStorage only supports version 1
	 * @ff.default 1 */
	public void setActionVersion(int actionVersion) {
		this.actionVersion = actionVersion;
	}

	/** NetStorage CP Code of the storage group */
	public void setCpCode(String cpCode) {
		this.cpCode = cpCode;
	}

	/** The destination URL for the Akamai NetStorage. (Only the hostname, without CpCode; eq. xyz-nsu.akamaihd.net) */
	@Override
	public void setUrl(String url) {
		if(!url.endsWith("/")) url += "/";
		super.setUrl(url);
	}

	/**
	 * Login is done via a Nonce and AccessToken
	 * @param nonce to use when logging in
	 */
	@IbisDoc({"the nonce or api username", ""})
	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	/**
	 * Version to validate queries made to NetStorage backend.
	 * @param signVersion supports 3 types; 3:MD5, 4:SHA1, 5: SHA256
	 */
	@IbisDoc({"the version used to sign the authentication headers. possible values: 3 (md5), 4 (sha1), 5 (sha256)", "5"})
	public void setSignVersion(int signVersion) {
		this.signVersion = signVersion;
	}
	public SignType getSignType() {
		if(getSignVersion() == 3)
			return SignType.HMACMD5;
		else if(getSignVersion() == 4)
			return SignType.HMACSHA1;
		else
			return SignType.HMACSHA256;
	}

	/**
	 * Login is done via a Nonce and AccessToken
	 * @param accessToken to use when logging in
	 */
	@IbisDoc({"the api accesstoken", ""})
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	@Override
	public String getPhysicalDestinationName() {
		return "URL ["+getUrl()+"] cpCode ["+getCpCode()+"] action ["+getAction()+"]";
	}

	/** Root directory (appended to the url + cpCode) */
	public void setRootDir(String rootDir) {
		if(!rootDir.startsWith("/")) rootDir = "/" + rootDir;
		if(rootDir.endsWith("/"))
			rootDir = rootDir.substring(0, rootDir.length()-1);
		this.rootDir = rootDir;
	}

	/** Alias used to obtain credentials for nonce (username) and accesstoken (password) */
	@Override //Overridden to prevent the super class from setting credentials
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
}
