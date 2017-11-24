package nl.nn.adapterframework.webcontrol;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.util.AppConstants;

public class RedirectIndexProxy extends HttpServlet {
	private static final long serialVersionUID = 1L;

	@Override
	public void init() throws ServletException {
		super.init();
	}

	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		ServletContext context = getServletContext();

		String indexLocation = AppConstants.getInstance().getProperty("webapp.index.location");
		if(indexLocation != null && !indexLocation.isEmpty()) {
			InputStream index = context.getResourceAsStream(indexLocation);

			if(index != null) {
				response.setDateHeader("Last-Modified", new Date().getTime());
				response.setHeader("Cache-Control", "no-cache, must-revalidate, max-age=60, post-check=0, pre-check=0");
				response.setHeader("Pragma", "no-cache");

				InputStreamReader fr = null;
				BufferedReader br = null;

				try {
					fr = new InputStreamReader(index);
					br = new BufferedReader(fr);

					String sCurrentLine;

					//Try to deduce base href
					String base = indexLocation;
					if(indexLocation.startsWith("/"))
						base = indexLocation.substring(1);
					base = base.substring(0, indexLocation.indexOf("index")-1);

					//Loop through all lines, replace where needed and print the response
					while ((sCurrentLine = br.readLine()) != null) {
						if(sCurrentLine.contains("<base") && sCurrentLine.contains("href=")) {
							sCurrentLine = sCurrentLine.trim();
							String match = "href=";
							int i = sCurrentLine.indexOf(match) + match.length();
							String quote = sCurrentLine.substring(i, i + 1);
							String preMatch = sCurrentLine.substring(0, i+1);
							String postMatch = sCurrentLine.substring(sCurrentLine.indexOf(quote, i+1));

							sCurrentLine = preMatch + base + postMatch;
						}
						if(sCurrentLine.contains("</head>")) {
							String path = request.getScheme() +"://"+ request.getServerName() +":"+ request.getServerPort() + request.getContextPath();
							response.getWriter().print("<script type=\"text/javascript\">var serverurl = '"+path+"';</script>");
						}
						response.getWriter().print(sCurrentLine);
					}

				} catch (IOException e) {
					response.sendError(500, "Unable to open index.html");
				} finally {
					try {
						if (br != null)
							br.close();
						if (fr != null)
							fr.close();
					} catch (IOException ex) {}
				}
			}
			else {
				response.sendError(500, "Unable to find index.html");
			}
		}
		else {
			response.sendRedirect("showConfigurationStatus.do");
		}
	}

	@Override
	public void destroy() {
		super.destroy();
	}
}
