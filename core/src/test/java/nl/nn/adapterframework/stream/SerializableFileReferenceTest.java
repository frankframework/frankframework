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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SerializableFileReferenceTest {
	private final static String TEST_DATA = "Test-DÁTAç";

	private SerializableFileReference reference;

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
	public void testSerialization() throws Exception {
		// Arrange
		reference = SerializableFileReference.of(TEST_DATA, StandardCharsets.UTF_8.name());

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);

		// Act
		oos.writeObject(reference);
		oos.flush();
		oos.close();

		byte[] data = bos.toByteArray();

		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		ObjectInputStream ois = new ObjectInputStream(bis);

		SerializableFileReference reference2 = (SerializableFileReference) ois.readObject();

		ois.close();

		// Assert
		assertNotEquals(reference.getPath(), reference2.getPath());

		BufferedReader reader = reference2.getReader();
		String result = reader.readLine();

		assertEquals(TEST_DATA, result);
	}
}
