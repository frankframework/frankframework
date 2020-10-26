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

import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.util.LogUtil;


/**
 * Abstract class for resulthandlers (handler that handles the transformed record).
 * 
 * 
 * @author  John Dekker
 */
public abstract class AbstractResultHandler implements IResultHandler, IWithParameters {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();

	private String name;
	private String prefix;
	private String suffix;
	private boolean defaultResultHandler;
	private boolean blockByRecordType=true;
	private AbstractPipe pipe;
	
	protected ParameterList paramList = null;

	@Override
	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
		if (StringUtils.isNotEmpty(getPrefix()) || StringUtils.isNotEmpty(getSuffix())) {
			ConfigurationWarnings.add(this, log, "the use of attributes prefix and suffix has been replaced by 'blocks'. Please replace with 'onBlockOpen' and 'onBlockClose', respectively", SuppressKeys.DEPRECATION_SUPPRESS_KEY, getPipe().getAdapter());	 
		}
	}
	@Override
	public void open() throws SenderException {
	}
	@Override
	public void close() throws SenderException {
	}

	@Override
	public void openDocument(IPipeLineSession session, String streamId) throws Exception {
	}
	@Override
	public void closeDocument(IPipeLineSession session, String streamId) {
	}

	@Override
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	@IbisDoc({"name of the resulthandler", ""})
	@Override
	public void setName(String string) {
		name = string;
	}
	@Override
	public String getName() {
		return name;
	}

	@IbisDoc({"<i>deprecated</i> prefix that has to be written before record, if the record is in another block than the previous record", ""})
	public void setPrefix(String string) {
		prefix = string;
	}
	public String getPrefix() {
		return prefix;
	}
	@Override
	public boolean hasPrefix() {
		return StringUtils.isNotEmpty(getPrefix());
	}

	@IbisDoc({"<i>deprecated</i> suffix that has to be written after the record, if the record is in another block than the next record. <br/>n.b. if a suffix is set without a prefix, it is only used at the end of processing (i.e. at the end of the file) as a final close", ""})
	public void setSuffix(String string) {
		suffix = string;
	}
	public String getSuffix() {
		return suffix;
	}

	@IbisDoc({"if set <code>true</code>, this resulthandler is the default for all {@link recordhandlingflow flow}s that do not have a handler specified", "false"})
	@Override
	public void setDefault(boolean isDefault) {
		this.defaultResultHandler = isDefault;
	}
	@Override
	public boolean isDefault() {
		return defaultResultHandler;
	}

	@IbisDoc({"when set <code>true</code>(default), every group of records, as indicated by {@link IRecordHandler#isNewRecordType(IPipeLineSession, boolean, List, List) RecordHandler.newRecordType} is handled as a block.", "true"})
	public void setBlockByRecordType(boolean b) {
		blockByRecordType = b;
	}
	@Override
	public boolean isBlockByRecordType() {
		return blockByRecordType;
	}

	@Override
	public void setPipe(AbstractPipe pipe) {
		this.pipe = pipe;
	}
	public AbstractPipe getPipe() {
		return pipe;
	}
}
