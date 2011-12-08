package nl.nn.adapterframework.util;

import org.apache.commons.lang.enums.Enum;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
/**
 * Enumeration of states for IManagable
 * @version Id
 * @author Gerrit van Brakel
 */
public class RunStateEnum extends Enum {
	
   public static final RunStateEnum STOPPED = new RunStateEnum("Stopped");
   public static final RunStateEnum STARTED = new RunStateEnum("Started");
   public static final RunStateEnum STOPPING = new RunStateEnum("Stopping");
   public static final RunStateEnum STARTING= new RunStateEnum("Starting");
   public static final RunStateEnum ERROR = new RunStateEnum("**ERROR**");
/**
 * RunStateEnum constructor 
 * @param stateDescriptor Value of new enumeration item
 */
protected RunStateEnum(String stateDescriptor) {
	super(stateDescriptor);
}
   public static RunStateEnum getEnum(String stateDescriptor) {
     return (RunStateEnum) getEnum(RunStateEnum.class, stateDescriptor);
   }
   public static List getEnumList() {
     return  getEnumList(RunStateEnum.class);
   }
   public static Map getEnumMap() {
     return  getEnumMap(RunStateEnum.class);
   }
   public static String getNames() {
	   String result="[";
	   for (Iterator i = iterator(RunStateEnum.class); i.hasNext(); ) {
            RunStateEnum c = (RunStateEnum) i.next();
            result+=c.getName();
            if (i.hasNext()) result+=",";
       }
	   result+="]";
	   return result;

   }
   public boolean isState(String state) {
	   return this.equals(getEnum(state.trim()));
   }
   public static Iterator iterator() {
     return  iterator(RunStateEnum.class);
   }
public String toString() {
	return getName().trim();
}
}
