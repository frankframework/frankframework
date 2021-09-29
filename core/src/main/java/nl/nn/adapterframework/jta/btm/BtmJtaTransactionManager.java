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
package nl.nn.adapterframework.jta.btm;

import java.io.FileOutputStream;

import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.jta.JtaTransactionManager;

import bitronix.tm.BitronixTransactionManager;
import bitronix.tm.TransactionManagerServices;
import lombok.Getter;
import lombok.Setter;

public class BtmJtaTransactionManager extends JtaTransactionManager implements DisposableBean {
	
	private enum Status {
		INITIALIZING,
		RUNNING,
		PENDING,
		COMPLETED;
	}
	
	private @Getter @Setter String statusFile;
	
	private volatile BitronixTransactionManager transactionManager;

	@Override
	protected UserTransaction retrieveUserTransaction() throws TransactionSystemException {
		return getBitronixTransactionManager();
	}

	@Override
	protected TransactionManager retrieveTransactionManager() throws TransactionSystemException {
		return getBitronixTransactionManager();
	}

	private BitronixTransactionManager getBitronixTransactionManager() throws TransactionSystemException {
		BitronixTransactionManager localRef = transactionManager;
		if (localRef == null) {
			synchronized (this) {
				localRef = transactionManager;
				if (localRef == null) {
					writeStatus(Status.INITIALIZING);
					transactionManager = localRef = TransactionManagerServices.getTransactionManager();
					writeStatus(Status.RUNNING);
				}
			}
		}
		return localRef;
	}
	
	
	@Override
	public void destroy() throws Exception {
		transactionManager.shutdown();
		int inflightCount = transactionManager.getInFlightTransactionCount();
		writeStatus(inflightCount>0 ? Status.PENDING : Status.COMPLETED);
	}

	private void writeStatus(Status status) throws TransactionSystemException {
		String statusFile = getStatusFile(); 
//		if (StringUtils.isEmpty(statusFile)) {
//			Configuration configuration = TransactionManagerServices.getConfiguration();
//			String logFile = configuration.getLogPart1Filename();
//			if (StringUtils.isNotEmpty(logFile)) {
//				statusFile = FilenameUtils.getPath(logFile)+"/"+DEFAULT_STATUS_FILENAME;
//				setStatusFile(statusFile);
//			}
//		}
		if (StringUtils.isNotEmpty(statusFile)) {
			try (FileOutputStream fos = new FileOutputStream(statusFile)) {
				fos.write(status.toString().getBytes());
			} catch (Exception e) {
				throw new TransactionSystemException("Cannot write status ["+status+"] to file ["+getStatusFile()+"]", e);
			}
		}
	}
	
}
