package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.HashPipe.HashEncoding;
import org.frankframework.pipes.hash.Algorithm;

public class HashPipeTest extends PipeTestBase<HashPipe> {

	@Override
	public HashPipe createPipe() {
		return new HashPipe();
	}

	@Test
	public void noSecret() {
		ConfigurationException e = assertThrows(ConfigurationException.class, this::configureAndStartPipe);
		assertThat(e.getMessage(), Matchers.endsWith("using a secret is mandatory"));
	}

	@Test
	public void basic() throws Exception {
		pipe.setSecret("Potato");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash = prr.getResult().asString();
		assertEquals("KZAvcWh5wSTeoBWty9MHZl+L4ApUjbWnJNaVq6xftAo=", hash);
	}

	@Test
	public void md5() throws Exception {
		pipe.setSecret("Potato");
		pipe.setAlgorithm(Algorithm.HmacMD5);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash = prr.getResult().asString();
		assertEquals("TwGD5U8BwKoLn8u/F+4R/g==", hash);
	}

	@Test
	public void sha512() throws Exception {
		pipe.setSecret("Potato");
		pipe.setAlgorithm(Algorithm.HmacSHA512);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash = prr.getResult().asString();
		assertEquals("56V9GhAPU9NPP76zJ5KVLrfMaCherC8JcY16PTPEO3W+yxNnoXwmLS+Ic61J3gqZyeUfc0VZzzgg23WqesXm2g==", hash);
	}

	@Test
	public void hex() throws Exception {
		pipe.setSecret("Potato");
		pipe.setHashEncoding(HashEncoding.Hex);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash = prr.getResult().asString();
		assertEquals("29902f716879c124dea015adcbd307665f8be00a548db5a724d695abac5fb40a", hash);
	}

	@Test
	public void md5hex() throws Exception {
		pipe.setSecret("Potato");
		pipe.setHashEncoding(HashEncoding.Hex);
		pipe.setAlgorithm(Algorithm.HmacMD5);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash = prr.getResult().asString();
		assertEquals("4f0183e54f01c0aa0b9fcbbf17ee11fe", hash);
	}

	@Test
	public void sha512hex() throws Exception {
		pipe.setSecret("Potato");
		pipe.setHashEncoding(HashEncoding.Hex);
		pipe.setAlgorithm(Algorithm.HmacSHA512);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash = prr.getResult().asString();
		assertEquals("e7a57d1a100f53d34f3fbeb32792952eb7cc68285eac2f09718d7a3d33c43b75becb1367a17c262d2f8873ad49de0a99c9e51f734559cf3820db75aa7ac5e6da", hash);
	}

	@Test
	public void paramSha512hex() throws Exception {
		pipe.setBinaryToTextEncoding(HashEncoding.Hex); //also tests deprecated BinaryToTextEncoding. same output as above test
		pipe.setAlgorithm(Algorithm.HmacSHA512);
		pipe.addParameter(new Parameter("secret", "Potato"));
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "hash me plz", session);
		String hash = prr.getResult().asString();
		assertEquals("e7a57d1a100f53d34f3fbeb32792952eb7cc68285eac2f09718d7a3d33c43b75becb1367a17c262d2f8873ad49de0a99c9e51f734559cf3820db75aa7ac5e6da", hash);
	}

	@Test
	public void emptyParamSha512hex() throws Exception {
		pipe.setSecret("Aardappel");
		pipe.setHashEncoding(HashEncoding.Hex);
		pipe.setAlgorithm(Algorithm.HmacSHA512);
		pipe.addParameter(new Parameter("secret", null));
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, "Potato", session);
		String hash = prr.getResult().asString();
		assertEquals("beb37d574dce469b92b4494daf92f2065dede3c0b14b7a5d5f0390d4829b580a2867c4d2af3d937757dbcfd083811682eed108d6b0fdf6f760e150f1dfeeb8e3", hash);
	}

	@Test
	public void largeMessage() throws Exception {
		pipe.setSecret("Potato");
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, getResource("largeInput.txt"), session);
		String hash = prr.getResult().asString();
		assertEquals("M7Z60BhL72SMyCEUVesQOuvBRUcokJPyy95lSQODDZU=", hash);
	}

	@Test
	public void largeMessageHex() throws Exception {
		pipe.setSecret("Potato");
		pipe.setHashEncoding(HashEncoding.Hex);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, getResource("largeInput.txt"), session);
		String hash = prr.getResult().asString();
		assertEquals("33b67ad0184bef648cc8211455eb103aebc14547289093f2cbde654903830d95", hash);
	}
}
