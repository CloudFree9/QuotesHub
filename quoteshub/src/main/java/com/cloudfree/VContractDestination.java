package com.cloudfree;

public abstract class VContractDestination implements IDestination {
	protected VContract m_Contract;

	public VContractDestination SetVContract(VContract c) {
		m_Contract = c;
		return this;
	}

	abstract protected VContractDestination Transform(IQuoteMessage msg);

}
