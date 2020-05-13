/*
   Copyright 2016, 2020 Nationale-Nederlanden

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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.http.rest.ApiDispatchConfig;
import nl.nn.adapterframework.http.rest.ApiListener;
import nl.nn.adapterframework.http.rest.ApiServiceDispatcher;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.soap.Wsdl;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;

/**
 * Webservices.
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class Webservices extends TimeoutGuardPipe {

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message input, IPipeLineSession session) throws PipeRunException {
		String method = (String) session.get("method");
		if (method.equalsIgnoreCase("GET")) {
			return new PipeRunResult(getForward(), doGet(session));
		} else {
			throw new PipeRunException(this, getLogPrefix(session) + "illegal value for method [" + method + "], must be 'GET'");
		}
	}

	private String doGet(IPipeLineSession session) throws PipeRunException {
		IbisManager ibisManager = RestListenerUtils.retrieveIbisManager(session);

		String uri = (String) session.get("uri");
		String indent = (String) session.get("indent");
		String useIncludes = (String) session.get("useIncludes");

		if (StringUtils.isNotEmpty(uri)
				&& (uri.endsWith(Wsdl.WSDL_EXTENSION) || uri.endsWith(".zip"))) {
			String adapterName = StringUtils.substringBeforeLast(
					StringUtils.substringAfterLast(uri, "/"), ".");
			IAdapter adapter = ibisManager.getRegisteredAdapter(adapterName);
			if (adapter == null) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "adapter [" + adapterName + "] doesn't exist");
			}
			try {
				if (uri.endsWith(Wsdl.WSDL_EXTENSION)) {
					RestListenerUtils.setResponseContentType(session,
							"application/xml");
					wsdl((Adapter) adapter, session, indent, useIncludes);
				} else {
					RestListenerUtils.setResponseContentType(session,
							"application/octet-stream");
					zip((Adapter) adapter, session);
				}

			} catch (Exception e) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "exception on retrieving wsdl", e);
			}
			return "";
		} else {
			return list(ibisManager, session);
		}
	}

	private String list(IbisManager ibisManager, IPipeLineSession session) {
		XmlBuilder webservicesXML = new XmlBuilder("webservices");

		//RestListeners
		XmlBuilder restsXML = new XmlBuilder("rests");
		for (IAdapter a : ibisManager.getRegisteredAdapters()) {
			Adapter adapter = (Adapter) a;
			Iterator recIt = adapter.getReceiverIterator();
			while (recIt.hasNext()) {
				IReceiver receiver = (IReceiver) recIt.next();
				if (receiver instanceof ReceiverBase) {
					ReceiverBase rb = (ReceiverBase) receiver;
					IListener listener = rb.getListener();
					if (listener instanceof RestListener) {
						RestListener rl = (RestListener) listener;
						if (rl.isView()) {
							XmlBuilder restXML = new XmlBuilder("rest");
							restXML.addAttribute("name", rb.getName());
							restXML.addAttribute("uriPattern",
									rl.getUriPattern());
							restsXML.addSubElement(restXML);
						}
					}
				}
			}
		}
		webservicesXML.addSubElement(restsXML);

		//WSDL's
		XmlBuilder wsdlsXML = new XmlBuilder("wsdls");
		for (IAdapter a : ibisManager.getRegisteredAdapters()) {
			XmlBuilder wsdlXML = new XmlBuilder("wsdl");
			try {
				Adapter adapter = (Adapter) a;
				Wsdl wsdl = new Wsdl(adapter.getPipeLine(), retrieveGenerationInfo(session));
				wsdlXML.addAttribute("name", wsdl.getName());
				wsdlXML.addAttribute("extention", Wsdl.WSDL_EXTENSION);
			} catch (Exception e) {
				wsdlXML.addAttribute("name", a.getName());
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

	private String retrieveGenerationInfo(IPipeLineSession session) throws IOException {
		return "at " + RestListenerUtils.retrieveRequestURL(session);
	}
	
	private void wsdl(Adapter adapter, IPipeLineSession session, String indent,
			String useIncludes) throws ConfigurationException,
			XMLStreamException, IOException, NamingException {
		Wsdl wsdl = new Wsdl(adapter.getPipeLine(), retrieveGenerationInfo(session));
		if (indent != null) {
			wsdl.setIndent(StringUtils.equalsIgnoreCase(indent, "true"));
		}
		if (useIncludes != null) {
			wsdl.setUseIncludes(StringUtils.equalsIgnoreCase(useIncludes,
					"true"));
		}
		wsdl.init();
		wsdl.wsdl(RestListenerUtils.retrieveServletOutputStream(session),
				RestListenerUtils.retrieveSOAPRequestURL(session));
	}

	private void zip(Adapter adapter, IPipeLineSession session)
			throws ConfigurationException, XMLStreamException, IOException,
			NamingException {
		Wsdl wsdl = new Wsdl(adapter.getPipeLine(), retrieveGenerationInfo(session));
		wsdl.setUseIncludes(true);
		wsdl.init();
		wsdl.zip(RestListenerUtils.retrieveServletOutputStream(session),
				RestListenerUtils.retrieveSOAPRequestURL(session));
	}
}
