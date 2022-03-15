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
package nl.nn.adapterframework.core;

import lombok.Getter;
import nl.nn.adapterframework.core.PipeLine.ExitState;

/**
 * The Exit of a Pipeline that specifies the end state of a PipeLine. The state is returned to the receiver as well as
 * the optionally specified http status code.
 * 
 * When a Pipeline doesn't have an Exits element the Pipeline will be initialized with one Exit having path READY and
 * state SUCCESS
 * 
 * The path of an Exit can be referenced by the Forward of a Pipe
 * 
 * <p>
 * <b>example:</b> <code><pre>
 *   &lt;exits&gt;
 *      &lt;exit path="READY" state="SUCCESS" /&gt;
 *      &lt;exit path="Created" state="ERROR" code="201" empty="true" /&gt;
 *      &lt;exit path="NotModified" state="ERROR" code="304" empty="true" /&gt;
 *      &lt;exit path="BadRequest" state="ERROR" code="400" empty="true" /&gt;
 *      &lt;exit path="NotAuthorized" state="ERROR" code="401" empty="true" /&gt;
 *      &lt;exit path="NotAllowed" state="ERROR" code="403" empty="true" /&gt;
 *      &lt;exit path="Teapot" state="SUCCESS" code="418" /&gt;
 *      &lt;exit path="ServerError" state="ERROR" code="500" /&gt;
 *   &lt;/exits&gt;
 * </pre></code>
 * </p>
 * 
 * @author Johan Verrips
 * @author Niels Meijer
 */
public class PipeLineExit implements IForwardTarget {

	private @Getter String path;
	private @Getter ExitState state;
	private @Getter int exitCode = 0;
	private @Getter String responseRoot;
	private @Getter boolean emptyResult = false;

	public boolean isSuccessExit() {
		return getState()==ExitState.SUCCESS;
	}

	/**
	 * The path of the Exit that can be referenced by the Forward of a Pipe. When a Pipeline doesn't have an Exits
	 * element the Pipeline will be initialized with one Exit having path READY (and state SUCCESS)
	 * @ff.mandatory
	 */
	public void setPath(String newPath) {
		path = newPath;
	}
	@Override
	// getName() is required by {@link IForwardTarget}. It is required that it returns the path,
	// this way PipeForwards can be resolved to either Pipes or PipeLineExits.
	public String getName() {
		return getPath();
	}

	/**
	 * The state of the Pipeline that is returned to the Receiver for this Exit. When a Pipeline doesn't have an Exits
	 * element the Pipeline will be initialized with one Exit having state SUCCESS (and path READY)
	 * @ff.mandatory
	 */
	public void setState(ExitState value) {
		state = value;
	}

	/**
	 * HTTP statusCode e.g. <code>500</code>
	 * @ff.default 200
	 */
	public void setCode(String code) {
		this.exitCode = Integer.parseInt(code);
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
	public void setEmpty(String b) {
		emptyResult = Boolean.parseBoolean(b);
	}

}
