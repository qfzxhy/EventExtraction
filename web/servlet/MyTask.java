import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import cetc28.java.eventdetection.preprocessing.Data;


import cetc28.java.news.label.EventItem;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.Pair;
import cetc28.java.program.EventExtractionWithoutBackGround;


public class MyTask {
	static String[] types = {"公开声明","呼吁","表达合作意向","商议","外交合作","实质合作","援助","让步","调查","要求",
		"不同意","拒绝","威胁","抗议","军事姿态","减少关系","强迫","袭击","战斗","屠杀",};
	public static int adder(int a,int b)
	{
		return a+b;
	}
	public static String getEntitys(String[] entitys)
	{
		if(entitys==null){return null;}
		Map<String, List<String>> res = new HashMap<>();
		int i =0;
		while(i< entitys.length)
		{
			String[] units = entitys[i].split("/");
			if(units[2].startsWith("b_"))
			{
				String type = units[2].substring(2);
				String entityStr = units[0];
				int j = i + 1;
				for(;j<entitys.length;j++)
				{
					String[] units1 = entitys[j].split("/");
					if(units1[2].equals("i_"+type))
					{
						entityStr += units1[0];
					}else
					{
						break;
					}
				}
				i = j;
				if(!res.containsKey(type))
				{
					res.put(type, new ArrayList<String>());
				}
				res.get(type).add(entityStr);
				
				
			}else
			{
				i++;
			}
		}
		String allentitys = "";
		for(Entry<String, List<String>> key : res.entrySet())
		{
			allentitys = allentitys + key.getKey()+":";
			
			for(int k = 0;k< key.getValue().size();k++)
			{
				String en = key.getValue().get(k);
				allentitys = allentitys + en ;
				if(k < key.getValue().size()-1){allentitys+=",";}
				else{allentitys += " ";}
			}
		}
		return allentitys.trim();
		
	}
	public static List<String> event_extraction(String txt)
	{
		List<String> results = new ArrayList<String>();
		EventExtractionWithoutBackGround bsl = new EventExtractionWithoutBackGround();
//		
		String sent  = txt;
		String newsURL = null;
		String imgAddress = null;
		String newsID = null;
		String saveTime = null;
		String newsTitle = null;
		String placeEntity = null;
		boolean isSummary = false;
		Pair<String, Data> result = null;
		try {
			result = bsl.extractbysentence(newsURL, imgAddress, newsID, saveTime, newsTitle, sent, placeEntity, isSummary);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		List<String> words = result.getSecond().words;
		String wordseq = "";
		
		String[] entitys = result.getSecond().nerArrs;
		for(String word : entitys){wordseq = wordseq + word.substring(0, word.lastIndexOf("/")) + " ";}
		results.add(wordseq.trim());
		String entityseq = "";
		for(String entity : entitys){entityseq = entityseq + entity + " ";}
		System.out.println(entityseq);
		String allentitys = getEntitys(entitys);
	
		results.add(allentitys);
		String trigger = "None";
		String source = "None";
		String target = "None";
		String time = "None";
		String loc = "None";
		String type = "None";
		if(result.getSecond().data.triggerWord != null && result.getSecond().data.triggerWord.length() > 0){trigger = result.getSecond().data.triggerWord;}
		if(result.getSecond().data.sourceActor != null && result.getSecond().data.sourceActor.length() > 0){source = result.getSecond().data.sourceActor;}
		if(result.getSecond().data.targetActor != null && result.getSecond().data.targetActor.length() > 0){target = result.getSecond().data.targetActor;}
		if(result.getSecond().data.eventTime != null && result.getSecond().data.eventTime.length() > 0){time = result.getSecond().data.eventTime;}
		if(result.getSecond().data.eventLocation != null && result.getSecond().data.eventLocation.length() > 0){loc = result.getSecond().data.eventLocation;}
		if(result.getSecond().data.eventType >0 && result.getSecond().data.eventType <=20 ){type = types[result.getSecond().data.eventType-1];}
		
		String eventResult = source+","+trigger+','+target
				+','+time+","+loc+","+type;
		results.add("<"+eventResult+">");
		String Proscore = result.getSecond().data.Proscore;
		
		String polarity = "中性";
		if(Proscore!=null && Integer.parseInt(Proscore) == 1){polarity = "正面";}
		if(Proscore!=null &&Integer.parseInt(Proscore) == -1){polarity = "负面";}
		if(result.getSecond().data.eventType == 15){polarity = "正面";}
		if(result.getSecond().data.eventType == 18){polarity = "负面";}
		results.add(polarity);
		String useTemplate = result.getSecond().data.triggerTemplate !=null ? "使用模板！":"使用机器学习！";
		results.add(useTemplate);
		return results;
//		return result.getSecond().triggerWord+"\t"+result.getSecond().sourceActor+'\t'+result.getSecond().targetActor;
		
	}
	public static List<String> content_extract(String content)
	{
		List<String> results = new ArrayList<String>();
		String newsURL = null;
		String imagAddress = null;
		String newsID = null;
		String saveTime = null;
		String newsTitle = null;
		String newsContent = content;
//		String sentence = "中智将关系提升至全面战略伙伴关系";
//		String placeEntity = "菲律宾";
		//new object
		EventExtractionWithoutBackGround bsl = new EventExtractionWithoutBackGround();
		/**
		 *全文事件抽取结果
		 */
		List<EventItem> eventList = null;
		try {
			eventList = bsl.eventExtractforWhole(newsID, newsURL, imagAddress, saveTime, newsTitle,newsContent);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for(EventItem event : eventList)
		{
			Pair<String, LabelItem> result = event.getCon_result();
			if(result != null && result.getSecond() != null && result.getSecond().triggerWord != null && 
					(result.getSecond().triggerWord.length() > 1||result.getSecond().triggerTemplate!=null))
			{
				String Proscore = result.getSecond().Proscore;
				
				
				String polarity = "中性";
				if(Proscore != null && Integer.parseInt(Proscore) == 1){polarity = "正面";}
				if(Proscore != null && Integer.parseInt(Proscore) == -1){polarity = "负面";}
				if(result.getSecond().eventType == 15){polarity = "正面";}
				if(result.getSecond().eventType == 18){polarity = "负面";}
				
				String trigger = "None";
				String source = "None";
				String target = "None";
				String time = "None";
				String loc = "None";
				String type = "None";
				if(result.getSecond().triggerWord != null && result.getSecond().triggerWord.length() > 0){trigger = result.getSecond().triggerWord;}
				if(result.getSecond().sourceActor != null && result.getSecond().sourceActor.length() > 0){source = result.getSecond().sourceActor;}
				if(result.getSecond().targetActor != null && result.getSecond().targetActor.length() > 0){target = result.getSecond().targetActor;}
				if(result.getSecond().eventTime != null && result.getSecond().eventTime.length() > 0){time = result.getSecond().eventTime;}
				if(result.getSecond().eventLocation != null && result.getSecond().eventLocation.length() > 0){loc = result.getSecond().eventLocation;}
				if(result.getSecond().eventType >0 && result.getSecond().eventType <=20){type = types[result.getSecond().eventType-1];}
				
				String eventResult = source+","+trigger+','+target
						+','+time+","+loc+","+type + ","+polarity;
				results.add(eventResult);
//				String eventResult = result.getSecond().triggerWord+","+result.getSecond().sourceActor+','+result.getSecond().targetActor
//						+ ","+result.getSecond().eventType + "," + Proscore ;
//				results.add(eventResult);
			}
			
		}
		return results;
	}
	public static String load_wordcloud_fig(String content) throws IOException
	{
		List<String> events = content_extract(content);
		System.out.println(events);
    	StringBuilder sb = new StringBuilder();
		for(String event: events)
		{
			String[] units = event.split(",");
			for(int i = 0; i < units.length-1;i++)
			{
				if(units[i].length() > 0)
				{
					sb.append(units[i] + ",");
				}
			}
		}
		System.out.println(sb.toString());
		PyCaller.execPy(sb.toString());
		
		return "cloud.jpg";
		
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MyTask mt = new MyTask();
		String results = mt.getEntitys(new String[]{"中国/ns/b_dev","航母/ns/i_dev","在/p/other","叙利亚/ns/b_country","下水/v/other"});
		System.out.println(results);
	}

}
