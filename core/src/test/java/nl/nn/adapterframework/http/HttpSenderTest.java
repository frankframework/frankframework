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
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.Misc;

import org.junit.Test;

public class HttpSenderTest extends BaseHttpSender<HttpSender> {

	@Override
	public HttpSender createSender() {
		return spy(new HttpSender());
	}

	@Test
	public void simpleMockedHttpGet() throws Throwable {
		HttpSender sender = getSender();
		String input = "hallo";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

			sender.setMethodType("GET");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(null, input, prc);
			assertEquals(getFile("simpleMockedHttpGet.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpGetWithParams() throws Throwable {
		HttpSender sender = getSender();
		String input = "hallo";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

			Parameter param1 = new Parameter();
			param1.setName("key");
			param1.setValue("value");
			sender.addParameter(param1);

			Parameter param2 = new Parameter();
			param2.setName("otherKey");
			param2.setValue("otherValue");
			sender.addParameter(param2);

			sender.setMethodType("GET");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(null, input, prc);
			assertEquals(getFile("simpleMockedHttpGetWithParams.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpPost() throws Throwable {
		HttpSender sender = getSender();
		String input = "<xml>input</xml>";

		try {
			IPipeLineSession pls = new PipeLineSessionBase();
			ParameterResolutionContext prc = new ParameterResolutionContext(input, pls);

			Parameter param1 = new Parameter();
			param1.setName("key");
			param1.setValue("value");
			sender.addParameter(param1);

			Parameter param2 = new Parameter();
			param2.setName("otherKey");
			param2.setValue("otherValue");
			sender.addParameter(param2);

			sender.setMethodType("POST");
			sender.setParamsInUrl(false);
			sender.setInputMessageParam("nameOfTheFirstContentId");

			sender.configure();
			sender.open();

			String result = sender.sendMessage(null, input, prc);
			assertEquals(getFile("simpleMockedHttpPost.txt"), result);
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpMultipart() throws Throwable {
		HttpSender sender = getSender();
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
			assertEquals(getFile("simpleMockedHttpMultipart.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void simpleMockedHttpMtom() throws Throwable {
		HttpSender sender = getSender();
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
			assertEquals(getFile("simpleMockedHttpMtom.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}
}