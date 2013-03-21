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
 * $Log: IbisServletResponseWrapper.java,v $
 * Revision 1.3  2011-11-30 13:51:58  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2008/07/24 12:38:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reintroduction
 *
 */
package nl.nn.adapterframework.webcontrol;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * Extension of HttpServletResponseRwapper on to capture response in a String.
 * 
 * @author  Peter Leeuwenburgh
 * @version $Id$
 */

public class IbisServletResponseWrapper	extends HttpServletResponseWrapper {
	
	IbisServletOutputStream ibisServletOutputStream;
	StringWriter stringWriter;

	public IbisServletResponseWrapper(HttpServletResponse response) {
		super(response);
		ibisServletOutputStream = new IbisServletOutputStream();
		stringWriter = new StringWriter();
	}

	public ServletOutputStream getOutputStream() {
		return ibisServletOutputStream;
	}

	public PrintWriter getWriter() {
		return new PrintWriter(stringWriter);
	}

	public StringWriter getStringWriter() {
		return stringWriter;
	}

	public class IbisServletOutputStream extends ServletOutputStream {

		public StringBuffer stringBuffer = new StringBuffer("");

		public void write(int i) {
			stringBuffer.append(i);
		}
	}
	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#addCookie(javax.servlet.http.Cookie)
	 */
	public void addCookie(Cookie arg0) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#containsHeader(java.lang.String)
	 */
	public boolean containsHeader(String arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#encodeURL(java.lang.String)
	 */
	public String encodeURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#encodeRedirectURL(java.lang.String)
	 */
	public String encodeRedirectURL(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#encodeUrl(java.lang.String)
	 */
	public String encodeUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#encodeRedirectUrl(java.lang.String)
	 */
	public String encodeRedirectUrl(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#sendError(int, java.lang.String)
	 */
	public void sendError(int arg0, String arg1) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#sendError(int)
	 */
	public void sendError(int arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#sendRedirect(java.lang.String)
	 */
	public void sendRedirect(String arg0) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#setDateHeader(java.lang.String, long)
	 */
	public void setDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#addDateHeader(java.lang.String, long)
	 */
	public void addDateHeader(String arg0, long arg1) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#setHeader(java.lang.String, java.lang.String)
	 */
	public void setHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#addHeader(java.lang.String, java.lang.String)
	 */
	public void addHeader(String arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#setIntHeader(java.lang.String, int)
	 */
	public void setIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#addIntHeader(java.lang.String, int)
	 */
	public void addIntHeader(String arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#setStatus(int)
	 */
	public void setStatus(int arg0) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServletResponse#setStatus(int, java.lang.String)
	 */
	public void setStatus(int arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#getCharacterEncoding()
	 */
	public String getCharacterEncoding() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#setContentLength(int)
	 */
	public void setContentLength(int arg0) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#setContentType(java.lang.String)
	 */
	public void setContentType(String arg0) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#setBufferSize(int)
	 */
	public void setBufferSize(int arg0) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#getBufferSize()
	 */
	public int getBufferSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#flushBuffer()
	 */
	public void flushBuffer() throws IOException {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#isCommitted()
	 */
	public boolean isCommitted() {
		// TODO Auto-generated method stub
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#reset()
	 */
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#setLocale(java.util.Locale)
	 */
	public void setLocale(Locale arg0) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponse#getLocale()
	 */
	public Locale getLocale() {
		// TODO Auto-generated method stub
		return null;
	}
	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponseWrapper#getResponse()
	 */
	public ServletResponse getResponse() {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponseWrapper#resetBuffer()
	 */
	public void resetBuffer() {
		// TODO Auto-generated method stub
//		super.resetBuffer();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.ServletResponseWrapper#setResponse(javax.servlet.ServletResponse)
	 */
	public void setResponse(ServletResponse arg0) {
		// TODO Auto-generated method stub
		super.setResponse(arg0);
	}

}