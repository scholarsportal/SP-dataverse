
package edu.harvard.iq.dataverse.export;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
@WebServlet(description = "Load data from webpage", urlPatterns = { "/ReadFile" })
public class ReadFile  extends HttpServlet{
        static InputStream inputStream;
    
   	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	Map<String, String> map =handleRequest(request, response);
           response.addHeader("Access-Control-Allow-Origin", "*");
           StringBuilder sb = new StringBuilder();
                String myURL = map.get("url")+"?key="+map.get("key")+"&variables="+map.get("variables");
		URLConnection urlConn = null;
		InputStreamReader in = null;
		try {
			URL url = new URL(myURL);
			urlConn = url.openConnection();
			if (urlConn != null)
				urlConn.setReadTimeout(60 * 1000);
			if (urlConn != null && urlConn.getInputStream() != null) {
				in = new InputStreamReader(urlConn.getInputStream(), Charset.defaultCharset());
                            try (BufferedReader bufferedReader = new BufferedReader(in)) {
                                int cp;
                                while ((cp = bufferedReader.read()) != -1) {
                                    sb.append((char) cp);
                                }
                            }
			}
			in.close();
		} catch (IOException e) {
			throw new RuntimeException("Exception while calling URL:" + myURL, e);
		}
                response.getWriter().println( sb.toString());
     }
        

	//handel url parameters
	public static Map<String, String> handleRequest(HttpServletRequest req, HttpServletResponse res) throws IOException{	
               Map<String, String> map = new HashMap<String, String>();
	       res.setContentType("text/plain");
	       Enumeration<String> parameterNames = req.getParameterNames();
	       while (parameterNames.hasMoreElements()) {
	           String paramName = parameterNames.nextElement();
	           String[] paramValues = req.getParameterValues(paramName);		           
	             map.put(paramName,paramValues[0]);//note all url parameters are assumed to be arrays so setting 
	       }
	      return map;
	  }
		
}