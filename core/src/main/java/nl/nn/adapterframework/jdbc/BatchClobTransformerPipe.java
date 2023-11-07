/*
   Copyright 2013 Nationale-Nederlanden, 2023 WeAreFrank!

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
package nl.nn.adapterframework.jdbc;

import java.io.Reader;
import java.sql.ResultSet;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;

/**
 * Pipe that batch-transforms the lines in a CLOB.
 *
 * N.B. the readerFactory is not used by this class.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
@Deprecated
@ConfigurationWarning("BatchClobTransformerPipe: Not tested and maintained, please look for alternatives if you use BatchClobTransformerPipe inform WeAreFrank! that there are no suitable alternatives for your use-case")
public class BatchClobTransformerPipe extends BatchTransformerPipeBase {


	@Override
	protected Reader getReader(ResultSet rs, String charset, String streamId, PipeLineSession session) throws SenderException {
		try {
			return rs.getCharacterStream(1);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}
}
