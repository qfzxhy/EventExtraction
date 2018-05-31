package cetc28.java.eventdetection.textrank;

import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.entity_extraction.FindActorandPerson;
import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.news.label.ActorItem;
import cetc28.java.news.label.EventItem;
import cetc28.java.nlptools.LtpTool;
import edu.hit.ir.ltp4j.Segmentor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TextRank 自动摘要
 * 
 * @author qianf
 */
public class TextRankSummary
{
	/**
	 * 阻尼系数（ＤａｍｐｉｎｇＦａｃｔｏｒ），一般取值为0.85
	 */
	final double d = 0.85f;
	/**
	 * 最大迭代次数
	 */
	final int max_iter = 200;
	final double min_diff = 0.001f;
	/**
	 * 文档句子的个数
	 */
	int D;
	/**
	 * 拆分为[句子[单词]]形式的文档
	 */
	List<List<String>> docs;
	/**
	 * 排序后的最终结果 score <-> index
	 */
	TreeMap<Double, Integer> top;

	/**
	 * 句子和其他句子的相关程度
	 */
	double[][] weight;
	/**
	 * 该句子和其他句子相关程度之和
	 */
	double[] weight_sum;
	/**
	 * 迭代之后收敛的权重
	 */
	double[] vertex;

	/**
	 * BM25相似度
	 */
	BM25 bm25;
	FindActorandPerson findActor;

	ArrayList<String> stopTermList = new ArrayList<String>();

	public TextRankSummary(List<List<String>> docs)
	{
		this.docs = docs;
		bm25 = new BM25(docs);
		D = docs.size();
		weight = new double[D][D];
		weight_sum = new double[D];
		vertex = new double[D];
		top = new TreeMap<Double, Integer>(Collections.reverseOrder());
	}

	public TextRankSummary(FindActorandPerson findActor)
	{
		try
		{
			LoadData(FileConfig.getStopWordsPath());
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.findActor = findActor;
	}

	public TextRankSummary()
	{
		// TODO Auto-generated constructor stub
	}
	/**
	 * 对正文做摘要，并过滤掉正文中一些指定的句子
	 * 
	 * @param content
	 * @param eventlist
	 */
	public void RemoveNoise(List<String> content, List<EventItem> eventlist)
	{
		/**
		 * 对句子做摘要
		 */
		int topSentenceIndex[] = getTopSents(content, 20);
		for (int i = 0; i < content.size(); i++)
		{
			String sentence = content.get(i).trim();
			/**
			 * 去掉新闻中特殊的空白格式
			 */
			while (Pattern.matches("　.*", sentence))
				sentence = sentence.substring(1);
			while (sentence.startsWith("    "))
				sentence = sentence.replaceFirst("    ", "");

			boolean flag = true;

			/**
			 * 过滤掉新闻中所有字数过小或者分句过多的字符
			 */
			if (sentence.split("，").length > 4 || sentence.length() <= 10)
				flag = false;
			/**
			 * 过滤掉所有句子中没有实体的
			 */
			else
			{
				ActorItem actorall = Ner.ner_actionitem(sentence);
				if (actorall == null || actorall.actor == null || actorall.actor.trim().length() == 0)
					flag = false;
			}
			// 句子不需要过滤
			if (flag == true)
				eventlist.add(new EventItem(sentence, false));// 非模板句子，可抽取事件
			else// 过滤掉句子
				eventlist.add(new EventItem(sentence, true));// 模板句子，不抽取事件
			/**
			 *  若当前句子在关键句中，标记之
			 */
			if (contains(topSentenceIndex, i) == true)
				eventlist.get(eventlist.size() - 1).setIf_summary(true);
		}
	}

	/**
	 * 获取前几个关键句子
	 * 
	 * @param size
	 *            要几个
	 * @return 关键句子的下标
	 */
	public int[] getTopSentence(int size)
	{
		Collection<Integer> values = top.values();
		size = Math.min(size, values.size());
		int[] indexArray = new int[size];
		Iterator<Integer> it = values.iterator();
		for (int i = 0; i < size; ++i)
		{
			indexArray[i] = it.next();
		}
		return indexArray;
	}


	/**
	 * 将文章分成每一段，并把每一段分割为句子
	 * 
	 * @param document
	 * @return
	 */
	public static List<String> spiltSentence(String document)
	{
		if (document == null)
			return null;
		List<String> sentences = new ArrayList<String>();
		/*
		 * 对每段正文进行处理
		 */
		for (String phase : document.split("[\r\n]"))
		{
			phase = phase.trim();
			if (phase.length() == 0)
				continue;
			String[] lineList = split_sentence(phase);
			if (lineList == null || lineList.length == 0)
				return null;
			for (String sent : lineList)
			{
				sent = sent.trim();
				/**
				 * 过滤掉某些长度过小的句子
				 */
				if (sent.length() <= 1)
					continue;
				else
					sentences.add(sent);
			}
		}
		return sentences;
	}

	/**
	 * 将文章中的每一段分割成句子
	 * @param str
	 * @return
	 */
	public static String[] split_sentence(String str)
	{
		// TODO Auto-generated method stub
		/* 正则表达式：句子结束符 */
		if (str == null)
			return null;
		String regEx = "；”|;”|？”|\\?”|！”|!”|。”" + "|”；|”;|”？|”\\?|”！|”!|”。" + "|；\"|;\"|？\"|\\?\"|！\"|!\"|。\""
				+ "|\"；|\";|\"？|\"\\?|\"！|\"!|\"。" + "|；|;|？|\\?|！|!|。|　| "; 
		Pattern p = Pattern.compile(regEx);
		Matcher m = p.matcher(str);
		/* 按照句子结束符分割句子 */
		String[] sentence = p.split(str);

		/* 将句子结束符连接到相应的句子后 */
		if (sentence.length > 0)
		{
			int count = 0;
			while (count < sentence.length)
			{
				if (m.find())
				{
					sentence[count] += m.group();
				}
				count++;
			}
		}
		return sentence;
	}

	public void LoadData(String filePath) throws IOException
	{
		BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
		String s = null;
		while ((s = bufferedReader.readLine().trim()) != null && !s.equals(""))
		{
			stopTermList.add(s);
		}
	}

	
	/**
	 * 一句话调用接口
	 * 
	 * @param document
	 *            目标文档
	 * @param k
	 *            需要的关键句的个数
	 * @return 关键句列表
	 */
	public int[] getTopSents(List<String> sentList, int k)
	{
		List<List<String>> docs = new ArrayList<List<String>>();
		for (String sentence : sentList)
		{
			List<String> wordList = new ArrayList<>();
			List<String> termList = new ArrayList<>();
			Segmentor.segment(sentence, termList);
			for (String term : termList)
			{
				if (shouldInclude(term) == true)
				{
					wordList.add(term);
				}
			}
			docs.add(wordList);
		}
		TextRankSummary textRankSummary = new TextRankSummary(docs);
		int[] topSentence = textRankSummary.getTopSentence(k);
		return topSentence;
	}

	/**
	 * 是否应当将这个term纳入计算
	 * @param term
	 * @return 是否应当
	 */
	public boolean shouldInclude(String term)
	{
		if (stopTermList == null || stopTermList.size() == 0)
			return true;
		for (String sterm : stopTermList)
		{
			if (sterm.trim().equals(term.trim()))
				return false;
		}
		return true;
	}

	private boolean contains(int[] contentArr, int content)
	{
		// TODO Auto-generated method stub
		if (contentArr == null || contentArr.length == 0)
			return false;
		for (int c : contentArr)
		{
			if (c == content)
				return true;
		}
		return false;
	}

	public static void main(String[] args)
	{
		LtpTool ltpTool = new LtpTool();
		TextRankSummary textRankSummary = new TextRankSummary();
		String document = "部队动向：韩美联军出动航母在黄海演习 点击图片进入下一页 资料图：韩国组建针对朝鲜核设施的航空特种部队";
		List<String> list = textRankSummary.spiltSentence(document);
		System.out.println(list);
	}
}
