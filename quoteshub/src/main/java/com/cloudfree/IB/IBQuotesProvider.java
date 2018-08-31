package com.cloudfree.IB;

import java.io.File;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import com.cloudfree.AbstractQuotesProvider;
import com.cloudfree.GenLogger;
import com.cloudfree.MongoQuotesStore;
import com.cloudfree.IB.EWrapperHandlers.ConnectionHandler;
import com.ib.controller.ExtController;

public class IBQuotesProvider extends AbstractQuotesProvider implements Runnable {

	private	static final long serialVersionUID = -4038373255083917871L;
	private	ExtController m_Controller = null;
	private	String m_TWSHost = "127.0.0.1";
	private	int m_TWSPort = 7496;
	private	int m_ClientID = 2018;
	private	String m_ConnectOptions = "";
	private	PropertiesConfiguration m_Configurations = null;
	private	GenLogger m_inLogger = new GenLogger("IBQuotesProviderIn", GenLogger.INFO);
	private	GenLogger m_outLogger = new GenLogger("IBQuotesProviderOut", GenLogger.INFO);
	private	static String DFT_CONF_FILE = "ibprovider.properties";
	public	final Object m_ConnectedSignal = new Object();
	public	final Object m_DisconnectedSignal = new Object();
	public	Boolean m_RestartSignal = new Boolean(false);
	private	ConnectionHandler m_ConnectionHandler = EWrapperHandlers.ConnectionHandler.GetInstance(this);
	private int m_VixServePerSubs = 30;
	private int m_VixServePerSession = 300;
	
	public	IBQuotesProvider(String name, String configfile) {

		m_Name = name;
		try {
			m_Configurations = new Configurations().properties(new File(configfile));
			m_TWSHost			= m_Configurations.getString("tws.host", m_TWSHost);
			m_TWSPort			= m_Configurations.getInt("tws.port", m_TWSPort);
			m_ClientID			= m_Configurations.getInt("tws.clientid", m_ClientID);
			m_ConnectOptions	= m_Configurations.getString("tws.connect_opts", m_ConnectOptions);
			
			String dbhost		= m_Configurations.getString("db.host", "");
			int port			= m_Configurations.getInt("db.port", 27017);
			String dbname		= m_Configurations.getString("db.database", "quotesdb");
			m_VixServePerSubs	= m_Configurations.getInt("vix_serve_per_subscription", m_VixServePerSubs);
			m_VixServePerSession	= m_Configurations.getInt("max_vix_serve_per_instance", m_VixServePerSession);
			
			m_Persistence		= new MongoQuotesStore(dbhost, port, dbname);

		} catch (ConfigurationException e) {
			m_inLogger.log("errors on loading TWS configuration.");
			m_inLogger.log(e.toString());
		}

	}

	public IBQuotesProvider(String name) {
		this(name, DFT_CONF_FILE);
	}

	public void SetLastErrorCode(int code) {
	}

	public void DumpConfig() {
		m_outLogger.log(String.format("TWS Host: %s", m_TWSHost));
		m_outLogger.log(String.format("TWS Port: %d", m_TWSPort));
		m_outLogger.log(String.format("TWS ClientID: %d", m_ClientID));
		m_outLogger.log(String.format("TWS extra options: %s", m_ConnectOptions));
		m_outLogger.log(String.format("MongoDB: %s", m_Persistence.GetDBConfig()));
	}

	public ExtController controller() {
		if (m_Controller == null) {
			m_Controller = new ExtController(m_ConnectionHandler, m_inLogger, m_outLogger, this);
		}
		return m_Controller;
	}

	@Override
	public IBQuotesProvider Launch() {

		new Thread(this).start();

		try {
			synchronized (m_ConnectedSignal) {
				m_ConnectedSignal.wait();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return this;
	}

	public int GetVixServePerSubs() {
		return m_VixServePerSubs;
	}
	
	public int GetVixServePerSession() {
		return m_VixServePerSession;
	}
	
	public void Refresh() {
		m_Subscribers.forEach(e -> {
			((IBQuotesSubscriber) e).Refresh();
		});
	}

	@Override
	public void run() {
		synchronized (m_ConnectedSignal) {
			controller().connect(m_TWSHost, m_TWSPort, m_ClientID, m_ConnectOptions);
		}
		
		try {
			m_Persistence.Connect();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String GetProviderName() {
		return m_Name;
	}

	@Override
	public IBQuotesProvider Stop() {
		m_Controller.disconnect();
		return this;
	}
}
