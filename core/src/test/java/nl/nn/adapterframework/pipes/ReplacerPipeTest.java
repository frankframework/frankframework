package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * ReplacerPipe Tester.
 *
 * @author <Sina Sen>
 */
public class ReplacerPipeTest extends PipeTestBase<ReplacerPipe>{

    @Override
    public ReplacerPipe createPipe() {
        return new ReplacerPipe();
    }


    @Test
    public void everythingNull() throws Exception {
        exception.expect(ConfigurationException.class);
        exception.expectMessage("cannot have a null replace-attribute");
        pipe.setFind("laa");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "", session);
        assertFalse(res.getPipeForward().getName().isEmpty());

    }

    @Test
    public void getFindEmpty() throws Exception {
        pipe.setFind("");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "dsf", session);
        assertFalse(res.getPipeForward().getName().isEmpty());

    }

    /**
     * Method: configure()
     */
    @Test
    public void testConfigureWithSeperator() throws Exception {
        pipe.setFind("sina/murat/niels");
        pipe.setLineSeparatorSymbol("/");
        pipe.setReplace("yo");
        pipe.setAllowUnicodeSupplementaryCharacters(true);
        pipe.configure();
        doPipe(pipe, pipe.getFind(), session);
        assertFalse( pipe.getFind().isEmpty());
    }

    @Test
    public void replaceNonXMLChar() throws Exception{
        pipe.setFind("test");
        pipe.setReplace("head");
        pipe.setReplaceNonXmlChar("k");
        pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "<test>lolo</test>/jacjac:)", session);
        assertEquals("String: <head>lolo</head>/jacjac:)", res.getResult().toString());
    }

    @Test
    public void replaceStringSuccess() throws Exception{
        pipe.setFind("test");
        pipe.setReplace("head");
        pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "<test>lolo</test>/jacjac:)", session);
        assertEquals("String: <head>lolo</head>/jacjac:)", res.getResult().toString());
    }

    @Test
    public void replaceNonXMLCharLongerThanOne() throws Exception{
        exception.expect(ConfigurationException.class);
        exception.expectMessage("replaceNonXmlChar [klkl] has to be one character");
        pipe.setFind("test");
        pipe.setReplace("head");
        pipe.setReplaceNonXmlChar("klkl");
        pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "<test>lolo</test>/jacjac:)", session);
        assertEquals("<head>lolo</head>/jacjac:)", res.getResult().toString());
    }





}
