/*
   Copyright 2013 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.logging.log4j.Logger;

import org.frankframework.core.IDataIterator;
import org.frankframework.core.SenderException;
import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.util.DB2XMLWriter;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.LogUtil;

/**
 * Iterator over ResultSet.
 *
 * Each row is returned in the same way a row is usually returned from a query.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
class ResultSetIterator implements IDataIterator<String> {
	protected Logger log = LogUtil.getLogger(this);

	private final IDbmsSupport dbmsSupport;
	private final Connection conn;
	private final ResultSet rs;
	private final boolean decompressBlobs;
	private final boolean blobSmartGet;
	private final ResultSetMetaData rsmeta;
	private final String blobCharset;

	private boolean lineChecked=true; // assumes at least one line is present, and cursor is on it!
	private boolean lineAvailable=true;
	private int rowNumber=0;

	public ResultSetIterator(IDbmsSupport dbmsSupport, Connection conn, ResultSet rs, String blobCharset, boolean decompressBlobs, boolean blobSmartGet) throws SQLException {
		super();
		this.dbmsSupport = dbmsSupport;
		this.conn = conn;
		this.rs = rs;
		this.decompressBlobs = decompressBlobs;
		this.blobSmartGet = blobSmartGet;
		this.blobCharset = blobCharset;
		rsmeta=rs.getMetaData();
	}

	@Override
	public boolean hasNext() throws SenderException {
		try {
			if (!lineChecked) {
				lineAvailable = rs.next();
				lineChecked = true;
			}
			return lineAvailable;
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	public String next() throws SenderException {
		try {
			lineChecked=false;
			return DB2XMLWriter.getRowXml(dbmsSupport, rs, rowNumber++, rsmeta, blobCharset, decompressBlobs, "", true, blobSmartGet);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	public void close() {
		JdbcUtil.fullClose(conn, rs);
	}
}
