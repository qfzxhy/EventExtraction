package cetc28.java.solrtool;
import org.apache.solr.client.solrj.impl.CloudSolrClient;

import cetc28.java.config.DBConfig;

public class GetCloudSolrClient {

	//private static String defaultCollection = DBConfig.getDEFAULT_COLLECTION_CRAWLER();
	public static CloudSolrClient getInstance(String defaultCollection){
        CloudSolrClient cloudSolrClient = new CloudSolrClient.Builder().withZkHost(DBConfig.getZOOKEEPER_HOSTS()).build();  
        final int zkClientTimeout = 60000;  
        final int zkConnectTimeout = 60000;  
        cloudSolrClient.setDefaultCollection(defaultCollection);  
        cloudSolrClient.setZkClientTimeout(zkClientTimeout);  
        cloudSolrClient.setZkConnectTimeout(zkConnectTimeout);  
		return cloudSolrClient;
	}
	
	public static CloudSolrClient getInstance(String zkHosts,String defaultCollection){
        CloudSolrClient cloudSolrClient = new CloudSolrClient.Builder().withZkHost(zkHosts).build();  
        final int zkClientTimeout = 60000;  
        final int zkConnectTimeout = 60000;  
        cloudSolrClient.setDefaultCollection(defaultCollection);  
        cloudSolrClient.setZkClientTimeout(zkClientTimeout);  
        cloudSolrClient.setZkConnectTimeout(zkConnectTimeout);  
		return cloudSolrClient;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

		CloudSolrClient client = GetCloudSolrClient.getInstance(DBConfig.getDEFAULT_COLLECTION_CRAWLER());
	}

}
