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
package nl.nn.adapterframework.extensions.akamai;

import static nl.nn.adapterframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.http.HttpResponseHandler;
import nl.nn.adapterframework.http.HttpSenderTestBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;

public class NetStorageSenderTest extends HttpSenderTestBase<NetStorageSender> {

	@Override
	public void setUp() throws Exception {
		super.setUp();
		AppConstants.getInstance().setProperty("http.headers.messageid", false);
	}

	@Override
	public NetStorageSender createSender() {
		return spy(new NetStorageSender() {
			@Override
			public Message extractResult(HttpResponseHandler responseHandler, PipeLineSession session) throws SenderException, IOException {
				return new Message( getResponseBodyAsString(responseHandler, true) );
			}
		});
	}

	@Override
	public NetStorageSender getSender() throws Exception {
		NetStorageSender sender = super.getSender(false);
		sender.setCpCode("cpCode123");
		sender.setNonce("myNonce");
		sender.setAccessToken(null); //As long as this is NULL, X-Akamai-ACS-Auth-Sign will be NULL
		return sender;
	}

	@Test
	public void testContentType() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("du");
		sender.configure();
		assertNull("no content-type should be present", sender.getFullContentType());
	}

	@Test
	public void duAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("du");
		Message input = new Message("my/special/path/"); //Last slash should be removed!

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("duAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void duActionWithRootDir() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("du");
		sender.setRootDir("/my/special/"); //Start and end with a slash!
		Message input = new Message("path/"); //Last slash should be removed!

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("duAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void dirAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("dir");
		Message input = new Message("my/special/path/"); //Last slash should be removed!

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("dirAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void deleteAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("delete");
		Message input = new Message("my/special/path/"); //Last slash should be removed!

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("deleteAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void uploadAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("upload");
		Message input = new Message("my/special/path/"); //Last slash should be removed!

		Parameter param = new Parameter();
		param.setName("file");
		param.setSessionKey("fileMessage");
		sender.addParameter(param);
		try {
			Message file = new Message("<dummyFile>");
			PipeLineSession pls = new PipeLineSession(session);
			pls.put("fileMessage", file);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("uploadAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void mkdirAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("mkdir");
		Message input = new Message("my/special/path/"); //Last slash should be removed!

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("mkdirAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void rmdirAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("rmdir");
		Message input = new Message("my/special/path/"); //Last slash should be removed!

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("rmdirAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void renameAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("rename");
		Message input = new Message("my/special/path/file1.txt");

		Parameter param = new Parameter();
		param.setName("destination");
		param.setValue("my/other/special/path/file2.txt");
		sender.addParameter(param);
		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("renameAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void mtimeAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("mtime");
		Message input = new Message("my/special/path/"); //Last slash should be removed!

		Parameter param = new Parameter();
		param.setName("mtime");
		param.setValue("1633945058");
		sender.addParameter(param);
		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("mtimeAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}

	@Test
	public void downloadAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction("download");
		Message input = new Message("my/special/path/"); //Last slash should be removed!

		try {
			PipeLineSession pls = new PipeLineSession(session);

			sender.configure();
			sender.open();

			String result = sender.sendMessage(input, pls).asString();
			assertEqualsIgnoreCRLF(getFile("downloadAction.txt"), result.trim());
		} catch (SenderException e) {
			throw e.getCause();
		} finally {
			if (sender != null) {
				sender.close();
			}
		}
	}
}