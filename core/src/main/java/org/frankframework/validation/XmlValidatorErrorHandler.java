/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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
package org.frankframework.validation;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import lombok.Getter;

import org.frankframework.util.XmlBuilder;

public class XmlValidatorErrorHandler implements ErrorHandler {
	private @Getter boolean warningsOccurred = false;
	private @Getter boolean errorOccurred = false;
	private @Getter String reasons;
	private final XmlValidatorContentHandler xmlValidatorContentHandler;
	private final XmlBuilder xmlReasons = new XmlBuilder("reasons");

	public enum ReasonType {
		WARNING,
		ERROR
	}


	public XmlValidatorErrorHandler(XmlValidatorContentHandler xmlValidatorContentHandler, String mainMessage) {
		this.xmlValidatorContentHandler = xmlValidatorContentHandler;
		XmlBuilder message = new XmlBuilder("message");
		message.setValue(mainMessage);
		xmlReasons.addSubElement(message);
		reasons = mainMessage + ":";
	}

	protected void addReason(String message, String xpath, String location, ReasonType reasonType) {
		if (xpath == null) {
			xpath = xmlValidatorContentHandler.getXpath();
		}

		XmlBuilder reason = new XmlBuilder("reason");
		reason.addAttribute("type", reasonType.name());
		XmlBuilder detail;

		detail = new XmlBuilder("xpath");
		detail.setValue(xpath);
		reason.addSubElement(detail);

		if (StringUtils.isNotEmpty(location)) {
			detail = new XmlBuilder("location");
			detail.setValue(location);
			reason.addSubElement(detail);
		}

		detail = new XmlBuilder("message");
		detail.setValue(message);
		reason.addSubElement(detail);

		xmlReasons.addSubElement(reason);

		if (StringUtils.isNotEmpty(location)) {
			message = location + ": " + message;
		}
		message = xpath + ": " + message;

		if (reasonType==ReasonType.WARNING) {
			warningsOccurred=true;
		} else {
			errorOccurred = true;
		}

		if (reasons == null) {
			reasons = message;
		} else {
			reasons = reasons + "\n" + message;
		}
	}

	protected void addReason(Throwable t, ReasonType reasonType) {
		String location = null;
		if (t instanceof SAXParseException spe) {
			int lineNumber = spe.getLineNumber();
			int columnNumber = spe.getColumnNumber();
			if (lineNumber>=0 || columnNumber>=0) {
				location = "at ("+lineNumber+ ","+columnNumber+")";
			}
		}
		String message;
		if (t instanceof SAXException) {
			message = t.getMessage();
		} else {
			StringWriter stringWriter = new StringWriter();
			try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
				t.printStackTrace(printWriter);
			}
			message = stringWriter.toString();
		}
		addReason(message, null, location, reasonType);
	}

	@Override
	public void warning(SAXParseException exception) {
		addReason(exception, ReasonType.WARNING);
	}

	@Override
	public void error(SAXParseException exception) {
		addReason(exception, ReasonType.ERROR);
	}

	@Override
	public void fatalError(SAXParseException exception) {
		addReason(exception, ReasonType.ERROR);
	}

	public String getXmlReasons() {
		return xmlReasons.asXmlString();
	}
}

