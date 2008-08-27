/*
 * $Log: IKnowsDeliveryCount.java,v $
 * Revision 1.1  2008-08-27 15:54:49  europe\L190409
 * introduced delivery count calculation
 *
 */
package nl.nn.adapterframework.core;

/**
 * Interface to be implemented by Listeners that can find out the delivery count 
 * of the messages they receive.
 * 
 * @author  Gerrit van Brakel
 * @since	4.9  
 * @version Id
 */
public interface IKnowsDeliveryCount {
	
	int getDeliveryCount(Object rawMessage);

}
