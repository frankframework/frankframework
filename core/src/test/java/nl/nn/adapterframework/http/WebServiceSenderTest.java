/*
   Copyright 2018-2019 Nationale-Nederlanden

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

import static nl.nn.adapterframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;

public class WebServiceSenderTest extends HttpSenderTestBase<WebServiceSender> {

	@Override
	public WebServiceSender createSender() {
		WebServiceSender sender = spy(new WebServiceSender());
		sender.setSoap(false);
		return sender;
	}

	@Test
	public void simpleMockedWss() throws Throwable {
		WebServiceSender sender = getSender();
		Message input = new Message("<hallo/>");

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("simpleMockedWss.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		}
	}

	@Test
	public void simpleMockedWssSoapAction() throws Throwable {
		WebServiceSender sender = getSender();
		Message input = new Message("<hallo/>");

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.setSoapAction(sender.getUrl());

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();;
			assertEqualsIgnoreCRLF(getFile("simpleMockedWssSoapAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		}
	}

	@Test
	public void simpleMockedWssMultipart() throws Throwable {
		WebServiceSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.setMethodType("POST");
			sender.setParamsInUrl(false);
			sender.setInputMessageParam("request");

			String xmlMultipart = "<parts><part type=\"file\" name=\"document.pdf\" "
					+ "sessionKey=\"part_file\" size=\"72833\" "
					+ "mimeType=\"application/pdf\"/></parts>";
			pls.put("multipartXml", xmlMultipart);
			pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

			sender.setMultipartXmlSessionKey("multipartXml");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();;
			assertEqualsIgnoreCRLF(getFile("simpleMockedWssMultipart.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		}
	}

	@Test
	public void simpleMockedWssMultipart2() throws Throwable {
		WebServiceSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.setMethodType("POST");
			sender.setParamsInUrl(false);
			sender.setInputMessageParam("request");

			String xmlMultipart = "<parts>"
					+ "<part type=\"file\" name=\"document1.pdf\" sessionKey=\"part_file1\" mimeType=\"application/pdf\"/>"
					+ "<part type=\"file\" name=\"document2.pdf\" sessionKey=\"part_file2\" mimeType=\"application/pdf\"/>"
					+ "</parts>";
			pls.put("multipartXml", xmlMultipart);
			pls.put("part_file1", new ByteArrayInputStream("<dummy pdf file/>".getBytes()));
			pls.put("part_file2", new ByteArrayInputStream("<dummy pdf file/>".getBytes()));

			sender.setMultipartXmlSessionKey("multipartXml");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();;
			assertEqualsIgnoreCRLF(getFile("simpleMockedWssMultipart2.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		}
	}

	@Test
	public void simpleMockedWssMtom() throws Throwable {
		WebServiceSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.setMethodType("POST");
			sender.setParamsInUrl(false);
			sender.setInputMessageParam("request");

			String xmlMultipart = "<parts><part type=\"file\" name=\"document.pdf\" "
					+ "sessionKey=\"part_file\" size=\"72833\" "
					+ "mimeType=\"application/pdf\"/></parts>";
			pls.put("multipartXml", xmlMultipart);
			pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

			sender.setMtomEnabled(true);
			sender.setMultipartXmlSessionKey("multipartXml");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("simpleMockedWssMtom.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		}
	}

	@Test
	public void simpleMockedWssMultipartMtomWithParameter() throws Throwable {
		WebServiceSender sender = getSender();
		Message input = new Message("<xml>hello world</xml>");

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.setParamsInUrl(false);
			sender.setInputMessageParam("file");
			sender.setMultipart(true);
			sender.setAllowSelfSignedCertificates(true);
			sender.setVerifyHostname(false);
			sender.setMtomEnabled(true);
	
			Parameter param = new Parameter();
			param.setName("file");
			param.setValue("<xml>I just sent some text! :)</xml>");
			sender.addParameter(param);
	
			sender.configure();
			sender.open();
	
			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("simpleMockedWssMultipartMtom.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		}
	}
}