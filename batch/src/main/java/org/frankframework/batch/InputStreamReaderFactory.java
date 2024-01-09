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
package org.frankframework.batch;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.util.StreamUtil;

/**
 * Basic InputStreamReaderFactory.
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public class InputStreamReaderFactory implements IReaderFactory {

	@Override
	public void configure() throws ConfigurationException {
	}

	@Override
	public Reader getReader(InputStream inputstream, String charset, String streamId, PipeLineSession session) throws SenderException {
		try {
			return StreamUtil.getCharsetDetectingInputStreamReader(inputstream, charset);
		} catch (IOException e) {
			throw new SenderException("cannot use charset ["+charset+"] to read stream ["+streamId+"]",e);
		}
	}


}
