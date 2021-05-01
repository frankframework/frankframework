/*
   Copyright 2016-2018, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Principal;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.lifecycle.IbisApplicationServlet;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Some utilities for working with
 * {@link nl.nn.adapterframework.http.RestListener RestListener}.
 * 
 * @author Peter Leeuwenburgh
 */
public class RestListenerUtils {
	private static final String SHOW_CONFIG_STATUS_CONFIGURATION = "IAF_WebControl";
	private static final String SHOW_CONFIG_STATUS_ADAPTER = "WebControlShowConfigurationStatus";
	private static final String SHOW_CONFIG_STATUS_RECEIVER = "WebControlShowConfigurationStatus";

	protected static Logger log = LogUtil.getLogger(RestListenerUtils.class);

	public static IbisManager retrieveIbisManager(PipeLineSession session) {
		ServletContext servletContext = (ServletContext) session.get(PipeLineSession.SERVLET_CONTEXT_KEY);
		if (servletContext != null) {
			IbisContext ibisContext = IbisApplicationServlet.getIbisContext(servletContext);
			return ibisContext.getIbisManager();
		}

		return null;
	}

	public static ServletOutputStream retrieveServletOutputStream(PipeLineSession session) throws IOException {
		HttpServletResponse response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);
		if (response != null) {
			return response.getOutputStream();
		}
		return null;
	}

	public static String retrieveRequestURL(PipeLineSession session) throws IOException {
		HttpServletRequest request = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
		if (request != null) {
			return request.getRequestURL().toString();
		}
		return null;
	}

	public static String retrieveSOAPRequestURL(PipeLineSession session) throws IOException {
		HttpServletRequest request = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
		if (request != null) {
			String url = request.getScheme() + "://" + request.getServerName();
			if(!(request.getScheme().equalsIgnoreCase("http") && request.getServerPort() == 80) && !(request.getScheme().equalsIgnoreCase("https") && request.getServerPort() == 443))
				url += ":" + request.getServerPort();
			url += request.getContextPath() + "/services/";
			return url;
		}
		return null;
	}

	public static void writeToResponseOutputStream(PipeLineSession session, byte[] bytes) throws IOException {
		retrieveServletOutputStream(session).write(bytes);
	}

	public static void writeToResponseOutputStream(PipeLineSession session, InputStream input) throws IOException {
		OutputStream output = retrieveServletOutputStream(session);
		StreamUtil.copyStream(input, output, 30000);
	}

	public static void setResponseContentType(PipeLineSession session, String contentType) throws IOException {
		HttpServletResponse response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);
		if (response != null) {
			response.setContentType(contentType);
		}
	}

	public static String retrieveRequestRemoteUser(PipeLineSession session) throws IOException {
		HttpServletRequest request = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
		if (request != null) {
			Principal principal = request.getUserPrincipal();
			if (principal != null) {
				return principal.getName();
			}
		}
		return null;
	}

	public static String formatEtag(String restPath, String uriPattern, int hash) {
		return formatEtag(restPath, uriPattern, ""+hash );
	}

	public static String formatEtag(String restPath, String uriPattern, String hash) {
		return Integer.toOctalString(restPath.hashCode()) + "_" +Integer.toHexString(uriPattern.hashCode()) + "_" + hash;
	}

	public static boolean restartShowConfigurationStatus(ServletContext servletContext) {
		// it's possible the adapter and/or receiver wasn't stopped completely
		// yet, so it couldn't be restarted
		try {
			IbisContext ibisContext = IbisApplicationServlet.getIbisContext(servletContext);

			int maxTries = 3;
			while (maxTries-- > 0) {
				try {
					boolean restarted = doRestartShowConfigurationStatus(ibisContext);
					if (restarted) {
						return true;
					}
					Thread.sleep(1000);
				} catch (InterruptedException ignore) {
				}
			}
			return false;
		} catch (IllegalStateException ignore) {
			//The ibis failed to start up, abort straight away!
			return false;
		}
	}

	private static boolean doRestartShowConfigurationStatus(IbisContext ibisContext) {

		Adapter adapter = null;
		Receiver receiver = null;
		if (ibisContext != null) {
			IbisManager ibisManager = ibisContext.getIbisManager();
			if (ibisManager != null) {
				Configuration configuration = ibisManager.getConfiguration(SHOW_CONFIG_STATUS_CONFIGURATION);
				if (configuration != null) {
					adapter = configuration.getRegisteredAdapter(SHOW_CONFIG_STATUS_ADAPTER);
					receiver = adapter.getReceiverByNameAndListener(SHOW_CONFIG_STATUS_RECEIVER, RestListener.class);
				}
			}
		}

		if (adapter == null) {
			log.info("could not restart ShowConfigurationStatus, adapter [" + SHOW_CONFIG_STATUS_ADAPTER + "] not found");
			return false;
		}
		if (receiver == null) {
			log.info("could not restart ShowConfigurationStatus, receiver [" + SHOW_CONFIG_STATUS_RECEIVER + "] not found");
			return false;
		}

		RunStateEnum adapterStatus = adapter.getRunState();
		RunStateEnum receiverStatus = receiver.getRunState();

		if (RunStateEnum.STARTED.equals(adapterStatus) && RunStateEnum.STARTED.equals(receiverStatus)) {
			log.info("ShowConfigurationStatus is already running, will restart it");
			ibisContext.getIbisManager().handleAdapter("stopadapter", SHOW_CONFIG_STATUS_CONFIGURATION, SHOW_CONFIG_STATUS_ADAPTER, SHOW_CONFIG_STATUS_RECEIVER, "system", true);
		}

		if (RunStateEnum.STOPPED.equals(adapterStatus)) {
			log.info("starting adapter of ShowConfigurationStatus");
			ibisContext.getIbisManager().handleAdapter("startadapter", SHOW_CONFIG_STATUS_CONFIGURATION, SHOW_CONFIG_STATUS_ADAPTER, SHOW_CONFIG_STATUS_RECEIVER, "system", true);
			return true;
		} else {
			if (RunStateEnum.STARTED.equals(adapterStatus) && RunStateEnum.STOPPED.equals(receiverStatus)) {
				log.info("starting receiver of ShowConfigurationStatus");
				ibisContext.getIbisManager().handleAdapter("startreceiver", SHOW_CONFIG_STATUS_CONFIGURATION, SHOW_CONFIG_STATUS_ADAPTER, SHOW_CONFIG_STATUS_RECEIVER, "system", true);
				return true;
			}
		}
		log.info("could not restart ShowConfigurationStatus with adapter status [" + adapterStatus + "] and receiver status [" + receiverStatus + "]");
		return false;
	}
}
