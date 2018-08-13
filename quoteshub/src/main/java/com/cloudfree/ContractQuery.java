package com.cloudfree;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ExtController;

public class ContractQuery {

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

	public ContractQuery SetHost(String host) {
		m_TWSHost = host;
		return this;
	}

	public ContractQuery SetPort(int port) {
		m_TWSPort = port;
		return this;
	}


	private class ConnectionHandler implements ExtController.IConnectionHandler {

		@Override
		public void accountList(List<String> arg0) {
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
			return 0;
		}

		@Override
		public int GetType() {
			return 0;
		}

		@Override
		public void SetReqId(int reqId) {

		}

	}

	public void DisableLog2Screen() {
		m_inLogger.SetLevel(GenLogger.FATAL);
		m_outLogger.SetLevel(GenLogger.FATAL);
	}
	public void StartExplorer() throws InterruptedException {

		if (m_Controller == null) {
			DisableLog2Screen();
			m_Controller = new ExtController(new ConnectionHandler(), m_inLogger, m_outLogger, null);
			
		}

		m_Controller.connect(m_TWSHost, m_TWSPort, m_ClientID, m_ConnectOptions);
		TimeUnit.SECONDS.sleep(3);
	}
	
	public void StopExplorer() {
		if (null != m_Controller) m_Controller.disconnect();
	}

	public void GetContracts(Contract con) {
		ContractDetailsHandler cdh = new ContractDetailsHandler();
		m_Controller.reqContractDetails(con, cdh);
	}

	public static void main(String[] args) throws InterruptedException {

		
		if (args.length < 1) {
			System.out.printf("Usage: java %s <ContractID> [TWSHost] [TWSPort]\n", ContractQuery.class.getName());
			System.out.printf("\tExample for JPY future: java %s JPY FUT 201809 0.009050", ContractQuery.class.getName());
			return;
		}

		ContractQuery ce = new ContractQuery();

		if (args.length > 1)
			ce.SetHost(args[1]);
		if (args.length > 2)
			ce.SetPort(Integer.parseInt(args[2]));

		Contract con = new Contract();
		con.conid(Integer.parseInt(args[0]));

		ce.StartExplorer();

		int i = 0;

		List<ContractDetails> cds;
		Set<Integer> ids = new HashSet<Integer>();
		ContractDetails cd0;

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
		ce.StopExplorer();
	}

}
