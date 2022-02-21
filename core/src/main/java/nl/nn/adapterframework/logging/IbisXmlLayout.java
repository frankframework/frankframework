/*
   Copyright 2020, 2022 WeAreFrank!

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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.message.Message;

import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Implementation of {@link IbisMaskingLayout} to serialize given log events 
 * in the following format: <br/>
 * <pre> {@code
 * <log4j:event logger="nl.nn.adapterframework.xxx" timestamp="123" level="DEBUG" thread="thread-1">
 * <log4j:message><![CDATA[my message here]]></log4j:message>
 * <log4j:throwable><![CDATA[]]></log4j:throwable>
 * </log4j:event>
 * } </pre>
 */
@Plugin(name = "IbisXmlLayout", category = "Core", elementType = "layout", printObject = true)
public class IbisXmlLayout extends IbisMaskingLayout {
	private boolean alwaysWriteExceptions = false;

	protected IbisXmlLayout(final Configuration config, final Charset charset, final boolean alwaysWriteExceptions) {
		super(config, charset);
		this.alwaysWriteExceptions = alwaysWriteExceptions;
	}

	@Override
	protected String serializeEvent(LogEvent event) {
		XmlBuilder eventBuilder = XmlBuilder.create("event");
		eventBuilder.addAttribute("logger", event.getLoggerName());
		eventBuilder.addAttribute("timestamp", ""+event.getTimeMillis());
		eventBuilder.addAttribute("level", event.getLevel().name());
		eventBuilder.addAttribute("thread", event.getThreadName());

		Message message = event.getMessage();
		XmlBuilder messageBuilder = XmlBuilder.create("message");
		messageBuilder.setValue(message.getFormattedMessage());
		eventBuilder.addSubElement(messageBuilder);

		Throwable t = message.getThrowable();
		if(t != null || alwaysWriteExceptions) {
			XmlBuilder throwableBuilder = XmlBuilder.create("throwable");
			StringWriter sw = new StringWriter();
			if(t != null) {
				t.printStackTrace(new PrintWriter(sw));
			}
			throwableBuilder.setValue(sw.toString());
			eventBuilder.addSubElement(throwableBuilder);
		}

		return eventBuilder.toString()+System.lineSeparator();
	}

	@PluginFactory
	public static IbisXmlLayout createLayout(
			@PluginConfiguration final Configuration config,
			// LOG4J2-783 use platform default by default, so do not specify defaultString for charset
			@PluginAttribute(value = "charset") final Charset charset,
			@PluginAttribute(value = "alwaysWriteExceptions", defaultBoolean = false) final boolean alwaysWriteExceptions) {
		return new IbisXmlLayout(config, charset, alwaysWriteExceptions);
	}

}
