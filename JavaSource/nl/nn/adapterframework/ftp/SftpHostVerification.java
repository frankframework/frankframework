/*
 * $Log: SftpHostVerification.java,v $
 * Revision 1.5  2011-11-30 13:52:04  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2007/02/12 13:50:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.2  2005/12/07 15:44:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * logging to log4j instead of System.out
 *
 * Revision 1.1  2005/11/07 08:21:36  John Dekker <john.dekker@ibissource.org>
 * Enable sftp public/private key authentication
 *
 */
package nl.nn.adapterframework.ftp;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

import com.sshtools.j2ssh.transport.AbstractKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

/**
 * Utility class that handles events concerning hosts.
 * 
 * @author  John Dekker
 * @version Id
 */
public class SftpHostVerification extends AbstractKnownHostsKeyVerification {
	private Logger log = LogUtil.getLogger(this);
	/**
	 * Constructs the verification instance with the specified known_hosts
	 * file.
	 *
	 * @param knownhosts the path to the known_hosts file
	 * @throws InvalidHostFileException if the known_hosts file is invalid.
	 */
	public SftpHostVerification(String knownhosts) throws InvalidHostFileException {
		super(knownhosts);
	}

	/**
	 * Prompts the user through the console to verify the host key.
	 *
	 * @param host the name of the host
	 * @param pk the current public key of the host
	 * @param actual the actual public key supplied by the host
	 */
	public void onHostKeyMismatch(String host, SshPublicKey pk, SshPublicKey actual) {
		try {
			log.warn("The host key supplied by [" + host + "] is [" + actual.getFingerprint()+"]");
			log.warn("The current allowed key for [" + host + "] is [" + pk.getFingerprint()+"]");
		}
		catch (Exception e) {
			log.error(e);
		}
	}

	/**
	 * Prompts the user through the console to verify the host key.
	 *
	 * @param host the name of the host
	 * @param pk the public key supplied by the host
	 */
	public void onUnknownHost(String host, SshPublicKey pk) {
		try {
			log.warn("The host [" + host + "], key fingerprint [" + pk.getFingerprint()+"] is currently unknown to the system");
		}
		catch (Exception e) {
			log.error(e);
		}
	}

}
