package com.cloudfree;

public interface IDestination {

	void Put(IQuoteMessage msg) throws Exception;

	void PutMessage(IQuoteMessage msg) throws Exception;

	IDestination ChainAnother(IDestination d);

	abstract public static class AbstractDestination implements IDestination {
		IDestination m_Next;

		@Override
		public AbstractDestination ChainAnother(IDestination d) {
			m_Next = d;
			return this;
		}

		@Override
		public void PutMessage(IQuoteMessage msg) throws Exception {

			Put(msg);

			if (null != m_Next)
				m_Next.PutMessage(msg);
		}
	}
}
