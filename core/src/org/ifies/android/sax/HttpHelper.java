/*
 *	This file is part of Transdroid Torrent Search 
 *	<http://code.google.com/p/transdroid-search/>
 *	
 *	Transdroid Torrent Search is free software: you can redistribute 
 *	it and/or modify it under the terms of the GNU Lesser General 
 *	Public License as published by the Free Software Foundation, 
 *	either version 3 of the License, or (at your option) any later 
 *	version.
 *	
 *	Transdroid Torrent Search is distributed in the hope that it will 
 *	be useful, but WITHOUT ANY WARRANTY; without even the implied 
 *	warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
 *	See the GNU Lesser General Public License for more details.
 *	
 *	You should have received a copy of the GNU Lesser General Public 
 *	License along with Transdroid.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.ifies.android.sax;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.zip.GZIPInputStream;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.protocol.HttpContext;

/**
 * Provides a set of general helper methods that can be used in web-based communication.
 * 
 * @author erickok
 *
 */
public class HttpHelper {

	/**
	 * HTTP request interceptor to allow for GZip-encoded data transfer 
	 */
	public static HttpRequestInterceptor gzipRequestInterceptor = new HttpRequestInterceptor() {
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            if (!request.containsHeader("Accept-Encoding")) {
                request.addHeader("Accept-Encoding", "gzip");
            }
        }
    };
    
    /**
     * HTTP response interceptor that decodes GZipped data
     */
    public static HttpResponseInterceptor gzipResponseInterceptor = new HttpResponseInterceptor() {
        public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
            HttpEntity entity = response.getEntity();
            Header ceheader = entity.getContentEncoding();
            if (ceheader != null) {
                HeaderElement[] codecs = ceheader.getElements();
                for (int i = 0; i < codecs.length; i++) {
                	
                    if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                        response.setEntity(new HttpHelper.GzipDecompressingEntity(response.getEntity())); 
                        return;
                    }
                }
            }
        }
        
    };

    /**
     * HTTP entity wrapper to decompress GZipped HTTP responses
     */
	private static class GzipDecompressingEntity extends HttpEntityWrapper {
	
	    public GzipDecompressingEntity(final HttpEntity entity) {
	        super(entity);
	    }
	
	    @Override
	    public InputStream getContent() throws IOException, IllegalStateException {
	
	        // the wrapped entity's getContent() decides about repeatability
	        InputStream wrappedin = wrappedEntity.getContent();
	
	        return new GZIPInputStream(wrappedin);
	    }
	
	    @Override
	    public long getContentLength() {
	        // length of ungzipped content is not known
	        return -1;
	    }
	
	}

    /*
     * To convert the InputStream to String we use the BufferedReader.readLine()
     * method. We iterate until the BufferedReader return null which means
     * there's no more data to read. Each line will appended to a StringBuilder
     * and returned as String.
     * 
     * Taken from http://senior.ceng.metu.edu.tr/2009/praeda/2009/01/11/a-simple-restful-client-at-android/
     */
    public static String ConvertStreamToString(InputStream is, String encoding) throws UnsupportedEncodingException {
    	InputStreamReader isr;
    	if (encoding != null) {
    		isr = new InputStreamReader(is, encoding);
    	} else {
    		isr = new InputStreamReader(is);
    	}
    	BufferedReader reader = new BufferedReader(isr);
        StringBuilder sb = new StringBuilder();
 
        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
    
    public static String ConvertStreamToString(InputStream is) {
    	try {
			return ConvertStreamToString(is, null);
		} catch (UnsupportedEncodingException e) {
			// Since this is going to use the default encoding, it is never going to crash on an UnsupportedEncodingException
			e.printStackTrace();
			return null;
		}
    }
    
}