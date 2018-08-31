package com.cloudfree;

import java.io.Serializable;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

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
	public final Semaphore m_Mutex = new Semaphore(1);
	
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

	protected Boolean m_IsWeekend = false;
	abstract public boolean AllInstrumentsOffTime(); 
	
	public AbstractQuotesSubscriber(String n, AbstractQuotesProvider p) {
		m_Name = n;
		m_Provider = p;
		if (p != null)
			p.AddSubscriber(this);
		
		new Thread() {
			
			public void run() {
				
				setName("Weekend detection thread");
				
				while (true) {
					Calendar c = Calendar.getInstance();
					if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
						m_IsWeekend = true;
					} else {
						m_IsWeekend = false;
					}
					
					if (m_IsWeekend && AllInstrumentsOffTime()) {
						System.out.println("Caution: It's weekend and all instruments are in off time.");
					}
					
					Calendar c1 = (Calendar) c.clone();
					c1.set(Calendar.HOUR_OF_DAY, 0);
					c1.set(Calendar.MINUTE, 0);
					c1.set(Calendar.SECOND, 1);
					c1.add(Calendar.DATE, 1);

					try {
						Thread.sleep(c1.get(Calendar.MILLISECOND) - c.get(Calendar.MILLISECOND));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
			}
		}.start();
	}

	public boolean IsWeekend() {
		return m_IsWeekend;
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
