package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.io.output.NullOutputStream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeRunException;
import org.frankframework.processors.CorePipeLineProcessor;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.senders.EchoSender;
import org.frankframework.stream.Message;
import org.frankframework.testutil.LargeStructuredMockData;

@Tag("slow")
public class ReplaceJsonPipeLineTest extends PipeTestBase<ReplacerPipe> {
	@Override
	public ReplacerPipe createPipe() throws ConfigurationException {
		return new ReplacerPipe();
	}

	@Test
	public void generateLargeJsonAndUseFindAndReplace() throws ConfigurationException, PipeRunException, IOException {
		// 1. generate large json
		long actualMinSize = 250 * 1024L * 1024L; // 250 MB
		Reader reader = LargeStructuredMockData.getLargeXmlDataReader(actualMinSize);
		Message input = Message.asMessage(reader);
		input.preserve();

		// 2. use find and replace to replace a value
		ReplacerPipe replacerPipe = new ReplacerPipe();
		replacerPipe.setName("replaceItemToBarleduc");
		replacerPipe.setFind("type");
		replacerPipe.setReplace("barleduc");
		replacerPipe.addForward(new PipeForward("success", "replaceBarleducToItem"));
		pipeline.addPipe(replacerPipe);

		// 3. use find and replace to replace the value back to the original value
		pipe.setName("replaceBarleducToItem");
		pipe.setFind("barleduc");
		pipe.setReplace("type");
		pipeline.addPipe(pipe);

		// 4. assert that this doesn't take ages
		CorePipeLineProcessor processor = new CorePipeLineProcessor();
		pipeline.setFirstPipe("replaceItemToBarleduc");
		pipeline.setPipeLineProcessor(processor);
		CorePipeProcessor cpp = new CorePipeProcessor();
		processor.setPipeProcessor(cpp);

		configureAdapter();
		replacerPipe.start();
		pipe.start();

		long start = System.currentTimeMillis();
		PipeLineResult pipeLineResult = pipeline.process("", input, session);
		pipeLineResult.getResult().asInputStream().transferTo(new NullOutputStream());
		long end = System.currentTimeMillis();

		// 5. Calculate the duration
		long duration = end - start;
		System.out.println("Execution time: " + duration + " ms");
		assertTrue(duration < 60000, "Execution time exceeded 1s");
	}

	@Test
	public void generateLargeJsonAndUseFindAndReplaceWithEcho() throws ConfigurationException, PipeRunException, IOException {
		// 1. generate large json
		long actualMinSize = 250 * 1024L * 1024L; // 250 MB
		Reader reader = LargeStructuredMockData.getLargeXmlDataReader(actualMinSize);
		Message input = Message.asMessage(reader);
		input.preserve();

		// 2. use find and replace to replace a value
		ReplacerPipe replacerPipe = new ReplacerPipe();
		replacerPipe.setName("replaceTypeToBarleduc");
		replacerPipe.setFind("type");
		replacerPipe.setReplace("barleduc");
		replacerPipe.addForward(new PipeForward("success", "echoPipe"));
		pipeline.addPipe(replacerPipe);

		// 3. use an echopipe
		EchoPipe echoPipe = new EchoPipe();
		echoPipe.setName("echoPipe");
		echoPipe.addForward(new PipeForward("success", "replaceBarleducToType"));
		pipeline.addPipe(echoPipe);

		// 4. use find and replace to replace the value back to the original value
		pipe.setName("replaceBarleducToType");
		pipe.setFind("barleduc");
		pipe.setReplace("type");
		pipeline.addPipe(pipe);

		// 5. assert that this doesn't take ages
		CorePipeLineProcessor processor = new CorePipeLineProcessor();
		pipeline.setFirstPipe("replaceTypeToBarleduc");
		pipeline.setPipeLineProcessor(processor);
		CorePipeProcessor cpp = new CorePipeProcessor();
		processor.setPipeProcessor(cpp);

		configureAdapter();
		replacerPipe.start();
		echoPipe.start();
		pipe.start();

		long start = System.currentTimeMillis();
		PipeLineResult pipeLineResult = pipeline.process("", input, session);
		pipeLineResult.getResult().asInputStream().transferTo(new NullOutputStream());
		long end = System.currentTimeMillis();

		// 6. Calculate the duration
		long duration = end - start;
		System.out.println("Execution time: " + duration + " ms");
		assertTrue(duration < 60000, "Execution time exceeded 1s");
	}

	@Test
	void testReplaceWithForEachChildElementPipe() throws ConfigurationException, PipeRunException, IOException {
		// 1. generate large json
		long actualMinSize = 250 * 1024L * 1024L; // 250 MB
		Reader reader = LargeStructuredMockData.getLargeXmlDataReader(actualMinSize);
		Message input = Message.asMessage(reader);
		input.preserve();

		// 2. use find and replace to replace a value
		ReplacerPipe replacerPipe = new ReplacerPipe();
		replacerPipe.setName("replaceTypeToBarleduc");
		replacerPipe.setFind("type");
		replacerPipe.setReplace("barleduc");
		replacerPipe.addForward(new PipeForward("success", "echoPipe"));
		pipeline.addPipe(replacerPipe);

		// 3. use an echopipe
		EchoPipe echoPipe = new EchoPipe();
		echoPipe.setName("echoPipe");
		echoPipe.addForward(new PipeForward("success", "replaceBarleducToType"));
		pipeline.addPipe(echoPipe);

		// 4. use find and replace to replace the value back to the original value
		pipe.setName("replaceBarleducToType");
		pipe.setFind("barleduc");
		pipe.setReplace("type");
		pipe.addForward(new PipeForward("success", "forEachPipe"));
		pipeline.addPipe(pipe);

		// 5. add a foreach pipe
		ForEachChildElementPipe forEachPipe = new ForEachChildElementPipe();
		forEachPipe.setName("forEachPipe");
		forEachPipe.setTargetElement("type");
		forEachPipe.setParallel(true);
		forEachPipe.setMaxChildThreads(20);
		forEachPipe.setSender(new EchoSender());
		pipeline.addPipe(forEachPipe);

		// 6. assert that this doesn't take ages
		CorePipeLineProcessor processor = new CorePipeLineProcessor();
		pipeline.setFirstPipe("replaceTypeToBarleduc");
		pipeline.setPipeLineProcessor(processor);
		CorePipeProcessor cpp = new CorePipeProcessor();
		processor.setPipeProcessor(cpp);

		configureAdapter();
		replacerPipe.start();
		echoPipe.start();
		pipe.start();
		forEachPipe.start();

		long start = System.currentTimeMillis();
		PipeLineResult pipeLineResult = pipeline.process("", input, session);
		// reading the result should not be necessary when using the foreach pipe
		// pipeLineResult.getResult().asInputStream().transferTo(new NullOutputStream());
		long end = System.currentTimeMillis();

		// 7. Calculate the duration
		long duration = end - start;
		System.out.println("Execution time: " + duration + " ms");
		assertTrue(duration < 60000, "Execution time exceeded 1s");
	}
}
