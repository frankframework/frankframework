/*
   Copyright 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class InputStreamDataSource implements DataSource {
	private String contentType;
	private InputStream inputStream;

	/**
	 * Use content type application/octet-stream in case it cannot be
	 * determined. See http://docs.oracle.com/javase/7/docs/api/javax/activation/DataSource.html#getContentType():
	 * This method returns the MIME type of the data in the form of a string.
	 * It should always return a valid type. It is suggested that getContentType
	 * return "application/octet-stream" if the DataSource implementation can
	 * not determine the data type.
	 */
	public InputStreamDataSource(String contentType, InputStream inputStream) {
		this.contentType = contentType;
		this.inputStream = inputStream;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return inputStream;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
}
