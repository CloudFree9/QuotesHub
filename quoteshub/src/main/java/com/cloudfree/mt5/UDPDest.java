package com.cloudfree.mt5;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import com.cloudfree.GenLogger;
import com.cloudfree.IDestination.AbstractDestination;
import com.cloudfree.IQuoteMessage;

public class UDPDest extends AbstractDestination {

	private static class InvalidNetworkException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 3670229714688658857L;
		private String m_Why;

		public InvalidNetworkException(String why) {
			m_Why = why;
		}

		@Override
		public String toString() {
			return m_Why;
		}
	}

	private String m_Host = "localhost";
	private int m_Port;
	private InetAddress m_Addr;
	private DatagramSocket m_Socket = null;

	public UDPDest(String h, int p) {
		m_Host = h;
		m_Port = p;
		Bind();
	}

	private void Bind() {
		try {
			m_Addr = InetAddress.getByName(m_Host);
			m_Socket = new DatagramSocket();
		} catch (UnknownHostException e) {
			GenLogger.DFT_LOGGER.log(e.toString());
			m_Addr = null;
		} catch (SocketException e) {
			GenLogger.DFT_LOGGER.log(e.toString());
			m_Socket = null;
		} catch (Exception e) {
			GenLogger.DFT_LOGGER.log(e.toString());
			m_Addr = null;
			m_Socket = null;
		}

	}

	@Override
	public void Put(IQuoteMessage msg) throws Exception {
		if (m_Addr == null || m_Socket == null) {
			throw new InvalidNetworkException("UDP Network connection invalid");
		}

		byte l[] = msg.StreamRepresentation();
		byte m[] = new byte[l.length - 4];
		System.arraycopy(l, 4, m, 0, l.length - 4);

		System.out.printf("Addr: %s:%d\n%s\n", m_Addr.toString(), m_Port, new String(m));
		DatagramPacket sendPacket = new DatagramPacket(l, l.length, m_Addr, m_Port);

		try {
			m_Socket.send(sendPacket);
			System.out.printf("### MSG Sent: %s ###\n", m);
		} catch (IOException e) {
			GenLogger.DFT_LOGGER.log(e.toString());
		}

	}

	public InetAddress GetIPAddress() {
		return m_Addr;
	}

	public int GetPort() {
		return m_Port;
	}

	public String GetHost() {
		return m_Host;
	}

	@Override
	public int hashCode() {
		return m_Addr.hashCode() + m_Port;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || this.getClass() != o.getClass())
			return false;

		UDPDest uo = (UDPDest) o;
		return uo.GetIPAddress().equals(m_Addr) && uo.GetPort() == m_Port;

	}
}
