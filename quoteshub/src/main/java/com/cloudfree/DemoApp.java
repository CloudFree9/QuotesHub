package com.cloudfree;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.cloudfree.IB.IBContract;
import com.cloudfree.IB.IBQuotesProvider;
import com.cloudfree.IB.IBQuotesSubscriber;
import com.cloudfree.IB.OptionChain;
// import com.cloudfree.mt5.UDPDest;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;

public class DemoApp {

	public static GenLogger logger;
	public static Object SyncObj = new Object();

	public static void main(String[] args) throws Exception {

		try {
			InetAddress a1 = InetAddress.getLocalHost();
			InetAddress a2 = InetAddress.getLocalHost();

			System.out.println(a1.hashCode());
			System.out.println(a2.hashCode());

			if (a1.equals(a2) && a1.hashCode() == a2.hashCode()) {
				System.out.println("They are the same!");
			} else {
				System.out.println("They are different!");
			}
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}

		logger = new GenLogger(DemoApp.class, GenLogger.INFO);

		Options options = new Options();
		CommandLine cmd = null;

		Option pconf = Option.builder("p").longOpt("provider-conf").desc("specify the provider configuration file")
				.hasArg().required().build();

		options.addOption(pconf);

		options.addOption(Option.builder("h").longOpt("mt5-host").desc("specify the host on which MT5 terminal runs")
				.hasArg().required().build());

		options.addOption(Option.builder("P").longOpt("mt5-port").desc("specify the port on which MT5 terminal listen")
				.hasArg().required().build());

		try {
			cmd = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			logger.log(e.toString(), GenLogger.ERROR);
			logger.log("Valid options: -p,--provider-conf <conf file> -h,--mt5-host <hostname> -P,--mt5-port <port>");
			return;
		}

		String paramFile = cmd.getOptionValue('p');
		// String mt5host = cmd.getOptionValue('h');
		// int mt5port = Integer.parseInt(cmd.getOptionValue('P'));

		logger.log(String.format("The config file is : %s", paramFile));

		IBQuotesProvider p = new IBQuotesProvider("IB Quotes Provider", paramFile);
		p.DumpConfig();

		// UDPDest ud = new UDPDest(mt5host, mt5port);
		// DummyDestination dd = new DummyDestination();

		IBQuotesSubscriber s = new IBQuotesSubscriber("IB Quotes Subscriber", p);

		// s.BindProvider(p);
		HashSet<AbstractQuotesProvider> Providers = new HashSet<AbstractQuotesProvider>();
		Providers.add(p);

		GenQuotesHub GenHub = GenQuotesHub.GetInstance().RegisterProvider(p);

		GenHub.Start();

		Thread.sleep(3000);

		Calendar c = Calendar.getInstance();
		c.set(2016, 1, 31);
		System.out.printf("Year: %d, Month: %d, Day: %d   %d\n", c.get(Calendar.YEAR), c.get(Calendar.MONTH),
				c.get(Calendar.DATE), 10 % 3);

		Contract con = new Contract();
		// con.secId("134471782");
		con.symbol("JPY");
		con.currency("USD");
		con.secType("FUT");
		con.exchange("GLOBEX");
		con.lastTradeDateOrContractMonth("201809");

		List<ContractDetails> cds = s.GetContractDetails(con);

		OptionChain oc = new OptionChain(s, new IBContract(cds.get(0).contract(), s));

		try {
			oc.RebuildMaps();
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		oc.Dump();
		// con.symbol("ZB");
		// con.secType("FOP");
		// con.currency("USD");
		// con.lastTradeDateOrContractMonth("20180622");
		// con.right("C");
		// con.strike(143);
		// con.multiplier("1000");
		/*
		 * con.symbol("ZB"); con.secType("FUT"); con.currency("USD"); //
		 * con.tradingClass("OZB"); con.lastTradeDateOrContractMonth("201812");
		 */

		if (cds != null) {
			for (ContractDetails cd : cds) {

				System.out.printf("============= Contract : %s =============\n", cd.longName());
				System.out.printf("\t\t\"id\" : \"%d\", \n", cd.contract().conid());
				System.out.printf("\t\t\"symbol\" : \"%s\", \n", cd.contract().symbol());
				System.out.printf("\t\t\"localsymbol\" : \"%s\", \n", cd.contract().localSymbol());
				System.out.printf("\t\t\"description\" : \"%s\", \n", cd.contract().description());
				System.out.printf("\t\t\"marketname\" : \"%s\", \n", cd.marketName());
				System.out.printf("\t\t\"lasttrade\" : \"%s\", \n", cd.contract().lastTradeDateOrContractMonth());
				System.out.printf("\t\t\"tradingclass\" : \"%s\", \n", cd.contract().tradingClass());
				System.out.printf("\t\t\"currency\" : \"%s\", \n", cd.contract().currency());
				System.out.printf("\t\t\"multiplier\" : \"%s\" \n", cd.contract().multiplier());
				System.out.printf("\t\t\"secid\" : \"%s\" \n", cd.contract().secId());
				System.out.printf("\t\t\"secidtype\" : \"%s\" \n", cd.contract().getSecIdType());
				System.out.printf("\t\t\"right\" : \"%s\" \n", cd.contract().getRight());
				System.out.printf("\t\t\"strike\" : \"%s\" \n", cd.contract().strike());
				System.out.printf("\t\t\"sectype\" : \"%s\" \n", cd.contract().getSecType());
				System.out.printf("\t\t\"exchange\" : \"%s\" \n", cd.contract().exchange());
				System.out.printf("\t\t\"tradingclass\" : \"%s\" \n", cd.contract().tradingClass());
				System.out.printf("\n");
				// IBContract icon = new IBContract(cd.contract());
				// IBContract icon = new IBContract(con);

				// s.Subscribe(icon, ud);

			}
		}
		/*
		 * con.conid(cds.get(0).contract().conid());
		 * 
		 * 
		 * IBContract c = new IBContract(con);
		 * 
		 * try { EWrapperHandlers.HistoricalDataHandler h =
		 * EWrapperHandlers.HistoricalDataHandler.GetHistoricalDataHandler(s)
		 * .SetDurationQuantity(5) .SetDurationUnit(DurationUnit.DAY)
		 * .SetBarSize(BarSize._1_min) .SetEndDate("20180430 00:00:00");
		 * EWrapperHandlers.RealTimeBarHandler h1 =
		 * EWrapperHandlers.RealTimeBarHandler.GetRealTimeHandler(s);
		 * EWrapperHandlers.TopMktDataHandler h2 =
		 * EWrapperHandlers.TopMktDataHandler.GetInstance(s);
		 * 
		 * s.Subscribe(c, dd, h1); s.Subscribe(c, dd, h); s.Subscribe(c, dd, h2);
		 * Thread.sleep(1000); s.CancelHistoricalBars(h); s.CancelRealTimeBars(h1);
		 * s.CancelRealTimeTicks(h2); } catch (Exception e1) { // TODO Auto-generated
		 * catch block e1.printStackTrace(); }
		 */
		/*
		 * try { HashMap<String, HashMap<Double, List<Contract>>> chain =
		 * s.GetOptionChain(con, "FOP"); List<String> dates = new LinkedList<String>();
		 * dates.addAll(chain.keySet());
		 * 
		 * Collections.sort(dates);
		 * 
		 * Iterator<String> it = dates.iterator();
		 * 
		 * System.out.printf(">>>> Total: %d strike dates  <<<<\n", dates.size()); while
		 * (it.hasNext()) { System.out.printf("Strike date: %s\n", it.next()); } } catch
		 * (Exception e1) { // TODO Auto-generated catch block e1.printStackTrace(); }
		 */

		// p.controller().reqPositions(p.controller().GetPositionHandler());

		// GenLogger.DFT_LOGGER.log("Before execution");
		// p.controller().client().reqIds(0);
		// p.controller().client().reqIds(1000);
		// GenLogger.DFT_LOGGER.log("After execution");

		// int tickerId = 1000001;//System.currentTimeMillis()/1000;
		// p.controller().client().reqMktData(tickerId, con, "", false, false, null);
		// p.controller().client().reqAllOpenOrders();

		// s.RequestRealTimeBars();
		// p.RequestTick();
		/*
		 * try {
		 * p.controller().cancelTopMktData(EWrapperHandlers.RealTimeTickDataHandler.
		 * GetRealTimeTickHandler(s)); } catch (Exception e) { // TODO Auto-generated
		 * catch block e.printStackTrace(); }
		 */
		synchronized (GenHub) {
			GenHub.wait();
		}
	}

}
