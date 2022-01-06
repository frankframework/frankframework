/*
   Copyright 2021 WeAreFrank!

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
package nl.nn.adapterframework.testtool;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

public class HttpServletResponseMock implements HttpServletResponse {
	String outputFile;
	ServletOutputStream outputStream;

	public String getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public void flushBuffer() throws IOException {
		// TODO Auto-generated method stub
		
	}

	public int getBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getContentType() {
		// TODO Auto-generated method stub
		return null;
	}

	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}

	public ServletOutputStream getOutputStream() throws IOException {
		if (outputStream == null) {
			outputStream = new ServletOutputStreamMock(getOutputFile());
		}
		return outputStream;
	}

	public PrintWriter getWriter() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isCommitted() {
		// TODO Auto-generated method stub
		return false;
	}

	public void reset() {
		// TODO Auto-generated method stub
		
	}

	public void resetBuffer() {
		// TODO Auto-generated method stub
		
	}

	public void setBufferSize(int arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setCharacterEncoding(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setContentLength(int arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setContentType(String arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setLocale(Locale arg0) {
		// TODO Auto-generated method stub
		
	}

	public void addCookie(Cookie arg0) {
		// TODO Auto-generated method stub
		
	}

	public void addDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub
		
	}

	public void addHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	public void addIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	public boolean containsHeader(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public String encodeRedirectURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public String encodeRedirectUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public String encodeURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public String encodeUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getHeader(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<String> getHeaderNames() {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<String> getHeaders(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getStatus() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void sendError(int arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void sendError(int arg0, String arg1) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void sendRedirect(String arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	public void setDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub
		
	}

	public void setHeader(String name, String value) {
		String line = name + ": " + value + "\n";
		try {
			getOutputStream().write(line.getBytes());
		} catch (IOException e) {
			throw new RuntimeException("Could not write header to output stream", e);
		}
	}

	public void setIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	public void setStatus(int arg0) {
		// TODO Auto-generated method stub
		
	}

	public void setStatus(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}
}

class ServletOutputStreamMock extends ServletOutputStream {
	FileOutputStream fileOutputStream;

	ServletOutputStreamMock(String outputFile) throws FileNotFoundException {
		fileOutputStream = new FileOutputStream(outputFile);
	}

	@Override
	public void write(int arg0) throws IOException {
		fileOutputStream.write(arg0);
	}

	@Override
	public void close() throws IOException {
		fileOutputStream.close();
		super.close();
	}
}