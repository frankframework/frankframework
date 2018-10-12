/*
   Copyright 2018 Nationale-Nederlanden

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

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.Misc;

import org.junit.Test;

public class WebServiceSenderTest extends BaseHttpSender<WebServiceSender> {

	@Override
	public WebServiceSender createSender() {
		WebServiceSender sender = spy(new WebServiceSender());
		sender.setSoap(false);
		return sender;
	}

	@Test
	public void simpleMockedWss() throws Throwable {
		WebServiceSender sender = getSender();
		String input = "<hallo/>";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(null, input, prc);
			assertEquals(Misc.streamToString(getFile("simpleMockedWss.txt")), result);
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedWssSoapAction() throws Throwable {
		WebServiceSender sender = getSender();
		String input = "<hallo/>";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

			sender.setSoapAction(sender.getUrl());

			sender.configure();
			sender.open();

			String result = sender.sendMessage(null, input, prc);
			assertEquals(Misc.streamToString(getFile("simpleMockedWssSoapAction.txt")), result);
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedWssMultipart() throws Throwable {
		WebServiceSender sender = getSender();
		String input = "<xml>input</xml>";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

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

			String result = sender.sendMessage(null, input, prc);
			assertEquals(Misc.streamToString(getFile("simpleMockedWssMultipart.txt")), result);
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedWssMultipart2() throws Throwable {
		WebServiceSender sender = getSender();
		String input = "<xml>input</xml>";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

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

			String result = sender.sendMessage(null, input, prc);
			assertEquals(Misc.streamToString(getFile("simpleMockedWssMultipart2.txt")), result);
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedWssMtom() throws Throwable {
		WebServiceSender sender = getSender();
		String input = "<xml>input</xml>";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

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

			String result = sender.sendMessage(null, input, prc);
			assertEquals(Misc.streamToString(getFile("simpleMockedWssMtom.txt")), result);
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}
}