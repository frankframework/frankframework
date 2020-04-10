package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.mockito.Mock;
import static org.junit.Assert.assertEquals;
import org.mockito.internal.configuration.injection.PropertyAndSetterInjection;

/**
 * FixedForwardPipe Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Mar 19, 2020</pre>
 */
public class FixedForwardPipeTest extends PipeTestBase<FixedForwardPipe> {
    @Mock
    private IPipeLineSession session1 = new PipeLineSessionBase();
    @Override
    public FixedForwardPipe createPipe() {
        return new FixedForwardPipe();
    }





    /**
     * Method: getParameterValue(ParameterValueList pvl, String parameterName)
     */
    @Test
    public void testDoPipeSuccess() throws Exception {
        Parameter p = new Parameter();
        p.setSessionKey("key"); p.setName("p1"); p.setValue("15"); p.setType("int"); p.configure();
        session1.put("key", p);
        PipeForward fw = new PipeForward();
        fw.setName("test");
        pipe.setIfParam("p1"); pipe.setForwardName("test"); pipe.registerForward(fw);
        pipe.addParameter(p);
        pipe.configure();
        PipeRunResult res = pipe.doInitialPipe("p1", session1);
        assertEquals(res.getResult(), "p1");
        assertEquals(res.getPipeForward(), fw);
    }

    @Test
    public void testDoPipeFailAsParamNotConfigured() throws Exception {
        exception.expectMessage("Pipe [FixedForwardPipe under test] msgId [null] exception on extracting parameters: Parameter [p1] not configured");
        exception.expect(PipeRunException.class);
        Parameter p = new Parameter();
        p.setSessionKey("key"); p.setName("p1"); p.setValue("15"); p.setType("int");
        session1.put("key", p);
        PipeForward fw = new PipeForward();
        fw.setName("test");
        pipe.setIfParam("p1"); pipe.setForwardName("test"); pipe.registerForward(fw);
        pipe.addParameter(p);
        PipeRunResult res = pipe.doInitialPipe("p1", session1);
    }

    /**
     * Method: configure()
     */
    @Test
    public void testSkipOnEmptyInput() throws Exception {
        Parameter p = new Parameter();
        p.setSessionKey("key"); p.setName("p1"); p.setValue("15"); p.setType("int"); p.configure();
        session1.put("key", p);
        PipeForward fw = new PipeForward();
        fw.setName("test");
        pipe.setIfParam("p1"); pipe.setForwardName("test"); pipe.registerForward(fw);
        pipe.setSkipOnEmptyInput(true);
        pipe.addParameter(p);
        pipe.configure();
        PipeRunResult res = pipe.doInitialPipe("", session1);
        assertEquals(res.getResult(), "");

    }

    /**
     * Method: doInitialPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testIfValueEqualsIfParam() throws Exception {
        Parameter p = new Parameter();
        p.setSessionKey("key"); p.setName("p1"); p.setValue("15"); p.setType("int"); p.configure();
        session1.put("key", p);
        PipeForward fw = new PipeForward();
        fw.setName("test");
        pipe.setIfParam("p1"); pipe.setForwardName("test"); pipe.registerForward(fw);
        pipe.addParameter(p); pipe.setIfValue("Parameter name=[p1] defaultValue=[null] sessionKey=[key] sessionKeyXPath=[null] xpathExpression=[null] type=[int] value=[15]");
        pipe.configure();
        PipeRunResult res = pipe.doInitialPipe("p1", session1);
        assertEquals(res, null);
    }


}
