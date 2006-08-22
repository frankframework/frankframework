/*
 * $Log: DumpIbisConsole.java,v $
 * Revision 1.2  2006-08-22 11:57:57  europe\L190409
 * removed call tot response sink
 *
 * Revision 1.1  2006/08/22 07:51:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * first version
 *
 */

package nl.nn.adapterframework.webcontrol;

import nl.nn.adapterframework.util.AppConstants;
import javax.servlet.ServletContext;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.RequestDispatcher;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import org.apache.log4j.Logger;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

/**
 * Dumps the entire Ibis Console to a zip-file which can be saved.
 * 
 * This functionality is developed because developers don't have rigths for the Ibis Console in the IUFWeb environment.
 * 
 * @author  Peter Leeuwenburgh
 * @version Id
 */

public class DumpIbisConsole extends HttpServlet {
	private static String directoryName = "dump";
	private static Logger log = Logger.getLogger(DumpIbisConsole.class);
	private static ServletContext servletContext;
	private static Set set = new HashSet();
	private static Set setFileViewer = new HashSet();
	private static Set setShowAdapterStatistics = new HashSet();
	private static ZipOutputStream zipOutputStream;

	public void doGet(HttpServletRequest request, HttpServletResponse response)
		throws IOException {
		process(request, response);
	}

	public void init(ServletConfig config) {
		servletContext = config.getServletContext();
	}

	public static void copyResource(String resource) {
		try {
			String fileName = directoryName + resource;

			InputStream inputStream =
				servletContext.getResourceAsStream(resource);
			zipOutputStream.putNextEntry(new ZipEntry(fileName));
			for (int c = inputStream.read(); c != -1; c = inputStream.read()) {
				zipOutputStream.write(c);
			}
			zipOutputStream.closeEntry();
		} catch (Exception e) {
			log.error("Error copying resource", e);
		}
	}

	public static void copyServletResponse(HttpServletRequest request, HttpServletResponse response, String resource, String destinationFileName) {
		try {
			RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(resource);
			IbisHttpServletResponseWrapper ibisHttpServletResponseWrapper = new IbisHttpServletResponseWrapper(response);
			requestDispatcher.include(request, ibisHttpServletResponseWrapper);
			String htmlString = ibisHttpServletResponseWrapper.getStringWriter().toString();
			InputStream inputStream = new ByteArrayInputStream(htmlString.getBytes());

			zipOutputStream.putNextEntry(new ZipEntry(destinationFileName));
			// HttpServletResponseSink sink = new HttpServletResponseSink(response,zipOutputStream);
			
			for (int c = inputStream.read(); c != -1; c = inputStream.read()) {
				zipOutputStream.write(c);
			}
			zipOutputStream.closeEntry();

			if (!resource.startsWith("FileViewerServlet")) {
				extractResources(htmlString);
			}
			if (resource.equals("/showLogging.do")) {
				extractLogging(htmlString);
			}
			if (resource.equals("/showConfigurationStatus.do")) {
				extractStatistics(htmlString);
			}
		} catch (Exception e) {
			log.error("Error copying servletResponse", e);
		}
	}

	public static void extractLogging(String htmlString) {

		String hs = htmlString.toUpperCase();
		int p0 = hs.indexOf("<A ");
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
							if (s.startsWith("FileViewerServlet")) {
								setFileViewer.add(s);
							}
						}
					}
				}
			}
			p0 = hs.indexOf("<A ", p1);
		}
	}

	public static void extractResources(String htmlString) {

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
							set.add(s);
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
							set.add(s);
						}
					}
				}
			}
			p0 = hs.indexOf("<IMG ", p1);
		}
	}

	public static void extractStatistics(String htmlString) {

		String hs = htmlString.toUpperCase();
		int p0 = hs.indexOf("<A ");
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
							if (s.startsWith("showAdapterStatistics.do")) {
								setShowAdapterStatistics.add(s);
							}
						}
					}
				}
			}
			p0 = hs.indexOf("<A ", p1);
		}
	}

	public static void process(HttpServletRequest request, HttpServletResponse response) {
		try {
			File zipFile = File.createTempFile("dump", ".zip", new File(AppConstants.getInstance().getResolvedProperty("logging.path")));
			zipFile.deleteOnExit();
			zipOutputStream = new ZipOutputStream(new FileOutputStream(zipFile));

			copyServletResponse( request, response, "/showConfigurationStatus.do", directoryName + "/showConfigurationStatus.html");

			for (Iterator iterator = setShowAdapterStatistics.iterator();
				iterator.hasNext();
				) {
				String s = (String) iterator.next();
				int p1 = s.length();
				int p2 = s.indexOf("adapterName=");
				String fileName = s.substring(p2 + 12, p1);
				File file = new File(fileName);
				copyServletResponse(request, response, "/" + s, directoryName + "/showAdapterStatistics_" + file.getName() + ".html");
			}

			copyServletResponse(request, response, "/showLogging.do", directoryName + "/showLogging.html");

			for (Iterator iterator = setFileViewer.iterator();iterator.hasNext();) {
				String s = (String) iterator.next();
				if (s.indexOf("resultType=text") >= 0 && s.indexOf("log4j=true") < 0) {
					int p1 = s.length();
					int p2 = s.indexOf("fileName=");
					String fileName = s.substring(p2 + 9, p1);
					File file = new File(fileName);
					copyServletResponse(request, response, "/" + s, directoryName + "/log/" + file.getName());
				}
			}

			copyServletResponse(request, response, "/showEnvironmentVariables.do", directoryName + "/showEnvironmentVariables.html");
			copyServletResponse(request, response, "/showConfiguration.do", directoryName + "/showConfiguration.html");
			copyServletResponse(request, response, "/showSchedulerStatus.do", directoryName + "/showSchedulerStatus.html");

			for (Iterator iterator = set.iterator(); iterator.hasNext();) {
				copyResource("/" + iterator.next());
			}

			zipOutputStream.close();

			FileInputStream in = new FileInputStream(zipFile);
			OutputStream out = response.getOutputStream();
			response.setContentType("application/x-zip-compressed");
			int c;
			while ((c = in.read()) != -1)
				out.write(c);
			in.close();
			zipFile.delete();

		} catch (Exception e) {
			log.error("Error dumping ibis console", e);
		}
	}
}
