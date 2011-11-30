/*
 * $Log: IfsaProviderListener.java,v $
 * Revision 1.8  2011-11-30 13:51:43  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2008/01/03 15:46:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * split IfsaProviderListener into a Pulling and a Pushing version
 *
 * Revision 1.5  2007/11/21 13:17:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved error logging
 *
 * Revision 1.4  2007/11/15 12:38:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed message wrapping
 *
 * Revision 1.3  2007/10/17 09:32:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * store originalRawMessage when wrapper is created, use it to send reply
 *
 * Revision 1.2  2007/10/16 08:39:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved IfsaException and IfsaMessageProtocolEnum back to main package
 *
 * Revision 1.1  2007/10/16 08:15:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced switch class for jms and ejb
 *
 * Revision 1.33  2007/10/03 08:32:41  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed HashMap to Map
 *
 * Revision 1.32  2007/09/25 11:33:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * show headers of incoming messages
 *
 * Revision 1.31  2007/09/13 09:12:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * move message wrapper from ifsa to receivers
 *
 * Revision 1.30  2007/09/05 15:48:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * moved XA determination capabilities to IfsaConnection
 *
 * Revision 1.29  2007/08/27 11:50:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * provide default result for RR
 *
 * Revision 1.28  2007/08/10 11:18:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed attribute 'transacted'
 * automatic determination of transaction state and capabilities
 * removed (more or less hidden) attribute 'commitOnState'
 * warning about non XA FF
 *
 * Revision 1.27  2007/02/16 14:19:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.26  2007/02/05 14:57:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * set default timeout to 3000
 *
 * Revision 1.25  2006/11/01 14:22:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE for null commitOnState
 *
 * Revision 1.24  2006/10/13 08:23:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * do not process null UDZ
 *
 * Revision 1.23  2006/10/13 08:11:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * copy UDZ to session-variables
 *
 * Revision 1.22  2006/08/21 15:08:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.21  2006/07/17 08:54:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * documented custom property ifsa.provider.useSelectors
 *
 * Revision 1.20  2006/03/08 13:55:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * getRawMessage now returns null again if no message received if transacted, 
 * to avoid transaction time out
 *
 * Revision 1.19  2006/02/20 15:49:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved handling of PoisonMessages, should now work under transactions control
 *
 * Revision 1.18  2006/01/05 13:55:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.17  2005/12/20 16:59:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented support for connection-pooling
 *
 * Revision 1.16  2005/10/27 08:48:31  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced RunStateEnquiries
 *
 * Revision 1.15  2005/10/24 15:14:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * shuffled positions of methods
 *
 * Revision 1.14  2005/09/26 11:47:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Jms-commit only if not XA-transacted
 * ifsa-messageWrapper for (non-serializable) ifsa-messages
 *
 * Revision 1.13  2005/09/13 15:48:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed acknowledge mode back to AutoAcknowledge
 *
 * Revision 1.12  2005/07/28 07:31:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * change default acknowledge mode to CLIENT
 *
 * Revision 1.11  2005/06/20 09:14:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid excessive logging
 *
 * Revision 1.10  2005/06/13 15:08:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid excessive logging in debug mode
 *
 * Revision 1.9  2005/06/13 12:43:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for pooled sessions and for XA-support
 *
 * Revision 1.8  2005/02/17 09:45:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * increased logging
 *
 * Revision 1.7  2005/01/13 08:55:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Make threadContext-attributes available in PipeLineSession
 *
 * Revision 1.6  2004/09/22 07:03:36  Johan Verrips <johan.verrips@ibissource.org>
 * Added logstatements for closing receiver and session
 *
 * Revision 1.5  2004/09/22 06:48:08  Johan Verrips <johan.verrips@ibissource.org>
 * Changed loglevel in getStringFromRawMessage to warn
 *
 * Revision 1.4  2004/07/19 09:50:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * try to send exceptionmessage as reply when sending reply results in exception
 *
 * Revision 1.3  2004/07/15 07:43:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.2  2004/07/08 12:55:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * logging refinements
 *
 * Revision 1.1  2004/07/05 14:28:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * First version, converted from IfsaServiceListener
 *
 * Revision 1.4  2004/03/26 07:25:42  Johan Verrips <johan.verrips@ibissource.org>
 * Updated erorhandling
 *
 * Revision 1.3  2004/03/24 15:27:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * solved uncaught exception in error message
 *
 */
package nl.nn.adapterframework.extensions.ifsa.jms;

public class IfsaProviderListener extends PushingIfsaProviderListener {

}
