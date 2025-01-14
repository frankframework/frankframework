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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.frankframework.dbms.IDbmsSupport;
import org.frankframework.dbms.JdbcException;
import org.frankframework.util.JdbcUtil;
import org.frankframework.util.XmlBuilder;

/**
 * Wrapper around DBMS provided Writer for CLOB, that updates CLOB and ResultSet and closes them at writer.close().
 *
 * @author Gerrit van Brakel
 */
public class ClobWriter extends FilterWriter {

	private final IDbmsSupport dbmsSupport;
	private final Object clobUpdateHandle;
	private final int clobColumn;
	private final ResultSet resultSet;
	private final XmlBuilder warnings;
	private boolean open;

	public ClobWriter(IDbmsSupport dbmsSupport, Object clobUpdateHandle, int clobColumn, Writer clobWriter, ResultSet resultSet, XmlBuilder warnings) {
		super(clobWriter);
		this.dbmsSupport=dbmsSupport;
		this.clobUpdateHandle=clobUpdateHandle;
		this.clobColumn=clobColumn;
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
				dbmsSupport.updateClob(resultSet, clobColumn, clobUpdateHandle);
				resultSet.updateRow();
				JdbcUtil.warningsToXml(resultSet.getWarnings(),warnings);
			} catch (JdbcException | SQLException e) {
				throw new IOException("cannot write CLOB",e);
			} finally {
				JdbcUtil.fullClose(null, resultSet);
			}
		}
	}

	public XmlBuilder getWarnings() {
		return warnings;
	}

}
