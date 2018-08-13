package com.cloudfree.IB;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import com.cloudfree.IB.EWrapperHandlers.OptionComputeHandler;
import com.cloudfree.IB.EWrapperHandlers.RTTickOptHandler;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Types;
import com.ib.client.Types.SecType;
import com.ib.controller.ExtController;
import com.ib.controller.ExtController.ICommonHandler;

public class OptionChain {

	public static class OptContract extends IBContract {

		private static final long serialVersionUID = 202308023907668114L;
		final private IBContract m_UnderContract;
		public ContractDetails m_Contract = null;
		public double m_Volatility = 0;
		public double m_Delta = 0;
		public double m_Gamma = 0;
		public double m_Theta = 0;
		public double m_Vega = 0;
		private RTTickOptHandler m_TickOptHandler = RTTickOptHandler.GetRTOptHandler(this);

		public final OptionComputeHandler m_OptComputeHandler = new OptionComputeHandler(this);

		public OptContract(Contract con, IBContract under) {
			super(con, under.GetSubscriber());
			m_UnderContract = under;
		}

		public IBContract GetUnderContract() {
			return m_UnderContract;
		}

		public OptContract RequestOptionVolatility(OptContract con, double price, double underprice,
				ExtController.IOptHandler h) {

			IBQuotesProvider p = (IBQuotesProvider) m_Subscriber.GetProvider();
			p.controller().reqOptionVolatility(con.GetRealContract(), price, underprice, h);
			return this;
		}

		@Override
		public OptContract Subscribe(int type) {

			if (m_Subscriber == null)
				return this;

			if (type == ICommonHandler.REALTIMETICK) {
				m_RealTimeTickHandler = m_TickOptHandler;
				RequestRealTimeTicks();
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

		private OptStructBuilderThread m_OptStructWatchThread = new OptStructBuilderThread();
		private Thread m_WatchThread = null;

		public static final int UPPER = 1;
		public static final int LOWER = 0;
		public final HashMap<Long, HashMap<Double, HashMap<Types.Right, OptContract>>> m_Map = new HashMap<>();

		public OptionMap() throws Exception {

			if (m_MainContract == null) {
				throw new Exception("Main cotract for optionMap is null!");
			}

			m_MainContract.m_RTBarNotify.add(this);
			m_MainContract.m_RTTickNotify.add(this);
			m_MainContract.m_HistBarNotify.add(this);
		}

		public void Refresh() {
			m_VIXWatchList
				.stream()
				.flatMap(e -> e.stream())
				.forEach(oc -> {
					oc.Subscribe(ICommonHandler.REALTIMETICK);
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

			if (!m_MainContract.IsVixEnabled() || m_Map == null || m_Map.size() == 0)
				return this;

			final double up_price;
			final double bot_price;

			final double underprice = GetUnderlyingPrice();

			/*
			 * Filter the whole option map with the option's contract month is the same as the underlying future.
			 */
			final List<HashMap<Double, HashMap<Types.Right, OptContract>>> cons =
					m_Map.entrySet()
					.stream()
					.map(e -> e.getValue())
					.collect(Collectors.toList());
			
			if (null == cons || cons.isEmpty())
				throw new Exception("No corresponding options found.");
			
			/*
			 * Get the strike price pair which embraces the current underlying price tightly
			 */
			Set<Double> stirke_prices = cons.get(0).keySet();

			bot_price = stirke_prices.stream().filter(e -> e <= underprice).max((e1, e2) -> e1.compareTo(e2))
					.orElse(new Double(-1.0));

			up_price = stirke_prices.stream().filter(e -> e > underprice).min((e1, e2) -> e1.compareTo(e2))
					.orElse(new Double(-1.0));

			if (bot_price == -1.0 || up_price == -1.0)
				throw new Exception("Error on getting UP || Down strike price.");

			if (bot_price != m_MainContract.GetBotStrike() || up_price != m_MainContract.GetUpStrike()) {
				m_MainContract.SetBotStrike(bot_price);
				m_MainContract.SetUpStrike(up_price);
			} else {
				return this;
			}

			List<OptContract> ulist = new ArrayList<>();
			List<OptContract> blist = new ArrayList<>();

			cons.stream()
				.flatMap(e -> e.entrySet().stream())
				.filter(e -> (e.getKey() == up_price || e.getKey() == bot_price))
				.forEach(m -> {
					double p = m.getKey();
					List<OptContract> tl = p == up_price ? ulist : blist;
					tl.add(m.getValue().get(Types.Right.Call));
					tl.add(m.getValue().get(Types.Right.Put));
				});

			synchronized (m_VIXWatchList) {

				m_VIXWatchList
					.stream()
					.flatMap(l -> l.stream())
					.forEach(c -> {
						c.UnSubscribe(ICommonHandler.REALTIMETICK);
					});
				
				synchronized (m_WatchList) {
//					m_WatchList.removeAll(lv);
					m_VIXWatchList.get(UPPER).clear();
					m_VIXWatchList.get(LOWER).clear();
					m_VIXWatchList.get(LOWER).addAll(blist);
					m_VIXWatchList.get(UPPER).addAll(ulist);
					m_WatchList.addAll(blist);
					m_WatchList.addAll(ulist);
				}

				m_VIXWatchList.stream().flatMap(s -> s.stream()).forEach(c -> {
					System.out.printf("Subscribe option quotes for %s %s", c.m_RealContract.localSymbol(),
							c.m_RealContract.lastTradeDateOrContractMonth());
					c.Subscribe(ICommonHandler.REALTIMETICK);
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
					Contract con = opt.m_Contract.contract();
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

		private class OptStructBuilderThread implements Runnable {

			@Override
			public void run() {

				Thread.currentThread().setName("Option struct watch monitor");
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

		public void StartOptStructWatch(boolean refresh) {

			if (m_WatchThread != null && m_WatchThread.isAlive())
				return;

			m_WatchThread = new Thread(m_OptStructWatchThread);
			m_WatchThread.start();

			if (refresh) {
				synchronized (m_MainContract.m_WatchSync) {
					m_MainContract.m_WatchSync.notifyAll();
				}
			}
		}

		public void StopOptStructWatch() {
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
	final public ContractDetails m_MConDetails;

	private boolean m_VIXStop = true;

	public OptionChain(IBQuotesSubscriber s, IBContract underlying) throws Exception {

		m_IBSubscriber = s;
		m_MainContract = underlying;
		m_OptionMap = new OptionMap();

		List<ContractDetails> cons = m_IBSubscriber.GetContractDetails(underlying.GetRealContract());
		m_MConDetails = cons.size() > 0 ? cons.get(0) : null;

		if (m_MConDetails == null)
			throw new Exception("Invalid main contract to retrieve the option chain.");

		m_MainContract.SetRealContract(m_MConDetails.contract());

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
				optcontract.m_Contract = con;
				opt.put(con.contract().right(), optcontract);
			}

			m_OptionMap.m_Map.clear();
			m_OptionMap.m_Map.putAll(m_Map);
			m_OptionMap.notifyAll();
		}
	}
	

	public void StartVIX() throws Exception {

		synchronized (m_OptionMap) {
			m_OptionMap.StartOptStructWatch(true);
			synchronized (m_MainContract.m_WatchSync) {
				m_MainContract.m_WatchSync.notifyAll();
			}
		}

		/*
		new Thread() {
			@Override
			public void run() {
				Thread.currentThread().setName("NULL Option price checker");

				while (true) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					
					long count = m_OptionMap.m_VIXWatchList.stream().flatMap(e -> e.stream())
							.filter(e -> e.GetPrice() == 0.0).count();
					long count1 = m_OptionMap.m_VIXWatchList.stream().flatMap(e -> e.stream()).count();
					System.out.printf(">>>> Warning: there are %d options with the price of ZERO, total: %d\n", count,
							count1);

				}
			}
		}.start();
		*/
		
		new Thread() {
			@Override
			public void run() {

				m_VIXStop = false;
				Thread.currentThread().setName("VIX Calaulator");
				while (!m_VIXStop) {

					try {
						synchronized (m_MainContract.m_VIXSync) {
							m_MainContract.m_VIXSync.wait(5000);
						}

						synchronized (m_MainContract.GetVIX()) {
							CalculateVIX();
						}

						System.out.printf("\n\n++++++ The overall VIX value for %s is: %f ++++++\n\n", m_MainContract.GetRealContract().localSymbol(), m_MainContract.GetVIX());
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}
	

	public void StopVIX() {

		synchronized (m_MainContract.GetVIX()) {
			m_VIXStop = true;
		}

		synchronized (m_OptionMap) {
			m_OptionMap.StopOptStructWatch();
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
					System.out.printf("[%s: %d]\t", r.toString(), pair.get(r).m_Contract.conid());
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
		return m_MConDetails;
	}

	public OptionMap GetOptionMap() {
		return m_OptionMap;
	}

	public IBQuotesSubscriber GetSubscriber() {
		return m_IBSubscriber;
	}
}
