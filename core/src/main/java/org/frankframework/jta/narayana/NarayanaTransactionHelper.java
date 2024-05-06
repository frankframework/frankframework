/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.jta.narayana;

import jakarta.jms.JMSException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionManager;

import org.jboss.narayana.jta.jms.ConnectionProxy;
import org.jboss.narayana.jta.jms.SessionProxy;
import org.jboss.narayana.jta.jms.TransactionHelperImpl;

import com.arjuna.ats.arjuna.coordinator.ActionStatus;
import com.arjuna.ats.internal.arjuna.coordinator.ReaperThread;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionImple;
import com.arjuna.ats.internal.jta.utils.arjunacore.StatusConverter;

public class NarayanaTransactionHelper extends TransactionHelperImpl {

	private final TransactionManager transactionManager;

	public NarayanaTransactionHelper(TransactionManager transactionManager) {
		super(transactionManager);
		this.transactionManager = transactionManager;
	}

	/**
	 * Connections were not always closed, because the super implementation of this method returns false too often. {@link ConnectionProxy#close() } and {@link SessionProxy#close() }
	 * both call this method before attempting to close the connection. When the connection is marked as
	 * {@link Status#STATUS_ROLLEDBACK STATUS_ROLLEDBACK} this method will return true, claiming it's available.
	 * This scenario happened when a JMSMessage was marked for rollback by the {@link ReaperThread} while being detected as 'stuck'
	 * because of a (too) short timeout. While, even though the timeout was too short, no other unexpected behavior was detected.
	 * This mechanism however, will prevent the connection from being closed, causing a connection-leak to occur.
	 * <p>
	 * NOTE:
	 *
	 * This problem is caused because in the {@link TransactionImple#getStatus()} the {@link ActionStatus internal connection status} is
	 * {@link StatusConverter#convert(int) converted} to a {@link Status javax.transaction.Status}. Because of the conversion the actual state is lost, and
	 * {@link ActionStatus#ABORTED} connections are marked as {@link Status#STATUS_ROLLEDBACK STATUS_ROLLEDBACK}.
	 */
	@Override
	public boolean isTransactionAvailable() throws JMSException {
		try {
			return super.isTransactionAvailable() && transactionManager.getStatus() != Status.STATUS_ROLLEDBACK;
		} catch (SystemException e) {
			JMSException jmsException = new JMSException("failed to get transaction status");
			jmsException.setLinkedException(e);
			throw jmsException;
		}
	}
}
