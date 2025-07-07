/*
   Copyright 2019, 2021-2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.frankframework.receivers.MessageWrapper;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SerializationTester;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.XmlUtils;
import org.frankframework.xml.XmlWriter;

public class MessageTest {
	private static final boolean TEST_CDATA = true;
	private static final String CDATA_START = TEST_CDATA ? "<![CDATA[" : "";
	private static final String CDATA_END = TEST_CDATA ? "]]>" : "";
	public static String testString = "<root><sub>abc&amp;&lt;&gt;</sub><sub>" + CDATA_START + "<a>a&amp;b</a>" + CDATA_END + "</sub><data attr=\"één €\">één €</data></root>";
	public static String testStringFile = "/Message/testString.txt";
	private static final String characterWire76 = "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300034c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000e777261707065645265717565737471007e00027870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7078";
	private static final String binaryWire76 = "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300034c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000e777261707065645265717565737471007e000278707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7078";
	private static final String characterWire77 = "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e767200106a6176612e6c616e672e537472696e67a0f0a4387a3bb34202000078707078";
	private static final String binaryWire77 = "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7671007e00077078";
	private static final String[][] characterWires = {
			{"7.6", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300034c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000e777261707065645265717565737471007e00027870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7078"},
			{"7.7", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e767200106a6176612e6c616e672e537472696e67a0f0a4387a3bb34202000078707078"},
			// between 2021-12-07 and 2022-04-06 the serialVersionUID of Message was removed, causing unsolvable deserialization problems.
			//{ "7.7 2021-12-07 2022-04-06", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d657373616765979c61c930446c0e0300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e767200106a6176612e6c616e672e537472696e67a0f0a4387a3bb34202000078707078" },
			{"7.7 2021-06-04", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b4c000e777261707065645265717565737471007e00027870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e707078"},
			{"7.7 2021-02-02", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300024c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e78"},
			{"7.8 2021-04-20", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7400106a6176612e6c616e672e537472696e6778"},
			{"7.9 2023-12-21", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002e4c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e740006537472696e6778"},
			{"7.9.1 2024-02-23", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002e4c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e740006537472696e677372002c6e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d657373616765436f6e7465787400000000000000010300014c00046461746174000f4c6a6176612f7574696c2f4d61703b787077080000000000000001737200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c7708000000100000000374000c544553542d4b45592d494e54737200116a6176612e6c616e672e496e746567657212e2a0a4f781873802000149000576616c7565787200106a6176612e6c616e672e4e756d62657286ac951d0b94e08b02000078700000000174000f544553542d4b45592d535452494e4774000a544553542d56414c554574000d4d657461646174612e53697a657372000e6a6176612e6c616e672e4c6f6e673b8be490cc8f23df0200014a000576616c75657871007e000f0000000000000074787878"},
			{"8.0 2023-12-21", "aced0005737200216f72672e6672616e6b6672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002a4c6f72672f6672616e6b6672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e740006537472696e6778"},
			{"8.0.1 2024-02-26", "aced0005737200216f72672e6672616e6b6672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002a4c6f72672f6672616e6b6672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b7870707400743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e740006537472696e67737200286f72672e6672616e6b6672616d65776f726b2e73747265616d2e4d657373616765436f6e7465787400000000000000010300014c00046461746174000f4c6a6176612f7574696c2f4d61703b787077080000000000000001737200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c7708000000100000000374000c544553542d4b45592d494e54737200116a6176612e6c616e672e496e746567657212e2a0a4f781873802000149000576616c7565787200106a6176612e6c616e672e4e756d62657286ac951d0b94e08b02000078700000000174000f544553542d4b45592d535452494e4774000a544553542d56414c554574000d4d657461646174612e53697a657372000e6a6176612e6c616e672e4c6f6e673b8be490cc8f23df0200014a000576616c75657871007e000f0000000000000074787878"},
	};
	private static final String[][] binaryWires = {
			{"7.6", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300034c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000e777261707065645265717565737471007e000278707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7078"},
			{"7.7", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7671007e00077078"},
			// between 2021-12-07 and 2022-04-06 the serialVersionUID of Message was removed, causing unsolvable deserialization problems.
//			{ "7.7 2021-12-07 2022-04-06", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d657373616765979c61c930446c0e0300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400114c6a6176612f6c616e672f436c6173733b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e7671007e00077078" },
			{"7.7 2021-06-04", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300044c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b4c000e777261707065645265717565737471007e000278707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e707078"},
			{"7.7 2021-02-02", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300024c0007636861727365747400124c6a6176612f6c616e672f537472696e673b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e78"},
			{"7.8 2021-04-20", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474000f4c6a6176612f7574696c2f4d61703b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e740006627974655b5d78"},
			{"7.9 2023-12-21", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002e4c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e740006627974655b5d78"},
			{"7.9.1 2024-02-23", "aced0005737200256e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002e4c6e6c2f6e6e2f616461707465726672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e740006627974655b5d7372002c6e6c2e6e6e2e616461707465726672616d65776f726b2e73747265616d2e4d657373616765436f6e7465787400000000000000010300014c00046461746174000f4c6a6176612f7574696c2f4d61703b787077080000000000000001737200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c7708000000100000000374000c544553542d4b45592d494e54737200116a6176612e6c616e672e496e746567657212e2a0a4f781873802000149000576616c7565787200106a6176612e6c616e672e4e756d62657286ac951d0b94e08b02000078700000000174000f544553542d4b45592d535452494e4774000a544553542d56414c55457400104d657461646174612e4368617273657471007e0006787878"},
			{"8.0 2023-12-21", "aced0005737200216f72672e6672616e6b6672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002a4c6f72672f6672616e6b6672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e740006627974655b5d78"},
			{"8.0.1 2024-02-26", "aced0005737200216f72672e6672616e6b6672616d65776f726b2e73747265616d2e4d65737361676506139a66311e9c450300055a00186661696c6564546f44657465726d696e65436861727365744c0007636f6e7465787474002a4c6f72672f6672616e6b6672616d65776f726b2f73747265616d2f4d657373616765436f6e746578743b4c0007726571756573747400124c6a6176612f6c616e672f4f626a6563743b4c000c72657175657374436c6173737400124c6a6176612f6c616e672f537472696e673b4c00107265736f7572636573546f436c6f736574000f4c6a6176612f7574696c2f5365743b78707400055554462d38757200025b42acf317f8060854e00200007870000000743c726f6f743e3c7375623e61626326616d703b266c743b2667743b3c2f7375623e3c7375623e3c215b43444154415b3c613e6126616d703b623c2f613e5d5d3e3c2f7375623e3c6461746120617474723d22c3a9c3a96e20e282ac223ec3a9c3a96e20e282ac3c2f646174613e3c2f726f6f743e740006627974655b5d737200286f72672e6672616e6b6672616d65776f726b2e73747265616d2e4d657373616765436f6e7465787400000000000000010300014c00046461746174000f4c6a6176612f7574696c2f4d61703b787077080000000000000001737200116a6176612e7574696c2e486173684d61700507dac1c31660d103000246000a6c6f6164466163746f724900097468726573686f6c6478703f4000000000000c7708000000100000000374000c544553542d4b45592d494e54737200116a6176612e6c616e672e496e746567657212e2a0a4f781873802000149000576616c7565787200106a6176612e6c616e672e4e756d62657286ac951d0b94e08b02000078700000000174000f544553542d4b45592d535452494e4774000a544553542d56414c55457400104d657461646174612e4368617273657471007e0006787878"},
	};
	private final SerializationTester<Message> serializationTester = new SerializationTester<>();
	protected Logger log = LogUtil.getLogger(this);
	private Message adapter;

	public static void writeContentsToFile(File file, String contents) throws IOException {
		try (Writer fw = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
			fw.write(contents);
		}
	}

	@AfterEach
	public void tearDown() {
		CloseUtils.closeSilently(adapter);
	}

	protected void testAsInputStream(Message message) throws IOException {
		String header = message.peek(6);
		assertEquals("<root>", header);

		InputStream result = message.asInputStream();
		String actual = StreamUtil.streamToString(result, null, StandardCharsets.UTF_8.name());
		MatchUtils.assertXmlEquals(testString, actual);
	}

	protected void testAsReader(Message message) throws IOException {
		String header = message.peek(6);
		assertEquals("<root>", header);

		Reader result = message.asReader();
		String actual = StreamUtil.readerToString(result, null);
		MatchUtils.assertXmlEquals(testString, actual);
	}

	protected void testAsInputSource(Message adapter) throws IOException, SAXException {
		InputSource result = adapter.asInputSource();
		XmlWriter sink = new XmlWriter();
		XmlUtils.parseXml(result, sink);

		String actual = sink.toString();
		MatchUtils.assertXmlEquals(testString, actual);
	}

	protected void testAsString(Message message) throws IOException {
		String header = message.peek(6);
		assertEquals("<root>", header);

		String actual = message.asString();
		MatchUtils.assertXmlEquals(testString, actual);
	}

	protected void testAsByteArray(Message message) throws IOException {
		String header = message.peek(6);
		assertEquals("<root>", header);

		byte[] actual = message.asByteArray();
		byte[] expected = testString.getBytes(StandardCharsets.UTF_8);
		assertEquals(expected.length, actual.length, "lengths differ");
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], actual[i], "byte arrays differ at position [" + i + "]");
		}
	}

	protected void testToString(Message adapter, Class<?> clazz) {
		testToString(adapter, clazz, null);
	}

	protected void testToString(Message adapter, Class<?> clazz, Class<?> wrapperClass) {
		String actual = adapter.toString();
		// remove the toStringPrefix(), if it is present
		String valuePart = actual.contains("value:\n") ? actual.split("value:\n")[1] : actual;
		valuePart = valuePart.replaceAll(".*Message\\[[a-fA-F0-9]+:", ""); //Strip 'Message[abcd1234:'
		assertEquals(clazz.getSimpleName(), valuePart.substring(0, valuePart.indexOf("]: ")));
		if (wrapperClass == null) {
			assertEquals(clazz.getSimpleName(), adapter.getRequestClass());
		} else {
			assertEquals(wrapperClass.getSimpleName(), adapter.getRequestClass());
		}
	}

	@Test
	public void testInputStreamAsInputStream() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
		adapter = new Message(source);
		testAsInputStream(adapter);
		source.close();
	}

	@Test
	public void testInputStreamAsReader() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
		adapter = new Message(source);
		testAsReader(adapter);
		source.close();
	}

	@Test
	public void testInputStreamWithCharsetAsReader() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
		adapter = new Message(source, StandardCharsets.UTF_8.name());
		testAsReader(adapter);
		source.close();
	}

	@Test
	public void testInputStreamAsInputSource() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
		adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testInputStreamAsByteArray() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
		adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testInputStreamAsString() throws Exception {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
		adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testInputStreamToString() {
		ByteArrayInputStream source = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
		adapter = new Message(source);
		testToString(adapter, ByteArrayInputStream.class);
	}

	@Test
	public void testInputStreamAsReaderMarkAndReset() throws Exception {
		// Arrange
		byte[] bytes = testString.getBytes(StandardCharsets.UTF_8);
		ByteArrayInputStream source = new ByteArrayInputStream(bytes);
		adapter = new Message(source);

		StringWriter target = new StringWriter();

		// Act
		Reader r =  adapter.asReader();
		r.mark(bytes.length);

		r.transferTo(target); // Cannot use StreamUtil#readerToString() as it closes the reader, which is not what we need for the test
		String actual = target.toString();

		r.reset();

		// Assert
		assertEquals(testString, actual);
		assertEquals(testString, adapter.asString());
	}

	@Test
	public void testReaderAsInputStreamMarkAndReset() throws Exception {
		// Arrange
		StringReader source = new StringReader(testString);
		adapter = new Message(source);

		// Act
		InputStream is =  adapter.asInputStream();
		is.mark(testString.length());

		String actual = StreamUtils.copyToString(is, StandardCharsets.UTF_8);

		is.reset();

		// Assert
		assertEquals(testString, actual);
		assertEquals(testString, adapter.asString());
	}

	@Test
	public void testReaderAsInputStream() throws Exception {
		StringReader source = new StringReader(testString);
		adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testReaderAsReader() throws Exception {
		StringReader source = new StringReader(testString);
		adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testReaderAsInputSource() throws Exception {
		StringReader source = new StringReader(testString);
		adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testReaderAsByteArray() throws Exception {
		StringReader source = new StringReader(testString);
		adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testReaderAsString() throws Exception {
		StringReader source = new StringReader(testString);
		adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testReaderToString() {
		StringReader source = new StringReader(testString);
		adapter = new Message(source);
		testToString(adapter, StringReader.class);
	}

	@Test
	public void testStringAsInputStream() throws Exception {
		String source = testString;
		adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testStringAsReader() throws Exception {
		String source = testString;
		adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testStringAsInputSource() throws Exception {
		String source = testString;
		adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testStringAsByteArray() throws Exception {
		String source = testString;
		adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testStringAsString() throws Exception {
		String source = testString;
		adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testEmptyStringAsString() throws Exception {
		String source = "";
		Message message = new Message(source);

		String header = message.peek(6);
		assertEquals("", header);

		String actual = message.asString();
		assertEquals("", actual);
		message.close();
	}

	@Test
	public void testStringToString() {
		String source = testString;
		adapter = new Message(source);
		testToString(adapter, String.class);
	}

	@Test
	public void testByteArrayAsInputStream() throws Exception {
		byte[] source = testString.getBytes(StandardCharsets.UTF_8);
		adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testByteArrayAsReader() throws Exception {
		byte[] source = testString.getBytes(StandardCharsets.UTF_8);
		adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testByteArrayAsInputSource() throws Exception {
		byte[] source = testString.getBytes(StandardCharsets.UTF_8);
		adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testByteArrayAsByteArray() throws Exception {
		byte[] source = testString.getBytes(StandardCharsets.UTF_8);
		adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testByteArrayAsString() throws Exception {
		byte[] source = testString.getBytes(StandardCharsets.UTF_8);
		adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testByteArrayToString() {
		byte[] source = testString.getBytes(StandardCharsets.UTF_8);
		adapter = new Message(source);
		testToString(adapter, byte[].class);
	}

	@Test
	public void testURLAsInputStream() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		adapter = new UrlMessage(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testUnknownURL() throws Exception {
		String unknownFile = "xxx.bestaat.niet.txt";
		URL source = new URL("file://" + unknownFile);
		adapter = new UrlMessage(source);
		Exception exception = assertThrows(Exception.class, adapter::asInputStream);
		assertThat(exception.getMessage(), containsString(unknownFile));
	}

	@Test
	public void testURLAsReader() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		adapter = new UrlMessage(source);
		testAsReader(adapter);
	}

	@Test
	public void testURLAsInputSource() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		adapter = new UrlMessage(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testURLAsByteArray() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		adapter = new UrlMessage(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testURLAsString() throws Exception {
		URL source = this.getClass().getResource(testStringFile);
		adapter = new UrlMessage(source);
		testAsString(adapter);
	}

	@Test
	public void testURLToString() {
		URL source = this.getClass().getResource(testStringFile);
		adapter = new UrlMessage(source);
		testToString(adapter, URL.class);
	}

	@Test
	public void testFileAsInputStream() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		adapter = new FileMessage(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testUnknownFile() {
		String unknownFilename = new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath() + "-bestaatniet";
		File source = new File(unknownFilename);
		adapter = new FileMessage(source);
		Exception exception = assertThrows(NoSuchFileException.class, () -> adapter.asInputStream());
		assertThat(exception.getMessage(), containsString(unknownFilename));
	}

	@Test
	public void testFileAsReader() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		adapter = new FileMessage(source);
		testAsReader(adapter);
	}

	@Test
	public void testFileAsInputSource() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		adapter = new FileMessage(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testFileAsByteArray() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		adapter = new FileMessage(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testFileAsString() throws Exception {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		adapter = new FileMessage(source);
		testAsString(adapter);
	}

	@Test
	public void testFileToString() {
		File source = new File(this.getClass().getResource(testStringFile).getPath());
		adapter = new FileMessage(source);
		testToString(adapter, File.class);
	}

	@Test
	public void testPathAsInputStream() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		adapter = new PathMessage(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testUnknownPath() {
		String unkownfilename = new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath() + "-bestaatniet";
		Path source = Paths.get(unkownfilename);
		adapter = new PathMessage(source);
		Exception exception = assertThrows(NoSuchFileException.class, () -> adapter.asInputStream());
		assertThat(exception.getMessage(), containsString(unkownfilename));
	}

	@Test
	public void testPathAsReader() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		adapter = new PathMessage(source);
		testAsReader(adapter);
	}

	@Test
	public void testPathAsInputSource() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		adapter = new PathMessage(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testPathAsByteArray() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		adapter = new PathMessage(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testPathAsString() throws Exception {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		adapter = new PathMessage(source);
		testAsString(adapter);
	}

	@Test
	public void testPathToString() {
		Path source = Paths.get(new File(this.getClass().getResource(testStringFile).getPath()).getAbsolutePath());
		adapter = new PathMessage(source);
		testToString(adapter, source.getClass());
	}

	@Test
	public void testDocumentAsInputStream() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testDocumentAsReader() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testDocumentAsInputSource() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testDocumentAsByteArray() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testDocumentAsString() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testDocumentToString() throws Exception {
		Document source = XmlUtils.buildDomDocument(new StringReader(testString));
		adapter = new Message(source);
		testToString(adapter, source.getClass());
	}

	@Test
	public void testNodeAsInputStream() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		adapter = new Message(source);
		testAsInputStream(adapter);
	}

	@Test
	public void testFileInputStreamAsInputStream() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/Message/testString.txt");
		assertNotNull(url, "cannot find testfile");

		File file = new File(url.toURI());
		FileInputStream fis = new FileInputStream(file);
		try (Message message = new Message(fis)) {
			testAsInputStream(message);
		}
	}

	@Test
	public void testNodeAsReader() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		adapter = new Message(source);
		testAsReader(adapter);
	}

	@Test
	public void testNodeAsInputSource() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		adapter = new Message(source);
		testAsInputSource(adapter);
	}

	@Test
	public void testNodeAsByteArray() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		adapter = new Message(source);
		testAsByteArray(adapter);
	}

	@Test
	public void testNodeAsString() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		adapter = new Message(source);
		testAsString(adapter);
	}

	@Test
	public void testNodeToString() throws Exception {
		Node source = XmlUtils.buildDomDocument(new StringReader(testString)).getFirstChild();
		adapter = new Message(source);
		testToString(adapter, source.getClass());
	}

	@Test
	public void testSerializeWithString() throws Exception {
		// NB: This test logs a serialization-wire that can be added to the array `characterWires` when there are change to the class structure, to protect a version against breakage by future changes.
		String source = testString;
		try (Message in = new Message(source)) {
			in.getContext().put("TEST-KEY-STRING", "TEST-VALUE");
			in.getContext().put("TEST-KEY-INT", 1);
			byte[] wire = serializationTester.serialize(in);
			log.debug("Current characterWire: [{}]", () -> Hex.encodeHexString(wire));

			assertNotNull(wire);
			try (Message out = serializationTester.deserialize(wire)) {
				assertFalse(out.isBinary());
				assertEquals(testString, out.asString());
				assertTrue(out.getContext().containsKey("TEST-KEY-STRING"));
				assertEquals("TEST-VALUE", out.getContext().get("TEST-KEY-STRING"));
				assertTrue(out.getContext().containsKey("TEST-KEY-INT"));
				assertEquals(1, out.getContext().get("TEST-KEY-INT"));
			}
		}
	}

	@Test
	public void testSerializeWithNumber() throws Exception {
		// NB: This test logs a serialization-wire that can be added to the array `characterWires` when there are change to the class structure, to protect a version against breakage by future changes.
		Long source = 12345L;
		try (Message in = Message.asMessage(source)) {
			in.getContext().put("TEST-KEY-STRING", "TEST-VALUE");
			in.getContext().put("TEST-KEY-INT", 1);
			byte[] wire = serializationTester.serialize(in);
			log.debug("Current characterWire: [{}]", () -> Hex.encodeHexString(wire));

			assertNotNull(wire);
			try (Message out = serializationTester.deserialize(wire)) {
				assertTrue(out.isBinary());
				assertEquals("12345", out.asString());
				assertEquals(5L, out.size()); // For Number, derived from length of "asString"
				assertTrue(out.getContext().containsKey("TEST-KEY-STRING"));
				assertEquals("TEST-VALUE", out.getContext().get("TEST-KEY-STRING"));
				assertTrue(out.getContext().containsKey("TEST-KEY-INT"));
				assertEquals(1, out.getContext().get("TEST-KEY-INT"));
			}
		}
	}

	@Test
	public void testSerializeWithBoolean() throws Exception {
		// NB: This test logs a serialization-wire that can be added to the array `characterWires` when there are change to the class structure, to protect a version against breakage by future changes.
		Boolean source = Boolean.FALSE;
		try (Message in = Message.asMessage(source)) {
			in.getContext().put("TEST-KEY-STRING", "TEST-VALUE");
			in.getContext().put("TEST-KEY-INT", 1);
			byte[] wire = serializationTester.serialize(in);
			log.debug("Current characterWire: [{}]", () -> Hex.encodeHexString(wire));

			assertNotNull(wire);
			try (Message out = serializationTester.deserialize(wire)) {
				assertTrue(out.isBinary());
				assertEquals("false", out.asString());
				assertEquals(5L, out.size()); // For Boolean, derived from the length of "asString"
				assertTrue(out.getContext().containsKey("TEST-KEY-STRING"));
				assertEquals("TEST-VALUE", out.getContext().get("TEST-KEY-STRING"));
				assertTrue(out.getContext().containsKey("TEST-KEY-INT"));
				assertEquals(1, out.getContext().get("TEST-KEY-INT"));
			}
		}
	}

	@Test
	public void testSerializeWithByteArray() throws Exception {
		// NB: This test logs a serialization-wire that can be added to the array `binaryWires` when there are change to the class structure, to protect a version against breakage by future changes.
		byte[] source = testString.getBytes(StandardCharsets.UTF_8);
		try (Message in = new Message(source)) {
			in.getContext().withCharset(StandardCharsets.UTF_8);
			in.getContext().put("TEST-KEY-STRING", "TEST-VALUE");
			in.getContext().put("TEST-KEY-INT", 1);

			byte[] wire = serializationTester.serialize(in);
			log.debug("Current binaryWire: [{}]", () -> Hex.encodeHexString(wire));

			assertNotNull(wire);
			try (Message out = serializationTester.deserialize(wire)) {

				assertTrue(out.isBinary());
				assertEquals(testString, out.asString());
				assertTrue(out.getContext().containsKey("TEST-KEY-STRING"));
				assertEquals("TEST-VALUE", out.getContext().get("TEST-KEY-STRING"));
				assertTrue(out.getContext().containsKey("TEST-KEY-INT"));
				assertEquals(1, out.getContext().get("TEST-KEY-INT"));
				assertEquals("UTF-8", out.getContext().get(MessageContext.METADATA_CHARSET));
			}
		}
	}

	@Test
	public void testSerializeWithReader() throws Exception {
		Reader source = new StringReader(testString);
		try (Message in = new Message(source)) {
			byte[] wire = serializationTester.serialize(in);

			assertNotNull(wire);
			Message out = serializationTester.deserialize(wire);

			assertFalse(out.isBinary());
			assertEquals(testString, out.asString());
			out.close();
		}
		source.close();
	}

	@Test
	public void testSerializeWithInputStream() throws Exception {
		InputStream source = new ByteArrayInputStream(testString.getBytes(StandardCharsets.UTF_8));
		try (Message in = new Message(source)) {
			byte[] wire = serializationTester.serialize(in);

			assertNotNull(wire);
			Message out = serializationTester.deserialize(wire);

			assertTrue(out.isBinary());
			assertEquals(testString, out.asString());
			out.close();
		}
	}

	@Test
	public void testSerializeWithFile(@TempDir Path folder) throws Exception {
		File source = folder.resolve("testSerializeWithFile").toFile();
		writeContentsToFile(source, testString);

		Message in = new FileMessage(source);
		byte[] wire = serializationTester.serialize(in);
		writeContentsToFile(source, "fakeContentAsReplacementOfThePrevious");
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals(testString, out.asString());
		out.close();
		in.close();
	}

	@Test
	public void testSerializeWithURL(@TempDir Path folder) throws Exception {
		File file = folder.resolve("testSerializeWithURL").toFile();
		writeContentsToFile(file, testString);
		URL source = file.toURI().toURL();

		Message in = new UrlMessage(source);
		byte[] wire = serializationTester.serialize(in);
		writeContentsToFile(file, "fakeContentAsReplacementOfThePrevious");
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals(testString, out.asString());
		out.close();
		in.close();
	}

	@Test
	public void testDeserialization76CompatibilityWithString() throws Exception {
//		String source = testString;
//		Message in = new Message(source);
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Character: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(characterWire76);
		Message out = serializationTester.deserialize(wire);

		assertFalse(out.isBinary());
		assertEquals(testString, out.asString());
		out.close();
	}

	@Test
	public void testDeserialization76CompatibilityWithByteArray() throws Exception {
//		byte[] source = testString.getBytes(StandardCharsets.UTF_8);
//		Message in = new Message(source, StandardCharsets.UTF_8);
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Bytes: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(binaryWire76);
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals(StandardCharsets.UTF_8.name(), out.getCharset());
		assertEquals(testString, out.asString());
		out.close();
	}

	@Test
	public void testDeserialization77CompatibilityWithString() throws Exception {
//		String source = testString;
//		Message in = new Message(source);
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Character: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(characterWire77);
		Message out = serializationTester.deserialize(wire);

		assertFalse(out.isBinary());
		assertEquals(testString, out.asString());
		out.close();
	}

	@Test
	public void testDeserialization77CompatibilityWithByteArray() throws Exception {
//		byte[] source = testString.getBytes(StandardCharsets.UTF_8);
//		Message in = new Message(source, StandardCharsets.UTF_8);
//		byte[] wire = serializationTester.serialize(in);
//		System.out.println("Bytes: "+Hex.encodeHexString(wire));

		byte[] wire = Hex.decodeHex(binaryWire77);
		Message out = serializationTester.deserialize(wire);

		assertTrue(out.isBinary());
		assertEquals(StandardCharsets.UTF_8.name(), out.getCharset());
		assertEquals(testString, out.asString());
		out.close();
	}

	@Test
	public void testDeserializationCompatibilityWithString() throws Exception {

		for (String[] characterWire : characterWires) {
			String label = characterWire[0];
			log.debug("testDeserializationCompatibilityWithString() {}", label);
			byte[] wire = Hex.decodeHex(characterWire[1]);
			Message out = serializationTester.deserialize(wire);

			assertFalse(out.isBinary(), label);
			assertEquals(testString, out.asString(), label);
			out.close();
		}
	}

	@Test
	public void testDeserializationCompatibilityWithByteArray() throws Exception {

		for (String[] binaryWire : binaryWires) {
			String label = binaryWire[0];
			log.debug("testDeserializationCompatibilityWithByteArray() {}", label);
			byte[] wire = Hex.decodeHex(binaryWire[1]);
			Message out = serializationTester.deserialize(wire);

			assertTrue(out.isBinary(), label);
			assertEquals(StandardCharsets.UTF_8.name(), out.getCharset(), label);
			assertEquals(testString, out.asString(), label);
			out.close();
		}
	}


	@Test
	public void testMessageSizeString() {
		Message message = new Message("string");
		assertEquals(6, message.size(), "size differs or could not be determined");
		message.close();
	}

	@Test
	public void testMessageSizeByteArray() {
		Message message = new Message("string".getBytes());
		assertEquals(6, message.size(), "size differs or could not be determined");
		message.close();
	}

	@Test
	public void testMessageSizeFileInputStream() throws Exception {
		URL url = this.getClass().getResource("/file.xml");
		assertNotNull(url, "cannot find testfile");

		File file = new File(url.toURI());
		try (FileInputStream fis = new FileInputStream(file);
			 Message message = new Message(fis)) {
			assertEquals(33, message.size(), "size differs or could not be determined");
		}
	}

	@Test
	public void testMessageSizeFile() throws Exception {
		URL url = this.getClass().getResource("/file.xml");
		assertNotNull(url, "cannot find testfile");

		File file = new File(url.toURI());
		try (Message message = new FileMessage(file)) {
			assertEquals(33, message.size(), "size differs or could not be determined");
		}
	}

	@Test
	public void testMessageSizeURL() {
		URL url = this.getClass().getResource("/file.xml");
		assertNotNull(url, "cannot find testfile");

		try (Message message = new UrlMessage(url)) {
			assertEquals(-1, message.size(), "size differs or could not be determined");
		}
	}

	@Test
	public void testNullMessageSize() {
		try (Message message = Message.nullMessage()) {
			assertEquals(0, message.size());
		}
	}

	@Test
	public void testMessageSizeExternalURL() throws Exception {
		URL url = new URL("http://www.file.xml");
		assertNotNull(url, "cannot find testfile");

		try (Message message = new UrlMessage(url)) {
			assertEquals(-1L, message.size());
		}
	}

	@Test
	public void testMessageSizeReader() {
		try (Message message = new Message(new StringReader("string"))) {
			assertEquals(-1L, message.size(), "size differs or could not be determined");
			assertDoesNotThrow(() -> message.asString());
			assertEquals(6L, message.size(), "size differs or could not be determined");
		}
	}

	@Test
	public void testMessageIsEmpty() {
		try (Message message = Message.nullMessage()) {
			assertTrue(message.isEmpty());
			assertTrue(Message.isEmpty(message));
		}
	}

	@Test
	public void testNullMessageIsEmpty() {
		assertTrue(Message.isEmpty(null));
	}

	@Test
	public void testMessageDefaultCharset() throws Exception {
		String utf8Input = "Më-×m👌‰Œœ‡TzdDEyMt120=";
		ByteArrayInputStream source = new ByteArrayInputStream(utf8Input.getBytes(StandardCharsets.UTF_8));
		Message binaryMessage = new Message(source); //non-repeatable stream, no provided charset

		assertEquals(utf8Input, binaryMessage.asString()); //Default must be used

		try (Message characterMessage = new Message(utf8Input)) {
			assertEquals(utf8Input, characterMessage.asString("ISO-8859-1")); //This should not be used as there is no binary conversion
		}
		binaryMessage.close();
	}

	@Test
	public void testMessageDetectCharset() throws Exception {
		String utf8Input = "Më-×m👌‰Œœ‡TzdDEyMt120=";
		ByteArrayInputStream source = new ByteArrayInputStream(utf8Input.getBytes(StandardCharsets.UTF_8));
		Message message = new Message(source, "auto"); //Set the MessageContext charset

		String stringResult = message.asString("ISO-8859-ik-besta-niet"); //use MessageContext charset
		assertEquals(utf8Input, stringResult);
		message.close();
	}

	@Test
	public void testMessageDetectCharsetISO8859() throws Exception {
		URL isoInputFile = TestFileUtils.getTestFileURL("/Util/MessageUtils/iso-8859-1.txt");
		assertNotNull(isoInputFile, "unable to find isoInputFile");

		try (Message message = new UrlMessage(isoInputFile)) { //repeatable stream, detect charset
			String stringResult = message.asString("auto"); //detect when reading
			assertEquals(StreamUtil.streamToString(isoInputFile.openStream(), "ISO-8859-1"), stringResult);
		}
	}

	@Test
	public void testCharsetDeterminationAndFallbackToDefault() throws Exception {
		Message messageNullCharset = new Message((byte[]) null) { //NullMessage, charset cannot be determined
			@Override
			public String getCharset() {
				return null;
			}
		};
		Message messageAutoCharset = new Message((byte[]) null) { //NullMessage, charset cannot be determined
			@Override
			public String getCharset() {
				return "AUTO";
			}
		};

		// getCharset()==null && defaultDecodingCharset==AUTO ==> decodingCharset = UTF-8
		assertEquals(StandardCharsets.UTF_8.name(), messageNullCharset.computeDecodingCharset("AUTO"));

		// getCharset()==AUTO && defaultDecodingCharset==xyz ==> decodingCharset = xyz
		assertEquals("ISO-8559-15", messageAutoCharset.computeDecodingCharset("ISO-8559-15"));

		// getCharset()==AUTO && defaultDecodingCharset==AUTO ==> decodingCharset = UTF-8
		assertEquals(StandardCharsets.UTF_8.name(), messageAutoCharset.computeDecodingCharset("AUTO"));

		// getCharset()==AUTO && defaultDecodingCharset==null ==> decodingCharset = UTF-8
		assertEquals(StandardCharsets.UTF_8.name(), messageAutoCharset.computeDecodingCharset(null));
		messageNullCharset.close();
		messageAutoCharset.close();
	}

	@Test
	public void shouldOnlyDetectCharsetOnce() throws Exception {
		Message message = new Message("’•†™".getBytes("cp-1252")) { //NullMessage, charset cannot be determined
			@Override
			public String getCharset() {
				return "AUTO";
			}

		};

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			message.asString("auto"); //calls asReader();
			message.asString(); //calls asReader();
			message.asString("auto"); //calls asReader();
			message.asString(); //calls asReader();
			message.asString("auto"); //calls asReader();
			message.asString(); //calls asReader();
			message.asString("auto"); //calls asReader();
			message.asString(); //calls asReader();
			message.close();
			int i = 0;
			for (String logLine : appender.getLogLines()) {
				if (logLine.contains("unable to detect charset for message")) {
					i++;
				}
			}
			assertEquals(1, i, "charset should be determined only once");
		}
		message.close();
	}

	@Test
	public void testCopyMessage1() throws IOException {
		// Arrange
		Message msg1 = new Message("a");

		// Act
		Message msg2 = msg1.copyMessage();

		msg1.close();

		// Assert
		assertTrue(msg1.isNull());
		assertFalse(msg2.isNull());
		assertEquals("a", msg2.asString());
		msg2.close();
	}

	@Test
	public void testCopyMessage2() throws IOException {
		// Arrange
		Message msg1 = new Message(new StringReader("á"));

		// Act
		Message msg2 = msg1.copyMessage();

		msg1.close();

		// Assert
		assertTrue(msg1.isNull());
		assertFalse(msg2.isNull());
		assertEquals("á", msg2.asString());
		msg2.close();
	}

	@Test
	public void testCopyMessage3() throws IOException {
		// Arrange
		Message msg1 = new Message(new ByteArrayInputStream("á".getBytes()));

		// Act
		Message msg2 = msg1.copyMessage();

		msg1.close();

		// Assert
		assertTrue(msg1.isNull());
		assertFalse(msg2.isNull());
		assertEquals("á", msg2.asString());
		msg2.close();
	}

	@Test
	void testMessageAsStringDoesNotCloseMessage() throws IOException {
		// Arrange
		String text = "text";
		Message msg = new Message(new StringReader(text));

		// Act
		String content = msg.asString();

		// Assert
		assertEquals(text, content);
		msg.close();
	}

	@Test
	void testMessageAsStringDoesNotCloseMessageWrapper() throws IOException {
		// Arrange: make it an object, so method can do instanceof check
		String text = "text";
		Message msg = new Message(new StringReader(text));
		MessageWrapper<Message> wrapper = new MessageWrapper<>(msg, null, null);

		// Act
		String content = wrapper.getMessage().asString();

		// Assert
		assertEquals(text, content);
		msg.close();
	}

	@Test
	void testMessageAsByteArrayDoesNotCloseMessage() throws IOException {
		// Arrange: make it an object, so method can do instanceof check
		Message msg = new Message(new StringReader("text"));

		// Act
		byte[] content = msg.asByteArray();

		// Assert
		Message message = (Message) msg;
		assertEquals("text", message.asString());
		assertEquals(4, content.length);
		message.close();
	}

	@Test
	void testMessageAsByteArrayDoesNotCloseMessageWrapper() throws IOException {
		// Arrange: make it an object, so method can do instanceof check
		Message msg = new Message(new StringReader("text"));
		MessageWrapper wrapper = new MessageWrapper<Message>(msg, null, null);

		// Act
		byte[] content = wrapper.getMessage().asByteArray();

		// Assert
		MessageWrapper<Message> messageWrapper = (MessageWrapper) wrapper;
		assertEquals("text", messageWrapper.getMessage().asString());
		assertEquals(4, content.length);
		msg.close();
	}
}
