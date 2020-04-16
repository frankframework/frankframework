package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.filesystem.FileSystemActor;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.testutil.TestFileUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * FileLineIteratorPipe Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Mar 26, 2020</pre>
 */
public class FileLineIteratorPipeTest extends PipeTestBase<FileLineIteratorPipe> {
    @Mock
    private IPipeLineSession session1 = new PipeLineSessionBase();

    private static IOutputStreamingSupport fsActor = new FileSystemActor<>();


    private static Map<String,Object> threadContext;

    @ClassRule
    public static TemporaryFolder testFolderSource = new TemporaryFolder();

    private static String sourceFolderPath;

    private static File newFile;

    @ClassRule
    public static TemporaryFolder testFolderDest = new TemporaryFolder();
    ;

    private static String destFolderPath;

    @Override
    public FileLineIteratorPipe createPipe() {
        return new FileLineIteratorPipe();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        sourceFolderPath = testFolderSource.getRoot().getPath();
        destFolderPath = testFolderDest.getRoot().getPath();
        newFile = testFolderSource.newFile("1.txt");
        newFile.setReadable(true); newFile.setWritable(true);
        FileWriter fw = new FileWriter(newFile);
        fw.write("testest");
        fw.close();
        threadContext = new HashMap<>();
    }


    /**
     * Method: doPipe(Object input, IPipeLineSession session, IOutputStreamingSupport next)
     */
    @Test
    public void testDoPipe() throws Exception {
        pipe.setMove2dirAfterError(destFolderPath+"/error");
        pipe.setMove2dirAfterTransform(destFolderPath+"/success");
        pipe.doPipe(sourceFolderPath+"/1.txt", session1, fsActor);

    }



    @Test
    public void testGetReader() throws Exception{
        FileReader reader = (FileReader) pipe.getReader(newFile, session1,"sfd", threadContext);
        FileReader reader2 = new FileReader(newFile);
        int a = reader.read();
        assertEquals(a, 116);
    }


}
