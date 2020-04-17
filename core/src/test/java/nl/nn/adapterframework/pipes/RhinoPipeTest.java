package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * RhinoPipe Tester.
 *
 * @author <Sina Sen>
 */
public class RhinoPipeTest extends PipeTestBase<RhinoPipe> {

    private String fileName = "/Pipes/1.txt";

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
        assertEquals(res.getResult().toString(), "9");
    }

    @Test
    public void testDoPipeLookupAtRuntime() throws Exception {
        pipe.setFileName(fileName);
        pipe.setjsfunctionName("giveNumber");
        pipe.setjsfunctionArguments("2");
        pipe.setLookupAtRuntime(true);
        PipeRunResult res = doPipe(pipe, "3", session);
        assertEquals(res.getResult().toString(), "9");
    }

    @Test
    public void testDoPipeFailNoFilename() throws Exception {
        exception.expectMessage("Pipe [RhinoPipe under test] has neither fileName nor inputString specified");
        pipe.setjsfunctionName("giveNumber"); pipe.setjsfunctionArguments("2");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "3", session);
        assertFalse(res.getPipeForward().getName().isEmpty());

    }

    @Test
    public void testDoPipeAsFunctionNotSpecified() throws Exception {
        exception.expectMessage("Pipe [RhinoPipe under test] JavaScript functionname not specified!");
        pipe.setFileName(fileName);
        pipe.setjsfunctionArguments("2");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "3", session);
        assertFalse(res.getPipeForward().getName().isEmpty());

    }
    @Test
    public void testDoPipeFailAsWrongFileName() throws Exception {
        exception.expectMessage("Pipe [RhinoPipe under test] cannot find resource [random]");
        pipe.setFileName("random");
        pipe.setjsfunctionName("giveNumber");
        pipe.setjsfunctionArguments("3");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "3", session);
        assertEquals(res.getResult().toString(), "9");
    }


    @Test
    public void testDoPipeLookupAtRuntimeFailAsWrongFileName() throws Exception {
        exception.expectMessage("Pipe [RhinoPipe under test] cannot find resource [wrong name]");
        pipe.setFileName("wrong name");
        pipe.setjsfunctionName("giveNumber");
        pipe.setjsfunctionArguments("2");
        pipe.setLookupAtRuntime(true);
        PipeRunResult res = doPipe(pipe, "3", session);
        assertEquals(res.getResult().toString(), "9");
    }

    @Test
    public void testDoPipeFailAsWrongInputType() throws Exception {
        exception.expectMessage("expected:<[16]> but was:<[NaN]>");
        pipe.setFileName(fileName);
        pipe.setjsfunctionName("giveNumber");
        pipe.setjsfunctionArguments("3");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, 4, session);
        assertEquals(res.getResult().toString(), "NaN");

    }

}
