package com.cloudfree;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public abstract class VContract implements Serializable {

	private static final long serialVersionUID = -8693147150651960457L;

	public abstract Object GetRealContract();

	@Override
	public abstract String toString();

	public abstract boolean EnableVIX();

	public abstract boolean DisableVIX();

	public abstract Object GetOptionChain();

	public abstract boolean IsVixEnabled();

	public abstract VContract Subscribe(int type);

	public abstract VContract UnSubscribe(int type);

	public final Set<Object> m_RTBarNotify = new HashSet<Object>();
	public final Set<Object> m_RTTickNotify = new HashSet<Object>();
	public final Set<Object> m_HistBarNotify = new HashSet<Object>();

	@Override
	public boolean equals(Object a1) {

		if (null == a1 || a1.getClass() != this.getClass())
			return false;
		if (this == a1)
			return true;

		VContract a = (VContract) a1;
		return GetRealContract().equals(a.GetRealContract());

	}

	@Override
	public int hashCode() {
		return GetRealContract().hashCode();
	}
}
