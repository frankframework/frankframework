package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.io.IOUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.testutil.ParameterBuilder;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;

/**
 * FixedResultPipe Tester.
 *
 * @author <Sina Sen>
 */
public class FixedResultPipeTest extends PipeTestBase<FixedResultPipe> {

	private static final String PIPES_2_TXT = "/Pipes/2.txt";
	private static final String PIPES_FILE_PDF = "/Pipes/file.pdf";

	@Override
	public FixedResultPipe createPipe() {
		return new FixedResultPipe();
	}

	@Test
	public void testSuccessWithParam() throws Exception {
		Parameter filename = ParameterBuilder.create().withName("filename").withValue(PIPES_2_TXT);

		pipe.addParameter(filename);
		pipe.configure();

		PipeRunResult res = doPipe(pipe, "whatisthis", session);
		assertEquals("inside the file", res.getResult().asString());
	}

	@Test
	public void testSuccessWithAttribute() throws Exception {
		pipe.setFilename(PIPES_2_TXT);
		pipe.configure();

		PipeRunResult pipeRunResult = doPipe(pipe, "whatisthis", session);

		try (InputStream inputStream = pipeRunResult.getResult().asInputStream()) {
			String fileContents = new String(inputStream.readAllBytes());

			assertEquals("inside the file", fileContents);
		}
	}

	@Test
	public void testFailureWithAttribute() {
		pipe.setFilename(PIPES_2_TXT + "/something.txt");

		assertThrows(ConfigurationException.class, this::configurePipe);
	}

	@Test
	public void testFailureWithParam() {
		Parameter filename = ParameterBuilder.create().withName("filename").withValue(PIPES_2_TXT + "/something.txt");
		pipe.addParameter(filename);

		assertThrows(ConfigurationException.class, this::configurePipe);
	}

	@Test
	public void testBinaryContent() throws Exception {
		pipe.setFilename(PIPES_FILE_PDF);
		pipe.configure();

		PipeRunResult pipeRunResult = doPipe(pipe, "whatisthis", session);

		try (InputStream inputStreamFromPipe = pipeRunResult.getResult().asInputStream()) {
			URL resourceFromClasspath = FixedResultPipe.class.getResource(PIPES_FILE_PDF);
			InputStream inputStream = resourceFromClasspath.openStream();
			boolean contentEquals = IOUtils.contentEquals(inputStreamFromPipe, inputStream);

			inputStream.close();

			assertTrue(contentEquals, "File contents differ");
		}
	}

	@Test
	public void testEmptyFileName(){
		ConfigurationException e = assertThrows(ConfigurationException.class, this::configurePipe);
		assertThat(e.getMessage(), Matchers.endsWith("No filename parameter found"));
	}
}
