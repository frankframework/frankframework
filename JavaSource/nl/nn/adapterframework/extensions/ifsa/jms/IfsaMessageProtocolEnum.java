package nl.nn.adapterframework.extensions.ifsa.jms;

import java.util.Iterator;
import java.util.Map;
import java.util.List;

import org.apache.commons.lang.enum.Enum;
/**
 * Enumeration of IFSA message protocols.
 * <p>Creation date: (13-05-2003 9:06:33)</p>
 * @author Johan Verrips IOS
 * @version Id
 */
public class IfsaMessageProtocolEnum extends Enum {
	
   public static final IfsaMessageProtocolEnum REQUEST_REPLY = new IfsaMessageProtocolEnum("RR");
   public static final IfsaMessageProtocolEnum FIRE_AND_FORGET = new IfsaMessageProtocolEnum("FF");
   public static final String version="$Id: IfsaMessageProtocolEnum.java,v 1.1 2007-10-16 08:15:43 europe\L190409 Exp $";
/**
 * MessageProtocolEnum constructor 
 * @param arg1 Value of new enumeration item
 */
protected IfsaMessageProtocolEnum(String arg1) {
	super(arg1);
}
   public static IfsaMessageProtocolEnum getEnum(String messageProtocol) {
     return (IfsaMessageProtocolEnum) getEnum(IfsaMessageProtocolEnum.class, messageProtocol);
   }
   public static List getEnumList() {
     return  getEnumList(IfsaMessageProtocolEnum.class);
   }
   public static Map getEnumMap() {
     return  getEnumMap(IfsaMessageProtocolEnum.class);
   }
   public static String getNames() {
	   String result="[";
	   for (Iterator i = iterator(IfsaMessageProtocolEnum.class); i.hasNext(); ) {
            IfsaMessageProtocolEnum c = (IfsaMessageProtocolEnum) i.next();
            result+=c.getName();
            if (i.hasNext()) result+=",";
       }
	   result+="]";
	   return result;

   }
   public static Iterator iterator() {
     return  iterator(IfsaMessageProtocolEnum.class);
   }
}
