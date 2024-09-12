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
package org.frankframework.batch;

import java.io.StringWriter;
import java.io.Writer;

import org.frankframework.core.PipeLineSession;

/**
 * Resulthandler that writes the transformed record to a String, that is passed to the next Pipe literally.
 *
 * @author Gerrit van Brakel
 * @since   4.7
 * @deprecated Warning: non-maintained functionality.
 */
public class Result2StringWriter extends ResultWriter {

	@Override
	protected Writer createWriter(PipeLineSession session, String streamId) throws Exception {
		return new StringWriter();
	}

	@Override
	public String finalizeResult(PipeLineSession session, String streamId, boolean error) throws Exception {
		super.finalizeResult(session,streamId, error);
		StringWriter writer = (StringWriter)getWriter(session,streamId,false);
		String result=null;
		if (writer!=null) {
			result = writer.getBuffer().toString();
		}
		return result;
	}

}
