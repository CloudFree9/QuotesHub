package com.cloudfree.IB;

import com.cloudfree.GenQuoteMessage;
import com.cloudfree.IQuoteMessage;
import com.cloudfree.VContract;
import com.ib.client.Contract;
import com.ib.controller.ExtController.ICommonHandler;

public class IBQuotesBar extends GenQuoteMessage {

	private static final long serialVersionUID = 7301720009994264688L;
	private ICommonHandler m_Source;

	public IBQuotesBar(double open, double high, double low, double close, long volume, long ts, long ts_ms,
			VContract vc, ICommonHandler source) {
		super(open, high, low, close, volume, ts, ts_ms, vc);
		m_Source = source;
	}

	@Override
	public ICommonHandler GetSource() {
		return m_Source;
	}

	public IBContract GetContract() {
		return (IBContract) m_Contract;
	}

	@Override
	public byte[] StreamRepresentation() {

		Contract c = (Contract) m_Contract.GetRealContract();
		String m;
		m = String.format("%d,%s,%d,%f,%f,%f,%f,%d,%d,%d", IQuoteMessage.TYPE_BAR, c.symbol(), c.conid(), GetOpen(),
				GetHigh(), GetLow(), GetClose(), GetVolume(), GetTimeStamp(), GetTimeStampMs());

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
