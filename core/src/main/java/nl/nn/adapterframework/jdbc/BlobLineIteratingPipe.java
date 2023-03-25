/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.JdbcUtil;

/**
 * Pipe that iterates over the lines in a BLOB.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class BlobLineIteratingPipe extends LobLineIteratingPipeBase {

	@Override
	protected Reader getReader(ResultSet rs) throws SenderException {
		try {
			return JdbcUtil.getBlobReader(querySender.getDbmsSupport(), rs,1,querySender.getBlobCharset(),querySender.isBlobsCompressed());
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}
}
