/*
   Copyright 2019-2021 WeAreFrank!

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

import com.aspose.pdf.Document;
import com.aspose.pdf.exceptions.InvalidPasswordException;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;

/**
 * Returns the amount of pages of a PDF file.
 *
 * @ff.forward passwordProtected the File is password protected
 *
 * @author Laurens Mäkel
 * @since  7.6
 *
 */
public class AmountOfPagesPipe extends FixedForwardPipe {
	private String charset = null;

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		int result = 0;

		try (InputStream binaryInputStream = input.asInputStream(getCharset())){
			Document doc = new Document(binaryInputStream);
			result = doc.getPages().size();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot encode message using charset [" + getCharset() + "]", e);
		} catch (InvalidPasswordException ip) {
			return new PipeRunResult(findForward("passwordProtected"), "File is password protected." );
		}

		return new PipeRunResult(getSuccessForward(), Integer.toString(result) );
	}

	/**
	 * Charset to be used to encode the given input string 
	 * @ff.default UTF-8
	 */
	public void setCharset(String charset){
		this.charset = charset;
	}
	public String getCharset(){
		return charset;
	}
}
