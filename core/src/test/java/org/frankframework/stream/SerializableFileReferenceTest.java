/*
   Copyright 2023-2024 WeAreFrank!

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
package org.frankframework.stream;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.testutil.SerializationTester;

@Log4j2
public class SerializableFileReferenceTest {
	private static final String TEST_DATA = "Test-DÁTAç";
	private String testDataEnriched;

	private static final String[][] referenceWires = {
		{"7.8.1 2023-05-15", "aced0005737200376e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e53657269616c697a61626c6546696c655265666572656e636500000000000000010300035a000662696e6172794a000473697a654c0007636861727365747400124c6a6176612f6c616e672f537472696e673b787077240000000000000001000000000000000c0000055554462d38546573742d44c3815441c3a778"},
		{"7.9.1 2023-12-21", "aced0005737200376e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e53657269616c697a61626c6546696c655265666572656e636500000000000000010300035a000662696e6172794a000473697a654c0007636861727365747400124c6a6176612f6c616e672f537472696e673b787077240000000000000001000000000000000c0000055554462d38546573742d44c3815441c3a778"},
		{"8.0.0 2023-12-21", "aced0005737200336f72672e6672616e6b6672616d65776f726b2e73747265616d2e53657269616c697a61626c6546696c655265666572656e636500000000000000010300035a000662696e6172794a000473697a654c0007636861727365747400124c6a6176612f6c616e672f537472696e673b787077240000000000000001000000000000000c0000055554462d38546573742d44c3815441c3a778"},
	};
	private static final String[][] messageWires = {
		{"7.8.1 2023-05-15", "aced0005737200296e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e46696c654d657373616765486ff55492ed0771020000787200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38737200376e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e53657269616c697a61626c6546696c655265666572656e636500000000000000010300035a000662696e6172794a000473697a654c00076368617273657471007e00047870771f0000000000000001000000000000000c010000546573742d44c3815441c3a77874000446696c6578"},
		{"7.9.1 2023-12-21", "aced0005737200296e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e46696c654d657373616765486ff55492ed0771020000787200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002e4c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38737200376e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e53657269616c697a61626c6546696c655265666572656e636500000000000000010300035a000662696e6172794a000473697a654c00076368617273657471007e00047870771f0000000000000001000000000000000c010000546573742d44c3815441c3a77874000446696c6578"},
		{"8.0.0 2023-12-21", "aced0005737200256f72672e6672616e6b6672616d65776f726b2e73747265616d2e46696c654d657373616765486ff55492ed0771020000787200216f72672e6672616e6b6672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002a4c6f72672f6672616e6b6672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38737200336f72672e6672616e6b6672616d65776f726b2e73747265616d2e53657269616c697a61626c6546696c655265666572656e636500000000000000010300035a000662696e6172794a000473697a654c00076368617273657471007e00047870771f0000000000000001000000000000000c010000546573742d44c3815441c3a77874000446696c6578"},
	};

	private SerializableFileReference reference;

	@BeforeEach
	public void setUp(TestInfo testInfo) throws Exception {
		testDataEnriched = TEST_DATA + "-" + testInfo.getDisplayName();
	}

	@AfterEach
	public void tearDown() {
		if (reference != null) {
			reference.close();
		}
	}

	@Test
	void testTemporaryFilesShouldBeRemoved() throws Exception {
		// Arrange & Act
		Path filePath = Path.of(performCleanerTests());
		assertTrue(Files.exists(filePath), "File should exist after test, otherwise it was already closed too early");

		// GC can clean up the file
		System.gc();
		// Assert: Give the garbage collector some time to clean up
		await().pollInterval(50, TimeUnit.MILLISECONDS)
				.atMost(2, TimeUnit.SECONDS)
				.until(() -> !Files.exists(filePath));
	}

	private String performCleanerTests() throws Exception {
		// Arrange
		reference = SerializableFileReference.of(testDataEnriched, StandardCharsets.UTF_8.name());

		// Act
		try (SerializableFileReference ref = reference) {
			// Assert
			assertNotNull(ref);
		}
		assertFalse(Files.exists(reference.getPath()), "Path should not exist after close");

		// Arrange
		SerializableFileReference reference2 = SerializableFileReference.of(testDataEnriched, StandardCharsets.UTF_8.name());
		return reference2.getPath().toString();
	}

	@Test
	public void ofInputStream() throws Exception {
		// Arrange
		final boolean[] closed = {false};
		InputStream in = new ByteArrayInputStream(testDataEnriched.getBytes(StandardCharsets.UTF_8)) {
			@Override
			public void close() throws IOException {
				closed[0] = true;
				super.close();
			}
		};

		// Act
		reference = SerializableFileReference.of(in);

		// Assert
		assertTrue(closed[0], "When creating from stream, stream should be closed when done");
		Path path = reference.getPath();
		assertNotNull(path, "Path should be set");
		assertTrue(Files.exists(path), "Path should exist");
		assertTrue(reference.isBinary(), "Should be binary");

		String fileData;
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			fileData = reader.readLine();
		}

		assertEquals(testDataEnriched, fileData);

		reference.close();
		assertFalse(Files.exists(path), "Path should not exist after close");
	}

	@Test
	public void ofByteArray() throws Exception {
		// Arrange
		byte[] data = testDataEnriched.getBytes(StandardCharsets.UTF_8);

		// Act
		reference = SerializableFileReference.of(data);

		// Assert
		Path path = reference.getPath();
		assertNotNull(path, "Path should be set");
		assertTrue(Files.exists(path), "Path should exist");
		assertTrue(reference.isBinary(), "Should be binary");

		String fileData;
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			fileData = reader.readLine();
		}

		assertEquals(testDataEnriched, fileData);

		reference.close();
		assertFalse(Files.exists(path), "Path should not exist after close");
	}

	@Test
	public void ofReader() throws Exception {
		// Arrange
		final boolean[] closed = {false};
		Reader in = new StringReader(testDataEnriched) {
			@Override
			public void close() {
				closed[0] = true;
				super.close();
			}
		};

		// Act
		reference = SerializableFileReference.of(in, StandardCharsets.UTF_8.name());

		// Assert
		assertTrue(closed[0], "When creating from stream, stream should be closed when done");
		Path path = reference.getPath();
		assertNotNull(path, "Path should be set");
		assertTrue(Files.exists(path), "Path should exist");
		assertFalse(reference.isBinary(), "Should not be binary");

		String fileData;
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			fileData = reader.readLine();
		}

		assertEquals(testDataEnriched, fileData);

		reference.close();
		assertFalse(Files.exists(path), "Path should not exist after close");
	}

	@Test
	public void ofString() throws Exception {
		// Act
		reference = SerializableFileReference.of(testDataEnriched, StandardCharsets.UTF_8.name());

		// Assert
		Path path = reference.getPath();
		assertNotNull(path, "Path should be set");
		assertTrue(Files.exists(path), "Path should exist");
		assertFalse(reference.isBinary(), "Should not be binary");

		String fileData;
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			fileData = reader.readLine();
		}

		assertEquals(testDataEnriched, fileData);

		reference.close();
		assertFalse(Files.exists(path), "Path should not exist after close");
	}

	@Test
	public void getSize() throws Exception {
		// Arrange
		reference = SerializableFileReference.of(testDataEnriched, StandardCharsets.UTF_8.name());

		// Act
		long result = reference.getSize();

		// Assert
		assertEquals(testDataEnriched.getBytes(StandardCharsets.UTF_8).length, result);
	}

	@Test
	public void getReader() throws Exception {
		// Arrange
		reference = SerializableFileReference.of(testDataEnriched, StandardCharsets.UTF_8.name());

		// Act
		String fileData;
		try (BufferedReader reader = reference.getReader()) {
			// Assert
			fileData = reader.readLine();
		}
		assertEquals(testDataEnriched, fileData);
	}

	@Test
	public void getInputStream() throws Exception {
		// Arrange
		reference = SerializableFileReference.of(testDataEnriched, StandardCharsets.UTF_8.name());

		// Act
		byte[] fileData = new byte[1000];
		int fileLen;
		try (InputStream in = reference.getInputStream()) {
			// Assert
			fileLen = in.read(fileData);
		}
		assertEquals(testDataEnriched.getBytes(StandardCharsets.UTF_8).length, fileLen);
		assertEquals(testDataEnriched, new String(fileData, 0, fileLen, StandardCharsets.UTF_8));
	}

	@Test
	public void testRemoveTempFileOnCloseOfSession() throws Exception {
		// Arrange
		reference = SerializableFileReference.of(testDataEnriched, StandardCharsets.UTF_8.name());
		Path path = reference.getPath();
		Message message = new Message(reference, new MessageContext(), reference.getClass());

		// Act
		try (PipeLineSession session = new PipeLineSession()) {
			message.closeOnCloseOf(session, "test closing");
		}

		// Assert
		assertFalse(Files.exists(path), "Path should not exist after close of PipeLineSession");
	}

	@Test
	public void testWithReferenceToExistingFile(@TempDir File tempDir) throws Exception {
		// Arrange
		File tempFile = File.createTempFile("file", null, tempDir);
		MessageTest.writeContentsToFile(tempFile, testDataEnriched);

		// Act
		Message message = new FileMessage(tempFile, "UTF-8");

		// Assert
		assertTrue(message.isRequestOfType(SerializableFileReference.class), "Message request should be instance of SerializableFileReference");
		assertFalse(message.isBinary(), "Should not be binary");

		reference = (SerializableFileReference) message.asObject();
		assertFalse(reference.isBinary(), "Should not be binary");
		Path path = reference.getPath();
		assertEquals(tempFile.toPath(), path);

		String result = message.asString();
		assertEquals(testDataEnriched, result);

		reference.close();
		assertTrue(Files.exists(path), "Reference does not own file, Path should still exist after close");
	}

	@Test
	public void testWithReferenceToExistingPath(@TempDir File tempDir) throws Exception {
		// Arrange
		File tempFile = File.createTempFile("file", null, tempDir);
		Path tempFilePath = tempFile.toPath();
		MessageTest.writeContentsToFile(tempFile, testDataEnriched);

		// Act
		Message message = new PathMessage(tempFilePath);

		// Assert
		assertTrue(message.isRequestOfType(SerializableFileReference.class), "Message request should be instance of SerializableFileReference");
		assertTrue(message.isBinary(), "Should be binary");
		String result = message.asString();
		assertEquals(testDataEnriched, result);

		reference = (SerializableFileReference) message.asObject();
		assertTrue(reference.isBinary(), "Should be binary");
		Path path = reference.getPath();
		assertEquals(tempFilePath, path);

		reference.close();
		assertTrue(Files.exists(path), "Reference does not own file, Path should still exist after close");
	}

	@Test
	public void testSerialization() throws Exception {
		// NB: This test logs a serialization-wire that can be added to the array `referenceWires` when there are change to the class structure, to protect a version against breakage by future changes.

		// Arrange
		reference = SerializableFileReference.of(testDataEnriched, StandardCharsets.UTF_8.name());

		SerializationTester<SerializableFileReference> serializationTester = new SerializationTester<>();

		// Act
		SerializableFileReference reference2 = serializationTester.testSerialization(reference);

		// Assert
		assertNotEquals(reference.getPath(), reference2.getPath());

		BufferedReader reader = reference2.getReader();
		String result = reader.readLine();

		assertEquals(testDataEnriched, result);
	}

	@Test
	public void testSerializationWithMessage(@TempDir File tempDir) throws Exception {
		// Arrange
		File tempFile = File.createTempFile("file", null, tempDir);
		MessageTest.writeContentsToFile(tempFile, testDataEnriched);
		Message message = new FileMessage(tempFile, "UTF-8");

		SerializationTester<Message> serializationTester = new SerializationTester<>();

		// Act
		Message message2 = serializationTester.testSerialization(message);

		// Assert
		String result = message2.asString();
		assertEquals(testDataEnriched, result);
		message2.close();
		message.close();
	}

	@Test
	public void testSerializationWithBinaryMessage(@TempDir File tempDir) throws Exception {
		// NB: This test logs a serialization-wire that can be added to the array `messageWires` when there are change to the class structure, to protect a version against breakage by future changes.

		// Arrange
		File tempFile = File.createTempFile("file", null, tempDir);
		MessageTest.writeContentsToFile(tempFile, testDataEnriched);
		Message message = new FileMessage(tempFile);
		message.getContext().withCharset("UTF-8");

		assertTrue(message.isBinary());

		SerializationTester<Message> serializationTester = new SerializationTester<>();

		// Act
		Message message2 = serializationTester.testSerialization(message);

		// Assert
		assertTrue(message2.isBinary());
		String result = message2.asString();
		assertEquals(testDataEnriched, result);
	}

	@Test
	public void testDeserializationCompatibilityWithReference() throws Exception {
		// Arrange
		SerializationTester<SerializableFileReference> serializationTester = new SerializationTester<>();

		// Act / Assert
		for (String[] referenceWire : referenceWires) {
			String label = referenceWire[0];
			log.debug("testDeserializationCompatibility() " + label);
			byte[] wire = Hex.decodeHex(referenceWire[1]);
			SerializableFileReference out = serializationTester.deserialize(wire);

			assertEquals(SerializableFileReference.class, out.getClass());
			assertFalse(out.isBinary(), label);
			BufferedReader reader = out.getReader();
			String result = reader.readLine();
			assertEquals(TEST_DATA, result);

			Path path = out.getPath();
			out.close();
			assertFalse(Files.exists(path), "Path should not exist after close");
		}
	}

	@Test
	public void testDeserializationCompatibilityWithMessage() throws Exception {
		// Arrange
		SerializationTester<Message> serializationTester = new SerializationTester<>();

		// Act / Assert
		for (String[] messageWire : messageWires) {
			String label = messageWire[0];
			log.debug("testDeserializationCompatibility() " + label);
			byte[] wire = Hex.decodeHex(messageWire[1]);
			Message out = serializationTester.deserialize(wire);

			assertEquals(FileMessage.class, out.getClass());
			assertTrue(out.isBinary(), label);
			assertEquals("UTF-8", out.getCharset(), label);
			assertEquals(TEST_DATA, out.asString(), label);
			assertEquals(TEST_DATA.getBytes(StandardCharsets.UTF_8).length, out.size());
		}
	}
}
