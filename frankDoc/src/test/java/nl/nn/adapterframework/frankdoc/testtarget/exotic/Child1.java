package nl.nn.adapterframework.frankdoc.testtarget.exotic;

/**
 * This class should not be accessible in syntax 2. It appears as an
 * argument of {@link Member1#setChild(Child1)}, so we have
 * syntax 1 name "child". There is also {@link Member2#setChild(Child2)},
 * which introduces class {@link Child2} with syntax 1 name "child". If
 * these would both be allowed as syntax 2 elements, then the generic
 * element option of {@link Master#setPart(IMember)} would have conflicting
 * <code>&lt;Child&gt;</code> elements.
 * 
 * @author martijn
 *
 */
public class Child1 {
}
