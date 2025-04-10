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
package org.frankframework.extensions.akamai;

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
import org.apache.logging.log4j.Logger;

import lombok.Setter;

import org.frankframework.core.SenderException;
import org.frankframework.extensions.akamai.NetStorageSender.Action;
import org.frankframework.http.AbstractHttpSender.HttpMethod;
import org.frankframework.http.HttpMessageEntity;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.LogUtil;

/**
 * Translates the request, adds required headers per action, creates a hash and signs the message.
 *
 * @author Niels Meijer
 */
public class NetStorageRequest {
	private final Logger log = LogUtil.getLogger(NetStorageRequest.class);

	private int version = 1;
	private Message file = null;
	private Action action = null;
	private @Setter HashAlgorithm hashAlgorithm = null;
	private final Map<String, String> actionHeader = new HashMap<>();
	private final HttpRequestBase method;

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
			file = pvl.get(NetStorageSender.FILE_PARAM_KEY).asMessage();
			if(Message.isEmpty(file)) {
				throw new SenderException("no file specified");
			}

			if(hashAlgorithm != null) {
				generateHash(pvl);
			}

			setEntity(file);

			if(pvl.contains("size")) {
				int size = pvl.get("size").asIntegerValue(0);
				actionHeader.put("size", size+"");
			}
		}

		if(action == Action.RENAME && pvl.contains(NetStorageSender.DESTINATION_PARAM_KEY)) {
			String destination = pvl.get(NetStorageSender.DESTINATION_PARAM_KEY).asStringValue(null);
			actionHeader.put("destination", destination);
		}

		if(pvl.contains(NetStorageSender.MTIME_PARAM_KEY)) {
			long mtime = pvl.get(NetStorageSender.MTIME_PARAM_KEY).asLongValue(-1L);
			actionHeader.put("mtime", mtime+"");
		}
	}

	private void generateHash(ParameterValueList pvl) throws SenderException {
		String hash = null;
		String algorithm = hashAlgorithm.name().toLowerCase();
		if(pvl.contains(NetStorageSender.HASHVALUE_PARAM_KEY)) {
			hash = pvl.get(NetStorageSender.HASHVALUE_PARAM_KEY).asStringValue(null);
		}
		else if(pvl.contains(algorithm)) { // Backwards compatibility
			hash = pvl.get(algorithm).asStringValue(null);
		}
		else {
			try {
				hash = hashAlgorithm.computeHash(file);
			}
			catch (IOException | IllegalStateException e) {
				throw new SenderException("error while calculating ["+hashAlgorithm+"] hash", e);
			}
		}

		if(StringUtils.isNotEmpty(hash)) {
			actionHeader.put(algorithm, hash);
		}
	}

	public HttpMethod getMethodType() {
		return EnumUtils.parse(HttpMethod.class, method.getMethod());
	}

	private void setEntity(Message file) {
		HttpEntity entity = new HttpMessageEntity(file);
		((HttpEntityEnclosingRequestBase) method).setEntity(entity);
	}

	public void sign(NetStorageCmsSigner signer) {
		Map<String, String> headers = signer.computeHeaders(this);

		for (Map.Entry<String, String> entry : headers.entrySet()) {
			if(log.isDebugEnabled()) log.debug("append header [{}] with value [{}]", entry.getKey(), entry.getValue());

			method.setHeader(entry.getKey(), entry.getValue());
		}
	}

	public HttpRequestBase build() {
		return method;
	}
}
