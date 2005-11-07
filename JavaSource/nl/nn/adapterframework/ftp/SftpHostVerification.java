/*
 * $Log: SftpHostVerification.java,v $
 * Revision 1.1  2005-11-07 08:21:36  europe\m00f531
 * Enable sftp public/private key authentication
 *
 */
package nl.nn.adapterframework.ftp;

import com.sshtools.j2ssh.transport.AbstractKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

/**
 * 
 * 
 * @author m00f531
 * @since  
 * @version Id
 */
public class SftpHostVerification extends AbstractKnownHostsKeyVerification {
	/**
	 * <p>
	 * Constructs the verification instance with the specified known_hosts
	 * file.
	 * </p>
	 *
	 * @param knownhosts the path to the known_hosts file
	 * @throws InvalidHostFileException if the known_hosts file is invalid.
	 */
	public SftpHostVerification(String knownhosts) throws InvalidHostFileException {
		super(knownhosts);
	}

	/**
	 * <p>
	 * Prompts the user through the console to verify the host key.
	 * </p>
	 *
	 * @param host the name of the host
	 * @param pk the current public key of the host
	 * @param actual the actual public key supplied by the host
	 */
	public void onHostKeyMismatch(String host, SshPublicKey pk, SshPublicKey actual) {
		try {
			System.err.println("The host key supplied by " + host + " is: " + actual.getFingerprint());
			System.err.println("The current allowed key for " + host + " is: " + pk.getFingerprint());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * <p>
	 * Prompts the user through the console to verify the host key.
	 * </p>
	 *
	 * @param host the name of the host
	 * @param pk the public key supplied by the host
	 *
	 * @since 0.2.0
	 */
	public void onUnknownHost(String host, SshPublicKey pk) {
		try {
			System.err.println("The host " + host + " is currently unknown to the system");
			System.err.println("The host key fingerprint is: " + pk.getFingerprint());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

}
