package com.cloudfree.IB;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
import com.ib.controller.ExtController.IConnectionHandler;

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
		// private IVMonitor m_IVMonThread = new IVMonitor();
		private Thread m_WatchThread = null;

		public static final int UPPER = 1;
		public static final int LOWER = 0;

		public final HashMap<Long, HashMap<Double, HashMap<Types.Right, OptContract>>> m_Map = new HashMap<>();
		// Date => < Strike price => < Right => Option> >

		public OptionMap() throws Exception {

			if (m_MainContract == null) {
				throw new Exception("Main cotract for optionMap is null!");
			}

			m_MainContract.m_RTBarNotify.add(this);
			m_MainContract.m_RTTickNotify.add(this);
			m_MainContract.m_HistBarNotify.add(this);
		}

		public void Refresh() {
			m_VIXWatchList.stream().flatMap(e -> e.stream()).forEach(oc -> {
				oc.Subscribe(ICommonHandler.REALTIMETICK);
			});
		}

		public double GetUnderlyingPrice() {
			return m_MainContract.GetPrice();
		}

		public HashSet<OptContract> GetWatchList() {
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

		private boolean OnFriday(long d) {
			Calendar cl = Calendar.getInstance();
			int y = (int) (d / 10000);
			int m = (int) (d % 10000 / 100);
			int dat = (int) (d % 100);

			cl.set(y, m, dat, 0, 0);

			int weekday = cl.get(Calendar.DAY_OF_WEEK);
			return weekday == Calendar.FRIDAY;
		}

		public OptionMap UpdateVIXWatchList() throws Exception {

			if (m_Map == null || m_Map.size() == 0)
				return this;

			final double up_price;
			final double bot_price;
			boolean refresh = false;
			final double underprice = GetUnderlyingPrice();

			final int und_y_m = Integer
					.parseInt(m_MainContract.GetRealContract().lastTradeDateOrContractMonth().substring(0, 6));

			final OptContract ocon = m_Map.entrySet().stream()
					.filter(e -> ((e.getKey() / 100) == und_y_m) && e.getValue() != null && !e.getValue().isEmpty())
					.flatMap(e -> e.getValue().entrySet().stream()).limit(1).map(e -> {
						return e.getValue().get(Types.Right.Call);
					}).findFirst().orElse(null);

			if (ocon == null)
				throw new Exception("No monthly option date found.");

			// final String tradingcls = ocon.GetRealContract().tradingClass();

			final long monthlyOptDate = m_Map.entrySet().stream().map(e -> {
				return e.getValue().entrySet().stream().limit(1).findFirst().get().getValue().get(Types.Right.Call);
			}).filter(e1 -> e1.GetRealContract().tradingClass().equals(ocon.GetRealContract().tradingClass()))
					.map(e2 -> {
						return Long.parseLong(e2.GetRealContract().lastTradeDateOrContractMonth().substring(0, 8));
					}).sorted().findFirst().orElse((long) -1);

			if (monthlyOptDate == -1)
				throw new Exception("Monthly option date not found.");

			List<Double> stirke_prices = m_Map.entrySet().stream().filter(e -> (e.getKey() == monthlyOptDate))
					.flatMap(e -> e.getValue().entrySet().stream()).map(e -> e.getKey()).collect(Collectors.toList());

			bot_price = stirke_prices.stream().filter(e -> e <= underprice).max((e1, e2) -> e1.compareTo(e2))
					.orElse(new Double(-1.0));

			up_price = stirke_prices.stream().filter(e -> e > underprice).min((e1, e2) -> e1.compareTo(e2))
					.orElse(new Double(-1.0));

			if (bot_price == -1.0 || up_price == -1.0)
				throw new Exception("Error on getting UP || Down strike price.");

			if (bot_price != m_MainContract.GetBotStrike() || up_price != m_MainContract.GetUpStrike()) {
				m_MainContract.SetBotStrike(bot_price);
				m_MainContract.SetUpStrike(up_price);
				refresh = true;
			} else {
				return this;
			}

			List<OptContract> ulist = new ArrayList<>();
			List<OptContract> blist = new ArrayList<>();

			m_Map.entrySet().stream().filter(e -> (e.getKey() == monthlyOptDate))
					.flatMap(e -> e.getValue().entrySet().stream())
					.filter(e -> (e.getKey() == up_price || e.getKey() == bot_price)).forEach(m -> {
						double p = m.getKey();
						List<OptContract> tl = p == up_price ? ulist : blist;
						tl.add(m.getValue().get(Types.Right.Call));
						tl.add(m.getValue().get(Types.Right.Put));
					});

			synchronized (m_VIXWatchList) {

				if (refresh) {
					List<IBContract> lv = m_VIXWatchList.stream().flatMap(l -> l.stream()).distinct()
							.collect(Collectors.toList());
					lv.forEach(c -> {
						c.SetPrice(0.0);
						if (c instanceof OptContract) {
							((OptContract) c).Volatility(0.0);
						}
						if (m_MainContract.IsVixEnabled()) {
							c.UnSubscribe(ICommonHandler.REALTIMETICK);
						}
					});

					synchronized (m_WatchList) {
						m_WatchList.removeAll(lv);
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

			}
			return this;
		}

		public OptionMap AddWatchList(Long date, Double strike, Types.Right t) {

			if (date != null && !m_Map.containsKey(date)) {
				return this;
			}

			synchronized (m_WatchList) {

				m_Map.entrySet().stream().filter(e -> {
					return e.getKey() == date;
				}).flatMap(e0 -> e0.getValue().entrySet().stream()).filter(e1 -> {
					return e1.getKey() == strike;
				}).flatMap(e2 -> e2.getValue().entrySet().stream()).filter(e3 -> {
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

			if (date == null || strike == null || t == null) {
				m_WatchList.clear();
				return this;
			}

			synchronized (m_WatchList) {

				List<OptContract> temp = m_WatchList.stream().filter(opt -> {
					Contract con = opt.m_Contract.contract();
					return !con.lastTradeDateOrContractMonth().equals(date.toString()) || con.strike() != strike
							|| con.right() != t;
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
			if (m_WatchThread == null || !m_WatchThread.isAlive())
				return;
			m_WatchThread.interrupt();

			m_OptionMap.m_VIXWatchList.stream().flatMap(e -> e.stream()).distinct().forEach(vc -> {
				vc.UnSubscribe(ICommonHandler.REALTIMETICK);
			});

		}

		/*
		 * public void StartIVMoitor() { if (!m_IVMonThread.isAlive())
		 * m_IVMonThread.start(); }
		 * 
		 * public void StopIVMoitor() { if (m_IVMonThread.isAlive())
		 * m_IVMonThread.Stop(); }
		 */
	}

	/*
	 * private class IVMonitor extends Thread {
	 * 
	 * private boolean m_Stop = false;
	 * 
	 * public void Stop() { m_Stop = true; }
	 * 
	 * @Override public void run() {
	 * 
	 * if (m_IBSubscriber == null) return;
	 * this.setName("IV calculate monitor for underlying contract : " +
	 * m_IBSubscriber.GetName());
	 * 
	 * while (!m_Stop) {
	 * 
	 * synchronized(m_MainContract.m_OptPriceSync) { try {
	 * m_MainContract.m_OptPriceSync.wait(); double underprice =
	 * m_MainContract.GetPrice();
	 * 
	 * synchronized(m_OptionMap.GetVIXWatchList()) {
	 * System.out.printf("### Total %d contracts in VIX Watch list ###\n",
	 * m_OptionMap.GetVIXWatchList().size()); m_OptionMap.GetVIXWatchList()
	 * .stream() .filter(e -> e.GetPrice() > 0) .forEach(con ->{ Contract c =
	 * con.GetRealContract();
	 * ((IBQuotesProvider)m_IBSubscriber.GetProvider()).controller().
	 * reqOptionVolatility( c, con.GetPrice(), underprice, con.m_OptComputeHandler);
	 * // con.m_TickOptHandler); System.out.
	 * printf("### Contract: %s, price: %f, vol: %f, under price :%f ###\n",
	 * c.localSymbol(), con.GetPrice(), con.m_Volatility,
	 * con.GetUnderContract().GetPrice());
	 * 
	 * }); }
	 * 
	 * } catch (InterruptedException e) { e.printStackTrace(); } }
	 * 
	 * }
	 * 
	 * } }
	 */
	final private IBContract m_MainContract; // underlying instrument of the options
	final private OptionMap m_OptionMap;
	final private IBQuotesSubscriber m_IBSubscriber;
	private Double m_VIX = 0.0;
	private boolean m_VIXStop = true;
	final public ContractDetails m_MConDetails;

	public OptionChain(IBQuotesSubscriber s, IBContract underlying) throws Exception {

		m_IBSubscriber = s;
		m_MainContract = underlying;
		m_OptionMap = new OptionMap();

		Contract con = underlying.GetRealContract().clone();
		List<ContractDetails> cons = m_IBSubscriber.GetContractDetails(con);
		m_MConDetails = cons.size() > 0 ? cons.get(0) : null;

		if (m_MConDetails == null)
			throw new Exception("Invalid contract to retrieve the option chain.");

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

		synchronized (m_VIX) {
			if (biv > 0 && uiv > 0) {
				double pricepos = (m_MainContract.GetUpStrike() - m_MainContract.GetPrice())
						/ (m_MainContract.GetUpStrike() - m_MainContract.GetBotStrike());
				m_VIX = new Double((biv * (1 - pricepos) + uiv * pricepos) * 100);
				m_MainContract.SetVIX(m_VIX);
			}
			return m_VIX;
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
				// c.lastTradeDateOrContractMonth(con.lastTradeDateOrContractMonth().substring(0,6));

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

			m_OptionMap.UpdateVIXWatchList();

		}
	}

	public void StartVIX() throws Exception {

		synchronized (m_OptionMap) {
			m_OptionMap.StartOptStructWatch(true);
			synchronized (m_MainContract.m_WatchSync) {
				m_MainContract.m_WatchSync.notifyAll();
			}
			// m_OptionMap.StartIVMoitor();
		}

		new Thread() {
			@Override
			public void run() {
				Thread.currentThread().setName("NULL Option price checker");
				long last_count = 0;

				while (true) {
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					long count = m_OptionMap.m_VIXWatchList.stream().flatMap(e -> e.stream())
							.filter(e -> e.GetPrice() == 0.0).count();
					long count1 = m_OptionMap.m_VIXWatchList.stream().flatMap(e -> e.stream()).count();
					System.out.printf(">>>>>>>>>>>>> There are %d options with the price of ZERO, total: %d\n", count,
							count1);
					last_count = count;

				}
			}
		}.start();

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

						synchronized (m_VIX) {
							CalculateVIX();
						}

						System.out.printf("\n\n+++++++++++ The overall VIX value is: %f +++++++++++\n\n", m_VIX);
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	public void StopVIX() {

		synchronized (m_VIX) {
			m_VIXStop = true;
		}

		synchronized (m_OptionMap) {
			m_OptionMap.StopOptStructWatch();
			// m_OptionMap.StopIVMoitor();
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
