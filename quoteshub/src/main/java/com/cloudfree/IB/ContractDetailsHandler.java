package com.cloudfree.IB;

import java.util.LinkedList;
import java.util.List;

import com.ib.client.Contract;
import com.ib.client.ContractDetails;
import com.ib.controller.ExtController.IContractDetailsHandler;

public class ContractDetailsHandler implements IContractDetailsHandler {

	protected class GenContractDetailsException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4012568860647901767L;
		protected Contract m_Contract;

		public GenContractDetailsException(Contract c) {
			m_Contract = c;
		}

		@Override
		public String toString() {
			return "GenContractDetailsException";
		}
	}

	public class NoContractDetailsException extends GenContractDetailsException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2577118680233275912L;

		public NoContractDetailsException(Contract c) {
			super(c);
		}

		@Override
		public String toString() {
			return String.format("Contact: {%s} doesn't exist.", m_Contract.toString());
		}
	}

	public class MultipleContractDetailsException extends GenContractDetailsException {

		/**
		 * 
		 */
		private static final long serialVersionUID = -3089666784915234353L;

		public MultipleContractDetailsException(Contract c) {
			super(c);
		}

		@Override
		public String toString() {
			return String.format("Contact: {%s} is not unique.", m_Contract.toString());
		}
	}

	private Contract m_Contract;
	private LinkedList<ContractDetails> m_List;

	public ContractDetailsHandler(Contract c) {
		m_Contract = c;
		m_List = new LinkedList<ContractDetails>();
	}

	public Contract GetContract() {
		return m_Contract;
	}

	public LinkedList<ContractDetails> GetContractDetails() {
		return m_List;
	}

	@Override
	public void contractDetails(List<ContractDetails> list) {
		m_List.clear();

		if (list == null || list.size() == 0) {
			try {
				throw new NoContractDetailsException(m_Contract);
			} catch (NoContractDetailsException e) {
				e.printStackTrace();
			}
		}

		m_List.addAll(list);
		synchronized (this) {
			notify();
		}
	}

	@Override
	public int GetReqId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int GetType() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void SetReqId(int reqId) {
		// TODO Auto-generated method stub

	}

}
