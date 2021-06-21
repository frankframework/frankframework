package nl.nn.adapterframework.monitoring;

/*
 * This derived class of Trigger is no different from Trigger itself. It is introduced
 * purely on behalf of the Frank!Doc. If Monitor.registerAlarm would take a Trigger
 * instead of an Alarm, then the Frank!Element Trigger would appear with multiple
 * element roles. That would cause a name clash. This name clash would also disappear
 * when an interface ITrigger would be introduced, but that would cause a name clash
 * between ITrigger and Trigger. The latter name clash would produce a generic element
 * option Alarm with a default className pointing to Java class Trigger. That generic
 * element option would have a anyAttribute, so the attributes would no longer
 * be checked.
 * 
 */
public class Alarm extends Trigger {
}
