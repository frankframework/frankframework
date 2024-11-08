package org.frankframework.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.MimeType;

import org.frankframework.collection.Collection;
import org.frankframework.collection.AbstractCollectorPipe.Action;
import org.frankframework.collection.TestCollector;
import org.frankframework.collection.TestCollectorPart;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.util.StreamUtil;

public class TestZipWriterPipe extends PipeTestBase<ZipWriterPipe> {

	@Override
	public ZipWriterPipe createPipe() throws ConfigurationException {
		ZipWriterPipe zipWriterPipe = new ZipWriterPipe();
		zipWriterPipe.setCollectionName("zipwriterhandle");
		return zipWriterPipe;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection<TestCollector, TestCollectorPart> getCollectionFromSession() {
		Collection collection = (Collection) session.get("zipwriterhandle");
		assertNotNull(collection);
		return collection;
	}

	private void createCollector() throws PipeRunException {
		pipe.setAction(Action.OPEN);
		pipe.doPipe(null, session);
		getCollectionFromSession(); // ensure its been created.
	}

	@Test
	void testCollectionName() throws Exception {
		pipe.setAction(Action.OPEN);
		configureAndStartPipe();
		assertEquals("zipwriterhandle", pipe.getCollectionName());
	}

	@Test
	void testStreamMissingFilenameParameter() throws Exception {
		pipe.setBackwardsCompatibility(true);
		pipe.setAction(Action.STREAM);
		assertThrows(ConfigurationException.class, this::configureAndStartPipe);
	}

	@Test
	void testStreamEmptyFilenameParameter() throws Exception {
		pipe.setBackwardsCompatibility(true);
		pipe.setAction(Action.STREAM);
		pipe.addParameter(new Parameter("filename", ""));
		configureAndStartPipe();

		assertThrows(PipeRunException.class, () -> pipe.doPipe(null, session));
	}

	@Test
	void testStreamWithFilenameParameter() throws Exception {
		createCollector();

		pipe.setBackwardsCompatibility(true);
		pipe.setAction(Action.STREAM);
		pipe.addParameter(new Parameter("filename", "test.zip"));

		configureAndStartPipe();

		PipeRunResult pipeRunResult = pipe.doPipe(null, session);
		assertTrue(pipeRunResult.getResult().asString().endsWith(".zip"));
	}

	@Test
	void testChangeCollectionName() throws Exception {
		pipe.setCollectionName("test123");
		pipe.setAction(Action.OPEN);
		configureAndStartPipe();
		assertEquals("test123", pipe.getCollectionName());
	}

	@Test
	void testOpenCollection() throws Exception {
		pipe.setAction(Action.OPEN);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(new Message("test123"));

		assertEquals("success", prr.getPipeForward().getName());
		assertTrue(Message.isEmpty(prr.getResult()));

		Collection<TestCollector, TestCollectorPart> collection = getCollectionFromSession();
		assertNotNull(collection);
		Message result = collection.build();
		assertNotNull(result);
		assertEquals(MimeType.valueOf("application/zip"), result.getContext().get(MessageContext.METADATA_MIMETYPE));
		try (ZipInputStream zipin = new ZipInputStream(result.asInputStream())) {
			assertNull(zipin.getNextEntry());
		}
	}

	@Test
	void testWrite() throws Exception {
		createCollector();
		pipe.setAction(Action.WRITE);
		pipe.addParameter(new Parameter("filename", "fakeFilename"));
		configureAndStartPipe();

		String fileContents = "some text to be compressed";

		PipeRunResult prr = doPipe(fileContents);
		assertEquals("success", prr.getPipeForward().getName());
		assertTrue(Message.isNull(prr.getResult()));
		//assertEquals(fileContents, prr.getResult().asString()); // ZipWriterPipe used to return it's input

		Message result = getCollectionFromSession().build();
		ZipInputStream zipin = new ZipInputStream(result.asInputStream());
		ZipEntry entry = zipin.getNextEntry();
		assertEquals("fakeFilename", entry.getName());
		assertEquals(fileContents, StreamUtil.readerToString(new InputStreamReader(zipin), null));
	}

	@Test
	void testLast() throws Exception {
		createCollector();
		pipe.setAction(Action.LAST);
		pipe.addParameter(new Parameter("filename","fakeFilename"));
		configureAndStartPipe();

		String fileContents = "some text to be compressed";

		PipeRunResult prr = doPipe(fileContents);
		assertEquals("success", prr.getPipeForward().getName());
		//assertEquals(fileContents, prr.getResult().asString()); // ZipWriterPipe used to return it's input

		Message result = prr.getResult();
		ZipInputStream zipin = new ZipInputStream(result.asInputStream());
		ZipEntry entry = zipin.getNextEntry();
		assertEquals("fakeFilename", entry.getName());
		assertEquals(fileContents, StreamUtil.readerToString(new InputStreamReader(zipin), null));
	}

	@Test
	void testDoubleWrite() throws Exception {
		createCollector();
		pipe.setAction(Action.WRITE);
		pipe.addParameter(ParameterBuilder.create().withName("filename").withSessionKey("filename"));
		configureAndStartPipe();

		String fileContents1 = "some text to be compressed";
		String fileContents2 = "more text to be compressed";

		String filename1 = "filename1";
		String filename2 = "filename2";

		session.put("filename", filename1);
		PipeRunResult prr = doPipe(fileContents1);
		assertEquals("success", prr.getPipeForward().getName());
		//assertEquals(fileContents, prr.getResult().asString()); // ZipWriterPipe used to return it's input

		session.put("filename", filename2);
		prr = doPipe(fileContents2);
		assertEquals("success", prr.getPipeForward().getName());
		//assertEquals(fileContents, prr.getResult().asString()); // ZipWriterPipe used to return it's input

		Message result = getCollectionFromSession().build();
		try (ZipInputStream zipin = new ZipInputStream(result.asInputStream())) {
			ZipEntry entry = zipin.getNextEntry();
			assertEquals(filename1, entry.getName());
			assertEquals(fileContents1, StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));

			entry = zipin.getNextEntry();
			assertEquals(filename2, entry.getName());
			assertEquals(fileContents2, StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));
		}
	}

	@Test
	void testWriteWithFileHeaders() throws Exception {
		pipe.setCompleteFileHeader(true);
		createCollector();
		pipe.setAction(Action.WRITE);
		pipe.addParameter(ParameterBuilder.create().withName("filename").withSessionKey("filename"));
		configureAndStartPipe();

		String fileContents1 = "some text to be compressed";
		String fileContents2 = "more text to be compressed";
		CRC32 checksum = new CRC32();
		checksum.update(fileContents1.getBytes());

		String filename1 = "filename1";
		String filename2 = "filename2";

		session.put("filename", filename1);
		PipeRunResult prr = doPipe(fileContents1);
		assertEquals("success", prr.getPipeForward().getName());
		//assertEquals(fileContents, prr.getResult().asString()); // ZipWriterPipe used to return it's input

		session.put("filename", filename2);
		prr = doPipe(fileContents2);
		assertEquals("success", prr.getPipeForward().getName());
		//assertEquals(fileContents, prr.getResult().asString()); // ZipWriterPipe used to return it's input

		Message result = getCollectionFromSession().build();
		try (ZipInputStream zipin = new ZipInputStream(result.asInputStream())) {
			ZipEntry entry = zipin.getNextEntry();
			assertEquals(filename1, entry.getName());
			String result222 = StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null);
			assertEquals(fileContents1, result222);
			assertEquals(checksum.getValue(), entry.getCrc());
			assertEquals(fileContents1.getBytes().length, entry.getSize());
			assertEquals(fileContents1.getBytes().length, result222.getBytes().length);

			entry = zipin.getNextEntry();
			assertEquals(filename2, entry.getName());
			assertEquals(fileContents2, StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));
		}
	}

	@Test
	void testClose() throws Exception {
		createCollector();
		pipe.setAction(Action.CLOSE);
		configureAndStartPipe();

		// Act
		PipeRunResult prr = doPipe("dummy");

		// Assert
		assertEquals("success", prr.getPipeForward().getName());
		Message result = prr.getResult();
		assertNotNull(result);
		assertEquals(MimeType.valueOf("application/zip"), result.getContext().get(MessageContext.METADATA_MIMETYPE));
		try (ZipInputStream zipin = new ZipInputStream(result.asInputStream())) {
			assertNull(zipin.getNextEntry());
		}
	}


	@Test
	void testBackwardsCompatibility(@TempDir Path tmpDir) throws Exception {
		// Arrange
		Path zipFile = tmpDir.resolve("tmp-zip-archive.zip");
		Files.createFile(zipFile);
		String zipFileLocation = zipFile.toString();
		pipe.setAction(Action.OPEN);
		pipe.setBackwardsCompatibility(true);
		doPipe(zipFileLocation);
		getCollectionFromSession(); // ensure its been created.

		pipe.setAction(Action.WRITE);
		pipe.addParameter(ParameterBuilder.create().withName("filename").withSessionKey("filename"));
		configureAndStartPipe();

		String fileContents1 = "some text to be compressed";
		String fileContents2 = "more text to be compressed";

		String filename1 = "filename1.txt";
		String filename2 = "filename2.txt";

		// Act1
		session.put("filename", filename1);
		PipeRunResult prr = doPipe(fileContents1);
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals(fileContents1, prr.getResult().asString()); // ZipWriterPipe used to return it's input

		// Act2
		session.put("filename", filename2);
		prr = doPipe(fileContents2);
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals(fileContents2, prr.getResult().asString()); // ZipWriterPipe used to return it's input

		// Assert contents
		Message result = getCollectionFromSession().build();
		try (ZipInputStream zipin = new ZipInputStream(result.asInputStream())) {
			ZipEntry entry = zipin.getNextEntry();
			assertEquals(filename1, entry.getName());
			assertEquals(fileContents1, StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));

			entry = zipin.getNextEntry();
			assertEquals(filename2, entry.getName());
			assertEquals(fileContents2, StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));
		}

		// Assert file
		assertEquals(zipFileLocation, result.getContext().get(MessageContext.METADATA_LOCATION));
		result.close();
		assertTrue(Files.exists(zipFile));
		Files.delete(zipFile);
	}
}
