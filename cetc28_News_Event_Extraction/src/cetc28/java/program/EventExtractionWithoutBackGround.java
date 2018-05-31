package cetc28.java.program;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cetc28.java.dbtool.GeonamesUtil;
import cetc28.java.dbtool.OracleUtil;
import cetc28.java.eventdetection.argument_extraction.ArgumentExtraction;
import cetc28.java.eventdetection.argument_extraction.Methods;
import cetc28.java.eventdetection.argument_extraction.ProcessActor;
import cetc28.java.eventdetection.argument_extraction.ReSegment;
import cetc28.java.eventdetection.argument_extraction.RoleExtract;
import cetc28.java.eventdetection.entity_extraction.FindActorandPerson;
import cetc28.java.eventdetection.entity_extraction.FindLocationforWhole;
import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.sentiment_extraction.CombineEvent;
import cetc28.java.eventdetection.sentiment_extraction.PolarityBasic;
import cetc28.java.eventdetection.sentiment_extraction.SentimentAnlaysis;
import cetc28.java.eventdetection.textrank.TextRankSummary;
import cetc28.java.eventdetection.time_location_extraction.TimeExtraction;
import cetc28.java.news.label.EventItem;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import cetc28.java.nlptools.Stanford_Parser;
import edu.hit.ir.ltp4j.Segmentor;
//事件抽取的主类
public class EventExtractionWithoutBackGround {
	public RoleExtract roleExtract;
	public TextRankSummary textRankSummary;
	public PolarityBasic polarityBasic;
	public RunDetection runDetection;
	public SentimentAnlaysis SA;
	public TimeExtraction timeExraction;
	public ReSegment preprocess;
	public Stanford_Parser stanford_Parser;
	public FindActorandPerson findActorbyDB;
	public Methods methods;
	public CombineEvent combineEvent;	
	public LtpTool ltpTool;
	public ArgumentExtraction argumentExtraction;
	public FindLocationforWhole findActorforWhole;
	public ProcessActor processActor;
	public EventExtractionWithoutBackGround( ) {
		// TODO Auto-generated constructor stub
		this.runDetection = new RunDetection();
		this.methods = new Methods();
		this.ltpTool = new LtpTool();
		this.combineEvent = new CombineEvent();
		this.polarityBasic = new PolarityBasic();
		try {
			SA = new SentimentAnlaysis();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.processActor = new ProcessActor();
		this.argumentExtraction = new ArgumentExtraction();
		this.runDetection = runDetection;
		this.stanford_Parser = new Stanford_Parser();
		
		this.findActorbyDB = new FindActorandPerson(this.preprocess, this.argumentExtraction);
		this.findActorforWhole = new FindLocationforWhole(findActorbyDB);
		this.timeExraction = new TimeExtraction(this.findActorbyDB);
		this.textRankSummary = new TextRankSummary(this.findActorbyDB);
		this.roleExtract = new RoleExtract();
	}

	
	

	/**
	 * 抽取整篇新闻中的所有事件
	 * @param newsID
	 * @param newsURL
	 * @param imgAddress
	 * @param saveTime
	 * @param newsTitle
	 * @param news_content
	 * @return 该篇新闻中每一句话的事件抽取结果的列表
	 * @throws SQLException
	 */
	public Pair<String, Data> extractbysentence1(String newsURL, String imgAddress, String newsID, String saveTime,
			String newsTitle, String sentence, String placeEntity, boolean isSummary) throws SQLException {
//		System.out.println("处理的句子:"+sentence);
		/**
		 * 判空
		 */
		if (sentence == null || sentence.trim().equals(""))
			return null;

		sentence = methods.PreInputTrim(sentence);// 去括号（）
		sentence = methods.PreInputTrim1(sentence);// 去括号【】
		sentence = methods.PreInputTrim2(sentence);// 去括号()
		sentence = methods.removeChinese(sentence);

		if (sentence == null || sentence.trim().equals(""))
			return null;

		boolean isTrigerTemplate = false;

		/**
		 * 抽取触发词和事件类别
		 */
		Data dataResult = this.runDetection.GetEventInforfromNews_Rule(newsURL, newsID, saveTime, sentence);

		if (dataResult != null && dataResult.data != null && dataResult.data.triggerTemplate != null
				&& dataResult.data.triggerTemplate.second != null
				&& !dataResult.data.triggerTemplate.second.trim().equals(""))
			isTrigerTemplate = true;
//		System.out.println(dataResult.data.triggerTemplate);
//		rectify_template(sentence,dataResult.data.triggerTemplate);
		/**
		 * 进一步使用模板和模型进行事件抽取
		 */
		dataResult = this.runDetection.GetEventInforfromNews_MLearning(dataResult);

		if (dataResult == null || dataResult.data == null || dataResult.data.triggerWord == null
				|| dataResult.data.triggerWord.equals(""))
			return new Pair<String, Data>(sentence, dataResult);;

		if (dataResult.data.eventType < 1 || dataResult.data.eventType > 20)
			return new Pair<String, Data>(sentence, dataResult);;
		
		if(!isTrigerTemplate)
		{
			String tmp = "source"+dataResult.data.triggerWord + "target";
			dataResult.data.triggerTemplate = new Pair<String, String>(dataResult.data.triggerWord,tmp);
		}
			
		
		Pair<String, Data> title_result = new Pair<String, Data>(sentence, dataResult);
		
		/**
		 * 抽取source、target
		 */
		this.roleExtract.extractactor(title_result);
		
//		System.out.println(title_result.second.data.triggerTemplate.getFirst());
//		System.out.println(title_result.second.data.sourceActor);
//		System.out.println(title_result.second.depResult.getFirst());
//		System.out.println(title_result.second.depResult.getSecond());
		String sourceActor = title_result.second.data.sourceActor;
		String targetActor = title_result.second.data.targetActor;
		String[] sources = null;
		String[] targets = null;
		if(sourceActor != null)
		{
			sources= title_result.second.data.sourceActor.split("_");
			sources= merge_entitys(title_result.second.words,sources);
			String sourceMerged = "";
			for(int i = 0; i < sources.length;i++)
			{
				sourceMerged += sources[i];
				if(i < sources.length - 1)
				{
					sourceMerged += '_';
				}
			}
			title_result.second.data.sourceActor = sourceMerged;
		}
		if(targetActor != null)
		{
			targets= title_result.second.data.targetActor.split("_");
			targets= merge_entitys(title_result.second.words,targets);
			String targetMerged = "";
			for(int i = 0; i < targets.length;i++)
			{
				targetMerged += targets[i];
				if(i < targets.length - 1)
				{
					targetMerged += '_';
				}
			}
			title_result.second.data.targetActor = targetMerged;
		}
			
		
		String source = extract_by_depenencyTree(title_result.second.data.triggerTemplate.getSecond(), sources, title_result.second.depResult.getFirst(), title_result.second.words);
		
		if(source != null)
		{
			title_result.second.data.sourceActor = source;
		}
		if(title_result.second.data.targetActor != null)
		{
			title_result.second.data.targetActor = title_result.second.data.targetActor.split("_")[0];
		}
		if (title_result == null || title_result.second == null || title_result.second.data == null)
			return null;
		/**
		 * 如果当前source为空的话，不作为一个事件
		 */
		if (title_result.second.data.sourceActor == null || title_result.second.data.sourceActor.trim().equals(""))
			return new Pair<String, Data>(title_result.getFirst(), title_result.getSecond());;

		/**
		 * 去掉target和source中重复的实体
		 */
		this.processActor.Finalprocess(title_result.second.data);

		/**
		 * 处理事件发生的时间
		 */
		this.timeExraction.setTimebyrule(title_result.second.words, title_result.second.tags,
				title_result.second.data, saveTime);
		/**
		 * 事件的发生时间的时间戳表示
		 */
		title_result.second.data.saveTime = this.timeExraction.String2Time(title_result.second.data.eventTime, saveTime);

		/**
		 * 事件句子中所有的命名实体和人名
		 */
		title_result.second.data.actorItem = Ner.ner_actionitem(sentence);
		title_result.second.data.allPerson = findActorbyDB.findallperson(title_result.second.data.actorItem);

		/**
		 * 对事件抽取结果补全赋值
		 */
		title_result.second.data.newsTitle = newsTitle;
		title_result.first = sentence;
		title_result.second.data.setValues(imgAddress, newsURL, newsID, placeEntity, true, sentence);
		methods.RemoveNull(title_result.second);
		String sentimentResult = this.SA.classify_words(title_result.second.words);
		title_result.second.data.Proscore = sentimentResult;
		return new Pair<String, Data>(title_result.first, title_result.second);
	}
	public Pair<String, LabelItem> extractbysentence(String newsURL, String imgAddress, String newsID, String saveTime,
			String newsTitle, String sentence, String placeEntity, boolean isSummary) throws SQLException {
//		System.out.println("处理的句子:"+sentence);
		/**
		 * 判空
		 */
		if (sentence == null || sentence.trim().equals(""))
			return null;

		sentence = methods.PreInputTrim(sentence);// 去括号（）
		sentence = methods.PreInputTrim1(sentence);// 去括号【】
		sentence = methods.PreInputTrim2(sentence);// 去括号()
		sentence = methods.removeChinese(sentence);

		if (sentence == null || sentence.trim().equals(""))
			return null;

		boolean isTrigerTemplate = false;

		/**
		 * 抽取触发词和事件类别
		 */
		Data dataResult = this.runDetection.GetEventInforfromNews_Rule(newsURL, newsID, saveTime, sentence);

		if (dataResult != null && dataResult.data != null && dataResult.data.triggerTemplate != null
				&& dataResult.data.triggerTemplate.second != null
				&& !dataResult.data.triggerTemplate.second.trim().equals(""))
			isTrigerTemplate = true;

		/**
		 * 进一步使用模板和模型进行事件抽取
		 */
		dataResult = this.runDetection.GetEventInforfromNews_MLearning(dataResult);

		if (dataResult == null || dataResult.data == null || dataResult.data.triggerWord == null
				|| dataResult.data.triggerWord.equals(""))
			return null;

		if (dataResult.data.eventType < 1 || dataResult.data.eventType > 20)
			return null;
		
		if(!isTrigerTemplate)
		{
			String tmp = "source"+dataResult.data.triggerWord + "target";
			dataResult.data.triggerTemplate = new Pair<String, String>(dataResult.data.triggerWord,tmp);
		}
			
		
		Pair<String, Data> title_result = new Pair<String, Data>(sentence, dataResult);
		
		/**
		 * 抽取source、target
		 */
		this.roleExtract.extractactor(title_result);
		
//		System.out.println(title_result.second.data.triggerTemplate.getFirst());
//		System.out.println(title_result.second.data.sourceActor);
//		System.out.println(title_result.second.depResult.getFirst());
//		System.out.println(title_result.second.depResult.getSecond());
		String sourceActor = title_result.second.data.sourceActor;
		String targetActor = title_result.second.data.targetActor;
		String[] sources = null;
		String[] targets = null;
		System.out.println(sourceActor);
		if(sourceActor != null)
		{
			sources= title_result.second.data.sourceActor.split("_");
			sources= merge_entitys(title_result.second.words,sources);
			String sourceMerged = "";
			for(int i = 0; i < sources.length;i++)
			{
				sourceMerged += sources[i];
				if(i < sources.length - 1)
				{
					sourceMerged += '_';
				}
			}
			title_result.second.data.sourceActor = sourceMerged;
		}
		System.out.println(targetActor);
		if(targetActor != null)
		{
			System.out.println(targetActor);
			targets= title_result.second.data.targetActor.split("_");
			
			targets= merge_entitys(title_result.second.words,targets);
			String targetMerged = "";
			for(int i = 0; i < targets.length;i++)
			{
				targetMerged += targets[i];
				if(i < targets.length - 1)
				{
					targetMerged += '_';
				}
			}
			title_result.second.data.targetActor = targetMerged;
		}
			
		
		String source = extract_by_depenencyTree(title_result.second.data.triggerTemplate.getSecond(), sources, title_result.second.depResult.getFirst(), title_result.second.words);
		
		if(source != null)
		{
			title_result.second.data.sourceActor = source;
		}
		if(title_result.second.data.targetActor != null)
		{
			title_result.second.data.targetActor = title_result.second.data.targetActor.split("_")[0];
		}
		if (title_result == null || title_result.second == null || title_result.second.data == null)
			return null;
		/**
		 * 如果当前source为空的话，不作为一个事件
		 */
		if (title_result.second.data.sourceActor == null || title_result.second.data.sourceActor.trim().equals(""))
			return null;

		/**
		 * 去掉target和source中重复的实体
		 */
		this.processActor.Finalprocess(title_result.second.data);

		/**
		 * 处理事件发生的时间
		 */
		this.timeExraction.setTimebyrule(title_result.second.words, title_result.second.tags,
				title_result.second.data, saveTime);
		/**
		 * 事件的发生时间的时间戳表示
		 */
		title_result.second.data.saveTime = this.timeExraction.String2Time(title_result.second.data.eventTime, saveTime);

		/**
		 * 事件句子中所有的命名实体和人名
		 */
		title_result.second.data.actorItem = Ner.ner_actionitem(sentence);
		title_result.second.data.allPerson = findActorbyDB.findallperson(title_result.second.data.actorItem);

		/**
		 * 对事件抽取结果补全赋值
		 */
		title_result.second.data.newsTitle = newsTitle;
		title_result.first = sentence;
		title_result.second.data.setValues(imgAddress, newsURL, newsID, placeEntity, true, sentence);
		methods.RemoveNull(title_result.second);
		
		return new Pair<String, LabelItem>(title_result.first, title_result.second.data);
	}

	/**
	 *
	 * 
	 * @param newsURL
	 * @param imgAddress
	 * @param newsID
	 * @param saveTime
	 * @param newsTitle
	 * @param sentence
	 * @param placeEntity
	 * @param placeEntity2 
	 * @param isSummary
	 * @return Pair<String, LabelItem> ("当前事件句"，"事件抽取结果");
	 * @throws IOException 
	 * @throws SQLException
	 */
	public String[] merge_entitys(List<String> words,String[] sources)
	{
		
		String sent = "";
		List<String> newSource = new ArrayList<>();
		for(String word : words){sent += word;}
		int i = 0;
		while(i < sources.length)
		{
			String sou = sources[i];
			int j = i + 1;
			while(j < sources.length)
			{
				if(sent.indexOf((sou + sources[j]))!=-1)
				{
					sou += sources[j];
					j++;
				}else
				{
					break;
				}
			}
			newSource.add(sou);
			i = j;
		}
	
		return newSource.toArray(new String[newSource.size()]);
	}
	public String extract_by_depenencyTree(String triggerTemplate,String[] sources,List<Integer> depids,List<String> words)
	{
		if(sources==null || sources.length <= 1)return null;
//		triggerTemplate = triggerTemplate.replace("source", "source_");
		System.out.println(triggerTemplate);
		String[] elements = triggerTemplate.split("_");
		String sou_nextword = "";
		for(String element : elements)
		{
			if(element.indexOf("source")!=-1)
			{
				sou_nextword = element.substring(6);
				break;
			}
		}
		if(sou_nextword.length()==0)return null;
		int sou_nextword_id = -1;
//		int[] ids = new int[sources.length];
		Map<Integer, Integer> ids = new HashMap<>();
		List<Integer> id_list = new ArrayList<>();
		int k = 0;
		for(int i =0;i<words.size();i++)
		{
			if(words.get(i).equals(sou_nextword)){sou_nextword_id = i;break;}
		}
		for(int i =0;i<words.size();i++)
		{
			boolean flag = false;
			String entity = "";
			int j = i;
			while(j<words.size())
			{
				entity += words.get(j);
				if(!sources[k].startsWith(entity)){break;}
				if(entity.equals(sources[k]))
				{
					flag = true;j++;break;
				}
				if(sources[k].startsWith(entity))
				{
					j++;
				}
			}
			if(flag)
			{
				for(int m = i;m < j;m++)
				{
					ids.put(m, k);
					id_list.add(m);
				}
				k++;
			}
			if(k == sources.length){break;}
		}
		int min_step_id = id_list.get(id_list.size()-1), min_step = 100;
		for(Integer key : ids.keySet())
		{
			int cur_id = key;
			int step = 0;
			boolean flag = false;
			for(int j = 0; j < 3;j++)
			{
//				System.out.println("cur_id"+cur_id);
				if(cur_id != -1)break;
				if(depids.get(cur_id) == sou_nextword_id + 1)
				{
					flag = true;
					step++;
					break;
				}else
				{
					cur_id = depids.get(cur_id) - 1;
					step++;
				}
			}
			if(flag && step <= min_step)
			{
				min_step = step;
				min_step_id = key;
			}

		}
		String source = sources[ids.get(min_step_id)];
		return source;
		
		
	}
	
	public void test() throws IOException, SQLException
	{
//		EventExtractionWithoutBackGround bsl = new EventExtractionWithoutBackGround();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File("src/result.txt")));
		List<String> sents = new ArrayList<>();
		BufferedReader br = new BufferedReader(new FileReader(new File("src/test_tmp_col.txt")));
		String line = "";
		String wordseq = "";
		List<List<String>> tokens_list = new ArrayList<>();
		List<String> tokens = new ArrayList<>();
		while((line=br.readLine())!=null)
		{
			if(line.trim().length()==0)
			{
				sents.add(wordseq);
				tokens_list.add(tokens);
				wordseq = "";
				tokens = new ArrayList<>();
			}else
			{
				String word = line.split("#")[0];
				wordseq += word;
				tokens.add(word);
			}
		}
		br.close();
		for(String sent : sents)
		{
			String newsURL = null;
			String imgAddress = null;
			String newsID = null;
			String saveTime = null;
			String newsTitle = null;
			String placeEntity = null;
			boolean isSummary = false;
			Pair<String, LabelItem> result = extractbysentence(newsURL, imgAddress, newsID, saveTime, newsTitle, sent, placeEntity, isSummary);
			
			if(result!=null)
			{
			
//				System.out.print(result.getSecond().eventType+"\n");
				bw.write(sent+'\t'+result.getSecond().triggerWord+"\t"+result.getSecond().sourceActor+'\t'+result.getSecond().targetActor+"\n");
				
			}else
			{
				bw.write(sent+'\n');
			}
		}
		bw.close();
		
	}
	public static void main(String[] args) throws SQLException {
		// TODO Auto-generated method stub	
		EventExtractionWithoutBackGround bsl = new EventExtractionWithoutBackGround();
//		try {
//			bsl.test();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		String sent  ="中俄两军第二十轮战略磋商在北京举行";
		String newsURL = null;
		String imgAddress = null;
		String newsID = null;
		String saveTime = null;
		String newsTitle = null;
		String placeEntity = null;
		boolean isSummary = false;
		Pair<String, LabelItem> result = bsl.extractbysentence(newsURL, imgAddress, newsID, saveTime, newsTitle, sent, placeEntity, isSummary);
		System.out.print(sent+'\t'+result.getSecond().triggerWord+"\t"+result.getSecond().sourceActor+'\t'+result.getSecond().targetActor+"\n");
		System.out.println(result.getSecond().eventTime);
//		
	}
}
