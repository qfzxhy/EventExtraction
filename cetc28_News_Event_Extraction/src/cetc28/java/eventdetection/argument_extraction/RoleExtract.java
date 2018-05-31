package cetc28.java.eventdetection.argument_extraction;

import java.awt.print.Printable;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import cetc28.java.config.FileConfig;
import cetc28.java.dbtool.GeonamesUtil;
import cetc28.java.dbtool.OracleUtil;
import cetc28.java.eventdetection.entity_extraction.FindActorandPerson;
import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.sentiment_extraction.PolarityBasic;
import cetc28.java.eventdetection.textrank.TextRankSummary;
import cetc28.java.eventdetection.time_location_extraction.LocationExtraction;
import cetc28.java.eventdetection.time_location_extraction.TimeExtraction;
import cetc28.java.news.label.ActorItem;
import cetc28.java.news.label.ActorProItem;
import cetc28.java.news.label.EventActorItem;
import cetc28.java.news.label.EventItem;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import cetc28.java.nlptools.Stanford_Parser;
import cetc28.java.program.RunDetection;
import edu.hit.ir.ltp4j.NER;
import edu.hit.ir.ltp4j.Parser;
import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.SRL;
import edu.hit.ir.ltp4j.Segmentor;

/**
 * 创建时间：2016.03.17
 * 
 * @author qianf
 * @version 1.0
 * @since JDK 1.7 文件名称：java 系统信息：Win10 类说明： 
 * 功能描述：提供基于规则、依存句法、语义角色等进行source_target字符串的抽取。并对抽取结果进行后处理，抽取其中的实体
 * @author nlp_daij
 *
 */
public class RoleExtract {
	/**
	 * flag==1 只使用实体表 flag == 0 使用实体表+词性标注结果
	 * 
	 * @param args
	 * @throws LoadModelException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IOException
	 *             新加入事件角色 新加入FillActor
	 * 
	 */
	
	public RoleExtract(LtpTool ltpTool, ReSegment preprocess, Stanford_Parser stanford_Parser,
			FindActorandPerson findActorbyDB, Methods methods, GeonamesUtil geonamesUtil) {
		
		this.preprocess = preprocess;
		this.stanford_Parser = stanford_Parser;
		this.findActorbyDB = findActorbyDB;
		this.methods = methods;
		this.geonamesUtil = geonamesUtil;
	}
	
	public RoleExtract() {
		// TODO Auto-generated constructor stub
		methods = new Methods();
		preprocess = new ReSegment();
		findActorbyDB = new FindActorandPerson(new ArgumentExtraction());
		stanford_Parser = new Stanford_Parser();
		
	}

	
	public ReSegment preprocess;
	public Stanford_Parser stanford_Parser;
	public FindActorandPerson findActorbyDB;
	public Methods methods;
	GeonamesUtil geonamesUtil;
	
	/**
	 * 根据触发词，抽取事件元素，并做后处理，抽取事件元素中的实体
	 * @param title_result
	 * @throws SQLException
	 */
	public void extractactor(Pair<String, Data> title_result) throws SQLException {
		// TODO Auto-generated method stub
		// 调用抽取实体的函数
		// System.out.println("------------------------------------------");
		if (title_result == null || title_result.second == null || title_result.second.data == null)
			return;

		EventActorItem eventActorItem = new EventActorItem();

		/**
		 * 抽取source、target，source一定是实体，target不一定，直接返回抽取的事件元素
		 */
		eventActorItem = setactor(title_result);
//		eventActorItem.print();

		/**
		 * 抽取target中的实体
		 */
		extractActorforTarget(title_result, eventActorItem);
//		title_result.second.data.Print();

	}

	
	/**
	 * 返回抽取结果的动态数组 flag == 0 主动词,flag == 1
	 * 
	 * @param titleResult
	 * @param flag
	 * @return EventActorItem:存储事件元素的类
	 * @throws SQLException
	 */
	public EventActorItem setactor(Pair<String, Data> titleResult) throws SQLException {
		EventActorItem eventActorItem = new EventActorItem();

		if (titleResult == null || titleResult.second == null || titleResult.second.data == null)
			return eventActorItem;

		LabelItem extractResult = titleResult.second.data;
		String sentence = titleResult.first;

		/**
		 * 事件触发词优先使用模板
		 */
		String triggerWord = extractResult.triggerTemplate == null ? extractResult.triggerWord
				: extractResult.triggerTemplate.second;
//		System.out.println("triggerWord:"+triggerWord);
		/**
		 * 句子和触发词都不能为空
		 */
		if (sentence == null || sentence.trim().equals("") || triggerWord == null || triggerWord.trim().equals(""))
			return eventActorItem;

		/**
		 * 找到句子中所有的实体，存储
		 */
		ActorItem allActor = Ner.ner_actionitem(sentence);
		
		/**
		 * 句子中没有实体，则不再进行事件抽取
		 */
		if (allActor == null || allActor.actor.trim().length() == 0)
			return eventActorItem;
		String[] actor = { "", "", "" };
		ActorItem sourceActor = new ActorItem();
		/**
		 * 对不同情况触发词进行分别处理
		 */
		/**
		 * 处理当前触发词非模板的情况
		 */
		if (triggerWord.indexOf("source") == -1 && triggerWord.indexOf("target") == -1) {
			/**
			 * 触发词为一类中特殊的几种，则直接使用前后词
			 */
			
			if (this.preprocess.Settag(triggerWord, this.preprocess.getT1paticular())) {
				
				actor[0] = sentence.substring(0, sentence.indexOf(triggerWord)).trim();
				actor[1] = (sentence.substring(sentence.indexOf(triggerWord) + triggerWord.length(), sentence.length())
						.trim());
				actor[2] = triggerWord;
//				System.out.println("触发词为一类中特殊的几种，则直接使用前后词");
//				 PrintArray(actor);
			} else {
				/**
				 * 否则使用依存句法分析+语义角色分析+规则
				 */
			
				try {
					actor = setactorbyparsebyparse(titleResult, allActor);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
//				System.out.println("使用依存句法分析+语义角色分析+规则");
//				PrintArray(actor);
			}
			/**
			 * 抽取source中的实体
			 */
			sourceActor = findActorbyDB.getAllActorRole(titleResult, actor[0]);
		} else {
			
			/**
			 * 触发词为模板，使用基于规则的方法
			 */
			actor = setactorbyrule(sentence, triggerWord);
//			System.out.println("触发词为模板，使用基于规则的方法");
//			for(String  a : actor){
//				System.out.println(a);
//			}
//			 PrintArray(actor);
			/**
			 * 对抽取的source、target做后处理
			 */
			 
			sourceActor = finalProcess(titleResult, actor, extractResult.triggerTemplate.second);
//			System.out.println("使用基于规则的方法抽取");
//			sourceActor.Print();
			/**
			 * 重新赋值触发词
			 */
			actor[2] = extractResult.triggerTemplate.first;
		}
		if (actor != null && sourceActor!=null) {
			
			eventActorItem.setSourceActor(sourceActor.actor);// source中的实体
//			eventActorItem.setSourceActorPro(sourceActor.actorPro);// source的实体属性
			eventActorItem.setTargetActor(actor[1]);
			String ActorPro = methods.findPro(actor[1]);
//			eventActorItem.setTargetActorPro(ActorPro);
			eventActorItem.setTriggerWord(actor[2]);
			eventActorItem.setAllActor(allActor);
		}
		
		
		/**
		 * 过滤点source中实体多于5个的情况
		 */
		if (eventActorItem.getSourceActor() == null || eventActorItem.getSourceActor().trim().equals("")
				|| eventActorItem.getSourceActor().split("_").length > 5)
			return null;
		return eventActorItem;
	}

	/**
	 * 触发词为模板的情况
	 * 
	 * @param newsInput
	 * @param triggerword
	 * @param eventType
	 * @param flag
	 * @return 返回source/target
	 */
	public String[] setactorbyrule(String newsInput, String triggerword) {
		// TODO Auto-generated method stub
		ArrayList<String> sourceActor = new ArrayList<String>();
		ArrayList<String> targetActor = new ArrayList<String>();
		String finalactor[] = { "", "", "" };
		ArrayList<String> S_Tlist = new ArrayList<String>();
		/**
		 * 找到模板中所有的source、target位置
		 */
		for (int i = 0; i + 5 < triggerword.length(); i++) {
			if (triggerword.substring(i, i + 6).equals("source") || triggerword.substring(i, i + 6).equals("target")) {
				S_Tlist.add(triggerword.substring(i, i + 6));
				i = i + 5;
			} else if (triggerword.substring(i, i + 1).equals("_")) {
				S_Tlist.add(triggerword.substring(i, i + 1));
			}
		}
		/**
		 * 使用模式匹配的方法抽取各个位置的source、target
		 */
		triggerword = triggerword.replaceAll("source", "(.*?)");
		triggerword = triggerword.replaceAll("target", "(.*?)");

		if (triggerword.startsWith("(.*?)"))
			triggerword = "(.*)".concat(triggerword.substring(5, triggerword.length()));
		if (triggerword.endsWith("(.*?)"))
			triggerword = triggerword.substring(0, triggerword.length() - 5).concat("(.*)");
		triggerword = triggerword.replaceAll("_", "(.*)");

//		System.out.println(triggerword);
		Pattern p = Pattern.compile(triggerword);
		Matcher m = p.matcher(newsInput);
		while (m.find()) {
			for (int num = 1; num < S_Tlist.size() + 1; num++) {
				if (S_Tlist.get(num - 1).equals("source")) {
					String sentence = m.group(num).trim();
					sourceActor.add(sentence);
				} else if (S_Tlist.get(num - 1).equals("target")) {
					String sentence = m.group(num).trim();
					targetActor.add(sentence);
				}
			}
		}
		for (int i = 0; i < sourceActor.size(); i++) {
			if (!sourceActor.get(i).trim().equals(""))
				finalactor[0] = finalactor[0].concat(sourceActor.get(i).trim() + "_");
		}
		for (int i = 0; i < targetActor.size(); i++) {
			if (!targetActor.get(i).trim().equals(""))
				finalactor[1] = finalactor[1].concat(targetActor.get(i).trim() + "_");
		}

		/*
		 * 去掉多余的下划线
		 */
		methods.removeLine(finalactor);
		finalactor[2] = triggerword;
		return finalactor;
	}

	/**
	 * 对抽取的source、target做后处理
	 * @param title_result
	 * @param actor
	 * @param triggerWord
	 * @return
	 * @throws SQLException
	 */
	private ActorItem finalProcess(Pair<String, Data> title_result, String[] actor, String triggerWord)
			throws SQLException {
		// TODO Auto-generated method stub
		/**
		 * 标识source、target谁在前面，若source在target之前，则tag=true
		 */
		boolean tag = false;
		ActorItem sourceActor = null;

		if (triggerWord.indexOf("source") < triggerWord.indexOf("target") || triggerWord.indexOf("target") == -1)
			tag = true;

		/**
		 * 处理source
		 */
		if (tag == true) { // source在前，从后往前找
			int lastChinesePunctuationPostion = methods.lastChinesePunctuationPostionExcept(actor[0]);// 当前source中有标点符号，则取标点符号之后的source
			if (lastChinesePunctuationPostion > -1) {
				String temp0 = actor[0].substring(lastChinesePunctuationPostion + 1, actor[0].length());
				sourceActor = findActorbyDB.getAllActorRole(title_result, temp0);

				/**
				 * 若标点符号之前没有实体，则选取标点符号之前的实体作为source
				 */
				if(sourceActor == null || sourceActor.actor == null || sourceActor.actor.trim().equals("")){
					
					String temp1 = actor[0].substring(0,lastChinesePunctuationPostion);//标点符号之前的source
					sourceActor = findActorbyDB.getAllActorRole(title_result, temp1);
					/**
					 * 如果之前有多个实体，选择第一个作为source
					 */
					if(!(sourceActor == null || sourceActor.actor == null || sourceActor.actor.trim().equals(""))&& 
							 sourceActor.actor.trim().indexOf("_") != -1 ){
						sourceActor.actor = sourceActor.actor.substring(0,sourceActor.actor.indexOf("_"));
						sourceActor.actorPro = sourceActor.actorPro.substring(0,sourceActor.actorPro.indexOf("_"));
					}
				}
				
			}
			/**
			 * 若source之中没有标点符号，则选取整个source之间的实体
			 */
			if (sourceActor == null || sourceActor.actor == null || sourceActor.actor.trim().equals("")){
				sourceActor = findActorbyDB.getAllActorRole(title_result, actor[0]);
			}

		} else {// source在后面，从前往后找
			int firstChinesePunctuationPostion = methods.firstChinesePunctuationPostionExcept(actor[0]);// 当前source中有标点符号，则取标点符号之前的source
			if (firstChinesePunctuationPostion > -1) {
				String temp0 = actor[0].substring(0, firstChinesePunctuationPostion);//标点符号之前的source
				sourceActor = findActorbyDB.getAllActorRole(title_result, temp0);
				/**
				 * 若标点符号之前没有实体，则选取标点符号之后的实体作为source
				 */
				if(sourceActor == null || sourceActor.actor == null || sourceActor.actor.trim().equals("")){
					String temp1 = actor[0].substring( firstChinesePunctuationPostion+1,actor[0].length());//标点符号之后的source
					sourceActor = findActorbyDB.getAllActorRole(title_result, temp1);
					/**
					 *如果之后有多个实体，选择第一个作为source
					 */
					if(!(sourceActor == null || sourceActor.actor == null || sourceActor.actor.trim().equals("") )&& 
							sourceActor.actor.trim().indexOf("_") != -1){						
						sourceActor.actor = sourceActor.actor.substring(0,sourceActor.actor.indexOf("_"));
						sourceActor.actorPro = sourceActor.actorPro.substring(0,sourceActor.actorPro.indexOf("_"));
					}	
				}
			}
			/**
			 * 若source之中没有标点符号或者，则选取整个source之间的实体
			 */
			if (sourceActor == null || sourceActor.actor == null || sourceActor.actor.trim().equals(""))
				sourceActor = findActorbyDB.getAllActorRole(title_result, actor[0]);
		}
		
		/**
		 * 处理target
		 */
		if (tag == false) {// target在前，从后往前找
			int lastChinesePunctuationPostion = methods.lastChinesePunctuationPostionExcept(actor[1]);// 当前target中有标点符号，则取标点符号之后的target
			if (lastChinesePunctuationPostion > -1)
				actor[1] = actor[1].substring(lastChinesePunctuationPostion + 1, actor[1].length());//标点符号之后的target
		} else {// target在后面，从前往后找
			int firstChinesePunctuationPostion = methods.firstChinesePunctuationPostionExcept(actor[1]);// 当前target中有标点符号，则取标点符号前的target
			if (firstChinesePunctuationPostion > -1)
				actor[1] = actor[1].substring(0, firstChinesePunctuationPostion);
		}
		return sourceActor;
	}
	/**
	 * 根据依存句法分析+语义角色分析+规则来抽取source\target
	 * 
	 * @param title_result
	 * @param allActor
	 * @param flag
	 * @return [source\target\triggerWord]
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws LoadModelException
	 * @throws IOException
	 */
	public String[] setactorbyparsebyparse(Pair<String, Data> title_result, ActorItem allActor)
			throws SQLException, ClassNotFoundException, IOException {
		String[] actor = { "", "", "" };
//		System.out.println("进入setactorbyparsebyparse：");
//		title_result.second.Print();
		
		if (title_result == null || title_result.second == null || title_result.second.data == null)
			return actor;

		LabelItem extractResult = title_result.second.data;
		String sentence = title_result.first;
		String triggerWord = extractResult.triggerWord;

		// 句子、trigger均不可以为空
		if (sentence == null || sentence.trim().equals("") || triggerWord == null || triggerWord.trim().equals("")
				|| title_result.second.triggerPos == -1)
			return actor;

		ArrayList wordList = new ArrayList();
		ArrayList tagList = new ArrayList();

		for (int i = 0; i < title_result.second.words.size(); i++) {
			wordList.add(title_result.second.words.get(i));
			tagList.add(title_result.second.tags.get(i));

		}

		/**
		 * 使用依存句法+语义角色进行抽取+被
		 */
		actor = setRolebyParse(sentence, triggerWord, title_result.second.triggerPos, wordList, tagList, allActor,
				title_result);
//		System.out.println("使用依存句法+语义角色进行抽取+被");
//		PrintArray(actor);
		if (actor == null || actor[0].equals("")) {
			/**
			 * 使用和与或的规则进行事件抽取
			 */
			actor = extractRole(sentence, triggerWord, wordList, tagList, allActor, title_result);
//			System.out.println("使用和与或的规则进行事件抽取");
//			PrintArray(actor);
		}
		return actor;
	}

	/**
	 * 使用工具对句子进行依存句法分析，语义角色分析
	 * 
	 * @param newsInput
	 * @param triggerWord
	 * @param triggerPos
	 * @param words0
	 * @param postags0
	 * @param allActor
	 * @param tag
	 * @param title_result
	 * @return [source\target\triggerWord]
	 * @throws SQLException
	 * @throws LoadModelException
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public String[] setRolebyParse(String newsInput, String triggerWord, int triggerPos, List<String> words0,
			List<String> postags0, ActorItem allActor, Pair<String, Data> title_result)
					throws SQLException, IOException, ClassNotFoundException {
		
		String actor[] = { "", "", "" };

		List<String> ners0 = new ArrayList();
		List<Integer> heads0 = new ArrayList();
		List<String> deprels0 = new ArrayList();
		List<edu.hit.ir.ltp4j.Pair<Integer, List<edu.hit.ir.ltp4j.Pair<String, edu.hit.ir.ltp4j.Pair<Integer, Integer>>>>> srls0 = new ArrayList();

		ArrayList<String> actor_Pro = new ArrayList<String>();

		/**
		 * 对当前句子做依存句法分析、语义角色标注
		 */
		
		srls0 = LtpTool.PreprocessParser(words0, postags0, ners0, heads0, deprels0);
		
		/**
		 * 使用被字句
		 */
		String actorBei[] = Rule(newsInput, triggerWord, words0, postags0, this.preprocess.getPbei(), allActor,
				title_result);
//		System.out.println("使用被字句");
//		PrintArray(actorBei);
		
		if (actorBei != null && !(actorBei[1].trim().equals(""))) {
			// XX被XXtriggerWord
			actor[0] = actorBei[1];
			actor[1] = actorBei[0];
			actor[2] = actorBei[2];
			return actor;
		}
		/**
		 * 对当前句子做依存句法分析、语义角色标注
		 */
		actor = FindActorbyParse(newsInput, triggerWord, triggerPos, words0, postags0, ners0, heads0, deprels0, srls0,
				allActor);
		
		return actor;
	}

	/**
	 * 根据依存句法分析结果进行sourc/target抽取
	 * 
	 * @param sentence
	 * @param triggerWord
	 * @param triggerPos
	 * @param newwords
	 * @param newpostags
	 * @param ners
	 * @param heads
	 * @param deprels
	 * @param srls
	 * @param allActor
	 * @return [source、target、triggerWord]
	 * @throws SQLException
	 */
	public String[] FindActorbyParse(String sentence, String triggerWord, int triggerPos, List<String> newwords,
			List<String> newpostags, List<String> ners, List<Integer> heads, List<String> deprels,
			List<edu.hit.ir.ltp4j.Pair<Integer, List<edu.hit.ir.ltp4j.Pair<String, edu.hit.ir.ltp4j.Pair<Integer, Integer>>>>> srls,
			ActorItem allActor) throws SQLException {
		// System.out.println("FindActorbyParse:"+newwords);
		String triggerPrime = triggerWord;
		int triggerPrimePos = triggerPos;
		String[] actor = new String[3];
		actor[0] = "";
		actor[1] = "";
		actor[2] = triggerWord;

		/**
		 * 使用依存句法分析,直接使用当前词做句法分析时，补充触发词
		 */
		actor = methods.Combine(actor, ActorBydeprel(triggerPos, heads, deprels, newwords, newpostags));
		// System.out.println("使用依存句法分析结果：");
		// PrintArray(actor);

		/**
		 * 直接使用当前词做句法分析时，利用直接宾语补充触发词
		 */
		if (!actor[0].equals("") && !actor[1].equals("")) {
//			actor[2] = findCompleteAction(triggerPos, triggerPrime, heads, deprels, newwords, newpostags);
//			actor[2] = actor[2] == null || actor[2].trim().equals("") ? triggerPrime : actor[2];
			actor[2] = triggerPrime;
			return actor;
		}

		/**
		 * 使用语义角色分析的结果
		 */
		actor = methods.Combine(actor, ActorBySrl(triggerPos, srls, newwords, newpostags));
		// System.out.println("使用语义角色结果：");
		// PrintArray(actor);

		/**
		 * 若当前词无直接的source和target，或者已经找到source、target， 则返回退出
		 */
		if (!(actor[0].equals("") || actor[1].equals("")) || (actor[0].equals("") && actor[1].equals("")))
			return actor;

		/**
		 * 一般来说，并列的动词共享主语/宾语;若当前词至少有一个source、target时，找COO
		 */
		String actorCoo[] = { "", "" };
		/*
		 * 原来的触发词直接主语为空时
		 */
		if (actor[0].equals("") && !actor[1].equals("")) {
			/**
			 * 从头开始找触发词的并列动词
			 */
			String newtriggercoo = findCOO(0, triggerPos, newwords, newpostags, heads, deprels,
					newwords.get(triggerPos));
			if (!newtriggercoo.equals("") && !newtriggercoo.equals(actor[2])) {
				actorCoo = ActorBydeprel(newwords.indexOf(newtriggercoo), heads, deprels, newwords, newpostags);
				actor[0] = actor[0].equals("") ? actorCoo[0] : actor[0];
				// System.out.println("newtriggercoo：" + newtriggercoo);
				// System.out.println("使用并列词:");
				// PrintArray(actor);
			}
		}
		/*
		 * 原来的触发词直接宾语为空时
		 */
		if (actor[1].equals("") && !actor[0].equals("")) {
			/**
			 * 从触发词向后开始找触发词的并列动词
			 */
			String newtriggercoo = findCOO(triggerPos + 1, newwords.size(), newwords, newpostags, heads, deprels,
					newwords.get(triggerPos));
			if (!newtriggercoo.equals("") && !newtriggercoo.equals(actor[2])) {
				actorCoo = ActorBydeprel(newwords.indexOf(newtriggercoo), heads, deprels, newwords, newpostags);
				actor[1] = actor[1].equals("") ? actorCoo[1] : actor[1];
				// System.out.println("newtriggercoo：" + newtriggercoo);
				// System.out.println("使用并列词:");
				// PrintArray(actor);
			}
		}

		/**
		 * 找当前触发词作为其他词的vob的情况
		 */
		String actorVob[] = { "", "" };
		Pair<String, Integer> newtriggerVobPos = findOther(
				new Pair<String, Integer>(newwords.get(triggerPrimePos), triggerPrimePos), heads, deprels, newwords,
				newpostags);
		if (newtriggerVobPos != null && !newtriggerVobPos.first.equals("") && !newtriggerVobPos.first.equals(actor[2])
				&& newtriggerVobPos.second != -1) {
			actorVob = ActorBydeprel(newtriggerVobPos.second, heads, deprels, newwords, newpostags);
			if (!actorVob[0].contains(triggerWord) && !actorVob[1].contains(triggerWord)) {
				actor[0] = actor[0].equals("") ? actorVob[0] : actor[0];
				actor[1] = actor[1].equals("") ? actorVob[1] : actor[1];
			}
		}
		/**
		 * 找当前触发词作为其他词的A1的情况
		 */
		if ((actor[0].equals("") && !actor[1].equals("")) || (actor[0].equals("") && !actor[1].equals(""))) {
			String actorVobSrl[] = ActorBySrlDe(triggerPos, heads, deprels, srls, newwords, newpostags);
			if (!actorVobSrl[0].contains(triggerWord) && !actorVobSrl[1].contains(triggerWord)) {
				actor[0] = actor[0].equals("") ? actorVobSrl[0] : actor[0];
				actor[1] = actor[1].equals("") ? actorVobSrl[1] : actor[1];
			}
		}
		actor[2] = triggerPrime;
		// PrintArray(actor);
		return actor;
	}

	/**
	 * 触发词本身为另外一个动词的A1，且触发词作为A1，其前面直接有实体的情况，将触发词前面的实体作为source
	 * 例如“中国正在加强钓鱼岛建设”
	 * @param triggerPos
	 * @param heads
	 * @param deprels
	 * @param srls
	 * @param newwords
	 * @param newpostags
	 * @return
	 */
	public String[] ActorBySrlDe(int triggerPos, List<Integer> heads, List<String> deprels,
			List<edu.hit.ir.ltp4j.Pair<Integer, List<edu.hit.ir.ltp4j.Pair<String, edu.hit.ir.ltp4j.Pair<Integer, Integer>>>>> srls,
			List<String> newwords, List<String> newpostags) {
		String actor[] = { "", "", "" };
		String actortemp[] = { "", "", "" };
		String triggerWord = null;
		triggerPos--;
		if (triggerPos != -1 && triggerPos < newwords.size()) {
			triggerWord = newwords.get(triggerPos);
			for (int i = 0; i < srls.size(); i++) {
				for (int j = 0; j < srls.get(i).second.size(); ++j) {
					// System.out.println(srls.get(i).second.get(j));
					String Aflag = srls.get(i).second.get(j).first;
					String act = methods.construct(newwords, srls.get(i).second.get(j).second.first,
							srls.get(i).second.get(j).second.second);
					// System.out.println("act:"+act);
					if (Aflag.equals("A1") || Aflag.equals("A2") || Aflag.equals("A3") || Aflag.equals("A4")
							|| Aflag.equals("A5") || Aflag.equals("PSE") || Aflag.equals("BNE"))
						actortemp[1] = actortemp[1].concat("_" + act);
					// Aflag.equals("A2")||Aflag.equals("A3")||Aflag.equals("A4")||Aflag.equals("A5")||
					else if (Aflag.equals("A0") || Aflag.equals("PSR"))
						actortemp[0] = actortemp[0].concat("_" + act);
				}
				break;
			}
			if (triggerWord != null && !triggerWord.trim().equals("") && actortemp[1].contains(triggerWord.trim())) {
				actor[0] = actortemp[1].replaceAll(triggerWord, "");
				actor[1] = actortemp[0];
				actor[2] = triggerWord;
			}
		}
		methods.removeLine(actor);
		return actor;
	}

//	public void PrintArray(String[] actor) {
//		// TODO Auto-generated method stub
//		System.out.println("actor[0]:" + actor[0]);
//		System.out.println("actor[1]:" + actor[1]);
//		System.out.println("actor[2]:" + actor[2]);
//
//	}

	
	/**
	 * 
	 * @param newsInput
	 * @param title_result
	 * @param triggerword
	 * @return 按照规则抽取的结果
	 * @throws SQLException
	 * @throws LoadModelException
	 * @throws IOException
	 */
	public String[] extractRole(String newsInput, String triggerWord, List<String> words, List<String> postags,
			ActorItem allActor, Pair<String, Data> title_result) throws SQLException, IOException {

		// System.out.println("利用规则");
		if (newsInput == null || newsInput.trim().equals("") || triggerWord == null || triggerWord.trim().equals("")
				|| allActor == null || allActor.actor.trim().length() == 0)
			return null;

		String actor[] = { "", "", triggerWord };

		String actorHz[] = Rule(newsInput, triggerWord, words, postags, this.preprocess.getPand(), allActor,
				title_result);
		String actorDui[] = Rule(newsInput, triggerWord, words, postags, this.preprocess.getPdui(), allActor,
				title_result);
		
		if (actorHz != null && !(actorHz[0].equals("") && actorHz[1].equals(""))) {
			actor[0] = actorHz[0];
			actor[1] = actorHz[1];
			actor[2] = actorHz[2];
		} // XX与XXtriggerWord
		else if (actorDui != null && !(actorDui[0].equals("") && actorDui[1].equals(""))) {
			// XX将/把/向/对 XXtriggerWord
			actor[0] = actorDui[0];
			actor[1] = actorDui[1];
			actor[2] = actorDui[2];
		}
		
		// System.out.println("trigger:"+actor[2]);
		if (actor[2] != null && actor[2].trim().equals("")) {
			actor[2] = methods.removeChinese(actor[2]);
		}
		return actor;
	}

	/**
	 * 和、被、对的规则
	 */
	public String[] Rule(String newsInput, String triggerWord, List<String> words, List<String> postags,
			ArrayList<String> pdui, ActorItem allActor, Pair<String, Data> title_result) throws SQLException {
		// TODO Auto-generated method stub
		if (newsInput == null || newsInput.trim().equals("") || triggerWord == null || triggerWord.trim().equals("")
				|| allActor == null || allActor.actor.trim().length() == 0)
			return null;
//		System.out.println(pdui);
		String actor[] = { "", "", triggerWord };
		int beipos = -1;
		int hzpos = -1;
		int tempbeipos = -1;
		String bei = "";
		for (String s : pdui) {
			if ((tempbeipos = methods.IndexARR(words, s)) > -1) {
				bei = s;
				beipos = tempbeipos;
				// System.out.println("newsInput.substring():"+newsInput.substring(beipos,beipos+1));
				break;
			}
		}
		// int triggerPos = methods.IndexARR(words, triggerWord);
		hzpos = methods.IndexARR(words, triggerWord);

		String StringfromBtoBei = methods.construct(words, 0, beipos - 1);// 开始到和之间的句子
		int lastPositionfromBtoBei = methods.lastChinesePunctuationPostionExcept(StringfromBtoBei);
		StringfromBtoBei = StringfromBtoBei.substring(lastPositionfromBtoBei == -1 ? 0 : lastPositionfromBtoBei + 1,
				StringfromBtoBei.length());
		ActorItem AllActorfromBtoBei = findActorbyDB.getAllActorRole(title_result, StringfromBtoBei);// .getAllActor(allActor,
																										// );-1从前往后，1从后往前

		String StringfromBeitoTrg = methods.construct(words, beipos + 1, hzpos - 1);// 从和到触发词之间的句子
		int lastPositionfromBeitoTrg = methods.lastChinesePunctuationPostionExcept(StringfromBeitoTrg);
		StringfromBeitoTrg = StringfromBeitoTrg.substring(
				lastPositionfromBeitoTrg == -1 ? 0 : lastPositionfromBeitoTrg + 1, StringfromBeitoTrg.length());
		ActorItem AllActorfromBeitoTrg = findActorbyDB.getAllActorRole(title_result, StringfromBeitoTrg);// .getAllActor(allActor,
																											// StringfromBeitoTrg);

		String StringActorfromTrgtoEnd = methods.construct(words, hzpos + 1, words.size() - 1);// 触发词和句子最后一个词之间的句子
		int lastPositionfromTrgtoEnd = methods.firstChinesePunctuationPostionExcept(StringActorfromTrgtoEnd);
		StringActorfromTrgtoEnd = StringActorfromTrgtoEnd.substring(0,
				lastPositionfromTrgtoEnd == -1 ? StringActorfromTrgtoEnd.length() : lastPositionfromTrgtoEnd);
		ActorItem AllActorfromTrgtoEnd = findActorbyDB.getAllActorRole(title_result, StringActorfromTrgtoEnd);// .getAllActor(allActor,
																												// StringActorfromTrgtoEnd);

		String StringActorfromBtoTrg = methods.construct(words, 0, hzpos - 1);// 从开始到触发词之间的句子
		int lastPositionfromBtoTrg = methods.lastChinesePunctuationPostionExcept(StringActorfromBtoTrg);
		StringActorfromBtoTrg = StringActorfromBtoTrg.substring(
				lastPositionfromBtoTrg == -1 ? 0 : lastPositionfromBtoTrg + 1, StringActorfromBtoTrg.length());

		ActorItem AllActorfromBtoTrg = findActorbyDB.getAllActorRole(title_result, StringActorfromBtoTrg);// .getAllActor(allActor,

		if (beipos > -1 && hzpos > beipos
				&& (AllActorfromTrgtoEnd == null || AllActorfromTrgtoEnd.actor.trim().length() == 0)) {
			/**
			 * 处理当前的触发词是最后一个词且之前有和、与、被，且触发词和最后一个词之间没有实体的情况
			 */
			if (AllActorfromBtoBei != null && AllActorfromBtoBei.actor.trim().length() > 0)
				actor[0] = AllActorfromBtoBei.actor.trim();
			if (AllActorfromBeitoTrg != null && AllActorfromBeitoTrg.actor.trim().length() > 0)
				actor[1] = AllActorfromBeitoTrg.actor.trim();
			actor[2] = triggerWord.concat(StringActorfromTrgtoEnd);

		} else if (beipos > -1 && hzpos > beipos && (hzpos < words.size() - 1)
				&& (AllActorfromTrgtoEnd != null && AllActorfromTrgtoEnd.actor.trim().length() > 0)) {
			/**
			 * 处理当前的触发词不是最后一个词且之前有 与、和、被,且与最后一个词之间有有实体的情况
			 */
			if (AllActorfromBtoTrg != null && AllActorfromBtoTrg.actor.trim().length() > 0)
				actor[0] = AllActorfromBtoTrg.actor.trim();
			actor[1] = AllActorfromTrgtoEnd.actor.trim();
			actor[2] = triggerWord;
		}
//		 System.out.println("Rule:"+actor[0]+";"+actor[1]);
		return actor;
	}

	/**
	 * 找到当前触发词的COO的动词
	 * @param newwords
	 * @param newpostags
	 * @param heads
	 * @param deprels
	 * @param srls
	 * @param triggerWord
	 * @return
	 */
	public String findCOO(int start, int end, List<String> newwords, List<String> newpostags, List<Integer> heads,
			List<String> deprels, String triggerWord) {
		// TODO Auto-generated method stub
		String newword = "";
		String actor[] = { "", "" };
		int triggerPos = newwords.indexOf(triggerWord);
		if (triggerPos != -1 && deprels.get(triggerPos).equals("COO") && heads.get(triggerPos) - 1 > -1) {
			newword = newwords.get(heads.get(triggerPos) - 1);
			return newword;
		}
		for (int i = start; i < end; i++) {
			String relation = deprels.get(i);
			int index = heads.get(i);
			if ((index == triggerPos + 1) && relation.equals("COO")) {
				newword = newwords.get(i);
				break;
			}
		}
		return newword;
	}

	/**
	 * 处理当前触发词为其他词的VOB的情况 返回触发词+位置
	 * 例如：“中国正在加强钓鱼岛建设”
	 */
	public Pair<String, Integer> findOther(Pair<String, Integer> triggerWord_triggerPos, List<Integer> heads,
			List<String> deprels, List<String> newwords, List<String> newpostags) {
		// TODO Auto-generated method stub
		if (triggerWord_triggerPos == null || triggerWord_triggerPos.second == -1)return triggerWord_triggerPos;

		String actor[] = { "", "" };
		// System.out.println("triggerPos:"+triggerPos);
		int newwordpos = Integer.valueOf(heads.get(triggerWord_triggerPos.second)) - 1;
		if (newwordpos == -1)return triggerWord_triggerPos;
		if (newpostags.get(newwordpos).equals("v") && deprels.get(triggerWord_triggerPos.second).trim().equals("VOB")) {
			triggerWord_triggerPos.first = newwords.get(newwordpos);
			triggerWord_triggerPos.second = newwordpos;
			triggerWord_triggerPos = findOther(triggerWord_triggerPos, heads, deprels, newwords, newpostags);// newwords.get(newwordpos);
		}
		return triggerWord_triggerPos;
	}

	/*
	 * 根据触发词、heads、deprels返回相关的sub/target
	 */
	/**
	 * 找直接主语和直接宾语 直接主语使用短语句法分析，直接宾语使用依存句法分析 用短语句法分析补充完整词
	 * @param triggerpos
	 * @param heads
	 * @param deprels
	 * @param newwords
	 * @param newpostags
	 * @return
	 * @throws SQLException
	 */
	public String[] ActorBydeprel(int triggerpos, List<Integer> heads, List<String> deprels, List<String> newwords,
			List<String> newpostags) throws SQLException {
		// TODO Auto-generated method stub
		String actor[] = { "", "", "" };
		String temp0 = "";
		String temp1 = "";
		String words[] = new String[newwords.size()];
		for (int count = 0; count < words.length; count++) {
			words[count] = newwords.get(count);
		}
		if (triggerpos != -1) {// pos是在分词链表中的位置+1
			/**
			 * 利用依存句法找直接主语
			 */
			for (int i = 0; i < heads.size() && i < newwords.size(); i++) {
				String word = newwords.get(i);
				int position = Integer.valueOf(heads.get(i)) - 1;
				if (position == triggerpos && deprels.get(i).equals("SBV") && !temp0.contains(word)
						&& !temp1.contains(word)) {

					String newword = stanford_Parser.NPcomplete(words, i);
					if (!(newword.trim().equals(word.trim()) && newpostags.get(i).trim().equals("v")))
						temp0 = temp0.concat(newword + "_");
					else if ((newword.trim().equals(word.trim()) && newpostags.get(i).trim().equals("v")))
						temp1 = temp1.concat(FillActor(0, triggerpos, i, heads, deprels, newwords) + "_");
				}
				/**
				 * 利用依存句法找直接宾语
				 */
				else if (position == triggerpos
						&& (deprels.get(i).equals("VOB") || deprels.get(i).equals("POB") || deprels.get(i).equals("IOB")
								|| deprels.get(i).equals("FOB") || deprels.get(i).equals("DBL"))
						&& !temp1.contains(word) && !temp0.contains(word)) {
					String newword = stanford_Parser.NPcomplete(words, i);

					if (!(newword.trim().equals(word.trim()) && (newpostags.get(i).trim().equals("v"))
							|| newpostags.get(i).trim().equals("a")))
						temp1 = temp1.concat(newword + "_");

					else if ((newword.trim().equals(word.trim()) && (newpostags.get(i).trim().equals("v"))
							|| newpostags.get(i).trim().equals("a")))
						temp1 = temp1.concat(FillActor(triggerpos, newwords.size(), i, heads, deprels, newwords) + "_");

				}
			}

			/**
			 * 在触发词前面找间接主语
			 */
			if (temp0.equals("")) {
				for (int i = 0; i < triggerpos && i < newwords.size(); i++) {
					String word = newwords.get(i);
					if (deprels.get(i).equals("SBV") && isPath(i, triggerpos, heads, deprels) && !temp0.contains(word)
							&& !temp1.contains(word)) {
						String newword = stanford_Parser.NPcomplete(words, i);
						// 如果使用NP扩充实体使得实体包括了触发词，则使用依存
						if (!(newword.trim().equals(word.trim()) && newpostags.get(i).trim().equals("v")))
							temp0 = temp0.concat(newword + "_");
						else if ((newword.trim().equals(word.trim()) && newpostags.get(i).trim().equals("v")))
							temp1 = temp1.concat(FillActor(0, triggerpos, i, heads, deprels, newwords) + "_");
					}
				}
			}

			/**
			 * 在触发词前面找间接宾语
			 */
			if (temp1.equals("")) {
				for (int i = triggerpos + 1; i < heads.size() && i < newwords.size(); i++) {
					String word = newwords.get(i);
					if ((deprels.get(i).equals("VOB") || deprels.get(i).equals("POB") || deprels.get(i).equals("IOB")
							|| deprels.get(i).equals("FOB") || deprels.get(i).equals("DBL"))
							&& isPath(triggerpos, i, heads, deprels) && !temp0.contains(word)
							&& !temp1.contains(word)) {
						String newword = stanford_Parser.NPcomplete(words, i);
						if (!(newword.trim().equals(word.trim()) && newpostags.get(i).trim().equals("v")))
							temp1 = temp1.concat(newword + "_");
						else if ((newword.trim().equals(word.trim()) && newpostags.get(i).trim().equals("v")))
							temp1 = temp1
									.concat(FillActor(triggerpos, newwords.size(), i, heads, deprels, newwords) + "_");
					}
				}
			}
		}

		actor[0] = temp0;
		actor[1] = temp1;
		methods.removeLine(actor);

		/*
		 * 如果target只是动词，不要了
		 */
		if (actor[1] != null && !actor[1].trim().equals("")) {
			int pos = newwords.indexOf(actor[1]);
			if (pos != -1 && newpostags.get(pos).equals("v"))
				actor[1] = "";
		}

		return actor;
	}

	/**
	 * 根据触发词、heads、deprels返回触发词的直接宾语，补全触发词
	 * @param triggerpos
	 * @param triggerWord
	 * @param heads
	 * @param deprels
	 * @param newwords
	 * @param newpostags
	 * @return
	 * @throws SQLException
	 */
	public String findCompleteAction(int triggerpos, String triggerWord, List<Integer> heads, List<String> deprels,
			List<String> newwords, List<String> newpostags) throws SQLException {
		if (triggerpos != -1 && triggerWord != null && !triggerWord.trim().equals("")) {// pos是在分词链表中的位置+1
			// 找动词性直接宾语
			for (int i = 0; i < heads.size() && i < newwords.size(); i++) {
				String word = newwords.get(i);
				int position = Integer.valueOf(heads.get(i)) - 1;
				if (position == triggerpos && deprels.get(i).equals("VOB") && newpostags.get(i).equals("v")) {
					triggerWord = triggerWord.concat("_" + word);
				}
			}
		}
		return methods.removeChinese(triggerWord);
	}

	/**
	 * 判断两个节点之间是否有路径
	 * @param start
	 * @param end
	 * @param heads
	 * @param deprels
	 * @return
	 */
	public boolean isPath(int start, int end, List<Integer> heads, List<String> deprels) {
		// TODO Auto-generated method stub
		boolean flag = false;
		int startpoint = Integer.valueOf(heads.get(start)) - 1;
		if (startpoint == end) {
			flag = true;
			return flag;
		} else if (startpoint != -1) {
			try {
				flag = isPath(startpoint, end, heads, deprels);
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
		}
		return flag;
	}

	/**
	 * 填充实体，找到所有VOB和SBV型的修饰词
	 * @param temppos:当前词所在的位置，当前位置从0算起
	 */
	public String FillActor(int startpos, int endpos, int temppos, List<Integer> heads, List<String> deprels,
			List<String> words) {
		// TODO Auto-generated method stub
		String actor = "";
		temppos++;// ，heads中的值从-1开始算起
		for (int i = startpos + 1; i < endpos; i++) {
			int point = Integer.valueOf(heads.get(i));
			// System.out.println("words.get("+i+"):"+words.get(i));
			if (point == temppos && deprels.get(i).trim().equals("VOB") || deprels.get(i).trim().equals("SBV")
					|| i + 1 == temppos) {
				actor = actor.concat(words.get(i) + "_");
			}
		}
		if (actor == null || actor.equals(""))
			actor = words.get(temppos - 1);
		actor = actor.endsWith("_") ? actor.substring(0, actor.length() - 1) : actor;
		return actor;
	}

	/**
	 * 根据触发词、srls返回相关的A0/A1
	 * @param triggerPos
	 * @param srls
	 * @param newwords
	 * @param newpostags
	 * @return
	 * @throws SQLException
	 */
	public String[] ActorBySrl(int triggerPos,
			List<edu.hit.ir.ltp4j.Pair<Integer, List<edu.hit.ir.ltp4j.Pair<String, edu.hit.ir.ltp4j.Pair<Integer, Integer>>>>> srls,
			List<String> newwords, List<String> newpostags) throws SQLException {
		// TODO Auto-generated method stub
		String actor[] = { "", "", "" };
		if (triggerPos != -1 && triggerPos < newwords.size()) {
			String triggerWord = newwords.get(triggerPos);
			for (int i = 0; i < srls.size(); i++) {
				if (newwords.get(srls.get(i).first).equals(triggerWord)) {
					for (int j = 0; j < srls.get(i).second.size(); ++j) {
						String Aflag = srls.get(i).second.get(j).first;
						String act = methods.construct(newwords, srls.get(i).second.get(j).second.first,
								srls.get(i).second.get(j).second.second - 1);
						if (Aflag.equals("A1") || Aflag.equals("A2") || Aflag.equals("A3") || Aflag.equals("A4")
								|| Aflag.equals("A5") || Aflag.equals("PSE") || Aflag.equals("BNE"))
							actor[1] = actor[1].concat("_" + act);
						else if (Aflag.equals("A0") || Aflag.equals("PSR"))
							actor[0] = actor[0].concat("_" + act);
					}
					break;
				}
			}
		}
		methods.removeLine(actor);
		return actor;
	}

	

	
	/**
	 * 利用事件类型判断事件的target，是否抽取实体
	 * 
	 * @param title_result
	 * @param eventActorItem
	 * @throws SQLException
	 */
	public void extractActorforTarget(Pair<String, Data> title_result, EventActorItem eventActorItem)
			throws SQLException {

		if (title_result == null || title_result.second == null || eventActorItem == null)
			return;
		LabelItem extractResult = title_result.second.data;

		/**
		 * 过滤掉所有事件source为空的句子
		 */
		if (extractResult == null || eventActorItem.getSourceActor() == null
				|| eventActorItem.getSourceActor().trim().equals("")) {
			return;
		}

		if ((this.preprocess.Settag(extractResult.triggerWord, this.preprocess.getT1paticular()))
				&& (extractResult.triggerTemplate == null || extractResult.triggerTemplate.second.trim().equals(""))) {
			/**
			 * 第一类有几个特殊的触发词时，target不作处理；且触发词不是模板时，对target不作处理
			 */
			if (!(eventActorItem.getTargetActor() == null || eventActorItem.getTargetActor().equals(""))) {
				/**
				 * 保存事件抽取结果
				 */
				extractResult.sourceActor = eventActorItem.getSourceActor();
				extractResult.sourceActorPro = eventActorItem.getSourceActorPro();
				extractResult.targetActor = methods.removeChinese(eventActorItem.getTargetActor().trim());
				extractResult.targetActorPro = eventActorItem.getTargetActorPro();
				extractResult.triggerWord = eventActorItem.getTriggerWord();// .split("_")[0];
			}
		} else {
			/**
			 * 对target进行查找实体处理
			 */
			String targetPrime = eventActorItem.getTargetActor();
			String targetProPrime = eventActorItem.getTargetActorPro();
			ActorItem targetActor = findActorbyDB.getAllActorRole(title_result, eventActorItem.getTargetActor());
			eventActorItem.setTargetActor(targetActor.actor);
			eventActorItem.setTargetActorPro(targetActor.actorPro);

			/**
			 * 将所有的命名实体识别中ns转化为region
			 */
//			this.methods.changeActorProforEventItem(eventActorItem);

			if ((extractResult.triggerTemplate != null && extractResult.triggerTemplate.second != null)) {
				/**
				 * 如果是使用模板，则对结果进行后处理 除了13、17、18类之外的其他类，target必须为实体，也可以为空
				 */
				extractResult.sourceActor = eventActorItem.getSourceActor();
				extractResult.sourceActorPro = eventActorItem.getSourceActorPro();
				extractResult.targetActor = eventActorItem.getTargetActor();
				extractResult.targetActorPro = eventActorItem.getTargetActorPro();
				extractResult.triggerWord = eventActorItem.getTriggerWord();

				/**
				 * 对模板中的合作类等事件做后处理，分开两个国家
				 */
				if ((extractResult.targetActor == null || extractResult.targetActor.trim().equals(""))) {

					if (extractResult.sourceActor != null && extractResult.sourceActorPro != null
							&& (extractResult.sourceActorPro.indexOf("country") != extractResult.sourceActorPro
									.lastIndexOf("country"))) {
						// 当前source中至少有两个国家
						String Actor = extractResult.sourceActor;
						String ActorPro = extractResult.sourceActorPro;
						extractResult.sourceActor = Actor.substring(0, Actor.indexOf("_"));
						extractResult.sourceActorPro = ActorPro.substring(0, ActorPro.indexOf("_"));
						extractResult.targetActor = Actor.substring(Actor.indexOf("_") + 1, Actor.length());
						extractResult.targetActorPro = ActorPro.substring(ActorPro.indexOf("_") + 1, ActorPro.length());
					}
				}

			} else if (!(eventActorItem.getTargetActor() == null || eventActorItem.getTargetActor().equals(""))) {
				/**
				 * 其他情况，且触发词不是模板时，target必须为实体，且不得为空
				 */
				extractResult.sourceActor = eventActorItem.getSourceActor();
				extractResult.sourceActorPro = eventActorItem.getSourceActorPro();
				extractResult.targetActor = eventActorItem.getTargetActor();
				extractResult.targetActorPro = eventActorItem.getTargetActorPro();
				extractResult.triggerWord = eventActorItem.getTriggerWord();
			}
		}
	}


	// public List<String> newwords0;
	public static void main(String[] args)
			throws SQLException, ClassNotFoundException, IOException {
		// TODO Auto-generated method stub
		RoleExtract roleExtract = new RoleExtract(); 
		roleExtract.methods = new Methods();
		
		roleExtract.geonamesUtil = new GeonamesUtil();
		roleExtract.stanford_Parser = new Stanford_Parser();
		roleExtract.preprocess = new ReSegment(roleExtract.methods, roleExtract.geonamesUtil);
		roleExtract.findActorbyDB = new FindActorandPerson(roleExtract.preprocess,new ArgumentExtraction());
		String sentence = "德国新闻电视台23日称，俄罗斯已在其接壤波兰及立陶宛的“飞地”加里宁格勒部署“堡垒”导弹发射器，可发射超音速P-800型巡航导弹" ;

		Data  data = new Data();
		
		LabelItem labelItem = new LabelItem();
		labelItem.newsTitle = sentence;//新闻事件句
//		labelItem.triggerWord = "source在target部署_导弹";//新闻触发词
		labelItem.triggerTemplate = new Pair<String, String>("在_部署“堡垒”导弹", "source在target部署“堡垒”导弹");
		labelItem.eventType = 15;
		data.data = labelItem;
		/**
		 * 新闻预处理
		 */
		data.setTrainData();
//		data.Print();
		
		Pair<String, Data> title_result = new Pair<String, Data>(sentence, data);
		roleExtract.extractactor(title_result );
		title_result.second.data.Printtemp();
	}

}