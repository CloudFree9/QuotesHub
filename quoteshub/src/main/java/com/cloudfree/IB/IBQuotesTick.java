package com.cloudfree.IB;

import com.cloudfree.GenQuoteMessage;
import com.cloudfree.IQuoteMessage;
import com.ib.client.Contract;
import com.ib.controller.ExtController.ICommonHandler;

public class IBQuotesTick extends GenQuoteMessage {

	private static final long serialVersionUID = -2977385995444461314L;
	private ICommonHandler m_Source;

	public IBQuotesTick(double ask, double bid, double price, long volume, long ts, long ts_ms, IBContract c,
			ICommonHandler source) {
		super(ask, bid, price, volume, ts, ts_ms, c);
		m_Source = source;
	}

	@Override
	public ICommonHandler GetSource() {
		return m_Source;
	}

	public IBContract GetContract() {
		return (IBContract) m_Contract;
	}

	public IBQuotesTick Clone() {
		return new IBQuotesTick(m_Ask, m_Bid, m_Price, m_Volume, m_TimeStamp, m_TimeStampMs, (IBContract) m_Contract,
				m_Source);
	}

	@Override
	public byte[] StreamRepresentation() {

		Contract c = (Contract) m_Contract.GetRealContract();
		String m;
		m = String.format("%d,%s,%d,%f,%f,%f,%d,%d,%d", IQuoteMessage.TYPE_TICK, c.symbol(), c.conid(), GetAsk(),
				GetBid(), GetPrice(), GetVolume(), GetTimeStamp(), GetTimeStampMs());

		byte buf[] = m.getBytes();
		Integer len = buf.length;
		byte l[] = new byte[4 + len];
		l[0] = (byte) ((len >> 24) & 0xff);
		l[1] = (byte) ((len >> 16) & 0xff);
		l[2] = (byte) ((len >> 8) & 0xff);
		l[3] = (byte) (len & 0xff);
		System.arraycopy(buf, 0, l, 4, len);
		return l;
	}
}
