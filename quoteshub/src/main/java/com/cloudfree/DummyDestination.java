package com.cloudfree;

import com.cloudfree.IDestination.AbstractDestination;
import com.cloudfree.IB.IBQuotesBar;
import com.cloudfree.IB.IBQuotesTick;

public class DummyDestination extends AbstractDestination {

	@Override
	public void Put(IQuoteMessage msg) throws Exception {

		if (msg.getClass().equals(IBQuotesTick.class)) {
			System.out.printf(">> Tick - bid: %f, ask: %f, price: %f, vol: %d\n", msg.GetBid(), msg.GetAsk(),
					msg.GetPrice(), msg.GetVolume());
		} else if (msg.getClass().equals(IBQuotesBar.class)) {
			System.out.printf("<< Bar - open: %f, high: %f, low: %f, close: %f, vol: %d\n", msg.GetOpen(),
					msg.GetHigh(), msg.GetLow(), msg.GetClose(), msg.GetVolume());
		} else {
			System.out.printf(">> MSG: %s", msg.toString());
		}
	}

}
