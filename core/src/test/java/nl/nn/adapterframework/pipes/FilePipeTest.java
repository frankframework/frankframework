package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * FilePipe Tester.
 *
 * @author <Sina Sen>
 */
public class FilePipeTest extends PipeTestBase<FilePipe> {

	@TempDir
	public static File testFolderSource;

	private static String sourceFolderPath;

	private final byte[] var = "Some String you want".getBytes();

	@Override
	public FilePipe createPipe() {
		return new FilePipe();
	}

	@BeforeAll
	public static void before() throws Exception {
		File.createTempFile("1.txt", null, testFolderSource);
		sourceFolderPath = testFolderSource.getPath();

	}

	@Test
	void doTestSuccess() throws Exception {
		PipeForward fw = new PipeForward();
		fw.setName("test");
		pipe.registerForward(fw);
		pipe.setCharset("/");
		pipe.setDirectory(sourceFolderPath);
		pipe.setOutputType("stream");
		pipe.setActions("read");
		pipe.setFilename("1.txt");
		pipe.setFileSource("filesystem");
		pipe.setActions("create");
		pipe.configure();
		PipeRunResult res = doPipe(pipe, var, session);

		assertEquals("success", res.getPipeForward().getName());
	}

	@Test
	void doTestFailAsEncodingNotSupportedBase64() throws Exception {
		PipeForward fw = new PipeForward();
		fw.setName("test");
		pipe.registerForward(fw);
		pipe.setCharset("/");
		pipe.setDirectory(sourceFolderPath);
		pipe.setOutputType("base64");
		pipe.setActions("read");
		pipe.setFilename("1.txt");
		pipe.setFileSource("filesystem");
		pipe.setActions("create");
		pipe.configure();

		PipeRunException e = assertThrows(PipeRunException.class, ()->doPipe(pipe, var, session));
		assertThat(e.getMessage(), Matchers.endsWith("Error while executing file action(s): (UnsupportedEncodingException) /"));
	}

}
