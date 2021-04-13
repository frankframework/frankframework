/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.PipeLineSession;
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

	private @Getter String name;
	private @Getter String prefix;
	private @Getter String suffix;
	private boolean defaultResultHandler;
	private @Getter boolean blockByRecordType=true;
	private @Getter AbstractPipe pipe;
	
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
	public void openDocument(PipeLineSession session, String streamId) throws Exception {
	}
	@Override
	public void closeDocument(PipeLineSession session, String streamId) {
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

	@Override
	public void setPipe(AbstractPipe pipe) {
		this.pipe = pipe;
	}

	@IbisDoc({"1", "name of the resulthandler", ""})
	@Override
	public void setName(String string) {
		name = string;
	}

	@IbisDoc({"<i>deprecated</i> prefix that has to be written before record, if the record is in another block than the previous record", ""})
	@Deprecated
	public void setPrefix(String string) {
		prefix = string;
	}
	@Override
	public boolean hasPrefix() {
		return StringUtils.isNotEmpty(getPrefix());
	}

	@IbisDoc({"<i>deprecated</i> suffix that has to be written after the record, if the record is in another block than the next record. <br/>n.b. if a suffix is set without a prefix, it is only used at the end of processing (i.e. at the end of the file) as a final close", ""})
	@Deprecated
	public void setSuffix(String string) {
		suffix = string;
	}

	@IbisDoc({"2", "if set <code>true</code>, this resulthandler is the default for all {@link recordhandlingflow flow}s that do not have a handler specified", "false"})
	@Override
	public void setDefault(boolean isDefault) {
		this.defaultResultHandler = isDefault;
	}
	@Override
	public boolean isDefault() {
		return defaultResultHandler;
	}

	@IbisDoc({"when set <code>true</code>(default), every group of records, as indicated by {@link IRecordHandler#isNewRecordType(PipeLineSession, boolean, List, List) RecordHandler.newRecordType} is handled as a block.", "true"})
	public void setBlockByRecordType(boolean b) {
		blockByRecordType = b;
	}
}
