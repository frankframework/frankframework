/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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
package org.frankframework.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.util.JdbcUtil;

/**
 * Pipe that batch-transforms the lines in a BLOB.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
@Deprecated
@ConfigurationWarning("BatchBlobTransformerPipe: Not tested and maintained, please look for alternatives if you use BatchBlobTransformerPipe inform WeAreFrank! that there are no suitable alternatives for your use-case")
public class BatchBlobTransformerPipe extends BatchTransformerPipeBase {

	@Override
	protected Reader getReader(ResultSet rs, String charset, String streamId, PipeLineSession session) throws SenderException {
		try {
			InputStream blobStream=JdbcUtil.getBlobInputStream(querySender.getDbmsSupport(), rs ,1 , querySender.isBlobsCompressed());
			return getReaderFactory().getReader(blobStream, charset, streamId, session);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Deprecated
	@ConfigurationWarning("please use attribute 'charset' instead")
	public void setBlobCharset(String charset) {
		setCharset(charset);
	}

	/**
	 * controls whether blobdata is stored compressed in the database
	 * @ff.default true
	 */
	public void setBlobsCompressed(boolean compressed) {
		querySender.setBlobsCompressed(compressed);
	}
}
