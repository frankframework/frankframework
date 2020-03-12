package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * FilePipe Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Feb 28, 2020</pre>
 */
public class FilePipeTest extends PipeTestBase<FilePipe> {

    @ClassRule
    public static TemporaryFolder testFolderSource = new TemporaryFolder();

    private static String sourceFolderPath;

    private static InputStream inputStream;
    @Mock
    private IPipeLineSession session1 = new PipeLineSessionBase();

    @Override
    public FilePipe createPipe() {
        return new FilePipe();
    }


    @BeforeClass
    public static void before() throws Exception {
        testFolderSource.newFile("1.txt");
        sourceFolderPath = testFolderSource.getRoot().getPath();
        inputStream = new FileInputStream(sourceFolderPath+"/1.txt");

    }

    @After
    public void after() throws Exception {
    }

    @Test
    public void doTest() throws Exception {
        //pipe.registerForward(new PipeForward());
        pipe.setCharset("/*"); pipe.setDirectory(sourceFolderPath);
        pipe.setOutputType("String"); pipe.setActions("read");
        pipe.setFileName("1.txt"); pipe.setFileSource("filesystem");
        pipe.configure(); pipe.doPipe(inputStream, session1);
    }

} 
