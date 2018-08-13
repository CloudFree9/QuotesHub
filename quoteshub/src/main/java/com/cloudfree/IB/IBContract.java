package com.cloudfree.IB;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Queue;
import com.cloudfree.VContract;
import com.cloudfree.IB.EWrapperHandlers.HistoricalDataHandler;
import com.cloudfree.IB.EWrapperHandlers.RealTimeBarHandler;
import com.cloudfree.IB.EWrapperHandlers.RealTimeTickDataHandler;
import com.cloudfree.IB.EWrapperHandlers.Tick;
import com.ib.client.Contract;
import com.ib.client.Types.WhatToShow;
import com.ib.controller.Bar;
import com.ib.controller.ExtController.ICommonHandler;

public class IBContract extends VContract {

	private static final long serialVersionUID = 8815420348891363024L;
	private OptionChain m_OptionChain = null;
	protected Contract m_RealContract;
	protected final IBQuotesSubscriber m_Subscriber;
	private boolean m_Started = false;
	private boolean m_VIXEnabled = false;
	private Double m_VIX = 0.0;
	private boolean m_PriceChanged = false;
	private double m_UpStrike = Double.MAX_VALUE;
	private double m_BotStrike = -1;

	public double m_RecentAsk = 0;
	public double m_RecentBid = 0;
	public double m_RecentPrice = 0;

	protected RealTimeBarHandler m_RealTimeBarHandler = null;
	protected RealTimeTickDataHandler m_RealTimeTickHandler = null;
	protected HistoricalDataHandler m_HistBarHandler = null;

	protected final Queue<Bar> m_RealTimeBarQueue = new LinkedList<>();
	protected final Queue<Tick> m_RealTimeTickQueue = new LinkedList<>();
	protected final Queue<Bar> m_HistBarQueue = new LinkedList<>();
	protected final Queue<Tick> m_HistTickQueue = new LinkedList<>();

	public final Object m_PriceSync = new Object();
	public final Object m_OptPriceSync = new Object();
	public final Object m_VIXSync = new Object();
	public final Object m_WatchSync = new Object();

	public IBContract(Contract c, IBQuotesSubscriber s) {
		m_Subscriber = s;
		m_RealContract = c;
	}

	public boolean IsStarted() {
		return m_Started;
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

	public boolean IsPriceChanged() {
		return m_PriceChanged;
	}

	public void SetPriceChangeFlag(boolean tf) {
		m_PriceChanged = tf;
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
			GetOptionChain();
			m_OptionChain.StartVIX();
			m_VIXEnabled = true;
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
			m_OptionChain.StopVIX();
			m_VIXEnabled = false;
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
				m_RealTimeBarHandler = RealTimeBarHandler.GetRealTimeHandler(this);
				RequestRealTimeBars();
				break;
			case ICommonHandler.HISTORICALBAR:
				m_HistBarHandler = HistoricalDataHandler.GetHistoricalDataHandler(this);
				RequestHistoricalBars();
				break;
			case ICommonHandler.REALTIMETICK:
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

	public Queue<Bar> GetRealTimeBarsQueue() {
		return m_RealTimeBarQueue;
	}

	public Queue<Bar> GetHistBarsQueue() {
		return m_HistBarQueue;
	}

	public Queue<Tick> GetRealTimeTickQueue() {
		return m_RealTimeTickQueue;
	}

	public Queue<Tick> GetHistTickQueue() {
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
		Queue<Bar> q = null;

		switch (type) {
		case ICommonHandler.REALTIMEBAR:
			q = m_RealTimeBarQueue;
			break;
		case ICommonHandler.HISTORICALBAR:
			q = m_HistBarQueue;
			break;
		}

		synchronized (q) {
			if (q != null)
				res = q.remove();
		}
		return res;
	}

	public Tick TakeTick(int type) {
		Tick res = null;
		Queue<Tick> q = null;

		switch (type) {
		case ICommonHandler.REALTIMETICK:
			q = m_RealTimeTickQueue;
			break;
		}

		synchronized (q) {
			if (q != null)
				res = q.remove();
		}
		return res;
	}
}
