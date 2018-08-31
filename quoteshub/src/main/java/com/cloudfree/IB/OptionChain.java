package com.cloudfree.IB;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bson.Document;

import com.cloudfree.MongoQuotesStore;
import com.cloudfree.IB.EWrapperHandlers.OptionComputeHandler;
import com.cloudfree.IB.EWrapperHandlers.RTBarOptHandler;
import com.cloudfree.IB.EWrapperHandlers.RTTickOptHandler;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Types;
import com.ib.client.Types.Right;
import com.ib.client.Types.SecType;
import com.ib.controller.ExtController;
import com.ib.controller.ExtController.ICommonHandler;

public class OptionChain {

	public static class OptContract extends IBContract {

		private static final long serialVersionUID = 202308023907668114L;
		final private IBContract m_UnderContract;

		public double m_Volatility = 0.0;
		public double m_Delta = 0.0;
		public double m_Gamma = 0.0;
		public double m_Theta = 0.0;
		public double m_Vega = 0.0;
		
		private RTTickOptHandler m_TickOptHandler = RTTickOptHandler.GetRTOptHandler(this);

//		public final OptionComputeHandler m_OptComputeHandler = new OptionComputeHandler(this);

		public OptContract(Contract con, IBContract under) throws Exception {
			super(con, under.GetSubscriber());
			m_UnderContract = under;
			m_RealTimeBarHandler = RTBarOptHandler.GetRTBarOptHandler(this);
		}

		public IBContract GetUnderContract() {
			return m_UnderContract;
		}

		public OptContract RequestOptionVolatility(OptContract con, double price, double underprice,
				ExtController.IOptHandler h) {

			if (null != con && price > 0.0 && underprice > 0.0 && null != h) {
				((IBQuotesProvider)m_Subscriber.GetProvider())
						.controller()
						.reqOptionVolatility(con.GetRealContract(), price, underprice, h);
			} else {
				System.out.println("£¡£¡£¡ WARN: Invalid parameters passed to OptContract::RequestOptionVolatility.");				
			}
			return this;
		}

		@Override
		public OptContract Subscribe(int type) {

			if (m_Subscriber == null)
				return this;

			if (type == ICommonHandler.REALTIMETICK) {
				RequestRTOptTicks();
//				m_RealTimeTickHandler = m_TickOptHandler;
//				RequestRealTimeTicks();
			} else {
				super.Subscribe(type);
			}

			return this;
		}

		public OptContract Delta(double v) {
			m_Delta = v;
			return this;
		}

		public OptContract Gamma(double v) {
			m_Gamma = v;
			return this;
		}

		public OptContract Theta(double v) {
			m_Theta = v;
			return this;
		}

		public OptContract Vega(double v) {
			m_Vega = v;
			return this;
		}

		public OptContract Volatility(double v) {
			m_Volatility = v;
			return this;
		}

		public double Volatility() {
			return m_Volatility;
		}

		public OptContract RequestRTOptTicks() {
			if (m_Subscriber == null || m_TickOptHandler == null)
				return this;

			IBQuotesProvider p = (IBQuotesProvider) m_Subscriber.GetProvider();
			p.controller().reqOptionMktData(GetRealContract(), "106", false, false, m_TickOptHandler);
			return this;
		}
	}

	public class OptionMap implements Serializable {

		private static final long serialVersionUID = 7691859077493922115L;
		private final HashSet<OptContract> m_WatchList = new HashSet<>();

		private final ArrayList<HashSet<OptContract>> m_VIXWatchList = new ArrayList<HashSet<OptContract>>() {
			private static final long serialVersionUID = 1790902310221485406L;

			{
				add(new HashSet<OptContract>()); // Lower
				add(new HashSet<OptContract>()); // Higher
			}
		};

		private VIXWatchMonThread m_VIXWatchMonThread = new VIXWatchMonThread();
		private Thread m_WatchThread = null;

		public static final int UPPER = 1;
		public static final int LOWER = 0;
		public final HashMap<Long, HashMap<Double, HashMap<Types.Right, OptContract>>> m_Map = new HashMap<>();

		public OptionMap() throws Exception {

			if (m_MainContract == null) {
				throw new Exception("Main cotract for optionMap is null!");
			}

		}

		public void Refresh() {
			m_VIXWatchList
				.stream()
				.flatMap(e -> e.stream())
				.forEach(oc -> {
					try {
//						oc.UnSubscribe(ICommonHandler.REALTIMETICK);
//						TimeUnit.SECONDS.sleep(3);
						oc.Subscribe(ICommonHandler.REALTIMETICK);
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				});
		}

		public double GetUnderlyingPrice() {
			return m_MainContract.GetPrice();
		}

		public Set<OptContract> GetWatchList() {
			return m_WatchList;
		}

		public Set<OptContract> GetVIXWatchList() {
			return m_VIXWatchList.stream().flatMap(l -> l.stream()).collect(Collectors.toSet());
		}

		public Set<OptContract> GetVIXWatchList(int type) {
			if (type != UPPER && type != LOWER)
				return null;
			return m_VIXWatchList.get(type);
		}

		public OptionMap UpdateVIXWatchList() throws Exception {

			if (!m_MainContract.IsVixEnabled() || m_Map == null || m_Map.isEmpty())
				return this;

			double up_price = -1.0;
			double bot_price = -1.0;
			double u_price = -1.0;
			
			while (u_price <= 0) {
				u_price = GetUnderlyingPrice();
				
				// Wait until underlying instrument gets a valid price
				if (u_price <= 0) {
					System.out.printf(">>>> The valid underlying price of %s is still not received, ignored this round <<<\n", m_MainContract.GetRealContract().localSymbol());
					TimeUnit.SECONDS.sleep(2);
				}
			}
			
			long t_date = m_LastOptDate;
			String tdcls = m_MonthlyTradingCls;
			
			for (Long t_dt : m_Map.keySet()) {
				if (t_dt < t_date && m_Map.get(t_dt)
						.values()
						.stream()
						.limit(1)
						.flatMap(e -> e.values().stream())
						.limit(1)
						.anyMatch(e -> {
							return e.m_RealContract.tradingClass().equals(tdcls);
						})) {
					t_date = t_dt;
				}
			}
			
			/*
			 * Filter the whole option map with the option's contract month is the same as the underlying future.
			 */
			final HashMap<Double, HashMap<Types.Right, OptContract>> cons = m_Map.get(t_date);
			
			if (null == cons || cons.isEmpty())
				throw new Exception("No corresponding options found.");
			
			/*
			 * Get the strike price pair which embraces the current underlying price tightly
			 */
			List<Double> stirke_prices = cons.keySet().stream().collect(Collectors.toList());
			stirke_prices.sort(Double::compare);
			
			if (stirke_prices.size() < 2) 
				throw new Exception("Not enough options found.");
			
			for (int i=1; i<stirke_prices.size(); i++) {
				if (stirke_prices.get(i-1) <= u_price && u_price < stirke_prices.get(i)) {
					bot_price = stirke_prices.get(i-1);
					up_price = stirke_prices.get(i);
					break;
				}
			}

			if (bot_price == -1.0 || up_price == -1.0)
				throw new Exception("Error on getting UP || Down strike price.");

			// Update the up/bottom price to underlying contract data fields
			if (bot_price != m_MainContract.GetBotStrike() || up_price != m_MainContract.GetUpStrike()) {
				m_MainContract.SetBotStrike(bot_price);
				m_MainContract.SetUpStrike(up_price);
			} else {
				return this;
			}

			synchronized (m_VIXWatchList) {

				m_VIXWatchList
					.stream()
					.flatMap(l -> l.stream())
					.forEach(c -> {
//						c.UnSubscribe(ICommonHandler.REALTIMETICK);
						try {
							TimeUnit.SECONDS.sleep(3);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					});
				
				synchronized (m_WatchList) {
					m_VIXWatchList.get(UPPER).clear();
					m_VIXWatchList.get(LOWER).clear();
					m_VIXWatchList.get(LOWER).addAll(cons.get(bot_price).values());
					m_VIXWatchList.get(UPPER).addAll(cons.get(up_price).values());
				}

				m_VIXWatchList.stream().flatMap(s -> s.stream()).forEach(c -> {
					System.out.printf("Subscribe option quotes for %s %s", c.m_RealContract.localSymbol(),
							c.m_RealContract.lastTradeDateOrContractMonth());
					c.Subscribe(ICommonHandler.REALTIMETICK);
					try {
						TimeUnit.SECONDS.sleep(3);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				});

			}
			return this;
		}

		public OptionMap AddWatchList(Long date, Double strike, Types.Right t) {

			if (null == date || (null != date && !m_Map.containsKey(date)) || null == strike) {
				return this;
			}

			synchronized (m_WatchList) {

				m_Map.entrySet().stream().filter(e -> {
					return e.getKey() == date;
				}).flatMap(e0 -> e0.getValue().entrySet().stream()).filter(e1 -> {
					return e1.getKey() == strike;
				}).flatMap(e2 -> e2.getValue().entrySet().stream()).filter(e3 -> {
					if (null == t) return true;
					return e3.getKey().equals(t);
				}).map(e4 -> e4.getValue()).forEach(con -> {
					if (!m_WatchList.contains(con)) {
						m_WatchList.add(con);
						m_IBSubscriber.Subscribe(ICommonHandler.REALTIMETICK, con);
					}
				});

			}

			return this;
		}

		public OptionMap RemoveWatchList(Integer date, Double strike, Types.Right t) {

			synchronized (m_WatchList) {

				List<OptContract> temp = m_WatchList.stream().filter(opt -> {
					Contract con = opt.m_ConDetails.contract();
					boolean dt = true;
					boolean stk = true;
					boolean rt = true;
					
					if (null != date) dt = (con.lastTradeDateOrContractMonth().equals(date.toString()));
					if (null != strike) stk = (con.strike() == strike);
					if (null != t) rt = (con.right() == t);
					
					return !(dt && stk && rt);
				}).collect(Collectors.toList());

				m_WatchList.clear();
				m_WatchList.addAll(temp);
			}

			return this;
		}

		private class VIXWatchMonThread implements Runnable {

			@Override
			public void run() {

				Thread.currentThread().setName("VIX watch monitor");
				while (true) {
					try {
						UpdateVIXWatchList();
						synchronized (m_MainContract.m_WatchSync) {
							m_MainContract.m_WatchSync.wait();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

				}
			}
		}

		public void StartVIXWatch(boolean refresh) {

			if (m_WatchThread != null && m_WatchThread.isAlive())
				return;

			m_WatchThread = new Thread(m_VIXWatchMonThread);
			m_WatchThread.start();

			if (refresh) {
				synchronized (m_MainContract.m_WatchSync) {
					m_MainContract.m_WatchSync.notifyAll();
				}
			}
		}

		public void StopVIXWatch() {
			if (null == m_WatchThread|| !m_WatchThread.isAlive())
				return;
			m_WatchThread.interrupt();

			m_OptionMap.m_VIXWatchList.stream().flatMap(e -> e.stream()).distinct().forEach(vc -> {
				vc.UnSubscribe(ICommonHandler.REALTIMETICK);
			});

		}

	}

	final private IBContract m_MainContract; // underlying instrument of the options
	final private OptionMap m_OptionMap;
	final private IBQuotesSubscriber m_IBSubscriber;
	protected long m_LastOptDate = 0;
	protected String m_MonthlyTradingCls;

	private boolean m_VIXCalcStop = true;

	public OptionChain(IBQuotesSubscriber s, IBContract underlying) throws Exception {

		m_IBSubscriber = s;
		m_MainContract = underlying;
		m_OptionMap = new OptionMap();

		List<ContractDetails> cons = m_IBSubscriber.GetContractDetails(underlying.GetRealContract());
		ContractDetails cd = cons.size() > 0 ? cons.get(0) : null;
		
		if (null == cd)
			throw new Exception("Invalid main contract to retrieve the option chain.");

		m_MainContract.SetRealContract(cd.contract());
		m_MainContract.SetContractDetails(cd);

		RebuildMaps();
	}
	

	public double CalculateVIX() {

		Set<OptContract> bvixcons = m_OptionMap.GetVIXWatchList(OptionMap.LOWER);
		Set<OptContract> uvixcons = m_OptionMap.GetVIXWatchList(OptionMap.UPPER);

		double biv = 0.0, uiv = 0.0;

		synchronized (bvixcons) {
			biv = bvixcons.stream().filter((con) -> con.m_Volatility > 0)
					.collect(Collectors.averagingDouble(OptContract::Volatility));
		}

		synchronized (uvixcons) {
			uiv = uvixcons.stream().filter((con) -> con.m_Volatility > 0)
					.collect(Collectors.averagingDouble(OptContract::Volatility));
		}

		synchronized (m_MainContract.GetVIX()) {
			if (biv > 0 && uiv > 0) {
				double pricepos = (m_MainContract.GetUpStrike() - m_MainContract.GetPrice())
						/ (m_MainContract.GetUpStrike() - m_MainContract.GetBotStrike());
				m_MainContract.SetVIX(new Double((biv * (1 - pricepos) + uiv * pricepos) * 100));
			}
			return m_MainContract.GetVIX();
		}
	}
	

	public void RebuildMaps() throws Exception {

		synchronized (m_OptionMap) {

			List<ContractDetails> cons = null;
			int underconid = -1;

			try {
				Contract con = m_MainContract.GetRealContract();
				underconid = con.conid();
				Contract c = new Contract();
				c.symbol(con.symbol());
				c.currency(con.currency());
				c.exchange(con.exchange());
				c.lastTradeDateOrContractMonth(con.lastTradeDateOrContractMonth().substring(0, 6));

				if (con.secType() == SecType.FUT) {
					c.secType("FOP");
				} else {
					c.secType("OPT");
				}

				cons = m_IBSubscriber.GetContractDetails(c);

			} catch (Exception e) {
				e.printStackTrace();
			}

			HashMap<Long, HashMap<Double, HashMap<Types.Right, OptContract>>> m_Map = new HashMap<>();

			while (cons.size() > 0) {
				ContractDetails con = cons.remove(0);
				if (con.underConid() != underconid)
					continue;

				long ldate = Long.parseLong(con.contract().lastTradeDateOrContractMonth().substring(0, 8));

				if (!m_Map.containsKey(ldate)) {
					m_Map.put(ldate, new HashMap<Double, HashMap<Types.Right, OptContract>>());
				}

				HashMap<Double, HashMap<Types.Right, OptContract>> item = m_Map.get(ldate);

				Double strike = con.contract().strike();

				if (!item.containsKey(strike)) {
					item.put(strike, new HashMap<Types.Right, OptContract>());
				}

				HashMap<Types.Right, OptContract> opt = item.get(strike);

				OptContract optcontract = new OptContract(con.contract(), m_MainContract);
				optcontract.m_ConDetails = con;
				optcontract.SetContractDetails(con);
				opt.put(con.contract().right(), optcontract);
				
				long tradingend = Long.parseLong(optcontract.m_RealContract.lastTradeDateOrContractMonth());
				if (tradingend > m_LastOptDate) {
					m_LastOptDate = tradingend;
					m_MonthlyTradingCls = optcontract.m_RealContract.tradingClass();
				}
			}

			m_OptionMap.m_Map.clear();
			m_OptionMap.m_Map.putAll(m_Map);
			m_OptionMap.notifyAll();
		}
	}
	

	public void StartVIX() throws Exception {

		// Start monitor for VIX participants - selected option contracts, which are
		// dynamic with accordance to the underlying price change
		synchronized (m_OptionMap) {
			m_OptionMap.StartVIXWatch(true);
			synchronized (m_MainContract.m_WatchSync) {
				m_MainContract.m_WatchSync.notifyAll();
			}
		}

		// Start VIX calculation
		new Thread() {
			@Override
			public void run() {

				int count = ((IBQuotesProvider)(m_IBSubscriber.GetProvider())).GetVixServePerSubs();
				int total = ((IBQuotesProvider)(m_IBSubscriber.GetProvider())).GetVixServePerSession();
				
				m_VIXCalcStop = false;
				Thread.currentThread().setName("VIX Calaulator");
				while (!m_VIXCalcStop) {

					try {
						if (m_IBSubscriber.IsWeekend() || m_MainContract.InOffTime()) {
							System.out.printf("Contract '%s' is Not in trading hours!\n", m_MainContract.GetRealContract().localSymbol());
							TimeUnit.MINUTES.sleep(5);
							continue;
						}

						synchronized (m_MainContract.GetVIX()) {
							CalculateVIX();
							count--; total--;
						}

						if (m_MainContract.GetVIX() > 0.0) {
					        MongoQuotesStore IBQuotesdb = (MongoQuotesStore)(m_MainContract.GetSubscriber().GetProvider()).GetPersistenceStore();
	
					        try {
					        	
					        	String collection = String.format("VIX_%s_%s_%d", 
					        			m_MainContract.GetRealContract().symbol(),
					        			m_MainContract.GetRealContract().lastTradeDateOrContractMonth(),
					        			m_MainContract.GetRealContract().conid()
					        			);
					        	
								IBQuotesdb.Put(collection, new Document()
										.append("ts", Calendar.getInstance(TimeZone.getTimeZone("US/Eastern")).getTimeInMillis())
										.append("vix", m_MainContract.GetVIX()));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}

						System.out.printf("\n\n++++++ The overall VIX value for %s %s is: %f ++++++\n\n", m_MainContract.GetRealContract().symbol(), m_MainContract.GetRealContract().lastTradeDateOrContractMonth(), m_MainContract.GetVIX());
						TimeUnit.SECONDS.sleep(5);
						
						if (total <= 0) {
							System.exit(0);
						}
/*						
						if (count <= 0) {
							m_IBSubscriber.DisableVIX(m_MainContract);
							TimeUnit.SECONDS.sleep(3);
							m_IBSubscriber.EnableVIX(m_MainContract);
							break;
						}
						*/
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
	

	public void StopVIX() {

		synchronized (m_MainContract.GetVIX()) {
			m_VIXCalcStop = true;
		}

		synchronized (m_OptionMap) {
			m_OptionMap.StopVIXWatch();
		}

	}
	

	public void Dump() {

		int dcount = 0, icount = 0;
		TreeSet<Long> ts = new TreeSet<>(m_OptionMap.m_Map.keySet());

		for (Long dt : ts) {
			System.out.printf("Date: %d\n", dt);
			HashMap<Double, HashMap<Types.Right, OptContract>> items = m_OptionMap.m_Map.get(dt);
			TreeSet<Double> strikelist = new TreeSet<Double>(items.keySet());

			for (Double strike : strikelist) {
				System.out.printf("\tStrike: %f,\t", strike);

				HashMap<Types.Right, OptContract> pair = items.get(strike);
				for (Types.Right r : pair.keySet()) {
					System.out.printf("[%s: %d]\t", r.toString(), pair.get(r).m_ConDetails.conid());
					icount++;
				}
				System.out.println();
			}
			System.out.println();
			dcount++;
		}

		System.out.printf("\nTotal: %d contracts of FOP, %d strike dates.\n", icount, dcount);
	}

	public IBContract GetMainContract() {
		return m_MainContract;
	}

	public ContractDetails GetMainContractDetails() {
		return null == m_MainContract ? null : m_MainContract.m_ConDetails;
	}

	public OptionMap GetOptionMap() {
		return m_OptionMap;
	}

	public IBQuotesSubscriber GetSubscriber() {
		return m_IBSubscriber;
	}
}
