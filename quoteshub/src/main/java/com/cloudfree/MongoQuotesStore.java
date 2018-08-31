package com.cloudfree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class MongoQuotesStore implements IMongoPersistence {
	
	private String m_ServerAddress = "127.0.0.1";
	private int m_Port = 27017;
	private String m_Database = "quotesdb";
	private MongoClient m_MongoClient = null;
	private MongoDatabase m_MongoDataBase = null;

	public MongoQuotesStore(String dbhost, int port, String dbname) {
		if (null != dbhost && "" != dbhost)
				m_ServerAddress = dbhost;
		
		if (port > 0)
			m_ServerAddress = dbhost;
		
		if (null != dbname && "" != dbname)
			m_Database = dbname;
		
	}
	
	public MongoQuotesStore() {
		this("", 0, "");
	}

	public MongoClient GetMongoClient() {
		return m_MongoClient;
	}

	@Override
	public String GetDBConfig() {
		return String.format("Host: %s, Port: %d, Database: %s", m_ServerAddress, m_Port, m_Database);
	}
	
	static public void main(String args[]) throws Exception {
		if (args.length < 1) {
			System.out.println("Usage: java IBQuotesStore <test_func>");
			System.out.println("\ttest_func can be:");
			System.out.println("\t\tDemoData");
			System.out.println("\t\tQueryData");
			System.exit(1);
		}
		
		if (args[0].equals("BuildData"))
			BuildData();
	}

	static public void BuildData() throws Exception {
		
        final String DBName = "quotes";
        final String ServerAddress = "127.0.0.1"; 
        final int PORT = 27017;
    
        MongoQuotesStore IBQuotesdb = new MongoQuotesStore(ServerAddress, PORT, DBName);
        IBQuotesdb.Connect();
        
		IBQuotesdb
		
		.Put("Instrument", new Document()
	        .append("contractid", 1234567)
	    	.append("timestamp", "20180827141900")
	    	.append("type","FUT")
	    	.append("price", 0.009036))
		
		.Put("Instrument", new Document()
		        .append("contractid", 1234567)
		    	.append("timestamp", "20180827141902")
		    	.append("type","FUT")
		    	.append("price", 0.009033))

		.Put("Instrument", new Document()
		        .append("contractid", 1234567)
		    	.append("timestamp", "20180827141904")
		    	.append("type","FUT")
		    	.append("price", 0.009037))

		.Put("Instrument", new Document()
		        .append("contractid", 1234568)
		    	.append("timestamp", "20180827141900")
		    	.append("type","FUT")
		    	.append("price", 0.007237))

		.Put("Instrument", new Document()
		        .append("contractid", 1234568)
		    	.append("timestamp", "20180827141904")
		    	.append("type","FUT")
		    	.append("price", 0.007234))
		
		.Put("Options", new Document()
		        .append("contractid", 1234568001)
		    	.append("timestamp", "20180827141900")
		    	.append("underlyingid", 123456800)
		    	.append("type","FOP")
		    	.append("price", 0.000038))
		
		.Put("VIX", new Document()
		        .append("contractid", 1234568001)
		    	.append("timestamp", "20180827141901")
		    	.append("underlyingid", 1234568)
		    	.append("type","CIND")
		    	.append("price", 6.32))
		
		.Put("VIX", new Document()
		        .append("contractid", 1234567001)
		    	.append("timestamp", "20180827141900")
		    	.append("underlyingid", 1234567)
		    	.append("type","CIND")
		    	.append("price", 6.28))

		.Put("VIX", new Document()
		        .append("contractid", 1234567001)
		    	.append("timestamp", "20180827141900")
		    	.append("underlyingid", 1234567)
		    	.append("type","CIND")
		    	.append("price", 8.07))

		.Put("VIX", new Document()
		        .append("contractid", 1234567001)
		    	.append("timestamp", "20180827141904")
		    	.append("underlyingid", 1234567)
		    	.append("type","CIND")
		    	.append("price", 7.9909990679));

		IBQuotesdb.Disconnect();
		
		System.out.println("Done!");
	}


	@Override
	public MongoQuotesStore Connect() throws Exception {
		
       	m_MongoClient = new MongoClient(m_ServerAddress, m_Port); 
        if (null == m_MongoClient)
			throw(new Exception("Failed to connect to DB server."));

       	m_MongoDataBase = m_MongoClient.getDatabase(m_Database);  
        if (null == m_MongoDataBase)
			throw(new Exception("Failed to connect to DB server."));

		return this;
	}


	@Override
	public void Disconnect() {
		if (null != m_MongoClient) m_MongoClient.close();
		
	}

	@Override
	public IMongoPersistence Put(String collection, Document d) throws Exception {
		m_MongoDataBase.getCollection(collection).insertOne(d);
		return this;
	}

	@Override
	public Collection<Document> Get(String collection, Bson query) throws Exception {
		List<Document> res = new ArrayList<Document>();
		m_MongoDataBase.getCollection(collection).find(query).into(res);
		return res;
	}
}
