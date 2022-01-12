/*
   Copyright 2018, 2020 Nationale-Nederlanden

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
package nl.nn.adapterframework.pipes;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

/**
 *<code>Pipe</code> that checks the well-formedness of the input message.
 * 
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td>"failure"</td><td>if a validation error occurred, probably caused by non-well-formed JSON</td></tr>
 * </table>
 * <br>
 * @author  Tom van der Heijden
 */

public class JsonWellFormedChecker extends FixedForwardPipe {
	
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		PipeForward forward = getSuccessForward();

		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot open stream", e);
		}
		if (input.isEmpty()) {
			forward = findForward("failure");
		} else {
			try {
				new JSONObject(input);
			} catch (JSONException ex) {
				try {
					new JSONArray(input);
				} catch (JSONException ex1) {
					forward = findForward("failure");
				}
			}
		}
		return new PipeRunResult(forward, message);
	}
}
