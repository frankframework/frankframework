/*
   Copyright 2020 WeAreFrank!

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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.XmlBuilder;

/**
 * Wrapper around DBMS provided OutputStream for BLOB, that updates BLOB and ResultSet and closes them at stream.close().
 *
 * @author Gerrit van Brakel
 */
public class BlobOutputStream extends FilterOutputStream {

	private final IDbmsSupport dbmsSupport;
	private final Object blobUpdateHandle;
	private final int blobColumn;
	private final ResultSet resultSet;
	private final XmlBuilder warnings;
	private boolean open;

	public BlobOutputStream(IDbmsSupport dbmsSupport, Object blobUpdateHandle, int blobColumn, OutputStream blobOutputStream, ResultSet resultSet, XmlBuilder warnings) {
		super(blobOutputStream);
		this.dbmsSupport=dbmsSupport;
		this.blobUpdateHandle=blobUpdateHandle;
		this.blobColumn=blobColumn;
		this.resultSet=resultSet;
		this.warnings=warnings;
		open=true;
	}

	@Override
	public void close() throws IOException {
		if (open) {
			open=false;
			try {
				super.close();
				dbmsSupport.updateBlob(resultSet, blobColumn, blobUpdateHandle);
				resultSet.updateRow();
				JdbcUtil.warningsToXml(resultSet.getWarnings(),warnings);
			} catch (JdbcException | SQLException e) {
				throw new IOException("cannot write BLOB",e);
			} finally {
				JdbcUtil.fullClose(null, resultSet);
			}
		}
	}

	public XmlBuilder getWarnings() {
		return warnings;
	}

}
