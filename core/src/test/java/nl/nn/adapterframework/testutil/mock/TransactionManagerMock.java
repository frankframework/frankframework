package nl.nn.adapterframework.testutil.mock;

import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import lombok.Getter;

public class TransactionManagerMock implements PlatformTransactionManager {
	private static Queue<TransactionStatus> items = new ConcurrentLinkedQueue<>();

	@Override
	public TransactionStatus getTransaction(TransactionDefinition definition) throws TransactionException {
		TransactionStatus status = DummyTransactionStatus.newMock();
		items.add(status);
		return status;
	}

	@Override
	public void commit(TransactionStatus status) throws TransactionException {
		if(status instanceof DummyTransactionStatus) {
			((DummyTransactionStatus) status).completed = true;
		}
	}

	@Override
	public void rollback(TransactionStatus status) throws TransactionException {
		status.setRollbackOnly();
		commit(status);
	}


	public static void reset() {
		items = new ConcurrentLinkedQueue<>();
	}
	public static synchronized DummyTransactionStatus peek() {
		return (DummyTransactionStatus) items.peek();
	}

	public abstract static class DummyTransactionStatus implements TransactionStatus {
		private @Getter(onMethod = @__(@Override)) boolean rollbackOnly = false;
		private @Getter(onMethod = @__(@Override)) boolean completed = false;
		public static DummyTransactionStatus newMock() {
			return mock(DummyTransactionStatus.class, CALLS_REAL_METHODS);
		}

		@Override
		public void setRollbackOnly() {
			this.rollbackOnly = true;
		}

		public boolean hasBeenRolledBack() {
			return rollbackOnly && isCompleted();
		}

		@Override
		public String toString() {
			return "txStatus rollbackOnly["+rollbackOnly+"] completed["+completed+"]";
		}
	}
}
