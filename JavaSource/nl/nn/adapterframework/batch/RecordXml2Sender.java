/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.batch;

import java.util.List;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Translate a record into XML, then send it using a sender.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.RecordXml2Sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the RecordHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputFields(String) inputFields}</td><td>Comma separated specification of fieldlengths. If neither this attribute nor <code>inputSeparator</code> is specified then the entire record is parsed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputSeparator(String) inputSeparator}</td><td>Separator that separated the fields in the input record. If neither this attribute nor <code>inputFields</code> is specified then the entire record is parsed</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTrim(boolean) trim}</td><td>when set <code>true</code>, trailing spaces are removed from each field</td><td>false</td></tr>
 * <tr><td>{@link #setRootTag(String) rootTag}</td><td>Roottag for the generated XML document that will be send to the Sender</td><td>record</td></tr>
 * <tr><td>{@link #setOutputFields(String) outputfields}</td><td>Comma separated string with tagnames for the individual input fields (related using there positions). If you leave a tagname empty, the field is not xml-ized</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setRecordIdentifyingFields(String) recordIdentifyingFields}</td><td>Comma separated list of numbers of those fields that are compared with the previous record to determine if a prefix must be written. If any of these fields is not equal in both records, the record types are assumed to be different</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStyleSheetName(String) styleSheetName}</td><td>name of stylesheet to transform an individual record, before handing it to the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setXpathExpression(String) xpathExpression}</td><td>alternatively: XPath-expression to create stylesheet from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setOutputType(String) outputType}</td><td>either 'text' or 'xml'. Only valid for xpathExpression</td><td>text</td></tr>
 * <tr><td>{@link #setOmitXmlDeclaration(boolean) omitXmlDeclaration}</td><td>force the transformer generated from the XPath-expression to omit the xml declaration</td><td>true</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ISender sender}</td><td>Sender that needs to handle the (XML) record</td></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the recordHandler will be handed to the sender, if this is a {@link nl.nn.adapterframework.core.ISenderWithParameters ISenderWithParameters}</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker
 * @version $Id$
 */
public class RecordXml2Sender extends RecordXmlTransformer {
	public static final String version = "$RCSfile: RecordXml2Sender.java,v $  $Revision: 1.18 $ $Date: 2012-06-01 10:52:48 $";

	private ISender sender = null; 
	
	public void configure() throws ConfigurationException {
		super.configure();
		if (sender==null) {
			throw new ConfigurationException(ClassUtils.nameOf(this)+" has no sender");
		}
		sender.configure();		
	}
	public void open() throws SenderException {
		super.open();
		sender.open();		
	}
	public void close() throws SenderException {
		super.close();
		sender.close();		
	}

	public Object handleRecord(IPipeLineSession session, List parsedRecord, ParameterResolutionContext prc) throws Exception {
		String xml = (String)super.handleRecord(session,parsedRecord,prc);
		ISender sender = getSender();
		if (sender instanceof ISenderWithParameters) {
			ISenderWithParameters psender = (ISenderWithParameters)sender;
			return psender.sendMessage(session.getMessageId(), xml,prc); 
		} else {
			return sender.sendMessage(session.getMessageId(), xml); 
		}
	}
	


	public void setSender(ISender sender) {
		this.sender = sender;
	}
	public ISender getSender() {
		return sender;
	}

}
