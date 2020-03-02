/*
   Copyright 2019 Nationale-Nederlanden

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

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.stream.Message;

public class MultipartHttpSenderTest extends HttpSenderTestBase<MultipartHttpSender> {

	@Override
	public MultipartHttpSender createSender() {
		return spy(new MultipartHttpSender());
	}

	@Test
	public void simpleMockedMultipartHttp1() throws Throwable {
		MultipartHttpSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			String xmlMultipart = "<parts><part type=\"file\" name=\"document.pdf\" "
					+ "sessionKey=\"part_file\" size=\"72833\" "
					+ "mimeType=\"application/pdf\"/></parts>";
			pls.put("multipartXml", xmlMultipart);
			pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

			sender.setMultipartXmlSessionKey("multipartXml");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedMultipartHttp1.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedMultipartHttp2() throws Throwable {
		MultipartHttpSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			String xmlMultipart = "<parts><part name=\"dummy\" filename=\"document.pdf\" "
					+ "sessionKey=\"part_file\" size=\"72833\" "
					+ "mimeType=\"application/pdf\"/></parts>";
			pls.put("multipartXml", xmlMultipart);
			pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

			sender.setMultipartXmlSessionKey("multipartXml");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedMultipartHttp2.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedMultipartHttp3() throws Throwable {
		MultipartHttpSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			String xmlMultipart = "<parts><part name=\"dummy\" "
					+ "value=\"{json:true}\" size=\"72833\" "
					+ "mimeType=\"application/json\"/></parts>";
			pls.put("multipartXml", xmlMultipart);
			pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

			sender.setMultipartXmlSessionKey("multipartXml");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedMultipartHttp3.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedMultipartMtom1() throws Throwable {
		MultipartHttpSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

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
			assertEquals(getFile("simpleMockedMultipartMtom1.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedMultipartMtom2() throws Throwable {
		MultipartHttpSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			String xmlMultipart = "<parts><part name=\"dummy\" filename=\"document.pdf\" "
					+ "sessionKey=\"part_file\" size=\"72833\" "
					+ "mimeType=\"application/pdf\"/></parts>";
			pls.put("multipartXml", xmlMultipart);
			pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

			sender.setMtomEnabled(true);
			sender.setMultipartXmlSessionKey("multipartXml");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedMultipartMtom2.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedMultipartMtom3() throws Throwable {
		MultipartHttpSender sender = getSender();
		Message input = new Message("<xml>input</xml>");

		try {
			IPipeLineSession pls = new PipeLineSessionBase(session);

			String xmlMultipart = "<parts><part name=\"dummy\" "
					+ "value=\"{json:true}\" size=\"72833\" "
					+ "mimeType=\"application/json\"/></parts>";
			pls.put("multipartXml", xmlMultipart);
			pls.put("part_file", new ByteArrayInputStream("<dummy xml file/>".getBytes()));

			sender.setMtomEnabled(true);
			sender.setMultipartXmlSessionKey("multipartXml");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEquals(getFile("simpleMockedMultipartMtom3.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}
}