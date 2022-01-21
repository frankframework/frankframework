/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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
package nl.nn.adapterframework.core;

import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Getter;

/**
 * Bean that knows a functional name of a Forward, to be referred by
 * other Pipes and the PipeLine.
 * <br/>
 * The <code>name</code> is the name which a Pipe may lookup to return
 * to the PipeLine, indicating the next pipe to be executed. (this is done
 * in the {@link nl.nn.adapterframework.pipes.AbstractPipe#findForward(String) findForward()}-method. The actual
 * pipeName is defined in the <code>path</code> property.<br/><br/>
 * In this manner it is possible to influence the flow through the pipeline
 * without affecting the Java-code. Simply change the forwarding-XML.<br/>
 *
 * @author Johan Verrips
 * @see PipeLine
 * @see nl.nn.adapterframework.pipes.AbstractPipe#findForward
 */
public class PipeForward {

	public final static String SUCCESS_FORWARD_NAME = "success";
	public final static String EXCEPTION_FORWARD_NAME = "exception";

	private @Getter String name;
	private @Getter String path;

	public PipeForward(String name, String path) {
		this.name = name;
		this.path = path;
	}

	public PipeForward() {

	}

	/**
	 * the <code>name</code> is a symbolic reference to a <code>path</code>.<br/>
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The path is the name of the Pipe to execute/store
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * uses reflection to return the value
	 */
	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}
}
