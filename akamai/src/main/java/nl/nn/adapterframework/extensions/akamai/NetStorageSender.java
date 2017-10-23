/*
   Copyright 2017 Nationale-Nederlanden

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

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.core.TimeoutGuardSenderWithParametersBase;
import nl.nn.adapterframework.extensions.akamai.NetStorageCmsSigner.SignType;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

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
 * <tr><td>{@link #setProxyHost(String) proxyHost}</td><td><b>in case a proxy is being used, the authentication will be globally set and thus can possibly affect other senders</b></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) proxyPort}</td><td>&nbsp;</td><td>80</td></tr>
 * <tr><td>{@link #setProxyAuthAlias(String) proxyAuthAlias}</td><td>alias used to obtain credentials for authentication to proxy</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUserName(String) proxyUserName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(String) proxyPassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
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
public class NetStorageSender extends TimeoutGuardSenderWithParametersBase implements HasPhysicalDestination {
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
	private CredentialFactory proxyCf = null;

	private int proxyPort = 80;
	private String proxyHost;
	private String proxyPassword;
	private String proxyAuthAlias;
	private String proxyUserName;
	private Proxy proxy = null;

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


		ParameterList<Parameter> parameterList = getParameterList();
		if(getAction().equals("upload") && parameterList.findParameter("file") == null)
			throw new ConfigurationException(getLogPrefix()+"the upload action requires a file parameter to be present");
		if(getAction().equals("rename") && parameterList.findParameter("destination") == null)
			throw new ConfigurationException(getLogPrefix()+"the rename action requires a destination parameter to be present");
		if(getAction().equals("mtime") && parameterList.findParameter("mtime") == null)
			throw new ConfigurationException(getLogPrefix()+"the mtime action requires a mtime parameter to be present");

		accessTokenCf = new CredentialFactory(getAuthAlias(), getNonce(), getAccessToken());
		proxyCf = new CredentialFactory(getProxyAuthAlias(), getProxyUserName(), getProxyPassword());

		//TODO probably when we introduce httpcomponents4 we should adapt to that instead of url.openconnection
		Authenticator.setDefault(
			new Authenticator() {
				@Override
				public PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(proxyCf.getUsername(), proxyCf.getPassword().toCharArray());
				}
			}
		);

		if(getProxyHost() != null) {
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getProxyHost(), getProxyPort()));
			log.debug("add proxy settings ["+getProxyHost() + getProxyPort() +"]");
		}
	}

	private URL buildUri(String path) throws SenderException {
		if (!path.startsWith("/")) path = "/" + path;
		try {
			String url = getUrl() + getCpCode();

			if(getRootDir() != null)
				url += getRootDir();

			url += path;

			if(url.endsWith("/")) //The path should never end with a '/'
				url = url.substring(0, url.length() -1);

			return new URL(url);
		} catch (MalformedURLException e) {
			throw new SenderException(e);
		}
	}

	public String sendMessageWithTimeoutGuarded(String correlationID, String path, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		NetStorageAction netStorageAction = new NetStorageAction(getAction());
		netStorageAction.setVersion(actionVersion);
		netStorageAction.setHashAlgorithm(hashAlgorithm);

		ParameterValueList pvl = null;
		try {
			if (prc != null && paramList != null) {
				pvl = prc.getValues(paramList);
				netStorageAction.mapParameters(pvl);
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix()+"Sender ["+getName()+"] caught exception evaluating parameters",e);
		}

		URL url = buildUri(path);
		log.debug("opening ["+netStorageAction.getMethod()+"] connection to ["+url+"] with action ["+getAction()+"]");

		NetStorageCmsSigner signer = new NetStorageCmsSigner(url, accessTokenCf.getUsername(), accessTokenCf.getPassword(), netStorageAction, getSignType());
		Map<String, String> headers = signer.computeHeaders();

		//SEND THE MESSAGE!
		HttpURLConnection request = null;
		String response = "UNKNOWN ERROR";
		try {
			if(proxy != null)
				request = (HttpURLConnection) url.openConnection(proxy);
			else
				request = (HttpURLConnection) url.openConnection();

			request.setRequestMethod(netStorageAction.getMethod());
			request.setConnectTimeout(10000);
			request.setReadTimeout(10000);

			for (Map.Entry<String, String> entry : headers.entrySet()) {
				log.debug("append header ["+ entry.getKey() +"] with value ["+  entry.getValue() +"]");
				request.setRequestProperty(entry.getKey(), entry.getValue());
			}

			if (netStorageAction.getMethod().equals("PUT") || netStorageAction.getMethod().equals("POST")) {
				request.setDoOutput(true);
				if (netStorageAction.getFile() == null) {
					request.setFixedLengthStreamingMode(0);
					request.connect();
				}
				else {
					byte[] buffer = new byte[1024*1024];
					request.setChunkedStreamingMode(buffer.length);
					request.connect();
	
					BufferedInputStream input = new BufferedInputStream(netStorageAction.getFile());
					OutputStream output = request.getOutputStream();
					for (int length; (length = input.read(buffer)) > 0; ) {
						output.write(buffer, 0, length);
					}
					output.flush();
					input.close();
				}
			}
			else
				request.connect();

			if (request.getResponseCode() == HttpURLConnection.HTTP_OK) {
				InputStream responseStream = request.getInputStream();
				if(getAction().equals("download")) {
					prc.getSession().put("fileStream", responseStream);
					response = "success";
				}
				else
					response = Misc.streamToString(responseStream);
			}
			else {
				// Validate Server-Time drift
				Date currentDate = new Date();
				long responseDate = request.getHeaderFieldDate("Date", 0);
				if (responseDate != 0 && currentDate.getTime() - responseDate > 30*1000)
					throw new SenderException("Local server Date is more than 30s out of sync with Remote server");
				else
					throw new SenderException(String.format("Unexpected Response from Server: %d %s\n%s",
						request.getResponseCode(), request.getResponseMessage(), request.getHeaderFields()));
			}
		} catch (Exception e) {
			throw new SenderException(getLogPrefix()+"Sender ["+getName()+"] caught exception opening the connection to url [" + url + "]",e);
		}
		finally {
			request.disconnect();
			Authenticator.setDefault(null);
		}

		return response;
	}

	public void setHashAlgorithm(String hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm.toUpperCase();
	}

	public void setAction(String action) {
		this.action = action.toLowerCase();
	}

	public String getAction() {
		return action;
	}

	public void setActionVersion(int actionVersion) {
		this.actionVersion = actionVersion;
	}

	public void setCpCode(String cpCode) {
		this.cpCode = cpCode;
	}

	public String getCpCode() {
		return cpCode;
	}

	public void setUrl(String url) {
		if(!url.endsWith("/")) url += "/";
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	public String getNonce() {
		return nonce;
	}

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
	public void setRootDir(String rootDir) {
		if(!rootDir.startsWith("/")) rootDir = "/" + rootDir;
		if(rootDir.endsWith("/"))
			rootDir = rootDir.substring(0, rootDir.length()-1);
		this.rootDir = rootDir;
	}

	public String getAuthAlias() {
		return authAlias;
	}
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	public String getProxyHost() {
		return proxyHost;
	}
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	public int getProxyPort() {
		return proxyPort;
	}
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	public String getProxyAuthAlias() {
		return proxyAuthAlias;
	}
	public void setProxyAuthAlias(String proxyAuthAlias) {
		this.proxyAuthAlias = proxyAuthAlias;
	}

	public String getProxyUserName() {
		return proxyUserName;
	}
	public void setProxyUserName(String proxyUserName) {
		this.proxyUserName = proxyUserName;
	}

	public String getProxyPassword() {
		return proxyPassword;
	}
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
}
