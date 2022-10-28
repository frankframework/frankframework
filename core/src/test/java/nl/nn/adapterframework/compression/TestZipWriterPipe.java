package nl.nn.adapterframework.compression;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
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

		zipWriter.close();

		ZipInputStream zipin = new ZipInputStream(new ByteArrayInputStream(baos.toByteArray()));
		ZipEntry entry = zipin.getNextEntry();
		assertEquals("fakeFilename", entry.getName());
		assertEquals(fileContents, StreamUtil.readerToString(new InputStreamReader(zipin), null));
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
		assertEquals(zipWriter.getZipoutput(), prr.getResult().asObject());

		zipWriter.close();
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
	}
}
