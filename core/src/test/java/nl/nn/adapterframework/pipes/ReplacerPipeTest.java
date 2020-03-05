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
 * @version 1.0
 * @since <pre>Mar 5, 2020</pre>
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
        pipe.doPipe("", session);
    }

    @Test
    public void getFindEmpty() throws Exception {
        pipe.setFind("");
        pipe.configure();
        pipe.doPipe("dsf", session);
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
        PipeRunResult res = pipe.doPipe(pipe.getFind(), session);
        assertEquals("sina\nmurat\nniels", pipe.getFind());
    }

    @Test
    public void ReplaceNonXMLChar() throws Exception{
        pipe.setFind("test");
        pipe.setReplace("head");
        pipe.setReplaceNonXmlChar("k");
        pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = pipe.doPipe("<test>lolo</test>/jacjac:)", session);
        assertEquals("<head>lolo</head>/jacjac:)", res.getResult().toString());
    }

    @Test
    public void ReplaceStringSuccess() throws Exception{
        pipe.setFind("test");
        pipe.setReplace("head");
        pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = pipe.doPipe("<test>lolo</test>/jacjac:)", session);
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
        PipeRunResult res = pipe.doPipe("<test>lolo</test>/jacjac:)", session);
        assertEquals("<head>lolo</head>/jacjac:)", res.getResult().toString());
    }

    /**
     * Method: replace(String target, String from, String to)
     */
    @Test
    public void testReplace() throws Exception {
        String res = pipe.replace("test12345", "test", "sadsd");
        assertEquals(res, "sadsd12345");
    }

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testDoPipe() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setFind(String find)
     */
    @Test
    public void testSetFind() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: getFind()
     */
    @Test
    public void testGetFind() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setReplace(String replace)
     */
    @Test
    public void testSetReplace() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: getReplace()
     */
    @Test
    public void testGetReplace() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: getLineSeparatorSymbol()
     */
    @Test
    public void testGetLineSeparatorSymbol() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setLineSeparatorSymbol(String string)
     */
    @Test
    public void testSetLineSeparatorSymbol() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setReplaceNonXmlChars(boolean b)
     */
    @Test
    public void testSetReplaceNonXmlChars() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: isReplaceNonXmlChars()
     */
    @Test
    public void testIsReplaceNonXmlChars() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setReplaceNonXmlChar(String replaceNonXmlChar)
     */
    @Test
    public void testSetReplaceNonXmlChar() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: getReplaceNonXmlChar()
     */
    @Test
    public void testGetReplaceNonXmlChar() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setAllowUnicodeSupplementaryCharacters(boolean b)
     */
    @Test
    public void testSetAllowUnicodeSupplementaryCharacters() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: isAllowUnicodeSupplementaryCharacters()
     */
    @Test
    public void testIsAllowUnicodeSupplementaryCharacters() throws Exception {
//TODO: Test goes here... 
    }



}
