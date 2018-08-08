package com.cloudfree.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.cloudfree.WebApp;
import com.cloudfree.IB.IBContract;
import com.cloudfree.IB.IBQuotesSubscriber;

public class IBQuotesHandler extends GenericQuotesHandler {

	public static class Functions {

		public static final String GETQUOTES = "quotes";
		public static final String GETINST = "geticontract";
	}

	public static String type = "IBQuotesHandler";
	public Object m_Syncobj = new Object();

	private HashSet<String> m_FunctionMap = new HashSet<String>() {

		private static final long serialVersionUID = 3005611065796060039L;

		{
			add(Functions.GETQUOTES);
			add(Functions.GETINST);
		}
	};

	public IBQuotesHandler(IBQuotesSubscriber s) {
		super(s);
	}

	@Override
	public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {

		response.setContentType("text/html; charset=utf-8");
		response.setStatus(HttpServletResponse.SC_OK);
		PrintWriter out = response.getWriter();

		String t = target.substring(1).toLowerCase();

		if (!m_FunctionMap.contains(t)) {
			out.printf("{'res':'error', 'reason': 'unknown request. Check if the correct handler is set.'}");
			baseRequest.setHandled(true);
			return;
		}

		try {
			switch (t) {
			case Functions.GETQUOTES:
				RequestQuotes(target, baseRequest, request, response);
				break;
			case Functions.GETINST:
				GetInstruments(target, baseRequest, request, response);
				break;
			default:
				out.printf("{'res':'error', 'reason': 'undefined request.'}\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		baseRequest.setHandled(true);
	}

	private synchronized void GetInstruments(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		String inst = baseRequest.getParameter("instruments");
		if (inst == null)
			inst = "";

		PrintWriter out = response.getWriter();

		String insts[] = inst.split(",");
		IBQuotesSubscriber s = (IBQuotesSubscriber) m_ActiveSubscriber;
		List<ContractDetails> contracts = new LinkedList<ContractDetails>();

		for (int j = 0; j < insts.length; j++) {

			int conid;

			if (!insts[j].matches("^\\d+$")) {
				continue;
			}

			conid = Integer.parseInt(insts[j]);
			Contract con = new Contract();
			con.conid(conid);

			List<ContractDetails> cds = s.GetContractDetails(con);
			if (cds == null || cds.size() == 0)
				continue;
			contracts.addAll(cds);
		}

		out.println("<pre>[");
		String prefix = "";
		for (ContractDetails cd : contracts) {
			out.println(prefix);
			out.println("\t{");
			out.printf("\t\t\"id\" : \"%d\", \n", cd.contract().conid());
			out.printf("\t\t\"symbol\" : \"%s\", \n", cd.contract().symbol());
			out.printf("\t\t\"localsymbol\" : \"%s\", \n", cd.contract().localSymbol());
			out.printf("\t\t\"description\" : \"%s\", \n", cd.contract().description());
			out.printf("\t\t\"marketname\" : \"%s\", \n", cd.marketName());
			out.printf("\t\t\"lasttrade\" : \"%s\", \n", cd.contract().lastTradeDateOrContractMonth());
			out.printf("\t\t\"tradingclass\" : \"%s\", \n", cd.contract().tradingClass());
			out.printf("\t\t\"currency\" : \"%s\", \n", cd.contract().currency());
			out.printf("\t\t\"multiplier\" : \"%s\" \n", cd.contract().multiplier());
			out.printf("\t\t\"secid\" : \"%s\" \n", cd.contract().secId());
			out.printf("\t\t\"secidtype\" : \"%s\" \n", cd.contract().getSecIdType());
			out.printf("\t\t\"right\" : \"%s\" \n", cd.contract().getRight());
			out.printf("\t\t\"sectype\" : \"%s\" \n", cd.contract().getSecType());
			out.printf("\t}");
			prefix = ",";
		}

		out.println("\n]</pre>");
	}

	private void RequestQuotes(String target, Request baseRequest, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		String instrument = baseRequest.getParameter("instrument");

		PrintWriter out = response.getWriter();

		if (instrument == null || instrument.equals("")) {
			out.printf("{\"res\":\"error\", \"reason\": \"unknown instrument\"}");
			baseRequest.setHandled(true);
			return;
		}

		IBQuotesSubscriber s = (IBQuotesSubscriber) m_ActiveSubscriber;

		Map<String, String> conMap = WebApp.globalContracts.stream().filter(e -> e.get("ContractID").equals(instrument))
				.findFirst().orElse(null);

		String id = null;
		String digits = null;

		if (null != conMap) {
			id = conMap.get("ContractID");
			digits = conMap.get("Digits");
		}

		boolean vix = false;
		if (null != id && id.startsWith("VIX@")) {
			vix = true;
			id = id.substring(4).trim();
		}

		IBContract ibcon = null;
		if (null != id && id.matches("^\\d+$")) {
			ibcon = s.FindIBContractByID(Integer.parseInt(id));
		}

		if (ibcon == null) {
			out.printf("{\"res\":\"error\", \"reason\": \"unknown instrument\"}");
			return;
		}

		if (null == digits || !digits.matches("^\\d+$")) {
			digits = "4";
		}

		String format = String.format("%%.%sf", digits);
		String returnpage;

		if (vix) {
			returnpage = String.format("{\"res\":\"ok\", \"timestamp\":\"%d\", \"price\":\"" + format + "\"}",
					System.currentTimeMillis(), ibcon.GetVIX());
		} else {
			returnpage = String.format(
					"{\"res\":\"ok\", \"timestamp\":\"%d\", \"price\": \"" + format + "\", \"vix\": \"%.4f\"}",
					System.currentTimeMillis(), ibcon.GetPrice(), ibcon.GetVIX());
		}

		out.println(returnpage);
		baseRequest.setHandled(true);
	}
}
