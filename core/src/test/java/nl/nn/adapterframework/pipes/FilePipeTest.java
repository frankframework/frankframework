package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;



import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * FilePipe Tester.
 *
 * @author <Sina Sen>
 */
public class FilePipeTest extends PipeTestBase<FilePipe> {

    @ClassRule
    public static TemporaryFolder testFolderSource = new TemporaryFolder();

    private static String sourceFolderPath;

    @Mock
    private IPipeLineSession session1 = new PipeLineSessionBase();

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
        Parameter p = new Parameter();
        p.setSessionKey("key"); p.setName("p1"); p.setValue("15"); p.setType("int"); p.configure();
        session1.put("key", p);
        PipeForward fw = new PipeForward();
        fw.setName("test");
        pipe.registerForward(fw);
        pipe.addParameter(p);
        pipe.setCharset("/");
        pipe.setDirectory(sourceFolderPath);
        pipe.setOutputType("stream");
        pipe.setActions("read");
        pipe.setFileName("1.txt");
        pipe.setFileSource("filesystem");
        pipe.setActions("create");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, var, session1);

        assertEquals("success", res.getPipeForward().getName());

    }

    @Test
    public void doTestFailAsEncodingNotSupportedBase64() throws Exception {
        exception.expect(PipeRunException.class);
        exception.expectMessage("Pipe [FilePipe under test] msgId [null] Error while executing file action(s): (UnsupportedEncodingException) /");
        Parameter p = new Parameter();
        p.setSessionKey("key");
        p.setName("p1");
        p.setValue("15");
        p.setType("int");
        p.configure();
        session1.put("key", p);
        PipeForward fw = new PipeForward();
        fw.setName("test");
        pipe.registerForward(fw);
        pipe.addParameter(p);
        pipe.setCharset("/");
        pipe.setDirectory(sourceFolderPath);
        pipe.setOutputType("base64");
        pipe.setActions("read");
        pipe.setFileName("1.txt");
        pipe.setFileSource("filesystem");
        pipe.setActions("create");
        pipe.configure();
        doPipe(pipe, var, session1);
        fail("this will fail");
    }

} 
