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

import java.io.Writer;
import java.sql.ResultSet;

import org.frankframework.batch.IResultHandler;
import org.frankframework.core.SenderException;
import org.frankframework.dbms.IDbmsSupport;


/**
 * {@link IResultHandler ResultHandler} that writes the transformed record to a CLOB.
 *
 * @author  Gerrit van Brakel
 * @since   4.7
 */
public class Result2ClobWriter extends Result2LobWriterBase {

	@Override
	protected Object getLobHandle(IDbmsSupport dbmsSupport, ResultSet rs) throws SenderException {
		try {
			return dbmsSupport.getClobHandle(rs, querySender.getClobColumn());
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

	/**
	 * Column that contains the CLOB to be updated
	 * @ff.default 1
	 */
	public void setClobColumn(int column) {
		querySender.setClobColumn(column);
	}
}
