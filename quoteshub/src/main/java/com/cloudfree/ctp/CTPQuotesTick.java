package com.cloudfree.ctp;

import com.cloudfree.GenQuoteMessage;
import com.cloudfree.VContract;
import com.ib.controller.ExtController.ICommonHandler;

public class CTPQuotesTick extends GenQuoteMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7547528418174636059L;
	String m_Instrument;

	public CTPQuotesTick(double ask, double bid, double price, long volume, long ts, long ts_ms, String inst,
			VContract c) {
		super(ask, bid, price, volume, ts, ts_ms, c);
		m_Instrument = inst;
	}

	public VContract GetContract() {
		// TODO Auto-generated method stub
		return m_Contract;
	}

	@Override
	public ICommonHandler GetSource() {
		// TODO Auto-generated method stub
		return null;
	}

}
