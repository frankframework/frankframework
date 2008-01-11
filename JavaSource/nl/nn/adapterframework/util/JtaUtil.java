/*
 * $Log: JtaUtil.java,v $
 * Revision 1.16  2008-01-11 09:59:33  europe\L190409
 * changed attributed definitions to Spring's
 * removed some functions
 *
 * Revision 1.15  2007/12/10 10:23:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed some functions
 *
 * Revision 1.14  2007/11/21 13:18:06  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added setRollBackOnly
 *
 * Revision 1.13  2007/08/10 11:22:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added non-argument inTransaction()
 *
 * Revision 1.12  2007/06/08 12:18:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * do not rollback after exception on commit if status is already final
 *
 * Revision 1.11  2007/05/08 16:01:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed stacktrace from debug-logging while obtaining user-transaction
 *
 * Revision 1.10  2007/02/12 14:12:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.9  2006/09/18 11:46:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * lookup UserTransaction only when necessary
 *
 * Revision 1.8  2006/09/14 11:47:10  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * optimized transactionStateCompatible()
 *
 * Revision 1.7  2006/08/21 15:14:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of transaction attribute handling
 * configuration of user transaction url in appconstants.properties
 *
 * Revision 1.6  2005/09/08 15:58:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;
import org.springframework.transaction.TransactionDefinition;

/**
 * Utility functions for JTA 
 * @version Id
 * @author Gerrit van Brakel
 * @since  4.1
 */
public class JtaUtil {
	public static final String version="$RCSfile: JtaUtil.java,v $ $Revision: 1.16 $ $Date: 2008-01-11 09:59:33 $";
	private static Logger log = LogUtil.getLogger(JtaUtil.class);
	
	private static final String USERTRANSACTION_URL1_KEY="jta.userTransactionUrl1";
	private static final String USERTRANSACTION_URL2_KEY="jta.userTransactionUrl2";
	
	public static final int TRANSACTION_ATTRIBUTE_DEFAULT=TransactionDefinition.PROPAGATION_SUPPORTS;

	public static final String TRANSACTION_ATTRIBUTE_REQUIRED_STR="Required";
	public static final String TRANSACTION_ATTRIBUTE_REQUIRES_NEW_STR="RequiresNew";
	public static final String TRANSACTION_ATTRIBUTE_MANDATORY_STR="Mandatory";
	public static final String TRANSACTION_ATTRIBUTE_NOT_SUPPORTED_STR="NotSupported";
	public static final String TRANSACTION_ATTRIBUTE_SUPPORTS_STR="Supports";
	public static final String TRANSACTION_ATTRIBUTE_NEVER_STR="Never";

	public static final String transactionAttributes[]=
		{ 
			TRANSACTION_ATTRIBUTE_REQUIRED_STR,
			TRANSACTION_ATTRIBUTE_REQUIRES_NEW_STR,
			TRANSACTION_ATTRIBUTE_MANDATORY_STR,
			TRANSACTION_ATTRIBUTE_NOT_SUPPORTED_STR, 
			TRANSACTION_ATTRIBUTE_SUPPORTS_STR,
			TRANSACTION_ATTRIBUTE_NEVER_STR
		};

	public static final int transactionAttributeNums[]=
		{ 
			TransactionDefinition.PROPAGATION_REQUIRED,
			TransactionDefinition.PROPAGATION_REQUIRES_NEW,
			TransactionDefinition.PROPAGATION_MANDATORY,
			TransactionDefinition.PROPAGATION_NOT_SUPPORTED, 
			TransactionDefinition.PROPAGATION_SUPPORTS,
			TransactionDefinition.PROPAGATION_NEVER
		};

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

//	/**
//	 * Convenience function for {@link #displayTransactionStatus(int status)}
//	 */
//	public static String displayTransactionStatus(Transaction tx) {
//		try {
//			return displayTransactionStatus(tx.getStatus());
//		} catch (Exception e) {
//			return "exception obtaining transaction status from transaction ["+tx+"]: "+e.getMessage();
//		}
//	}
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
	
//	/**
//	 * Convenience function for {@link #displayTransactionStatus(int status)}
//	 */
//	public static String displayTransactionStatus(TransactionManager tm) {
//		try {
//			return displayTransactionStatus(tm.getStatus());
//		} catch (Exception e) {
//			return "exception obtaining transaction status from transactionmanager ["+tm+"]: "+e.getMessage();
//		}
//	}

//	/**
//	 * Convenience function for {@link #displayTransactionStatus(int status)}
//	 */
//	public static String displayTransactionStatus(TransactionStatus txStatus) {
//		String result="";
//		if (txStatus instanceof DefaultTransactionStatus) {
//			Object tr = ((DefaultTransactionStatus)txStatus).getTransaction();
//			if (tr instanceof JtaTransactionObject) {
//				JtaTransactionObject jto=(JtaTransactionObject)tr;
//				UserTransaction utr=jto.getUserTransaction();
//				result= "transaction: "+ToStringBuilder.reflectionToString(utr, ToStringStyle.MULTI_LINE_STYLE);;
//			} else {
//				result= "transaction: "+ToStringBuilder.reflectionToString(tr, ToStringStyle.MULTI_LINE_STYLE);;
//			}
//		} else { 
//			result= "txStatus: "+ToStringBuilder.reflectionToString(txStatus, ToStringStyle.MULTI_LINE_STYLE);
//		}
//		return result;		
//	}


	/**
	 * Convenience function for {@link #displayTransactionStatus(int status)}
	 */
	public static String displayTransactionStatus() {
		UserTransaction utx;
		try {
			utx = getUserTransaction();
		} catch (Exception e) {
			return "exception obtaining user transaction: "+e.getMessage();
		}
		return displayTransactionStatus(utx);
	}


	/** 
	 * returns true if the current thread is associated with a transaction
	 */
	private static boolean inTransaction(UserTransaction utx) throws SystemException {
		return utx != null && utx.getStatus() != Status.STATUS_NO_TRANSACTION;
	}
	public static boolean inTransaction() throws SystemException, NamingException {
		return inTransaction(getUserTransaction());
	}

	/**
	 * Returns a UserTransaction object, that is used by Receivers and PipeLines to demarcate transactions. 
	 */
//	public static UserTransaction getUserTransaction(Context ctx, String userTransactionUrl) throws NamingException {
//	
//		if (utx == null) {
//			log.debug("looking up UserTransaction ["+userTransactionUrl+"] in context ["+ctx.toString()+"]");
//			utx = (UserTransaction)ctx.lookup(userTransactionUrl);
//		}
//		return utx;
//	}

	/**
	 * Returns a UserTransaction object, that is used by Receivers and PipeLines to demarcate transactions. 
	 */
	public static UserTransaction getUserTransaction() throws NamingException {
		if (utx == null) {
			Context ctx= (Context) new InitialContext();
			String url = AppConstants.getInstance().getProperty(USERTRANSACTION_URL1_KEY,null);
			log.debug("looking up UserTransaction ["+url+"] in context ["+ctx.toString()+"]");
			try {
				utx = (UserTransaction)ctx.lookup(url);
			} catch (Exception e) {
				log.debug("Could not lookup UserTransaction from url ["+url+"], will try alternative uri: "+e.getMessage());
				url = AppConstants.getInstance().getProperty(USERTRANSACTION_URL2_KEY,null);
				log.debug("looking up UserTransaction ["+url+"] in context ["+ctx.toString()+"]");
				utx = (UserTransaction)ctx.lookup(url);
			}
		}
		return utx;
	}

	
	public static int getTransactionAttributeNum(String transactionAttribute) {
		int i=transactionAttributes.length-1;
		while (i>=0 && !transactionAttributes[i].equalsIgnoreCase(transactionAttribute))
			i--; // try next
		return transactionAttributeNums[i]; 
	}

	public static String getTransactionAttributeString(int transactionAttribute) {
		if (transactionAttribute<0 || transactionAttribute>=transactionAttributes.length) {
			return "UnknownTransactionAttribute:"+transactionAttribute;
		}
		switch (transactionAttribute) {
			case TransactionDefinition.PROPAGATION_MANDATORY:
			return TRANSACTION_ATTRIBUTE_MANDATORY_STR;
    
			case TransactionDefinition.PROPAGATION_NEVER:
			return TRANSACTION_ATTRIBUTE_NEVER_STR;
    
			case TransactionDefinition.PROPAGATION_NOT_SUPPORTED:
			return TRANSACTION_ATTRIBUTE_NOT_SUPPORTED_STR;
    
			case TransactionDefinition.PROPAGATION_SUPPORTS:
			return TRANSACTION_ATTRIBUTE_SUPPORTS_STR;
    
			case TransactionDefinition.PROPAGATION_REQUIRED:
			return TRANSACTION_ATTRIBUTE_REQUIRED_STR;
    
			case TransactionDefinition.PROPAGATION_REQUIRES_NEW:
			return TRANSACTION_ATTRIBUTE_REQUIRES_NEW_STR;
    
			default:
			return null;
		}
	}
	
	public static boolean transactionStateCompatible(int transactionAttribute) throws SystemException, NamingException {
		if (transactionAttribute==TransactionDefinition.PROPAGATION_NEVER) {
			return !inTransaction(getUserTransaction());
		} else if (transactionAttribute==TransactionDefinition.PROPAGATION_MANDATORY) {
			return inTransaction(getUserTransaction());
		}
		return true;
	}

//	public static boolean isolationRequired(int transactionAttribute) throws SystemException, TransactionException, NamingException {
//		if (transactionAttribute!=TRANSACTION_ATTRIBUTE_REQUIRES_NEW &&
//		    transactionAttribute!=TRANSACTION_ATTRIBUTE_NOT_SUPPORTED) {
//		    	return false;
//		}
//		if (!transactionStateCompatible(transactionAttribute)) {
//			throw new TransactionException("transaction attribute ["+getTransactionAttributeString(transactionAttribute)+"] not compatible with state ["+displayTransactionStatus(utx)+"]");
//		}
//		UserTransaction utx = getUserTransaction();
//		return inTransaction(utx) &&
//				(transactionAttribute==TRANSACTION_ATTRIBUTE_REQUIRES_NEW ||
//				 transactionAttribute==TRANSACTION_ATTRIBUTE_NOT_SUPPORTED);
//	}

//	public static boolean newTransactionRequired(int transactionAttribute) throws SystemException, TransactionException, NamingException {
//		if (!transactionStateCompatible(transactionAttribute)) {
//			throw new TransactionException("transaction attribute ["+getTransactionAttributeString(transactionAttribute)+"] not compatible with state ["+displayTransactionStatus(utx)+"]");
//		}
//		if (transactionAttribute==TRANSACTION_ATTRIBUTE_REQUIRED) {
//			UserTransaction utx = getUserTransaction();
//			return !inTransaction(utx);
//		}
//		return transactionAttribute==TRANSACTION_ATTRIBUTE_REQUIRES_NEW;
//	}

//	private static boolean stateEvaluationRequired(int transactionAttribute) {
//		return transactionAttribute>=0 && 
//			   transactionAttribute!=TRANSACTION_ATTRIBUTE_REQUIRES_NEW &&
//			   transactionAttribute!=TRANSACTION_ATTRIBUTE_SUPPORTS;
//	}
	
//	public static void startTransaction() throws NamingException, NotSupportedException, SystemException {
//		log.debug("starting new transaction");
//		utx=getUserTransaction();
//		utx.begin();
//	}
//
//	public static void finishTransaction() throws NamingException, IllegalStateException, SecurityException, SystemException {
//		finishTransaction(false);
//	}

//	public static void setRollBackOnly() throws NamingException, IllegalStateException, SystemException {
//		UserTransaction utx=JtaUtil.getUserTransaction();
//		if (inTransaction(utx)) {
//			log.debug("marking transaction for rollback");
//			utx.setRollbackOnly();
//		}
//	}
	
//	public static void finishTransaction(boolean rollbackonly) throws NamingException, IllegalStateException, SecurityException, SystemException {
//		utx=getUserTransaction();
//		try {
//			if (inTransaction(utx) && !rollbackonly) {
//				log.debug("committing transaction");
//				utx.commit();
//			} else {
//				log.debug("rolling back transaction");
//				utx.rollback();
//			}
//		} catch (Throwable t1) {
//			try {
//				int currentStatus=-1;
//				try {
//					currentStatus=utx.getStatus();
//				} catch (Throwable t) {
//					log.debug("caught exception obtaining transaction status: "+ t.getMessage());
//				}
//				if (currentStatus != Status.STATUS_COMMITTED &&
//					currentStatus != Status.STATUS_NO_TRANSACTION &&
//					currentStatus != Status.STATUS_ROLLEDBACK &&
//					currentStatus != Status.STATUS_ROLLING_BACK) {
//						log.warn("current status ["+displayTransactionStatus(currentStatus)+"], trying to roll back transaction after exception ",t1);
//						utx.rollback();
//				} else {
//					log.info("current status ["+displayTransactionStatus(currentStatus)+"], will not issue rollback command");
//				}
//			} catch (Throwable t2) {
//				log.warn("exception rolling back transaction",t2);
//			}
//		}		
//	}
}
