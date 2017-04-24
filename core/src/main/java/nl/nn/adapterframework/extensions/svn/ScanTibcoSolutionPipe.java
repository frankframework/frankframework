/*
   Copyright 2015, 2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.svn;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPathExpressionException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe which scans TIBCO sources in Subversion and creates a report in xml.
 * 
 * @author Peter Leeuwenburgh
 */

public class ScanTibcoSolutionPipe extends FixedForwardPipe {

	private String url;
	private int level = 0;

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		StringWriter stringWriter = new StringWriter();
		XMLStreamWriter xmlStreamWriter;
		try {
			xmlStreamWriter = XmlUtils.OUTPUT_FACTORY
					.createXMLStreamWriter(stringWriter);
			xmlStreamWriter.writeStartDocument();
			xmlStreamWriter.writeStartElement("root");
			xmlStreamWriter.writeAttribute("url", getUrl());
			// xmlStreamWriter.writeAttribute("level",
			// String.valueOf(getLevel()));
			process(xmlStreamWriter, getUrl(), getLevel());
			xmlStreamWriter.writeEndDocument();
			xmlStreamWriter.flush();
			xmlStreamWriter.close();
		} catch (XMLStreamException e) {
			throw new PipeRunException(this, "XMLStreamException", e);
		} catch (DomBuilderException e) {
			throw new PipeRunException(this, "DomBuilderException", e);
		} catch (XPathExpressionException e) {
			throw new PipeRunException(this, "XPathExpressionException", e);
		}

		return new PipeRunResult(getForward(), stringWriter.getBuffer()
				.toString());
	}

	public void process(XMLStreamWriter xmlStreamWriter, String cUrl, int cLevel)
			throws XMLStreamException, DomBuilderException,
			XPathExpressionException {
		String html;
		try {
			html = getHtml(cUrl);
		} catch (Exception e) {
			error(xmlStreamWriter, "error occured during getting html", e, true);
			html = null;
		}
		if (html != null) {
			Collection<String> c = XmlUtils.evaluateXPathNodeSet(html,
					"html/body/ul/li/a/@href");
			if (c != null) {
				for (Iterator<String> it = c.iterator(); it.hasNext();) {
					String token = it.next();
					if (token.equals("../")) {
						// skip reference to parent directory
					} else if (cLevel == 0 && !token.equals("BW/")
							&& !token.equals("SOA/")) {
						skipDir(xmlStreamWriter, token);
						// } else if (cLevel == 1 &&
						// !token.startsWith("Customer")) {
						// skipDir(xmlStreamWriter, token);
					} else if (cLevel == 2
							&& (token.equals("branches/") || token
									.equals("tags/")) && c.contains("trunk/")) {
						skipDir(xmlStreamWriter, token);
					} else if (cLevel == 3 && !token.equals("src/")
							&& c.contains("src/") && !token.equals("release/")) {
						skipDir(xmlStreamWriter, token);
						// } else if (cLevel == 5 && token.endsWith("/")) {
						// skipDir(xmlStreamWriter, token);
					} else {
						String newUrl = cUrl + token;
						boolean dir = false;
						if (token.endsWith("/")) {
							dir = true;
						}
						if (dir) {
							xmlStreamWriter.writeStartElement("dir");
							xmlStreamWriter.writeAttribute("name",
									skipLastCharacter(token));
							// xmlStreamWriter.writeAttribute("level",
							// String.valueOf(cLevel + 1));
							if (cLevel == 1 || cLevel == 4) {
								addCommit(xmlStreamWriter, newUrl);
							}
							process(xmlStreamWriter, newUrl, cLevel + 1);
						} else {
							xmlStreamWriter.writeStartElement("file");
							xmlStreamWriter.writeAttribute("name", token);
							if (cLevel > 5) {
								if (token.endsWith(".jmsDest")) {
									addFileContent(xmlStreamWriter, newUrl,
											"jmsDest");
								}
								if (token.endsWith(".jmsDestConf")) {
									addFileContent(xmlStreamWriter, newUrl,
											"jmsDestConf");
								}
								if (token.endsWith(".composite")) {
									addFileContent(xmlStreamWriter, newUrl,
											"composite");
								}
								if (token.endsWith(".process")) {
									addFileContent(xmlStreamWriter, newUrl,
											"process");
								}
								if (token.equals("defaultVars.substvar")) {
									addFileContent(xmlStreamWriter, newUrl,
											"substVar");
								}
							}
						}
						xmlStreamWriter.writeEndElement();
					}
				}
			}
		}
	}

	private void skipDir(XMLStreamWriter xmlStreamWriter, String token)
			throws XMLStreamException {
		xmlStreamWriter.writeStartElement("dir");
		xmlStreamWriter.writeAttribute("name", skipLastCharacter(token));
		xmlStreamWriter.writeAttribute("skip", "true");
		xmlStreamWriter.writeEndElement();
	}

	private String skipLastCharacter(String str) {
		return StringUtils.left(str, str.length() - 1);
	}

	private void addCommit(XMLStreamWriter xmlStreamWriter, String urlString)
			throws XMLStreamException {
		xmlStreamWriter.writeStartElement("commit");
		try {
			String logReport = SvnUtils.getLogReport(urlString);
			String creator = XmlUtils.evaluateXPathNodeSetFirstElement(
					logReport, "log-report/log-item/creator-displayname");
			xmlStreamWriter.writeAttribute("creator", creator);
			String date = XmlUtils.evaluateXPathNodeSetFirstElement(logReport,
					"log-report/log-item/date");
			xmlStreamWriter.writeAttribute("date", date);
		} catch (Exception e) {
			error(xmlStreamWriter, "error occured during adding commit info",
					e, false);
		}
		xmlStreamWriter.writeEndElement();
	}

	private void addFileContent(XMLStreamWriter xmlStreamWriter,
			String urlString, String type) throws XMLStreamException {
		xmlStreamWriter.writeStartElement("content");
		xmlStreamWriter.writeAttribute("type", type);
		String content;
		try {
			content = getHtml(urlString);
		} catch (Exception e) {
			error(xmlStreamWriter, "error occured during getting file content",
					e, true);
			content = null;
		}
		if (content != null) {
			Vector<String> warnMessage = new Vector<String>();
			try {
				if (type.equals("jmsDest") || type.equals("jmsDestConf")) {
					// AMX - receive (for jmsInboundDest)
					Collection<String> c1 = XmlUtils.evaluateXPathNodeSet(
							content, "namedResource/@name");
					if (c1 != null && c1.size() > 0) {
						if (c1.size() > 1) {
							warnMessage.add("more then one resourceName found");
						}
						String resourceName = (String) c1.iterator().next();
						xmlStreamWriter.writeStartElement("resourceName");
						xmlStreamWriter.writeCharacters(resourceName);
						xmlStreamWriter.writeEndElement();
					} else {
						warnMessage.add("no resourceName found");
					}
					Collection<String> c2 = XmlUtils.evaluateXPathNodeSet(
							content, "namedResource/configuration/@jndiName");
					if (c2 != null && c2.size() > 0) {
						if (c2.size() > 1) {
							warnMessage
									.add("more then one resourceJndiName found");
						}
						String resourceJndiName = (String) c2.iterator().next();
						xmlStreamWriter.writeStartElement("resourceJndiName");
						xmlStreamWriter.writeCharacters(resourceJndiName);
						xmlStreamWriter.writeEndElement();
					} else {
						warnMessage.add("no resourceJndiName found");
					}
				} else if (type.equals("composite")) {
					// AMX - receive
					Collection<String> c1 = XmlUtils
							.evaluateXPathNodeSet(
									content,
									"composite/service/bindingAdjunct/property[@name='JmsInboundDestinationConfig']/@simpleValue");
					if (c1 != null && c1.size() > 0) {
						for (Iterator<String> c1it = c1.iterator(); c1it
								.hasNext();) {
							xmlStreamWriter.writeStartElement("jmsInboundDest");
							xmlStreamWriter.writeCharacters(c1it.next());
							xmlStreamWriter.writeEndElement();
						}
					} else {
						warnMessage.add("no jmsInboundDest found");
					}
					// AMX - send
					Collection<String> c2 = XmlUtils.evaluateXPathNodeSet(
							content,
							"composite/reference/interface.wsdl/@wsdlLocation");
					if (c2 != null && c2.size() > 0) {
						for (Iterator<String> c2it = c2.iterator(); c2it
								.hasNext();) {
							String itn = c2it.next();
							String wsdl = null;
							try {
								URL url = new URL(urlString);
								URL wsdlUrl = new URL(url, itn);
								wsdl = getHtml(wsdlUrl.toString());
							} catch (Exception e) {
								error(xmlStreamWriter,
										"error occured during getting wsdl file content",
										e, true);
								wsdl = null;
							}
							if (wsdl != null) {
								Collection<String> c3 = XmlUtils
										.evaluateXPathNodeSet(wsdl,
										// "definitions/service/port/targetAddress",
										// "concat(.,';',../../@name)");
												"definitions/service/port/targetAddress");
								if (c3 != null && c3.size() > 0) {
									for (Iterator<String> c3it = c3.iterator(); c3it
											.hasNext();) {
										xmlStreamWriter
												.writeStartElement("targetAddr");
										xmlStreamWriter.writeCharacters(c3it
												.next());
										xmlStreamWriter.writeEndElement();
									}
								} else {
									warnMessage.add("no targetAddr found");
								}
							} else {
								warnMessage.add("wsdl [" + itn + "] not found");
							}
						}
					} else {
						warnMessage.add("no wsdlLocation found");
					}
				} else if (type.equals("process")) {
					// BW - receive
					Double d1 = XmlUtils
							.evaluateXPathNumber(
									content,
									"count(ProcessDefinition/starter[type='com.tibco.plugin.soap.SOAPEventSource']/config)");
					if (d1 > 0) {
						Collection<String> c1 = XmlUtils
								.evaluateXPathNodeSet(
										content,
										"ProcessDefinition/starter[type='com.tibco.plugin.soap.SOAPEventSource']/config/sharedChannels/jmsChannel/JMSTo");
						if (c1 != null && c1.size() > 0) {
							for (Iterator<String> c1it = c1.iterator(); c1it
									.hasNext();) {
								xmlStreamWriter.writeStartElement("jmsTo");
								xmlStreamWriter.writeAttribute("type",
										"soapEventSource");
								xmlStreamWriter.writeCharacters(c1it.next());
								xmlStreamWriter.writeEndElement();
							}
						} else {
							warnMessage
									.add("no jmsTo found for soapEventSource");
						}
					} else {
						warnMessage.add("no soapEventSource found");
					}
					// BW - send
					Double d2 = XmlUtils
							.evaluateXPathNumber(
									content,
									"count(ProcessDefinition/activity[type='com.tibco.plugin.soap.SOAPSendReceiveActivity']/config)");
					if (d2 > 0) {
						Collection<String> c2 = XmlUtils
								.evaluateXPathNodeSet(
										content,
										"ProcessDefinition/activity[type='com.tibco.plugin.soap.SOAPSendReceiveActivity']/config/sharedChannels/jmsChannel/JMSTo");
						if (c2 != null && c2.size() > 0) {
							for (Iterator<String> c2it = c2.iterator(); c2it
									.hasNext();) {
								xmlStreamWriter.writeStartElement("jmsTo");
								xmlStreamWriter.writeAttribute("type",
										"soapSendReceiveActivity");
								xmlStreamWriter.writeCharacters(c2it.next());
								xmlStreamWriter.writeEndElement();
							}
						} else {
							warnMessage
									.add("no jmsTo found for soapSendReceiveActivity");
						}
					} else {
						warnMessage.add("no soapSendReceiveActivity found");
					}
				} else if (type.equals("substVar")) {
					String path = StringUtils.substringBeforeLast(StringUtils
							.substringAfterLast(urlString, "/defaultVars/"),
							"/");
					Map<String, String> m1 = XmlUtils.evaluateXPathNodeSet(
							content,
							"repository/globalVariables/globalVariable",
							"name", "value");
					if (m1 != null && m1.size() > 0) {
						for (Iterator<String> m1it = m1.keySet().iterator(); m1it
								.hasNext();) {
							Object key = m1it.next();
							Object value = m1.get(key);
							xmlStreamWriter.writeStartElement("globalVariable");
							xmlStreamWriter
									.writeAttribute("name", (String) key);
							xmlStreamWriter.writeAttribute("ref", "%%" + path
									+ "/" + key + "%%");
							xmlStreamWriter.writeCharacters((String) value);
							xmlStreamWriter.writeEndElement();
						}
					} else {
						warnMessage.add("no globalVariable found");
					}
					/*
					 * } else { content = XmlUtils.removeNamespaces(content);
					 * xmlStreamWriter.writeCharacters(content);
					 */
				}
			} catch (Exception e) {
				error(xmlStreamWriter, "error occured during processing "
						+ type + " file", e, true);
			}
			if (warnMessage.size() > 0) {
				xmlStreamWriter.writeStartElement("warnMessages");
				for (int i = 0; i < warnMessage.size(); i++) {
					xmlStreamWriter.writeStartElement("warnMessage");
					xmlStreamWriter.writeCharacters(warnMessage.elementAt(i));
					xmlStreamWriter.writeEndElement();
				}
				xmlStreamWriter.writeEndElement();
			}
		}
		xmlStreamWriter.writeEndElement();
	}

	private void error(XMLStreamWriter xmlStreamWriter, String msg,
			Throwable t, boolean printStackTrace) throws XMLStreamException {
		log.warn(msg, t);
		xmlStreamWriter.writeStartElement("errorMessage");
		String errorMsg;
		if (printStackTrace) {
			StringWriter trace = new StringWriter();
			t.printStackTrace(new PrintWriter(trace));
			errorMsg = msg + ": " + trace;
		} else {
			errorMsg = msg + ": " + t.getMessage();
		}
		xmlStreamWriter.writeCharacters(errorMsg);
		xmlStreamWriter.writeEndElement();
	}

	private String getHtml(String urlString) throws ConfigurationException,
			SenderException, TimeOutException {
		HttpSender httpSender = null;
		try {
			httpSender = new HttpSender();
			httpSender.setUrl(urlString);
			httpSender.setAllowSelfSignedCertificates(true);
			httpSender.setVerifyHostname(false);
			httpSender.setIgnoreCertificateExpiredException(true);
			httpSender.setXhtml(true);
			httpSender.configure();
			httpSender.open();
			String result = httpSender.sendMessage(null, "");
			return result;
		} finally {
			if (httpSender != null) {
				httpSender.close();
			}
		}
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String s) {
		url = s;
	}

	public void setLevel(int i) {
		level = i;
	}

	public int getLevel() {
		return level;
	}
}