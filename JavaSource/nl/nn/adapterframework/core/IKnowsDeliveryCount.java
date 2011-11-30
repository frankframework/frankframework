/*
 * $Log: IKnowsDeliveryCount.java,v $
 * Revision 1.3  2011-11-30 13:51:55  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.1  2008/08/27 15:54:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
