/*
 * $Log: ITransactionRequirements.java,v $
 * Revision 1.1  2011-12-05 15:30:02  l190409
 * first version
 *
 */
package nl.nn.adapterframework.core;

public interface ITransactionRequirements {

	boolean transactionalRequired();
	boolean transactionalAllowed();
}
