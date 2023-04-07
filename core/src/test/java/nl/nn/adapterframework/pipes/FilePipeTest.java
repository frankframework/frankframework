package nl.nn.adapterframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * FilePipe Tester.
 *
 * @author <Sina Sen>
 */
public class FilePipeTest extends PipeTestBase<FilePipe> {

	@ClassRule
	public static TemporaryFolder testFolderSource = new TemporaryFolder();

	private static String sourceFolderPath;

	private byte[] var = "Some String you want".getBytes();

	@Override
	public FilePipe createPipe() {
		return new FilePipe();
	}

	@BeforeClass
	public static void before() throws Exception {
		testFolderSource.newFile("1.txt");
		sourceFolderPath = testFolderSource.getRoot().getPath();

	}

	@Test
	public void doTestSuccess() throws Exception {
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
	public void doTestFailAsEncodingNotSupportedBase64() throws Exception {
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
