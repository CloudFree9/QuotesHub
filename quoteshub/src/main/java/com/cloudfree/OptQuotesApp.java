package com.cloudfree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
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
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ExtController;
import com.ib.controller.ExtController.ICommonHandler;

public class OptQuotesApp {

	private static GenQuotesHub hub;
	public final static Set<Map<String, String>> globalContracts = new HashSet<>();
	public static Object m_Syncobj = new Object();

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

	public static void QuotesHubProvision(String paramFile, int conid) {

		IBQuotesProvider provider = new IBQuotesProvider("IB Quotes Provider", paramFile);
		IBQuotesSubscriber subscriber = new IBQuotesSubscriber("IB Quotes", provider);

		hub = GenQuotesHub.GetInstance().RegisterProvider(provider);
		hub.Start();

		try {
			TimeUnit.SECONDS.sleep(3);
		} catch (InterruptedException e2) {
			e2.printStackTrace();
		}

		subscriber.Subscribe(ICommonHandler.REALTIMEBAR, conid);

	}

	public static void main(String[] args) {

		CommandLine cmd = GetOpitions(args);
		String paramFile = cmd.getOptionValue('p');
		int lPort = Integer.parseInt(cmd.getOptionValue('l'));
		String preloadContracts = cmd.getOptionValue('c');

		try {
			QuotesHubProvision(paramFile, 288762153);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
