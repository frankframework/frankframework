package org.frankframework.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class OverflowToDiskOutputStreamTest {

	private @TempDir Path tmpDir;

	@Test
	public void testWritingInMemory() throws IOException {
		String testString = "shorter-then-100-characters";
		OverflowToDiskOutputStream oos = new OverflowToDiskOutputStream(100, tmpDir);
		oos.write(testString.getBytes());
		oos.write(48);
		oos.flush(); //shouldn't do anything
		oos.write(48);

		oos.flush();
		assertEquals(0, Files.list(tmpDir).count(), "no file should have been created");
		oos.close();

		Message msg = oos.toMessage();
		String location = (String) msg.getContext().get(MessageContext.METADATA_LOCATION);
		assertNull(location, "there should not be a file location, the file should be in memory");

		assertEquals(testString+"00", msg.asString(), "contents of Message differs from the file");
		msg.close();
	}

	@Test
	public void testWritingToDisk() throws IOException {
		String testString = "longer-then-10-characters";
		OverflowToDiskOutputStream oos = new OverflowToDiskOutputStream(10, tmpDir);
		oos.write(testString.getBytes());
		oos.flush(); //shouldn't do anything

		List<Path> files = Files.list(tmpDir).toList();
		assertEquals(1, files.size(), "1 file should have been created");
		String fileContents = new String(Files.newInputStream(files.get(0)).readAllBytes());
		assertEquals(testString, fileContents, "contents of both files differ, do we have the correct test file?");

		oos.close();
		assertTrue(Files.exists(files.get(0)), "closing the OverflowOutputStream should not remove the file");

		String location = (String) oos.toMessage().getContext().get(MessageContext.METADATA_LOCATION);
		assertEquals(files.get(0).toString(), location, "the file location is incorrect");

		oos.toMessage().close();
		assertFalse(Files.exists(files.get(0)), "File should be removed after message is closed");
	}

	@ParameterizedTest
	@ValueSource(ints = {27, 28, 50})
	public void testMultipleWritesToDisk(int bufferSize) throws IOException {
		String testString = "abcdefghijklmnopqrstuvwxyz";
		OverflowToDiskOutputStream oos = new OverflowToDiskOutputStream(bufferSize, tmpDir);
		oos.write(testString.getBytes());

		assertEquals(0, Files.list(tmpDir).count(), "no file should have been created");

		oos.write(testString.getBytes());

		oos.close();
		List<Path> files = Files.list(tmpDir).toList();
		assertEquals(1, files.size(), "1 file should have been created");
		String fileContents = new String(Files.newInputStream(files.get(0)).readAllBytes());
		assertEquals(testString + testString, fileContents, "contents of both files differ, do we have the correct test file?");

		assertTrue(Files.exists(files.get(0)), "closing the OverflowOutputStream should not remove the file");
		assertEquals(testString + testString, oos.toMessage().asString(), "contents of Message differs from the file");

		String location = (String) oos.toMessage().getContext().get(MessageContext.METADATA_LOCATION);
		assertEquals(files.get(0).toString(), location, "the file location is incorrect");

		oos.toMessage().close();
		assertFalse(Files.exists(files.get(0)), "File should be removed after message is closed");
	}

	@Test
	public void testSameBufferSizeAsContentLength() throws IOException {
		String testString = "abcdefghijklmnopqrstuvwxyz";
		OverflowToDiskOutputStream oos = new OverflowToDiskOutputStream(testString.length(), tmpDir);
		oos.write(testString.getBytes());

		List<Path> files = Files.list(tmpDir).toList();
		assertEquals(1, files.size(), "1 file should have been created");
		assertEquals(0, Files.newInputStream(files.get(0)).available(), "default buffer size not exceeded nothing will be written until close is called");
		oos.close();

		String fileContents = new String(Files.newInputStream(files.get(0)).readAllBytes());
		assertEquals(testString, fileContents, "contents of both files differ, do we have the correct test file?");

		oos.close();
		assertTrue(Files.exists(files.get(0)), "closing the OverflowOutputStream should not remove the file");

		String location = (String) oos.toMessage().getContext().get(MessageContext.METADATA_LOCATION);
		assertEquals(files.get(0).toString(), location, "the file location is incorrect");

		oos.toMessage().close();
		assertFalse(Files.exists(files.get(0)), "File should be removed after message is closed");
	}

	@ParameterizedTest
	@ValueSource(ints = {0, -1, -23156786})
	public void testBufferSizeLessThenOne(int bufferSize) throws IOException {
		OverflowToDiskOutputStream oos = new OverflowToDiskOutputStream(bufferSize, tmpDir);
		oos.write(48);
		oos.flush(); //shouldn't do anything
		oos.write(48);

		List<Path> files = Files.list(tmpDir).toList();
		assertEquals(1, files.size(), "1 file should have been created");

		oos.close();
		String fileContents = new String(Files.newInputStream(files.get(0)).readAllBytes());
		assertEquals("00", fileContents, "contents of both files differ, do we have the correct test file?");

		oos.close();
		assertTrue(Files.exists(files.get(0)), "closing the OverflowOutputStream should not remove the file");

		String location = (String) oos.toMessage().getContext().get(MessageContext.METADATA_LOCATION);
		assertEquals(files.get(0).toString(), location, "the file location is incorrect");

		oos.toMessage().close();
		assertFalse(Files.exists(files.get(0)), "File should be removed after message is closed");
	}

	@Test
	public void testLargeFile() throws IOException {
		OverflowToDiskOutputStream oos = new OverflowToDiskOutputStream(5_000_000, tmpDir);
		ByteArrayOutputStream boas = new ByteArrayOutputStream();

		URL data = OverflowToDiskOutputStreamTest.class.getResource("/Documents/doc001.pdf");
		assertNotNull(data, "unable to find test file");
		for (int i = 0; i < 100; i++) {
			data.openStream().transferTo(oos);
			data.openStream().transferTo(boas);
		}

		assertEquals(0, Files.list(tmpDir).count(), "no file should have been created");

		oos.flush(true);
		assertEquals(1, Files.list(tmpDir).count(), "file should have been created");
		data.openStream().transferTo(oos);
		data.openStream().transferTo(boas);

		List<Path> files = Files.list(tmpDir).toList();
		assertEquals(1, files.size(), "1 file should have been created");
		long size = files.get(0).toFile().length();
		assertTrue(size > 2_500_000);

		oos.flush(true);
		oos.close();
		assertTrue(Files.exists(files.get(0)), "closing the OverflowOutputStream should not remove the file");

		String location = (String) oos.toMessage().getContext().get(MessageContext.METADATA_LOCATION);
		assertEquals(files.get(0).toString(), location, "the file location is incorrect");

		Message message = oos.toMessage();
		assertInstanceOf(PathMessage.class, message);
		byte[] content = message.asByteArray();
		assertArrayEquals(boas.toByteArray(), content);

		message.close();
		assertFalse(Files.exists(files.get(0)), "File should be removed after message is closed");
	}
}
