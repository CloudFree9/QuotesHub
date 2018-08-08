package com.cloudfree;

public interface IQuoteMessage {
	static int TYPE_TICK = 0;
	static int TYPE_BAR = 1;

	double GetOpen();

	double GetHigh();

	double GetLow();

	double GetClose();

	double GetBid();

	double GetAsk();

	double GetPrice();

	long GetVolume();

	long GetTimeStamp();

	long GetTimeStampMs();

	void SetOpen(double b);

	void SetHigh(double b);

	void SetLow(double b);

	void SetClose(double b);

	void SetBid(double b);

	void SetAsk(double a);

	void SetPrice(double p);

	void SetVolume(long v);

	void StampTimeStamp();

	void StampTimeStampMs();

	void StampTimeStamp(long ts);

	void StampTimeStampMs(long ts);

	byte[] StreamRepresentation();

	int GetType();

	VContract GetVContract();
}
