package cetc28.java.eventdetection.triggertype_extraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.preprocessing.LoadDataSingleton;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
/**
 * 事件类别抽取
 * @author qf
 *
 */
public class EventTypeExtraction {
	public TriggerTemplate template;//触发类别
	public double[] thresholds;//阈值
	LoadDataSingleton dataStorer = LoadDataSingleton.getInstance();
	public EventTypeExtraction(String triggerNumModelPath, String triggerPath, String eventTypeThresholdPath) {
		// TODO Auto-generated constructor stub
		this.template = new TriggerTemplate(this.load_triggerNumber(triggerNumModelPath, triggerPath));
		thresholds = EventTypeThresholdController.getThresholdfromFile(eventTypeThresholdPath);
	}
	
	public boolean hasFirstType(String sentence)
	{
		List<String> wordList = LtpTool.getWords(sentence);
		for(String word : wordList)
		{
			if(template.getEventType(word) == 1 || template.getEventType(word) == 11|| template.getEventType(word) == 12)
			{
				return true;
			}
		}
		return false;
	}

	private  HashMap<String, int[]> load_triggerNumber(String triggerNumModelPath, String triggerPath) {
		// TODO Auto-generated method stub
		HashMap<String, int[]> triggerNumber = TriggerController.getMapfromFile(triggerNumModelPath);
		/*
		 * 1、要把所有数据都放进去（包括测试数据）
		 * 2、event type model 要挟y
		 */
		BufferedReader br = null;
		for(int id = 1; id <= 20; id++)
		{
			ArrayList<String> triggers = new ArrayList<>();
			try {
				br = new BufferedReader(new FileReader(new File(triggerPath + id + ".txt")));
				String trigger = null;
				while((trigger = br.readLine())!=null)
				{
					triggers.add(trigger);
					if(!triggerNumber.containsKey(trigger))
					{
						int[] typeNum = new int[20];
						triggerNumber.put(trigger, typeNum);
					}
					triggerNumber.get(trigger)[id - 1]++;
				}
				br.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return triggerNumber;
	}
	/**
	 *利用触发词列表抽取触发词类别
	 * @param result 
	 */
	public void setEventType(LabelItem result)
	{
		/*
		 * 如果triggerNum 或者 triggerList 中有trigger，则返回类别
		 * 如果没有，利用相似度计算类别
		 */
		String triggerWord = result.triggerWord;
		if(triggerWord == null || triggerWord.trim().length() == 0)
		{
			return;
		}
		int triggerType = template.getEventType(triggerWord);
		if(triggerType == 0)
		{
			double maxMargin = 0.0;
			int type = 0;
			for(int i = 1; i <= 20; i++)
			{
				if(EventTypeThresholdController.getSimilarScore(triggerWord, i)>thresholds[i-1])
				{
					double margin = EventTypeThresholdController.getSimilarScore(triggerWord, i) - thresholds[i-1];
					if(margin > maxMargin)
					{
						maxMargin = margin;
						type = i;
					}
				}
			}
			if(type != 0) triggerType = type;
		}
		result.eventType = triggerType;
	}
	/**
	 * 最核心函数 抽取句子中触发词列表
	 * @param testData 
	 */
	public  void eventTypeExtract(Data testData) {
		// TODO Auto-generated method stub
		int eventType = 0;
		eventType = template.getEventType(testData.data.triggerWord);//触发词列表中包含
		if(eventType == 0)//说明是拆字后得到的新触发词
		{
			int type = 0;
			String predictedTrigger = testData.data.triggerWord;
			if(dataStorer.word2Vec.containsKey(predictedTrigger))
			{
				float[] vector = dataStorer.word2Vec.get(predictedTrigger);
				Map<Float,Integer> distanceMap = new TreeMap<Float,Integer>().descendingMap();//treeMap 倒序
				for(java.util.Map.Entry<String, int[]> entry : this.template.triggerNumber.entrySet())//距离排序
				{
					if(template.getEventType(entry.getKey())==0){continue;}//如果trigger 是triggerList中且在训练数据中没有出现
					if(dataStorer.word2Vec.containsKey(entry.getKey()))
					{
						float[] vec = dataStorer.word2Vec.get(entry.getKey());
						distanceMap.put(getCosinDis(vector,vec), template.getEventType(entry.getKey()));
					}
				}
				HashMap<Integer, Integer> typeMapTime = new HashMap<>();
				int k = 0;
				for(java.util.Map.Entry<Float, Integer> p : distanceMap.entrySet())
				{
					int t = p.getValue();
					if(typeMapTime.containsKey(t))
					{
						typeMapTime.put(t, typeMapTime.get(t)+1);
					}else
					{
						typeMapTime.put(t, 1);
					}
					if(++k == 1){break;}
				}
				int max = Integer.MIN_VALUE;
				for(java.util.Map.Entry<Integer, Integer> entry : typeMapTime.entrySet())
				{
					if(entry.getValue()>max)
					{
						max = entry.getValue();
						type = entry.getKey();
					}
				}
				eventType = type;
			}else
			{
				eventType = 21;
			}
		}
		testData.data.eventType = eventType;
	}
	/**
	 * 余弦距离
	 * @param vector 词向量
	 * @param vec 词向量
	 * @return
	 */
	private static float getCosinDis(float[] vector, float[] vec) {
		// TODO Auto-generated method stub
		double vectorNorm = 0.0d,vecNorm = 0.0d;
		for(int i = 0;i<vector.length;i++)
		{
			vectorNorm += vector[i]*vector[i];
			vecNorm += vec[i]*vec[i];
		}
		vectorNorm = Math.sqrt(vectorNorm);
		vecNorm = Math.sqrt(vecNorm);
		double cosinDis = 0.0d;
		for(int i = 0;i<vector.length;i++)
		{
			cosinDis += vector[i]*vec[i];
		}
		return (float)(cosinDis/(vecNorm*vectorNorm));
	}
}
