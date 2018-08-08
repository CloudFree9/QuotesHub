package com.cloudfree;

import java.io.Serializable;

public abstract class GenQuoteMessage implements IQuoteMessage, Serializable {

	private static final long serialVersionUID = 1223731166723008255L;
	protected double m_Open;
	protected double m_High;
	protected double m_Low;
	protected double m_Close;
	protected double m_Ask;
	protected double m_Bid;
	protected double m_Price;
	protected long m_Volume;
	protected long m_TimeStamp;
	protected long m_TimeStampMs;
	protected VContract m_Contract;
	protected int m_TickType;
	protected int m_Type;

	abstract public Object GetSource();

	public GenQuoteMessage(double ask, double bid, double price, long volume, long ts, long ts_ms, VContract vc) {

		m_Ask = ask;
		m_Bid = bid;
		m_Price = price;
		m_Volume = volume;
		m_Contract = vc;
		m_Type = IQuoteMessage.TYPE_TICK;

		if (ts != -1) {
			m_TimeStamp = ts;
		} else {
			StampTimeStamp();
		}

		if (ts_ms != -1) {
			m_TimeStampMs = ts_ms;
		} else {
			StampTimeStampMs();
		}

	}

	public GenQuoteMessage(double open, double high, double low, double close, long volume, long ts, long ts_ms,
			VContract vc) {

		m_Open = open;
		m_High = high;
		m_Low = low;
		m_Close = close;
		m_Volume = volume;
		m_TimeStamp = ts;
		m_TimeStampMs = ts_ms;
		m_Contract = vc;
		m_Type = IQuoteMessage.TYPE_BAR;

		if (ts != -1) {
			m_TimeStamp = ts;
		} else {
			StampTimeStamp();
		}

		if (ts_ms != -1) {
			m_TimeStampMs = ts_ms;
		} else {
			StampTimeStampMs();
		}

		if (ts != -1) {
			m_TimeStamp = ts;
		} else {
			StampTimeStamp();
		}

		if (ts_ms != -1) {
			m_TimeStampMs = ts_ms;
		} else {
			StampTimeStampMs();
		}

	}

	@Override
	public VContract GetVContract() {
		return m_Contract;
	}

	@Override
	public double GetOpen() {
		return m_Open;
	}

	@Override
	public double GetHigh() {
		return m_High;
	}

	@Override
	public double GetLow() {
		return m_Low;
	}

	@Override
	public double GetClose() {
		return m_Close;
	}

	@Override
	public int GetType() {
		return m_Type;
	}

	@Override
	public double GetBid() {
		return m_Bid;
	}

	@Override
	public double GetAsk() {
		return m_Ask;
	}

	@Override
	public double GetPrice() {
		return m_Price;
	}

	@Override
	public long GetVolume() {
		return m_Volume;
	}

	@Override
	public void StampTimeStamp() {
		m_TimeStampMs = System.currentTimeMillis();
		m_TimeStamp = m_TimeStampMs / 1000;

	}

	@Override
	public void StampTimeStampMs() {
		m_TimeStampMs = System.currentTimeMillis();
		m_TimeStamp = m_TimeStampMs / 1000;
	}

	@Override
	public byte[] StreamRepresentation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void StampTimeStamp(long ts) {
		m_TimeStampMs = ts * 1000;
		m_TimeStamp = ts;

	}

	@Override
	public void StampTimeStampMs(long ts) {
		m_TimeStampMs = ts;
		m_TimeStamp = ts / 1000;
	}

	@Override
	public void SetBid(double b) {
		m_Bid = b;
	}

	@Override
	public void SetAsk(double a) {
		m_Ask = a;
	}

	@Override
	public void SetPrice(double p) {
		m_Price = p;
	}

	@Override
	public void SetVolume(long v) {
		m_Volume = v;
	}

	@Override
	public long GetTimeStamp() {
		return m_TimeStamp;
	}

	@Override
	public long GetTimeStampMs() {
		return m_TimeStampMs;
	}

	@Override
	public void SetOpen(double o) {
		m_Open = o;
	}

	@Override
	public void SetHigh(double h) {
		m_High = h;

	}

	@Override
	public void SetLow(double l) {
		m_Low = l;
	}

	@Override
	public void SetClose(double c) {
		m_Close = c;
	}

}
