/*
   Copyright 2013, 2019, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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

import java.io.File;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.FileMessage;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Sends a message to a Sender for each line of the file that the input message refers to.
 *
 * @author  Gerrit van Brakel
 */
public class FileLineIteratorPipe extends StreamLineIteratorPipe {

	private String move2dirAfterTransform;
	private String move2dirAfterError;

	/**
	 * Open a reader for the file named according the input messsage and 
	 * transform it.
	 * Move the input file to a done directory when transformation is finished
	 * and return the names of the generated files. 
	 * 
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Message, PipeLineSession)
	 */
	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		if (input==null) {
			throw new PipeRunException(this,"got null input instead of String containing filename");
		}
		
		if (!(input.asObject() instanceof String)) {
			throw new PipeRunException(this,"expected String containing filename as input, got ["+ClassUtils.nameOf(input)+"], value ["+input+"]");
		}
		String filename	= (String)input.asObject();
		File file = new File(filename);

		try {
			
			PipeRunResult result = super.doPipe(new FileMessage(file), session);
			if (! StringUtils.isEmpty(getMove2dirAfterTransform())) {
				File move2 = new File(getMove2dirAfterTransform(), file.getName());
				file.renameTo(move2); 
			}
			return result;
		} catch (PipeRunException e) {
			if (! StringUtils.isEmpty(getMove2dirAfterError())) {
				File move2 = new File(getMove2dirAfterError(), file.getName());
				file.renameTo(move2); 
			}
			throw e;
		}
	}

	
	/**
	 * @param readyDir directory where input file is moved to in case of a succesful transformation
	 */
	@IbisDoc({"directory in which the transformed file(s) is stored", ""})
	public void setMove2dirAfterTransform(String readyDir) {
		move2dirAfterTransform = readyDir;
	}
	public String getMove2dirAfterTransform() {
		return move2dirAfterTransform;
	}

	/**
	 * @param errorDir directory where input file is moved to in case of an error
	 */
	@IbisDoc({"directory to which the inputfile is moved in case an error occurs", ""})
	public void setMove2dirAfterError(String errorDir) {
		move2dirAfterError = errorDir;
	}
	public String getMove2dirAfterError() {
		return move2dirAfterError;
	}

}
