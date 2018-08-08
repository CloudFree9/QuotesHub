package com.cloudfree;

public interface IQuotesHub {
	IQuotesHub Init();

	IQuotesHub TearDown();

	IQuotesHub Start();

	IQuotesHub Stop();

	IQuotesHub Restart();

	IQuotesHub RegisterProvider(AbstractQuotesProvider provider);

	IQuotesHub UnRegisterProvider(String provider);

	AbstractQuotesProvider GetProviderByName(String name);
}
