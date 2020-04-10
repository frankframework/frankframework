package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.SequenceInputStream;
import java.util.zip.ZipInputStream;

/**
 * UnzipPipe Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Mar 20, 2020</pre>
 */
public class UnzipPipeTest extends PipeTestBase<UnzipPipe> {

    @Override
    public UnzipPipe createPipe() {
        return new UnzipPipe();
    }
    @Mock
    private IPipeLineSession session1 = new PipeLineSessionBase();

    @ClassRule
    public static TemporaryFolder testFolderSource = new TemporaryFolder();

    private static String sourceFolderPath;

    private String folderPath = "Pipes/a.xquery";

    @ClassRule
    public static TemporaryFolder testFolderDest = new TemporaryFolder();

    private static FileInputStream fis1;
    private static FileInputStream fis2;

    private static SequenceInputStream sis;
    private static ZipInputStream zis;

    private static String destFolderPath = "/resources/Pipes";

    private static File newFile;
    private static File newFile2;

    @BeforeClass
    public static void beforeClass() throws Exception {
        sourceFolderPath = testFolderSource.getRoot().getPath();
        destFolderPath = testFolderDest.getRoot().getPath();
        newFile = testFolderSource.newFile("1.txt");
        FileWriter fw = new FileWriter(newFile);
        fw.write("asdfdf");
        fw.close();


    }

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipe() throws Exception {
        pipe.setDirectory(destFolderPath);
        pipe.configure();
        PipeRunResult res = pipe.doPipe(newFile.getPath(), session1);
        String reso = res.getResult().toString();
    }




} 
