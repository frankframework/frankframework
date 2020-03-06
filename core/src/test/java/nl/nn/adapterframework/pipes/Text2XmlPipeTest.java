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


}
