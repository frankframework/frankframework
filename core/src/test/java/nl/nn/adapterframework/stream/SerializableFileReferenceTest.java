/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.testutil.SerializationTester;
import nl.nn.adapterframework.util.FileUtils;

public class SerializableFileReferenceTest {
	private final Logger log = LogManager.getLogger(SerializableFileReferenceTest.class);
	private final static String TEST_DATA = "Test-DÁTAç";

	private SerializableFileReference reference;

	private final String[][] referenceWires = {
		{"7.8.1 2023-05-15", "aced0005737200376e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e53657269616c697a61626c6546696c655265666572656e636500000000000000010300035a000662696e6172794a000473697a654c0007636861727365747400124c6a6176612f6c616e672f537472696e673b787077240000000000000001000000000000000c0000055554462d38546573742d44c3815441c3a778"},
	};
	private final String[][] messageWires = {
		{"7.8.1 2023-05-15", "aced0005737200296e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e46696c654d657373616765486ff55492ed0771020000787200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38737200376e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e53657269616c697a61626c6546696c655265666572656e636500000000000000010300035a000662696e6172794a000473697a654c00076368617273657471007e00047870771f0000000000000001000000000000000c010000546573742d44c3815441c3a77874000446696c6578"},
	};

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
		if (reference != null) {
			reference.close();
		}
	}

	@Test
	public void ofInputStream() throws Exception {
		// Arrange
		InputStream in = new ByteArrayInputStream(TEST_DATA.getBytes(StandardCharsets.UTF_8));

		// Act
		reference = SerializableFileReference.of(in);

		// Assert
		Path path = reference.getPath();
		assertNotNull("Path should be set", path);
		assertTrue("Path should exist", Files.exists(path));
		assertTrue("Should be binary", reference.isBinary());

		String fileData;
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			fileData = reader.readLine();
		}

		assertEquals(TEST_DATA, fileData);

		reference.close();
		assertFalse("Path should not exist after close", Files.exists(path));
	}

	@Test
	public void ofByteArray() throws Exception {
		// Arrange
		byte[] data = TEST_DATA.getBytes(StandardCharsets.UTF_8);

		// Act
		reference = SerializableFileReference.of(data);

		// Assert
		Path path = reference.getPath();
		assertNotNull("Path should be set", path);
		assertTrue("Path should exist", Files.exists(path));
		assertTrue("Should be binary", reference.isBinary());

		String fileData;
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			fileData = reader.readLine();
		}

		assertEquals(TEST_DATA, fileData);

		reference.close();
		assertFalse("Path should not exist after close", Files.exists(path));
	}

	@Test
	public void ofReader() throws Exception {
		// Arrange
		Reader in = new StringReader(TEST_DATA);

		// Act
		reference = SerializableFileReference.of(in, StandardCharsets.UTF_8.name());

		// Assert
		Path path = reference.getPath();
		assertNotNull("Path should be set", path);
		assertTrue("Path should exist", Files.exists(path));
		assertFalse("Should not be binary", reference.isBinary());

		String fileData;
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			fileData = reader.readLine();
		}

		assertEquals(TEST_DATA, fileData);

		reference.close();
		assertFalse("Path should not exist after close", Files.exists(path));
	}

	@Test
	public void ofString() throws Exception {
		// Act
		reference = SerializableFileReference.of(TEST_DATA, StandardCharsets.UTF_8.name());

		// Assert
		Path path = reference.getPath();
		assertNotNull("Path should be set", path);
		assertTrue("Path should exist", Files.exists(path));
		assertFalse("Should not be binary", reference.isBinary());

		String fileData;
		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			fileData = reader.readLine();
		}

		assertEquals(TEST_DATA, fileData);

		reference.close();
		assertFalse("Path should not exist after close", Files.exists(path));
	}

	@Test
	public void getSize() throws Exception {
		// Arrange
		reference = SerializableFileReference.of(TEST_DATA, StandardCharsets.UTF_8.name());

		// Act
		long result = reference.getSize();

		// Assert
		assertEquals(TEST_DATA.getBytes(StandardCharsets.UTF_8).length, result);
	}

	@Test
	public void getReader() throws Exception {
		// Arrange
		reference = SerializableFileReference.of(TEST_DATA, StandardCharsets.UTF_8.name());

		// Act
		String fileData;
		try (BufferedReader reader = reference.getReader()) {
			// Assert
			fileData = reader.readLine();
		}
		assertEquals(TEST_DATA, fileData);
	}

	@Test
	public void getInputStream() throws Exception {
		// Arrange
		reference = SerializableFileReference.of(TEST_DATA, StandardCharsets.UTF_8.name());

		// Act
		byte[] fileData = new byte[1000];
		int fileLen;
		try (InputStream in = reference.getInputStream()) {
			// Assert
			fileLen = in.read(fileData);
		}
		assertEquals(TEST_DATA.getBytes(StandardCharsets.UTF_8).length, fileLen);
		assertEquals(TEST_DATA, new String(fileData, 0, fileLen, StandardCharsets.UTF_8));
	}

	@Test
	public void testWithReferenceToExistingFile() throws Exception {
		// Arrange
		File tempFile = FileUtils.createTempFile();
		MessageTest.writeContentsToFile(tempFile, TEST_DATA);

		// Act
		Message message = new FileMessage(tempFile, "UTF-8");

		// Assert
		assertTrue("Message request should be instance of SerializableFileReference", message.asObject() instanceof SerializableFileReference);
		assertTrue("Should be binary", message.isBinary());
		String result = message.asString();
		assertEquals(TEST_DATA, result);

		reference = (SerializableFileReference)message.asObject();
		assertTrue("Should be binary", reference.isBinary());
		Path path = reference.getPath();
		assertEquals(tempFile.toPath(), path);

		reference.close();
		assertTrue("Reference does not own file, Path should still exist after close", Files.exists(path));
	}

	@Test
	public void testWithReferenceToExistingPath() throws Exception {
		// Arrange
		File tempFile = FileUtils.createTempFile();
		Path tempFilePath = tempFile.toPath();
		MessageTest.writeContentsToFile(tempFile, TEST_DATA);

		// Act
		Message message = new PathMessage(tempFilePath);

		// Assert
		assertTrue("Message request should be instance of SerializableFileReference", message.asObject() instanceof SerializableFileReference);
		assertTrue("Should be binary", message.isBinary());
		String result = message.asString();
		assertEquals(TEST_DATA, result);

		reference = (SerializableFileReference)message.asObject();
		assertTrue("Should be binary", reference.isBinary());
		Path path = reference.getPath();
		assertEquals(tempFilePath, path);

		reference.close();
		assertTrue("Reference does not own file, Path should still exist after close", Files.exists(path));
	}

	@Test
	public void testSerialization() throws Exception {
		// Arrange
		reference = SerializableFileReference.of(TEST_DATA, StandardCharsets.UTF_8.name());

		SerializationTester<SerializableFileReference> serializationTester = new SerializationTester<>();

		// Act
		SerializableFileReference reference2 = serializationTester.testSerialization(reference);

		// Assert
		assertNotEquals(reference.getPath(), reference2.getPath());

		BufferedReader reader = reference2.getReader();
		String result = reader.readLine();

		assertEquals(TEST_DATA, result);
	}

	@Test
	public void testSerializationWithMessage() throws Exception {
		// Arrange
		File tempFile = FileUtils.createTempFile();
		MessageTest.writeContentsToFile(tempFile, TEST_DATA);
		Message message = new FileMessage(tempFile, "UTF-8");

		SerializationTester<Message> serializationTester = new SerializationTester<>();

		// Act
		Message message2 = serializationTester.testSerialization(message);

		// Assert
		String result = message2.asString();
		assertEquals(TEST_DATA, result);
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
			assertFalse(label, out.isBinary());
			BufferedReader reader = out.getReader();
			String result = reader.readLine();
			assertEquals(TEST_DATA, result);

			Path path = out.getPath();
			out.close();
			assertFalse("Path should not exist after close", Files.exists(path));
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
			assertTrue(label, out.isBinary());
			assertEquals(label, "UTF-8", out.getCharset());
			assertEquals(label, TEST_DATA, out.asString());
			assertEquals(TEST_DATA.getBytes(StandardCharsets.UTF_8).length, out.size());
		}
	}
}
