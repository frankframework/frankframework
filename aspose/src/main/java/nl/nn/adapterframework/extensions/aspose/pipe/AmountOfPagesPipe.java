/*
   Copyright 2019, 2020 WeAreFrank!

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
package nl.nn.adapterframework.extensions.aspose.pipe;

import java.io.IOException;
import java.io.InputStream;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.StreamUtil;

import com.aspose.pdf.Document;
import com.aspose.pdf.exceptions.InvalidPasswordException;

/**
 * Returns the amount of pages of a PDF file.
 * 
 * 
 * @author Laurens Mäkel
 * @since  7.6
 *
 */
public class AmountOfPagesPipe extends FixedForwardPipe {
	private String charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
	}

	@Override
	public PipeRunResult doPipe(Message input, IPipeLineSession session) throws PipeRunException {
		int result = 0;

		InputStream binaryInputStream;
		try {
			binaryInputStream = input.asInputStream(getCharset());
		} catch (IOException e1) {
			throw new PipeRunException(this,
						getLogPrefix(session) + "cannot encode message using charset [" + getCharset() + "]", e1);
		}

		try {
			Document doc = new Document(binaryInputStream);
			result = doc.getPages().size();
		} catch (InvalidPasswordException ip) {
			return new PipeRunResult(findForward("passwordProtected"), "File is password protected." );
		}
      
		return new PipeRunResult(getForward(), Integer.toString(result) );
	}

	@IbisDoc({ "charset to be used to encode the given input string ", "ISO-8859-1" })
	public void setCharset(String charset){
		this.charset = charset;
	}

	public String getCharset(){
		return charset;
	}
}
