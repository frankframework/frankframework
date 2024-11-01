/*
   Copyright 2013 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import lombok.Getter;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.doc.Category;
import org.frankframework.doc.FrankDocGroup;
import org.frankframework.doc.FrankDocGroupValue;

/**
 * The Exit of a Pipeline that specifies the end state of a PipeLine. The state is returned to the receiver as well as
 * the optionally specified http status code. Each Exit should have a unique name. See {@link PipeLineExits Exits}
 * for examples.
 * <br/><br/>
 * When a Pipeline doesn't have an Exits element configured it will be initialized with one Exit having name {@value PipeLine#DEFAULT_SUCCESS_EXIT_NAME} and
 * state {@value PipeLine.ExitState#SUCCESS_EXIT_STATE}.
 * <br/><br/>
 * The name of an Exit can be referenced by the <code>path</code> attribute of a Forward within a Pipe.
 *
 * @author Johan Verrips
 * @author Niels Meijer
 */
@Category(Category.Type.BASIC)
@FrankDocGroup(FrankDocGroupValue.OTHER)
public class PipeLineExit implements IForwardTarget {

	private @Getter String name;
	private @Getter ExitState state;
	private @Getter int exitCode = 0; // TODO this should become NULL
	private @Getter String responseRoot;
	private @Getter boolean emptyResult = false;
	private @Getter boolean skipValidation = false;
	private @Getter boolean skipWrapping = false;

	public boolean isSuccessExit() {
		return getState()==ExitState.SUCCESS;
	}

	/**
	 * The name of the Exit that can be referenced by a {@link PipeForward}'s <code>path</code> attribute. When a Pipeline doesn't have an Exits
	 * element configured it will be initialized with one Exit having name {@value PipeLine#DEFAULT_SUCCESS_EXIT_NAME} (and state {@link PipeLine.ExitState#SUCCESS SUCCESS})
	 * @ff.mandatory ignoreInCompatibilityMode
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Deprecated(forRemoval = true, since = "7.8.0")
	@ConfigurationWarning("The attribute 'path' has been renamed to: 'name'")
	public void setPath(String path) {
		setName(path);
	}

	/**
	 * The state of the Pipeline that is returned to the Receiver for this Exit. When a Pipeline doesn't have an Exits
	 * element configured it will be initialized with one Exit having state {@link PipeLine.ExitState#SUCCESS SUCCESS} (and name {@value PipeLine#DEFAULT_SUCCESS_EXIT_NAME})
	 * @ff.mandatory
	 */
	public void setState(ExitState value) {
		state = value;
	}

	/**
	 * HTTP statusCode e.g. <code>500</code>
	 * @ff.default 200
	 */
	public void setCode(int code) {
		this.exitCode = code;
	}

	/**
	 * Configures the responseRoot in the OpenAPI schema for this exit. If not set, the responseRoot value of the validator will be used. If that contains multiple (comma separated) values, the first will be used for the exits with state <code>SUCCESS</code>, the last for the other exits.
	 */
	public void setResponseRoot(String responseRoot) {
		this.responseRoot = responseRoot;
	}

	/**
	 * If using RestListener and set to <code>true</code>, this removes the output and shows a blank page, the output is still logged in the ladybug testtool
	 * @ff.default <code>false</code>
	 */
	public void setEmpty(boolean b) {
		emptyResult = b;
	}

	/**
	 * If set to <code>true</code>, the output will not be wrapped by the OutputWrapper.
	 * @ff.default <code>false</code>
	 */
	public void setSkipWrapping(boolean b) {
		skipWrapping = b;
	}

	/**
	 * If set to <code>true</code>, the output will not be validated or transformed by the validator.
	 * @ff.default <code>false</code>
	 */
	public void setSkipValidation(boolean b) {
		skipValidation = b;
	}

}
