/*
   Copyright 2019 Integration Partners B.V.
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
package nl.nn.adapterframework.senders;

import java.io.IOException;
import java.io.InputStream;

import com.amazonaws.services.s3.model.S3ObjectInputStream;

/**
* <p>
* This class is used for the download action. 
* When a download action is performed this class is used to close the InputStream when there is nothing to be read.
* </p>
**/
public class S3ObjectInputStreamCloser extends InputStream
{
	S3ObjectInputStream s3InputStream;

	public S3ObjectInputStreamCloser(S3ObjectInputStream s3InputStream)
	{
		this.s3InputStream = s3InputStream;
	}

	@Override
	public int read() throws IOException {
		int i = s3InputStream.read();
		if (i == -1) {
			close();
		}
		return i;
	}

	@Override
	public int read(byte[] b) throws IOException {
		int i = s3InputStream.read(b);
		if (i == -1) {
			close();
		}
		return i;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int i = s3InputStream.read(b, off, len);
		if (i == -1) {
			close();
		}
		return i;
	}

	@Override
	public void close() throws IOException {
		s3InputStream.close();
		System.out.println("Stream closed.");
	}

} 