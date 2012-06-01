/*
 * $Log: AbstractResultHandler.java,v $
 * Revision 1.18  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.17  2011/11/30 13:51:56  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.15  2010/01/27 13:33:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added attribute blockByRecordType
 *
 * Revision 1.14  2008/02/19 09:23:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.13  2007/09/24 14:55:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.ClassUtils;
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
 * <tr><td>{@link #setDefault(boolean) default}</td><td>If set <code>true</code>, this ResultHandler is the default for all {@link RecordHandlingFlow flow}s that do not have a handler specified</td><td>false</td></tr>
 * <tr><td>{@link #setBlockByRecordType(boolean) blockByRecordType}</td><td>when set <code>true</code>(default), every group of records, as indicated by {@link IRecordHandler.isNewRecordType RecordHandler.newRecordType} is handled as a block.</td><td>true</td></tr>
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
	private boolean blockByRecordType=true;
	
	protected ParameterList paramList = null;

	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
		if (StringUtils.isNotEmpty(getPrefix()) || StringUtils.isNotEmpty(getSuffix())) {
			ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			configWarnings.add(ClassUtils.nameOf(this)+" ["+getName()+"]: the use of attributes prefix and suffix has been replaced by 'blocks'. Please replace with 'onBlockOpen' and 'onBlockClose', respectively");	 
		}
	}
	public void open() throws SenderException {
	}
	public void close() throws SenderException {
	}

	public void openDocument(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
	}
	public void closeDocument(IPipeLineSession session, String streamId, ParameterResolutionContext prc) {
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

	public void setBlockByRecordType(boolean b) {
		blockByRecordType = b;
	}
	public boolean isBlockByRecordType() {
		return blockByRecordType;
	}
}
