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

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.extensions.akamai.NetStorageUtils.HashAlgorithm;
import nl.nn.adapterframework.http.HttpSenderBase.HttpMethod;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * @author Niels Meijer
 */
public class NetStorageAction {
	private int version = 1;
	private HttpMethod method = null;
	private byte[] fileBytes = null;
	private String action = null;
	private HashAlgorithm hashAlgorithm = null;
	private Map<String, String> actionHeader = new HashMap<>();

	public NetStorageAction(String action) {
		this.action = action;
		actionHeader.put("action", action);
		if(action.equals("dir")) {
			method = HttpMethod.GET;
		}
		if(action.equals("du")) {
			method = HttpMethod.GET;
		}
		if(action.equals("delete")) {
			method = HttpMethod.PUT;
		}
		if(action.equals("upload")) {
			method = HttpMethod.PUT;
		}
		if(action.equals("mkdir")) {
			method = HttpMethod.PUT;
		}
		if(action.equals("rmdir")) {
			method = HttpMethod.POST;
		}
		if(action.equals("rename")) {
			method = HttpMethod.POST;
		}
		if(action.equals("mtime")) {
			method = HttpMethod.POST;
		}
		if(action.equals("download")) {
			method = HttpMethod.GET;
		}
	}

	public HttpMethod getMethod() {
		return method;
	}

	public boolean requiresFileParam() {
		return action.equals("upload");
	}

	public String compileHeader() {
		if(getMethod() == HttpMethod.GET) {
			actionHeader.put("format", "xml");
		}
		actionHeader.put("version", version+"");

		return NetStorageUtils.convertMapAsQueryParams(actionHeader);
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void mapParameters(ParameterValueList pvl) throws SenderException {
		if(requiresFileParam()) {
			Object paramValue = pvl.getParameterValue(NetStorageSender.FILE_PARAM_KEY).getValue();
			Message file = Message.asMessage(paramValue);
			try {
				fileBytes = file.asByteArray();
			} catch (IOException e) {
				throw new SenderException("unable to read file from parameter", e);
			}

			String md5 = null;
			String sha1 = null;
			String sha256 = null;
			if(pvl.parameterExists("md5")) {
				md5 = pvl.getParameterValue("md5").asStringValue(null);
			}

			if(pvl.parameterExists("sha1")) {
				sha1 = pvl.getParameterValue("sha1").asStringValue(null);
			}

			if(pvl.parameterExists("sha256")) {
				sha256 = pvl.getParameterValue("sha256").asStringValue(null);
			}

			if(hashAlgorithm != null) {
				try {
					if(md5 == null && hashAlgorithm == HashAlgorithm.MD5) {
						byte[] checksum = NetStorageUtils.computeHash(fileBytes, HashAlgorithm.MD5);
						if(checksum != null)
							md5 = NetStorageUtils.convertByteArrayToHexString(checksum);
					}
					if(sha1 == null && hashAlgorithm == HashAlgorithm.SHA1) {
						byte[] checksum = NetStorageUtils.computeHash(fileBytes, HashAlgorithm.SHA1);
						if(checksum != null)
							sha1 = NetStorageUtils.convertByteArrayToHexString(checksum);
					}
					if(sha256 == null && hashAlgorithm == HashAlgorithm.SHA256) {
						byte[] checksum = NetStorageUtils.computeHash(fileBytes, HashAlgorithm.SHA256);
						if(checksum != null)
							sha256 = NetStorageUtils.convertByteArrayToHexString(checksum);
					}
				}
				catch (Exception e) {
					throw new SenderException("error while calculating ["+hashAlgorithm+"] hash", e);
				}
			}
			if(md5 != null)
				actionHeader.put("md5", md5);
			if(sha1 != null)
				actionHeader.put("sha1", sha1);
			if(sha256 != null)
				actionHeader.put("sha256", sha256);

			if(pvl.parameterExists("size")) {
				int size = pvl.getParameterValue("size").asIntegerValue(0);
				actionHeader.put("size", size+"");
			}
		}

		if(action.equals("rename") && pvl.parameterExists(NetStorageSender.DESTINATION_PARAM_KEY)) {
			String destination = pvl.getParameterValue(NetStorageSender.DESTINATION_PARAM_KEY).asStringValue(null);
			actionHeader.put("destination", destination);
		}

		if(pvl.parameterExists(NetStorageSender.MTIME_PARAM_KEY)) {
			long mtime = pvl.getParameterValue(NetStorageSender.MTIME_PARAM_KEY).asLongValue(-1L);
			actionHeader.put("mtime", mtime+"");
		}
	}

	public byte[] getFile() {
		return fileBytes;
	}

	public void setHashAlgorithm(HashAlgorithm hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm;
	}

}
