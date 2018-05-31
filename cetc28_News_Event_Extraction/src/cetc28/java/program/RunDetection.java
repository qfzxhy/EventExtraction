/*  
* 创建时间：2015年10月31日 下午1:55:29  
* 项目名称：Java_EventDetection_News  
* @author GreatShang  
* @version 1.0   
* @since JDK 1.8.0_21  
* 文件名称：RunDetection.java  
* 系统信息：Windows Server 2008
* 类说明： 
* 功能描述： 从标题中抽取事件各项信息存入LabelItem
* demo for run detection event.
*/
package cetc28.java.program;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;




import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.ruleextractor.Extractor_Rule;
import cetc28.java.eventdetection.ruleextractor.Rule;
import cetc28.java.eventdetection.trigger_extraction.PostProcessing;
import cetc28.java.eventdetection.trigger_extraction.TriggerExtractor;
import cetc28.java.eventdetection.triggertype_extraction.EventTypeExtraction;
import cetc28.java.eventdetection.triggertype_extraction.TriggerController;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.Pair;

import java.io.IOException;
import java.sql.SQLException;

public class RunDetection 
{
	public TriggerExtractor triggerExtractor ;//触发词抽取
	public EventTypeExtraction eventTypeExtractor;//事件类别抽取
//	public Actors_Db_Methods adm;
	public Extractor_Rule extractor_rule = null;
	public PostProcessing postProcessing = null;
	public RunDetection()
	{
//		EventExtractor extractor = new EventExtractor();
		triggerExtractor = new TriggerExtractor();
		String triggerNumModelPath = FileConfig.getEventTypeModelPath();
		String eventTypeThresholdPath = FileConfig.getThresholdPath();
		String triggerPath = FileConfig.getTriggerPath();
		// TODO Auto-generated method stub、
		eventTypeExtractor = new EventTypeExtraction(triggerNumModelPath, triggerPath, eventTypeThresholdPath);
		extractor_rule = new Extractor_Rule(FileConfig.getRulePath());
		postProcessing = new PostProcessing();
	}

	/*
	 * for qianf
	 */
	public void LoadEventExtractor()
	{
		this.triggerExtractor = new TriggerExtractor();
		String triggerNumModelPath = FileConfig.getEventTypeModelPath();
		String eventTypeThresholdPath = FileConfig.getThresholdPath();
		String triggerPath = FileConfig.getTriggerPath();
		// TODO Auto-generated method stub、
		this.eventTypeExtractor = new EventTypeExtraction(triggerNumModelPath, triggerPath, eventTypeThresholdPath);
	}
	
	/**从新闻文本中抽取时间 地点实体
	 * 对labelresult中的事件元素赋值
	 * result.eventTime
	 * result.eventLocation
	 * NER识别标题中的事件时间和时间地点，未识别到可不充，请赋值为null，eg：result.eventTime = null;
	 * @newsInput 新闻输入文本
	 * @labelresult 抽取结果临时存储对象
	 * 
	 * for QianF
	 */
	public void setTimeandLocation(List<Pair<String,String>> nerResult,List<Pair<String,String>> tagResult,LabelItem labelresult)
	{
		/*
		 * 1、只有一个时间、一个地点好办
		 * 2、多个时间，选择第一个
		 * 3、多个地点，选择1、p+ns  2、nr/nt + ns 3、ns + n
		 */
		//List<Pair<String,String>> nerResult = ne.nerResult(newsInput);
		//List<Pair<String,Integer>> nvResult = ne.getN_V(newsInput);
		//List<Pair<String,String>> tagResult = ne.tagResult(newsInput);
//		labelresult.eventTime = GetTimeAndLocation.getTime(nerResult);
//		labelresult.eventLocation = GetTimeAndLocation.getLocation(tagResult);
		
	}
	
	
	/**
	 * 抽取事件核心程序
	 * @param newsSource
	 * @param newsID
	 * @param newsTime
	 * @param newsTitle
	 * @return
	 */
	public Data GetEventInforfromNews_Rule(String newsSource,String newsID,String newsTime,String newsTitle)
	{
		LabelItem result  = new LabelItem("",newsSource,newsID,newsTitle);//标注结果存储对象
		Data result_Data = new Data(result);
		result_Data.setTrainData();//new HashMap<>()是停用词
		result.ifEvent = true;
		if (result.eventTime == null)//未识别到时间，事件时间使用新闻发布时间填充
			result.eventTime = newsTime;
		String triggerWord = "";
		Rule rule_result = extractor_rule.extract(result_Data);
		return result_Data;
	}
	public Data GetEventInforfromNews_MLearning(Data data)
	{
		if(data.data.triggerWord  == null)
		{
			triggerExtractor.extract(data);
			eventTypeExtractor.eventTypeExtract(data);
			if(data.data.eventType == 21 || data.data.eventType == 0){data.data.triggerWord = null;}
			postProcessing.postProcessing(data);
		}
		return data;
	}
	
	
	
	public static void main(String []args) throws SQLException, ParseException
	{
		//load extractor
		RunDetection demoTest = new RunDetection();
		demoTest.LoadEventExtractor();
		//sentence information for extraction
		String newsSource = "xinhua";
		String newsID = "1";
		String newsTime = "2015年5月1日";
		String newsTitle = "美国防部官员宣布美韩军演可能会重新启动";
		//模板抽取
		Data result = demoTest.GetEventInforfromNews_Rule(newsSource,newsID,newsTime,newsTitle);
		//统计学习方法抽取
		Data extractResult = demoTest.GetEventInforfromNews_MLearning(result);
		//输出抽取结果
		System.out.println("触发词："+extractResult.data.triggerWord);
		System.out.println("触发词类别："+extractResult.data.eventType);		
	}
}
