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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;

import lombok.Getter;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.extensions.akamai.NetStorageSender.Action;
import nl.nn.adapterframework.http.HttpSenderBase.HttpMethod;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * @author Niels Meijer
 */
public class NetStorageAction {
	private int version = 1;
	private @Getter HttpMethod method = null;
	private Message file = null;
	private Action action = null;
	private HashAlgorithm hashAlgorithm = null;
	private Map<String, String> actionHeader = new HashMap<>();

	public NetStorageAction(Action action) {
		this.action = action;
		actionHeader.put("action", action.name().toLowerCase());
		switch (action) {
			case DIR:
			case DU:
			case DOWNLOAD:
				method = HttpMethod.GET;
				break;
			case DELETE:
			case UPLOAD:
			case MKDIR:
				method = HttpMethod.PUT;
				break;
			case RMDIR:
			case RENAME:
			case MTIME:
				method = HttpMethod.POST;
				break;
		}
	}

	public String compileHeader() {
		if(method == HttpMethod.GET) {
			actionHeader.put("format", "xml");
		}
		actionHeader.put("version", version+"");

		return NetStorageUtils.convertMapAsQueryParams(actionHeader);
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void mapParameters(ParameterValueList pvl) throws SenderException {
		if(action == Action.UPLOAD && pvl.parameterExists(NetStorageSender.FILE_PARAM_KEY)) {
			Object paramValue = pvl.getParameterValue(NetStorageSender.FILE_PARAM_KEY).getValue();
			file = Message.asMessage(paramValue);

			if(hashAlgorithm != null) {
				try {
					String hash = null;
					String algorithm = hashAlgorithm.name().toLowerCase();
					if(pvl.parameterExists(NetStorageSender.HASHVALUE_PARAM_KEY)) {
						hash = pvl.getParameterValue(NetStorageSender.HASHVALUE_PARAM_KEY).asStringValue(null);
					}
					else if(pvl.parameterExists(algorithm)) { //backwards compatibility
						hash = pvl.getParameterValue(algorithm).asStringValue(null);
					}
					else {
						hash = hashAlgorithm.computeHash(file);
					}

					if(StringUtils.isNotEmpty(hash)) {
						actionHeader.put(algorithm, hash);
					}
				}
				catch (IOException e) {
					throw new SenderException("error while calculating ["+hashAlgorithm+"] hash", e);
				}
			}

			if(pvl.parameterExists("size")) {
				int size = pvl.getParameterValue("size").asIntegerValue(0);
				actionHeader.put("size", size+"");
			}
		}

		if(action == Action.RENAME && pvl.parameterExists(NetStorageSender.DESTINATION_PARAM_KEY)) {
			String destination = pvl.getParameterValue(NetStorageSender.DESTINATION_PARAM_KEY).asStringValue(null);
			actionHeader.put("destination", destination);
		}

		if(pvl.parameterExists(NetStorageSender.MTIME_PARAM_KEY)) {
			long mtime = pvl.getParameterValue(NetStorageSender.MTIME_PARAM_KEY).asLongValue(-1L);
			actionHeader.put("mtime", mtime+"");
		}
	}

	public HttpEntity getFileEntity() throws IOException {
		if(file == null) {
			return null;
		}

		if(file.requiresStream()) {
			return new InputStreamEntity(file.asInputStream());
		}
		return new ByteArrayEntity(file.asByteArray());
	}

	public void setHashAlgorithm(HashAlgorithm hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm;
	}

}
