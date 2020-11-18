/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.logging;

import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 * Implementation of {@link IbisMaskingLayout} to serialize given log events 
 * in the following format: <br/>
 * <pre> {@code
 * <log4j:event timestamp="1605705678703" thread="thread-1">
 * <log4j:configuration>my message here</log4j:configuration>
 * </log4j:event>
 * } </pre>
 */
@Plugin(name = "IbisConfigurationXmlLayout", category = "Core", elementType = "layout", printObject = true)
public class IbisConfigurationXmlLayout extends IbisMaskingLayout {

	protected IbisConfigurationXmlLayout(final Configuration config, final Charset charset) {
		super(config, charset);
	}

	@Override
	protected String serializeEvent(LogEvent event) {
		XmlBuilder eventBuilder = XmlBuilder.create("event");
		eventBuilder.addAttribute("timestamp", ""+event.getTimeMillis());
		eventBuilder.addAttribute("thread", event.getThreadName());

		Message message = event.getMessage();
		XmlBuilder messageBuilder = XmlBuilder.create("configuration");
		messageBuilder.setElementContent(message.getFormattedMessage());
		eventBuilder.setSubElement(messageBuilder);

		return eventBuilder.toString()+System.lineSeparator();
	}

	@PluginFactory
	public static IbisConfigurationXmlLayout createLayout(
			@PluginConfiguration final Configuration config,
			// LOG4J2-783 use platform default by default, so do not specify defaultString for charset
			@PluginAttribute(value = "charset") final Charset charset) {
		return new IbisConfigurationXmlLayout(config, charset);
	}

	private static class XmlBuilder {
		private Element element;

		public static XmlBuilder create(String tagName) {
			return new XmlBuilder(tagName);
		}

		private XmlBuilder(String tagName) {
			element = new Element(tagName);
		}

		public void setElementContent(String value) {
			if (value != null) {
				//Escape illegal JDOM characters
				element.setText(StringEscapeUtils.escapeJava(value));
			}
		}

		public void setSubElement(XmlBuilder newElement) {
			addSubElement(newElement, true);
		}

		public void addSubElement(XmlBuilder newElement, boolean adoptNamespace) {
			if (newElement != null) {
				if (adoptNamespace && StringUtils.isNotEmpty(element.getNamespaceURI())) {
					addNamespaces(newElement.element, element.getNamespace());
				}
				element.addContent(newElement.element);
			}
		}

		private void addNamespaces(Element element, Namespace namespace) {
			if (StringUtils.isEmpty(element.getNamespaceURI())) {
				element.setNamespace(namespace);
				List<Element> childList = element.getChildren();
				if (!childList.isEmpty()) {
					for (Element child : childList) {
						addNamespaces(child, namespace);
					}
				}
			}
		}

		public void addAttribute(String name, String value) {
			if (value != null) {
				if (name.equalsIgnoreCase("xmlns")) {
					element.setNamespace(Namespace.getNamespace(value));
				} else if (StringUtils.startsWithIgnoreCase(name, "xmlns:")) {
					String prefix = name.substring(6);
					element.addNamespaceDeclaration(Namespace.getNamespace(prefix, value));
				} else {
					element.setAttribute(new Attribute(name, value));
				}
			}
		}

		@Override
		public String toString() {
			Document document = new Document(element.detach());
			XMLOutputter xmlOutputter = new XMLOutputter();
			xmlOutputter.getFormat().setOmitDeclaration(true);
			return xmlOutputter.outputString(document);
		}
	}
}
