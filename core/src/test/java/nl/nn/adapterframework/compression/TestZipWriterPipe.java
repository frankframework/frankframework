package nl.nn.adapterframework.compression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Test;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.collection.Collection;
import nl.nn.adapterframework.collection.CollectorPipeBase.Action;
import nl.nn.adapterframework.collection.TestCollector;
import nl.nn.adapterframework.collection.TestCollectorPart;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.util.StreamUtil;

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
	public void testOpenCollection() throws Exception {
		pipe.setAction(Action.OPEN);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(Message.asMessage("test123"));

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
	public void testWrite() throws Exception {
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
	public void testLast() throws Exception {
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
	public void testDoubleWrite() throws Exception {
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
	public void testClose() throws Exception {
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
}
