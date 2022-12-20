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
package nl.nn.adapterframework.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.sql.ResultSet;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * Pipe that batch-transforms the lines in a BLOB.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
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
