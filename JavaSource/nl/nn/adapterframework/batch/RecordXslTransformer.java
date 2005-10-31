/*
 * $Log: RecordXslTransformer.java,v $
 * Revision 1.3  2005-10-31 14:38:02  europe\m00f531
 * Add . in javadoc
 *
 * Revision 1.2  2005/10/27 12:32:18  John Dekker <john.dekker@ibissource.org>
 * *** empty log message ***
 *
 * Revision 1.1  2005/10/11 13:00:21  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import java.net.URL;
import java.util.ArrayList;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * Translate a record using XSL.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.ibis4fundation.transformation.RecordXslTransformer</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputFields(String) outputfields}</td><td>Comma seperated string with tagnames for the individual input fields (related using there positions). If you leave a tagname empty, the field is not xml-ized</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXslFile(String) xslFilename}</td><td>Filename of xsl to transform an individual record</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author: John Dekker
 */
public class RecordXslTransformer extends RecordXmlTransformer {
	public static final String version = "$RCSfile: RecordXslTransformer.java,v $  $Revision: 1.3 $ $Date: 2005-10-31 14:38:02 $";

	private TransformerPool transformer; 

	public RecordXslTransformer() {
	}

	public Object handleRecord(PipeLineSession session, ArrayList parsedRecord) throws Exception {
		String xml = getXml(parsedRecord);
		return transformer.transform(xml, null);
	}
	
	public void setXslFile(String xslFile) throws ConfigurationException {
		URL resource = ClassUtils.getResourceURL(this, xslFile);
		if (resource == null) {
			throw new ConfigurationException("Xsl file does not exist " + xslFile); 
		}
		try {
			transformer = new TransformerPool(resource);
			transformer.open();
		}
		catch(Exception e) {
			throw new ConfigurationException("Xsl file [" + xslFile + "] contains errors", e); 
		}
	}

}
