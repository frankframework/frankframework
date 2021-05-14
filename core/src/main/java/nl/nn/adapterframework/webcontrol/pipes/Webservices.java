/*
   Copyright 2016, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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

import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.http.rest.ApiDispatchConfig;
import nl.nn.adapterframework.http.rest.ApiListener;
import nl.nn.adapterframework.http.rest.ApiServiceDispatcher;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.soap.WsdlGenerator;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Webservices.
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class Webservices extends TimeoutGuardPipe {

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message input, PipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return new PipeRunResult(getSuccessForward(), doGet(session));
		} else {
			throw new PipeRunException(this, getLogPrefix(session) + "illegal value for method [" + method + "], must be 'GET'");
		}
	}

	private String doGet(PipeLineSession session) throws PipeRunException {
		IbisManager ibisManager = RestListenerUtils.retrieveIbisManager(session);

		String uri = (String) session.get("uri");
		String indent = (String) session.get("indent");
		String useIncludes = (String) session.get("useIncludes");

		if (StringUtils.isNotEmpty(uri) && (uri.endsWith(WsdlGenerator.WSDL_EXTENSION) || uri.endsWith(".zip"))) {
			String adapterName = StringUtils.substringBeforeLast(StringUtils.substringAfterLast(uri, "/"), ".");
			Adapter adapter = ibisManager.getRegisteredAdapter(adapterName);
			if (adapter == null) {
				throw new PipeRunException(this, getLogPrefix(session) + "adapter [" + adapterName + "] doesn't exist");
			}
			try {
				if (uri.endsWith(WsdlGenerator.WSDL_EXTENSION)) {
					RestListenerUtils.setResponseContentType(session, "application/xml");
					wsdl(adapter, session, indent, useIncludes);
				} else {
					RestListenerUtils.setResponseContentType(session, "application/octet-stream");
					zip(adapter, session);
				}

			} catch (Exception e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception on retrieving wsdl", e);
			}
			return "";
		} else {
			return list(ibisManager, session);
		}
	}

	private String list(IbisManager ibisManager, PipeLineSession session) {
		XmlBuilder webservicesXML = new XmlBuilder("webservices");

		//RestListeners
		XmlBuilder restsXML = new XmlBuilder("rests");
		for (Adapter adapter : ibisManager.getRegisteredAdapters()) {
			for (Receiver receiver: adapter.getReceivers()) {
				IListener listener = receiver.getListener();
				if (listener instanceof RestListener) {
					RestListener rl = (RestListener) listener;
					if (rl.getView()) {
						XmlBuilder restXML = new XmlBuilder("rest");
						restXML.addAttribute("name", receiver.getName());
						restXML.addAttribute("uriPattern", rl.getUriPattern());
						restsXML.addSubElement(restXML);
					}
				}
			}
		}
		webservicesXML.addSubElement(restsXML);

		//WSDL's
		XmlBuilder wsdlsXML = new XmlBuilder("wsdls");
		for (Adapter adapter : ibisManager.getRegisteredAdapters()) {
			XmlBuilder wsdlXML = new XmlBuilder("wsdl");
			try {
				WsdlGenerator wsdl = new WsdlGenerator(adapter.getPipeLine(), retrieveGenerationInfo(session));
				wsdlXML.addAttribute("name", wsdl.getName());
				wsdlXML.addAttribute("extension", WsdlGenerator.WSDL_EXTENSION);
			} catch (Exception e) {
				wsdlXML.addAttribute("name", adapter.getName());
				XmlBuilder errorXML = new XmlBuilder("error");
				if (e.getMessage() != null) {
					errorXML.setCdataValue(e.getMessage());
				} else {
					errorXML.setCdataValue(e.toString());
				}
				wsdlXML.addSubElement(errorXML);
			}
			wsdlsXML.addSubElement(wsdlXML);
		}
		webservicesXML.addSubElement(wsdlsXML);

		//ApiListeners
		XmlBuilder apiListenerXML = new XmlBuilder("apiListeners");
		SortedMap<String, ApiDispatchConfig> patternClients = ApiServiceDispatcher.getInstance().getPatternClients();
		for (Entry<String, ApiDispatchConfig> client : patternClients.entrySet()) {
			XmlBuilder apiXML = new XmlBuilder("apiListener");
			ApiDispatchConfig config = client.getValue();

			Set<String> methods = config.getMethods();
			for (String method : methods) {
				ApiListener listener = config.getApiListener(method);
				XmlBuilder methodXML = new XmlBuilder(method);
				String name = listener.getName();
				if(name.contains("listener of ["))
					name = name.substring(13, name.length()-1);
				methodXML.addAttribute("name", name);
				methodXML.addAttribute("updateEtag", listener.getUpdateEtag());
				methodXML.addAttribute("isRunning", listener.isRunning());
				apiXML.addSubElement(methodXML);
			}

			apiXML.addAttribute("uriPattern", config.getUriPattern());
			apiListenerXML.addSubElement(apiXML);
		}
		webservicesXML.addSubElement(apiListenerXML);

		return webservicesXML.toXML();
	}

	private String retrieveGenerationInfo(PipeLineSession session) throws IOException {
		return "at " + RestListenerUtils.retrieveRequestURL(session);
	}
	
	private void wsdl(Adapter adapter, PipeLineSession session, String indent, String useIncludes) throws ConfigurationException, XMLStreamException, IOException, NamingException {
		WsdlGenerator wsdl = new WsdlGenerator(adapter.getPipeLine(), retrieveGenerationInfo(session));
		if (indent != null) {
			wsdl.setIndent(StringUtils.equalsIgnoreCase(indent, "true"));
		}
		if (useIncludes != null) {
			wsdl.setUseIncludes(StringUtils.equalsIgnoreCase(useIncludes, "true"));
		}
		wsdl.init();
		wsdl.wsdl(RestListenerUtils.retrieveServletOutputStream(session), RestListenerUtils.retrieveSOAPRequestURL(session));
	}

	private void zip(Adapter adapter, PipeLineSession session) throws ConfigurationException, XMLStreamException, IOException, NamingException {
		WsdlGenerator wsdl = new WsdlGenerator(adapter.getPipeLine(), retrieveGenerationInfo(session));
		wsdl.setUseIncludes(true);
		wsdl.init();
		wsdl.zip(RestListenerUtils.retrieveServletOutputStream(session), RestListenerUtils.retrieveSOAPRequestURL(session));
	}
}
