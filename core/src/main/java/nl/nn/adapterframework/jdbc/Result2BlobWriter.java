/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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

import java.io.Writer;
import java.sql.ResultSet;

import nl.nn.adapterframework.batch.IResultHandler;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.util.JdbcUtil;


/**
 * {@link IResultHandler ResultHandler} that writes the transformed record to a BLOB.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class Result2BlobWriter extends Result2LobWriterBase {

	@Override
	protected Object getLobHandle(IDbmsSupport dbmsSupport, ResultSet rs) throws SenderException {
		try {
			return dbmsSupport.getBlobHandle(rs, querySender.getBlobColumn());
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}
	@Override
	protected void updateLob(IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException {
		try {
			dbmsSupport.updateBlob(rs, querySender.getBlobColumn(), lobHandle);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	protected Writer getWriter(IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException {
		try {
			return JdbcUtil.getBlobWriter(dbmsSupport,lobHandle,rs,querySender.getBlobColumn(), querySender.getBlobCharset(), querySender.isBlobsCompressed());
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	/**
	 * Column that contains the BLOB to be updated
	 * @ff.default 1
	 */
	public void setBlobColumn(int column) {
		querySender.setBlobColumn(column);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setBlobsCompressed(boolean compressed) {
		querySender.setBlobsCompressed(compressed);
	}

	/** @ff.ref nl.nn.adapterframework.jdbc.FixedQuerySender */
	public void setBlobCharset(String charset) {
		querySender.setBlobCharset(charset);
	}

}
