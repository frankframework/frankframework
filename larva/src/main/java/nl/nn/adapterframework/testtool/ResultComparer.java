/**
 * 
 */
package nl.nn.adapterframework.testtool;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipInputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.Diff;

import nl.nn.adapterframework.util.XmlUtils;

/**
 * This class is used to compare step outputs.
 * @author Murat Kaan Meral
 *
 */
public class ResultComparer {

	public static int compareResult(String step, String stepDisplayName, String fileName, String expectedResult, String actualResult, Properties properties, String queueName, String originalFilePath) {
		int ok = TestTool.RESULT_ERROR;
		String testName = properties.getProperty("scenario.description");
		String printableExpectedResult = XmlUtils.replaceNonValidXmlCharacters(expectedResult);
		String printableActualResult = XmlUtils.replaceNonValidXmlCharacters(actualResult);
		String preparedExpectedResult = printableExpectedResult;
		String preparedActualResult = printableActualResult;
		MessageListener.debugMessage(testName, "Check decodeUnzipContentBetweenKeys properties");
		boolean decodeUnzipContentBetweenKeysProcessed = false;
		int i = 1;
		while (!decodeUnzipContentBetweenKeysProcessed) {
			String key1 = properties.getProperty("decodeUnzipContentBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("decodeUnzipContentBetweenKeys" + i + ".key2");
			boolean replaceNewlines = false;
			if ("true".equals(properties.getProperty("decodeUnzipContentBetweenKeys" + i + ".replaceNewlines"))) {
				replaceNewlines = true;
			}
			if (key1 != null && key2 != null) {
				MessageListener.debugMessage(testName, "Decode and unzip content between key1 '" + key1 + "' and key2 '" + key2 + "' (replaceNewlines is " + replaceNewlines + ")");
				preparedExpectedResult = decodeUnzipContentBetweenKeys(testName, preparedExpectedResult, key1, key2, replaceNewlines);
				preparedActualResult = decodeUnzipContentBetweenKeys(testName, preparedActualResult, key1, key2, replaceNewlines);
				i++;
			} else {
				decodeUnzipContentBetweenKeysProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check canonicaliseFilePathContentBetweenKeys properties");
		boolean canonicaliseFilePathContentBetweenKeysProcessed = false;
		i = 1;
		while (!canonicaliseFilePathContentBetweenKeysProcessed) {
			String key1 = properties.getProperty("canonicaliseFilePathContentBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("canonicaliseFilePathContentBetweenKeys" + i + ".key2");
			if (key1 != null && key2 != null) {
				MessageListener.debugMessage(testName, "Canonicalise filepath content between key1 '" + key1 + "' and key2 '" + key2 + "'");
				preparedExpectedResult = canonicaliseFilePathContentBetweenKeys(testName, preparedExpectedResult, key1, key2);
				preparedActualResult = canonicaliseFilePathContentBetweenKeys(testName, preparedActualResult, key1, key2);
				i++;
			} else {
				canonicaliseFilePathContentBetweenKeysProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check ignoreRegularExpressionKey properties");
		boolean ignoreRegularExpressionKeyProcessed = false;
		i = 1;
		while (!ignoreRegularExpressionKeyProcessed) {
			String key = properties.getProperty("ignoreRegularExpressionKey" + i + ".key");
			if (key != null) {
				MessageListener.debugMessage(testName, "Ignore regular expression key '" + key + "'");
				preparedExpectedResult = ignoreRegularExpression(preparedExpectedResult, key);
				preparedActualResult = ignoreRegularExpression(preparedActualResult, key);
				i++;
			} else {
				ignoreRegularExpressionKeyProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check removeRegularExpressionKey properties");
		boolean removeRegularExpressionKeyProcessed = false;
		i = 1;
		while (!removeRegularExpressionKeyProcessed) {
			String key = properties.getProperty("removeRegularExpressionKey" + i + ".key");
			if (key != null) {
				MessageListener.debugMessage(testName, "Remove regular expression key '" + key + "'");
				preparedExpectedResult = removeRegularExpression(preparedExpectedResult, key);
				preparedActualResult = removeRegularExpression(preparedActualResult, key);
				i++;
			} else {
				removeRegularExpressionKeyProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check replaceRegularExpressionKeys properties");
		boolean replaceRegularExpressionKeysProcessed = false;
		i = 1;
		while (!replaceRegularExpressionKeysProcessed) {
			String key1 = properties.getProperty("replaceRegularExpressionKeys" + i + ".key1");
			String key2 = properties.getProperty("replaceRegularExpressionKeys" + i + ".key2");
			if (key1 != null && key2 != null) {
				MessageListener.debugMessage(testName, "Replace regular expression from '" + key1 + "' to '" + key2 + "'");
				preparedExpectedResult = replaceRegularExpression(preparedExpectedResult, key1, key2);
				preparedActualResult = replaceRegularExpression(preparedActualResult, key1, key2);
				i++;
			} else {
				replaceRegularExpressionKeysProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check ignoreContentBetweenKeys properties");
		boolean ignoreContentBetweenKeysProcessed = false;
		i = 1;
		while (!ignoreContentBetweenKeysProcessed) {
			String key1 = properties.getProperty("ignoreContentBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("ignoreContentBetweenKeys" + i + ".key2");
			if (key1 != null && key2 != null) {
				MessageListener.debugMessage(testName, "Ignore content between key1 '" + key1 + "' and key2 '" + key2 + "'");
				preparedExpectedResult = ignoreContentBetweenKeys(preparedExpectedResult, key1, key2);
				preparedActualResult = ignoreContentBetweenKeys(preparedActualResult, key1, key2);
				i++;
			} else {
				ignoreContentBetweenKeysProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check ignoreKeysAndContentBetweenKeys properties");
		boolean ignoreKeysAndContentBetweenKeysProcessed = false;
		i = 1;
		while (!ignoreKeysAndContentBetweenKeysProcessed) {
			String key1 = properties.getProperty("ignoreKeysAndContentBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("ignoreKeysAndContentBetweenKeys" + i + ".key2");
			if (key1 != null && key2 != null) {
				MessageListener.debugMessage(testName, "Ignore keys and content between key1 '" + key1 + "' and key2 '" + key2 + "'");
				preparedExpectedResult = ignoreKeysAndContentBetweenKeys(preparedExpectedResult, key1, key2);
				preparedActualResult = ignoreKeysAndContentBetweenKeys(preparedActualResult, key1, key2);
				i++;
			} else {
				ignoreKeysAndContentBetweenKeysProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check removeKeysAndContentBetweenKeys properties");
		boolean removeKeysAndContentBetweenKeysProcessed = false;
		i = 1;
		while (!removeKeysAndContentBetweenKeysProcessed) {
			String key1 = properties.getProperty("removeKeysAndContentBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("removeKeysAndContentBetweenKeys" + i + ".key2");
			if (key1 != null && key2 != null) {
				MessageListener.debugMessage(testName, "Remove keys and content between key1 '" + key1 + "' and key2 '" + key2 + "'");
				preparedExpectedResult = removeKeysAndContentBetweenKeys(preparedExpectedResult, key1, key2);
				preparedActualResult = removeKeysAndContentBetweenKeys(preparedActualResult, key1, key2);
				i++;
			} else {
				removeKeysAndContentBetweenKeysProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check ignoreKey properties");
		boolean ignoreKeyProcessed = false;
		i = 1;
		while (!ignoreKeyProcessed) {
			String key = properties.getProperty("ignoreKey" + i);
			if (key != null) {
				MessageListener.debugMessage(testName, "Ignore key '" + key + "'");
				preparedExpectedResult = ignoreKey(preparedExpectedResult, key);
				preparedActualResult = ignoreKey(preparedActualResult, key);
				i++;
			} else {
				ignoreKeyProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check removeKey properties");
		boolean removeKeyProcessed = false;
		i = 1;
		while (!removeKeyProcessed) {
			String key = properties.getProperty("removeKey" + i);
			if (key != null) {
				MessageListener.debugMessage(testName, "Remove key '" + key + "'");
				preparedExpectedResult = removeKey(preparedExpectedResult, key);
				preparedActualResult = removeKey(preparedActualResult, key);
				i++;
			} else {
				removeKeyProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check replaceKey properties");
		boolean replaceKeyProcessed = false;
		i = 1;
		while (!replaceKeyProcessed) {
			String key1 = properties.getProperty("replaceKey" + i + ".key1");
			String key2 = properties.getProperty("replaceKey" + i + ".key2");
			if (key1 != null && key2 != null) {
				MessageListener.debugMessage(testName, "Replace key from '" + key1 + "' to '" + key2 + "'");
				preparedExpectedResult = replaceKey(preparedExpectedResult, key1, key2);
				preparedActualResult = replaceKey(preparedActualResult, key1, key2);
				i++;
			} else {
				replaceKeyProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check replaceEverywhereKey properties");
		boolean replaceEverywhereKeyProcessed = false;
		i = 1;
		while (!replaceEverywhereKeyProcessed) {
			String key1 = properties.getProperty("replaceEverywhereKey" + i + ".key1");
			String key2 = properties.getProperty("replaceEverywhereKey" + i + ".key2");
			if (key1 != null && key2 != null) {
				MessageListener.debugMessage(testName, "Replace key from '" + key1 + "' to '" + key2 + "'");
				preparedExpectedResult = replaceKey(preparedExpectedResult, key1, key2);
				preparedActualResult = replaceKey(preparedActualResult, key1, key2);
				i++;
			} else {
				replaceEverywhereKeyProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check ignoreCurrentTimeBetweenKeys properties");
		boolean ignoreCurrentTimeBetweenKeysProcessed = false;
		i = 1;
		while (!ignoreCurrentTimeBetweenKeysProcessed) {
			String key1 = properties.getProperty("ignoreCurrentTimeBetweenKeys" + i + ".key1");
			String key2 = properties.getProperty("ignoreCurrentTimeBetweenKeys" + i + ".key2");
			String pattern = properties.getProperty("ignoreCurrentTimeBetweenKeys" + i + ".pattern");
			String margin = properties.getProperty("ignoreCurrentTimeBetweenKeys" + i + ".margin");
			boolean errorMessageOnRemainingString = true;
			if ("false".equals(properties.getProperty("ignoreCurrentTimeBetweenKeys" + i + ".errorMessageOnRemainingString"))) {
				errorMessageOnRemainingString = false;
			}
			if (key1 != null && key2 != null && margin != null) {
				MessageListener.debugMessage(testName, "Ignore current time between key1 '" + key1 + "' and key2 '" + key2 + "' (errorMessageOnRemainingString is " + errorMessageOnRemainingString + ")");
				MessageListener.debugMessage(testName, "For result string");
				preparedActualResult = ignoreCurrentTimeBetweenKeys(testName, preparedActualResult, key1, key2, pattern, margin, errorMessageOnRemainingString, false);
				MessageListener.debugMessage(testName, "For expected string");
				preparedExpectedResult = ignoreCurrentTimeBetweenKeys(testName, preparedExpectedResult, key1, key2, pattern, margin, errorMessageOnRemainingString, true);
				i++;
			} else {
				ignoreCurrentTimeBetweenKeysProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check ignoreContentBeforeKey properties");
		boolean ignoreContentBeforeKeyProcessed = false;
		i = 1;
		while (!ignoreContentBeforeKeyProcessed) {
			String key = properties.getProperty("ignoreContentBeforeKey" + i);
			if (key == null) {
				key = properties.getProperty("ignoreContentBeforeKey" + i + ".key");
			}
			if (key != null) {
				MessageListener.debugMessage(testName, "Ignore content before key '" + key + "'");
				preparedExpectedResult = ignoreContentBeforeKey(preparedExpectedResult, key);
				preparedActualResult = ignoreContentBeforeKey(preparedActualResult, key);
				i++;
			} else {
				ignoreContentBeforeKeyProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check ignoreContentAfterKey properties");
		boolean ignoreContentAfterKeyProcessed = false;
		i = 1;
		while (!ignoreContentAfterKeyProcessed) {
			String key = properties.getProperty("ignoreContentAfterKey" + i);
			if (key == null) {
				key = properties.getProperty("ignoreContentAfterKey" + i + ".key");
			}
			if (key != null) {
				MessageListener.debugMessage(testName, "Ignore content after key '" + key + "'");
				preparedExpectedResult = ignoreContentAfterKey(preparedExpectedResult, key);
				preparedActualResult = ignoreContentAfterKey(preparedActualResult, key);
				i++;
			} else {
				ignoreContentAfterKeyProcessed = true;
			}
		}
		MessageListener.debugMessage(testName, "Check ignoreContentAfterKey properties");
		String diffType = properties.getProperty(step + ".diffType");
		if ((diffType != null && (diffType.equals(".xml") || diffType.equals(".wsdl")))
				|| (diffType == null && (fileName.endsWith(".xml") || fileName.endsWith(".wsdl")))) {
			// xml diff
			Diff diff = null;
			boolean identical = false;
			Exception diffException = null;
			try {
				diff = new Diff(preparedExpectedResult, preparedActualResult);
				identical = diff.identical();
			} catch(Exception e) {
				diffException = e;
			}
			if (identical) {
				ok = TestTool.RESULT_OK;
				MessageListener.debugMessage(testName, "Strings are identical");
				MessageListener.debugPipelineMessage(testName, stepDisplayName, "Result", printableActualResult);
				MessageListener.debugPipelineMessagePreparedForDiff(testName, stepDisplayName, "Result as prepared for diff", preparedActualResult);
			} else {
				MessageListener.debugMessage(testName, "Strings are not identical");
				String message;
				if (diffException == null) {
					message = diff.toString();
				} else {
					message = "Exception during XML diff: " + diffException.getMessage();
					MessageListener.errorMessage(testName, "Exception during XML diff: ", diffException);
				}
				MessageListener.wrongPipelineMessage(testName, stepDisplayName, message, printableActualResult, printableExpectedResult, originalFilePath);
				MessageListener.wrongPipelineMessagePreparedForDiff(testName, stepDisplayName, preparedActualResult, preparedExpectedResult, originalFilePath);
				if (TestTool.autoSaveDiffs) {
					String filenameAbsolutePath = (String)properties.get(step + ".absolutepath");
					MessageListener.debugMessage(testName, "Copy actual result to ["+filenameAbsolutePath+"]");
					try {
						org.apache.commons.io.FileUtils.writeStringToFile(new File(filenameAbsolutePath), actualResult);
					} catch (IOException e) {
					}
					ok = TestTool.RESULT_AUTOSAVED;
				}
			}
		} else {
			// txt diff
			String formattedPreparedExpectedResult = formatString(testName, preparedExpectedResult);
			String formattedPreparedActualResult = formatString(testName, preparedActualResult);
			if (formattedPreparedExpectedResult.equals(formattedPreparedActualResult)) {
				ok = TestTool.RESULT_OK;
				MessageListener.debugMessage(testName, "Strings are identical");
				MessageListener.debugPipelineMessage(testName, stepDisplayName, "Result", printableActualResult);
				MessageListener.debugPipelineMessagePreparedForDiff(testName, stepDisplayName, "Result as prepared for diff", preparedActualResult);
			} else {
				MessageListener.debugMessage(testName, "Strings are not identical");
				String message = null;
				StringBuilder diffActual = new StringBuilder();
				StringBuilder diffExcpected = new StringBuilder();
				int j = formattedPreparedActualResult.length();
				if (formattedPreparedExpectedResult.length() > i) {
					j = formattedPreparedExpectedResult.length();
				}
				for (i = 0; i < j; i++) {
					if (i >= formattedPreparedActualResult.length() || i >= formattedPreparedExpectedResult.length()
							|| formattedPreparedActualResult.charAt(i) != formattedPreparedExpectedResult.charAt(i)) {
						if (message == null) {
							message = "Starting at char " + (i + 1);
						}
						if (i < formattedPreparedActualResult.length()) {
							diffActual.append(formattedPreparedActualResult.charAt(i));
						}
						if (i < formattedPreparedExpectedResult.length()) {
							diffExcpected.append(formattedPreparedExpectedResult.charAt(i));
						}
					}
				}
				if (diffActual.length() > 250) {
					diffActual.delete(250, diffActual.length());
					diffActual.append(" ...");
				}
				if (diffExcpected.length() > 250) {
					diffExcpected.delete(250, diffExcpected.length());
					diffExcpected.append(" ...");
				}
				message = message + " actual result is '" + diffActual + "' and expected result is '" + diffExcpected + "'";
				MessageListener.wrongPipelineMessage(testName, stepDisplayName, message, printableActualResult, printableExpectedResult, originalFilePath);
				MessageListener.wrongPipelineMessagePreparedForDiff(testName, stepDisplayName, preparedActualResult, preparedExpectedResult, originalFilePath);
				if (TestTool.autoSaveDiffs) {
					String filenameAbsolutePath = (String)properties.get(step + ".absolutepath");
					MessageListener.debugMessage(testName, "Copy actual result to ["+filenameAbsolutePath+"]");
					try {
						org.apache.commons.io.FileUtils.writeStringToFile(new File(filenameAbsolutePath), actualResult);
					} catch (IOException e) {
					}
					ok = TestTool.RESULT_AUTOSAVED;
				}
			}
		}
		return ok;
	}

	public static String decodeUnzipContentBetweenKeys(String testName, String string, String key1, String key2, boolean replaceNewlines) {
		String result = string;
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			MessageListener.debugMessage(testName, "Key 1 found");
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				MessageListener.debugMessage(testName, "Key 2 found");
				String encoded = result.substring(i + key1.length(), j);
				String unzipped = null;
				byte[] decodedBytes = null;
				Base64 decoder = new Base64();
				MessageListener.debugMessage(testName, "Decode");
				decodedBytes = decoder.decodeBase64(encoded);
				if (unzipped == null) {
					try {
						MessageListener.debugMessage(testName, "Unzip");
						StringBuffer stringBuffer = new StringBuffer();
						stringBuffer.append("<tt:file xmlns:tt=\"testtool\">");
						ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(decodedBytes));
						stringBuffer.append("<tt:name>" + zipInputStream.getNextEntry().getName() + "</tt:name>");
						stringBuffer.append("<tt:content>");
						byte[] buffer = new byte[1024];
						int readLength = zipInputStream.read(buffer);
						while (readLength != -1) {
							String part = new String(buffer, 0, readLength, "UTF-8");
							if (replaceNewlines) {
								part = StringUtils.replace(StringUtils.replace(part, "\r", "[CARRIAGE RETURN]"), "\n", "[LINE FEED]");
							}
							stringBuffer.append(part);
							readLength = zipInputStream.read(buffer);
						}
						stringBuffer.append("</tt:content>");
						stringBuffer.append("</tt:file>");
						unzipped = stringBuffer.toString();
					} catch(Exception e) {
						MessageListener.errorMessage(testName, "Could not unzip: " + e.getMessage(), e);
						unzipped = encoded;
					}
				}
				result = result.substring(0, i) + key1 + unzipped + result.substring(j);
				i = result.indexOf(key1, i + key1.length() + unzipped.length() + key2.length());
			} else {
				i = -1;
			}
		}
		return result;
	}

	public static String ignoreCurrentTimeBetweenKeys(String testName, String string, String key1, String key2, String pattern, String margin, boolean errorMessageOnRemainingString, boolean isControlString) {
		String result = string;
		String ignoreText = "IGNORE_CURRENT_TIME";
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			MessageListener.debugMessage(testName, "Key 1 found");
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				MessageListener.debugMessage(testName, "Key 2 found");
				String dateString = result.substring(i + key1.length(), j);
				Date date;
				boolean remainingString = false;
				try {
					SimpleDateFormat simpleDateFormat = null;
					if (pattern == null) {
						// Expect time in milliseconds
						date = new Date(Long.parseLong(dateString));
					} else {
						simpleDateFormat = new SimpleDateFormat(pattern);
						ParsePosition parsePosition = new ParsePosition(0);
						date = simpleDateFormat.parse(dateString, parsePosition);
						if (parsePosition.getIndex() != dateString.length()) {
							remainingString = true;
							i = result.indexOf(key1, j + key2.length());
							if (errorMessageOnRemainingString) {
								MessageListener.errorMessage(testName, "Found remaining string after parsing date with pattern '"
											 + pattern + "': "
											 + dateString.substring(parsePosition.getIndex()));
							}
						}
					}
					if (!remainingString) {
						if (isControlString) {
							// Ignore the date in the control string independent on margin from current time
							result = result.substring(0, i) + key1 + ignoreText + result.substring(j);
							i = result.indexOf(key1, i + key1.length() + ignoreText.length() + key2.length());
						} else {
							// Ignore the date in the test string dependent on margin from current time
							String currentTime;
							long currentTimeMillis;
							if (pattern == null) {
								currentTime = "" + System.currentTimeMillis();
								currentTimeMillis = Long.parseLong(currentTime);
							} else {
								currentTime = simpleDateFormat.format(new Date(System.currentTimeMillis()));
								currentTimeMillis = simpleDateFormat.parse(currentTime).getTime();
							}
							if (date.getTime() >= currentTimeMillis - Long.parseLong(margin) && date.getTime() <= currentTimeMillis + Long.parseLong(margin)) {
								result = result.substring(0, i) + key1 + ignoreText + result.substring(j);
								i = result.indexOf(key1, i + key1.length() + ignoreText.length() + key2.length());
							} else {
								MessageListener.errorMessage(testName, "Dates differ too much. Current time: '" + currentTime + "'. Result time: '" + dateString + "'");
								i = result.indexOf(key1, j + key2.length());
							}
						}
					}
				} catch(ParseException e) {
					i = -1;
					MessageListener.errorMessage(testName, "Could not parse margin or date: " + e.getMessage(), e);
				} catch(NumberFormatException e) {
					i = -1;
					MessageListener.errorMessage(testName, "Could not parse long value: " + e.getMessage(), e);
				}
			} else {
				i = -1;
			}
		}
		return result;
	}

	public static String canonicaliseFilePathContentBetweenKeys(String testName, String string, String key1, String key2) {
		String result = string;
		if (key1.equals("*") && key2.equals("*")) {
			File file = new File(result);
			try {
				result = file.getCanonicalPath();
			} catch (IOException e) {
				MessageListener.errorMessage(testName, "Could not canonicalise filepath: " + e.getMessage(), e);
			}
			result = FilenameUtils.normalize(result);
		} else {
			int i = result.indexOf(key1);
			while (i != -1 && result.length() > i + key1.length()) {
				int j = result.indexOf(key2, i + key1.length());
				if (j != -1) {
					String fileName = result.substring(i + key1.length(), j);
					File file = new File(fileName);
					try {
						fileName = file.getCanonicalPath();
					} catch (IOException e) {
						MessageListener.errorMessage(testName, "Could not canonicalise filepath: " + e.getMessage(), e);
					}
					fileName = FilenameUtils.normalize(fileName);
					result = result.substring(0, i) + key1 + fileName + result.substring(j);
					i = result.indexOf(key1, i + key1.length() + fileName.length() + key2.length());
				} else {
					i = -1;
				}
			}
		}
		return result;
	}

	public static String ignoreContentBeforeKey(String string, String key) {
		int i = string.indexOf(key);
		if (i == -1) {
			return string;
		} else {
			return string.substring(i) + "IGNORE";
		}
	}

	public static String ignoreContentAfterKey(String string, String key) {
		int i = string.indexOf(key);
		if (i == -1) {
			return string;
		} else {
			return string.substring(0, i + key.length()) + "IGNORE";
		}
	}

	public static String ignoreRegularExpression(String string, String regex) {
		return string.replaceAll(regex, "IGNORE");
	}

	public static String removeRegularExpression(String string, String regex) {
		return string.replaceAll(regex, "");
	}

	public static String replaceRegularExpression(String string, String from, String to) {
		return string.replaceAll(from, to);
	}

	public static String ignoreContentBetweenKeys(String string, String key1, String key2) {
		String result = string;
		String ignoreText = "IGNORE";
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				result = result.substring(0, i) + key1 + ignoreText + result.substring(j);
				i = result.indexOf(key1, i + key1.length() + ignoreText.length() + key2.length());
			} else {
				i = -1;
			}
		}
		return result;
	}

	public static String ignoreKeysAndContentBetweenKeys(String string, String key1, String key2) {
		String result = string;
		String ignoreText = "IGNORE";
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				result = result.substring(0, i) + ignoreText + result.substring(j + key2.length());
				i = result.indexOf(key1, i + ignoreText.length());
			} else {
				i = -1;
			}
		}
		return result;
	}

	public static String removeKeysAndContentBetweenKeys(String string, String key1, String key2) {
		String result = string;
		int i = result.indexOf(key1);
		while (i != -1 && result.length() > i + key1.length()) {
			int j = result.indexOf(key2, i + key1.length());
			if (j != -1) {
				result = result.substring(0, i) + result.substring(j + key2.length());
				i = result.indexOf(key1, i);
			} else {
				i = -1;
			}
		}
		return result;
	}

	public static String ignoreKey(String string, String key) {
		String result = string;
		String ignoreText = "IGNORE";
		int i = result.indexOf(key);
		while (i != -1) {
			result = result.substring(0, i) + ignoreText + result.substring(i + key.length());
			i = result.indexOf(key, i);
		}
		return result;
	}

	public static String removeKey(String string, String key) {
		String result = string;
		int i = result.indexOf(key);
		while (i != -1) {
			result = result.substring(0, i) + result.substring(i + key.length());
			i = result.indexOf(key, i);
		}
		return result;
	}

	public static String replaceKey(String string, String from, String to) {
		String result = string;
		if (!from.equals(to)) {
			int i = result.indexOf(from);
			while (i != -1) {
				result = result.substring(0, i) + to + result.substring(i + from.length());
				i = result.indexOf(from, i);
			}
		}
		return result;
	}

	public static String formatString(String testName, String string) {
		StringBuffer sb = new StringBuffer();
		try {
			Reader reader = new StringReader(string);
			BufferedReader br = new BufferedReader(reader);
			String l = null;
			while ((l = br.readLine()) != null) {
				if (sb.length()==0) {
					sb.append(l);
				} else {
					sb.append(System.getProperty("line.separator") + l);
				}
			}
			br.close();
		} catch(Exception e) {
			MessageListener.errorMessage(testName, "Could not read string '" + string + "': " + e.getMessage(), e);
		}
		return sb.toString();
	}
}
