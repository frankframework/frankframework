/*
 * $Log: JtaUtil.java,v $
 * Revision 1.1  2004-03-23 17:14:31  L190409
 * initial version
 *
 */
package nl.nn.adapterframework.util;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

/**
 * Utility functions for JTA 
 * <p>$Id: JtaUtil.java,v 1.1 2004-03-23 17:14:31 L190409 Exp $</p>
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class JtaUtil {
	private static final String version="$Id: JtaUtil.java,v 1.1 2004-03-23 17:14:31 L190409 Exp $";
    private static UserTransaction utx;

	/**
	 * returns a meaningfull string describing the transaction status.
	 * @param status
	 * @return
	 */
	public static String displayTransactionStatus(int status) {
		switch (status) {
			case 	Status.STATUS_ACTIVE 			 : return status+"=STATUS_ACTIVE:"+ 	    " A transaction is associated with the target object and it is in the active state."; 
			case 	Status.STATUS_COMMITTED 		 : return status+"=STATUS_COMMITTED:"+ 	    " A transaction is associated with the target object and it has been committed."; 
			case 	Status.STATUS_COMMITTING 		 : return status+"=STATUS_COMMITTING:"+ 	" A transaction is associated with the target object and it is in the process of committing."; 
			case 	Status.STATUS_MARKED_ROLLBACK 	 : return status+"=STATUS_MARKED_ROLLBACK:"+" A transaction is associated with the target object and it has been marked for rollback, perhaps as a result of a setRollbackOnly operation."; 
			case 	Status.STATUS_NO_TRANSACTION 	 : return status+"=STATUS_NO_TRANSACTION:"+ " No transaction is currently associated with the target object.";
			case 	Status.STATUS_PREPARED 			 : return status+"=STATUS_PREPARED:"+ 	    " A transaction is associated with the target object and it has been prepared.";
			case 	Status.STATUS_PREPARING 		 : return status+"=STATUS_PREPARING:"+ 	    " A transaction is associated with the target object and it is in the process of preparing.";
			case 	Status.STATUS_ROLLEDBACK 		 : return status+"=STATUS_ROLLEDBACK:"+ 	" A transaction is associated with the target object and the outcome has been determined to be rollback.";
			case 	Status.STATUS_ROLLING_BACK 		 : return status+"=STATUS_ROLLING_BACK:"+ 	" A transaction is associated with the target object and it is in the process of rolling back.";
			case 	Status.STATUS_UNKNOWN 	 		 : return status+"=STATUS_UNKNOWN:"+ 	    " A transaction is associated with the target object but its current status cannot be determined.";
			default : return "unknown transaction status";
		}   
	}

	/**
	 * Convenience function for {@link displayTransactionStatus(int status)}
	 */
	public static String displayTransactionStatus(Transaction tx) {
		try {
			return displayTransactionStatus(tx.getStatus());
		} catch (Exception e) {
			return "exception obtaining transaction status from transaction ["+tx+"]: "+e.getMessage();
		}
	}
	/**
	 * Convenience function for {@link displayTransactionStatus(int status)}
	 */
	public static String displayTransactionStatus(UserTransaction utx) {
		try {
			return displayTransactionStatus(utx.getStatus());
		} catch (Exception e) {
			return "exception obtaining transaction status from transaction ["+utx+"]: "+e.getMessage();
		}
	}
	
	/**
	 * Convenience function for {@link displayTransactionStatus(int status)}
	 */
	public static String displayTransactionStatus(TransactionManager tm) {
		try {
			return displayTransactionStatus(tm.getStatus());
		} catch (Exception e) {
			return "exception obtaining transaction status from transactionmanager ["+tm+"]: "+e.getMessage();
		}
	}

	/** 
	 * returns true if the current thread is associated with a transaction
	 */
	public static boolean inTransaction(UserTransaction utx) throws SystemException {
		return utx != null && utx.getStatus() != Status.STATUS_NO_TRANSACTION;
	}

	/**
	 * Returns a UserTransaction object, that is used by Receivers and PipeLines to demarcate transactions. 
	 * @param ctx
	 * @param UserTransactionUrl
	 * @return
	 * @throws NamingException
	 */
	public static UserTransaction getUserTransaction(Context ctx, String UserTransactionUrl) throws NamingException {
	
		if (utx == null) {
			utx = (UserTransaction)ctx.lookup(UserTransactionUrl);
		}
		return utx;
	}
}
