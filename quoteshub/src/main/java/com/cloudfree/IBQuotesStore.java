package com.cloudfree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.bson.Document;
import org.bson.conversions.Bson;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class IBQuotesStore implements IMongoPersistence {
	
	private String m_ServerAddress;
	private int m_Port;
	private String m_Database;
	private MongoClient m_MongoClient = null;
	private MongoDatabase m_MongoDataBase = null;

	public IBQuotesStore(String dbhost, int port, String dbname) {
		m_ServerAddress = dbhost;
		m_Port = port;
		m_Database = dbname;
		
	}
	
	public IBQuotesStore(String dbname) {
		this("127.0.0.1", 27017, dbname);
	}
	
	static public void main(String args[]) {
		
        final String DBName = "quotes";
        final String ServerAddress = "192.168.31.27"; 
        final int PORT = 27017;
        
        MongoClient mongoClient = null;
        MongoDatabase mongoDataBase = null;
        
        try {
            mongoClient = new MongoClient(ServerAddress, PORT); 
            System.out.println("Connect to mongodb successfully");
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(1);
        }
		
        try {  
            if (mongoClient != null) {  
                mongoDataBase = mongoClient.getDatabase(DBName);  
                System.out.println("Connect to DataBase successfully");
            } else {  
                throw new RuntimeException("MongoClient²»ÄÜ¹»Îª¿Õ");  
            }  
        } catch (Exception e) {  
            e.printStackTrace();  
            System.exit(1);
        }
        
        MongoCollection<Document> collection = mongoDataBase.getCollection("JPY_Future");
        Document doc = new Document();
        doc.put("timestamp", 20180812);
        doc.put("price", 0.009550);
        
        collection.insertOne(doc);
        
        mongoClient.close();
	}


	@Override
	public IBQuotesStore Connect() throws Exception {
		
        try {
        	m_MongoClient = new MongoClient(m_ServerAddress, m_Port); 
        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
        }
        
        if (null == m_MongoClient)
			throw(new Exception("Failed to connect to DB server."));

        try {  
        	m_MongoDataBase = m_MongoClient.getDatabase(m_Database);  
        } catch (Exception e) {  
            e.printStackTrace();  
        }
        
        if (null == m_MongoDataBase)
			throw(new Exception("Failed to connect to DB server."));

		return this;
	}


	@Override
	public void Disconnect() {
		if (null != m_MongoClient) m_MongoClient.close();
		
	}

	@Override
	public IMongoPersistence Put(String collection, Document d) {
		MongoCollection<Document> c = m_MongoDataBase.getCollection(collection);
        c.insertOne(d);
		return this;
	}

	@Override
	public Collection<Document> Get(String collection, Bson query) {
		List<Document> res = new ArrayList<Document>();
		MongoCollection<Document> c = m_MongoDataBase.getCollection(collection);
		if (null != query)
			c.find(query).into(res);
		else
			c.find().into(res);
		return res;
	}
}
