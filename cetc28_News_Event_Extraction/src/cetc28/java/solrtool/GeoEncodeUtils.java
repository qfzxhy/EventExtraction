package cetc28.java.solrtool;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;


public class GeoEncodeUtils {
	public GeoEncodeSearch solrSearch = null;
	public static final String alternate_ch_names = "alternate_ch_names";
	public GeoEncodeUtils() {
		// TODO Auto-generated constructor stub
		solrSearch = new GeoEncodeSearch();
	}
	
	public SolrDocument getCoordinate(String name, String attri)
	{
		String[] fields = null;
		String[] keys = null;
		if(attri.equals("country"))
		{
			fields = new String[]{"alternatenames","admin1code"};
			keys = new String[]{name,"00"};
		}else
		{
			fields = new String[]{alternate_ch_names};
			keys = new String[]{name};
		}
		SolrDocumentList list = solrSearch.searchByQuery(fields, keys, 0, 10, null, null);
		SolrDocument result = null;
		int maxLen = Integer.MIN_VALUE;
		for(SolrDocument doc : list)
		{
			String alternatenames = doc.get(alternate_ch_names).toString();
			if(alternatenames != null)
			{
				int len = alternatenames.split(",").length;
				maxLen = maxLen <  len? len : maxLen;
				result = doc;
			}
		}
		return result;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

//		List<String> idList = new ArrayList<>();
//		idList.add("2013100113530078aa0b69215b2a4952732c7327c7430f");
		GeoEncodeUtils solrUtil = new GeoEncodeUtils();
		SolrDocument doc = solrUtil.getCoordinate("中国", "00");
		System.out.println(doc.get("name"));
		System.out.println(doc.getFieldValue("latitude"));
		System.out.println(doc.getFieldValue("longitude"));
	}

}
