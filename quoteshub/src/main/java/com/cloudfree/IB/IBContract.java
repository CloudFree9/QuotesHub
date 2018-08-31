package com.cloudfree.IB;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import com.cloudfree.VContract;
import com.cloudfree.IB.EWrapperHandlers.HistoricalDataHandler;
import com.cloudfree.IB.EWrapperHandlers.RealTimeBarHandler;
import com.cloudfree.IB.EWrapperHandlers.RealTimeTickDataHandler;
import com.cloudfree.IB.EWrapperHandlers.Tick;
import com.cloudfree.IB.OptionChain.OptContract;
import com.cloudfree.IB.OptionChain.OptionMap;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.Bar;
import com.ib.controller.ExtController.ICommonHandler;

public class IBContract extends VContract {

	public class VIXTuple {
		public long m_TimeStamp = 0;
		public double m_VIX = 0.0;
	}
	
	private static final long serialVersionUID = 8815420348891363024L;
	private OptionChain m_OptionChain = null;
	protected Contract m_RealContract = null;
	protected ContractDetails m_ConDetails = null;
	protected final IBQuotesSubscriber m_Subscriber;
	private boolean m_VIXEnabled = false;
	private Double m_VIX = 0.0;
	private double m_UpStrike = Double.MAX_VALUE;
	private double m_BotStrike = -1;

	public double m_RecentAsk = 0;
	public double m_RecentBid = 0;
	public double m_RecentPrice = 0;

	protected RealTimeBarHandler m_RealTimeBarHandler = null;
	protected RealTimeTickDataHandler m_RealTimeTickHandler = null;
	protected HistoricalDataHandler m_HistBarHandler = null;

	protected final Deque<Bar> m_RealTimeBarQueue = new LinkedList<>();
	protected final Deque<VIXTuple> m_VIXQueue = new LinkedList<>();
	protected final Deque<Tick> m_RealTimeTickQueue = new LinkedList<>();
	protected final Deque<Bar> m_HistBarQueue = new LinkedList<>();
	protected final Deque<Tick> m_HistTickQueue = new LinkedList<>();
	
	public final Object m_PriceSync = new Object();
	public final Object m_OptPriceSync = new Object();
	public final Object m_VIXSync = new Object();
	public final Object m_WatchSync = new Object();
	
	protected final int m_DefaultQueueSize = 1000;

	public IBContract(Contract c, IBQuotesSubscriber s) throws Exception {
		m_Subscriber = s;
		m_RealContract = c;
	}

	public IBContract SetContractDetails(ContractDetails cd) {
		m_ConDetails = cd;
		return this;
	}
	
	public ContractDetails GetConDetails() {
		return m_ConDetails;
	}
	
	public synchronized Double GetVIX() {
		return m_VIX;
	}

	public synchronized IBContract SetVIX(double v) {
		m_VIX = v;
		return this;
	}

	public IBContract Ask(double a) {
		m_RecentAsk = a;
		return this;
	}

	public IBContract Bid(double b) {
		m_RecentBid = b;
		return this;
	}

	public synchronized double GetPrice() {
		return m_RecentPrice;
	}

	public synchronized void SetPrice(double p) {
		m_RecentPrice = p;
	}

	public synchronized double GetUpStrike() {
		return m_UpStrike;
	}

	public synchronized void SetUpStrike(double v) {
		m_UpStrike = v;
	}

	public synchronized double GetBotStrike() {
		return m_BotStrike;
	}

	public synchronized void SetBotStrike(double v) {
		m_BotStrike = v;
	}

	public IBQuotesSubscriber GetSubscriber() {
		return m_Subscriber;
	}

	@Override
	public String toString() {
		return m_RealContract == null ? "None" : m_RealContract.localSymbol();
	}

	@Override
	public Contract GetRealContract() {
		return m_RealContract;
	}

	public IBContract SetRealContract(Contract c) {
		m_RealContract = c;
		return this;
	}

	@Override
	public boolean equals(Object c) {

		if (c == null || c.getClass() != this.getClass())
			return false;

		IBContract c1 = (IBContract) c;

		if (m_RealContract == null || c1.GetRealContract() == null)
			return false;

		if (this == c1 || m_RealContract == c1.GetRealContract()
				|| m_RealContract.conid() == c1.GetRealContract().conid())
			return true;

		return false;
	}

	@Override
	public int hashCode() {
		if (m_RealContract == null)
			return Integer.MIN_VALUE;
		return m_RealContract.conid();
	}

	@Override
	public boolean EnableVIX() {
		if (m_Subscriber == null || m_VIXEnabled) {
			return false;
		}

		try {
			m_Subscriber.m_Mutex.acquire();
			GetOptionChain();
			m_OptionChain.StartVIX();
			m_VIXEnabled = true;
			m_Subscriber.m_Mutex.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return m_VIXEnabled;
	}

	@Override
	public OptionChain GetOptionChain() {
		if (m_OptionChain == null)
			try {
				m_OptionChain = new OptionChain(m_Subscriber, this);
			} catch (Exception e) {
				e.printStackTrace();
			}

		return m_OptionChain;
	}

	@Override
	public boolean IsVixEnabled() {
		return m_VIXEnabled;
	}

	@Override
	public boolean DisableVIX() {
		if (m_Subscriber == null || m_OptionChain == null) {
			return false;
		}

		try {
			m_Subscriber.m_Mutex.acquire();
			m_OptionChain.StopVIX();
			m_VIXEnabled = false;
			m_Subscriber.m_Mutex.release();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public IBContract Subscribe(int type) {

		if (m_Subscriber == null)
			return this;

		try {
			switch (type) {
			case ICommonHandler.REALTIMEBAR:
				if (null == m_RealTimeBarHandler)
					m_RealTimeBarHandler = RealTimeBarHandler.GetRealTimeHandler(this);
				RequestRealTimeBars();
				break;
			case ICommonHandler.HISTORICALBAR:
				if (null == m_HistBarHandler)
					m_HistBarHandler = HistoricalDataHandler.GetHistoricalDataHandler(this);
				RequestHistoricalBars();
				break;
			case ICommonHandler.REALTIMETICK:
				if (null == m_RealTimeTickHandler)
					m_RealTimeTickHandler = RealTimeTickDataHandler.GetRealTimeTickHandler(this);
				RequestRealTimeTicks();
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return this;
	}

	@Override
	public IBContract UnSubscribe(int type) {

		if (m_Subscriber == null)
			return this;

		try {
			switch (type) {
			case ICommonHandler.REALTIMEBAR:
				CancelRealTimeBars();
				break;
			case ICommonHandler.HISTORICALBAR:
				CancelHistoricalBars();
				break;
			case ICommonHandler.REALTIMETICK:
				CancelRealTimeTicks();
				break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return this;
	}

	public Deque<Bar> GetRealTimeBarsQueue() {
		return m_RealTimeBarQueue;
	}

	public Deque<VIXTuple> GetVIXQueue() {
		return m_VIXQueue;
	}

	public Deque<Bar> GetHistBarsQueue() {
		return m_HistBarQueue;
	}

	public Deque<Tick> GetRealTimeTickQueue() {
		return m_RealTimeTickQueue;
	}

	public Deque<Tick> GetHistTickQueue() {
		return m_HistTickQueue;
	}

	public IBContract RequestRealTimeBars() {
		if (m_Subscriber == null || m_RealTimeBarHandler == null)
			return this;

		IBQuotesProvider p = (IBQuotesProvider) m_Subscriber.GetProvider();
		p.controller().reqRealTimeBars(this.GetRealContract(), WhatToShow.TRADES, false, m_RealTimeBarHandler);
		return this;
	}

	public IBContract CancelRealTimeBars() {
		if (m_Subscriber == null || m_RealTimeBarHandler == null)
			return this;

		IBQuotesProvider p = (IBQuotesProvider) m_Subscriber.GetProvider();
		p.controller().cancelRealtimeBars(m_RealTimeBarHandler);
		return this;
	}

	public IBContract RequestHistoricalBars() {
		if (m_Subscriber == null || m_RealTimeBarHandler == null)
			return this;

		IBQuotesProvider p = (IBQuotesProvider) m_Subscriber.GetProvider();
		p.controller().reqHistoricalData(GetRealContract(),
				new SimpleDateFormat("yyyyMMdd HH:mm:ss").format(new Date()), m_HistBarHandler.GetDurationQuantity(),
				m_HistBarHandler.GetDurationUnit(), m_HistBarHandler.GetBarSize(), WhatToShow.TRADES, false, false,
				m_HistBarHandler);
		return this;
	}

	public IBContract CancelHistoricalBars() {
		if (m_Subscriber == null || m_RealTimeBarHandler == null)
			return this;

		IBQuotesProvider p = (IBQuotesProvider) m_Subscriber.GetProvider();
		p.controller().cancelHistoricalData(m_HistBarHandler);
		return this;
	}

	public IBContract RequestRealTimeTicks() {
		if (m_Subscriber == null || m_RealTimeTickHandler == null)
			return this;

		IBQuotesProvider p = (IBQuotesProvider) m_Subscriber.GetProvider();
		
		p.controller().reqTopMktData(GetRealContract(), "106", false, false, m_RealTimeTickHandler);
		return this;
	}

	public IBContract CancelRealTimeTicks() {
		if (m_Subscriber == null || m_RealTimeTickHandler == null)
			return this;

		IBQuotesProvider p = (IBQuotesProvider) m_Subscriber.GetProvider();

		p.controller().cancelTopMktData(m_RealTimeTickHandler);
		return this;
	}

	public Bar TakeBar(int type) {
		Bar res = null;
		Deque<Bar> q = null;

		switch (type) {
		case ICommonHandler.REALTIMEBAR:
			q = m_RealTimeBarQueue;
			break;
		case ICommonHandler.HISTORICALBAR:
			q = m_HistBarQueue;
			break;
		}

		synchronized (q) {
			if (null != q && q.size() > 0)
				res = q.removeFirst();
		}
		return res;
	}

	public void PutBar(int type, Bar bar) {
		Deque<Bar> q = null;

		switch (type) {
		case ICommonHandler.REALTIMEBAR:
			q = m_RealTimeBarQueue;
			break;
		case ICommonHandler.HISTORICALBAR:
			q = m_HistBarQueue;
			break;
		}

		synchronized (q) {
			if (null != q) {
				q.addLast(bar);
				if (q.size() > m_DefaultQueueSize)
					q.removeFirst();
			}
		}
	}
	
	public Tick TakeTick(int type) {
		Tick res = null;
		Deque<Tick> q = null;

		switch (type) {
		case ICommonHandler.REALTIMETICK:
			q = m_RealTimeTickQueue;
			break;
		}

		synchronized (q) {
			if (null != q && q.size() > 0)
				res = q.removeFirst();
		}
		return res;
	}

	public void PutTick(int type, Tick tick) {

		Deque<Tick> q = null;

		switch (type) {
		case ICommonHandler.REALTIMETICK:
			q = m_RealTimeTickQueue;
			break;
		}

		synchronized (q) {
			if (null != q) {
				q.addLast(tick);
				if (q.size() > m_DefaultQueueSize)
					q.removeFirst();
			}
		}
	}
	
	@Override
	public boolean InOffTime() throws Exception {
		
		boolean res = true;
		TimeZone ny = TimeZone.getTimeZone("US/Eastern");
		Calendar now_in_us = Calendar.getInstance(ny);
		
		for (String th : m_ConDetails.tradingHours().split(";")) {
			if (th.matches("\\s*")) 
				continue;
			String duration[] = th.split("-");
			String start[] = duration[0].split(":");
			if (start[1].equals("CLOSED")) continue;
			String end[] = duration[1].split(":");
			
			Calendar st = Calendar.getInstance(ny);
			int ymd = Integer.parseInt(start[0]);
			int hm = Integer.parseInt(start[1]);
			st.clear();
			st.set(ymd / 10000, ymd % 10000 / 100 - 1, ymd % 100, hm / 100, hm % 100, 0);

			Calendar ed = Calendar.getInstance(ny);
			ymd = Integer.parseInt(end[0]);
			hm = Integer.parseInt(end[1]);
			ed.clear();
			ed.set(ymd / 10000, (ymd % 10000) / 100 - 1, ymd % 100, hm / 100, hm % 100, 0);
			
			if (now_in_us.after(st) && now_in_us.before(ed)) {
				res = false;
				break;
			}
			
		}
		
		return res;
	}

}
