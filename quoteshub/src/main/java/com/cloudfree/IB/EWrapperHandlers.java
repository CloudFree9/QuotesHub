package com.cloudfree.IB;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;

import org.bson.Document;

import com.cloudfree.GenLogger;
import com.cloudfree.MongoQuotesStore;
import com.cloudfree.IB.OptionChain.OptContract;
import com.cloudfree.IB.OptionChain.OptionMap;
import com.ib.client.ContractDetails;
import com.ib.client.TickAttr;
import com.ib.client.TickType;
import com.ib.client.Types.BarSize;
import com.ib.client.Types.DurationUnit;
import com.ib.controller.Bar;
import com.ib.controller.ExtController;
import com.ib.controller.ExtController.IConnectionHandler;

public class EWrapperHandlers {

	public static class Tick {
		public TickType tickType;
		public double price;
		public TickAttr attribs;
		public long time;

		public Tick(TickType type, double p, TickAttr attr, long ts) {
			tickType = type;
			price = p;
			attribs = attr;
			time = ts;
		}
	}

	public static class OptionComputeHandler implements ExtController.IOptHandler {

		private OptContract m_OptContract;

		public OptionComputeHandler(OptContract optcon) {
			m_OptContract = optcon;
		}

		@Override
		public void tickPrice(TickType tickType, double price, TickAttr attribs) {
			// TODO Auto-generated method stub

		}

		@Override
		public void tickSize(TickType tickType, int size) {
			// TODO Auto-generated method stub

		}

		@Override
		public void tickString(TickType tickType, String value) {
			// TODO Auto-generated method stub

		}

		@Override
		public void tickSnapshotEnd() {
			// TODO Auto-generated method stub

		}

		@Override
		public void marketDataType(int marketDataType) {
			// TODO Auto-generated method stub

		}

		@Override
		public void tickReqParams(int tickerId, double minTick, String bboExchange, int snapshotPermissions) {
			// TODO Auto-generated method stub

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

		@Override
		public void tickOptionComputation(TickType tickType, double impliedVol, double delta, double optPrice,
				double pvDividend, double gamma, double vega, double theta, double undPrice) {
			// TODO Auto-generated method stub

			// System.out.printf("$$$$ IV:%.2f $$$$ \n", impliedVol * 100);

			synchronized (m_OptContract) {
				m_OptContract.Volatility(impliedVol);
				m_OptContract.Delta(delta);
				m_OptContract.Gamma(gamma);
				m_OptContract.Vega(vega);
				m_OptContract.Theta(theta);
				IBContract con = m_OptContract.GetUnderContract();
				synchronized (con.m_VIXSync) {
					con.m_VIXSync.notifyAll();
				}
			}

		}

	}

	public static class ContractDetailsHandler implements ExtController.IContractDetailsHandler {

		public final Object m_SyncObj = new Object();
		private List<ContractDetails> m_List = null;
		// private static final ContractDetailsHandler m_Handler = new
		// ContractDetailsHandler();

		public static ContractDetailsHandler GetInstance() {
			return new ContractDetailsHandler();
		}

		private ContractDetailsHandler() {

		}

		public void Clean() {
			if (m_List != null)
				m_List.clear();
		}

		public List<ContractDetails> GetContractDetailsList() {
			return m_List;
		}

		@Override
		public void contractDetails(List<ContractDetails> list) {

			System.out.println("Contract Detail list got");
			synchronized (m_SyncObj) {
				m_List = list;
				m_SyncObj.notify();
			}
		}

		@Override
		public int GetReqId() {
			return ExtController.ICommonHandler.NONE;
		}

		@Override
		public int GetType() {
			return ExtController.ICommonHandler.NONE;
		}

		@Override
		public void SetReqId(int reqId) {
		}

	}

	public static class RealTimeBarHandler implements ExtController.IRealTimeBarHandler {

		public final Object m_Syncobj = new Object();
		public final IBContract m_VContract;
		private Bar m_Bar;
		private int m_reqId = -1;
		public int m_Count = 0;
		public long m_LastTimeStamp = -1;

		public static RealTimeBarHandler GetRealTimeHandler(IBContract vc) throws Exception {

			RealTimeBarHandler m_Handler = null;

			if (vc == null) {
				throw new Exception("Null VContract object passed to RealTimeBarHandler");
			}

			m_Handler = new RealTimeBarHandler(vc);
			return m_Handler;
		}

		private RealTimeBarHandler(IBContract vc) {
			m_VContract = vc;
		}

		public synchronized Bar GetBar() {
			return m_Bar;
		}

		@Override
		public int GetReqId() {
			return m_reqId;
		}

		@Override
		public void realtimeBar(Bar bar) {

			synchronized (m_Syncobj) {
				System.out.printf("Bar of %s | time:%s, open:%f, high:%f, low:%f, close:%f, volume:%d\n", 
						m_VContract.m_RealContract.localSymbol(), 
						bar.formattedTime(),
						bar.open(), 
						bar.high(), 
						bar.low(), 
						bar.close(), 
						bar.volume());

				m_Bar = bar;
				double orig_price = m_VContract.m_RecentPrice;
				
				m_VContract.PutBar(REALTIMEBAR, bar);
		        MongoQuotesStore IBQuotesdb = (MongoQuotesStore)(m_VContract.GetSubscriber().GetProvider()).GetPersistenceStore();

		        String collection = String.format("%s_%s_%d_%s", 
		        		m_VContract.m_RealContract.symbol(),
		        		m_VContract.m_RealContract.lastTradeDateOrContractMonth(),
		        		m_VContract.m_RealContract.conid(),
		        		m_VContract.m_RealContract.getSecType()
		        		);
		        
		        try {
					IBQuotesdb.Put(collection, new Document()
							.append("ts", bar.time() * 1000)
							.append("price", bar.close()));
				} catch (Exception e) {
					e.printStackTrace();
				}
		        
				synchronized (m_VContract) {
					m_VContract.SetPrice(bar.close());
				}

				if (bar.close() != orig_price) {
	
					synchronized (m_VContract.m_WatchSync) {
						m_VContract.m_WatchSync.notifyAll();
					}

				}

				m_Count++;
				m_LastTimeStamp = bar.time();

				m_Syncobj.notifyAll();
			}
		}

		public int GetBarsCount() {
			return m_Count;
		}

		@Override
		public int GetType() {
			return ExtController.ICommonHandler.REALTIMEBAR;
		}

		@Override
		public void SetReqId(int reqId) {
			// TODO Auto-generated method stub
			m_reqId = reqId;
		}

	}

	public static class RTBarOptHandler extends RealTimeBarHandler {
		
		private RTTickOptHandler m_VolComputer = null; 
		private double m_UndPrice = 0.0;
		
		public static RTBarOptHandler GetRTBarOptHandler(OptionChain.OptContract c) {

			RTBarOptHandler inst = null;
			try {
				inst = new RTBarOptHandler(c);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return inst;
		}

		private RTBarOptHandler(OptionChain.OptContract c) throws Exception {
			super(c);
			m_VolComputer = RTTickOptHandler.GetRTOptHandler(c);
		}
		
		public RTTickOptHandler GetVolComputer() {
			return m_VolComputer;
		}
		
		@Override
		public void realtimeBar(Bar bar) {

			synchronized (m_Syncobj) {
				System.out.printf("Option Bar %s | time:%s, open:%f, high:%f, low:%f, close:%f, volume:%d\n", m_VContract.GetRealContract().localSymbol(), bar.formattedTime(),
						bar.open(), bar.high(), bar.low(), bar.close(), bar.volume());

/*				
		        MongoQuotesStore IBQuotesdb = (MongoQuotesStore)(m_VContract.GetSubscriber().GetProvider()).GetPersistenceStore();

		        try {
					IBQuotesdb.Put("InstrumentBar", new Document()
							.append("contractid", m_VContract.GetRealContract().conid())
							.append("instrument", m_VContract.GetRealContract().symbol() + " " + m_VContract.GetRealContract().lastTradeDateOrContractMonth())
							.append("timestamp", bar.time() * 1000)
							.append("type", m_VContract.GetRealContract().getSecType())
							.append("price", bar.close()));
				} catch (Exception e) {
					e.printStackTrace();
				}
*/		        
				OptionChain.OptContract optCon = (OptionChain.OptContract) m_VContract;
				double undprice = optCon.GetUnderContract().GetPrice();

				if (bar.close() != m_VContract.m_RecentPrice || m_UndPrice != undprice) {

					synchronized (m_VContract) {
						m_VContract.SetPrice(bar.close());
					}

					optCon.RequestOptionVolatility(optCon, m_VContract.m_RecentPrice, undprice, m_VolComputer);
					m_UndPrice = undprice;
					m_Syncobj.notifyAll();
				}

			}
		}
	}
	
	public static class HistoricalDataHandler implements ExtController.IHistoricalDataHandler {

		public final Object m_Syncobj = new Object();
		private final IBContract m_VContract;
		private int m_reqId;
		private int m_Count = 0;
		private DurationUnit m_Du = DurationUnit.DAY;
		private int m_Howmany = 100;
		private BarSize m_BarSize = BarSize._1_hour;
		private String m_EndDate = new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(new Date());

		public HistoricalDataHandler SetEndDate(String dt) {
			m_EndDate = dt;
			return this;
		}

		public String GetEndDate() {
			return m_EndDate;
		}

		public HistoricalDataHandler SetDurationUnit(DurationUnit du) {
			m_Du = du;
			return this;
		}

		public DurationUnit GetDurationUnit() {
			return m_Du;
		}

		public HistoricalDataHandler SetDurationQuantity(int q) {
			m_Howmany = q;
			return this;
		}

		public int GetDurationQuantity() {
			return m_Howmany;
		}

		public HistoricalDataHandler SetBarSize(BarSize bsz) {
			m_BarSize = bsz;
			return this;
		}

		public BarSize GetBarSize() {
			return m_BarSize;
		}

		public static HistoricalDataHandler GetHistoricalDataHandler(IBContract c) throws Exception {

			HistoricalDataHandler m_Handler = null;

			if (c == null) {
				throw new Exception() {

					private static final long serialVersionUID = 2610934613256157899L;

					@Override
					public String toString() {
						return "Null IBContract object passed to RealTimeBarHandler";
					}
				};
			}

			m_Handler = new HistoricalDataHandler(c);

			return m_Handler;
		}

		private HistoricalDataHandler(IBContract vc) {
			m_VContract = vc;
		}

		@Override
		public synchronized int GetReqId() {
			return m_reqId;
		}

		@Override
		public void historicalData(Bar bar) {
			// System.out.printf("Entering historicalData...\n");
			synchronized (m_Syncobj) {
				// System.out.printf("reqId %d\n", reqId);
				// System.out.printf("Bar | time:%s, open:%f, high:%f, low:%f, close:%f,
				// volume:%d\n", bar.formattedTime(), bar.open(), bar.high(), bar.low(),
				// bar.close(), bar.volume());
		        MongoQuotesStore IBQuotesdb = (MongoQuotesStore)(m_VContract.GetSubscriber().GetProvider()).GetPersistenceStore();

		        try {
					IBQuotesdb.Put("InstrumentBar", new Document()
							.append("contractid", m_VContract.GetRealContract().conid())
							.append("instrument", m_VContract.GetRealContract().symbol() + " " + m_VContract.GetRealContract().lastTradeDateOrContractMonth())
							.append("timestamp", bar.time() * 1000)
							.append("type", m_VContract.GetRealContract().getSecType())
							.append("price", bar.close()));
				} catch (Exception e) {
					e.printStackTrace();
				}

		        Queue<Bar> q = m_VContract.GetHistBarsQueue();
				synchronized (q) {
					q.add(bar);
					if (q.size() > 1000)
						q.remove(); // Keep the unhandled bars limited to 1000
					q.notifyAll();
				}
				m_Syncobj.notify();
			}
			m_Count++;

		}

		@Override
		public void historicalDataEnd() {
			// TODO Auto-generated method stub
			// System.out.printf("Historical bars for reqId %d done! Total %d bars
			// handled!\n", reqId, m_Count);
		}

		@Override
		public int GetType() {
			// TODO Auto-generated method stub
			return ExtController.ICommonHandler.HISTORICALBAR;
		}

		@Override
		public void SetReqId(int reqId) {
			// TODO Auto-generated method stub
			m_reqId = reqId;
		}

	}

	public static class ConnectionHandler implements IConnectionHandler {

		private final IBQuotesProvider m_Provider;
		private boolean m_Quit = false;

		public ConnectionHandler(IBQuotesProvider p) {
			m_Provider = p;
		}

		public static ConnectionHandler GetInstance(IBQuotesProvider p) {
			ConnectionHandler m_Handler = null;
			m_Handler = new ConnectionHandler(p);
			return m_Handler;
		}

		public ConnectionHandler SetQuitFlag() {
			m_Quit = true;
			return this;
		}

		public ConnectionHandler ClearQuitFlag() {
			m_Quit = false;
			return this;
		}

		@Override
		public void accountList(List<String> arg0) {
		}

		@Override
		public void connected() {
			GenLogger.DFT_LOGGER.log("IB TWS Connected.", GenLogger.INFO);
			synchronized (m_Provider.m_ConnectedSignal) {
				m_Provider.m_ConnectedSignal.notifyAll();
			}
		}

		@Override
		public void disconnected() {
			GenLogger.DFT_LOGGER.log("IB TWS Disconnected.", GenLogger.INFO);

		}

		@Override
		public void error(Exception arg0) {
			GenLogger.DFT_LOGGER.log("Error: " + arg0.getMessage(), GenLogger.ERROR);

			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// m_Provider.SetLastErrorCode(Integer.MAX_VALUE);
			// m_Provider.m_RestartSignal = true;
			System.exit(1);
		}

		@Override
		public void message(int arg0, int arg1, String arg2) {

			String msg = String.format("IB Connection message : ID:%d, Code:%d, Msg:%s", arg0, arg1, arg2);
			GenLogger.DFT_LOGGER.log(msg, GenLogger.INFO);

			if (arg1 == 507 || arg1 == 504 || arg1 == 100) {
				m_Provider.SetLastErrorCode(arg1);
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				GenLogger.DFT_LOGGER.log("TWS connection needs to be be restarted", GenLogger.INFO);

				// m_Provider.m_RestartSignal = true;
				System.exit(2);
			}
		}

		@Override
		public void show(String arg0) {
			GenLogger.DFT_LOGGER.log(arg0);
		}

		@Override
		public int GetReqId() {
			return ExtController.ICommonHandler.NONE;
		}

		@Override
		public int GetType() {
			return ExtController.ICommonHandler.NONE;
		}

		@Override
		public void SetReqId(int reqId) {
		}
	}

	public static class RTTickOptHandler extends RealTimeTickDataHandler implements ExtController.IOptHandler {

		public static RTTickOptHandler GetRTOptHandler(OptionChain.OptContract c) {

			RTTickOptHandler inst = null;
			try {
				inst = new RTTickOptHandler(c);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return inst;
		}

		private RTTickOptHandler(OptionChain.OptContract c) throws Exception {
			super(c);
		}

		@Override
		public void tickPrice(TickType tickType, double price, TickAttr attribs) {

			switch (tickType) {

			case BID:
//			case BID_OPTION:
				System.out.printf("Bid: %f\n", price);
				if (price == m_VContract.m_RecentBid) return;
				m_VContract.Bid(price);
				break;
			case ASK:
//			case ASK_OPTION:
				System.out.printf("Ask: %f\n", price);
				if (price == m_VContract.m_RecentAsk) return;
				m_VContract.Ask(price);
				break;
			case LAST:
//			case LAST_OPTION:
			case CLOSE:
				System.out.printf("Last: %f\n", price);
				m_VContract.SetPrice(price);
				break;
			default:
				System.out.printf("Others Tick: Type - %s, Price: %f, ignored\n", tickType.name(), price);
				return;
			}

			if (m_VContract.m_RecentBid <= m_VContract.m_RecentPrice && m_VContract.m_RecentAsk >= m_VContract.m_RecentPrice
					&& (tickType == com.ib.client.TickType.BID || tickType == com.ib.client.TickType.ASK)) {
				return;
			}

			if (m_VContract.m_RecentBid > m_VContract.m_RecentPrice || m_VContract.m_RecentAsk < m_VContract.m_RecentPrice) {
				if (tickType == com.ib.client.TickType.LAST || tickType == com.ib.client.TickType.CLOSE) {
					if (m_VContract.m_RecentBid > m_VContract.m_RecentPrice)  
						m_VContract.m_RecentBid = m_VContract.m_RecentPrice;
					
					if (m_VContract.m_RecentAsk < m_VContract.m_RecentPrice)  
						m_VContract.m_RecentAsk = m_VContract.m_RecentPrice;
				} else {
					m_VContract.m_RecentPrice = (m_VContract.m_RecentAsk + m_VContract.m_RecentBid) / 2;
				}
			}

			OptContract optCon = (OptContract) m_VContract;
			double undprice = optCon.GetUnderContract().GetPrice();

	        MongoQuotesStore IBQuotesdb = (MongoQuotesStore)(m_VContract.GetSubscriber().GetProvider()).GetPersistenceStore();

	        long ts = Calendar.getInstance(TimeZone.getTimeZone("US/Eastern")).getTimeInMillis();
	        
	        try {
	        	String cat = "";
	        	if (optCon.GetUnderContract().GetOptionChain().GetOptionMap().GetVIXWatchList(OptionMap.LOWER).contains(optCon)) {
	        		cat = "F";
	        	} else if (optCon.GetUnderContract().GetOptionChain().GetOptionMap().GetVIXWatchList(OptionMap.UPPER).contains(optCon)) {
	        		cat = "H";
	        	}
	        	
	        	String collection = String.format("VIXTick_%s_%s_%d",
	        			optCon.GetUnderContract().GetRealContract().symbol(),
	        			optCon.GetUnderContract().GetRealContract().lastTradeDateOrContractMonth(),
	        			optCon.GetUnderContract().GetRealContract().conid()
	        	);
	        	
	        	if (cat != "")
					IBQuotesdb.Put(collection, new Document()
							.append("ts", ts)
							.append("cat", cat)
							.append("type", tickType.toString())
							.append("price", price));
	        	else
	        		System.out.printf("Oops, invalid HEAD/FOOT option contract %d\n", optCon.m_ConDetails.conid());
			} catch (Exception e) {
				e.printStackTrace();
			}

	        if (undprice <= 0)
				return;

//			optCon.RequestOptionVolatility(optCon, m_VContract.m_RecentPrice, undprice, this);

		}

		@Override
		public void tickOptionComputation(TickType tickType, double impliedVol, double delta, double optPrice,
				double pvDividend, double gamma, double vega, double theta, double undPrice) {

			if (optPrice > undPrice ||  tickType != TickType.MODEL_OPTION)
				return;

			System.out.printf("@@@@@@ Contract: %s, Type: %s, IV:%f, Delta:%f, optPrice: %f, underPrice: %f\n",
					m_VContract.GetRealContract().localSymbol(), tickType, impliedVol, delta, optPrice, undPrice);

			switch (tickType) {

//			case CUST_OPTION_COMPUTATION:
			case MODEL_OPTION:

				OptionChain.OptContract optcon = (OptionChain.OptContract) m_VContract;
				optcon.Volatility(impliedVol);
				optcon.Delta(delta);
				optcon.Gamma(gamma);
				optcon.Vega(vega);
				optcon.Theta(theta);
				break;
			case BID_OPTION:
				System.out.printf("Bid: %f\n", optPrice);
				if (optPrice == m_VContract.m_RecentBid) return;
				tickPrice(TickType.BID, optPrice, null);
				break;
			case ASK_OPTION:
				System.out.printf("Ask: %f\n", optPrice);
				if (optPrice == m_VContract.m_RecentAsk) return;
				tickPrice(TickType.ASK, optPrice, null);
				break;
			case LAST_OPTION:
				System.out.printf("LAST: %f\n", optPrice);
				if (optPrice == m_VContract.m_RecentAsk) return;
				tickPrice(TickType.LAST, optPrice, null);
				break;
			default:
				System.out.printf("Others Tick: Type - %s, Price: %f, ignored\n", tickType.name(), optPrice);
				return;
			}
	//		if (tickType != TickType.CUST_OPTION_COMPUTATION)
	//			tickPrice(tickType, optPrice, null);
			// Here need to notify VIX calculation and put the calc result as the recent tick of VIX to the VIX queue
		}

	}

	public static class RealTimeTickDataHandler implements ExtController.ITopMktDataHandler {

		public final Object m_Syncobj = new Object();
		protected IBContract m_VContract;
		protected int m_reqId = -1;

		public static RealTimeTickDataHandler GetRealTimeTickHandler(IBContract c) throws Exception {
			return new RealTimeTickDataHandler(c);
		}

		private RealTimeTickDataHandler(IBContract c) throws Exception {
			m_VContract = c;
		}

		@Override
		public void tickPrice(TickType tickType, double price, TickAttr attribs) {

			switch (tickType) {

			case BID:
//				System.out.printf("Bid: %f\n", price);
				if (price == m_VContract.m_RecentBid) return;
				m_VContract.Bid(price);
				if(price > m_VContract.m_RecentAsk) {
					m_VContract.Ask(price);
				}
				break;
			case ASK:
//				System.out.printf("Ask: %f\n", price);
				if (price == m_VContract.m_RecentAsk) return;
				m_VContract.Ask(price);
				if(price < m_VContract.m_RecentBid) {
					m_VContract.Bid(price);
				}
				break;
			case LAST:
			case CLOSE:
				System.out.printf("Last: %f\n", price);
				if (price == m_VContract.m_RecentPrice) return;
				break;
			default:
				System.out.printf("Others Tick: Type - %s, Price: %f, ignored\n", tickType.name(), price);
				return;
			}

	        MongoQuotesStore IBQuotesdb = (MongoQuotesStore)(m_VContract.GetSubscriber().GetProvider()).GetPersistenceStore();

	        long ts = Calendar.getInstance(TimeZone.getTimeZone("US/Eastern")).getTimeInMillis();
	        
	        try {
	        	
				IBQuotesdb.Put("InstrumentTick", new Document()
						.append("contractid", m_VContract.GetRealContract().conid())
						.append("instrument", m_VContract.GetRealContract().localSymbol() + " " + m_VContract.GetRealContract().lastTradeDateOrContractMonth())
						.append("timestamp", ts)
						.append("ticktype", tickType.toString())
						.append("price", price));
			} catch (Exception e) {
				e.printStackTrace();
			}

	        double c_price = m_VContract.m_RecentPrice;
			
			if ((tickType == com.ib.client.TickType.BID || tickType == com.ib.client.TickType.ASK) &&  (c_price < m_VContract.m_RecentBid || c_price > m_VContract.m_RecentAsk)) {
				c_price = (m_VContract.m_RecentBid + m_VContract.m_RecentAsk) / 2;
			}
			
			if (tickType == com.ib.client.TickType.LAST || tickType == com.ib.client.TickType.CLOSE) {
				c_price = price;
				if (c_price > m_VContract.m_RecentAsk) {
					m_VContract.m_RecentAsk = c_price;
				}
				if (c_price < m_VContract.m_RecentBid) {
					m_VContract.m_RecentBid = c_price;
				}
			}
			
//			if(m_VContract.m_RecentBid <0 || m_VContract.m_RecentAsk <0 || price <0) {
//				System.out.printf("Warn : negative tick price got: %f, ticktype: %s\n", price, tickType.toString());
//				return;
//			}
			
			m_VContract.SetPrice(c_price);

			m_VContract.PutTick(REALTIMETICK, new Tick(tickType, c_price, attribs, Calendar.getInstance().get(Calendar.MILLISECOND)));

			synchronized (m_VContract.m_PriceSync) {
				m_VContract.m_PriceSync.notifyAll();
			}
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
	}
}
