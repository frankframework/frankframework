/*
   Copyright 2013, 2019, 2020 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.FileMessage;
import nl.nn.adapterframework.stream.Message;

/**
 * Sends a message to a Sender for each line of the file that the input message refers to.
 *
 * @author  Gerrit van Brakel
 */
@Deprecated
@ConfigurationWarning("Please use StreamLineIteratorPipe")
public class FileLineIteratorPipe extends StreamLineIteratorPipe {

	private @Getter String move2dirAfterTransform;
	private @Getter String move2dirAfterError;
	private @Getter String charset;

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

		String filename;
		try {
			filename = input.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot read input", e);
		}
		File file = new File(filename);

		try {
			PipeRunResult result = super.doPipe(new FileMessage(file, getCharset()), session);
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
	 * Directory where input file is moved to in case of a successful transformation
	 */
	public void setMove2dirAfterTransform(String readyDir) {
		move2dirAfterTransform = readyDir;
	}

	/**
	 * Directory where input file is moved to in case an error occurred
	 */
	public void setMove2dirAfterError(String errorDir) {
		move2dirAfterError = errorDir;
	}

	/**
	 * Default charset attribute
	 * 
	 * @ff.default UTF-8
	 */
	public void setCharset(String value) {
		charset = value;
	}
}
