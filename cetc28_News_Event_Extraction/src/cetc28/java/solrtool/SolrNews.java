package cetc28.java.solrtool;

import java.util.ArrayList;
import java.util.List;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import cetc28.java.config.DBConfig;
import cetc28.java.news.label.SolrLabelItem;

public class SolrNews
{
	public SolrSearch newsSolrSearch = null;// 新闻solr搜索

	public SolrNews()
	{
		// TODO Auto-generated constructor stub
		newsSolrSearch = new SolrSearch(DBConfig.getZOOKEEPER_HOSTS(), DBConfig.getDEFAULT_COLLECTION_CRAWLER());
	}

	/**
	 * 获得一天的所有数据
	 * 
	 * @param date
	 *            日期
	 * @param includeFields
	 *            需要的字段
	 * @return 指定日期所有数据
	 */
	public List<SolrLabelItem> getOneDayData(String date, List<String> includeFields)
	{
		List<SolrLabelItem> dataList = new ArrayList<>();
		SolrDocumentList solrDataList = newsSolrSearch.searchByDate(date, includeFields);
		for (SolrDocument solrData : solrDataList)
		{
			if (solrData != null && solrData.containsKey("content")
					&& !solrData.get("content").toString().trim().equals(""))
			{
				SolrLabelItem data = getLabelItem(solrData);
				dataList.add(data);
			}
		}
		return dataList;
	}

	private SolrLabelItem getLabelItem(SolrDocument solrData)
	{
		String event_id = solrData.containsKey("id") ? solrData.getFirstValue("id").toString().trim() : null;
		String news_title = solrData.containsKey("title") ? solrData.getFirstValue("title").toString().trim() : null;
		String news_url = solrData.containsKey("pageUrl") ? solrData.getFirstValue("pageUrl").toString().trim() : null;
		String img_address = solrData.containsKey("mainImgUrl") ? solrData.getFirstValue("mainImgUrl").toString().trim()
				: null;
		String news_content = solrData.containsKey("content") ? solrData.getFirstValue("content").toString().trim()
				: null;
		String saveTime = solrData.containsKey("publishTime") ? solrData.getFirstValue("publishTime").toString().trim()
				: null;
		String dataSource = solrData.containsKey("dataSource") ? solrData.getFirstValue("dataSource").toString().trim()
				: null;

		return new SolrLabelItem(event_id, news_title, news_url, img_address, news_content, saveTime, dataSource);
	}

	/*
	 * 每过10分钟，就获取当天数据（之前没有被获取）
	 */
	/**
	 * 每过10分钟，就获取当天数据（之前没有被获取）
	 * 
	 * @param date
	 *            当天日期
	 * @param includeFields
	 *            需要的字段
	 * @param start
	 *            数据行数开始
	 * @param newsNum
	 *            获取行数
	 * @return
	 */
	public List<SolrLabelItem> getcurDayData(String date, List<String> includeFields, int start, int newsNum)
	{
		List<SolrLabelItem> dataList = new ArrayList<>();
		SolrDocumentList solrDataList = newsSolrSearch.searchByDate(date, includeFields, start, newsNum);
		for (SolrDocument solrData : solrDataList)
		{
			if (solrData != null && solrData.containsKey("id"))
			{
				if (solrData.containsKey("content") && !solrData.get("content").toString().trim().equals(""))
				{
					SolrLabelItem data = getLabelItem(solrData);//
					dataList.add(data);
				}
			}
		}
		return dataList;
	}

	public static void main(String[] args)
	{
		// TODO Auto-generated method stub

	}

	public void close()
	{
		// TODO Auto-generated method stub
		newsSolrSearch.close();
	}

}
