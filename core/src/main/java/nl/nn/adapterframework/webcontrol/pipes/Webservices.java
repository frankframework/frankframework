/*
   Copyright 2016 Nationale-Nederlanden

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

import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.http.RestListener;
import nl.nn.adapterframework.http.RestListenerUtils;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.soap.Wsdl;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Webservices.
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class Webservices extends TimeoutGuardPipe {

	public String doPipeWithTimeoutGuarded(Object input,
			IPipeLineSession session) throws PipeRunException {
		Configuration config = RestListenerUtils.retrieveConfiguration(session);

		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			ParameterResolutionContext prc = new ParameterResolutionContext(
					(String) input, session);
			try {
				pvl = prc.getValues(getParameterList());
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "exception on extracting parameters", e);
			}
		}

		String uri_work = getParameterValue(pvl, "uri");
		String indent_work = getParameterValue(pvl, "indent");
		String useIncludes_work = getParameterValue(pvl, "useIncludes");

		if (StringUtils.isNotEmpty(uri_work)
				&& (uri_work.endsWith(getWsdlExtention()) || uri_work
						.endsWith(".zip"))) {
			String adapterName = StringUtils.substringBeforeLast(
					StringUtils.substringAfterLast(uri_work, "/"), ".");
			IAdapter adapter = config.getRegisteredAdapter(adapterName);
			if (adapter == null) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "adapter [" + adapterName + "] doesn't exist");
			}
			try {
				if (uri_work.endsWith(getWsdlExtention())) {
					RestListenerUtils.setResponseContentType(session,
							"application/xml");
					wsdl((Adapter) adapter, session, indent_work,
							useIncludes_work);
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
			return list(config);
		}
	}

	private String list(Configuration config) {
		XmlBuilder webservicesXML = new XmlBuilder("webservices");

		XmlBuilder restsXML = new XmlBuilder("rests");
		for (IAdapter a : config.getRegisteredAdapters()) {
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

		XmlBuilder wsdlsXML = new XmlBuilder("wsdls");
		for (IAdapter a : config.getRegisteredAdapters()) {
			XmlBuilder wsdlXML = new XmlBuilder("wsdl");
			try {
				Adapter adapter = (Adapter) a;
				Wsdl wsdl = new Wsdl(adapter.getPipeLine());
				wsdlXML.addAttribute("name", wsdl.getName());
				wsdlXML.addAttribute("extention", getWsdlExtention());
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

		return webservicesXML.toXML();
	}

	private String getWsdlExtention() {
		return ".wsdl";
	}

	private void wsdl(Adapter adapter, IPipeLineSession session, String indent,
			String useIncludes) throws ConfigurationException,
			XMLStreamException, IOException, NamingException {
		Wsdl wsdl = new Wsdl(adapter.getPipeLine());
		if (indent != null) {
			wsdl.setIndent(StringUtils.equalsIgnoreCase(indent, "true"));
		}
		if (useIncludes != null) {
			wsdl.setUseIncludes(StringUtils.equalsIgnoreCase(useIncludes,
					"true"));
		}
		wsdl.setDocumentation(getDocumentation(wsdl, session));
		wsdl.init();
		wsdl.wsdl(RestListenerUtils.retrieveServletOutputStream(session),
				RestListenerUtils.retrieveRequestURL(session));
	}

	private void zip(Adapter adapter, IPipeLineSession session)
			throws ConfigurationException, XMLStreamException, IOException,
			NamingException {
		Wsdl wsdl = new Wsdl(adapter.getPipeLine());
		wsdl.setUseIncludes(true);
		wsdl.setDocumentation(getDocumentation(wsdl, session));
		wsdl.init();
		String requestURL = RestListenerUtils.retrieveRequestURL(session);
		wsdl.zip(RestListenerUtils.retrieveServletOutputStream(session),
				requestURL.substring(0, requestURL.lastIndexOf("."))
						+ getWsdlExtention());
	}

	private String getDocumentation(Wsdl wsdl, IPipeLineSession session)
			throws IOException {
		return "Generated at " + RestListenerUtils.retrieveRequestURL(session)
				+ " as " + wsdl.getFilename() + getWsdlExtention() + " on "
				+ DateUtils.getIsoTimeStamp() + ".";
	}
}
