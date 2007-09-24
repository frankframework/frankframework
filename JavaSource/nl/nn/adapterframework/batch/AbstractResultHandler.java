/*
 * $Log: AbstractResultHandler.java,v $
 * Revision 1.13  2007-09-24 14:55:33  europe\L190409
 * support for parameters
 *
 * Revision 1.12  2007/09/19 11:15:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added openDocument() and closeDocument()
 *
 * Revision 1.11  2007/09/17 08:24:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.10  2007/09/17 07:43:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added hasPrefix()
 *
 * Revision 1.9  2007/09/11 11:51:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.8  2007/09/10 11:11:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.7  2007/08/03 08:25:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added configure(), open() and close()
 * moved setDefault() to here
 *
 * Revision 1.6  2007/07/26 16:05:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.5  2006/05/19 09:28:36  Peter Eijgermans <peter.eijgermans@ibissource.org>
 * Restore java files from batch package after unwanted deletion.
 *
 * Revision 1.3  2005/10/31 14:38:02  John Dekker <john.dekker@ibissource.org>
 * Add . in javadoc
 *
 * Revision 1.2  2005/10/31 07:27:58  John Dekker <john.dekker@ibissource.org>
 * Resolves bug for writing suffix
 *
 * Revision 1.1  2005/10/11 13:00:21  John Dekker <john.dekker@ibissource.org>
 * New ibis file related elements, such as DirectoryListener, MoveFilePie and 
 * BatchFileTransformerPipe
 *
 */
package nl.nn.adapterframework.batch;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


/**
 * Abstract class for resulthandlers (handler that handles the transformed record).
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.batch.AbstractResultHandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>Name of the resulthandler</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPrefix(String) prefix}</td><td><i>Deprecated</i> Prefix that has to be written before record, if the record is in another block than the previous record</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSuffix(String) suffix}</td><td><i>Deprecated</i> Suffix that has to be written after the record, if the record is in another block than the next record. <br/>N.B. If a suffix is set without a prefix, it is only used at the end of processing (i.e. at the end of the file) as a final close</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefault(boolean) default}</td><td>If true, this resulthandler is the default for all RecordHandlingFlow that do not have a handler specified</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  John Dekker
 * @version Id
 */
public abstract class AbstractResultHandler implements IResultHandler, IWithParameters {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String prefix;
	private String suffix;
	private boolean defaultResultHandler;
	
	protected ParameterList paramList = null;

	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
	}
	public void open() throws SenderException {
	}
	public void close() throws SenderException {
	}

	public void openDocument(PipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
	}
	public void closeDocument(PipeLineSession session, String streamId, ParameterResolutionContext prc) {
	}

	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}


	public void setName(String string) {
		name = string;
	}
	public String getName() {
		return name;
	}

	public void setPrefix(String string) {
		prefix = string;
	}
	public String getPrefix() {
		return prefix;
	}
	public boolean hasPrefix() {
		return StringUtils.isNotEmpty(getPrefix());
	}

	public void setSuffix(String string) {
		suffix = string;
	}
	public String getSuffix() {
		return suffix;
	}

	public void setDefault(boolean isDefault) {
		this.defaultResultHandler = isDefault;
	}
	public boolean isDefault() {
		return defaultResultHandler;
	}

}
