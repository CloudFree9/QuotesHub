package com.cloudfree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.TickAttr;
import com.ib.client.TickType;
import com.ib.client.Types.SecType;
import com.ib.controller.ExtController;
import com.ib.controller.ExtController.IOptHandler;

public class ContractExplorer {

	private ExtController m_Controller;
	private String m_TWSHost = "127.0.0.1";
	private int m_TWSPort = 7496;
	private int m_ClientID = 20;
	private String m_ConnectOptions = "";
	private GenLogger m_inLogger = new GenLogger("IBQuotesProviderIn", GenLogger.INFO);
	private GenLogger m_outLogger = new GenLogger("IBQuotesProviderOut", GenLogger.INFO);

	public static Object m_Syncobj = new Object();
	private List<ContractDetails> m_List = null;

	public List<ContractDetails> GetContractDetailsList() {
		return m_List;
	}

	public ContractExplorer SetHost(String host) {
		m_TWSHost = host;
		return this;
	}

	public ContractExplorer SetPort(int port) {
		m_TWSPort = port;
		return this;
	}

	public class RealTimeTickDataHandler implements ExtController.IOptHandler {

		private int m_reqId = -1;
		private double bid = -1;
		private double ask = -1;
		private double price = -1;
		private Contract con = null;
		private double underprice = -1;

		public RealTimeTickDataHandler GetRealTimeTickHandler(Contract c) throws Exception {
			return new RealTimeTickDataHandler(c);
		}

		private RealTimeTickDataHandler(Contract c) {
			con = c;
		}

		@Override
		public void tickPrice(TickType tickType, double price, TickAttr attribs) {

			switch (tickType) {

			case BID:
				System.out.printf("Bid: %f\n", price);
				bid = price;
				break;
			case ASK:
				System.out.printf("Ask: %f\n", price);
				ask = price;
				break;
			case LAST:
			case CLOSE:
				System.out.printf("Last: %f\n", price);
				this.price = price;
				break;
			// case VOLUME:
			// System.out.printf("Voume: %f\n", price);
			// break;
			// case OPTION_IMPLIED_VOL:
			// System.out.printf("+++++++++++++++++ IV: %f\n", price);
			// break;
			default:
				System.out.printf("!!!!!!!!!!!!!!!!! Others: Type - %s, Price: %f, ignored\n", tickType.name(), price);
				return;
			}

			if ((tickType == TickType.BID || tickType == TickType.ASK) && bid > 0 && ask > 0 && bid <= ask
					&& underprice <= 0)
				price = (bid + ask) / 2;

			if (price >= 0 && underprice >= 0)
				m_Controller.reqOptionVolatility(con, price, underprice, this);
		}

		@Override
		public void tickSize(TickType tickType, int price) {
		}

		@Override
		public void tickString(TickType tickType, String value) {
		}

		@Override
		public void tickSnapshotEnd() {
		}

		@Override
		public void marketDataType(int marketDataType) {
		}

		@Override
		public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
		}

		@Override
		public int GetReqId() {
			return m_reqId;
		}

		@Override
		public int GetType() {
			return ExtController.ICommonHandler.REALTIMETICK;
		}

		@Override
		public void SetReqId(int reqId) {
			m_reqId = reqId;
		}

		@Override
		public void tickOptionComputation(TickType tickType, double impliedVol, double delta, double optPrice,
				double pvDividend, double gamma, double vega, double theta, double undPrice) {
			// TODO Auto-generated method stub

			// if (tickType == TickType.MODEL_OPTION || tickType == TickType.BID_OPTION ||
			// tickType == TickType.ASK_OPTION) {
			System.out.println(String.format("@@@@@@@@@@@@@@@@ Handled contract: %s %f %f %f %f %f %f %f %f", tickType,
					impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice));
			System.out.printf(
					"@@@@@@@@@@@@@@@@ Contract: %s, ReqId: %d, IV:%f, Delta:%f, optPrice: %f, underPrice: %f\n",
					con.localSymbol(), m_reqId, impliedVol, delta, optPrice, undPrice);
			price = optPrice;
			underprice = undPrice;
			// }
		}
	}

	private class ConnectionHandler implements ExtController.IConnectionHandler {

		@Override
		public void accountList(List<String> arg0) {
			// TODO Auto-generated method stub

		}

		@Override
		public void connected() {
			m_outLogger.log("IB TWS Connected.", GenLogger.INFO);
		}

		@Override
		public void disconnected() {
			m_outLogger.log("IB TWS Disconnected.", GenLogger.INFO);

		}

		@Override
		public void error(Exception arg0) {
			m_outLogger.log(arg0.getMessage(), GenLogger.ERROR);
		}

		@Override
		public void message(int arg0, int arg1, String arg2) {
			String msg = String.format("id:%d, code:%d, msg:%s", arg0, arg1, arg2);
			m_outLogger.log(msg, GenLogger.INFO);
		}

		@Override
		public void show(String arg0) {
			m_outLogger.log(arg0);
		}

		@Override
		public int GetReqId() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int GetType() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void SetReqId(int reqId) {
			// TODO Auto-generated method stub

		}

	}

	private class ContractDetailsHandler implements ExtController.IContractDetailsHandler {

		@Override
		public void contractDetails(List<ContractDetails> list) {

			synchronized (m_Syncobj) {
				m_List = list;
				m_Syncobj.notify();
			}
		}

		@Override
		public int GetReqId() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int GetType() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void SetReqId(int reqId) {
			// TODO Auto-generated method stub

		}

	}

	public void StartExplorer() {

		if (m_Controller == null) {
			m_Controller = new ExtController(new ConnectionHandler(), m_inLogger, m_outLogger, null);
		}

		m_Controller.connect(m_TWSHost, m_TWSPort, m_ClientID, m_ConnectOptions);
	}

	public void GetContracts(Contract con) {
		ContractDetailsHandler cdh = new ContractDetailsHandler();
		m_Controller.reqContractDetails(con, cdh);
	}

	public void GetRTTick(Contract con) throws Exception {
		IOptHandler h = new RealTimeTickDataHandler(con);
		m_Controller.reqOptionMktData(con, "106", false, false, h);
	}

	public static void main(String[] args) throws InterruptedException {

		if (args.length < 2) {
			System.out.printf("Usage: java %s <Symbol> <SecType> [TWSHost] [TWSPort]\n");
			System.out.printf("\tExample for mini S&P future: java %s ES FUT");
			return;
		}

		ContractExplorer ce = new ContractExplorer();

		if (args.length > 2)
			ce.SetHost(args[2]);
		if (args.length > 3)
			ce.SetPort(Integer.parseInt(args[3]));

		Contract con = new Contract();
		con.symbol("JPY");
		con.secType(SecType.FUT.name());
		con.currency("USD");
		con.lastTradeDateOrContractMonth("201809");

		ce.StartExplorer();

		int i = 0;

		List<ContractDetails> cds;
		Set<Integer> ids = new HashSet<Integer>();
		int parentconid = -1;

		ContractDetails cd0;
		String tradingcls = "";

		synchronized (ContractExplorer.m_Syncobj) {
			if (ce.GetContractDetailsList() != null)
				ce.GetContractDetailsList().clear();
			ce.GetContracts(con);
			ContractExplorer.m_Syncobj.wait(5000);
			cds = ce.GetContractDetailsList();
		}

		if (cds == null || cds.size() == 0) {
			System.out.printf("Nothing got, exit!\n");
			System.exit(1);
		}

		cd0 = cds.get(0);
		parentconid = cd0.conid();

		ids.add(cd0.conid());
		System.out.printf("************* Main contract %s *************\n", cd0.contract().symbol());
		System.out.printf("\tID : %d \n", cd0.contract().conid());
		System.out.printf("\tSymbol : %s \n", cd0.contract().symbol());
		System.out.printf("\tLocal Symbol : %s \n", cd0.contract().localSymbol());
		System.out.printf("\tDescription : %s \n", cd0.contract().description());
		System.out.printf("\tMarket Name : %s \n", cd0.marketName());
		System.out.printf("\tLast Trade : %s \n", cd0.contract().lastTradeDateOrContractMonth());
		System.out.printf("\tTrading Class : %s \n", cd0.contract().tradingClass());
		System.out.printf("\tCurrency : %s \n", cd0.contract().currency());
		System.out.printf("\tMultiplier : %s \n", cd0.contract().multiplier());
		System.out.printf("\tUnder : %s \n", cd0.underSymbol());
		System.out.printf("\tUnder id : %s \n", cd0.underConid());
		System.out.printf("\tContract Month : %s \n", cd0.contractMonth());
		System.out.println();

		TimeUnit.SECONDS.sleep(2);
		final int pcid = parentconid;

		con.secType(SecType.FOP.name());
		con.currency("USD");
		con.right("C");
		con.strike(0.009050);

		ContractDetails cd1;

		synchronized (ContractExplorer.m_Syncobj) {
			if (ce.GetContractDetailsList() != null)
				ce.GetContractDetailsList().clear();
			ce.GetContracts(con);
			ContractExplorer.m_Syncobj.wait(5000);
			cds = ce.GetContractDetailsList();
		}

		if (cds == null || cds.size() == 0) {
			System.out.printf("Nothing got, exit!\n");
			System.exit(1);
		}

		cd1 = cds.get(0);

		ids.add(cd1.conid());
		System.out.printf("************* Primary Monthly Option Contract %s *************\n", cd1.contract().symbol());
		System.out.printf("\tID : %d \n", cd1.contract().conid());
		System.out.printf("\tSymbol : %s \n", cd1.contract().symbol());
		System.out.printf("\tLocal Symbol : %s \n", cd1.contract().localSymbol());
		System.out.printf("\tDescription : %s \n", cd1.contract().description());
		System.out.printf("\tMarket Name : %s \n", cd1.marketName());
		System.out.printf("\tLast Trade : %s \n", cd1.contract().lastTradeDateOrContractMonth());
		System.out.printf("\tTrading Class : %s \n", cd1.contract().tradingClass());
		System.out.printf("\tCurrency : %s \n", cd1.contract().currency());
		System.out.printf("\tMultiplier : %s \n", cd1.contract().multiplier());
		System.out.printf("\tUnder : %s \n", cd1.underSymbol());
		System.out.printf("\tUnder id : %s \n", cd1.underConid());
		System.out.printf("\tContract Month : %s \n", cd1.contractMonth());
		System.out.println();

		TimeUnit.SECONDS.sleep(2);

		tradingcls = cd1.contract().tradingClass();
		final String tcls = tradingcls;

		con.secType(SecType.FOP.name());
		con.right("C");
		con.lastTradeDateOrContractMonth("");
		con.strike(0.009050);

		i = 0;

		synchronized (ContractExplorer.m_Syncobj) {
			if (ce.GetContractDetailsList() != null)
				ce.GetContractDetailsList().clear();
			ce.GetContracts(con);
			ContractExplorer.m_Syncobj.wait(5000);
			cds = ce.GetContractDetailsList();
		}

		TimeUnit.SECONDS.sleep(5);

		if (cds == null || cds.size() == 0) {
			System.err.printf("Trying No. %d failed...", i++);
			System.exit(2);
		} else {

			cds.stream().filter(e -> (e.underConid() == pcid && e.contract().tradingClass().equals(tcls)))
					.forEach(cd -> {
						System.out.printf("************* %s *************\n", cd.contract().symbol());
						System.out.printf("\tID : %d \n", cd.contract().conid());
						System.out.printf("\tSymbol : %s \n", cd.contract().symbol());
						System.out.printf("\tLocal Symbol : %s \n", cd.contract().localSymbol());
						System.out.printf("\tDescription : %s \n", cd.contract().description());
						System.out.printf("\tMarket Name : %s \n", cd.marketName());
						System.out.printf("\tLast Trade : %s \n", cd.contract().lastTradeDateOrContractMonth());
						System.out.printf("\tTrading Class : %s \n", cd.contract().tradingClass());
						System.out.printf("\tCurrency : %s \n", cd.contract().currency());
						System.out.printf("\tMultiplier : %s \n", cd.contract().multiplier());
						System.out.printf("\tUnder : %s \n", cd.underSymbol());
						System.out.printf("\tUnder id : %s \n", cd.underConid());
						System.out.printf("\tContract Month : %s \n", cd.contractMonth());
						System.out.println();

					});

		}

		System.out.println("That's it, bye!");
	}
}
