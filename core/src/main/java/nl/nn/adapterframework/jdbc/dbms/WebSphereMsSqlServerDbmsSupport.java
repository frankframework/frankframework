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
package nl.nn.adapterframework.jdbc.dbms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * @author  Gerrit van Brakel
 * @since  
 */
public class WebSphereMsSqlServerDbmsSupport extends MsSqlServerDbmsSupport {


	public String getDbmsName() {
		return "MS SQL for WebSphere";
	}

	public Object getClobUpdateHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		try {
			File clobFile= FileUtils.createTempFile("clob",".txt");
			return clobFile;
		} catch (IOException e) {
			throw new JdbcException("Cannot create tempfile for clob in column ["+column+"]",e);
		}
	}
	public Object getClobUpdateHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		try {
			File clobFile= FileUtils.createTempFile("clob",".txt");
			return clobFile;
		} catch (IOException e) {
			throw new JdbcException("Cannot create tempfile for clob in column ["+column+"]",e);
		}
	}
	
	public Writer getClobWriter(ResultSet rs, int column, Object clobUpdateHandle) throws SQLException, JdbcException {
		File clobFile = (File)clobUpdateHandle;
		try {
			OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(clobFile),StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			return fw;
		} catch (Exception e) {
			throw new JdbcException("cannot write clob for column ["+column+"] to file ["+clobFile.toString()+"]",e);
		}
	}
	public Writer getClobWriter(ResultSet rs, String column, Object clobUpdateHandle) throws SQLException, JdbcException {
		File clobFile = (File)clobUpdateHandle;
		try {
			OutputStreamWriter fw = new OutputStreamWriter(new FileOutputStream(clobFile),StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
			return fw;
		} catch (Exception e) {
			throw new JdbcException("cannot write clob for column ["+column+"] to file ["+clobFile.toString()+"]",e);
		}
	}
	public void updateClob(ResultSet rs, int column, Object clobUpdateHandle) throws SQLException, JdbcException {
		File clobFile = (File)clobUpdateHandle;
		try {
			InputStream is = new FileInputStream(clobFile); 
			Reader isr = StreamUtil.getCharsetDetectingInputStreamReader(is);
			rs.updateCharacterStream(column, isr, (int)clobFile.length()); // FIXME: should use character count instead of byte count!
		} catch (Exception e) {
			throw new JdbcException("cannot read clob for column ["+column+"] from file ["+clobFile.toString()+"]",e);
		}
	}
	public void updateClob(ResultSet rs, String column, Object clobUpdateHandle) throws SQLException, JdbcException {
		File clobFile = (File)clobUpdateHandle;
		try {
			InputStream is = new FileInputStream(clobFile); 
			Reader isr = StreamUtil.getCharsetDetectingInputStreamReader(is);
			rs.updateCharacterStream(column, isr, (int)clobFile.length()); // FIXME: should use character count instead of byte count!
		} catch (Exception e) {
			throw new JdbcException("cannot read clob for column ["+column+"] from file ["+clobFile.toString()+"]",e);
		}
	}

	
	public Object getBlobUpdateHandle(ResultSet rs, int column) throws SQLException, JdbcException {
		try {
			File blobFile= FileUtils.createTempFile("blob",".bin");
			return blobFile;
		} catch (IOException e) {
			throw new JdbcException("Cannot create tempfile for blob in column ["+column+"]",e);
		}
	}
	public Object getBlobUpdateHandle(ResultSet rs, String column) throws SQLException, JdbcException {
		try {
			File blobFile= FileUtils.createTempFile("blob",".bin");
			return blobFile;
		} catch (IOException e) {
			throw new JdbcException("Cannot create tempfile for blob in column ["+column+"]",e);
		}
	}

	protected  OutputStream getBlobOutputStream(ResultSet rs, Object blobUpdateHandle) throws SQLException, JdbcException {
		File blobFile = (File)blobUpdateHandle;
		try {
			FileOutputStream fos = new FileOutputStream(blobFile);
			return fos;
		} catch (FileNotFoundException e) {
			throw new JdbcException("cannot write blob to file ["+blobFile.toString()+"]",e);
		}
	}
	public void updateBlob(ResultSet rs, int column, Object blobUpdateHandle) throws SQLException, JdbcException {
		File blobFile = (File)blobUpdateHandle;
		try {
			InputStream is = new FileInputStream(blobFile); 
			rs.updateBinaryStream(column, is, (int)blobFile.length());
		} catch (FileNotFoundException e) {
			throw new JdbcException("cannot read blob for column ["+column+"] from file ["+blobFile.toString()+"]",e);
		}
	}
	public void updateBlob(ResultSet rs, String column, Object blobUpdateHandle) throws SQLException, JdbcException {
		File blobFile = (File)blobUpdateHandle;
		try {
			InputStream is = new FileInputStream(blobFile); 
			rs.updateBinaryStream(column, is, (int)blobFile.length());
		} catch (FileNotFoundException e) {
			throw new JdbcException("cannot read blob for column ["+column+"] from file ["+blobFile.toString()+"]",e);
		}
	}

}
