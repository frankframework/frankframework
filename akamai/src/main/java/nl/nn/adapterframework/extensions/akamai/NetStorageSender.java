/*
   Copyright 2017 - 2018 Nationale-Nederlanden

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
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.InputStreamEntity;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.extensions.akamai.NetStorageCmsSigner.SignType;
import nl.nn.adapterframework.http.HttpResponseHandler;
import nl.nn.adapterframework.http.HttpSenderBase;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Sender for Akamai NetStorage (HTTP based).
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setAction(String) action}</td><td>possible values: delete, dir, download, du, mkdir, mtime, rename, rmdir, upload</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setActionVersion(int) actionVersion}</td><td>Akamai currently only supports action version 1!</td><td>1</td></tr>
 * 
 * <tr><td>{@link #setCpCode(String) cpCode}</td><td>the CP Code to be used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRootDir(String) rootDir}</td><td><i>optional</i> root directory</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>The destination, aka Akamai host. Only the hostname is allowed; eq. xyz-nsu.akamaihd.net</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSignVersion(int) signVersion}</td><td>the version used to sign the authentication headers. Possible values: 3 (MD5), 4 (SHA1), 5 (SHA256)</td><td>5</td></tr>
 * <tr><td>{@link #setHashAlgorithm(String) hashAlgorithm}</td><td>only works in combination with the <code>upload</code> action. If set, and not specified as parameter, the sender will sign the file to be uploaded. Possible values: md5, sha1, sha256. <br/>NOTE: if the file input is a Stream this will put the file in memory!</td><td>&nbsp;</td></tr>
 * 
 * <tr><td>{@link #setNonce(String) nonce}</td><td>the nonce or api username</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAccessToken(String) accessToken}</td><td>the api accesstoken</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for nonce (username) and accesstoken (password)</td><td>&nbsp;</td></tr>
 * 
 * </table>
 * </p>
 * <p>See {@link nl.nn.adapterframework.http.HttpSenderBase} for more arguments and parameters!</p>
 * 
 * <p><b>Parameters:</b></p>
 * <p>Some actions require specific parameters to be set. Optional parameters for the <code>upload</code> action are: md5, sha1, sha256 and mtime.</p>
 * 
 * <p><b>AuthAlias: (WebSphere based application servers)</b></p>
 * <p>If you do not want to specify the nonce and the accesstoken used to authenticate with Akamai, you can use the authalias property. The username represents the nonce and the password the accesstoken.</p>
 * 
 * <br/>
 * <br/>
 * <br/>
 *  
 * 
 * @author	Niels Meijer
 * @since	7.0-B4
 */
public class NetStorageSender extends HttpSenderBase implements HasPhysicalDestination {
	private Logger log = LogUtil.getLogger(NetStorageSender.class);

	private String action = null;
	private List<String> actions = Arrays.asList("du", "dir", "delete", "upload", "mkdir", "rmdir", "rename", "mtime", "download");
	private String url = null;
	private String nonce = null;
	private int signVersion = 5;
	private int actionVersion = 1;
	private String hashAlgorithm = null;
	private List<String> hashAlgorithms = Arrays.asList("MD5", "SHA1", "SHA256");
	private String rootDir = null;

	private String authAlias;
	private String cpCode = null;
	private String accessToken = null;
	private CredentialFactory accessTokenCf = null;

	public void configure() throws ConfigurationException {
		super.configure();

		//Safety checks
		if(getAction() == null)
			throw new ConfigurationException(getLogPrefix()+"action must be specified");
		if(!actions.contains(getAction()))
			throw new ConfigurationException(getLogPrefix()+"unknown or invalid action ["+getAction()+"] supported actions are "+actions.toString()+"");

		if(getCpCode() == null)
			throw new ConfigurationException(getLogPrefix()+"cpCode must be specified");
		if(!getUrl().startsWith("http"))
			throw new ConfigurationException(getLogPrefix()+"url must be start with http(s)");

		if(hashAlgorithm != null && !hashAlgorithms.contains(hashAlgorithm))
			throw new ConfigurationException(getLogPrefix()+"unknown authenticationMethod ["+hashAlgorithm+"] supported methods are "+hashAlgorithms.toString()+"");

		if(getSignVersion() < 3 || getSignVersion() > 5)
			throw new ConfigurationException(getLogPrefix()+"signVersion must be either 3, 4 or 5");


		ParameterList parameterList = getParameterList();
		if(getAction().equals("upload") && parameterList.findParameter("file") == null)
			throw new ConfigurationException(getLogPrefix()+"the upload action requires a file parameter to be present");
		if(getAction().equals("rename") && parameterList.findParameter("destination") == null)
			throw new ConfigurationException(getLogPrefix()+"the rename action requires a destination parameter to be present");
		if(getAction().equals("mtime") && parameterList.findParameter("mtime") == null)
			throw new ConfigurationException(getLogPrefix()+"the mtime action requires a mtime parameter to be present");

		accessTokenCf = new CredentialFactory(getAuthAlias(), getNonce(), getAccessToken());
	}

	/**
	 * Builds the URI with the rootDirectory, optional CpCode and makes sure the
	 * path never ends with a slash '/'.
	 * @param path to append to the root
	 * @return full path to use as endpoint
	 * @throws SenderException
	 */
	private URIBuilder buildUri(String path) throws SenderException {
		if (!path.startsWith("/")) path = "/" + path;
		try {
			String url = getUrl() + getCpCode();

			if(getRootDir() != null)
				url += getRootDir();

			url += path;

			if(url.endsWith("/")) //The path should never end with a '/'
				url = url.substring(0, url.length() -1);

			return new URIBuilder(url);
		} catch (URISyntaxException e) {
			throw new SenderException(e);
		}
	}

	public String sendMessageWithTimeoutGuarded(String correlationID, String path, ParameterResolutionContext prc) throws SenderException, TimeOutException {

		//The input of this sender is the path where to send or retrieve info from.
		staticUri = buildUri(path);

		//We don't need to send any message to the HttpSenderBase
		return super.sendMessageWithTimeoutGuarded(correlationID, "", prc);
	}

	@Override
	public HttpRequestBase getMethod(URIBuilder uri, String message, ParameterValueList parameters, Map<String, String> headersParamsMap, IPipeLineSession session) throws SenderException {

		NetStorageAction netStorageAction = new NetStorageAction(getAction());
		netStorageAction.setVersion(actionVersion);
		netStorageAction.setHashAlgorithm(hashAlgorithm);

		if(parameters != null)
			netStorageAction.mapParameters(parameters);

		try {
			URL url = uri.build().toURL();

			setMethodType(netStorageAction.getMethod());
			log.debug("opening ["+netStorageAction.getMethod()+"] connection to ["+url+"] with action ["+getAction()+"]");

			NetStorageCmsSigner signer = new NetStorageCmsSigner(url, accessTokenCf.getUsername(), accessTokenCf.getPassword(), netStorageAction, getSignType());
			Map<String, String> headers = signer.computeHeaders();

			boolean queryParametersAppended = false;
			StringBuffer path = new StringBuffer(uri.getPath());

			if (getMethodType().equals("GET")) {
				if (parameters!=null) {
					queryParametersAppended = appendParameters(queryParametersAppended,path,parameters,headersParamsMap);
					log.debug(getLogPrefix()+"path after appending of parameters ["+path.toString()+"]");
				}
				HttpGet method = new HttpGet(uri.build());
				for (Map.Entry<String, String> entry : headers.entrySet()) {
					log.debug("append header ["+ entry.getKey() +"] with value ["+  entry.getValue() +"]");

					method.setHeader(entry.getKey(), entry.getValue());
				}
				log.debug(getLogPrefix()+"HttpSender constructed GET-method ["+method.getURI()+"] query ["+method.getURI().getQuery()+"] ");

				return method;
			}
			else if (getMethodType().equals("PUT")) {
				HttpPut method = new HttpPut(uri.build());

				for (Map.Entry<String, String> entry : headers.entrySet()) {
					log.debug("append header ["+ entry.getKey() +"] with value ["+  entry.getValue() +"]");

					method.setHeader(entry.getKey(), entry.getValue());
				}
				log.debug(getLogPrefix()+"HttpSender constructed GET-method ["+method.getURI()+"] query ["+method.getURI().getQuery()+"] ");

				if(netStorageAction.getFile() != null) {
					HttpEntity entity = new InputStreamEntity(netStorageAction.getFile());
					method.setEntity(entity);
				}
				return method;
			}
			else if (getMethodType().equals("POST")) {
				HttpPost method = new HttpPost(uri.build());

				for (Map.Entry<String, String> entry : headers.entrySet()) {
					log.debug("append header ["+ entry.getKey() +"] with value ["+  entry.getValue() +"]");

					method.setHeader(entry.getKey(), entry.getValue());
				}
				log.debug(getLogPrefix()+"HttpSender constructed GET-method ["+method.getURI()+"] query ["+method.getURI().getQuery()+"] ");

				if(netStorageAction.getFile() != null) {
					HttpEntity entity = new InputStreamEntity(netStorageAction.getFile());
					method.setEntity(entity);
				}
				return method;
			}

		}
		catch (Exception e) {
			throw new SenderException(e);
		}
		return null;
	}

	@Override
	public String extractResult(HttpResponseHandler responseHandler, ParameterResolutionContext prc) throws SenderException, IOException {
		int statusCode = responseHandler.getStatusLine().getStatusCode();

		boolean ok = false;
		if (StringUtils.isNotEmpty(getResultStatusCodeSessionKey())) {
			prc.getSession().put(getResultStatusCodeSessionKey(), Integer.toString(statusCode));
			ok = true;
		} else {
			if (statusCode==HttpServletResponse.SC_OK) {
				ok = true;
			} else {
				if (isIgnoreRedirects()) {
					if (statusCode==HttpServletResponse.SC_MOVED_PERMANENTLY || statusCode==HttpServletResponse.SC_MOVED_TEMPORARILY || statusCode==HttpServletResponse.SC_TEMPORARY_REDIRECT) {
						ok = true;
					}
				}
			}
		}

		if (!ok) {
			throw new SenderException(getLogPrefix() + "httpstatus "
				+ statusCode + ": " + responseHandler.getStatusLine().getReasonPhrase()
				+ " body: " + getResponseBodyAsString(responseHandler));
		}

		XmlBuilder result = new XmlBuilder("result");

		HttpServletResponse response = (HttpServletResponse) prc.getSession().get(IPipeLineSession.HTTP_RESPONSE_KEY);
		if(response == null) {
			XmlBuilder statuscode = new XmlBuilder("statuscode");
			statuscode.setValue(statusCode + "");
			result.addSubElement(statuscode);

			String responseString = getResponseBodyAsString(responseHandler);
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

		return result.toXML();
	}

	public String getResponseBodyAsString(HttpResponseHandler responseHandler) throws IOException {
		String charset = responseHandler.getContentType();
		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"response body uses charset ["+charset+"]");

		String responseBody = responseHandler.getResponseAsString(true);
		int rbLength = responseBody.length();
		long rbSizeWarn = Misc.getResponseBodySizeWarnByDefault();
		if (rbLength >= rbSizeWarn) {
			log.warn(getLogPrefix()+"retrieved result size [" +Misc.toFileSize(rbLength)+"] exceeds ["+Misc.toFileSize(rbSizeWarn)+"]");
		}
		return responseBody;
	}

	/**
	 * Only works in combination with the UPLOAD action. If set, and not 
	 * specified as parameter, the sender will sign the file to be uploaded. 
	 * NOTE: if the file input is a Stream this will put the file in memory!
	 * @param hashAlgorithm supports 3 types; md5, sha1, sha256
	 */
	public void setHashAlgorithm(String hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm.toUpperCase();
	}

	/**
	 * NetStorage action to be used
	 * @param action delete, dir, download, du, mkdir, mtime, rename, 
	 * rmdir, upload
	 * @IbisDoc.required
	 */
	public void setAction(String action) {
		this.action = action.toLowerCase();
	}

	public String getAction() {
		return action;
	}

	/**
	 * At the time of writing, NetStorage only supports version 1
	 * @param actionVersion
	 * @IbisDoc.default 1
	 */
	public void setActionVersion(int actionVersion) {
		this.actionVersion = actionVersion;
	}

	/**
	 * NetStorage CP Code
	 * @param cpCode of the storage group
	 * @IbisDoc.optional
	 */
	public void setCpCode(String cpCode) {
		this.cpCode = cpCode;
	}

	public String getCpCode() {
		return cpCode;
	}

	/**
	 * @param url the base URL for NetStorage (without CpCode)
	 * @IbisDoc.required
	 */
	@Override
	public void setUrl(String url) {
		if(!url.endsWith("/")) url += "/";
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * Login is done via a Nonce and AccessToken
	 * @param nonce to use when logging in
	 */
	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	public String getNonce() {
		return nonce;
	}

	/**
	 * Version to validate queries made to NetStorage backend.
	 * @param signVersion supports 3 types; 3:MD5, 4:SHA1, 5: SHA256
	 * @IbisDoc.default 5 (SHA256)
	 */
	public void setSignVersion(int signVersion) {
		this.signVersion = signVersion;
	}

	public int getSignVersion() {
		return signVersion;
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
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public String getPhysicalDestinationName() {
		return "URL ["+getUrl()+"] cpCode ["+getCpCode()+"] action ["+getAction()+"]";
	}

	public String getRootDir() {
		return rootDir;
	}
	/**
	 * rootDirectory on top of the url + cpCode
	 * @param rootDir
	 * @IbisDoc.optional
	 */
	public void setRootDir(String rootDir) {
		if(!rootDir.startsWith("/")) rootDir = "/" + rootDir;
		if(rootDir.endsWith("/"))
			rootDir = rootDir.substring(0, rootDir.length()-1);
		this.rootDir = rootDir;
	}

	@Override
	public String getAuthAlias() {
		return authAlias;
	}
	/**
	 * @param authAlias to contain the Nonce (username) and AccessToken (password)
	 */
	@Override
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
}
