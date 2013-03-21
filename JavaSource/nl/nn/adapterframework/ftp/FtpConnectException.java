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
 * $Log: FtpConnectException.java,v $
 * Revision 1.5  2011-11-30 13:52:04  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2005/12/07 15:43:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
 * @version $Id$
 */
public class FtpConnectException extends IbisException {
	public static final String version = "$RCSfile: FtpConnectException.java,v $  $Revision: 1.5 $ $Date: 2011-11-30 13:52:04 $";

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
