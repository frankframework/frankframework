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
 * @version $Id$
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
