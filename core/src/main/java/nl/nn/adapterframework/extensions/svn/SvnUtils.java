/*
   Copyright 2015 Nationale-Nederlanden

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

import java.io.IOException;

import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.http.HttpSender;
import nl.nn.adapterframework.http.HttpSenderBase.HttpMethod;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlUtils;
/**
 * Some utilities for working with SVN.
 *
 * @author Peter Leeuwenburgh
 */
public class SvnUtils {
	protected static Logger log = LogUtil.getLogger(SvnUtils.class);

	public static String getLogReport(String urlString) throws DomBuilderException, XPathExpressionException, ConfigurationException, SenderException, TimeoutException, IOException {
		String head = getHeadHtml(urlString);
		String etag = XmlUtils.evaluateXPathNodeSetFirstElement(head,
				"headers/header[lower-case(@name)='etag']");
		if (etag != null) {
			if (StringUtils.countMatches(etag, "\"") >= 2) {
				String s = StringUtils.substringAfter(etag, "\"");
				String s2 = StringUtils.substringBefore(s, "\"");
				String s3 = StringUtils.substringBefore(s2, "/");
				String s4 = StringUtils.substringAfter(s2, "/");
				return getReportHtml(urlString, s3, s4);
			}
		}
		return null;
	}

	private static String getHeadHtml(String urlString) throws ConfigurationException, SenderException, TimeoutException, IOException {
		HttpSender httpSender = null;
		try {
			httpSender = new HttpSender();
			httpSender.setUrl(urlString);
			httpSender.setAllowSelfSignedCertificates(true);
			httpSender.setVerifyHostname(false);
			httpSender.setIgnoreCertificateExpiredException(true);
			httpSender.setXhtml(true);
			httpSender.setMethodType(HttpMethod.HEAD);
			httpSender.configure();
			httpSender.open();
			try (Message result = httpSender.sendMessageOrThrow(Message.nullMessage(), null)) {
				return result.asString();
			}
		} finally {
			if (httpSender != null) {
				httpSender.close();
			}
		}
	}

	private static String getReportHtml(String urlString, String revision, String path) throws ConfigurationException, SenderException, TimeoutException, IOException {
		HttpSender httpSender = null;
		try {
			httpSender = new HttpSender();
			httpSender.setUrl(urlString);
			httpSender.setAllowSelfSignedCertificates(true);
			httpSender.setVerifyHostname(false);
			httpSender.setIgnoreCertificateExpiredException(true);
			httpSender.setXhtml(true);
			httpSender.setMethodType(HttpMethod.REPORT);
			httpSender.configure();
			httpSender.open();

			String logReportRequest = "<S:log-report xmlns:S=\"svn:\">"
					+ "<S:start-revision>" + revision + "</S:start-revision>"
					+ "<S:end-revision>" + revision + "</S:end-revision>"
					+ "<S:limit>1</S:limit>" + "<S:path>" + path + "</S:path>"
					+ "</S:log-report>";

			try (Message result = httpSender.sendMessageOrThrow(new Message(logReportRequest), null)) {
				return result.asString();
			}
		} finally {
			httpSender.close();
		}
	}

}
