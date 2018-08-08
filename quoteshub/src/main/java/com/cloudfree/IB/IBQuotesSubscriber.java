package com.cloudfree.IB;

import java.util.List;
import java.util.stream.Stream;

import com.cloudfree.AbstractQuotesSubscriber;
import com.cloudfree.VContract;
import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ExtController.ICommonHandler;
import com.ib.controller.ExtController.IConnectionHandler;

public class IBQuotesSubscriber extends AbstractQuotesSubscriber {

	private static final long serialVersionUID = 2760425961148453215L;

	public IBQuotesSubscriber(String n, IBQuotesProvider p) {
		super(n, p);
	}

	public List<ContractDetails> GetContractDetails(Contract con) throws Exception {

		if (con == null) {
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
			if (res != null)
				break;
		}

		return res;
	}

	public ContractDetails GetOneContractDetails(Contract con) throws Exception {

		if (con == null) {
			throw new Exception("Null contract passed to IBQuotesSubscriber::GetOneContractDetails()");
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
			if (res != null)
				break;
		}

		return res.size() == 1 ? res.get(0) : null;
	}

	public ContractDetails GetOneContractDetails(int conid) throws Exception {

		if (conid <= 0) {
			throw new Exception("Zero passed to IBQuotesSubscriber::GetOneContractDetails(int)");
		}

		Contract con = new Contract();
		con.conid(conid);

		return GetOneContractDetails(con);
	}

	@Override
	public IBQuotesSubscriber Subscribe(int type, VContract c) {

		if (c == null)
			return this;

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
			if (cds != null && cds.size() > 0) {
				con = cds.get(0).contract();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		IBContract ibcon = new IBContract(con, this);
		return Subscribe(type, ibcon);
	}

	@Override
	public IBQuotesSubscriber UnSubscribe(int type, VContract c) {

		if (c == null)
			return this;

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
		if (vc == null || (!m_RTBarVContracts.contains(vc) && !m_RTTickVContracts.contains(vc)
				&& !m_HistBarVContracts.contains(vc)))
			return this;

		vc.DisableVIX();
		return this;
	}

	public IBQuotesSubscriber EnableAllVIX() {
		Stream.of(m_RTBarVContracts, m_RTTickVContracts).flatMap(e -> e.stream()).distinct().forEach(vc -> {
			System.out.printf("vc : %s\n", vc.toString());
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
			System.out.printf("vc : %s re-subscribe\n", vc.toString());
			UnSubscribe(ICommonHandler.REALTIMETICK, vc);
			Subscribe(ICommonHandler.REALTIMETICK, vc);
			IBContract ibcon = (IBContract) vc;
			ibcon.GetOptionChain().GetOptionMap().Refresh();
		});

	}
}
