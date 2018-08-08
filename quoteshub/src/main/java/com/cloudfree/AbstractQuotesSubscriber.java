package com.cloudfree;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractQuotesSubscriber implements Serializable {

	private static final long serialVersionUID = 2804676499407432113L;

	abstract public AbstractQuotesSubscriber Subscribe(int type, VContract c);

	abstract public AbstractQuotesSubscriber UnSubscribe(int type, VContract c);

	abstract public AbstractQuotesSubscriber EnableVIX(VContract vc);

	abstract public AbstractQuotesSubscriber DisableVIX(VContract vc);

	public final String m_Name;
	protected final AbstractQuotesProvider m_Provider;
	protected final Set<VContract> m_RTBarVContracts = new HashSet<>();
	protected final Set<VContract> m_RTTickVContracts = new HashSet<>();
	protected final Set<VContract> m_HistBarVContracts = new HashSet<>();

	public AbstractQuotesSubscriber(String n, AbstractQuotesProvider p) {
		m_Name = n;
		m_Provider = p;
		if (p != null)
			p.AddSubscriber(this);
	}

	public String GetName() {
		return m_Name;
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
