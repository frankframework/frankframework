/*
 * $Log: FtpConnectException.java,v $
 * Revision 1.1  2005-10-11 13:03:28  europe\m00f531
 * Supports retrieving files (FtpFileRetrieverPipe) and sending files (FtpSender)
 * via one of the FTP protocols (ftp, sftp, ftps both implicit as explicit).
 *
 */
package nl.nn.adapterframework.ftp;

/**
 * @author John Dekker
 * @version Id
 */
public class FtpConnectException extends Exception {
	public static final String version = "$RCSfile: FtpConnectException.java,v $  $Revision: 1.1 $ $Date: 2005-10-11 13:03:28 $";

	public FtpConnectException() {
		super();
	}

	public FtpConnectException(String message) {
		super(message);
	}

	public FtpConnectException(String message, Throwable cause) {
		super(message, cause);
	}

	public FtpConnectException(Throwable cause) {
		super(cause);
	}

}
