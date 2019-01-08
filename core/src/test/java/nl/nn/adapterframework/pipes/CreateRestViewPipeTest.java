package nl.nn.adapterframework.pipes;

import org.junit.Test;

import static org.junit.Assert.*;

public class CreateRestViewPipeTest extends PipeTestBase<CreateRestViewPipe> {

    @Override
    public CreateRestViewPipe createPipe() {
        return new CreateRestViewPipe();
    }

    @Test
    public void getterSetterContentType() {
        String dummyString = "Hello World";
        pipe.setContentType(dummyString);
        assertEquals(pipe.getContentType(), dummyString);
    }
}