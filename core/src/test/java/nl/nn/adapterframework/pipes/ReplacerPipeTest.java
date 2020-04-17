package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.assertEquals;

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

    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
    }

    @Test
    public void everythingNull() throws Exception {
        exception.expect(ConfigurationException.class);
        exception.expectMessage("cannot have a null replace-attribute");
        pipe.setFind("laa");
        pipe.configure();
        doPipe(pipe, "", session);
    }

    @Test
    public void getFindEmpty() throws Exception {
        pipe.setFind("");
        pipe.configure();
        doPipe(pipe, "dsf", session);
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
        PipeRunResult res = doPipe(pipe, pipe.getFind(), session);
        assertEquals("sina\nmurat\nniels", pipe.getFind());
    }

    @Test
    public void ReplaceNonXMLChar() throws Exception{
        pipe.setFind("test");
        pipe.setReplace("head");
        pipe.setReplaceNonXmlChar("k");
        pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "<test>lolo</test>/jacjac:)", session);
        assertEquals("<head>lolo</head>/jacjac:)", res.getResult().toString());
    }

    @Test
    public void ReplaceStringSuccess() throws Exception{
        pipe.setFind("test");
        pipe.setReplace("head");
        pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "<test>lolo</test>/jacjac:)", session);
        assertEquals("<head>lolo</head>/jacjac:)", res.getResult().toString());
    }

    @Test
    public void ReplaceNonXMLCharLongerThanOne() throws Exception{
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
