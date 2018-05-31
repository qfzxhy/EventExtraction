/*  
* 创建时间：2016年4月1日 下午1:08:56  
* 项目名称：Java_EventDetection_News  
* @author GreatShang  
* @version 1.0   
* @since JDK 1.8.0_21  
* 文件名称：Tools.java  
* 系统信息：Windows Server 2008
* 类说明：  
* 功能描述：
*/
package cetc28.java.eventdetection.trigger_extraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.news.label.LabelItem;


/**
 * 
 * @author qf
 *读数据+处理数据（用来训练和测试）
 */
public class Tools {
	int i = 0;
	/**
	 * 
	 * @param stopwords 
	 * @throws IOException
	 */
//	private static void loadStopWord(HashMap<String, Integer> stopwords) throws IOException {
//		// TODO Auto-generated method stub
//		BufferedReader br = new BufferedReader(new FileReader("D:/shangd/My Workspace/Java_EventDetection_News/models/stopwords/StopWords.txt"));
//		String word = "";
//		while((word = br.readLine())!=null)
//		{
//			stopwords.put(word, 1);
//		}
//		br.close();
//	}
	/**
	 * 
	 * @param filepath 训练或者测试数据文件
	 * @param trainDatas 处理训练数据，放到traindatas中          labelItem是来存储抽取结果的类
	 * @param trainDataList 处理训练数据，放到traindataList中 Data是来存储训练数据的一些预处理结果（分词+词性等）
	 */
	public static  void loadTrainData(String filepath, List<LabelItem> trainDatas, List<Data> trainDataList)  {
		// TODO Auto-generated method stub
		HashMap<String, Integer> stopwords = new HashMap<>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(filepath));
			String lineStr = null;
			while((lineStr=br.readLine())!=null)
			{
				
				String title = lineStr.substring(lineStr.indexOf("title")+6, lineStr.indexOf("triggerword")).trim();
				String triggerword = lineStr.substring(lineStr.indexOf("triggerword")+12, lineStr.indexOf("source")).trim();
				String eventType = lineStr.substring(lineStr.indexOf("eventType")+10).trim();
				LabelItem traindata = new LabelItem("", "", "", title);
				traindata.ifEvent = true;
				traindata.eventType = Integer.parseInt(eventType);
				traindata.triggerWord = triggerword;
				if(trainDatas != null)
					trainDatas.add(traindata);
				LabelItem traindata1 = new LabelItem("", "", "", title);
				traindata1.ifEvent = true;
				traindata1.eventType = Integer.parseInt(eventType);
				traindata1.triggerWord = triggerword;
				Data trainData = new Data(traindata1);
				trainData.setTrainData(stopwords);
				trainDataList.add(trainData);
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	


	/**
	 * 
	 * @param testX 测试数据预测结果
	 * @param testDataResult 测试数据gold结果
	 * @return 触发词准确率
	 */
	public static float getPrecise_trigger(ArrayList<Data> testX,
			ArrayList<LabelItem> testDataResult) {
		// TODO Auto-generated method stub
		float rightTime = 0;
		for(int i=0;i<testX.size();++i)
		{
			if(testX.get(i).data.triggerWord==null&&testDataResult.get(i).triggerWord==null) 
			{
				rightTime+=1;continue;
			}
			if(testX.get(i).data.triggerWord!=null)
				if(convert(testX.get(i).data.triggerWord).equals(convert(testDataResult.get(i).triggerWord)))
				{
					rightTime+=1;
				}
				
		}
		return (float) (rightTime*1.0/testX.size());
	}
	private static String convert(String trigger)
	{
		String triggerPattern = trigger;
		if(trigger.indexOf("*")!=-1) triggerPattern = triggerPattern.replace("*", ".*");
		if(trigger.indexOf("_")!=-1) triggerPattern = triggerPattern.replace("_", ".*");
		return triggerPattern;
	}
	/**
	 * 
	 * @param testX 测试数据预测结果
	 * @param testDataResult 测试数据gold结果
	 * @return 触发词类别准确率
	 */
	public static float getPrecise_eventType(ArrayList<Data> testX,
			ArrayList<LabelItem> testDataResult) {
		// TODO Auto-generated method stub
		float rightTime = 0;
		for(int i=0;i<testX.size();++i)
		{
			if((testX.get(i).data.eventType==testDataResult.get(i).eventType))
			{
				rightTime+=1;
			}
		}
		return (float) (rightTime*1.0/testX.size());
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
