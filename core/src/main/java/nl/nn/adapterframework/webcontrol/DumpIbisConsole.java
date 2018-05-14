/*
   Copyright 2013 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Dumps the entire Ibis-Console to a zip-file which can be saved.
 * 
 * This functionality is developed because developers don't have rigths for the Ibis Console in the IUFWeb environment.
 * 
 * @author  Peter Leeuwenburgh
 */
public class DumpIbisConsole extends HttpServlet {
	private Logger log = LogUtil.getLogger(this);

	private String directoryName = "";
	private ServletContext servletContext;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws IOException {
		process(request, response);
	}

	public void init(ServletConfig config) {
		servletContext = config.getServletContext();
	}

	public void copyResource(ZipOutputStream zipOutputStream, String resource) {
		try {
			String rsc = resource;
			String fileName = directoryName + rsc;

			InputStream inputStream = servletContext.getResourceAsStream(rsc);
			if (inputStream==null) {
				log.debug("did not find resource [" + rsc + "], try again with added preceding slash");
				rsc = '/'  + rsc;
				inputStream = servletContext.getResourceAsStream(rsc);
			}
			if (inputStream==null) {
				log.warn("did not find resource [" + rsc + "]");
			} else
			{
				zipOutputStream.putNextEntry(new ZipEntry(fileName));
				Misc.streamToStream(inputStream,zipOutputStream);
				zipOutputStream.closeEntry();
				log.debug("copied resource [" + rsc + "]");
			}
		} catch (Exception e) {
			log.error("Error copying resource", e);
		}
	}

	public void copyResource(ZipOutputStream zipOutputStream, String resourceName, String resourceContents) {
		try {
			String fileName = directoryName + resourceName;

			PrintWriter pw = new PrintWriter(new OutputStreamWriter(zipOutputStream,Misc.DEFAULT_INPUT_STREAM_ENCODING));
			zipOutputStream.putNextEntry(new ZipEntry(fileName));
			pw.print(resourceContents);
			pw.flush();
			zipOutputStream.closeEntry();
			log.debug("copied resource [" + resourceName + "]");
		} catch (Exception e) {
			log.error("Error copying resource", e);
		}
	}

	public void copyServletResponse(ZipOutputStream zipOutputStream, HttpServletRequest request, HttpServletResponse response, String resource,
		String destinationFileName, Set resources, Set linkSet, String linkFilter) {
		long timeStart = new Date().getTime();
		try {
			IbisServletResponseWrapper ibisHttpServletResponseGrabber =  new IbisServletResponseWrapper(response);
			RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(resource);
			requestDispatcher.include(request, ibisHttpServletResponseGrabber);
			String htmlString = ibisHttpServletResponseGrabber.getStringWriter().toString();

			ZipEntry zipEntry=new ZipEntry(destinationFileName);
//			if (resourceModificationTime!=0) {
//				zipEntry.setTime(resourceModificationTime);
//			}

			zipOutputStream.putNextEntry(zipEntry);
			
			PrintWriter pw = new PrintWriter(zipOutputStream);
			pw.print(htmlString);
			pw.flush();
			zipOutputStream.closeEntry();

			if (!resource.startsWith("FileViewerServlet")) {
				extractResources(resources, htmlString);
			}
			if (linkSet!=null) {
				followLinks(linkSet, htmlString, linkFilter);
			}
		} catch (Exception e) {
			log.error("Error copying servletResponse", e);
		}
		long timeEnd = new Date().getTime();
		log.debug("dumped file [" + destinationFileName + "] in " + (timeEnd - timeStart) + " msec.");
	}

	public void followLinks(Set resourceSet, String htmlString, String linkStartFilter) {
		String hs = htmlString.toUpperCase();
		int posLinkStart = hs.indexOf("<A ");
		while (posLinkStart >= 0) {
			int posLinkStartClose = hs.indexOf(">", posLinkStart);
			if (posLinkStartClose > posLinkStart) {
				int posHref = hs.indexOf(" HREF", posLinkStart);
				if (posHref > posLinkStart && posHref < posLinkStartClose) {
					int posHrefContentStart = hs.indexOf("\"", posHref);
					if (posHrefContentStart > posHref && posHrefContentStart < posLinkStartClose) {
						posHrefContentStart++;
						int posHrefContentEnd = hs.indexOf("\"", posHrefContentStart);
						if (posHrefContentEnd > posHrefContentStart && posHrefContentEnd < posLinkStartClose) {
							String s = htmlString.substring(posHrefContentStart, posHrefContentEnd);
							if (StringUtils.isEmpty(linkStartFilter) || s.startsWith(linkStartFilter)) {
								resourceSet.add(s);
							}
						}
					}
				}
			}
			posLinkStart = hs.indexOf("<A ", posLinkStartClose);
		}
	}

	public void extractResources(Set resources, String htmlString) {
		String hs = htmlString.toUpperCase();
		int p0 = hs.indexOf("<LINK ");
		while (p0 >= 0) {
			int p1 = hs.indexOf(">", p0);
			if (p1 > p0) {
				int p2 = hs.indexOf(" HREF", p0);
				if (p2 > p0 && p2 < p1) {
					int p3 = hs.indexOf("\"", p2);
					if (p3 > p2 && p3 < p1) {
						p3++;
						int p4 = hs.indexOf("\"", p3);
						if (p4 > p3 && p4 < p1) {
							String s = htmlString.substring(p3, p4);
							resources.add(s);
						}
					}
				}
			}
			p0 = hs.indexOf("<LINK ", p1);
		}

		p0 = hs.indexOf("<IMG ");
		while (p0 >= 0) {
			int p1 = hs.indexOf(">", p0);
			if (p1 > p0) {
				int p2 = hs.indexOf(" SRC", p0);
				if (p2 > p0 && p2 < p1) {
					int p3 = hs.indexOf("\"", p2);
					if (p3 > p2 && p3 < p1) {
						p3++;
						int p4 = hs.indexOf("\"", p3);
						if (p4 > p3 && p4 < p1) {
							String s = htmlString.substring(p3, p4);
							resources.add(s);
						}
					}
				}
			}
			p0 = hs.indexOf("<IMG ", p1);
		}
	}


	public void process(HttpServletRequest request, HttpServletResponse response) {
		try {
			Set resources = new HashSet();
			Set setFileViewer = new HashSet();
			Set setShowAdapterStatistics = new HashSet();

			ZipOutputStream zipOutputStream = StreamUtil.openZipDownload(response,"IbisConsoleDump-"+AppConstants.getInstance().getProperty("instance.name","")+"-"+Misc.getHostname()+".zip");

			copyServletResponse(zipOutputStream, request, response, "/showConfigurationStatus.do", directoryName + "showConfigurationStatus.html", resources, setShowAdapterStatistics, "showAdapterStatistics.do");

			for (Iterator iterator = setShowAdapterStatistics.iterator();
				iterator.hasNext();
				) {
				String s = (String) iterator.next();
				int p1 = s.length();
				int p2 = s.indexOf("adapterName=");
				String fileName = s.substring(p2 + 12, p1);
				File file = new File(fileName);
				copyServletResponse(zipOutputStream, request, response, "/" + s, directoryName + "showAdapterStatistics_" + file.getName() + ".html", resources, null,null);
			}

			copyServletResponse(zipOutputStream, request, response, "/showLogging.do", directoryName + "showLogging.html", resources, setFileViewer, "FileViewerServlet");

			// find filename of all log files
			String ibisLogFileStart = AppConstants.getInstance().getResolvedProperty("instance.name.lc");
			String sysLogFileStart = "System"; //SystemOut and SystemErr
			String garbageCollectionLogFileStart = "GClog";

			for (Iterator iterator = setFileViewer.iterator();iterator.hasNext();) {
				String s = (String) iterator.next();
				if (s.indexOf("resultType=bin") >= 0) {
					int p1 = s.length();
					int p2 = s.indexOf("fileName=");
					String fileName = s.substring(p2 + 9, p1);
					int p3 = fileName.indexOf("&amp;");
					if (p3 >= 0) {
						fileName = fileName.substring(0, p3);
					}
					File file = new File(fileName);
					String fn = file.getName();
					if (fn.startsWith(ibisLogFileStart) || 
						fn.startsWith(sysLogFileStart) ||
						fn.startsWith(garbageCollectionLogFileStart)) {
						// best solution would be to stream files binary, but because log files are not binary reading as text also works
						s = StringUtils.replace(s, "resultType=bin", "resultType=text");
						s = StringUtils.replace(s, "&amp;", "&");
						if (fn.startsWith(garbageCollectionLogFileStart)) {
							// rename garbage collection log files so there are ordered correctly 
							fn = garbageCollectionLogFileStart + ".log." + DateUtils.format(new Date(file.lastModified()), "yyyyMMddHHmmss");
						}
						copyServletResponse(zipOutputStream, request, response, "/" + s, directoryName + "log/" + fn, resources, null,null);
					}
				}
			}

			copyServletResponse(zipOutputStream, request, response, "/showEnvironmentVariables.do", directoryName + "showEnvironmentVariables.html", resources, null,null);
			copyServletResponse(zipOutputStream, request, response, "/showConfiguration.do", directoryName + "showConfiguration.html", resources, null,null);
			copyServletResponse(zipOutputStream, request, response, "/showSchedulerStatus.do", directoryName + "showSchedulerStatus.html", resources, null,null);
			copyServletResponse(zipOutputStream, request, response, "/showMonitors.do", directoryName + "showMonitors.html", resources, null,null);

			for (Iterator iterator = resources.iterator(); iterator.hasNext();) {
				copyResource(zipOutputStream, (String)iterator.next());
			}

			//copyResource(zipOutputStream,"statistics.xml", configuration.getStatisticsOfZo());
			zipOutputStream.close();

		} catch (Exception e) {
			log.error("Error dumping ibis console", e);
		}
	}
}
