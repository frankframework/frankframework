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
/*
 * $Log: LobLineIteratingPipeBase.java,v $
 * Revision 1.5  2012-02-17 18:04:02  m00f069
 * Use proxiedDataSources for JdbcIteratingPipeBase too
 * Call close on original/proxied connection instead of connection from statement that might be the unproxied connection
 *
 * Revision 1.4  2011/11/30 13:51:43  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2008/02/26 08:36:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed IteratingPipeBase to JdbcIteratingPipeBase
 *
 * Revision 1.1  2007/07/26 16:15:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */
package nl.nn.adapterframework.jdbc;

import java.io.Reader;
import java.sql.Connection;
import java.sql.ResultSet;

import nl.nn.adapterframework.core.IDataIterator;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.util.JdbcUtil;
import nl.nn.adapterframework.util.ReaderLineIterator;

/**
 * abstract baseclass for Pipes that iterate over the lines in a lob.
 * 
 * @author  Gerrit van Brakel
 * @since   4.7
 * @version $Id$
 */
public abstract class LobLineIteratingPipeBase extends JdbcIteratingPipeBase {

	protected abstract Reader getReader(ResultSet rs) throws SenderException;

	protected class ResultStreamIterator extends ReaderLineIterator {
		Connection conn;
		ResultSet rs;
		
		ResultStreamIterator(Connection conn, ResultSet rs, Reader reader) throws SenderException {
			super(reader);
			this.conn=conn;
			this.rs=rs;
		}
		
		public void close() throws SenderException {
			try {
				super.close();
			} finally {
				JdbcUtil.fullClose(conn, rs);
			}
		}

	}

	protected IDataIterator getIterator(Connection conn, ResultSet rs) throws SenderException {
		return new ResultStreamIterator(conn, rs, getReader(rs));
	}

}
