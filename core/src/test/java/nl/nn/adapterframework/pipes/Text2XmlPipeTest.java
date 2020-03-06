package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import static org.junit.Assert.assertEquals;

/**
 * Text2XmlPipe Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>Mar 6, 2020</pre>
 */
public class Text2XmlPipeTest extends PipeTestBase<Text2XmlPipe> {

    @Override
    public Text2XmlPipe createPipe() {
        return new Text2XmlPipe();
    }

    @Before
    public void before() throws Exception {
    }

    @After
    public void after() throws Exception {
    }

    /**
     * Method: configure()
     */
    @Test
    public void testConfigure() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testSuccessCDataAndReplaceNonXMLSplitLines() throws Exception {
        pipe.setXmlTag("address"); pipe.setSplitLines(true); pipe.setUseCdataSection(true);
        pipe.setIncludeXmlDeclaration(true); pipe.setReplaceNonXmlChars(true); pipe.configure();
        PipeRunResult res = pipe.doPipe("this is an example\nim in cdata", session);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><address><line><![CDATA[this is an example]]></line><line><![CDATA[im in cdata]]></line></address>", res.getResult().toString());
    }

    @Test
    public void testSuccessCDataAndXMLDeclaration() throws Exception {
        pipe.setXmlTag("address"); pipe.setSplitLines(false); pipe.setUseCdataSection(false);
        pipe.setIncludeXmlDeclaration(true); pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = pipe.doPipe("this is an example\nim in cdata", session);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><address>this is an example\n" +
                "im in cdata</address>", res.getResult().toString());
    }

    @Test
    public void testSuccessWithoutAdditionalProperties() throws Exception {
        pipe.setXmlTag("address"); pipe.setSplitLines(false); pipe.setUseCdataSection(false);
        pipe.setIncludeXmlDeclaration(true); pipe.setReplaceNonXmlChars(false);
        pipe.configure();
        PipeRunResult res = pipe.doPipe("this is an example\nim in cdata", session);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><address>this is an example\n" +
                "im in cdata</address>", res.getResult().toString());
    }

    /**
     * Method: doPipe(Object input, IPipeLineSession session)
     */
    @Test
    public void testSuccessSplitWithoutReplacingNonXMLChars() throws Exception {
        pipe.setXmlTag("address"); pipe.setSplitLines(true); pipe.setUseCdataSection(true);
        pipe.setIncludeXmlDeclaration(true); pipe.setReplaceNonXmlChars(false); pipe.configure();
        PipeRunResult res = pipe.doPipe("this is an example\nim in cdata", session);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><address><line><![CDATA[this is an example]]></line><line><![CDATA[im in cdata]]></line></address>", res.getResult().toString());
    }


    /**
     * Method: getXmlTag()
     */
    @Test
    public void testEmptyXmlTag() throws Exception {
        exception.expect(ConfigurationException.class);
        exception.expectMessage("You have not defined xmlTag");
        pipe.configure(); pipe.doPipe("bara", session);
    }

    /**
     * Method: setXmlTag(String xmlTag)
     */
    @Test
    public void testSetXmlTag() throws Exception {
        pipe.setSplitLines(true);
        pipe.doPipe(new Text2XmlPipe(), session);
    }

    @Test
    public void nullInput() throws Exception {
        pipe.setXmlTag("balltype"); pipe.setIncludeXmlDeclaration(false); pipe.setSplitLines(false);
        pipe.doPipe(null, session);

    }

    /**
     * Method: isIncludeXmlDeclaration()
     */
    @Test
    public void testIsIncludeXmlDeclaration() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setIncludeXmlDeclaration(boolean b)
     */
    @Test
    public void testSetIncludeXmlDeclaration() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: isSplitLines()
     */
    @Test
    public void testIsSplitLines() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setSplitLines(boolean b)
     */
    @Test
    public void testSetSplitLines() throws Exception {
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
     * Method: isUseCdataSection()
     */
    @Test
    public void testIsUseCdataSection() throws Exception {
//TODO: Test goes here... 
    }

    /**
     * Method: setUseCdataSection(boolean b)
     */
    @Test
    public void testSetUseCdataSection() throws Exception {
//TODO: Test goes here... 
    }


    /**
     * Method: addCdataSection(String input)
     */
    @Test
    public void testAddCdataSection() throws Exception {
//TODO: Test goes here... 
/* 
try { 
   Method method = Text2XmlPipe.getClass().getMethod("addCdataSection", String.class); 
   method.setAccessible(true); 
   method.invoke(<Object>, <Parameters>); 
} catch(NoSuchMethodException e) { 
} catch(IllegalAccessException e) { 
} catch(InvocationTargetException e) { 
} 
*/
    }


}
