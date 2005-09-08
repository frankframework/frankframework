/*
 * $Log: JtaUtil.java,v $
 * Revision 1.6  2005-09-08 15:58:15  europe\L190409
 * added logging
 *
 * Revision 1.5  2004/10/05 09:57:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made version public
 *
 * Revision 1.4  2004/03/31 15:03:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.3  2004/03/26 10:42:38  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.2  2004/03/26 09:50:52  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 * Revision 1.1  2004/03/23 17:14:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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

import org.apache.log4j.Logger;

/**
 * Utility functions for JTA 
 * @version Id
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class JtaUtil {
	public static final String version="$RCSfile: JtaUtil.java,v $ $Revision: 1.6 $ $Date: 2005-09-08 15:58:15 $";
	private static Logger log = Logger.getLogger(JtaUtil.class);

    private static UserTransaction utx;

	/**
	 * returns a meaningful string describing the transaction status.
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
	 * Convenience function for {@link #displayTransactionStatus(int status)}
	 */
	public static String displayTransactionStatus(Transaction tx) {
		try {
			return displayTransactionStatus(tx.getStatus());
		} catch (Exception e) {
			return "exception obtaining transaction status from transaction ["+tx+"]: "+e.getMessage();
		}
	}
	/**
	 * Convenience function for {@link #displayTransactionStatus(int status)}
	 */
	public static String displayTransactionStatus(UserTransaction utx) {
		try {
			return displayTransactionStatus(utx.getStatus());
		} catch (Exception e) {
			return "exception obtaining transaction status from transaction ["+utx+"]: "+e.getMessage();
		}
	}
	
	/**
	 * Convenience function for {@link #displayTransactionStatus(int status)}
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
	 */
	public static UserTransaction getUserTransaction(Context ctx, String userTransactionUrl) throws NamingException {
	
		if (utx == null) {
			log.debug("looking up UserTransaction ["+userTransactionUrl+"] in context ["+ctx.toString()+"]");
			utx = (UserTransaction)ctx.lookup(userTransactionUrl);
		}
		return utx;
	}
}
