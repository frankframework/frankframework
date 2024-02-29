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
package org.frankframework.jdbc;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.zip.DeflaterOutputStream;

import org.frankframework.batch.IResultHandler;
import org.frankframework.core.SenderException;
import org.frankframework.dbms.DbmsException;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.doc.ReferTo;
import org.frankframework.util.StreamUtil;


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
			return getBlobWriter(dbmsSupport,lobHandle,rs,querySender.getBlobColumn(), querySender.getBlobCharset(), querySender.isBlobsCompressed());
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	private static Writer getBlobWriter(IDbmsSupport dbmsSupport, Object blobUpdateHandle, final ResultSet rs, int columnIndex, String charset, boolean compressBlob) throws IOException, DbmsException, SQLException {
		Writer result;
		OutputStream out = dbmsSupport.getBlobOutputStream(rs, columnIndex, blobUpdateHandle);
		if (charset == null) {
			charset = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		}
		if (compressBlob) {
			result = new BufferedWriter(new OutputStreamWriter(new DeflaterOutputStream(out), charset));
		} else {
			result = new BufferedWriter(new OutputStreamWriter(out, charset));
		}
		return result;
	}

	/**
	 * Column that contains the BLOB to be updated
	 * @ff.default 1
	 */
	public void setBlobColumn(int column) {
		querySender.setBlobColumn(column);
	}

	@ReferTo(FixedQuerySender.class)
	public void setBlobsCompressed(boolean compressed) {
		querySender.setBlobsCompressed(compressed);
	}

	@ReferTo(FixedQuerySender.class)
	public void setBlobCharset(String charset) {
		querySender.setBlobCharset(charset);
	}

}
