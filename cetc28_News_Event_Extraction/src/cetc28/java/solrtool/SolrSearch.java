package cetc28.java.solrtool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;

import cetc28.java.config.DBConfig;

public class SolrSearch {
	private CloudSolrClient client = null;

	public SolrSearch(String zkHosts,String defaultCollection) {
		client = GetCloudSolrClient.getInstance(zkHosts,defaultCollection);
	}

	public void close() {
		try {
			client.close();;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 指定返回条数的查询
	 * 
	 * @param query
	 *            查询语句
	 * @param start
	 *            开始返回的id
	 * @param rows
	 *            返回的条数
	 * @return
	 */
	public SolrDocumentList searchByQuery(String query, String start, String rows) {
		return searchByQuery(query, new ArrayList<String>(), start, rows);
	}

	/**
	 * 指定返回条数和返回数据域的查询
	 * 
	 * @param query
	 *            查询语句
	 * @param includeFields
	 *            包含的数据域
	 * @param start
	 *            开始返回的id
	 * @param rows
	 *            返回的条数
	 * @return
	 */
	public SolrDocumentList searchByQuery(String query, List<String> includeFields, int start, int newsNum) {

		SolrDocumentList list = searchByQuery(query, includeFields, Integer.toString(start), Integer.toString(newsNum));

		return list;
	}

	public SolrDocumentList searchByQuery(String query, List<String> includeFields, String start, String rows) {
		StringBuffer fl = new StringBuffer();
		if (includeFields == null || includeFields.size() == 0) { // 返回数据域默认包含所有
			fl.append("*");
		} else {
			for (String field : includeFields) {
				fl.append(field);
				fl.append(",");
			}
		}
		SolrDocumentList sdl = new SolrDocumentList();
		try {
			ModifiableSolrParams msp = new ModifiableSolrParams();
			msp.set("q", query);
			msp.set("fl", fl.toString());
			msp.set("start", start);
			msp.set("rows", rows);
			msp.set("sort", "publishTime asc"); 
			QueryResponse qresponse = client.query(msp);
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
	 * 
	 * @param query
	 *            查询语句
	 * @return
	 */
	public SolrDocumentList searchByQuery(String query) {
		return searchByQuery(query, new ArrayList<String>());
	}

	/**
	 * 返回所有符合条件文档的查询（指定返回数据域）
	 * 
	 * @param query
	 *            查询语句
	 * @param includeFields
	 *            包含的数据域
	 * @return
	 */
	public SolrDocumentList searchByQuery(String query, List<String> includeFields) {
		int start = 0; // 起始id
		int includeRows = 10000; // 每页条数
		SolrDocumentList List = new SolrDocumentList();
		while (true) {
			SolrDocumentList temp = searchByQuery(query, includeFields, Integer.toString(start),
					Integer.toString(includeRows));
			if (temp.size() == 0) {
				break;
			} else {
				List.addAll(temp);
				start += includeRows;
			}
		}
		return List;
	}

	// yes
	public SolrDocumentList searchByDate(String date, List<String> includeFields, int start, int newsNum) {
		String query = "publishDate:" + date + "000000";
		SolrDocumentList list = searchByQuery(query, includeFields, start, newsNum);
		return list;
	}

	// yes
	public SolrDocumentList searchByDate(String date, List<String> includeFields) {
		String query = "publishDate:" + date + "000000";
		SolrDocumentList list = searchByQuery(query, includeFields);
		return list;
	}

	// yes
	public SolrDocumentList searchByQuery(String[] fields, String[] keys, int start, int count, String[] sortfield,
			Boolean[] flag) {
		if (null == fields || null == keys || fields.length != keys.length) {
			return null;
		}
		// if (null == sortfield || null == flag
		// || sortfield.length != flag.length) {
		// return null;
		// }
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
			rsp = client.query(query);
		} catch (SolrServerException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SolrDocumentList sdl = rsp.getResults();
		return sdl;

	}

	public static void main(String[] args) {
		SolrSearch solrS = new SolrSearch(DBConfig.getZOOKEEPER_HOSTS(),DBConfig.getDEFAULT_COLLECTION_CRAWLER());
		String query = "content:　　中国";
		SolrDocumentList list =solrS.searchByQuery(query);
		for (SolrDocument l : list) {
			System.out.println(l.getFieldValue("id").toString());
		}
//		SolrSearch solrS = new SolrSearch(DBConfig.getDEFAULT_COLLECTION_CRAWLER());
//		String query = "content:　　中国";
//		// String query = "id:20151118000000384e997aa160ddd74529c667d01f9b54";
//		// String query = "siteName:人民网_老版-人民网-财经";
//		List<String> includeFields = new ArrayList<String>();
//		includeFields.add("id");
//		includeFields.add("title");
//		includeFields.add("content");
//		includeFields.add("pageUrl");
//		includeFields.add("publishTime");
//		includeFields.add("mainImgUrl");
//		String date = "20161018";
//		SolrDocumentList list = solrS.searchByQuery(query);
//		// SolrDocumentList list = solrS.searchByDate(date , includeFields, 0
//		// ,10);
//		System.out.println(list.size());
//		for (SolrDocument doc : list) {
//			System.out.println(doc.getFieldValue("id").toString());
//		}
	}
}
