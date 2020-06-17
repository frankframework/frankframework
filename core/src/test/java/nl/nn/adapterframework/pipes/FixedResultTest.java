package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
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
 * FixedResult Tester.
 *
 * @author <Sina Sen>
 */
public class FixedResultTest extends PipeTestBase<FixedResult> {

    @ClassRule
    public static TemporaryFolder testFolderSource = new TemporaryFolder();

    private static String sourceFolderPath;
    @Mock
    private IPipeLineSession session1 = new PipeLineSessionBase();

    @Override
    public FixedResult createPipe() {
        return new FixedResult();
    }

    @BeforeClass
    public static void before() throws Exception {
        sourceFolderPath = "/Pipes/2.txt";
        testFolderSource.newFile("2.txt");

    }

    public static Parameter setUp(IPipeLineSession session1){
        Parameter param = new Parameter();
        param.setName("param1");
        param.setValue("abs");
        param.setSessionKey("*");
        session1.put("param1","yarr");
        return param;
    }



    /**
     * Method: configure()
     */
    @Test
    public void testSuccess() throws Exception {
        Parameter param = setUp(session1);
        pipe.addParameter(param);
        pipe.setLookupAtRuntime(true);
        pipe.setFileName(sourceFolderPath);
        pipe.setReplaceFrom("param1");
        pipe.setReplaceTo("kar");
        pipe.setReturnString("${param1}andandandparam2");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "whatisthis", session1);
        assertEquals("inside the file", res.getResult().asString());
    }

    @Test
    public void testFailAsWrongDirectory() throws Exception {
        exception.expectMessage("Pipe [FixedResult under test] cannot find resource [/Pipes/2.txt/something]");
        Parameter param = setUp(session1);
        pipe.addParameter(param);
        pipe.setFileName(sourceFolderPath + "/something");
        pipe.setReplaceFrom("param1");
        pipe.setReplaceTo("kar");
        pipe.setReturnString("${param1}andandandparam2");
        pipe.configure();
        doPipe(pipe, "whatisthis", session1);
        fail("this is expected to fail");
    }

    @Test
    public void testEmptyFileName() throws Exception{
        exception.expectMessage("Pipe [FixedResult under test] has neither fileName nor fileNameSessionKey nor returnString specified");
        pipe.configure();
        fail("this should fail");
    }

    @Test
    public void xsltSuccess() throws Exception{
        Parameter param = setUp(session1);
        pipe.addParameter(param);
        pipe.setSubstituteVars(true);
        pipe.addParameter(param);
        pipe.setLookupAtRuntime(true);
        pipe.setStyleSheetName("/Xslt/importNotFound/name.xsl");
        pipe.setReplaceFrom("param1");
        pipe.setReplaceTo("kar");
        pipe.setReturnString("${param1}andandandparam2");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "whatisthis", session1);
        assertEquals("success", res.getPipeForward().getName());
    }
  
    @Test
    public void xsltFailForTransformation() throws Exception{
        exception.expect(PipeRunException.class);
        Parameter param = setUp(session1);
        pipe.addParameter(param);
        pipe.addParameter(param);
        pipe.setLookupAtRuntime(true);
        pipe.setStyleSheetName("/Xslt/importNotFound/name2.xsl");
        pipe.setReplaceFrom("param1");
        pipe.setReplaceTo("kar");
        pipe.setReturnString("${param1}andandandparam2");
        pipe.configure();
        doPipe(pipe, "whatisthis", session1);
        fail("this is expected to fail");
    }

    @Test
    public void xsltFailForFindingFileButSuceed() throws Exception{
        Parameter param = setUp(session1);
        pipe.addParameter(param);
        pipe.addParameter(param);
        pipe.setLookupAtRuntime(true);
        pipe.setStyleSheetName("/Xslt/importNsddsotFound/namdsfe2.xsl");
        pipe.setReplaceFrom("param1");
        pipe.setReplaceTo("kar"); pipe.setReturnString("${param1}andandandparam2");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "whatisthis", session1);
        assertEquals("success", res.getPipeForward().getName());
    }




}
