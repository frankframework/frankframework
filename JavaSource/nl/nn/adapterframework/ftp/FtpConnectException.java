/*
 * $Log: FtpConnectException.java,v $
 * Revision 1.3  2005-12-07 15:43:14  europe\L190409
 * made to exend IbisException
 *
 * Revision 1.1  2005/10/11 13:03:28  John Dekker <john.dekker@ibissource.org>
 * Supports retrieving files (FtpFileRetrieverPipe) and sending files (FtpSender)
 * via one of the FTP protocols (ftp, sftp, ftps both implicit as explicit).
 *
 */
package nl.nn.adapterframework.ftp;

import nl.nn.adapterframework.core.IbisException;

/**
 * @author John Dekker
 * @version Id
 */
public class FtpConnectException extends IbisException {
	public static final String version = "$RCSfile: FtpConnectException.java,v $  $Revision: 1.3 $ $Date: 2005-12-07 15:43:14 $";

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
