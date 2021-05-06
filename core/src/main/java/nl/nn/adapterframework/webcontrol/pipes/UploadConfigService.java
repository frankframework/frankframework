/*
   Copyright 2019, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol.pipes;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;

public class UploadConfigService extends FixedForwardPipe {
	private static final String HTTP_STATUS_CODE = "httpStatusCode";
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final boolean CONFIG_AUTO_DB_CLASSLOADER = APP_CONSTANTS.getBoolean("configurations.autoDatabaseClassLoader", false);

	private IbisContext ibisContext;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		ibisContext = getAdapter().getConfiguration().getIbisManager().getIbisContext();
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result = null;

		InputStream inputStream = (InputStream) session.get("file");
		if (inputStream == null) {
			result = createJsonErrorResponse("invalid_request", "missing_file");
			session.put(HTTP_STATUS_CODE, 400);
			return new PipeRunResult(getSuccessForward(), result);
		}
		String fileName = (String) session.get("fileName");
		if (StringUtils.isEmpty(fileName)) {
			result = createJsonErrorResponse("invalid_request",
					"missing_filename");
			session.put(HTTP_STATUS_CODE, 400);
			return new PipeRunResult(getSuccessForward(), result);
		}

		String name = null;
		String version = null;
		String datasource = null;
		String remoteUser = (String) session.get("realPrincipal");
		if (StringUtils.isEmpty(remoteUser)) {
			remoteUser = (String) session.get("principal");
		}

		try {
			// convert inputStream to byteArray so it can be read twice
			byte[] bytes = IOUtils.toByteArray(inputStream);
			String[] buildInfo = ConfigurationUtils.retrieveBuildInfo(new ByteArrayInputStream(bytes));
			name = buildInfo[0];
			version = buildInfo[1];
			if (StringUtils.isEmpty(name) || StringUtils.isEmpty(version)) {
				result = createJsonErrorResponse("invalid_request", "missing_name_version");
				session.put(HTTP_STATUS_CODE, 400);
				return new PipeRunResult(getSuccessForward(), result);
			}

			fileName = name + "-" + version + ".jar";
			if (ConfigurationUtils.addConfigToDatabase(ibisContext, datasource, true, true, name, version, fileName, new ByteArrayInputStream(bytes), remoteUser)) {
				if (CONFIG_AUTO_DB_CLASSLOADER && ibisContext.getIbisManager().getConfiguration(name) == null) {
					ibisContext.reload(name);
				}
				result = "{\"ok\":\"\"}";
				session.put(HTTP_STATUS_CODE, 201);
			} else {
				throw new PipeRunException(this, getLogPrefix(session) + "Adding config to database resulted in rowcount zero");
			}
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + "Error occured on adding config to database", e);
		}

		return new PipeRunResult(getSuccessForward(), result);
	}

	private String createJsonErrorResponse(String error,
			String errorDescription) throws PipeRunException {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.put("error", error);
			if (errorDescription != null) {
				jsonObject.put("error_description", errorDescription);
			}
			return jsonObject.toString();
		} catch (JSONException e) {
			throw new PipeRunException(this,
					"error occured on creating json error response", e);
		}
	}
}