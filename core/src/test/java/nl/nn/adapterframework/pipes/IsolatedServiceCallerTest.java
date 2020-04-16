package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.receivers.JavaListener;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import java.util.HashMap;

/**
 * IsolatedServiceCaller Tester.
 *
 * @author <Sina Sen>
 * @version 1.0
 * @since <pre>Apr 2, 2020</pre>
 */
//public class IsolatedServiceCallerTest{
//
//    private static JavaListener listener;
//
//    private static HashMap a;
//
//    private static TaskExecutor te;
//
//    private static IsolatedServiceCaller isoCaller;
//
//    @Mock
//    private IPipeLineSession session = new PipeLineSessionBase();
//
//    @Rule
//    public ExpectedException exception = ExpectedException.none();
//
//    @BeforeClass
//    public static void before(){
//        listener = new JavaListener();
//        a = new HashMap();
//        isoCaller = new IsolatedServiceCaller();
//        te = new SimpleAsyncTaskExecutor();
//
//        isoCaller.setTaskExecutor(te);
//
//    }
//    /**
//     * Method: setTaskExecutor(TaskExecutor executor)
//     */
//    @Test
//    public void testCallServiceIsolatedException() throws Exception {
//        exception.expect(ListenerException.class);
//        isoCaller.callServiceIsolated("asas", "asasd", "fame", a, false);
//    }
//
//    @Test
//    public void testCallServiceIsolated() throws Exception {
//
//        isoCaller.callServiceIsolated("asas", "asasd", "fame", a, true);
//    }






//}
