/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.jta;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jakarta.transaction.TransactionManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.util.StreamUtils;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.frankframework.util.Misc;
import org.frankframework.util.UUIDUtil;

/**
 * JtaTransactionManager-wrapper that enables to recover transaction logs produced by another instance.
 *
 * @author Gerrit van Brakel
 *
 */
public abstract class AbstractStatusRecordingTransactionManager extends ThreadConnectableJtaTransactionManager implements DisposableBean {
	protected Logger log = LogUtil.getLogger(this);

	private static final long serialVersionUID = 1L;
	private static final int TMUID_MAX_LENGTH=36; // 36 appears to work for Narayana i.c.w. MS_SQL, MySQL and MariaDB, avoiding SQLException: ConnectionImple.registerDatabase - ARJUNA017017: enlist of resource failed

	private @Getter @Setter String statusFile;
	private @Getter @Setter String uidFile;
	private @Getter String uid;

	protected enum Status {
		INITIALIZING,
		ACTIVE,
		PENDING,
		COMPLETED
	}

	protected abstract TransactionManager createTransactionManager() throws TransactionSystemException;

	/**
	 * Shutdown the transaction manager, attempting to complete all running transactions.
	 * @return true if a successful shutdown took place, or false if any transactions are pending that need to be recovered later.
	 */
	protected abstract boolean shutdownTransactionManager() throws TransactionSystemException;

	/*
	 * N.B. retrieveTransactionManager() is called once from JtaTransactionManager.initUserTransactionAndTransactionManager(),
	 * but only if no transactionManager or transactionManagerName are specified.
	 */
	@Override
	protected TransactionManager retrieveTransactionManager() throws TransactionSystemException {
		determineTmUid();
		writeStatus(Status.INITIALIZING);
		TransactionManager result = createTransactionManager();
		writeStatus(Status.ACTIVE);
		setTransactionManager(result);
		return result;
	}

	protected String determineTmUid() {
		String recordedTmUid = read(getUidFile());
		if (StringUtils.isNotEmpty(recordedTmUid)) {
			log.info("retrieved tmuid [{}] from [{}]", recordedTmUid, getUidFile());
			setUid(recordedTmUid);
		}
		if (StringUtils.isEmpty(getUid())) {
			String tmuid = Misc.getHostname()+"-"+ UUIDUtil.createSimpleUUID();
			if (tmuid.length()>TMUID_MAX_LENGTH) {
				tmuid = tmuid.substring(0, TMUID_MAX_LENGTH);
			}
			log.info("created tmuid [{}]", tmuid);
			setUid(tmuid);
		}
		if (!getUid().equals(recordedTmUid)) {
			write(getUidFile(),getUid());
		}
		return getUid();
	}

	private void setUid(String transactionManagerUid) {
		AppConstants.getInstance().setProperty("transactionmanager.uid", transactionManagerUid);
		this.uid = transactionManagerUid;
	}

	@Override
	public void destroy() {
		log.info("shutting down transaction manager");
		boolean noTransactionsPending = shutdownTransactionManager();
		writeStatus(noTransactionsPending ? Status.COMPLETED : Status.PENDING);
		log.info("transaction manager shutdown completed");
	}

	public void writeStatus(Status status) throws TransactionSystemException {
		write(getStatusFile(), status.toString());
	}

	public void write(String filename, String text) throws TransactionSystemException {
		if (StringUtils.isNotEmpty(filename)) {
			Path file = Paths.get(filename);
			Path folder = file.getParent();
			try {
				if (folder!=null) {
					Files.createDirectories(folder);
				}
				try (OutputStream fos = Files.newOutputStream(file)) {
					fos.write((text+"\n").getBytes(StandardCharsets.UTF_8));
				}
			} catch (Exception e) {
				throw new TransactionSystemException("Cannot write line ["+text+"] to file ["+file+"]", e);
			}
		}
	}

	public String read(String filename) {
		if (StringUtils.isNotEmpty(filename)) {
			Path file = Paths.get(filename);
			if (!Files.exists(file)) {
				return null;
			}
			try (InputStream fis = Files.newInputStream(file)) {
				return StreamUtils.copyToString(fis, StandardCharsets.UTF_8).trim();
			} catch (Exception e) {
				throw new TransactionSystemException("Cannot read from file ["+file+"]", e);
			}
		}
		return null;
	}

}
