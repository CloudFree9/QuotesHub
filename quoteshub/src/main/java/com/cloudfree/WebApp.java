package com.cloudfree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import com.cloudfree.IB.IBQuotesProvider;
import com.cloudfree.IB.IBQuotesSubscriber;
import com.cloudfree.web.IBQuotesHandler;
import com.ib.controller.ExtController.ICommonHandler;

public class WebApp {

	public static final String CONTEXTPATH = "/ibquotes";
	private static Server server;

	private static GenQuotesHub hub;
	public final static Set<Map<String, String>> globalContracts = new HashSet<>();

	public static CommandLine GetOpitions(String[] args) {

		Options options = new Options();
		CommandLine cmd;

		Option pconf = Option.builder("p").longOpt("provider-conf").desc("specify the provider configuration file")
				.hasArg().required().build();

		options.addOption(pconf);

		options.addOption(Option.builder("l").longOpt("listen-port").desc("specify the port on which jetty listens")
				.hasArg().required().build());

		options.addOption(Option.builder("c").longOpt("contracts-conf")
				.desc("specify the to be preloaded contracts config file").hasArg().build());

		try {
			cmd = new DefaultParser().parse(options, args);
		} catch (ParseException e) {
			GenLogger.DFT_LOGGER.log(e.toString(), GenLogger.ERROR);
			GenLogger.DFT_LOGGER.log(
					"Valid options: -p,--provider-conf <conf file> -l,--listen-port <port> -c,--contracts-conf <preloaded contract configuration file>");
			return null;
		}

		return cmd;
	}

	public static Set<Integer> GetPreloadContracts(String contractsfile) throws Exception {

		File f = new File(contractsfile);
		if (!f.exists() || !f.isFile())
			return null;

		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(contractsfile)));
		String txt = br.lines().reduce((a, s) -> {
			return a + s;
		}).orElse(null);
		br.close();

		Set<Integer> res = new HashSet<>();
		JSONArray contracts = (JSONArray) ((JSONObject) JSONValue.parse(txt)).get("ConfigSymbols");

		for (Object o : contracts) {
			JSONObject c = (JSONObject) o;
			Map<String, String> con = new HashMap<>();
			con.put("Symbol", (String) c.get("Symbol"));
			con.put("ContractID", (String) c.get("ContractID"));
			con.put("Description", (String) c.get("Description"));
			con.put("Digits", (String) c.get("Digits"));
			globalContracts.add(con);
			res.add(Integer.parseInt((String) c.get("ContractID")));
		}

		return res;
	}

	public static void QuotesHubProvision(String paramFile, String contractsfile) {

		IBQuotesProvider provider = new IBQuotesProvider("IB Quotes Provider", paramFile);
		IBQuotesSubscriber subscriber = new IBQuotesSubscriber("IB Quotes", provider);

		hub = GenQuotesHub.GetInstance().RegisterProvider(provider);
		hub.Start();

		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}

		Set<Integer> cons = null;
		try {
			cons = GetPreloadContracts(contractsfile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (cons != null && cons.size() > 0) {
			for (int conid : cons) {
				subscriber.Subscribe(ICommonHandler.REALTIMEBAR, conid);
			}
			new Thread() {
				@Override
				public void run() {
					Thread.currentThread().setName("Enable All VIX while web server is started.");
					subscriber.EnableAllVIX();
				}
			}.start();
		}

		ContextHandler context = new ContextHandler();
		context.setContextPath(CONTEXTPATH);
		context.setHandler(new IBQuotesHandler(subscriber));

		server.setHandler(context);
	}

	public static void main(String[] args) {

		CommandLine cmd = GetOpitions(args);
		String paramFile = cmd.getOptionValue('p');
		int lPort = Integer.parseInt(cmd.getOptionValue('l'));
		String preloadContracts = cmd.getOptionValue('c');

		server = new Server(lPort);

		try {
			QuotesHubProvision(paramFile, preloadContracts);
			server.start();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
