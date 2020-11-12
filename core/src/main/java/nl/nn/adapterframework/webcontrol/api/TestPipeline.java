/*
Copyright 2016-2017, 2020 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Test a PipeLine.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class TestPipeline extends Base {
	@Context ServletConfig servletConfig;

	protected Logger secLog = LogUtil.getLogger("SEC");

	private boolean secLogMessage = AppConstants.getInstance().getBoolean("sec.log.includeMessage", false);

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/test-pipeline")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postTestPipeLine(MultipartFormDataInput input) throws ApiException {
		Map<String, Object> result = new HashMap<String, Object>();

		IbisManager ibisManager = getIbisManager();
		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}

		String message = null, fileName = null;
		InputStream file = null;

		Map<String, List<InputPart>> inputDataMap = input.getFormDataMap();

		String adapterName = resolveStringFromMap(inputDataMap, "adapter");
		//Make sure the adapter exists!
		IAdapter adapter = ibisManager.getRegisteredAdapter(adapterName);
		if(adapter == null) {
			throw new ApiException("Adapter ["+adapterName+"] not found");
		}

		String fileEncoding = resolveTypeFromMap(inputDataMap, "encoding", String.class, Misc.DEFAULT_INPUT_STREAM_ENCODING);

		try {
			if(inputDataMap.get("file") != null) {
				file = inputDataMap.get("file").get(0).getBody(InputStream.class, null);
				MultivaluedMap<String, String> headers = inputDataMap.get("file").get(0).getHeaders();
				String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");
				for (String name : contentDispositionHeader) {
					if ((name.trim().startsWith("filename"))) {
						String[] tmp = name.split("=");
						fileName = tmp[1].trim().replaceAll("\"","");
					}
				}

				if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
					try {
						processZipFile(result, file, fileEncoding, adapter, secLogMessage);
					} catch (Exception e) {
						throw new ApiException("An exception occurred while processing zip file", e);
					}
				} else {
					message = Misc.streamToString(file, "\n", fileEncoding, false);
				}
			} else {
				if(inputDataMap.get("message") != null) {
					InputPart part = inputDataMap.get("message").get(0);
					part.setMediaType(part.getMediaType().withCharset(fileEncoding));
					message = part.getBodyAsString();
				}
			}
		} catch (UnsupportedEncodingException e) {
			throw new ApiException("unsupported file encoding ["+fileEncoding+"]");
		} catch (IOException e) {
			throw new ApiException(e);
		}

		if(message == null && file == null) {
			throw new ApiException("must provide either a message or file");
		}

		if (StringUtils.isNotEmpty(message)) {
			try {
				PipeLineResult plr = processMessage(adapter, message, secLogMessage);
				result.put("state", plr.getState());
				result.put("result", plr.getResult());
			} catch (Exception e) {
				throw new ApiException("exception on sending message", e);
			}
		}

		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	private void processZipFile(Map<String, Object> returnResult, InputStream inputStream, String fileEncoding, IAdapter adapter, boolean writeSecLogMessage) throws IOException {
		StringBuilder result = new StringBuilder();
		String lastState = null;
		ZipInputStream archive = new ZipInputStream(inputStream);
		for (ZipEntry entry = archive.getNextEntry(); entry != null; entry = archive.getNextEntry()) {
			String name = entry.getName();
			byte contentBytes[] = StreamUtil.streamToByteArray(archive, true);
			String message = XmlUtils.readXml(contentBytes, fileEncoding, false);
			if (result.length() > 0) {
				result.append("\n");
			}
			lastState = processMessage(adapter, message, writeSecLogMessage).getState();
			result.append(name + ":" + lastState);
			archive.closeEntry();
		}
		archive.close();
		returnResult.put("state", lastState);
		returnResult.put("result", result);
	}

	@SuppressWarnings("rawtypes")
	private PipeLineResult processMessage(IAdapter adapter, String message, boolean writeSecLogMessage) {
		String messageId = "testmessage" + Misc.createSimpleUUID();
		IPipeLineSession pls = new PipeLineSessionBase();
		Map ibisContexts = XmlUtils.getIbisContext(message);
		String technicalCorrelationId = null;
		if (ibisContexts != null) {
			String contextDump = "ibisContext:";
			for (Iterator it = ibisContexts.keySet().iterator(); it.hasNext();) {
				String key = (String) it.next();
				String value = (String) ibisContexts.get(key);
				if (log.isDebugEnabled()) {
					contextDump = contextDump + "\n " + key + "=[" + value + "]";
				}
				if (key.equals(IPipeLineSession.technicalCorrelationIdKey)) {
					technicalCorrelationId = value;
				} else {
					pls.put(key, value);
				}
			}
			if (log.isDebugEnabled()) {
				log.debug(contextDump);
			}
		}
		Date now = new Date();
		PipeLineSessionBase.setListenerParameters(pls, messageId, technicalCorrelationId, now, now);
		if (writeSecLogMessage) {
			secLog.info("message [" + message + "]");
		}
		return adapter.processMessage(messageId, message, pls);
	}
}
