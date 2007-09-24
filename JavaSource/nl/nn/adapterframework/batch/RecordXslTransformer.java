/*
 * $Log: RecordXslTransformer.java,v $
 * Revision 1.7  2007-09-24 14:55:32  europe\L190409
 * support for parameters
 *
 * Revision 1.6  2007/07/26 16:11:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed seperator into separator
 * allow use of xPathExpression and parameters
 *
 * Revision 1.5  2006/05/19 09:28:36  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.3  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
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

import java.util.ArrayList;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.TransformerPool;

/**
 * Translate a record using XSL.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.RecordXslTransformer</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputFields(String) outputfields}</td><td>Comma seperated string with tagnames for the individual input fields (related using there positions). If you leave a tagname empty, the field is not xml-ized</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>name of stylesheet to transform an individual record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>alternatively: XPath-expression to create stylesheet from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either 'text' or 'xml'. Only valid for xpathExpression</td><td>text</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) omitXmlDeclaration}</td><td>force the transformer generated from the XPath-expression to omit the xml declaration</td><td>true</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker
 * @version Id
 */
public class RecordXslTransformer extends RecordXmlTransformer {
	public static final String version = "$RCSfile: RecordXslTransformer.java,v $  $Revision: 1.7 $ $Date: 2007-09-24 14:55:32 $";

	private String xpathExpression=null;
	private String styleSheetName;
	private String outputType="text";
	private boolean omitXmlDeclaration=true;

	private TransformerPool transformerPool; 
	private ParameterList parameterList = new ParameterList();

	public void configure() throws ConfigurationException {
		super.configure();
		ParameterList params = getParameterList();
		if (params!=null) {
			try {
				params.configure();
			} catch (ConfigurationException e) {
				throw new ConfigurationException("while configuring parameters",e);
			}
		}
		transformerPool = TransformerPool.configureTransformer(ClassUtils.nameOf(this)+" ["+getName()+"]", getXpathExpression(), getStyleSheetName(), getOutputType(), !isOmitXmlDeclaration(), getParameterList());
	}


	private ParameterList getParameterList() {
		return parameterList;
	}

	public void addParameter(Parameter param) {
		log.debug("added parameter ["+param.toString()+"]");
		parameterList.add(param);
	}


	public Object handleRecord(PipeLineSession session, ArrayList parsedRecord, ParameterResolutionContext prc) throws Exception {
		String xml = getXml(parsedRecord);
		return transformerPool.transform(xml, prc.getValueMap(paramList));
	}
	

	public void setXpathExpression(String string) {
		xpathExpression = string;
	}
	public String getXpathExpression() {
		return xpathExpression;
	}


	public void setOmitXmlDeclaration(boolean b) {
		omitXmlDeclaration = b;
	}
	public boolean isOmitXmlDeclaration() {
		return omitXmlDeclaration;
	}

	public void setOutputType(String string) {
		outputType = string;
	}
	public String getOutputType() {
		return outputType;
	}

	/**
	 * @deprecated configuration using attribute 'xslFile' is deprecated. Please use attribute 'styleSheetName' 
	 */
	public void setXslFile(String xslFile) {
		log.warn("configuration using attribute 'xslFile' is deprecated. Please use attribute 'styleSheetName'");
		setStyleSheetName(xslFile);
	}
	public void setStyleSheetName(String string) {
		styleSheetName = string;
	}
	public String getStyleSheetName() {
		return styleSheetName;
	}

}
