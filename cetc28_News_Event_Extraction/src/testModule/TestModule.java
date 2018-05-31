package testModule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import ModelController.eventTriggerClassifierTrain;
import ModelController.eventTypeModelTrain;
import ModelController.nameEntityModelTrain;
import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.news.label.EventItem;
import cetc28.java.news.label.SolrLabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import cetc28.java.program.EventExtract;
import cetc28.java.solrtool.SolrNews;

public class TestModule {
	//1新闻网站爬虫
	//2句子预处理功能
	public static void testFuncProcessText() throws IOException
	{
		ArrayList<String> results = new ArrayList<>();
		ArrayList<String> datas = loadData("test.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter("result1.txt"));
		for(String textString : datas)
		{
			List<String> wordList = LtpTool.getWords(textString);
			List<String> tagList = LtpTool.getPosTag(textString);
			Pair<List<Integer>, List<String>> depResult = LtpTool.parse(textString);
			List<Integer> verbList = LtpTool.getVerbList(depResult.getFirst(), tagList);
			Pair<String, Integer> headWord = LtpTool.getHeadWord(depResult.getFirst(), wordList);
			bw.write("句子："+textString+"\n");
			bw.write("分词结果："+wordList+"\n");
			bw.write("词性标注结果："+tagList+"\n");
			bw.write("依存句法分析结果："+depResult+"\n");
			bw.write("句子核心词："+headWord+"\n");
			bw.write("句子中的主动词和部分从句动词："+verbList+"\n");
		}
		bw.close();
		
	}
	
	//3实体抽取模型训练
	public static void testFuncEntityModelTrain()
	{
		nameEntityModelTrain nc = new nameEntityModelTrain();
		nc.trainNerModel(FileConfig.getNerTrainDataPath(), FileConfig.getNerModelPath());
	}
	//4实体抽取性能
	public static void testFuncEntityExtraction() throws IOException
	{
		ArrayList<String> results = new ArrayList<>();
		ArrayList<String> datas = loadData("test.txt");
		BufferedWriter bw = new BufferedWriter(new FileWriter("result2.txt"));
		for(String textString : datas)
		{
			String result = Ner.ner3(textString);
			bw.write("实体抽取结果："+result+"\n");
			
		}
		bw.close();
	}
	//5事件抽取模型训练
	public static void testFuncEventModelTrain()
	{
		eventTriggerClassifierTrain mc = new eventTriggerClassifierTrain();
		mc.trainModel1(FileConfig.getEventTrainDataPath(), FileConfig.getMaxentModel1Path());
		mc.trainModel2(FileConfig.getEventTrainDataPath(), FileConfig.getMaxentModel2Path());
		eventTypeModelTrain m = new eventTypeModelTrain();
		m.eventTypeModel_train(FileConfig.getEventTrainDataPath(), FileConfig.getEventTypeModelPath());
		m.eventTypeThreshold_train(FileConfig.getTriggerPath(), FileConfig.getThresholdPath());
	}

	public void newsExtraction(EventExtract eventExtract) throws Exception {// 测试之前数据到最新的数据
		SolrNews solrUtil = new SolrNews();
		List<String> includeFields = new ArrayList<String>();
		includeFields.add("id");
		includeFields.add("title");
		includeFields.add("content");
		includeFields.add("pageUrl");
		includeFields.add("publishTime");
		includeFields.add("mainImgUrl");
		includeFields.add("dataSource");
		Date curDate1 = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		Date endDate = new Date(116, 10, 14);
//		Calendar calendar1 = new GregorianCalendar();
//		calendar1.setTime(endDate);
//		calendar1.add(calendar1.DATE, -1);
//		endDate = calendar1.getTime();

		Date beginDate = new Date(116, 10, 14);		// begin Date
		Calendar calendar = new GregorianCalendar();
		int newsNum = 0;
		while (beginDate.compareTo(endDate) == -1) {
			/*
			 * process
			 */
			List<SolrLabelItem> datalist = solrUtil.getOneDayData(sdf.format(beginDate), includeFields);

			List<EventItem> eventlist = new ArrayList<EventItem>();
//			
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
			calendar.add(calendar.DATE, 1);
			beginDate = calendar.getTime();
		}
		solrUtil.close();// 0: 新闻solr
		Date curDate2 = new Date();
		float seconds = (curDate2.getTime() - curDate1.getTime()) / 1000;
		System.out.println("总共花费时间："+seconds);
		System.out.println("评价每篇处理时间：" + seconds / newsNum);
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
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		TestModule.testFuncEntityModelTrain();
	}
	
	public static ArrayList<SolrLabelItem> loadCorpusData(String path) throws IOException 
	{
		ArrayList<SolrLabelItem> datas = new ArrayList<>();
		BufferedReader br = null;

		br = new BufferedReader(new FileReader(new File(path)));
		
		List<String> lines = new ArrayList<>();
		String line = "";
		
		
		while((line = br.readLine())!=null)
		{
			lines.add(line);
		}
		br.close();
//		System.out.println(lines.size());
		int numCorpus = lines.size() / 6;
		
		for(int i = 0;i < numCorpus;i++)
		{
			String event_id = lines.get(i*6 + 0);
			String img_address = lines.get(i*6 + 1);
			String dataSource = lines.get(i*6 + 2);
			String news_title = lines.get(i*6 + 4);
			String saveTime = lines.get(i*6 + 3);
			String news_content = lines.get(i*6 + 5);
			SolrLabelItem item = new SolrLabelItem(event_id, news_title, "http://null", img_address, news_content, saveTime, dataSource);
			datas.add(item);
		}
		
		
		return datas;
	}
	public static ArrayList<String> loadData(String path) 
	{
		ArrayList<String> datas = new ArrayList<>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(path)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String line = "";
		try {
			while((line = br.readLine())!=null)
			{
				datas.add(line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return datas;
	}
}
