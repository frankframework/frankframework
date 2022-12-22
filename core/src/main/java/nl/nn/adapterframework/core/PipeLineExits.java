/*
   Copyright 2022 WeAreFrank!

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

import java.util.LinkedList;
import java.util.List;

import lombok.Getter;

/**
 * Pipeline exit container in which all (required) {@link PipeLineExit Exit}s must be defined.
 * Multiple exits may be provided each with their unique name.
 * <br/><br/>
 * If no exits are specified, a default one is created with name="READY" and state="SUCCESS".
 * <br/><br/>
 * <b>example:</b> <code><pre>
 *   &lt;Exits&gt;
 *      &lt;Exit name="{@value PipeLine#DEFAULT_SUCCESS_EXIT_NAME}" state="{@value PipeLine.ExitState#SUCCESS_EXIT_STATE}" /&gt;
 *      &lt;Exit name="Created" state="ERROR" code="201" empty="true" /&gt;
 *      &lt;Exit name="NotModified" state="ERROR" code="304" empty="true" /&gt;
 *      &lt;Exit name="BadRequest" state="ERROR" code="400" empty="true" /&gt;
 *      &lt;Exit name="NotAuthorized" state="ERROR" code="401" empty="true" /&gt;
 *      &lt;Exit name="NotAllowed" state="ERROR" code="403" empty="true" /&gt;
 *      &lt;Exit name="Teapot" state="SUCCESS" code="418" /&gt;
 *      &lt;Exit name="ServerError" state="ERROR" code="500" /&gt;
 *   &lt;/Exits&gt;
 * </pre></code>
 */
public class PipeLineExits {

	private @Getter List<PipeLineExit> exits = new LinkedList<>();

	/** 
	 * PipeLine exits.
	 * @ff.mandatory
	 */
	public void registerPipeLineExit(PipeLineExit exit) {
		exits.add(exit);
	}
}
