package nl.nn.adapterframework.filesystem;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;

public class FtpFileSystemSender extends FileSystemSender<FTPFile, FtpFileSystem> {

	private FtpFileSystem ftpFileSystem;
	
//	@Override
//	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc)
//			throws SenderException, TimeOutException {
//		try {
//			IPipeLineSession session=null;
//			if (prc!=null) {
//				session=prc.getSession();
//			}
//			ftpFileSystem.getFtpSession().put(paramList, session, message, ftpFileSystem.getRemoteDirectory(), ftpFileSystem.getRemoteFilenamePattern(), true);
//		} catch(SenderException e) {
//			throw e;
//		} catch(Exception e) {
//			throw new SenderException("Error during ftp-ing " + message, e);
//		}
//		return message;
//	}
}