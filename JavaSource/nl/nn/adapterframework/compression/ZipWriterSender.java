/*
 * $Log: ZipWriterSender.java,v $
 * Revision 1.4  2010-03-25 12:55:31  L190409
 * added attribute closeInputstreamOnExit
 *
 * Revision 1.3  2010/03/10 14:30:06  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.1  2010/01/06 17:57:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * classes for reading and writing zip archives
 *
 */
package nl.nn.adapterframework.compression;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Sender that writes an entry to a ZipStream, similar to ZipWriterPipe with action='write'.
 * Filename and contents are taken from parameters. If one of the parameters is not present, the input message 
 * is used for either filename or contents.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.compression.ZipWriterSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setZipWriterHandle(String) zipWriterHandle}</td>  <td>session key used to refer to zip session. Must be used if ZipWriterPipes are nested</td><td>"zipwriterhandle"</td></tr>
 * <tr><td>{@link #setCloseInputstreamOnExit(boolean) closeInputstreamOnExit}</td><td>when set to <code>false</code>, the inputstream is not closed after it has been used</td><td>true</td></tr>
 * <tr><td>{@link #setCharset(String) charset}</td><td>characterset used for writing zip entry</td><td>UTF-8</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>filename</td><td>string</td><td>filename of the zipentry</td></tr>
 * <tr><td>contents</td><td>string</td><td>contents of the zipentry</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9.10
 * @version Id
 */
public class ZipWriterSender extends SenderWithParametersBase {

	private static final String PARAMETER_FILENAME="filename";
	private static final String PARAMETER_CONTENTS="contents";
 
	private String zipWriterHandle="zipwriterhandle";
	private boolean closeInputstreamOnExit=true;
	private String charset=StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
	
	private Parameter filenameParameter=null;
	private Parameter contentsParameter=null;

	public void configure() throws ConfigurationException {
		super.configure();
		filenameParameter=paramList.findParameter(PARAMETER_FILENAME);
		contentsParameter=paramList.findParameter(PARAMETER_CONTENTS);
		if (filenameParameter==null && contentsParameter==null) {
			throw new ConfigurationException(getLogPrefix()+"parameter '"+PARAMETER_FILENAME+"' or parameter '"+PARAMETER_CONTENTS+"' is required");
		}
	}



	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		ParameterValueList pvl;
		try {
			pvl = prc.getValues(paramList);
		} catch (ParameterException e) {
			throw new SenderException("cannot determine filename and/or contents of zip entry",e);
		}

		PipeLineSession session = prc.getSession();
		ZipWriter sessionData=ZipWriter.getZipWriter(session,getZipWriterHandle());
		if (sessionData==null) {
			throw new SenderException("zipWriterHandle in session key ["+getZipWriterHandle()+"] is not open");		
		} 
		String filename=filenameParameter==null?message:(String)pvl.getParameterValue(PARAMETER_FILENAME).getValue();
		try {
			if (contentsParameter==null) {
				if (message!=null) {
					sessionData.writeEntry(filename,message,isCloseInputstreamOnExit(),getCharset());
				}
			} else {
				Object paramValue=pvl.getParameterValue(PARAMETER_CONTENTS).getValue();
				sessionData.writeEntry(filename,paramValue,isCloseInputstreamOnExit(),getCharset());
			}
			return message;
		} catch (UnsupportedEncodingException e) {
			throw new SenderException(getLogPrefix()+"cannot encode zip entry", e);
		} catch (CompressionException e) {
			throw new SenderException(getLogPrefix()+"cannot store zip entry", e);
		} catch (IOException e) {
			throw new SenderException(getLogPrefix()+"cannot store zip entry", e);
		}
	}


	public void setCloseInputstreamOnExit(boolean b) {
		closeInputstreamOnExit = b;
	}
	public void setCloseStreamOnExit(boolean b) {
		ConfigurationWarnings.getInstance().add("attribute 'closeStreamOnExit' has been renamed into 'closeInputstreamOnExit'");
		setCloseInputstreamOnExit(b);
	}
	public boolean isCloseInputstreamOnExit() {
		return closeInputstreamOnExit;
	}

	public void setCharset(String string) {
		charset = string;
	}
	public String getCharset() {
		return charset;
	}
	
	public void setZipWriterHandle(String string) {
		zipWriterHandle = string;
	}
	public String getZipWriterHandle() {
		return zipWriterHandle;
	}

}
