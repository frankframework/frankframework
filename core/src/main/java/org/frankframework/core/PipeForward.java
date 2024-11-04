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
package org.frankframework.core;

import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Getter;

import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;
import org.frankframework.doc.Mandatory;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.pipes.FixedResultPipe;

/**
 * Appears inside a pipe and defines what pipe or exit to execute next. When the
 * execution of a pipe is done, the pipe looks up the next pipe or exit to execute.
 * This pipe or exit is searched based on a key that describes what happened during
 * pipe execution. For example a {@link FixedResultPipe} searches for key
 * <code>filenotfound</code> if it tried to read a file that did not exist,
 * preventing it from producing the desired output message. If there was
 * no error, the {@link FixedResultPipe} searches for key <code>success</code>.
 * <br/><br/>
 * Each <code>&lt;Forward&gt;</code> tag is used to link a search key (<code>name</code> attribute)
 * to a pipe or exit to execute next (<code>path</code> attribute). The forward's <code>path</code>
 * attribute references the target pipe or exit by its <code>name</code> attribute, see
 * {@link AbstractPipe} and {@link PipeLineExit}. For most pipes and most keys, the next
 * pipe is executed if no forward is found. By default, the pipes in a pipeline are executed consecutively.
 *
 * @author Johan Verrips
 * @see PipeLine
 * @see AbstractPipe#findForward
 *
 */
// Looking up the next pipe or exit is done by method AbstractPipe.findForward(String)
@FrankDocGroup(FrankDocGroupValue.OTHER)
public class PipeForward {

	public static final String SUCCESS_FORWARD_NAME = "success";
	public static final String EXCEPTION_FORWARD_NAME = "exception";

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
	@Mandatory
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * The name of the next Pipe or Exit. When the Pipeline doesn't have an Exits element configured it will be
	 * initialized with one Exit having name READY and state SUCCESS
	 */
	@Mandatory
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
