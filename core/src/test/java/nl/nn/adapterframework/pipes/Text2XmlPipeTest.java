package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


/**
 * Text2XmlPipe Tester.
 *
 * @author <Sina Sen>
 */
public class Text2XmlPipeTest extends PipeTestBase<Text2XmlPipe> {

    @Override
    public Text2XmlPipe createPipe() {
        return new Text2XmlPipe();
    }

    /**
     * Method: doPipe(Object input, PipeLineSession session)
     */
    @Test
    public void testSuccessCDataAndReplaceNonXMLSplitLines() throws Exception {
        pipe.setXmlTag("address");
        pipe.setSplitLines(true);
        pipe.setUseCdataSection(true);
        pipe.setIncludeXmlDeclaration(true);
        pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "this is an example\nim in cdata", session);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><address><line><![CDATA[this is an example]]></line><line><![CDATA[im in cdata]]></line></address>", res.getResult().asString());
    }

    @Test
    public void testSuccessCDataAndXMLDeclaration() throws Exception {
        pipe.setXmlTag("address");
        pipe.setSplitLines(false);
        pipe.setUseCdataSection(false);
        pipe.setIncludeXmlDeclaration(true);
        pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "this is an example\nim not in cdata", session);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><address>this is an example\n" +
                "im not in cdata</address>", res.getResult().asString());
    }

    @Test
    public void testSuccessWithoutAdditionalProperties() throws Exception {
        pipe.setXmlTag("address");
        pipe.setReplaceNonXmlChars(true);
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "this will be in cdata tag\b", session);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><address><![CDATA[this will be in cdata tag¿]]></address>", res.getResult().asString());
    }

    /**
     * Method: doPipe(Object input, PipeLineSession session)
     */
    @Test
    public void testSuccessSplitWithoutReplacingNonXMLChars() throws Exception {
        pipe.setXmlTag("address"); pipe.setSplitLines(true); pipe.setUseCdataSection(true);
        pipe.setIncludeXmlDeclaration(true); pipe.setReplaceNonXmlChars(false); pipe.configure();
        PipeRunResult res = doPipe(pipe, "this is an example\nim in cdata", session);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><address><line><![CDATA[this is an example]]></line><line><![CDATA[im in cdata]]></line></address>", res.getResult().asString());
    }


    /**
     * Method: getXmlTag()
     */
    @Test
    public void testEmptyXmlTag() throws Exception {
        exception.expect(ConfigurationException.class);
        exception.expectMessage("You have not defined xmlTag");
        pipe.configure();
        PipeRunResult res = doPipe(pipe, "bara", session);
        assertFalse(res.getPipeForward().getName().isEmpty());

    }



}
