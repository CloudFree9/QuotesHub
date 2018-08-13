package com.cloudfree;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractQuotesSubscriber implements Serializable {

	private static final long serialVersionUID = 2804676499407432113L;

	/* Subscribe/Unsubscribe the market data from service provider like IB or CTP
	 * for a specific Contract.
	 * type - to indicate what kind of data to be subscribed. Possible types are:
	 * 		REALTIMEBAR
	 *      REALTIMETICK
	 *      HISTORICALBAR
	 *      HISTORICALTICK
	 */
	
	abstract public AbstractQuotesSubscriber Subscribe(int type, VContract c); 
	abstract public AbstractQuotesSubscriber UnSubscribe(int type, VContract c);
	
	/*
	 * For a specific contract, open/close the VIX calculation and streaming.
	 */
	abstract public AbstractQuotesSubscriber EnableVIX(VContract vc);
	abstract public AbstractQuotesSubscriber DisableVIX(VContract vc);

	public final String m_Name;
	
	// Bind to a provider, which is the reference to IB or CTP etc.
	protected final AbstractQuotesProvider m_Provider;
	
	/*
	 *  Collections to hold:
	 *  	Contract for which the real time bar data is subscribed
	 *  	Contract for which the real time tick data is subscribed
	 *  	Contract for which the historical bar data is subscribed
	 */
	protected final Set<VContract> m_RTBarVContracts = new HashSet<>();
	protected final Set<VContract> m_RTTickVContracts = new HashSet<>();
	protected final Set<VContract> m_HistBarVContracts = new HashSet<>();

	public AbstractQuotesSubscriber(String n, AbstractQuotesProvider p) {
		m_Name = n;
		m_Provider = p;
		if (p != null)
			p.AddSubscriber(this);
	}

	public AbstractQuotesProvider GetProvider() {
		return m_Provider;
	}

	public Set<VContract> GetRTBarSubscriptions() {
		return m_RTBarVContracts;
	}

	public Set<VContract> GetRTTickSubscriptions() {
		return m_RTTickVContracts;
	}

	public Set<VContract> GetHistBarSubscriptions() {
		return m_HistBarVContracts;
	}

}
