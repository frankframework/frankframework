/*
   Copyright 2018, 2020 Nationale-Nederlanden, 2021-2022 WeAreFrank!

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
package org.frankframework.pipes;

import java.io.IOException;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonReader;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
import org.frankframework.stream.Message;

/**
 *<code>Pipe</code> that checks the well-formedness of the input message.
 *
 * @ff.forward failure if a validation error occurred, probably caused by non-well-formed JSON
 *
 * @author  Tom van der Heijden
 */
@ElementType(ElementTypes.VALIDATOR)
public class JsonWellFormedChecker extends FixedForwardPipe {

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if(Message.isEmpty(message)) {
			return new PipeRunResult(findForward("failure"), message);
		}

		try(JsonReader jr = Json.createReader(message.asReader())) {
			jr.read();
		} catch (JsonException e) {
			return new PipeRunResult(findForward("failure"), message);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}

		return new PipeRunResult(getSuccessForward(), message);
	}
}
