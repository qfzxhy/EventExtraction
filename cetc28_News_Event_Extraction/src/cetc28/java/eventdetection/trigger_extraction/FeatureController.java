package cetc28.java.eventdetection.trigger_extraction;

/*  
* 创建时间：2016年3月19日 下午6:58:08  
* 项目名称：Java_EventDetection_News  
* @author GreatShang  
* @version 1.0   
* @since JDK 1.8.0_21  
* 文件名称：FeatureController.java  
* 系统信息：Windows Server 2008
* 类说明：  
* 功能描述：
*/


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.preprocessing.LoadDataSingleton;
import cetc28.java.eventdetection.triggertype_extraction.EventTypeExtraction;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import opennlp.tools.ml.model.MaxentModel;
/**
 * 特征抽取类
 * @author qf
 *
 */
public class FeatureController {

	/*
	 * feature by "Automatic Event Trigger Word Extraction in Chinese"
	 */
	public int x = 0; //左窗口
	public int y = 0;//right窗口
	public boolean wordFeature = false; //词特征
	public boolean lexicalFeature = false;//词性特征
	public boolean syntacticFeature = false;//句法特征
	public boolean semanticFeature = false;//语义特征
	public boolean entityFeature = false;//实体特征
	public boolean binFeature = false;//字特征
	public boolean binSemanticFeature = false;//字特征
	public boolean triggerTypeFeature = false;//触发词类别特征
	public LoadDataSingleton dataStorer = null;
	public FeatureController() {
		// TODO Auto-generated constructor stub
		/*
		 * word lexical syntactic semantic bin binSemantic triggerType
		 */
		dataStorer = LoadDataSingleton.getInstance();
		x = 3;
		y = 2;
		wordFeature = true;
		lexicalFeature = true;
		syntacticFeature = true;
		semanticFeature = true;
		entityFeature = false;
		binFeature = true;
		binSemanticFeature = true;
		triggerTypeFeature = true;
	}
	/**
	 * 抽取特征（匹配到候选触发词情况）
	 * @param data 数据
	 * @param trainOrTest 训练或者是测试 
	 * @param triggerPos 候选触发词下标
	 * @param eventTypeExtractor 事件抽取类
	 * @return
	 */
	public ArrayList<Feature> extractFeature(Data data,String trainOrTest,List<Integer> triggerPos, EventTypeExtraction eventTypeExtractor)
	{
		ArrayList<Feature> featureList = new ArrayList<>();
		List<String> wordList = data.words;
		List<String> tagList = data.tags;
		String HeadWord = data.HeadWord;
		Pair<List<Integer>, List<String>> depResult = data.depResult;
		for(int i = 0;i<tagList.size();i++)
		{
			boolean isCandidate = false;
			String word = wordList.get(i);
			String pos = tagList.get(i);
			if(data.nerArrs[i].indexOf("other") == -1){continue;}
			if(eventTypeExtractor.template.containsKey(word))//triggerNumber.containsKey(word)
			{
				if(triggerPos != null)//下标
					triggerPos.add(i);
				ArrayList<String> wordPath = new ArrayList<>();
				ArrayList<String> lexicalPath = new ArrayList<>();
				ArrayList<String> relationPath = new ArrayList<>();
				int wordId = LtpTool.getPath(i, depResult,wordList,tagList,wordPath,relationPath,lexicalPath);
				Feature feature = new Feature();
				int id = -x;
				for(int j = i-x;j<=i+y;j++)
				{
					WordInfo wordInfo = new WordInfo();
					if(wordFeature)
					{
						if(j>=0&&j<wordList.size())
							wordInfo.setWord(wordList.get(j)+"_"+String.valueOf(id)+"w");
						else
							wordInfo.setWord("null"+"_"+String.valueOf(id)+"w");
					}
					if(lexicalFeature)
					{
						if(j>=0&&j<tagList.size())
							wordInfo.setLexical(tagList.get(j)+"_"+String.valueOf(id)+"t");
						else
							wordInfo.setLexical("null"+"_"+String.valueOf(id)+"t");
					}
					if(syntacticFeature)
					{
						if(relationPath.get(wordId).equals("HED"))
						{
							wordInfo.setSyntactic("1_"+"s");
						}
					}
					id++;
					feature.wordFeatureList.add(wordInfo);
				}
				if(semanticFeature)
				{
					if(dataStorer.wordSemanticInfo.containsKey(wordList.get(i)))
					{
						feature.setSemanticEntry(dataStorer.wordSemanticInfo.get(wordList.get(i))+"_"+"s");
					}
						
				}
				if(binFeature)
				{
					String triggerWord = wordPath.get(wordId);
					if(triggerWord.length() == 2){feature.setBin1(triggerWord.substring(0, 1));feature.setBin2(triggerWord.substring(1, 2));}
					if(triggerWord.length() == 1){feature.setBin1(triggerWord);feature.setBin2(triggerWord);}
					//if(triggerWord.length() == 3){feature.setBin1(triggerWord);feature.setBin2(triggerWord);}
					if(triggerWord.length() == 4){feature.setBin1(triggerWord.substring(0, 2));feature.setBin2(triggerWord.substring(2, 4));}
				}
				if(binSemanticFeature)
				{
					feature.setBin1_SemanticEntry(dataStorer.wordSemanticInfo.get(feature.bin1));
					feature.setBin2_SemanticEntry(dataStorer.wordSemanticInfo.get(feature.bin2));
				}
				if(triggerTypeFeature)
				{
					feature.set_triggerTypeFeature(eventTypeExtractor.template.getEventType(word));
				}
				if(entityFeature)
				{
					int left = wordId,right = wordId;
					while(left>=0)
					{
						if(lexicalPath.get(left).equals("nh"))
						{
							feature.setLeftEntityType("nh_left");
							break;
						}//i l s
						if(lexicalPath.get(left).equals("ni"))
						{
							feature.setLeftEntityType("ni_left");
							break;
						}//i l s
						if(lexicalPath.get(left).equals("nl"))
						{
							feature.setLeftEntityType("nl_left");
							break;
						}//i l s
						if(lexicalPath.get(left).equals("ns"))
						{
							feature.setLeftEntityType("ns_left");
							break;
						}//i l s
						left -- ;
					}
					while(right<lexicalPath.size())
					{
						if(lexicalPath.get(right).equals("nh"))
						{
							feature.setRightEntityType("nh_right");
							break;
						}//i l s
						if(lexicalPath.get(right).equals("ni"))
						{
							feature.setRightEntityType("ni_right");
							break;
						}//i l s
						if(lexicalPath.get(right).equals("nl"))
						{
							feature.setRightEntityType("nl_right");
							break;
						}//i l s
						if(lexicalPath.get(right).equals("ns"))
						{
							feature.setRightEntityType("ns_right");
							break;
						}//i l s
						right ++ ;
					}
					if(feature.left_entityType==null){feature.setLeftEntityType("null_left");}
					if(feature.right_entityType == null)feature.setRightEntityType("null_right");
				}
				if(trainOrTest.equals("training"))
				{
					if(wordList.get(i).equals(data.data.triggerWord))
					{
						feature.setLabel("Yes");
					}else
					{
						feature.setLabel("No");
					}
				}else
				if(trainOrTest.equals("testing"))
				{
					feature.setTrigger(wordList.get(i));
				}
				featureList.add(feature);
			}
		}
		return featureList;
	}
	/**
	 * 抽取特征（没有匹配的触发词，对核心动词分类）
	 * @param data 数据
	 * @param trainOrTest 训练或者测试
	 * @param triggerPos 候选触发词下标
	 * @return
	 */
	public ArrayList<Feature> extractFeature_V(Data data,String trainOrTest,List<Integer> triggerPos) {
		// TODO Auto-generated method stub
		ArrayList<Feature> featureList = new ArrayList<>();
		List<String> wordList= data.words;
		List<String> tagList = data.tags;
		String HeadWord = data.HeadWord;
		ArrayList<Integer> verbIdList = data.verbIdList;
		Pair<List<Integer>, List<String>> depResult = data.depResult;
		for(int k = 0;k<verbIdList.size();k++)
		{
			int i = verbIdList.get(k);
			//当前sentence命名实体识别的i下表所对应的结果必须是“other”，如果不是“other”则代表改词是一个实体，不应该作为触发词
			if(data.nerArrs[i].indexOf("other") == -1){continue;}
			if(triggerPos != null)
				triggerPos.add(i);
			boolean isCandidate = false;
			String word = wordList.get(i);
			if(tagList.get(i).matches("v"))
			{
				ArrayList<String> wordPath = new ArrayList<>();
				ArrayList<String> lexicalPath = new ArrayList<>();
				ArrayList<String> relationPath = new ArrayList<>();
				int wordId = LtpTool.getPath(i, depResult,wordList,tagList,wordPath,relationPath,lexicalPath);
				Feature feature = new Feature();
				int id = -x;
				for(int j = i-x;j<=i+y;j++)
				{
					WordInfo wordInfo = new WordInfo();
					if(wordFeature)
					{
						if(j>=0&&j<wordList.size())
							wordInfo.setWord(wordList.get(j)+"_"+String.valueOf(id)+"w");
						else
							wordInfo.setWord("null");
					}
					if(lexicalFeature)
					{
						if(j>=0&&j<tagList.size())
							wordInfo.setLexical(tagList.get(j)+"_"+String.valueOf(id)+"t");
						else
							wordInfo.setLexical("null");
					}
					if(syntacticFeature)
					{
						if(relationPath.get(wordId).equals("HED"))
						{
							wordInfo.setSyntactic("1_"+"s");
						}
					}
					id++;
					feature.wordFeatureList.add(wordInfo);
				}
				if(semanticFeature)
				{
					if(dataStorer.wordSemanticInfo.containsKey(wordList.get(i)))
					{
						feature.setSemanticEntry(dataStorer.wordSemanticInfo.get(wordList.get(i))+"_"+"s");
					}
					else
					{
						//feature.setSemanticEntry("null"+"_"+"s");
					}
				}
				if(binFeature)
				{
					String triggerWord = wordList.get(i);
					if(triggerWord.length() == 2){feature.setBin1(triggerWord.substring(0, 1));feature.setBin2(triggerWord.substring(1, 2));}
					if(triggerWord.length() == 1){feature.setBin1(triggerWord);feature.setBin2(triggerWord);}
					//if(triggerWord.length() == 3){feature.setBin1(triggerWord);feature.setBin2(triggerWord);}
					if(triggerWord.length() == 4){feature.setBin1(triggerWord.substring(0, 2));feature.setBin2(triggerWord.substring(2, 4));}
				}
				if(binSemanticFeature)
				{
					if(dataStorer.wordSemanticInfo.containsKey(feature.bin1))
						feature.setBin1_SemanticEntry(dataStorer.wordSemanticInfo.get(feature.bin1));
					
						//feature.setBin1_SemanticEntry("null_");
					if(dataStorer.wordSemanticInfo.containsKey(feature.bin2))
						feature.setBin2_SemanticEntry(dataStorer.wordSemanticInfo.get(feature.bin2));
				}
				if(entityFeature)
				{
					int left = i,right = i;
					while(left>=0)
					{
						if(lexicalPath.get(left).equals("nh"))
						{
							feature.setLeftEntityType("nh_left");
							break;
						}//i l s
						if(lexicalPath.get(left).equals("ni"))
						{
							feature.setLeftEntityType("ni_left");
							break;
						}//i l s
						if(lexicalPath.get(left).equals("nl"))
						{
							feature.setLeftEntityType("nl_left");
							break;
						}//i l s
						if(lexicalPath.get(left).equals("ns"))
						{
							feature.setLeftEntityType("ns_left");
							break;
						}//i l s
						left -- ;
					}
					
					while(right<lexicalPath.size())
					{
						if(lexicalPath.get(right).equals("nh"))
						{
							feature.setRightEntityType("nh_right");
							break;
						}//i l s
						if(lexicalPath.get(right).equals("ni"))
						{
							feature.setRightEntityType("ni_right");
							break;
						}//i l s
						if(lexicalPath.get(right).equals("nl"))
						{
							feature.setRightEntityType("nl_right");
							break;
						}//i l s
						if(lexicalPath.get(right).equals("ns"))
						{
							feature.setRightEntityType("ns_right");
							break;
						}//i l s
						right ++ ;
					}
					if(feature.left_entityType==null){feature.setLeftEntityType("null_left");}
					if(feature.right_entityType == null)feature.setRightEntityType("null_right");
				}
				if(trainOrTest.equals("training"))
				{
					if(wordList.get(i).equals(data.data.triggerWord))
					{
						feature.setLabel("Yes");
					}else
					{
						feature.setLabel("No");
					}
				}else
				if(trainOrTest.equals("testing"))
				{
					feature.setTrigger(wordList.get(i));
				}
				featureList.add(feature);
			}
		}
		return featureList;
	}
}
