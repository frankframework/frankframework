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
package org.frankframework.extensions.akamai;

import static org.frankframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.spy;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.extensions.akamai.NetStorageSender.Action;
import org.frankframework.http.HttpResponseHandler;
import org.frankframework.http.HttpSenderTestBase;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.AppConstants;

public class NetStorageSenderTest extends HttpSenderTestBase<NetStorageSender> {

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		AppConstants.getInstance().setProperty("http.headers.messageid", false);
	}

	@Override
	@AfterEach
	public void tearDown() {
		super.tearDown();
		AppConstants.getInstance().remove("http.headers.messageid");
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
	protected String getFile(String file) throws IOException {
		String content = TestFileUtils.getTestFile("/http/requests/"+file);
		assertNotNull(content, "file ["+"/http/requests/"+file+"] not found");
		return content;
	}

	@Override
	public NetStorageSender getSender() throws Exception {
		NetStorageSender sender = super.getSender(false);
		sender.setCpCode("cpCode123");
		sender.setNonce("myNonce");
		sender.setAccessToken(null); // As long as this is NULL, X-Akamai-ACS-Auth-Sign will be NULL
		return sender;
	}

	@Test
	public void testContentType() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.DU);
		sender.configure();
		assertNull(sender.getFullContentType(), "no content-type should be present");
	}

	@Test
	public void duAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.DU);
		Message input = new Message("my/special/path/"); // Last slash should be removed!

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("duAction.txt"), result.trim());
	}

	@Test
	public void duActionWithRootDir() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.DU);
		sender.setRootDir("/my/special/"); // Start and end with a slash!
		Message input = new Message("path/"); // Last slash should be removed!

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("duAction.txt"), result.trim());
	}

	@Test
	public void dirAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.DIR);
		Message input = new Message("my/special/path/");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("dirAction.txt"), result.trim());
	}

	@Test
	public void deleteAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.DELETE);
		Message input = new Message("my/special/path/");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("deleteAction.txt"), result.trim());
	}

	@Test
	public void uploadAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.UPLOAD);
		Message input = new Message("my/special/path/");

		sender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("fileMessage"));
		Message file = new Message("<dummyFile>");
		session.put("fileMessage", file);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("uploadAction.txt"), result.trim());
	}

	@Test
	public void uploadActionWithMD5Hash() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.UPLOAD);
		sender.setHashAlgorithm(HashAlgorithm.MD5);
		Message input = new Message("my/special/path/");

		sender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("fileMessage"));
		Message file = new Message("<dummyFile>");
		session.put("fileMessage", file);

			sender.configure();
			sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("uploadActionMD5.txt"), result.trim());
	}

	@Test
	public void uploadActionWithCustomMD5Hash() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.UPLOAD);
		sender.setHashAlgorithm(HashAlgorithm.MD5);
		Message input = new Message("my/special/path/");

		sender.addParameter(ParameterBuilder.create().withName("md5").withValue("a1658c154b6af0fba9d93aa86e5be06f")); // Matches response file but uses a different input message
		sender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("fileMessage"));
		Message file = new Message("<dummyFile>----");
		session.put("fileMessage", file);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("uploadActionMD5.txt"), result.replace("----", "").trim());
	}

	@Test
	public void uploadActionWithSHA1Hash() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.UPLOAD);
		sender.setHashAlgorithm(HashAlgorithm.SHA1);
		Message input = new Message("my/special/path/");

		sender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("fileMessage"));
		Message file = new Message("<dummyFile>");
		session.put("fileMessage", file);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("uploadActionSHA1.txt"), result.trim());
	}

	@Test
	public void uploadActionWithCustomSHA1Hash() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.UPLOAD);
		sender.setHashAlgorithm(HashAlgorithm.SHA1);
		Message input = new Message("my/special/path/");

		sender.addParameter(ParameterBuilder.create().withName("sha1").withValue("51e8bbf813bdbcede109d13b863a58132e80b2e2")); // Matches response file but uses a different input message
		sender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("fileMessage"));
		Message file = new Message("<dummyFile>----");
		session.put("fileMessage", file);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("uploadActionSHA1.txt"), result.replace("----", "").trim());
	}

	@Test
	public void uploadActionWithSHA256Hash() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.UPLOAD);
		sender.setHashAlgorithm(HashAlgorithm.SHA256);
		Message input = new Message("my/special/path/");

		sender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("fileMessage"));
		Message file = new Message("<dummyFile>");
		session.put("fileMessage", file);

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("uploadActionSHA256.txt"), result.trim());
	}

	@Test
	public void uploadActionWithCustomSHA256Hash() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.UPLOAD);
		Message input = new Message("my/special/path/");

		// Matches response file but uses a different input message
		sender.addParameter(ParameterBuilder.create().withName("sha256").withValue("71d1503b5afba60e212a46e4112fba56503e281224957ad8dee6034ad25f12dc"));
		sender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("fileMessage"));
			Message file = new Message("<dummyFile>----");
			session.put("fileMessage", file);

			sender.configure();
			sender.start();

			String result = sender.sendMessageOrThrow(input, session).asString();
			assertEqualsIgnoreCRLF(getFile("uploadActionSHA256.txt"), result.replace("----", "").trim());
	}

	@Test
	public void mkdirAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.MKDIR);
		Message input = new Message("my/special/path/");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("mkdirAction.txt"), result.trim());
	}

	@Test
	public void rmdirAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.RMDIR);
		Message input = new Message("my/special/path/");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("rmdirAction.txt"), result.trim());
	}

	@Test
	public void renameAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.RENAME);
		Message input = new Message("my/special/path/file1.txt");

		sender.addParameter(new Parameter("destination", "my/other/special/path/file2.txt"));
		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("renameAction.txt"), result.trim());
	}

	@Test
	public void mtimeAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.MTIME);
		Message input = new Message("my/special/path/");

		sender.addParameter(new Parameter("mtime", "1633945058"));

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("mtimeAction.txt"), result.trim());
	}

	@Test
	public void downloadAction() throws Throwable {
		NetStorageSender sender = getSender();
		sender.setAction(Action.DOWNLOAD);
		Message input = new Message("my/special/path/");

		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEqualsIgnoreCRLF(getFile("downloadAction.txt"), result.trim());
	}
}
