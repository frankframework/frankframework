/*
   Copyright 2013 Nationale-Nederlanden, 2020 WeAreFrank!

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

import nl.nn.adapterframework.doc.IbisDoc;

/**
 * The PipeLineExit, that represents a terminator of the PipeLine, provides a placeholder
 * for a path (corresponding to a pipeforward) and a state (that is returned to the receiver).
 * 
 * <p>An exit consists out of two mandatory and two optional parameters:
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setPath(String) path}</td><td>name of the pipeline exit</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setState(String) state}</td><td>The exit state defines possible exists to the pipeline. The state can be one of the following: <code>success, error</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setCode(String) code}</td><td>http statuscode e.g. <code>500</code></td><td>200</td></tr>
 * <tr><td>{@link #setEmpty(String) empty}</td><td>when using RestListener and set to <code>true</code>, this removes the output and shows a blank page, the output is still logged in the ladybug testtool</td><td>false</td></tr>
 * </table>
 * </p>
 * 
 * <p>
 * <b>example:</b> <code><pre>
 *   &lt;exits&gt;
 *      &lt;exit path="EXIT" state="success" /&gt;
 *      &lt;exit path="Created" state="error" code="201" empty="true" /&gt;
 *      &lt;exit path="NotModified" state="error" code="304" empty="true" /&gt;
 *      &lt;exit path="BadRequest" state="error" code="400" empty="true" /&gt;
 *      &lt;exit path="NotAuthorized" state="error" code="401" empty="true" /&gt;
 *      &lt;exit path="NotAllowed" state="error" code="403" empty="true" /&gt;
 *      &lt;exit path="Teapot" state="success" code="418" /&gt;
 *      &lt;exit path="ServerError" state="error" code="500" /&gt;
 *   &lt;/exits&gt;
 * </pre></code>
 * </p>
 * 
 * @author Johan Verrips
 * @author Niels Meijer
 */
public class PipeLineExit implements IForwardTarget {

	public static final String EXIT_STATE_SUCCESS = "success";

	private String path;
	private String state;
	private int exitCode = 0;
	private boolean emptyResult = false;
	private String elementName;

	@IbisDoc({"name of the pipeline exit", ""})
	public void setPath(String newPath) {
		path = newPath;
	}
	public String getPath() {
		return path;
	}
	@Override
	// getName() is required by {@link IForwardTarget}. It is required that it returns the path,
	// this way PipeForwards can be resolved to either Pipes or PipeLineExits.
	public String getName() {
		return getPath();
	}

	@IbisDoc({"the exit state defines possible exists to the pipeline. the state can be one of the following: <code>success, error</code>", ""})
	public void setState(String newState) {
		state = newState;
	}
	public String getState() {
		return state;
	}

	@IbisDoc({"http statuscode e.g. <code>500</code>", "200"})
	public void setCode(String code) {
		this.exitCode = Integer.parseInt(code);
	}
	public int getExitCode() {
		return exitCode;
	}

	@IbisDoc({"when using restlistener and set to <code>true</code>, this removes the output and shows a blank page, the output is still logged in the ladybug testtool", "false"})
	public void setEmpty(String b) {
		emptyResult = Boolean.parseBoolean(b);
	}
	public boolean getEmptyResult() {
		return emptyResult;
	}
	// TODO: validate the output by looking at the elementName
	@IbisDoc({"configures exit specific element reference in open api schema. If it is not set, but the responseRoot attribute is set in the validator, for the exit code 200 it will use the first element in the responseRoot and for the other codes last element of the responseRoot will be used.", ""})
	public void setElementName(String elementName) {
		this.elementName = elementName;
	}
	public String getElementName() {
		return elementName;
	}
}
