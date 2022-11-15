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

import static nl.nn.adapterframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;
import static org.mockito.Mockito.spy;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import nl.nn.adapterframework.collection.CollectionActor.Action;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.http.HttpSender.PostType;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;

public class MultipartHttpSenderTest extends HttpSenderTestBase<MultipartHttpSender> {

	@Override
	public MultipartHttpSender createSender() {
		return spy(new MultipartHttpSender());
	}

	@Test
	public void simpleMockedMultipartHttp1() throws Throwable {
		MultipartHttpSender senderOpen= getSender();
		senderOpen.setAction(Action.OPEN);
		senderOpen.configure();
		senderOpen.open();

		MultipartHttpSender senderWrite= new MultipartHttpSender();
		senderWrite.setAction(Action.WRITE);
		senderWrite.setPartname("part_file");
		senderWrite.setFilename("document.pdf");
		senderWrite.addParameter(ParameterBuilder.create().withName("contents").withSessionKey("part_file"));
		senderWrite.setMimeType("application/pdf");
		senderWrite.configure();
		senderWrite.open();

		MultipartHttpSender senderClose = getSender();
		senderClose.setAction(Action.CLOSE);
		senderClose.configure();
		senderClose.open();

		Message input = new Message("<xml>input</xml>");
		Message fileMessage = new Message(new ByteArrayInputStream("<dummy xml file/>".getBytes()));

		senderOpen.sendMessage(input, session);
		senderWrite.sendMessage(fileMessage, session);

		try {
			String result = senderClose.sendMessageOrThrow(Message.nullMessage(), session).asString();
			assertEqualsIgnoreCRLF(getFile("simpleMockedMultipartHttp1.txt"), result.trim());
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
		MultipartHttpSender senderOpen= getSender();
		senderOpen.setAction(Action.OPEN);
		senderOpen.configure();
		senderOpen.open();

		MultipartHttpSender senderWrite= new MultipartHttpSender();
		senderWrite.setAction(Action.WRITE);
		senderWrite.setPartname("dummy");
		senderWrite.setFilename("document.pdf");
		senderWrite.setMimeType("application/pdf");
		senderWrite.configure();
		senderWrite.open();

		MultipartHttpSender senderClose = getSender();
		senderClose.setAction(Action.CLOSE);
		senderClose.configure();
		senderClose.open();

		Message inputOpen = new Message("<xml>input</xml>");
		Message inputWrite = new Message(new ByteArrayInputStream("<dummy xml file/>".getBytes()));
		Message inputClose = Message.nullMessage();

		try {

			senderOpen.sendMessage(inputOpen, session);
			senderWrite.sendMessage(inputWrite, session);

			String result = senderClose.sendMessageOrThrow(inputClose, session).asString();
			assertEqualsIgnoreCRLF(getFile("simpleMockedMultipartHttp2.txt"), result.trim());
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
		MultipartHttpSender senderOpen= getSender();
		senderOpen.setAction(Action.OPEN);
		senderOpen.configure();
		senderOpen.open();

		MultipartHttpSender senderWrite= new MultipartHttpSender();
		senderWrite.setAction(Action.WRITE);
		senderWrite.setPartname("dummy");
		senderWrite.setMimeType("application/json");
		senderWrite.configure();
		senderWrite.open();

		MultipartHttpSender senderClose = getSender();
		senderClose.setAction(Action.CLOSE);
		senderClose.configure();
		senderClose.open();

		Message inputOpen = new Message("<xml>input</xml>");
		Message inputWrite = new Message("{json:true}");
		Message inputClose = Message.nullMessage();

		try {
			senderOpen.sendMessage(inputOpen, session);
			senderWrite.sendMessage(inputWrite, session);

			String result = senderClose.sendMessageOrThrow(inputClose, session).asString();
			assertEqualsIgnoreCRLF(getFile("simpleMockedMultipartHttp3.txt"), result.trim());
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
		MultipartHttpSender senderOpen= getSender();
		senderOpen.setAction(Action.OPEN);
		senderOpen.setPostType(PostType.MTOM);
		senderOpen.configure();
		senderOpen.open();

		MultipartHttpSender senderWrite= new MultipartHttpSender();
		senderWrite.setAction(Action.WRITE);
		senderWrite.setPartname("part_file");
		senderWrite.setFilename("document.pdf");
		senderWrite.setMimeType("application/pdf");
		senderWrite.configure();
		senderWrite.open();

		MultipartHttpSender senderClose = getSender();
		senderClose.setAction(Action.CLOSE);
		senderClose.configure();
		senderClose.open();

		Message inputOpen = new Message("<xml>input</xml>");
		Message inputWrite = new Message(new ByteArrayInputStream("<dummy xml file/>".getBytes()));
		Message inputClose = Message.nullMessage();

		try {
			senderOpen.sendMessage(inputOpen, session);
			senderWrite.sendMessage(inputWrite, session);

			String result = senderClose.sendMessageOrThrow(inputClose, session).asString();
			assertEqualsIgnoreCRLF(getFile("simpleMockedMultipartMtom1.txt"), result.trim());
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
		MultipartHttpSender senderOpen= getSender();
		senderOpen.setAction(Action.OPEN);
		senderOpen.setPostType(PostType.MTOM);
		senderOpen.configure();
		senderOpen.open();

		MultipartHttpSender senderWrite= new MultipartHttpSender();
		senderWrite.setAction(Action.WRITE);
		senderWrite.setPartname("dummy");
		senderWrite.setFilename("document.pdf");
		senderWrite.setMimeType("application/pdf");
		senderWrite.configure();
		senderWrite.open();

		MultipartHttpSender senderClose = getSender();
		senderClose.setAction(Action.CLOSE);
		senderClose.configure();
		senderClose.open();

		Message inputOpen = new Message("<xml>input</xml>");
		Message inputWrite = new Message(new ByteArrayInputStream("<dummy xml file/>".getBytes()));
		Message inputClose = Message.nullMessage();

		try {
			senderOpen.sendMessage(inputOpen, session);
			senderWrite.sendMessage(inputWrite, session);

			String result = senderClose.sendMessageOrThrow(inputClose, session).asString();
			assertEqualsIgnoreCRLF(getFile("simpleMockedMultipartMtom2.txt"), result.trim());
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
		MultipartHttpSender senderOpen= getSender();
		senderOpen.setAction(Action.OPEN);
		senderOpen.setPostType(PostType.MTOM);
		senderOpen.configure();
		senderOpen.open();

		MultipartHttpSender senderWrite= new MultipartHttpSender();
		senderWrite.setAction(Action.WRITE);
		senderWrite.setPartname("dummy");
		senderWrite.setMimeType("application/json");
		senderWrite.configure();
		senderWrite.open();

		MultipartHttpSender senderClose = getSender();
		senderClose.setAction(Action.CLOSE);
		senderClose.configure();
		senderClose.open();

		Message inputOpen = new Message("<xml>input</xml>");
		Message inputWrite = new Message("{json:true}");
		Message inputClose = Message.nullMessage();

		try {
			senderOpen.sendMessage(inputOpen, session);
			senderWrite.sendMessage(inputWrite, session);

			String result = senderClose.sendMessageOrThrow(inputClose, session).asString();
			assertEqualsIgnoreCRLF(getFile("simpleMockedMultipartMtom3.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}
}