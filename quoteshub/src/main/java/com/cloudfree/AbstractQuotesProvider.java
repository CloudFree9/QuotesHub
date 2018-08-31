package com.cloudfree; 

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public abstract class AbstractQuotesProvider implements Serializable {

	private static final long serialVersionUID = 3352443003188468565L;
	protected IQuotesHub m_QHub;
	protected String m_Name;
	protected IMongoPersistence m_Persistence;

	protected Set<AbstractQuotesSubscriber> m_Subscribers = new HashSet<>();

	abstract public AbstractQuotesProvider Launch();
	abstract public AbstractQuotesProvider Stop();

	public AbstractQuotesSubscriber GetSubscriberByName(String name) {
		return m_Subscribers.stream().filter(s -> s.m_Name
				.equals(name)).findFirst().orElse(null);
	}

	public AbstractQuotesProvider AddSubscriber(AbstractQuotesSubscriber s) {
		if (null != s)	m_Subscribers.add(s);
		return this;
	}

	public AbstractQuotesProvider RemoveSubscriber(AbstractQuotesSubscriber s) {
		if (null != s)	m_Subscribers.remove(s);
		return this;
	}

	public AbstractQuotesProvider RemoveSubscriber(String s) {
		AbstractQuotesSubscriber sub = GetSubscriberByName(s);
		if (null != sub) m_Subscribers.remove(sub);
		return this;
	}

	public IMongoPersistence GetPersistenceStore() {
		return m_Persistence;
	}
	
	public String GetName() {
		return m_Name;
	}

	public IQuotesHub GetQuotesHub() {
		return m_QHub;
	}

	public AbstractQuotesProvider AttachHost(IQuotesHub qh) {
		m_QHub = qh;
		return this;
	}

	public Set<AbstractQuotesSubscriber> GetSubscribers() {
		return m_Subscribers;
	}

}
