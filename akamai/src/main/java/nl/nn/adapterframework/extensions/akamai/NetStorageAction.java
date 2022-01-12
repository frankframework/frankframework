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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.extensions.akamai.NetStorageUtils.HashAlgorithm;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.Misc;

/**
 * @author Niels Meijer
 */
public class NetStorageAction {
	private int version = 1;
	private String method = null;
	private InputStream fileStream = null;
	private byte[] fileBytes = null;
	private String action = null;
	private String hashAlgorithm = null;
	private Map<String, String> actionHeader = new HashMap<String, String>();

	public NetStorageAction(String action) {
		this.action = action;
		actionHeader.put("action", action);
		if(action.equals("dir")) {
			method = "GET";
		}
		if(action.equals("du")) {
			method = "GET";
		}
		if(action.equals("delete")) {
			method = "PUT";
		}
		if(action.equals("upload")) {
			method = "PUT";
		}
		if(action.equals("mkdir")) {
			method = "PUT";
		}
		if(action.equals("rmdir")) {
			method = "POST";
		}
		if(action.equals("rename")) {
			method = "POST";
		}
		if(action.equals("mtime")) {
			method = "POST";
		}
		if(action.equals("download")) {
			method = "GET";
		}
	}

	public String getMethod() {
		return method;
	}

	public boolean requiresFileParam() {
		return action.equals("upload");
	}

	public String compileHeader() {
		if(getMethod().equals("GET"))
			actionHeader.put("format", "xml");
		actionHeader.put("version", version+"");
		return NetStorageUtils.convertMapAsQueryParams(actionHeader);
	}

	public void setVersion(int version) {
		this.version = version;
	}

	public void mapParameters(ParameterValueList pvl) throws SenderException {
		if(requiresFileParam()) {
			Object paramValue = pvl.getParameterValue("file").getValue();
			if(paramValue instanceof InputStream)
				fileStream = (InputStream) paramValue;
			else if(paramValue instanceof byte[])
				fileBytes = (byte[]) paramValue;
			else
				throw new SenderException("expected InputStream or ByteArray but got ["+paramValue.getClass().getName()+"] instead");

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
					if(fileStream != null)
						fileBytes = Misc.streamToBytes(fileStream);

					if(md5 == null && hashAlgorithm.equals("MD5")) {
						byte[] checksum = NetStorageUtils.computeHash(fileBytes, HashAlgorithm.MD5);
						if(checksum != null)
							md5 = NetStorageUtils.convertByteArrayToHexString(checksum);
					}
					if(sha1 == null && hashAlgorithm.equals("SHA1")) {
						byte[] checksum = NetStorageUtils.computeHash(fileBytes, HashAlgorithm.SHA1);
						if(checksum != null)
							sha1 = NetStorageUtils.convertByteArrayToHexString(checksum);
					}
					if(sha256 == null && hashAlgorithm.equals("SHA256")) {
						byte[] checksum = NetStorageUtils.computeHash(fileBytes, HashAlgorithm.SHA256);
						if(checksum != null)
							sha256 = NetStorageUtils.convertByteArrayToHexString(checksum);
					}
					fileStream = new ByteArrayInputStream(fileBytes);
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

		if(pvl.parameterExists("mtime")) {
			long mtime = pvl.getParameterValue("mtime").asLongValue(-1L);
			actionHeader.put("mtime", mtime+"");
		}
	}

	public InputStream getFile() {
		return fileStream;
	}

	public String getName() {
		return action;
	}

	public void setHashAlgorithm(String hashAlgorithm) {
		this.hashAlgorithm = hashAlgorithm;
	}

}
