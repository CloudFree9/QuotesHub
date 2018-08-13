package com.cloudfree.IB;

import java.util.List;
import java.util.stream.Stream;

import com.cloudfree.AbstractQuotesSubscriber;
import com.cloudfree.VContract;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ExtController.ICommonHandler;

/*
 * Class for IB as a quotes provider
 */

public class IBQuotesSubscriber extends AbstractQuotesSubscriber {

	private static final long serialVersionUID = 2760425961148453215L;

	public IBQuotesSubscriber(String n, IBQuotesProvider p) {
		super(n, p);
	}

	/*
	 * Given an IB Contract, get the ContractDetails list corresponding to the contract
	 */
	
	public List<ContractDetails> GetContractDetails(Contract con) throws Exception {

		if (null == con) {
			throw new Exception("Null contract passed to IBQuotesSubscriber::GetContractDetails()");
		}

		List<ContractDetails> res = null;
		int i = 0;

		EWrapperHandlers.ContractDetailsHandler handler = EWrapperHandlers.ContractDetailsHandler.GetInstance();

		while (i < 3) {
			try {
				synchronized (handler.m_SyncObj) {
					((IBQuotesProvider) GetProvider()).controller().reqContractDetails(con, handler);
					handler.m_SyncObj.wait();
					res = handler.GetContractDetailsList();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			i++;
			
			if (null != res) break;
		}

		return res;
	}

	@Override
	public IBQuotesSubscriber Subscribe(int type, VContract c) {

		if (null == c)	return this;

		IBContract con = (IBContract) c;

		switch (type) {
		case ICommonHandler.REALTIMEBAR:
			m_RTBarVContracts.add(con);
			break;
		case ICommonHandler.HISTORICALBAR:
			m_HistBarVContracts.add(con);
			break;
		case ICommonHandler.REALTIMETICK:
			m_RTTickVContracts.add(con);
			break;
		}
		con.Subscribe(type);
		return this;
	}

	public IBQuotesSubscriber Subscribe(int type, int conid) {
		Contract con = new Contract();
		con.conid(conid);

		try {
			List<ContractDetails> cds = GetContractDetails(con);
			if (null != cds && cds.size() > 0) {
				con = cds.get(0).contract();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		IBContract ibcon = new IBContract(con, this);
		return Subscribe(type, ibcon);
	}

	@Override
	public IBQuotesSubscriber UnSubscribe(int type, VContract c) {

		if (null == c)	return this;

		IBContract con = (IBContract) c;

		switch (type) {
		case ICommonHandler.REALTIMEBAR:
			m_RTBarVContracts.remove(con);
			break;
		case ICommonHandler.HISTORICALBAR:
			m_RTTickVContracts.remove(con);
			break;
		case ICommonHandler.REALTIMETICK:
			m_HistBarVContracts.remove(con);
			break;
		}

		con.UnSubscribe(type);
		return this;
	}

	@Override
	public IBQuotesSubscriber EnableVIX(VContract vc) {
		if (vc == null || (!m_RTBarVContracts.contains(vc) && !m_RTTickVContracts.contains(vc)
				&& !m_HistBarVContracts.contains(vc)))
			return this;

		vc.EnableVIX();
		return this;
	}

	@Override
	public IBQuotesSubscriber DisableVIX(VContract vc) {
		if (null == vc || (!m_RTBarVContracts.contains(vc) && !m_RTTickVContracts.contains(vc)
				&& !m_HistBarVContracts.contains(vc)))
			return this;

		vc.DisableVIX();
		return this;
	}

	public IBQuotesSubscriber EnableAllVIX() {
		Stream.of(m_RTBarVContracts, m_RTTickVContracts).flatMap(e -> e.stream()).distinct().forEach(vc -> {
			EnableVIX(vc);
		});
		return this;
	}

	public IBContract FindIBContractByID(int conid) {
		return (IBContract) Stream.of(m_RTBarVContracts, m_RTTickVContracts).flatMap(e -> e.stream()).filter(e -> {
			Contract c = (Contract) e.GetRealContract();
			return c.conid() == conid;
		}).findAny().orElse(null);
	}

	public void Refresh() {
		Stream.of(m_RTBarVContracts, m_RTTickVContracts).flatMap(e -> e.stream()).distinct().forEach(vc -> {
			UnSubscribe(ICommonHandler.REALTIMETICK, vc);
			Subscribe(ICommonHandler.REALTIMETICK, vc);
			IBContract ibcon = (IBContract) vc;
			ibcon.GetOptionChain().GetOptionMap().Refresh();
		});

	}
}
