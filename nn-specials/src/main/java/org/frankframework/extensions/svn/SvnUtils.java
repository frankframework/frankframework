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
package org.frankframework.extensions.svn;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.http.HttpSender;
import org.frankframework.http.AbstractHttpSender.HttpMethod;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.frankframework.util.XmlException;
import org.frankframework.util.XmlUtils;
/**
 * Some utilities for working with SVN.
 *
 * @author Peter Leeuwenburgh
 */
public class SvnUtils {
	protected static Logger log = LogUtil.getLogger(SvnUtils.class);

	public static String getLogReport(String urlString) throws ConfigurationException, SenderException, TimeoutException, IOException, XmlException {
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
		HttpSender httpSender = new HttpSender();
		try {
			httpSender.setUrl(urlString);
			httpSender.setAllowSelfSignedCertificates(true);
			httpSender.setVerifyHostname(false);
			httpSender.setIgnoreCertificateExpiredException(true);
			httpSender.setXhtml(true);
			httpSender.setMethodType(HttpMethod.HEAD);
			httpSender.configure();
			httpSender.start();
			try (PipeLineSession session = new PipeLineSession();
				Message result = httpSender.sendMessageOrThrow(Message.nullMessage(), session)) {
				return result.asString();
			}
		} finally {
			httpSender.stop();
		}
	}

	private static String getReportHtml(String urlString, String revision, String path) throws ConfigurationException, SenderException, TimeoutException, IOException {
		HttpSender httpSender = new HttpSender();
		try {
			httpSender.setUrl(urlString);
			httpSender.setAllowSelfSignedCertificates(true);
			httpSender.setVerifyHostname(false);
			httpSender.setIgnoreCertificateExpiredException(true);
			httpSender.setXhtml(true);
			httpSender.setMethodType(HttpMethod.REPORT);
			httpSender.configure();
			httpSender.start();

			String logReportRequest = "<S:log-report xmlns:S=\"svn:\">"
					+ "<S:start-revision>" + revision + "</S:start-revision>"
					+ "<S:end-revision>" + revision + "</S:end-revision>"
					+ "<S:limit>1</S:limit>" + "<S:path>" + path + "</S:path>"
					+ "</S:log-report>";

			try (PipeLineSession session = new PipeLineSession();
				Message result = httpSender.sendMessageOrThrow(new Message(logReportRequest), session)) {
				return result.asString();
			}
		} finally {
			httpSender.stop();
		}
	}

}
