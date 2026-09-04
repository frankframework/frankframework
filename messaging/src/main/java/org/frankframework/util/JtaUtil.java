/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package org.frankframework.util;

import java.util.List;
import java.util.Map;

import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Utility functions for JTA
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class JtaUtil {
//	/**
//	 * returns a meaningful string describing the transaction status.
//	 */
//	public static String displayTransactionStatus(int status) {
//		switch (status) {
//			case 	Status.STATUS_ACTIVE 			 : return status+"=STATUS_ACTIVE:"+ 	    " A transaction is associated with the target object and it is in the active state.";
//			case 	Status.STATUS_COMMITTED 		 : return status+"=STATUS_COMMITTED:"+ 	    " A transaction is associated with the target object and it has been committed.";
//			case 	Status.STATUS_COMMITTING 		 : return status+"=STATUS_COMMITTING:"+ 	" A transaction is associated with the target object and it is in the process of committing.";
//			case 	Status.STATUS_MARKED_ROLLBACK 	 : return status+"=STATUS_MARKED_ROLLBACK:"+" A transaction is associated with the target object and it has been marked for rollback, perhaps as a result of a setRollbackOnly operation.";
//			case 	Status.STATUS_NO_TRANSACTION 	 : return status+"=STATUS_NO_TRANSACTION:"+ " No transaction is currently associated with the target object.";
//			case 	Status.STATUS_PREPARED 			 : return status+"=STATUS_PREPARED:"+ 	    " A transaction is associated with the target object and it has been prepared.";
//			case 	Status.STATUS_PREPARING 		 : return status+"=STATUS_PREPARING:"+ 	    " A transaction is associated with the target object and it is in the process of preparing.";
//			case 	Status.STATUS_ROLLEDBACK 		 : return status+"=STATUS_ROLLEDBACK:"+ 	" A transaction is associated with the target object and the outcome has been determined to be rollback.";
//			case 	Status.STATUS_ROLLING_BACK 		 : return status+"=STATUS_ROLLING_BACK:"+ 	" A transaction is associated with the target object and it is in the process of rolling back.";
//			case 	Status.STATUS_UNKNOWN 	 		 : return status+"=STATUS_UNKNOWN:"+ 	    " A transaction is associated with the target object but its current status cannot be determined.";
//			default : return "unknown transaction status";
//		}
//	}

	public static String displayTransactionStatus(TransactionStatus txStatus) {
		StringBuilder result;
		result = new StringBuilder("txName [" + TransactionSynchronizationManager.getCurrentTransactionName() + "]");
		if (txStatus!=null) {
			result.append(" status new [").append(txStatus.isNewTransaction()).append("]");
			result.append(" status completeted [").append(txStatus.isCompleted()).append("]");
			result.append(" status rollbackOnly [").append(txStatus.isRollbackOnly()).append("]");
			result.append(" status hasSavepoint [").append(txStatus.hasSavepoint()).append("]");
		} else {
			result.append(" currently not in a transaction");
		}
		result.append(" isolation [").append(TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()).append("]");
		result.append(" active [").append(TransactionSynchronizationManager.isActualTransactionActive()).append("]");
		boolean syncActive=TransactionSynchronizationManager.isSynchronizationActive();
		result.append(" synchronization active [").append(syncActive).append("]");
		result.append("\n");

		Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
		result.append("resources:\n");

		if (resources.isEmpty()) {
			result.append("  map is null\n");
		} else {
			for (Map.Entry<Object, Object> entry : resources.entrySet()) {
				Object key = entry.getKey();
				Object resource = entry.getValue();

				result.append(ClassUtils.nameOf(key))
						.append("(")
						.append(key)
						.append("): ")
						.append(ClassUtils.nameOf(resource))
						.append("(")
						.append(resource)
						.append(")\n");
				if (resource instanceof JmsResourceHolder jrh) {
					result.append("  connection: ").append(jrh.getConnection()).append(", session: ").append(jrh.getSession()).append("\n");
				}
			}
		}
		if (syncActive) {
			List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
			result.append("synchronizations:\n");
			for (TransactionSynchronization synchronization : synchronizations) {
				result.append(ClassUtils.nameOf(synchronization)).append("(").append(synchronization).append(")\n");
			}
		}
		return result.toString();
	}


	/**
	 * returns true if the current thread is associated with a transaction
	 */
	public static boolean inTransaction() {
		return TransactionSynchronizationManager.isSynchronizationActive();
	}

}
