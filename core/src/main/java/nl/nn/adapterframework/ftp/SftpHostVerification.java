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
package nl.nn.adapterframework.ftp;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.logging.log4j.Logger;

import com.sshtools.j2ssh.transport.AbstractKnownHostsKeyVerification;
import com.sshtools.j2ssh.transport.InvalidHostFileException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;

/**
 * Utility class that handles events concerning hosts.
 * 
 * @author  John Dekker
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
