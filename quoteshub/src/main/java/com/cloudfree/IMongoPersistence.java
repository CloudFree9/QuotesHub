package com.cloudfree;

import java.util.Collection;
import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoCollection;

public interface IMongoPersistence {

	IMongoPersistence Connect() throws Exception;
	void Disconnect();
	IMongoPersistence Put(String collection, Document d);
	Collection<Document> Get(String collection, Bson query);
	
}
