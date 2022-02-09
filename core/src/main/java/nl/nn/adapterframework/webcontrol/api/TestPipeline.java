/*
Copyright 2016-2017, 2020-2022 WeAreFrank!

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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.Transformer;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
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
public class TestPipeline extends Base {

	protected Logger secLog = LogUtil.getLogger("SEC");
	private boolean secLogMessage = AppConstants.getInstance().getBoolean("sec.log.includeMessage", false);

	public static final String PIPELINE_RESULT_STATE_ERROR="ERROR";
	public static final String PIPELINE_RESULT_STATE="state";
	public static final String PIPELINE_RESULT="result";

	@Data
	public static class PostedSessionKey {
		int index;
		String key;
		String value;
	}

	@POST
	@RolesAllowed("IbisTester")
	@Path("/test-pipeline")
	@Relation("pipeline")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response postTestPipeLine(MultipartBody inputDataMap) throws ApiException {
		Map<String, Object> result = new HashMap<>();

		IbisManager ibisManager = getIbisManager();
		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}

		String message = null;
		InputStream file = null;
		boolean isZipFile = false;

		String adapterName = resolveStringFromMap(inputDataMap, "adapter");
		//Make sure the adapter exists!
		IAdapter adapter = ibisManager.getRegisteredAdapter(adapterName);
		if(adapter == null) {
			throw new ApiException("Adapter ["+adapterName+"] not found");
		}
		// resolve session keys
		String sessionKeys = resolveTypeFromMap(inputDataMap, "sessionKeys", String.class, "");
		Map<String, String> sessionKeyMap = null;
		if(StringUtils.isNotEmpty(sessionKeys)) {
			try {
				sessionKeyMap = Stream.of(new ObjectMapper().readValue(sessionKeys, PostedSessionKey[].class))
						.collect(Collectors.toMap(item -> item.key, item-> item.value));
			} catch (Exception e) {
				throw new ApiException("An exception occurred while parsing session keys", e);
			}
		}

		String fileEncoding = resolveTypeFromMap(inputDataMap, "encoding", String.class, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);

		Attachment filePart = inputDataMap.getAttachment("file");
		if(filePart != null) {
			String fileName = filePart.getContentDisposition().getParameter( "filename" );

			if (StringUtils.endsWithIgnoreCase(fileName, ".zip")) {
				try {
					isZipFile = true;
					file = filePart.getObject(InputStream.class);
					processZipFile(result, file, fileEncoding, adapter, sessionKeyMap, secLogMessage);
				} catch (Exception e) {
					throw new ApiException("An exception occurred while processing zip file", e);
				}
			} else {
				message = resolveStringWithEncoding(inputDataMap, "file", fileEncoding);
			}
		} else {
			message = resolveStringWithEncoding(inputDataMap, "message", fileEncoding);
		}
		if(!isZipFile) {
			result.put("message", message);
			try {
				PipeLineResult plr = processMessage(adapter, message, sessionKeyMap, secLogMessage);
				try {
					result.put(PIPELINE_RESULT_STATE, plr.getState());
					result.put(PIPELINE_RESULT, plr.getResult().asString());
				} catch (Exception e) {
					String msg = "An exception occurred while extracting the result of the PipeLine with exit state ["+plr.getState()+"]"; 
					log.warn(msg, e);
					result.put(PIPELINE_RESULT_STATE, PIPELINE_RESULT_STATE_ERROR);
					result.put(PIPELINE_RESULT, msg+": ("+e.getClass().getTypeName()+") "+e.getMessage());
				}
			} catch (Exception e) {
				String msg = "An exception occurred while processing the message"; 
				log.warn(msg, e);
				result.put(PIPELINE_RESULT_STATE, PIPELINE_RESULT_STATE_ERROR);
				result.put(PIPELINE_RESULT, msg + ": ("+e.getClass().getTypeName()+") "+e.getMessage());
			}
		}

		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	private void processZipFile(Map<String, Object> returnResult, InputStream inputStream, String fileEncoding, IAdapter adapter, Map<String, String> sessionKeyMap, boolean writeSecLogMessage) throws IOException {
		StringBuilder result = new StringBuilder();
		ExitState lastState = null;
		try (ZipInputStream archive = new ZipInputStream(inputStream)) {
			for (ZipEntry entry = archive.getNextEntry(); entry != null; entry = archive.getNextEntry()) {
				String name = entry.getName();
				byte[] contentBytes = StreamUtil.streamToByteArray(StreamUtil.dontClose(archive), true);
				String message = XmlUtils.readXml(contentBytes, fileEncoding, false);
				if (result.length() > 0) {
					result.append("\n");
				}
				lastState = processMessage(adapter, message, sessionKeyMap, writeSecLogMessage).getState();
				result.append(name + ":" + lastState);
				archive.closeEntry();
			}
		}
		returnResult.put(PIPELINE_RESULT_STATE, lastState);
		returnResult.put(PIPELINE_RESULT, result);
	}

	@SuppressWarnings("rawtypes")
	private PipeLineResult processMessage(IAdapter adapter, String message, Map<String, String> sessionKeyMap, boolean writeSecLogMessage) {
		String messageId = "testmessage" + Misc.createSimpleUUID();
		try (PipeLineSession pls = new PipeLineSession()) {
			if(sessionKeyMap != null) {
				pls.putAll(sessionKeyMap);
			}
			Map ibisContexts = getIbisContext(message);
			String technicalCorrelationId = null;
			if (ibisContexts != null) {
				String contextDump = "ibisContext:";
				for (Iterator it = ibisContexts.keySet().iterator(); it.hasNext();) {
					String key = (String) it.next();
					String value = (String) ibisContexts.get(key);
					if (log.isDebugEnabled()) {
						contextDump = contextDump + "\n " + key + "=[" + value + "]";
					}
					if (key.equals(PipeLineSession.technicalCorrelationIdKey)) {
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
			PipeLineSession.setListenerParameters(pls, messageId, technicalCorrelationId, now, now);

			secLog.info(String.format("testing pipeline of adapter [%s] %s", adapter.getName(), (writeSecLogMessage ? "message [" + message + "]" : "")));

			PipeLineResult plr = adapter.processMessage(messageId, new Message(message), pls);
			plr.getResult().unscheduleFromCloseOnExitOf(pls);
			return plr;
		}
	}
	/**
	 * Parses the 'ibisContext' processing instruction defined in the input
	 * @return key value pair map
	 */
	protected Map<String, String> getIbisContext(String input) {
		if (StringUtils.isEmpty(input) || (!input.startsWith("<?") && !input.startsWith("<!"))) {
			return null;
		}
		if (XmlUtils.isWellFormed(input)) {
			String getIbisContext_xslt = makeGetIbisContextXslt();
			try {
				Transformer t = XmlUtils.createTransformer(getIbisContext_xslt);
				String str = XmlUtils.transformXml(t, input);
				Map<String, String> ibisContexts = new LinkedHashMap<>();
				int indexBraceOpen = str.indexOf("{");
				int indexBraceClose = 0;
				int indexStartNextSearch = 0;
				while (indexBraceOpen >= 0) {
					indexBraceClose = str.indexOf("}",indexBraceOpen+1);
					if (indexBraceClose > indexBraceOpen) {
						String ibisContextLength = str.substring(indexBraceOpen+1, indexBraceClose);
						int icLength = Integer.parseInt(ibisContextLength);
						if (icLength > 0) {
							indexStartNextSearch = indexBraceClose + 1 + icLength;
							String ibisContext = str.substring(indexBraceClose+1, indexStartNextSearch);
							int indexEqualSign = ibisContext.indexOf("=");
							String key;
							String value;
							if (indexEqualSign < 0) {
								key = ibisContext;
								value = "";
							} else {
								key = ibisContext.substring(0,indexEqualSign);
								value = ibisContext.substring(indexEqualSign+1);
							}
							ibisContexts.put(key, value);
						} else {
							indexStartNextSearch = indexBraceClose + 1;
						}
					} else {
						indexStartNextSearch = indexBraceOpen + 1;
					}
					indexBraceOpen = str.indexOf("{",indexStartNextSearch);
				}
				return ibisContexts;
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
		}
	}

	private String makeGetIbisContextXslt() {
		return
		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\" version=\"1.0\">"
			+ "<xsl:output method=\"text\"/>"
			+ "<xsl:template match=\"/\">"
			+ "<xsl:for-each select=\"processing-instruction('ibiscontext')\">"
			+ "<xsl:variable name=\"ic\" select=\"normalize-space(.)\"/>"
			+ "<xsl:text>{</xsl:text>"
			+ "<xsl:value-of select=\"string-length($ic)\"/>"
			+ "<xsl:text>}</xsl:text>"
			+ "<xsl:value-of select=\"$ic\"/>"
			+ "</xsl:for-each>"
			+ "</xsl:template>"
			+ "</xsl:stylesheet>";
	}
}
