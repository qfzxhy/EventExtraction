package cetc28.java.program;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import cetc28.java.config.DBConfig;
import cetc28.java.eventdetection.argument_extraction.Methods;
import cetc28.java.eventdetection.argument_extraction.RoleExtract;
import cetc28.java.news.label.EventItem;
import cetc28.java.news.label.SolrLabelItem;
import cetc28.java.solrtool.SolrNews;


public class MainProcess {
	SolrNews solrUtil;

	public MainProcess() {
		// TODO Auto-generated constructor stub
		this.solrUtil = new SolrNews();
	}

	private static List<EventItem> extract_event(EventExtract eventExtract, String event_id, String news_url, String img_address,
			String saveTime, String news_title, String news_content) throws SQLException {
		// TODO Auto-generated method stub
		List<EventItem> eventList = null;
		if (news_title == null || news_title.trim().equals(""))return null;
		/*
		 * 去掉标题中的所有的[]
		 */
//		news_title = Methods.fomateTitle(news_title);
		
		/**
		 * 抽取当前新闻的所有的事件
		 */
		eventList = eventExtract.eventExtractforWhole(event_id, news_url, img_address, saveTime, news_title,
				news_content);
		return eventList;
	}

	public void curDateNewsExtraction(EventExtract eventExtract) throws Exception // 测试当天数据
	{
		List<String> includeFields = new ArrayList<String>();
		includeFields.add("id");
		includeFields.add("title");
		includeFields.add("content");
		includeFields.add("pageUrl");
		includeFields.add("publishTime");
		includeFields.add("mainImgUrl");
		includeFields.add("dataSource");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		// List<String> idList = new ArrayList<>();
		Date date = new Date();
		int start = 0;
		float newsNum = 0;// 新闻的数量
		while (true) {
			Date curDate = new Date();
			String dateStr = sdf.format(curDate);
			if (!dateStr.equals(sdf.format(date))) {
				date = curDate;
				start = 0;
			}
			List<SolrLabelItem> datalist = this.solrUtil.getcurDayData(dateStr, includeFields, start, 100);
			List<EventItem> eventlist = null;
			// System.out.println(datalist.size());
			long startTime = System.currentTimeMillis();
			for (SolrLabelItem data : datalist) {
				System.out.println("当前已经处理：" + newsNum + "篇新闻");
				try {
					/**
					 * 当前新闻标题以及正文进行处理，将处理结果放入eventlist
					 */
					eventlist = extract_event(eventExtract, data.event_id, data.news_url, data.img_address, data.saveTime,data.news_title, data.news_content);
					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					/**
					 * 将该正文的事件抽取结果插入到数据库中
					 */
					eventExtract.oracleUtil.InsertOracle(eventlist,true);					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				newsNum++;
			}
			start += datalist.size();
		}
	}

	public void preDateNewsExtraction(EventExtract eventExtract) throws Exception {// 测试之前数据到最新的数据
		List<String> includeFields = new ArrayList<String>();
		includeFields.add("id");
		includeFields.add("title");
		includeFields.add("content");
		includeFields.add("pageUrl");
		includeFields.add("publishTime");
		includeFields.add("mainImgUrl");
		includeFields.add("dataSource");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date curDate = new Date();
		Calendar calendar1 = new GregorianCalendar();
		calendar1.setTime(curDate);
		calendar1.add(calendar1.DATE, -1);
		curDate = calendar1.getTime();

		Date beginDate = new Date(116, 10, 14);		// begin Date
		Calendar calendar = new GregorianCalendar();
		while (beginDate.compareTo(curDate) == -1) {
			/*
			 * process
			 */
			List<SolrLabelItem> datalist = this.solrUtil.getOneDayData(sdf.format(beginDate), includeFields);

			List<EventItem> eventlist = new ArrayList<EventItem>();
			int newsNum = 0;
			for (SolrLabelItem data : datalist) {
				System.out.println("当前已经处理：" + newsNum + "篇新闻");
				try {
					/**
					 * 当前新闻标题以及正文进行处理，将处理结果放入eventlist
					 */
					eventlist = extract_event(eventExtract, data.event_id, data.news_url, data.img_address, data.saveTime,data.news_title, data.news_content);
					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				try {
					/**
					 * 将该正文的事件抽取结果插入到数据库中
					 */
					eventExtract.oracleUtil.InsertOracle(eventlist,true);					
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				newsNum++;
			}

			calendar.setTime(beginDate);
			// calendar.add(calendar.DATE, 100);
			beginDate = calendar.getTime();
		}
		this.solrUtil.close();// 0: 新闻solr
	}

	public void oneDayNewsExtraction(EventExtract eventExtract) throws Exception {
		// 处理某一天
		List<String> includeFields = new ArrayList<String>();
		includeFields.add("id");
		includeFields.add("title");
		includeFields.add("content");
		includeFields.add("pageUrl");
		includeFields.add("publishTime");
		includeFields.add("mainImgUrl");

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date date = new Date(116, 10, 21);

		List<SolrLabelItem> datalist = solrUtil.getOneDayData(sdf.format(date), includeFields);

		// time++
		List<EventItem> eventlist = new ArrayList<EventItem>();
		// System.out.println("datalist.size():"+datalist.size());
		int newsNum = 0;
		for (SolrLabelItem data : datalist) {
			System.out.println("当前已经处理：" + newsNum + "篇新闻");
			try {
				/**
				 * 当前新闻标题以及正文进行处理，将处理结果放入eventlist
				 */
				eventlist = extract_event(eventExtract, data.event_id, data.news_url, data.img_address, data.saveTime,data.news_title, data.news_content);
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			try {
				/**
				 * 将该正文的事件抽取结果插入到数据库中
				 */
				eventExtract.oracleUtil.InsertOracle(eventlist,true);					
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			newsNum++;
		}
		this.solrUtil.close();// 0: 新闻solr
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		EventExtract eventExtract = new EventExtract();
		MainProcess mp = new MainProcess();
		try {
			//从begindate（2016-0-0）到curdate(2016-12-26)
			mp.preDateNewsExtraction(eventExtract);
			//从curdate(2016-12-26)一直跑，直到手动关闭程序
			mp.curDateNewsExtraction(eventExtract);
		} catch (Exception e) {
			// TODO Auto-generated catch block

			e.printStackTrace();
		}
	}
}
