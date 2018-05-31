package cetc28.java.solrtool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.FacetField.Count;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import cetc28.java.config.DBConfig;
import cetc28.java.solrtool.GeoEncoder.Place;
import cetc28.java.solrtool.GeoEncoder.SearchResultWithFacet;
import cetc28.java.solrtool.GetCloudSolrClient;

public class GeoEncodeSearch {
	private CloudSolrClient cloudClient = null;
	
	public GeoEncodeSearch() {
		cloudClient = GetCloudSolrClient.getInstance(DBConfig.getDEFAULT_COLLECTION_GEOENCODE());
	}
	public GeoEncodeSearch(String zkHosts,String collname) {
		cloudClient = GetCloudSolrClient.getInstance(zkHosts,collname);
	}
	
	public void removeByID(String id) throws SolrServerException, IOException{
		cloudClient.deleteById(id);
	}
	public void batchRemoveByIDs(List<String> ids) throws SolrServerException, IOException{
		cloudClient.deleteById(ids);
	}
	public void batchAddFromBeans(List<Place> places) throws SolrServerException, IOException{
		cloudClient.addBeans(places);
		UpdateResponse ur = cloudClient.commit();
		System.out.println(ur.getStatus());
	}
	public List<Place> searchByID(String id) throws SolrServerException, IOException{
		String querystring = "id:\""+id+"\"";
		ModifiableSolrParams sp = new ModifiableSolrParams();
		sp.set("q", querystring);
		QueryResponse qr = cloudClient.query(sp);
		return qr.getBeans(Place.class);
	}
	
	public boolean searchByQuery(SolrQuery query, SearchResultWithFacet result) throws SolrServerException, IOException{
		QueryResponse qr = cloudClient.query(query);
		if(qr.getStatus()!=0)
			return false;
		long totalnum = qr.getResults().getNumFound();
		result.setTotalnumfound(totalnum);
		List<FacetField> facetfields = qr.getFacetFields();
		for(FacetField ff:facetfields){
			List<Count> count = ff.getValues();
			for(Count c:count){
				result.appendFacetMap(ff.getName(), c.getName(), c.getCount());
			}
		}
		result.setPlaces(qr.getBeans(Place.class));
		return true;
	}
	
	public List<Place> searchByQuery(SolrQuery query) throws SolrServerException, IOException{
		QueryResponse qr = cloudClient.query(query);		
		if(qr.getStatus()!=0)
			return null;
		return qr.getBeans(Place.class);
	}
	
	public UpdateResponse insertIndex(SolrInputDocument input) throws SolrServerException, IOException{
		UpdateResponse ur = cloudClient.add(input);
		cloudClient.commit();
		return ur;
	}
	
	public UpdateResponse insertIndex(Place input) throws SolrServerException, IOException{
		UpdateResponse ur = cloudClient.addBean(input);
		cloudClient.commit();
		return ur;
	}
	/**
	 * 指定返回条数的查询
	 * @param query 查询语句
	 * @param start 开始返回的id
	 * @param rows 返回的条数
	 * @return
	 */
	public SolrDocumentList searchByQuery(String query, String start, String rows) {
		return searchByQuery(query, new ArrayList<String>(), start, rows);
	}
	/**
	 * 指定返回条数和返回数据域的查询
	 * @param query 查询语句
	 * @param includeFields 包含的数据域
	 * @param start 开始返回的id
	 * @param rows 返回的条数
	 * @return
	 */
	public SolrDocumentList searchByQuery(String query, List<String> includeFields, int start, int newsNum) {


			SolrDocumentList list = searchByQuery(query, includeFields, Integer.toString(start), Integer.toString(newsNum));
			
			return list;
	}
	public SolrDocumentList searchByQuery(String query, List<String> includeFields, String start, String rows) {
		StringBuffer fl = new StringBuffer();
		if (includeFields.size() == 0) { //返回数据域默认包含所有
			fl.append("*");
		} else {
			for(String field : includeFields) {
				fl.append(field);
				fl.append(",");
			}
		}
		SolrDocumentList sdl = new SolrDocumentList();
		try {
			ModifiableSolrParams msp = new ModifiableSolrParams();
			msp.set("q", query);
//			msp.set("sort", "publishTime asc"); //按日期时间升序排列
			msp.set("fl", fl.toString());
			msp.set("start", start);
			msp.set("rows", rows);
			QueryResponse qresponse = cloudClient.query(msp);
			sdl = qresponse.getResults();
		} catch (SolrServerException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return sdl;
	}
	/**
	 * 返回所有符合条件文档的查询
	 * @param query 查询语句
	 * @return
	 */
	public SolrDocumentList searchByQuery(String query) {
		return searchByQuery(query, new ArrayList<String>());
	}
	/**
	 * 返回所有符合条件文档的查询（指定返回数据域）
	 * @param query 查询语句
	 * @param includeFields 包含的数据域
	 * @return
	 */
	public SolrDocumentList searchByQuery(String query, List<String> includeFields) {
		int start = 0; //起始id
		int includeRows = 10000; //每页条数
		SolrDocumentList List = new SolrDocumentList();
		while(true) {
			SolrDocumentList temp = searchByQuery(query, includeFields, Integer.toString(start), Integer.toString(includeRows));
			if(temp.size() == 0) {
				break;
			}
			else {
				List.addAll(temp);
				start += includeRows;
			}
		}
		return List;
	}
	public SolrDocumentList searchByDate(String date, List<String> includeFields, int start, int newsNum)
	{
		String query = "publishDate:"+date+"000000";
		SolrDocumentList list = searchByQuery(query, includeFields, start, newsNum);
		return list;
	}
	public SolrDocumentList searchByDate(String date, List<String> includeFields)
	{
		String query = "publishDate:"+date+"000000";
		SolrDocumentList list = searchByQuery(query, includeFields);
		return list;
	}
	public SolrDocumentList searchByQuery(String[] fields, String[] keys, int start,  
            int count, String[] sortfield, Boolean[] flag)
	{
		 if (null == fields || null == keys || fields.length != keys.length) {  
	            return null;  
	        }  
//	        if (null == sortfield || null == flag  
//	                || sortfield.length != flag.length) {  
//	            return null;  
//	        } 
	        SolrQuery query = null;
	        try {  
	            // 初始化查询对象  
	            query = new SolrQuery(fields[0] + ":" + keys[0]);  
	            for (int i = 0; i < fields.length; i++) {  
	                query.addFilterQuery(fields[i] + ":" + keys[i]);  
	            }  
	            // 设置起始位置与返回结果数  
	            query.setStart(start);  
	            query.setRows(count);  
	            // 设置排序  
	            if (null != sortfield && null != flag)
	            for (int i = 0; i < sortfield.length; i++) {  
	                if (flag[i]) {  
	                    query.addSort(sortfield[i], SolrQuery.ORDER.asc);  
	                } else {  
	                    query.addSort(sortfield[i], SolrQuery.ORDER.desc);  
	                }  
	            }  
	        } catch (Exception e) {  
	            e.printStackTrace();  
	        }  
	        QueryResponse rsp = null;  
	       
	            try {
					rsp = cloudClient.query(query);
				} catch (SolrServerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	            SolrDocumentList sdl = rsp.getResults();
		return sdl;
		
	}
	
	/**
	 * @param lat
	 * @param lon
	 * @param distance
	 * @param queryString
	 * @return
	 */
	public SolrDocumentList searchByLocation(String lat,String lon,String distance,String queryString){
		SolrQuery params = new SolrQuery();
		// alternate_ch_names:\"徐庄\"
		params.set("q", queryString);    
		params.set("fq", "{!geofilt}");//距离过滤函数
		params.set("pt", lon+" "+lat); //当前经纬度
		params.set("sfield", "location"); //经纬度的字段
		params.set("d", distance); //就近 d km的所有数据
		params.set("sort", "geodist() asc");  //根据距离排序：由近到远
		params.set("start", "0");  //记录开始位置
		params.set("rows", "100");  //查询的行数
		params.set("fl", "*,_dist_:geodist(),score");//查询的结果中添加距离和score
		QueryResponse rsp = null; 
		try {
			rsp = cloudClient.query(params);
		} catch (SolrServerException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SolrDocumentList sdl = rsp.getResults();
		return sdl;
	}
	
	public void closeConnection() throws IOException{
		this.cloudClient.close();
	}
	public static void main(String[] args) throws IOException
	{
		GeoEncodeSearch solrS = new GeoEncodeSearch();
//		String query = "alternate_ch_names:中国";
//		List<String> includeFields = new ArrayList<String>();
//		includeFields.add("id");
//		includeFields.add("title");
//		includeFields.add("content");
//		includeFields.add("pageUrl");
//		includeFields.add("publishTime");
//		includeFields.add("mainImgUrl");
//		String date = "20161018";
//		SolrDocumentList list = solrS.searchByDate(date , includeFields, 0 ,10);
	}
}
