package nl.nn.adapterframework.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.model.TestTimedOutException;

import javax.ejb.Timeout;

import static org.junit.Assert.assertEquals;

/**
 * Semaphore Tester.
 * @author <Sina Sen>
 */
public class SemaphoreTest {



    /**
     * Method: acquire()
     */
    @Test
    public void testAcquire() throws Exception {
        Semaphore s = new Semaphore(4);
        s.acquire();
    }


    /**
     * Method: acquire()
     *//*
    @Test(timeout=3000)
    public void testAcquireWait() throws Exception {
        Semaphore s = new Semaphore(0);
        s.acquire();
        }*/

    /**
     * Method: tighten()
     */
    @Test
    public void testTighten() throws Exception {
        Semaphore s = new Semaphore(0);
        s.tighten();
    }

    /**
     * Method: release()
     */
    @Test
    public void testReleaseNotify() throws Exception {
        Semaphore s = new Semaphore(0);
        s.tighten();    }

    /**
     * Method: release()
     */
    @Test
    public void testRelease() throws Exception {
        Semaphore s = new Semaphore(3);
        s.tighten();    }




} 
