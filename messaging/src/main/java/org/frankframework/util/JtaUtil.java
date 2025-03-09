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

import java.util.Iterator;
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
		String result;
		result="txName ["+TransactionSynchronizationManager.getCurrentTransactionName()+"]";
		if (txStatus!=null) {
			result+=" status new ["+txStatus.isNewTransaction()+"]";
			result+=" status completeted ["+txStatus.isCompleted()+"]";
			result+=" status rollbackOnly ["+txStatus.isRollbackOnly()+"]";
			result+=" status hasSavepoint ["+txStatus.hasSavepoint()+"]";
		} else {
			result+=" currently not in a transaction";
		}
		result+=" isolation ["+TransactionSynchronizationManager.getCurrentTransactionIsolationLevel()+"]";
		result+=" active ["+TransactionSynchronizationManager.isActualTransactionActive()+"]";
		boolean syncActive=TransactionSynchronizationManager.isSynchronizationActive();
		result+=" synchronization active ["+syncActive+"]";
		result+="\n";
		Map<Object, Object> resources = TransactionSynchronizationManager.getResourceMap();
		result += "resources:\n";
		if (resources==null) {
			result+="  map is null\n";
		} else {
			for (Iterator<Object> it=resources.keySet().iterator(); it.hasNext();) {
				Object key = it.next();
				Object resource = resources.get(key);
				result += ClassUtils.nameOf(key)+"("+key+"): "+ClassUtils.nameOf(resource)+"("+resource+")\n";
				if (resource instanceof JmsResourceHolder jrh) {
					result+="  connection: "+jrh.getConnection()+", session: "+jrh.getSession()+"\n";
				}
			}
		}
		if (syncActive) {
			List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
			result += "synchronizations:\n";
			for (int i=0; i<synchronizations.size(); i++) {
				TransactionSynchronization synchronization = synchronizations.get(i);
				result += ClassUtils.nameOf(synchronization)+"("+synchronization+")\n";
			}
		}
		return result;
	}


	/**
	 * returns true if the current thread is associated with a transaction
	 */
	public static boolean inTransaction() {
		return TransactionSynchronizationManager.isSynchronizationActive();
	}

}
