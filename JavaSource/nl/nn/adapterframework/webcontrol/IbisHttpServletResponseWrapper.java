/*
 * $Log: IbisHttpServletResponseWrapper.java,v $
 * Revision 1.1  2006-08-22 11:34:12  europe\L190409
 * first version, by Peter Leeuwenburgh
 *
 */

package nl.nn.adapterframework.webcontrol;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

/**
 * Extension of HttpServletResponseRwapper on to capture response in a String.
 * 
 * @author  Peter Leeuwenburgh
 * @version Id
 */

public class IbisHttpServletResponseWrapper	extends HttpServletResponseWrapper {
	
	IbisServletOutputStream ibisServletOutputStream;
	StringWriter stringWriter;

	public IbisHttpServletResponseWrapper(HttpServletResponse httpServletResponse) {
		super(httpServletResponse);
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
}