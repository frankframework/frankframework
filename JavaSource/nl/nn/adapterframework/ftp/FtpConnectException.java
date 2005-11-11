/*
 * $Log: FtpConnectException.java,v $
 * Revision 1.2  2005-11-11 12:30:38  europe\l166817
 * Aanpassingen door John Dekker
 *
 * Revision 1.1  2005/10/11 13:03:28  John Dekker <john.dekker@ibissource.org>
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
	public static final String version = "$RCSfile: FtpConnectException.java,v $  $Revision: 1.2 $ $Date: 2005-11-11 12:30:38 $";

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
