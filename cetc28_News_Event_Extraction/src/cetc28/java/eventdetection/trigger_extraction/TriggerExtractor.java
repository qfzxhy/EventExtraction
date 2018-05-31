package cetc28.java.eventdetection.trigger_extraction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.w3c.dom.events.EventException;

import ModelController.eventTriggerClassifierTrain;
import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.preprocessing.LoadDataSingleton;
import cetc28.java.eventdetection.triggertype_extraction.EventTypeExtraction;
import cetc28.java.eventdetection.triggertype_extraction.EventTypeThresholdController;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import net.sf.javaml.distance.fastdtw.util.Arrays;

public class TriggerExtractor
{
	public eventTriggerClassifierTrain maxentClassifier1 = null;// 对候选触发词训练的分类器，也只对候选触发词进行预测
	public eventTriggerClassifierTrain maxentClassifier2 = null;// 对V训练的分类器，也只对V进行预测
	public FeatureController featureExtractor = null;
	public LoadDataSingleton dataStorer = null;
	public double[] thresholds;
	public EventTypeExtraction eventTypeExtractor = null;

	public TriggerExtractor()
	{
		// TODO Auto-generated constructor stub
		featureExtractor = new FeatureController();
		dataStorer = LoadDataSingleton.getInstance();
		createMaxentModel1();
		createMaxentModel2();
		thresholds = EventTypeThresholdController.getThresholdfromFile(FileConfig.getThresholdPath());
		eventTypeExtractor = new EventTypeExtraction(FileConfig.getEventTypeModelPath(), FileConfig.getTriggerPath(),
				FileConfig.getThresholdPath());
	}

	private void createMaxentModel2()
	{
		// TODO Auto-generated method stub
		String modelPath = FileConfig.getMaxentModel2Path();
		// 加载模型2
		maxentClassifier2 = new eventTriggerClassifierTrain(modelPath);
	}

	private void createMaxentModel1()
	{
		String modelPath = FileConfig.getMaxentModel1Path();
		// 加载模型1
		maxentClassifier1 = new eventTriggerClassifierTrain(modelPath);
	}
	/**
	 * 核心代码，利用机器学习模型抽取触发词
	 * @param testData 测试数据（已预处理）
	 */
	public void extract(Data testData)// 更加详细的抽取 考虑空格的问题
	{
		String title = testData.data.newsTitle;
		title = title.replaceAll("\\s+", " ");
		title = title.replaceAll("\\t+", " ");
		title = title.trim();
		ArrayList<String> triggerCandidateList = getTrigger(testData);
		int eventType = -1;
		Pair<String, Integer> trigger = null;
		if (title.indexOf(" ") == -1)// 没有空格
		{
			if (trigger == null)
			{
				trigger = triggerExtract1(testData, triggerCandidateList);
			} // 包含主动词
			if (trigger == null)
			{
				trigger = triggerExtract2(testData);
			} // 主动词不行，那就候选列表中有的单词
			if (trigger == null && (title.indexOf("：") != -1 || title.indexOf(":") != -1))
			{
				// 前面必须要有人名
				if (hasNameEntity(testData))
				{
					trigger = new Pair<String, Integer>("：", -1);
				}
			}
			if (trigger == null)
			{
				trigger = triggerExtract3(testData);
			}
		} else
		{
			Pair<String, Integer> leftTrigger = null;
			Pair<String, Integer> rightTrigger = null;
			Data leftData = null, rightData = null;
			// left
			String leftSentence = title.substring(0, title.indexOf(" "));
			if (leftSentence.indexOf("　") != -1)
				leftSentence = leftSentence.replaceAll("　", " ");
			if (leftSentence != null && leftSentence.trim().length() > 0)
			{
				leftData = new Data(new LabelItem("", "", "", leftSentence));
				leftData.setTrainData();
			}

			// right
			String rightSentence = title.substring(title.indexOf(" ") + 1);
			if (rightSentence.indexOf("　") != -1)
				rightSentence = rightSentence.replaceAll("　", " ");
			if (rightSentence != null && rightSentence.trim().length() > 0)
			{
				rightData = new Data(new LabelItem("", "", "", rightSentence));
				rightData.setTrainData();
			}
			if (trigger == null)
			{
				leftTrigger = triggerExtract1(leftData, triggerCandidateList);
				rightTrigger = triggerExtract1(rightData, triggerCandidateList);
				if (leftTrigger != null && rightTrigger != null)
				{
					rightTrigger = new Pair<String, Integer>(rightTrigger.getFirst(),
							rightTrigger.getSecond() + (leftData != null ? leftData.words.size() : 0));// 再加上左边句子的长度
					List<String> triggerPriorityList = triggerExtract_condition1(testData);
					trigger = triggerPriorityList.indexOf(leftTrigger) < triggerPriorityList.indexOf(rightTrigger)
							? leftTrigger : rightTrigger;
				} else if (leftTrigger != null)
				{
					trigger = leftTrigger;
				} else if (rightTrigger != null)// 再加上左边句子的长度
				{
					trigger = new Pair<String, Integer>(rightTrigger.getFirst(),
							rightTrigger.getSecond() + (leftData != null ? leftData.words.size() : 0));
				}
			}
			if (trigger == null)
			{
				leftTrigger = triggerExtract2(leftData);
				rightTrigger = triggerExtract2(rightData);
				if (leftTrigger != null && rightTrigger != null)
				{
					rightTrigger = new Pair<String, Integer>(rightTrigger.getFirst(),
							rightTrigger.getSecond() + (leftData != null ? leftData.words.size() : 0));// 再加上左边句子的长度
					List<String> triggerPriorityList = triggerExtract_condition1(testData);
					trigger = triggerPriorityList.indexOf(leftTrigger) < triggerPriorityList.indexOf(rightTrigger)
							? leftTrigger : rightTrigger;
				} else if (leftTrigger != null)
				{
					trigger = leftTrigger;
				} else if (rightTrigger != null)
				{
					trigger = new Pair<String, Integer>(rightTrigger.getFirst(),
							rightTrigger.getSecond() + (leftData != null ? leftData.words.size() : 0));// 再加上左边句子的长度
				}

			}
			if (trigger == null && title.indexOf("：") != -1)
			{
				if (hasNameEntity(testData))
				{
					trigger = new Pair<String, Integer>("：", -1);
				}
			}
			if (trigger == null)
			{
				leftTrigger = triggerExtract3(leftData);
				rightTrigger = triggerExtract3(rightData);
				if (leftTrigger != null && rightTrigger != null)
				{
					rightTrigger = new Pair<String, Integer>(rightTrigger.getFirst(),
							rightTrigger.getSecond() + (leftData != null ? leftData.words.size() : 0));// 再加上左边句子的长度
					List<String> triggerPriorityList = triggerExtract_condition2(testData);
					trigger = triggerPriorityList.indexOf(leftTrigger) < triggerPriorityList.indexOf(rightTrigger)
							? leftTrigger : rightTrigger;
				} else if (leftTrigger != null)
				{
					trigger = leftTrigger;
				} else if (rightTrigger != null)
					trigger = new Pair<String, Integer>(rightTrigger.getFirst(),
							rightTrigger.getSecond() + (leftData != null ? leftData.words.size() : 0));// 再加上左边句子的长度
			}
		}
		if (trigger == null)
		{
			return;
		}
		testData.data.triggerWord = trigger.getFirst();
		testData.triggerPos = trigger.getSecond();

	}
	/**
	 * 判断是否有实体
	 * @param testData 测试数据（已预处理）
	 * @return 有 ：true
	 */
	private boolean hasNameEntity(Data testData)
	{
		// TODO Auto-generated method stub
		List<String> words = testData.words;
		int id = words.indexOf(":");
		if (id == -1)
			id = words.indexOf("：");
		for (id--; id >= 0; id--)
		{
			if (isChinesePunctuation(words.get(id)))
			{
				break;
			}
			if (testData.nerArrs[id].indexOf("nr") != -1 || testData.nerArrs[id].indexOf("nt") != -1
					|| testData.nerArrs[id].indexOf("ns") != -1)
			{
				return true;
			}
		}
		return false;
	}

	// 根据UnicodeBlock方法判断中文标点符号
	public boolean isChinesePunctuation(String s)
	{
		if (s.length() != 1)
			return false;
		char c = s.charAt(0);
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS || ub == Character.UnicodeBlock.VERTICAL_FORMS)
		{
			return true;
		} else
		{
			return false;
		}
	}
	/**
	 * 判断当前V是否是触发词，利用相似度阈值计算
	 * @param verb 动词
	 * @return 是候选触发词：true
	 */
	private boolean isTrigger(String verb)
	{
		Random rand = new Random();
		double maxScore = -1;
		int eventType = -1;
		for (int i = 0; i < 20; i++)
		{
			double score = EventTypeThresholdController.getSimilarScore(verb, i + 1);
			if (score >= thresholds[i])
			{
				return true;
			}
		}
		return false;
	}
	/**
	 * 
	 * @param word 单词
	 * @param triggerList 触发词列表
	 * @return 距离，余弦相似度
	 */
	private double getDis(String word, ArrayList<String> triggerList)
	{
		double score = 0.0;
		int id = 0;
		HashMap<String, float[]> embedding = dataStorer.word2Vec;
		if (embedding.containsKey(word))
		{
			for (String trigger : triggerList)
			{
				if (embedding.containsKey(trigger))
				{
					id++;
					score += getCosinDis(embedding.get(word), embedding.get(trigger));
				}
			}
		} else
		{
			return 0.0;
		}
		return score / (double) id;
	}
	/**
	 * 抽取情况3，只抽动词
	 * @param testData 测试数据
	 * @return pair<触发词，触发词id>
	 */
	private Pair<String, Integer> triggerExtract3(Data testData)
	{
		// TODO Auto-generated method stub
		if (testData == null)
		{
			return null;
		}
		// 当处理的句子分词后长度>30就不处理
		if (testData.words.size() > 30)
		{
			return null;
		}
		List<Integer> triggerPosList = new ArrayList<>();
		ArrayList<Feature> featureObjList_V = featureExtractor.extractFeature_V(testData, "testing", triggerPosList);// 抽取特征
		double maxProb = 0;
		String predictedTrigger = null;
		// int eventType = -1;
		int pos = 0;
		int predictedTriggerPos = -1;
		for (Feature featureObj : featureObjList_V)
		{
			String[] featureArray = featureObj.toArray();
			String trigger = featureObj.triggerWord;
			if (!isTrigger(trigger))
				continue;
			int triggerPos = triggerPosList.get(pos++);
			if (!isStandard(testData, triggerPos))
			{
				continue;
			}
			double[] outcomes = maxentClassifier2.predict(featureArray);
			int trueIndex = maxentClassifier2.classifier_maxent.getIndex("Yes");
			String val = maxentClassifier2.classifier_maxent.getBestOutcome(outcomes);
			// System.out.println("V"+trigger+"
			// "+maxentClassifier2.classifier_maxent.getAllOutcomes(outcomes));//输出
			double triggerProb = outcomes[trueIndex];
			if (trigger.equals(testData.HeadWord))
			{
				triggerProb += 0.2;
			}
			if (triggerProb > maxProb)
			{
				maxProb = triggerProb;
				predictedTrigger = trigger;
				predictedTriggerPos = triggerPos;
			}
		}
		if (predictedTrigger == null)
		{
			return null;
		}
		return new Pair<String, Integer>(predictedTrigger, predictedTriggerPos);
	}
	/**
	 * 抽取情况2，只抽在触发词列表中出现的候选词
	 * @param testData 测试数据
	 * @return pair<触发词，触发词id>
	 */
	private Pair<String, Integer> triggerExtract2(Data testData)
	{
		// TODO Auto-generated method stub
		if (testData == null)
		{
			return null;
		}
		// 当处理的句子分词后长度>40就不处理
		int[] prioriys = new int[]
		{ 11, 12, 13, 8, 10, 3 };
		if (testData.words.size() > 40)
		{
			return null;
		}
		List<Integer> triggerPosList = new ArrayList<>();
		ArrayList<Feature> featureObjList = featureExtractor.extractFeature(testData, "testing", triggerPosList,
				eventTypeExtractor);

		HashMap<String, Double> triggerProbMap = new HashMap<>();
		double maxProb = 0;
		String predictedTrigger = null;
		int predictedTriggerPos = -1;
		int pos = 0;
		for (Feature featureObj : featureObjList)// 识别到触发词
		{
			String trigger = featureObj.triggerWord;
			int triggerPos = triggerPosList.get(pos++);
			if (!isStandard(testData, triggerPos))
			{
				continue;
			}
			String[] featureArray = featureObj.toArray();
			double[] outcomes = maxentClassifier1.predict(featureArray);
			int trueIndex = maxentClassifier1.classifier_maxent.getIndex("Yes");
			String val = maxentClassifier1.classifier_maxent.getBestOutcome(outcomes);
			double triggerProb = outcomes[trueIndex];
			if (trigger.equals(testData.HeadWord))
			{
				triggerProb += 0.2;
			}
			int type = eventTypeExtractor.template.getEventType(trigger);
			if (type == 1 || type == 2)
			{
				triggerProb -= 0.3;
			}
			if (Arrays.contains(prioriys, type))
			{
				triggerProb += 0.3;
			}
			if (triggerProb > maxProb)
			{
				maxProb = triggerProb;
				predictedTrigger = trigger;
				predictedTriggerPos = triggerPos;
			}
		}

		if (predictedTrigger != null)
			return new Pair<String, Integer>(predictedTrigger, predictedTriggerPos);
		return null;
	}
	/**
	 *  * 抽取情况1，抽取动词
	 * @param testData 测试数据
	 * @param triggerList 触发词列表
	 * @return pair<触发词，触发词id>
	 */
	private Pair<String, Integer> triggerExtract1(Data testData, ArrayList<String> triggerList)
	{ // 主动词是否是触发词
		// 且has
		// 主语
		// TODO Auto-generated method stub
		if (testData == null)
		{
			return null;
		}
		String headWord = testData.HeadWord;
		int headwordId = testData.headWordId;
		if (triggerList.contains(headWord) && isStandard(testData, headwordId)
				&& testData.nerArrs[headwordId].indexOf("other") != -1)
		{
			if (eventTypeExtractor.template.getEventType(headWord) != 1)
			{
				return new Pair<String, Integer>(headWord, headwordId);
			}

		}
		return null;
	}
	/**
	 * trigger的路径是否有SBV
	 * @param testData 
	 * @param headwordId 
	 * @return
	 */
	private boolean isStandard(Data testData, int headwordId)
	{// trigger的路径是否有SBV
		// TODO Auto-generated method stub
		if (!testData.tags.get(headwordId).equals("v") || testData.depResult.getSecond().get(headwordId).equals("ATT"))
		{
			return false;
		}
		for (int i = 0; i < headwordId; i++)
		{
			if (testData.nerArrs[i].indexOf("b_") != -1)
			{
				return true;
			}
		}
		return false;
	}
	
	private List<String> triggerExtract_condition2(Data testData)
	{
		// TODO Auto-generated method stub
		List<String> triggerPriorityList = new ArrayList<>();
		List<Integer> triggerPos = new ArrayList<>();
		ArrayList<Feature> featureObjList_V = featureExtractor.extractFeature_V(testData, "testing", triggerPos);// 抽取特征
		double maxProb = 0;
		String predictedTrigger = null;
		HashMap<String, Double> triggerProbMap = new HashMap<>();
		for (Feature featureObj : featureObjList_V)
		{
			String[] featureArray = featureObj.toArray();
			String trigger = featureObj.triggerWord;
			double[] outcomes = maxentClassifier2.predict(featureArray);
			int trueIndex = maxentClassifier2.classifier_maxent.getIndex("Yes");
			String val = maxentClassifier2.classifier_maxent.getBestOutcome(outcomes);
			// System.out.println("V"+trigger+"
			// "+maxentClassifier2.classifier_maxent.getAllOutcomes(outcomes));//输出
			double triggerProb = outcomes[trueIndex];
			if (trigger.equals(testData.HeadWord))
			{
				triggerProb += 0.2;
			}
			triggerProbMap.put(trigger, triggerProb);
		}
		if (triggerProbMap.size() == 0)
		{
			return triggerPriorityList;
		} // 表示没有候选触发词
		List<Map.Entry<String, Double>> list = new ArrayList<>(triggerProbMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>()
		{

			@Override
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2)
			{
				// TODO Auto-generated method stub
				if (o2.getValue() > o1.getValue())
					return 1;
				else if (o2.getValue() < o1.getValue())
					return -1;
				else
					return 0;
			}
		});
		for (Map.Entry<String, Double> entry : list)
		{
			triggerPriorityList.add(entry.getKey());
		}
		return triggerPriorityList;

	}

	private List<String> triggerExtract_condition1(Data testData)
	{
		// TODO Auto-generated method stub
		List<String> triggerPriorityList = new ArrayList<>();
		List<Integer> triggerPos = new ArrayList<>();
		ArrayList<Feature> featureObjList = featureExtractor.extractFeature(testData, "testing", triggerPos,
				eventTypeExtractor);
		HashMap<String, Double> triggerProbMap = new HashMap<>();
		double maxProb = 0;
		String predictedTrigger = null;
		// int eventType = -1;
		for (Feature featureObj : featureObjList)// 识别到触发词
		{
			String trigger = featureObj.triggerWord;
			String[] featureArray = featureObj.toArray();
			double[] outcomes = maxentClassifier1.predict(featureArray);
			int trueIndex = maxentClassifier1.classifier_maxent.getIndex("Yes");
			String val = maxentClassifier1.classifier_maxent.getBestOutcome(outcomes);
			double triggerProb = outcomes[trueIndex];
			triggerProbMap.put(trigger, triggerProb);
		}
		for (java.util.Map.Entry<String, Double> entry : triggerProbMap.entrySet())
		{
			String trigger = entry.getKey();
			double triggerProb = entry.getValue();
			if (trigger.equals(testData.HeadWord))
			{
				triggerProb += 0.2;
			}
			if (eventTypeExtractor.template.getEventType(trigger) == 1)
			{
				triggerProb -= 0.1;
			}
		}
		if (triggerProbMap.size() == 0)
		{
			return triggerPriorityList;
		} // 表示没有候选触发词
		List<Map.Entry<String, Double>> list = new ArrayList<>(triggerProbMap.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Double>>()
		{

			@Override
			public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2)
			{
				// TODO Auto-generated method stub
				if (o2.getValue() > o1.getValue())
					return 1;
				else if (o2.getValue() < o1.getValue())
					return -1;
				else
					return 0;
			}
		});
		for (Map.Entry<String, Double> entry : list)
		{
			triggerPriorityList.add(entry.getKey());
		}
		return triggerPriorityList;
	}

	private ArrayList<String> getTrigger(Data testData)
	{
		// TODO Auto-generated method stub
		ArrayList<String> triggerList = new ArrayList<>();
		int wordId = 0;
		for (String word : testData.words)
		{
			String pos = testData.tags.get(wordId);
			if (eventTypeExtractor.template.containsKey(word))
			{
				triggerList.add(word);
			}
			wordId++;
		}
		return triggerList;
	}

	public void extract_MainWord(Data testData)
	{
		testData.data.triggerWord = testData.HeadWord;
	}

	private float getCosinDis(float[] vector, float[] vec)
	{
		// TODO Auto-generated method stub
		double vectorNorm = 0.0d, vecNorm = 0.0d;
		for (int i = 0; i < vector.length; i++)
		{
			vectorNorm += vector[i] * vector[i];
			vecNorm += vec[i] * vec[i];
		}
		vectorNorm = Math.sqrt(vectorNorm);
		vecNorm = Math.sqrt(vecNorm);
		double cosinDis = 0.0d;
		for (int i = 0; i < vector.length; i++)
		{
			cosinDis += vector[i] * vec[i];
		}
		return (float) (cosinDis / (vecNorm * vectorNorm));
	}

	class PairCompare implements Comparator<Pair<Float, Integer>>
	{

		@Override
		public int compare(Pair<Float, Integer> o1, Pair<Float, Integer> o2)
		{
			// TODO Auto-generated method stub
			if (o1.getFirst() > o2.getFirst())
			{
				return -1;
			}
			return 1;
		}

	}

	public static void main(String[] args)
	{
		//data initial
		Data data = new Data(new LabelItem("", "", "", "美国制裁日本"));
		data.setTrainData();
		//new object
		TriggerExtractor e = new TriggerExtractor();
		e.extract(data);
		System.out.println("触发词："+data.data.triggerWord);
	}

}
