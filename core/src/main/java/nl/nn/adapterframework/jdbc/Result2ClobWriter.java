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
package nl.nn.adapterframework.jdbc;

import java.io.Writer;
import java.sql.ResultSet;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;


/**
 * {@link nl.nn.adapterframework.batch.IResultHandler ResultHandler} that writes the transformed record to a CLOB.
 * 
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>any parameters defined on the resultHandler will be applied to the SQL statement</td></tr>
 * </table>
 * <p/>
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class Result2ClobWriter extends Result2LobWriterBase {
	
	@Override
	protected Object getLobHandle(IDbmsSupport dbmsSupport, ResultSet rs) throws SenderException {
		try {
			return dbmsSupport.getClobUpdateHandle(rs, querySender.getClobColumn());
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}
	
	@Override
	protected void updateLob(IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException {
		try {
			dbmsSupport.updateClob(rs, querySender.getClobColumn(), lobHandle);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	protected Writer getWriter(IDbmsSupport dbmsSupport, Object lobHandle, ResultSet rs) throws SenderException {
		try {
			return dbmsSupport.getClobWriter(rs,querySender.getClobColumn(), lobHandle);
		} catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@IbisDoc({"column that contains the clob to be updated", "1"})
	public void setClobColumn(int column) {
		querySender.setClobColumn(column);
	}
}
