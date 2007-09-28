/*
 * TestAspect.java
 * 
 * Created on 26-sep-2007, 14:48:40
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.unmanaged;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 *
 * @author m00035f
 */
public class TestAspect {
    private final static Logger log = Logger.getLogger(TestAspect.class);
    
    public Object testAspectInvocation(ProceedingJoinPoint pjp) throws Throwable {
        log.fatal("<*****>testAspectInvocation for pjp ["+pjp+"]<*****>");
        try {
            return pjp.proceed();
        } finally {
            log.fatal("<******> done invoking method; return result <******>");
        }
    }
}
