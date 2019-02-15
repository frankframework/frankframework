/*
   Copyright 2013 Nationale-Nederlanden

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

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;


/**
 * Resulthandler that writes the transformed record to a String, that is passed literally to the next Pipe.
 * 
 * 
 * @author Gerrit van Brakel
 * @since   4.7
 */
public class Result2StringWriter extends ResultWriter {
	
	private Map openWriters = Collections.synchronizedMap(new HashMap());
	
	protected Writer createWriter(IPipeLineSession session, String streamId, ParameterResolutionContext prc) throws Exception {
		Writer writer=new StringWriter();
		openWriters.put(streamId,writer);
		return writer;
	}
	
	public Object finalizeResult(IPipeLineSession session, String streamId, boolean error, ParameterResolutionContext prc) throws Exception {
		super.finalizeResult(session,streamId, error, prc);
		StringWriter writer = (StringWriter)getWriter(session,streamId,false, prc);
		String result=null;
		if (writer!=null) {
			result = (writer).getBuffer().toString();
		} 
		return result;		
	}

}
