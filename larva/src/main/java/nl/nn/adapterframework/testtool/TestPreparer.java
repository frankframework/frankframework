package nl.nn.adapterframework.testtool;

import com.sun.syndication.io.XmlReader;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.*;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartHttpServletRequest;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/**
 * @author Jaco de Groot, Murat Kaan Meral
 *
 */
public class TestPreparer {
	public static Map<String, String> scenariosRootDirectories;
	private static Logger logger = LogUtil.getLogger(TestPreparer.class);
	public static AppConstants getAppConstantsFromDirectory(String currentScenariosRootDirectory,
															AppConstants appConstants) {
		String appConstantsDirectory = appConstants.getResolvedProperty("larva.appconstants.directory");
		if (appConstantsDirectory != null) {
			appConstantsDirectory = TestPreparer.getAbsolutePath(currentScenariosRootDirectory, appConstantsDirectory);
			if (new File(currentScenariosRootDirectory).exists()) {
				if (new File(appConstantsDirectory).exists()) {
					logger.debug("Get AppConstants from directory: " + appConstantsDirectory);
					appConstants = AppConstants.getInstance(appConstantsDirectory);
				} else {
					logger.error("Directory for AppConstans not found");
				}
			}
		}
		return appConstants;
	}

	public static Map<String, String> getScenariosList(Map<String, List<File>> scenarioFiles, String scenariosRootDirectory, AppConstants appConstants) {
		if(scenarioFiles == null) {
			readScenarioFiles(scenariosRootDirectory, true, appConstants);
		}
		logger.debug("Listing possible executable scenarios.");
		Map<String, String> scenarios = new HashMap<>();
		Iterator<Entry<String, List<File>>> mapIterator = scenarioFiles.entrySet().iterator();
		while(mapIterator.hasNext()) {
			Entry<String, List<File>> entry = mapIterator.next();
			Iterator<File> scenarioFilesIterator = entry.getValue().iterator();
			while (scenarioFilesIterator.hasNext()) {
				File scenarioFile = (File) scenarioFilesIterator.next();
				String scenarioDirectory = scenarioFile.getParentFile().getAbsolutePath() + File.separator;
				Properties properties = readProperties(appConstants, scenarioFile);
				logger.debug("Add parent directories of '" + scenarioDirectory + "'");
				int i = -1;
				String scenarioDirectoryCanonicalPath;
				String scenariosRootDirectoryCanonicalPath;
				try {
					scenarioDirectoryCanonicalPath = new File(scenarioDirectory).getCanonicalPath();
					scenariosRootDirectoryCanonicalPath = new File(scenariosRootDirectory).getCanonicalPath();
				} catch (IOException e) {
					scenarioDirectoryCanonicalPath = scenarioDirectory;
					scenariosRootDirectoryCanonicalPath = scenariosRootDirectory;
					logger.error("Could not get canonical path: " + e.getMessage(), e);
				}
				if (scenarioDirectoryCanonicalPath.startsWith(scenariosRootDirectoryCanonicalPath)) {
					i = scenariosRootDirectory.length() - 1;
					while (i != -1) {
						String longName = scenarioDirectory.substring(0, i + 1);
						if (!scenarios.containsKey(longName)) {
							String shortName = scenarioDirectory.substring(scenariosRootDirectory.length() - 1, i + 1);
							logger.debug("Added '" + longName + "' as '" + shortName + "' to the scenarios list.");

							scenarios.put(longName, shortName);
						}
						i = scenarioDirectory.indexOf(File.separator, i + 1);
					}
					String longName = scenarioFile.getAbsolutePath();
					String shortName = longName.substring(scenariosRootDirectory.length() - 1,
							longName.length() - ".properties".length());
					logger.debug("Added '" + longName + "' as '" + shortName + "' to the scenarios list.");
					scenarios.put(longName, shortName);
				}
			}
		}
		return scenarios;
	}

	public static String initScenariosRootDirectories(String realPath, String paramScenariosRootDirectory, AppConstants appConstants) {
		scenariosRootDirectories = new HashMap<String, String>();
		String currentScenariosRootDirectory = null;
		String firstScenarioRootDirectory = null;
		if (realPath == null) {
			logger.error("Could not read webapp real path");
		} else {
			if (!realPath.endsWith(File.separator)) {
				realPath = realPath + File.separator;
			}
			Map<String, String> scenariosRoots = new HashMap<String, String>();
			Map<String, String> scenariosRootsBroken = new HashMap<String, String>();
			int j = 1;
			String directory = appConstants.getResolvedProperty("scenariosroot" + j + ".directory");
			String description = appConstants.getResolvedProperty("scenariosroot" + j + ".description");
			while (directory != null) {
				if (description == null) {
					logger.error("Could not find description for root directory '" + directory + "'");
				} else if (scenariosRoots.get(description) != null) {
					logger.error("A root directory named '" + description + "' already exist");
				} else {
					String parent = realPath;
					String m2eFileName = appConstants.getResolvedProperty("scenariosroot" + j + ".m2e.pom.properties");
					if (m2eFileName != null) {
						File m2eFile = new File(realPath, m2eFileName);
						if (m2eFile.exists()) {
							logger.debug("Read m2e pom.properties: " + m2eFileName);
							Properties m2eProperties = readProperties(null, m2eFile, false);
							parent = m2eProperties.getProperty("m2e.projectLocation");
							logger.debug("Use m2e parent: " + parent);
						}
					}
					directory = getAbsolutePath(parent, directory, true);
					if (new File(directory).exists()) {
						scenariosRoots.put(description, directory);
					} else {
						scenariosRootsBroken.put(description, directory);
					}
				}
				j++;
				directory = appConstants.getResolvedProperty("scenariosroot" + j + ".directory");
				description = appConstants.getResolvedProperty("scenariosroot" + j + ".description");
			}
			TreeSet<String> treeSet = new TreeSet<String>(new CaseInsensitiveComparator());
			treeSet.addAll(scenariosRoots.keySet());
			Iterator<String> iterator = treeSet.iterator();
			while (iterator.hasNext()) {
				description = (String) iterator.next();
				scenariosRootDirectories.put(description, scenariosRoots.get(description));
				if(firstScenarioRootDirectory == null) {
					firstScenarioRootDirectory = scenariosRoots.get(description);
				}
			}
			treeSet.clear();
			treeSet.addAll(scenariosRootsBroken.keySet());
			iterator = treeSet.iterator();
			while (iterator.hasNext()) {
				description = (String) iterator.next();
				scenariosRootDirectories.put("X " + description, scenariosRootsBroken.get(description));
				if(firstScenarioRootDirectory == null) {
					firstScenarioRootDirectory = scenariosRootsBroken.get(description);
				}
			}
			logger.debug("Read scenariosrootdirectory parameter");
			logger.debug("Get current scenarios root directory");
			if (paramScenariosRootDirectory == null || paramScenariosRootDirectory.equals("")) {
				String scenariosRootDefault = appConstants.getResolvedProperty("scenariosroot.default");
				if (scenariosRootDefault != null) {
					currentScenariosRootDirectory = scenariosRoots.get(scenariosRootDefault);
				}
				if (currentScenariosRootDirectory == null && scenariosRootDirectories.size() > 0) {
					currentScenariosRootDirectory = firstScenarioRootDirectory;
				}
			} else {
				currentScenariosRootDirectory = paramScenariosRootDirectory;
			}
		}
		return currentScenariosRootDirectory;
	}

	/**
	 * Reads all scenario files in a given directory, and groups them by their
	 * parent path for multithreading.
	 *
	 * @param scenariosDirectory
	 *            Root directory to start the search from.
	 * @param forMultiThreading
	 *            to decide if we want multithreading or not, otherwise map will
	 *            only have one entry with a list that contains all the scenarios.
	 * @return map that contains path names and list of scenarios that are in the
	 *         path.
	 */
	public static Map<String, List<File>> readScenarioFiles(String scenariosDirectory, boolean forMultiThreading, AppConstants appConstants) {
		Map<String, List<File>> scenarioFiles;
		logger.debug("Read scenarios from directory '" + scenariosDirectory + "'");
		scenarioFiles = new HashMap<>();
		String generalKey = "";
		if(!forMultiThreading) {
			scenarioFiles.put(generalKey, new ArrayList<File>());
		}
		// If only one scenario is selected
		if (scenariosDirectory.endsWith(".properties")) {
			logger.debug("Only one scenario detected!");
			List<File> fileList = new ArrayList<>();
			fileList.add(new File(scenariosDirectory));
			scenarioFiles.put(scenariosDirectory, fileList);
			return scenarioFiles;
		}

		logger.debug("List all files in directory '" + scenariosDirectory + "'");
		File[] files = new File(scenariosDirectory).listFiles();
		if (files == null) {
			logger.debug("Could not read files from directory '" + scenariosDirectory + "'");
		} else {
			// This will later be helpful, when we want to execute scenarios sequentially.
			logger.debug("Sort files");
			Arrays.sort(files);
			logger.debug("Filter out property files containing a 'scenario.description' property");
			for (int i = 0; i < files.length; i++) {
				File file = files[i];
				if (file.getName().endsWith(".properties")) {
					Properties properties = readProperties(appConstants, file);
					if (properties != null && properties.get("scenario.description") != null) {
						String active = properties.getProperty("scenario.active", "true");
						String unstable = properties.getProperty("adapter.unstable", "false");
						if (active.equalsIgnoreCase("true") && unstable.equalsIgnoreCase("false")) {

							String parent = file.getParent();
							if (forMultiThreading && !scenarioFiles.containsKey(parent)) {
								scenarioFiles.put(parent, new ArrayList<File>());
							}
							scenarioFiles.get(forMultiThreading ? parent : generalKey).add(file);
						}
					}
				} else if (file.isDirectory() && (!file.getName().equals("CVS"))) {
					Map<String, List<File>> recursiveOutput = readScenarioFiles(file.getAbsolutePath(),
							forMultiThreading, appConstants);
					Iterator<Entry<String, List<File>>> mapIterator = recursiveOutput.entrySet().iterator();
					while (mapIterator.hasNext()) {
						Map.Entry<String, List<File>> entry = mapIterator.next();

						if (scenarioFiles.containsKey(entry.getKey())) {
							// This is case for no multithreading, and anomalies in multithreading version
							scenarioFiles.get(entry.getKey()).addAll(entry.getValue());
						} else {
							scenarioFiles.put(entry.getKey(), entry.getValue());
						}
					}
				}
			}
		}
		logger.debug(scenarioFiles.size() + " scenario files found");
		return scenarioFiles;
	}

	/**
	 * Returns the total number of scenarios stored in the map
	 *
	 * @param scenarios
	 *            Map that contains scenario files
	 * @return total number of scenarios that are in the map.
	 */
	public static int getNumberOfScenarios(Map<String, List<File>> scenarios) {
		int numberOfScenarios = 0;
		Iterator<Entry<String, List<File>>> scenariosIterator = scenarios.entrySet().iterator();

		while (scenariosIterator.hasNext()) {
			Map.Entry<String, List<File>> entry = scenariosIterator.next();

			numberOfScenarios += entry.getValue().size();
		}

		return numberOfScenarios;
	}

	public static Properties readProperties(AppConstants appConstants, File propertiesFile) {
		return readProperties(appConstants, propertiesFile, true);
	}

	public static Properties readProperties(AppConstants appConstants, File propertiesFile, boolean root) {
		String directory = new File(propertiesFile.getAbsolutePath()).getParent();
		Properties properties = new Properties();
		FileInputStream fileInputStreamPropertiesFile = null;
		try {
			fileInputStreamPropertiesFile = new FileInputStream(propertiesFile);
			properties.load(fileInputStreamPropertiesFile);
			fileInputStreamPropertiesFile.close();
			fileInputStreamPropertiesFile = null;
			Properties includedProperties = new Properties();
			int i = 0;
			String includeFilename = properties.getProperty("include");
			if (includeFilename == null) {
				i++;
				includeFilename = properties.getProperty("include" + i);
			}
			while (includeFilename != null) {
				logger.debug("Load include file: " + includeFilename);
				File includeFile = new File(getAbsolutePath(directory, includeFilename));
				Properties includeProperties = readProperties(appConstants, includeFile, false);
				includedProperties.putAll(includeProperties);
				i++;
				includeFilename = properties.getProperty("include" + i);
			}
			properties.putAll(includedProperties);
			if (root) {
				properties.putAll(appConstants);
				for (Object key : properties.keySet()) {
					properties.put(key, StringResolver.substVars((String) properties.get(key), properties));
				}
				addAbsolutePathProperties(directory, properties);
			}
			logger.debug(properties.size() + " properties found");
		} catch (Exception e) {
			properties = null;
			logger.error("Could not read properties file: " + e.getMessage(), e);
			if (fileInputStreamPropertiesFile != null) {
				try {
					fileInputStreamPropertiesFile.close();
				} catch (Exception e2) {
					logger.error("Could not close file '" + propertiesFile.getAbsolutePath() + "': " + e2.getMessage(), e);
				}
			}
		}
		return properties;
	}

	public static String getAbsolutePath(String parent, String child) {
		return getAbsolutePath(parent, child, false);
	}

	/**
	 * Returns the absolute pathname for the child pathname. The parent pathname is
	 * used as a prefix when the child pathname is an not absolute.
	 *
	 * @param parent
	 *            the parent pathname to use
	 * @param child
	 *            the child pathname to convert to a absolute pathname
	 */
	public static String getAbsolutePath(String parent, String child, boolean addFileSeparator) {
		File result;
		File file = new File(child);
		if (file.isAbsolute()) {
			result = file;
		} else {
			result = new File(parent, child);
		}
		String absPath = FilenameUtils.normalize(result.getAbsolutePath());
		if (addFileSeparator) {
			return absPath + File.separator;
		} else {
			return absPath;
		}
	}

	public static void addAbsolutePathProperties(String propertiesDirectory, Properties properties) {
		Properties absolutePathProperties = new Properties();
		Iterator<?> iterator = properties.keySet().iterator();
		while (iterator.hasNext()) {
			String property = (String) iterator.next();
			if (property.equalsIgnoreCase("configurations.directory"))
				continue;

			if (property.endsWith(".read") || property.endsWith(".write") || property.endsWith(".directory")
					|| property.endsWith(".filename") || property.endsWith(".valuefile")
					|| property.endsWith(".valuefileinputstream")) {
				String absolutePathProperty = property + ".absolutepath";
				String value = getAbsolutePath(propertiesDirectory, (String) properties.get(property));
				if (value != null) {
					absolutePathProperties.put(absolutePathProperty, value);
				}
			}
		}
		properties.putAll(absolutePathProperties);
	}

	public static List<String> getSteps(Properties properties) {
		List<String> steps = new ArrayList<String>();
		int i = 1;
		boolean lastStepFound = false;
		while (!lastStepFound) {
			boolean stepFound = false;
			Enumeration<?> enumeration = properties.propertyNames();
			while (enumeration.hasMoreElements()) {
				String key = (String) enumeration.nextElement();
				if (key.startsWith("step" + i + ".") && (key.endsWith(".read") || key.endsWith(".write"))) {
					if (!stepFound) {
						steps.add(key);
						stepFound = true;
						logger.debug("Added step '" + key + "'");
					} else {
						logger.error("More than one step" + i + " properties found, already found '"
								+ steps.get(steps.size() - 1) + "' before finding '" + key + "'");
					}
				}
			}
			if (!stepFound) {
				lastStepFound = true;
			}
			i++;
		}
		logger.debug(steps.size() + " steps found");
		return steps;
	}

	public static String readFile(String fileName) {
		String result = null;
		String encoding = null;
		if (fileName.endsWith(".xml") || fileName.endsWith(".wsdl")) {
			// Determine the encoding the XML way but don't use an XML parser to
			// read the file and transform it to a string to prevent changes in
			// formatting and prevent adding an xml declaration where this is
			// not present in the file. For example, when using a
			// WebServiceSender to send a message to a WebServiceListener the
			// xml message must not contain an xml declaration.
			// AFAIK the Java 1.4 standard XML api doesn't provide a method
			// to determine the encoding used by an XML file, thus use the
			// XmlReader from ROME (https://rome.dev.java.net/).
			try {
				XmlReader xmlReader = new XmlReader(new File(fileName));
				encoding = xmlReader.getEncoding();
				xmlReader.close();
			} catch (IOException e) {
				logger.error("Could not determine encoding for file '" + fileName + "': " + e.getMessage(), e);
			}
		} else if (fileName.endsWith(".utf8")) {
			encoding = "UTF-8";
		} else {
			encoding = "ISO-8859-1";
		}
		if (encoding != null) {
			InputStreamReader inputStreamReader = null;
			try {
				StringBuffer stringBuffer = new StringBuffer();
				inputStreamReader = new InputStreamReader(new FileInputStream(fileName), encoding);
				char[] cbuf = new char[4096];
				int len = inputStreamReader.read(cbuf);
				while (len != -1) {
					stringBuffer.append(cbuf, 0, len);
					len = inputStreamReader.read(cbuf);
				}
				result = stringBuffer.toString();
			} catch (Exception e) {
				logger.error("Could not read file '" + fileName + "': " + e.getMessage(), e);
			} finally {
				if (inputStreamReader != null) {
					try {
						inputStreamReader.close();
					} catch (Exception e) {
						logger.error("Could not close file '" + fileName + "': " + e.getMessage(), e);
					}
				}
			}
		}
		return result;
	}

	/**
	 * Create a Map for a specific property based on other properties that are the
	 * same except for a .param1.name, .param1.value or .param1.valuefile suffix.
	 * The property with the .name suffix specifies the key for the Map, the
	 * property with the value suffix specifies the value for the Map. A property
	 * with a the .valuefile suffix can be used as an alternative for a property
	 * with a .value suffix to specify the file to read the value for the Map from.
	 * More than one param can be specified by using param2, param3 etc.
	 *
	 * @param properties
	 * @param property
	 * @return A map with parameters
	 */
	public static Map<String, Object> createParametersMapFromParamProperties(Properties properties, String property,
																			 boolean createParameterObjects, ParameterResolutionContext parameterResolutionContext) {
		logger.debug("Search parameters for property '" + property + "'");
		Map<String, Object> result = new HashMap<String, Object>();
		boolean processed = false;
		int i = 1;
		while (!processed) {
			String name = properties.getProperty(property + ".param" + i + ".name");
			if (name != null) {
				Object value;
				String type = properties.getProperty(property + ".param" + i + ".type");
				if ("httpResponse".equals(type)) {
					String outputFile;
					String filename = properties.getProperty(property + ".param" + i + ".filename");
					if (filename != null) {
						outputFile = properties.getProperty(property + ".param" + i + ".filename.absolutepath");
					} else {
						outputFile = properties.getProperty(property + ".param" + i + ".outputfile");
					}
					HttpServletResponseMock httpServletResponseMock = new HttpServletResponseMock();
					httpServletResponseMock.setOutputFile(outputFile);
					value = httpServletResponseMock;
				} else if ("httpRequest".equals(type)) {
					value = properties.getProperty(property + ".param" + i + ".value");
					if ("multipart".equals(value)) {
						MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
						// following line is required to avoid
						// "(FileUploadException) the request was rejected because
						// no multipart boundary was found"
						request.setContentType("multipart/mixed;boundary=gc0p4Jq0M2Yt08jU534c0p");
						List<Part> parts = new ArrayList<Part>();
						boolean partsProcessed = false;
						int j = 1;
						while (!partsProcessed) {
							String filename = properties
									.getProperty(property + ".param" + i + ".part" + j + ".filename");
							if (filename == null) {
								partsProcessed = true;
							} else {
								String partFile = properties
										.getProperty(property + ".param" + i + ".part" + j + ".filename.absolutepath");
								String partType = properties
										.getProperty(property + ".param" + i + ".part" + j + ".type");
								String partName = properties
										.getProperty(property + ".param" + i + ".part" + j + ".name");
								if ("file".equalsIgnoreCase(partType)) {
									File file = new File(partFile);
									try {
										FilePart filePart = new FilePart("file" + j,
												(partName == null ? file.getName() : partName), file);
										parts.add(filePart);
									} catch (FileNotFoundException e) {
										logger.error("Could not read file '" + partFile + "': " + e.getMessage(), e);
									}
								} else {
									String string = readFile(partFile);
									StringPart stringPart = new StringPart((partName == null ? "string" + j : partName),
											string);
									parts.add(stringPart);
								}
								j++;
							}
						}
						Part allParts[] = new Part[parts.size()];
						allParts = parts.toArray(allParts);
						MultipartRequestEntity multipartRequestEntity = new MultipartRequestEntity(allParts,
								new PostMethod().getParams());
						ByteArrayOutputStream requestContent = new ByteArrayOutputStream();
						try {
							multipartRequestEntity.writeRequest(requestContent);
						} catch (IOException e) {
							logger.error("Could not create multipart: " + e.getMessage(), e);
						}
						request.setContent(requestContent.toByteArray());
						request.setContentType(multipartRequestEntity.getContentType());
						value = request;
					} else {
						MockHttpServletRequest request = new MockHttpServletRequest();
						value = request;
					}
				} else {
					value = properties.getProperty(property + ".param" + i + ".value");
					if (value == null) {
						String filename = properties.getProperty(property + ".param" + i + ".valuefile.absolutepath");
						if (filename != null) {
							value = readFile(filename);
						} else {
							String inputStreamFilename = properties
									.getProperty(property + ".param" + i + ".valuefileinputstream.absolutepath");
							if (inputStreamFilename != null) {
								try {
									value = new FileInputStream(inputStreamFilename);
								} catch (FileNotFoundException e) {
									logger.error("Could not read file '" + inputStreamFilename + "': " + e.getMessage(), e);
								}
							}
						}
					}
				}
				if (value != null && value instanceof String) {
					if ("node".equals(properties.getProperty(property + ".param" + i + ".type"))) {
						try {
							value = XmlUtils.buildNode((String) value, true);
						} catch (DomBuilderException e) {
							logger.error("Could not build node for parameter '" + name + "' with value: " + value, e);
						}
					} else if ("domdoc".equals(properties.getProperty(property + ".param" + i + ".type"))) {
						try {
							value = XmlUtils.buildDomDocument((String) value, true);
						} catch (DomBuilderException e) {
							logger.error("Could not build node for parameter '" + name + "' with value: " + value, e);
						}
					} else if ("list".equals(properties.getProperty(property + ".param" + i + ".type"))) {
						List<String> parts = new ArrayList<String>(
								Arrays.asList(((String) value).split("\\s*(,\\s*)+")));
						List<String> list = new LinkedList<String>();
						for (String part : parts) {
							list.add(part);
						}
						value = list;
					} else if ("map".equals(properties.getProperty(property + ".param" + i + ".type"))) {
						List<String> parts = new ArrayList<String>(
								Arrays.asList(((String) value).split("\\s*(,\\s*)+")));
						Map<String, String> map = new LinkedHashMap<String, String>();
						for (String part : parts) {
							String[] splitted = part.split("\\s*(=\\s*)+", 2);
							if (splitted.length == 2) {
								map.put(splitted[0], splitted[1]);
							} else {
								map.put(splitted[0], "");
							}
						}
						value = map;
					}
				}
				if (createParameterObjects) {
					String pattern = properties.getProperty(property + ".param" + i + ".pattern");
					if (value == null && pattern == null) {
						logger.error("Property '" + property + ".param" + i + " doesn't have a value or pattern");
					} else {
						try {
							Parameter parameter = new Parameter();
							parameter.setName(name);
							if (value != null && !(value instanceof String)) {
								parameter.setSessionKey(name);
								parameterResolutionContext.getSession().put(name, value);
							} else {
								parameter.setValue((String) value);
								parameter.setPattern(pattern);
							}
							parameter.configure();
							result.put(name, parameter);
							logger.debug("Add param with name '" + name + "', value '" + value
									+ "' and pattern '" + pattern + "' for property '" + property + "'");
						} catch (ConfigurationException e) {
							logger.error("Parameter '" + name + "' could not be configured");
						}
					}
				} else {
					if (value == null) {
						logger.error("Property '" + property + ".param" + i + ".value' or '" + property
								+ ".param" + i + ".valuefile' or '" + property + ".param" + i
								+ ".valuefileinputstream' not found while property '" + property + ".param" + i
								+ ".name' exist");
					} else {
						result.put(name, value);
						logger.debug("Add param with name '" + name + "' and value '" + value
								+ "' for property '" + property + "'");
					}
				}
				i++;
			} else {
				processed = true;
			}
		}
		return result;
	}
}
