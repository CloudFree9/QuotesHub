package com.cloudfree;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.cloudfree.IB.IBContract;
import com.cloudfree.IB.IBQuotesProvider;
import com.cloudfree.IB.IBQuotesSubscriber;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ExtController.ICommonHandler;

public class OptionChainTest {

	public static GenLogger logger;
	public static Object SyncObj = new Object();

	public static void main(String[] args) throws Exception {

		logger = new GenLogger(OptionChainTest.class, GenLogger.INFO);

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

		logger.log(String.format("The config file is : %s", paramFile));

		IBQuotesProvider p = new IBQuotesProvider("IB Quotes Provider", paramFile);
		IBQuotesSubscriber s = new IBQuotesSubscriber("IB Quotes Subscriber", p);

		GenQuotesHub GenHub = GenQuotesHub.GetInstance().RegisterProvider(p);

		GenHub.Start();

		Contract con = new Contract();
		con.symbol("JPY");
		con.currency("USD");
		con.secType("FUT");
		con.exchange("GLOBEX");
		con.lastTradeDateOrContractMonth("201809");

		List<ContractDetails> cds = s.GetContractDetails(con);

		if (cds.size() < 1) {
			System.out.printf("The contract is invalid, please check!\n");
			System.exit(1);
		}

		con = cds.get(0).contract();

		IBContract ibcon = new IBContract(con, s);

		s.Subscribe(ICommonHandler.REALTIMEBAR, ibcon);
//		s.Subscribe(ICommonHandler.REALTIMETICK, ibcon);

		s.EnableVIX(ibcon);

		// OptionChain oc = new OptionChain(s, con);
		// oc.GetOptionMap().AddWatchList(20180629, 0.009200, null);
		// oc.GetOptionMap().AddVIXWatchList();
		// oc.Dump();
		// oc.StartVIX();
		/*
		 * new Thread() {
		 * 
		 * @Override public void run() { while(true) { try { Thread.sleep(3000);
		 * 
		 * HashSet<OptContract> wlist = oc.GetOptionMap().GetWatchList();
		 * synchronized(wlist) { for (OptContract ocon: wlist) {
		 * System.out.printf("Contract:%s, IV:%.2f%%, Underlying Price: %f\n",
		 * ocon.m_Contract.contract().localSymbol(), ocon.m_Volatility * 100,
		 * oc.GetOptionMap().GetUnderlyingPrice()); } } } catch (InterruptedException e)
		 * { e.printStackTrace(); }
		 * 
		 * } } }.start();
		 */
	}
}
