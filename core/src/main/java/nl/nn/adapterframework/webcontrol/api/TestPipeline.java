/*
Copyright 2016 Integration Partners B.V.

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

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

/**
* Test a PipeLine.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class TestPipeline extends TimeoutGuardPipe {
	@Context ServletConfig servletConfig;

	protected Logger secLog = LogUtil.getLogger("SEC");

	private boolean secLogEnabled = AppConstants.getInstance().getBoolean("sec.log.enabled", false);
	private boolean secLogMessage = AppConstants.getInstance().getBoolean("sec.log.includeMessage", false);

	@POST
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/test-pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postTestPipeLine(MultipartFormDataInput input) throws ApiException, PipeRunException {
		Map<String, Object> result = new HashMap<String, Object>();
		
		IbisManager ibisManager = getIbisManager();
		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}

		boolean writeSecLogMessage = (Boolean) (secLogEnabled && secLogMessage);
		
		String message = null, fileEncoding = null, fileName = null;
		InputStream file = null;
		IAdapter adapter = null;
		
		Map<String, List<InputPart>> inputDataMap = input.getFormDataMap();
		try {
			if(inputDataMap.get("message") != null)
				message = inputDataMap.get("message").get(0).getBodyAsString();
			if(inputDataMap.get("encoding") != null)
				fileEncoding = inputDataMap.get("encoding").get(0).getBodyAsString();
			if(inputDataMap.get("adapter") != null) {
				String adapterName = inputDataMap.get("adapter").get(0).getBodyAsString();
				adapter = ibisManager.getRegisteredAdapter(adapterName);
			}
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
						processZipFile(result, file, fileEncoding, adapter, writeSecLogMessage);
					} catch (Exception e) {
						throw new PipeRunException(this, getLogPrefix(null) + "exception on processing zip file", e);
					}
				} else {
					message = Misc.streamToString(file, "\n", fileEncoding, false);
				}
			}
		} catch (IOException e) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		if(fileEncoding == null || StringUtils.isEmpty(fileEncoding))
			fileEncoding = Misc.DEFAULT_INPUT_STREAM_ENCODING;

		if(adapter == null && ( message == null && file == null )) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}
		
		if (StringUtils.isNotEmpty(message)) {
			try {
				PipeLineResult plr = processMessage(adapter, message, writeSecLogMessage);
				result.put("state", plr.getState());
				result.put("result", plr.getResult());
			} catch (Exception e) {
				throw new PipeRunException(this, getLogPrefix(null) + "exception on sending message", e);
			}
		}

		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	private String processZipFile(Map<String, Object> returnResult, InputStream inputStream, String fileEncoding, IAdapter adapter, boolean writeSecLogMessage) throws IOException {
		String result = "";
		String lastState = null;
		ZipInputStream archive = new ZipInputStream(inputStream);
		for (ZipEntry entry = archive.getNextEntry(); entry != null; entry = archive.getNextEntry()) {
			String name = entry.getName();
			int size = (int) entry.getSize();
			if (size > 0) {
				byte[] b = new byte[size];
				int rb = 0;
				int chunk = 0;
				while (((int) size - rb) > 0) {
					chunk = archive.read(b, rb, (int) size - rb);
					if (chunk == -1) {
						break;
					}
					rb += chunk;
				}
				String message = XmlUtils.readXml(b, 0, rb, fileEncoding, false);
				if (StringUtils.isNotEmpty(result)) {
					result += "\n";
				}
				lastState = processMessage(adapter, message, writeSecLogMessage).getState();
				result += name + ":" + lastState;
			}
			archive.closeEntry();
		}
		archive.close();
		returnResult.put("state", lastState);
		returnResult.put("result", result);
		return "";
	}

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
	
	private IbisManager getIbisManager() {
		String attributeKey = AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_CONTEXT);
		IbisContext ibisContext = (IbisContext) servletConfig.getServletContext().getAttribute(attributeKey);
		if (ibisContext != null) {
			IbisManager ibisManager = ibisContext.getIbisManager();
			if (ibisManager==null) {
				log.warn("Could not retrieve ibisManager from context");
			} else {
				log.debug("retrieved ibisManager ["+ClassUtils.nameOf(ibisManager)+"]["+ibisManager+"] from servlet context attribute ["+attributeKey+"]");
				return ibisManager;
			}
		}
		return null;
	}
}
