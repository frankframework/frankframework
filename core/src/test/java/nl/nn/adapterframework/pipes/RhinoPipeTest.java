package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * RhinoPipe Tester.
 *
 * @author <Sina Sen>
 */
public class RhinoPipeTest extends PipeTestBase<RhinoPipe> {

    private String fileName = "/Pipes/javascript/rhino-test.js";

    @Override
    public RhinoPipe createPipe() {
        return new RhinoPipe();
    }




    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipe() throws Exception {
        pipe.setFileName(fileName);
        pipe.setjsfunctionName("giveNumber");
        pipe.setjsfunctionArguments("3");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "3", session);
        assertEquals("9", res.getResult().asString());
    }

    @Test
    public void testDoPipeLookupAtRuntime() throws Exception {
        pipe.setFileName(fileName);
        pipe.setjsfunctionName("giveNumber");
        pipe.setjsfunctionArguments("2");
        pipe.setLookupAtRuntime(true);
        PipeRunResult res = doPipe(pipe, "3", session);
        assertEquals( "9", res.getResult().asString());
    }

    @Test
    public void testDoPipeFailNoFilename() throws Exception {
        exception.expectMessage("Pipe [RhinoPipe under test] has neither fileName nor inputString specified");
        pipe.setjsfunctionName("giveNumber"); pipe.setjsfunctionArguments("2");
        pipe.configure();
        doPipe(pipe, "3", session);
        fail("this is expected to fail");

    }

    @Test
    public void testDoPipeAsFunctionNotSpecified() throws Exception {
        exception.expectMessage("Pipe [RhinoPipe under test] JavaScript functionname not specified!");
        pipe.setFileName(fileName);
        pipe.setjsfunctionArguments("2");
        pipe.configure();
        doPipe(pipe, "3", session);
        fail("this is expected to fail");

    }
    @Test
    public void testDoPipeFailAsWrongFileName() throws Exception {
        exception.expectMessage("Pipe [RhinoPipe under test] cannot find resource [random]");
        pipe.setFileName("random");
        pipe.setjsfunctionName("giveNumber");
        pipe.setjsfunctionArguments("3");
        pipe.configure();
        doPipe(pipe, "3", session);
        fail("this is expected to fail");
    }


    @Test
    public void testDoPipeLookupAtRuntimeFailAsWrongFileName() throws Exception {
        exception.expectMessage("Pipe [RhinoPipe under test] msgId [null] cannot find resource [wrong name]");
        pipe.setFileName("wrong name");
        pipe.setjsfunctionName("giveNumber");
        pipe.setjsfunctionArguments("2");
        pipe.setLookupAtRuntime(true);
        doPipe(pipe, "3", session);
        fail("this is expected to fail");
    }

    @Test
    public void testDoPipeFailAsWrongInputType() throws Exception {
        pipe.setFileName(fileName);
        pipe.setjsfunctionName("giveNumber");
        pipe.setjsfunctionArguments("3");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, 4 + "s", session);
        assertEquals("NaN", res.getResult().asString());
    }

}
