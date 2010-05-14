/*
 * $Log: RecordXslTransformer.java,v $
 * Revision 1.15  2010-05-14 16:52:00  L190409
 * corrected deprecation warning
 *
 * Revision 1.14  2010/05/03 16:58:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * deprecated
 *
 * Revision 1.13  2008/12/30 17:01:13  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added configuration warnings facility (in Show configurationStatus)
 *
 * Revision 1.12  2008/07/17 16:13:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.11  2008/02/28 16:17:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * move xslt functionality to base class RecordXmlTransformer
 *
 * Revision 1.10  2008/02/19 09:23:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.9  2008/02/15 16:05:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.8  2007/10/08 13:28:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.7  2007/09/24 14:55:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

/**
 * Translate a record using XSL.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.RecordXslTransformer</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputFields(String) inputFields}</td><td>Comma separated specification of fieldlengths. If neither this attribute nor <code>inputSeparator</code> is specified then the entire record is parsed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputSeparator(String) inputSeparator}</td><td>Separator that separated the fields in the input record. If neither this attribute nor <code>inputFields</code> is specified then the entire record is parsed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTrim(boolean) trim}</td><td>when set <code>true</code>, trailing spaces are removed from each field</td><td>false</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document that will be send to the Sender</td><td>record</td></tr>
 * <tr><td>{@link #setOutputFields(String) outputfields}</td><td>Comma seperated string with tagnames for the individual input fields (related using there positions). If you leave a tagname empty, the field is not xml-ized</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRecordIdentifyingFields(String) recordIdentifyingFields}</td><td>Comma separated list of numbers of those fields that are compared with the previous record to determine if a prefix must be written. If any of these fields is not equal in both records, the record types are assumed to be different</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>name of stylesheet to transform an individual record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>alternatively: XPath-expression to create stylesheet from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either 'text' or 'xml'. Only valid for xpathExpression</td><td>text</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) omitXmlDeclaration}</td><td>force the transformer generated from the XPath-expression to omit the xml declaration</td><td>true</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker
 * @version Id
 * @deprecated Please replace by RecordXmlTransformer.
 */
public class RecordXslTransformer extends RecordXmlTransformer {
	public static final String version = "$RCSfile: RecordXslTransformer.java,v $  $Revision: 1.15 $ $Date: 2010-05-14 16:52:00 $";

	public void configure() throws ConfigurationException {
		super.configure();
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = "class ["+this.getClass().getName()+"] is deprecated. Please replace by [nl.nn.adapterframework.batch.RecordXmlTransformer]";
		configWarnings.add(log, msg);
	}

	/**
	 * @deprecated configuration using attribute 'xslFile' is deprecated. Please use attribute 'styleSheetName' 
	 */
	public void setXslFile(String xslFile) {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = "configuration using attribute 'xslFile' is deprecated. Please use attribute 'styleSheetName'";
		configWarnings.add(log, msg);
		setStyleSheetName(xslFile);
	}

}
