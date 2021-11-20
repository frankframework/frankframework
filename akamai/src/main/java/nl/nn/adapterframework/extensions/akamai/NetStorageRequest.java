/*
   Copyright 2017 Nationale-Nederlanden, 2021 WeAreFrank!

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
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.logging.log4j.Logger;

import lombok.Setter;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.extensions.akamai.NetStorageSender.Action;
import nl.nn.adapterframework.http.HttpSenderBase.HttpMethod;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.LogUtil;

/**
 * @author Niels Meijer
 */
public class NetStorageRequest {
	private Logger log = LogUtil.getLogger(NetStorageRequest.class);

	private int version = 1;
	private Message file = null;
	private Action action = null;
	private @Setter HashAlgorithm hashAlgorithm = null;
	private Map<String, String> actionHeader = new HashMap<>();
	private HttpRequestBase method;

	protected NetStorageRequest(Action action) {
		this(null, action);
	}

	public NetStorageRequest(URI uri, Action action) {
		this.action = action;
		actionHeader.put("action", action.name().toLowerCase());

		switch (action) {
			case DIR:
			case DU:
			case DOWNLOAD:
				method = new HttpGet(uri);
				break;
			case DELETE:
			case UPLOAD:
			case MKDIR:
				method = new HttpPut(uri);
				break;
			case RMDIR:
			case RENAME:
			case MTIME:
				method = new HttpPost(uri);
				break;
			default:
				throw new NotImplementedException("unknown action ["+action+"]");
		}
	}

	public String compileHeader() {
		if(method instanceof HttpGet) {
			actionHeader.put("format", "xml");
		}
		actionHeader.put("version", version+"");

		return NetStorageUtils.convertMapAsQueryParams(actionHeader);
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void mapParameters(ParameterValueList pvl) throws SenderException {
		if(action == Action.UPLOAD && pvl.contains(NetStorageSender.FILE_PARAM_KEY)) {
			file = pvl.getParameterValue(NetStorageSender.FILE_PARAM_KEY).asMessage();
			if(Message.isEmpty(file)) {
				throw new SenderException("no file specified");
			}

			if(hashAlgorithm != null) {
				try {
					generateHash(pvl);
				}
				catch (IOException e) {
					throw new SenderException("error while calculating ["+hashAlgorithm+"] hash", e);
				}
			}

			try {
				setEntity(file);
			} catch (IOException e) {
				throw new SenderException("unable to parse file", e);
			}

			if(pvl.contains("size")) {
				int size = pvl.getParameterValue("size").asIntegerValue(0);
				actionHeader.put("size", size+"");
			}
		}

		if(action == Action.RENAME && pvl.contains(NetStorageSender.DESTINATION_PARAM_KEY)) {
			String destination = pvl.getParameterValue(NetStorageSender.DESTINATION_PARAM_KEY).asStringValue(null);
			actionHeader.put("destination", destination);
		}

		if(pvl.contains(NetStorageSender.MTIME_PARAM_KEY)) {
			long mtime = pvl.getParameterValue(NetStorageSender.MTIME_PARAM_KEY).asLongValue(-1L);
			actionHeader.put("mtime", mtime+"");
		}
	}

	private void generateHash(ParameterValueList pvl) throws IOException {
		String hash = null;
		String algorithm = hashAlgorithm.name().toLowerCase();
		if(pvl.contains(NetStorageSender.HASHVALUE_PARAM_KEY)) {
			hash = pvl.getParameterValue(NetStorageSender.HASHVALUE_PARAM_KEY).asStringValue(null);
		}
		else if(pvl.contains(algorithm)) { //backwards compatibility
			hash = pvl.getParameterValue(algorithm).asStringValue(null);
		}
		else {
			hash = hashAlgorithm.computeHash(file);
		}

		if(StringUtils.isNotEmpty(hash)) {
			actionHeader.put(algorithm, hash);
		}
	}

	public HttpMethod getMethodType() {
		return EnumUtils.parse(HttpMethod.class, method.getMethod());
	}

	private void setEntity(Message file) throws IOException {
		HttpEntity entity = toEntity(file);
		((HttpEntityEnclosingRequestBase) method).setEntity(entity);
	}

	private HttpEntity toEntity(Message file) throws IOException {
		if(file.requiresStream()) {
			return new InputStreamEntity(file.asInputStream());
		}
		return new ByteArrayEntity(file.asByteArray());
	}

	public void sign(NetStorageCmsSigner signer) {
		Map<String, String> headers = signer.computeHeaders(this);

		for (Map.Entry<String, String> entry : headers.entrySet()) {
			if(log.isDebugEnabled()) log.debug("append header ["+ entry.getKey() +"] with value ["+  entry.getValue() +"]");

			method.setHeader(entry.getKey(), entry.getValue());
		}
	}

	public HttpRequestBase build() {
		return method;
	}
}
