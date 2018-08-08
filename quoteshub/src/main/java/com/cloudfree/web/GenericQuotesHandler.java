package com.cloudfree.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import com.cloudfree.AbstractQuotesSubscriber;

public class GenericQuotesHandler extends AbstractHandler {

	public static String type = "GenericQuotesHandler";
	protected AbstractQuotesSubscriber m_ActiveSubscriber = null;

	public GenericQuotesHandler(AbstractQuotesSubscriber s) {
		m_ActiveSubscriber = s;
	}

	public AbstractQuotesSubscriber GetActiveSubscriber() {
		return m_ActiveSubscriber;
	}

	public String GetType() {
		return type;
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

	}

}
