package cetc28.java.eventdetection.ruleextractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.triggertype_extraction.EventTypeExtraction;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.Pair;
/**
 * 
 * @author qianf
 *用模板抽取事件类
 */
public class Extractor_Rule
{
	final int[] priorityArr =
	{ 12, 11, 10, 16, 14, 17, 13, 18, 19, 20, 8, 15, 3, 5, 6, 7, 9, 4, 2, 1 };
	public ArrayList<ArrayList<Rule>> ruleListList = null;// 所有模板
	public String rulePackagePath = null;
	public Extractor_Rule(String rulePackagePath)
	{
		// TODO Auto-generated constructor stub
		ruleListList = new ArrayList();
		this.rulePackagePath = rulePackagePath;
		loadRule();
	}
	/**
	 * load rule 并且解析
	 */
	private void loadRule()
	{
		// TODO Auto-generated method stub
		File[] paths = findFiles(rulePackagePath);
		for (File path : paths)
		{
			ArrayList<Rule> ruleList = new ArrayList<>();
			String pathStr = path.getName();
			int ruleType = Integer.parseInt(pathStr.substring(0, 2));
			BufferedReader br = null;
			try
			{
				br = new BufferedReader(new FileReader(path));
				String lineStr = "";
				while ((lineStr = br.readLine()) != null)
				{
					if (lineStr.trim().equals(""))
					{
						continue;
					}
//					System.out.println(lineStr);
					ruleList.add(RuleParser.ruleParsing(lineStr, ruleType));
					if(ruleList.get(ruleList.size()-1).coreWords == null)
					{
						System.out.println(path.toString());
					}
				}
				br.close();
			} catch (FileNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ruleListList.add(ruleList);
		}
	}

	private File[] findFiles(String rulePackagePath)
	{
		// TODO Auto-generated method stub
		ArrayList<String> filePathList = new ArrayList<>();
		File f = null;
		File[] paths;
		f = new File(rulePackagePath);
		paths = f.listFiles();
		return paths;
	}

	/*
	 * source(参加)target{军演 演习} len =2 (参加)target{军演 演习}{的}source len =3
	 */
	/**
	 * 如果多个模板同时匹配，那么选择最长的模板,考虑优先级
	 * @param rule 当前规则字符串
	 * @return 字符串几何长度
	 */
	private int getRuleLen(String rule)
	{
		if (rule == null)
		{
			return 0;
		}
		int len = 0;
		for (int i = 0; i < rule.length(); i++)
		{
			if (rule.charAt(i) == '(' || rule.charAt(i) == '{' || rule.charAt(i) == '[')
			{
				len++;
			}
		}
		return len;
	}

	/*
	 * 
	 */
	/**
	 * 
	 * @param testData	输入数据
	 * @return 抽取结果
	 */ 
	public Rule extract(Data testData)// 第一类优先级最低
	{
		ArrayList<String> triggerList = new ArrayList<>();
		Rule ruleMatched = null;
		String ruleStr = null;
		for (int prioriy : priorityArr)
		{
			ArrayList<Rule> ruleList = ruleListList.get(prioriy - 1);
			int maxLen = Integer.MIN_VALUE;
			for (Rule rule : ruleList)
			{
				String str = rule.isMatchRule(testData);
				if (str != null && getRuleLen(rule.ruleStr) > maxLen)
				{
					ruleStr = str;
					ruleMatched = rule;
					maxLen = getRuleLen(rule.ruleStr);
				}
			}
			if (ruleMatched != null)
			{
//				System.out.println(ruleMatched.ruleStr);
//				System.out.println(ruleStr);
				String ruleStrDisPlay = getDisplayedRule(ruleStr);//_
//				System.out.println(ruleStrDisPlay);
				testData.data.triggerTemplate = new Pair<String, String>(ruleStrDisPlay, ruleStr);
				testData.data.triggerWord = ruleStrDisPlay;
				testData.data.eventType = ruleMatched.ruleType;
				return ruleMatched;
			}
		}
		return null;

	}
	/**
	 * 
	 * @param ruleStr ： source在target航行
	 * @return 显示结果 在_航行
	 */
	private String getDisplayedRule(String ruleStr)
	{
		// TODO Auto-generated method stub
		// 处理情况source在_与target举行_演习
		int id = ruleStr.indexOf("在");
		if (id >= 0 && id + 1 < ruleStr.length() && ruleStr.charAt(id + 1) == '_')
		{
			ruleStr = ruleStr.substring(0, id) + ruleStr.substring(id + 2);
		}
//		ruleStr = ruleStr.replaceAll("_", "");
		ruleStr = ruleStr.replaceAll("source", "_");
		ruleStr = ruleStr.replaceAll("target", "_");
		int begin = 0, end = ruleStr.length();
		if (ruleStr.charAt(0) == '_')
		{
			begin = 1;
		}
		if (ruleStr.charAt(end - 1) == '_')
		{
			end = end - 1;
		}

		return ruleStr.substring(begin, end);
	}

	/*
	 * test 日本与意大利共享军事情报 全面推进安保合作 驻韩美军司令：韩美将在一周内讨论萨德部署事宜 美方否认将就网络问题制裁中国 称需要加强合作
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		//new object
		Extractor_Rule test = new Extractor_Rule(FileConfig.getRulePath());
		//抽取的句子
		String newsTitle = "解放军潜艇首次连射四枚导弹";
		//预处理
		LabelItem data = new LabelItem("", "", "", newsTitle);
		Data testData = new Data(data);
		testData.setTrainData();
		//分词结果
		System.out.println("分词结果："+testData.words);
		//词性标注结果
		System.out.println("词性标注结果："+testData.tags);
		//实体识别结果
		System.out.println("实体识别结果："+Arrays.asList(testData.nerArrs));
		//依存句法结果
		System.out.println("依存句法分析结果："+testData.depResult.getFirst());
		System.out.println("依存句法分析结果："+testData.depResult.getSecond());
		//模板抽取
		test.extract(testData);
		//输出抽取到的触发词
		System.out.println("触发词："+testData.data.triggerWord);;
		//输出抽取到的触发词类别
		System.out.println("触发词类别："+testData.data.eventType);
	}

}
