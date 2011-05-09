//
//  ProxyCacheHandler
//  Copyright 2004 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
//  Copyright 2011 by Florian Richter
//  First released 2011 at http://yacy.net
//  
//  $LastChangedDate$
//  $LastChangedRevision$
//  $LastChangedBy$
//
//  This library is free software; you can redistribute it and/or
//  modify it under the terms of the GNU Lesser General Public
//  License as published by the Free Software Foundation; either
//  version 2.1 of the License, or (at your option) any later version.
//  
//  This library is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//  Lesser General Public License for more details.
//  
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program in the file lgpl21.txt
//  If not, see <http://www.gnu.org/licenses/>.
//

package net.yacy.http;

import java.io.IOException;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.yacy.cora.protocol.RequestHeader;
import net.yacy.cora.protocol.ResponseHeader;
import net.yacy.kelondro.data.meta.DigestURI;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnection;
import org.eclipse.jetty.server.Request;

import de.anomic.crawler.retrieval.Response;
import de.anomic.http.client.Cache;

/**
 * jetty http handler
 * serves pages from cache if available and valid
 */
public class ProxyCacheHandler extends AbstractRemoteHandler implements Handler {

	
	private void handleRequestFromCache(HttpServletRequest request, HttpServletResponse response, ResponseHeader cachedResponseHeader, byte[] content) throws IOException {
		System.err.println("handle from cache");
		// TODO: check if-modified
		for(Entry<String, String> entry: cachedResponseHeader.entrySet()) {
			response.addHeader(entry.getKey(), entry.getValue());
		}
		response.setStatus(HttpServletResponse.SC_NON_AUTHORITATIVE_INFORMATION);
		response.getOutputStream().write(content);
        // we handled this request, break out of handler chain
		Request base_request = (request instanceof Request) ? (Request)request:HttpConnection.getCurrentConnection().getRequest();
		base_request.setHandled(true);
	}

	@Override
	public void handleRemote(String target, Request baseRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		if(request.getMethod().equals("GET")) {
			String queryString = request.getQueryString()!=null ? "?" + request.getQueryString() : "";
			DigestURI url = new DigestURI(request.getRequestURL().toString() + queryString);
			ResponseHeader cachedResponseHeader = Cache.getResponseHeader(url);
			
			if(cachedResponseHeader != null) {
				RequestHeader proxyHeaders = ProxyHandler.convertHeaderFromJetty(request);
				// TODO: this convertion is only necessary
            	final de.anomic.crawler.retrieval.Request yacyRequest = new de.anomic.crawler.retrieval.Request(
            			null, 
                        url, 
                        proxyHeaders.referer() == null ? null : new DigestURI(proxyHeaders.referer()).hash(), 
                        "", 
                        cachedResponseHeader.lastModified(),
                        sb.crawler.defaultProxyProfile.handle(),
                        0, 
                        0, 
                        0,
                        0);

                final Response cachedResponse = new Response(
                		yacyRequest,
                		proxyHeaders,
                        cachedResponseHeader,
                        "200 OK",
                        sb.crawler.defaultProxyProfile
                );
                byte[] cacheContent = Cache.getContent(url);
                if (cacheContent != null && cachedResponse.isFreshForProxy()) {
                	handleRequestFromCache(request, response, cachedResponseHeader, cacheContent);
                }
			}
			
		}
	}

}