package com.cloudfree;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class GenQuotesHub implements IQuotesHub, Serializable {

	private static final long serialVersionUID = 1951079424256562584L;
	private List<AbstractQuotesProvider> m_Providers = new LinkedList<>();
	private static GenQuotesHub m_TheHub = new GenQuotesHub();

	public static GenQuotesHub GetInstance() {
		return m_TheHub == null ? new GenQuotesHub() : m_TheHub;
	}

	private GenQuotesHub() {
		Init();
	}

	public GenQuotesHub RegisterProviders(List<AbstractQuotesProvider> p) {

		m_Providers = p;
		if (m_Providers != null) {
			m_Providers.forEach(provider -> provider.AttachHost(this));
		}
		return this;
	}

	public List<AbstractQuotesProvider> GetProviders() {
		return m_Providers;
	}

	@Override
	public GenQuotesHub Init() {
		return this;
	}

	@Override
	public GenQuotesHub TearDown() {
		Stop();
		m_Providers.clear();
		return this;
	}

	@Override
	public GenQuotesHub Start() {
		if (m_Providers != null) {
			m_Providers.forEach(provider -> provider.Launch());
		}
		return this;
	}

	@Override
	public GenQuotesHub Stop() {
		if (m_Providers != null) {
			m_Providers.forEach(provider -> provider.Stop());
		}
		return this;
	}

	@Override
	public GenQuotesHub Restart() {

		Stop();
		Start();
		return this;
	}

	@Override
	public GenQuotesHub RegisterProvider(AbstractQuotesProvider provider) {

		if (provider == null)
			return this;
		if (m_Providers == null)
			m_Providers = new LinkedList<>();

		m_Providers.add(provider.AttachHost(this));
		return this;
	}

	@Override
	public GenQuotesHub UnRegisterProvider(String provider) {
		if (provider == null)
			return this;
		m_Providers = m_Providers.stream().filter(p -> !p.GetName().equals(provider)).collect(Collectors.toList());
		return this;
	}

	@Override
	public AbstractQuotesProvider GetProviderByName(String name) {
		return m_Providers.stream().filter(p -> p.GetName().equals(name)).findFirst().orElse(null);
	}

}
