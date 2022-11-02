package nl.nn.adapterframework.compression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.Before;
import org.junit.Test;

//import nl.nn.adapterframework.compression.ZipWriterPipeOld.Action;
import nl.nn.adapterframework.collection.CollectionActor.Action;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.StreamingPipeTestBase;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.util.StreamUtil;

public class TestZipWriterPipe extends StreamingPipeTestBase<ZipWriterPipe>{

	private ByteArrayOutputStream baos;

	@Override
	public ZipWriterPipe createPipe() throws ConfigurationException {
		return new ZipWriterPipe();
	}

	@Before
	public void setup() {
		baos = new ByteArrayOutputStream();
	}

	@Test
	public void testOpenCollection() throws Exception {
		pipe.setAction(Action.OPEN);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(Message.asMessage(baos));

		assertEquals("success", prr.getPipeForward().getName());
		assertEquals(baos, prr.getResult().asObject());

		ZipWriter zipWriter = (ZipWriter) session.get("zipwriterhandle");
		assertNotNull(zipWriter);
	}

	protected ZipWriter prepareZipWriter() {
		ZipWriter zipWriter = new ZipWriter(baos, true);
		session.put("zipwriterhandle", zipWriter);
		return zipWriter;
	}

	@Test
	public void testWrite() throws Exception {
		pipe.setAction(Action.WRITE);
		pipe.addParameter(new Parameter("filename","fakeFilename"));
		configureAndStartPipe();

		ZipWriter zipWriter = prepareZipWriter();
		String fileContents = "some text to be compressed";

		PipeRunResult prr = doPipe(fileContents);
		assertEquals("success", prr.getPipeForward().getName());
		//assertEquals(fileContents, prr.getResult().asString()); // ZipWriterPipe used to return it's input

		assertEquals(86, baos.size());
		zipWriter.close();
		assertEquals(166, baos.size());

		ZipInputStream zipin = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()));
		ZipEntry entry = zipin.getNextEntry();
		assertEquals("fakeFilename", entry.getName());
		assertEquals(fileContents, StreamUtil.readerToString(new InputStreamReader(zipin), null));
	}

	@Test
	public void testLast() throws Exception {
		pipe.setAction(Action.LAST);
		pipe.addParameter(new Parameter("filename","fakeFilename"));
		configureAndStartPipe();

		prepareZipWriter();
		String fileContents = "some text to be compressed";

		PipeRunResult prr = doPipe(fileContents);
		assertEquals("success", prr.getPipeForward().getName());
		//assertEquals(fileContents, prr.getResult().asString()); // ZipWriterPipe used to return it's input

		assertEquals(166, baos.size());

		ZipInputStream zipin = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()));
		ZipEntry entry = zipin.getNextEntry();
		assertEquals("fakeFilename", entry.getName());
		assertEquals(fileContents, StreamUtil.readerToString(new InputStreamReader(zipin), null));
	}

	@Test
	public void testDoubleWrite() throws Exception {
		pipe.setAction(Action.WRITE);
		pipe.addParameter(ParameterBuilder.create().withName("filename").withSessionKey("filename"));
		configureAndStartPipe();

		ZipWriter zipWriter = prepareZipWriter();
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

		zipWriter.close();

		try (ZipInputStream zipin = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
			ZipEntry entry = zipin.getNextEntry();
			assertEquals(filename1, entry.getName());
			assertEquals(fileContents1, StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));

			entry = zipin.getNextEntry();
			assertEquals(filename2, entry.getName());
			assertEquals(fileContents2, StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));
		}
	}

	@Test
	public void testStream() throws Exception {
		pipe.setAction(Action.STREAM);
		pipe.addParameter(new Parameter("filename","fakeFilename"));
		configureAndStartPipe();

		ZipWriter zipWriter = prepareZipWriter();
		String fileContents = "some text to be compressed";

		PipeRunResult prr = doPipe(fileContents);
		assertEquals("success", prr.getPipeForward().getName());

		OutputStream stream = (OutputStream)prr.getResult().asObject();
		stream.write(fileContents.getBytes());
		stream.close();

		zipWriter.close();

		ZipInputStream zipin = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()));
		ZipEntry entry = zipin.getNextEntry();
		assertEquals("fakeFilename", entry.getName());
		assertEquals(fileContents, StreamUtil.readerToString(new InputStreamReader(zipin), null));
	}

	@Test
	public void testDoubleStream() throws Exception {
		pipe.setAction(Action.STREAM);
		pipe.addParameter(ParameterBuilder.create().withName("filename").withSessionKey("filename"));
		configureAndStartPipe();

		ZipWriter zipWriter = prepareZipWriter();
		String fileContents1 = "some text to be compressed";
		String fileContents2 = "more text to be compressed";

		String filename1 = "filename1";
		String filename2 = "filename2";

		session.put("filename", filename1);
		PipeRunResult prr = doPipe(fileContents1);
		assertEquals("success", prr.getPipeForward().getName());

		OutputStream stream = (OutputStream)prr.getResult().asObject();
		stream.write(fileContents1.getBytes());
		stream.close();

		session.put("filename", filename2);
		prr = doPipe(fileContents2);
		assertEquals("success", prr.getPipeForward().getName());
		stream = (OutputStream)prr.getResult().asObject();

		stream.write(fileContents2.getBytes());
		stream.close();

		zipWriter.close();

		try (ZipInputStream zipin = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
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
		pipe.setAction(Action.CLOSE);
		configureAndStartPipe();

		ZipWriter zipWriter = prepareZipWriter();
		String input = "dummy";

		assertEquals(0, baos.size());

		PipeRunResult prr = doPipe(input);
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals(input, prr.getResult().asString());

		assertEquals(22, baos.size());

		zipWriter.close(); // multiple close should not be a problem
		assertEquals(22, baos.size());
	}
}
