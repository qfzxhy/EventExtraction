package cetc28.java.eventdetection.entity_extraction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import cetc28.java.dbtool.GeonamesUtil;
import cetc28.java.eventdetection.argument_extraction.ArgumentExtraction;
import cetc28.java.eventdetection.argument_extraction.Methods;
import cetc28.java.eventdetection.argument_extraction.ReSegment;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.textrank.TextRankSummary;
import cetc28.java.news.label.ActorItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.Segmentor;
import edu.hit.ir.ltp4j.SplitSentence;

import opennlp.tools.parser.Parse;
/**
 * 功能描述：
 * 提供从子串中查找sourc、target中实体的方法
 * 从当前实体表中获取所有人名
 * @author qianf
 *
 */
public class FindActorandPerson {
//	private Methods methods;
	private ReSegment reSegment;
	ArgumentExtraction argumentExtraction = new ArgumentExtraction();

	public ReSegment getPreprocess() {
		return reSegment;
	}

	public void setPreprocess(ReSegment preprocess) {
		this.reSegment = preprocess;
	}

	public FindActorandPerson( ReSegment reSegment,ArgumentExtraction argumentExtraction) {
		this.argumentExtraction = argumentExtraction;
		this.reSegment = reSegment;
	}
	public FindActorandPerson( ArgumentExtraction argumentExtraction) {
		this.argumentExtraction = argumentExtraction;
		
	}
	
	/**
	 * 从当前实体表中获取所有人名
	 * @param actorItem
	 * @return
	 */
	public String findallperson(ActorItem actorItem) {
		// TODO Auto-generated method stub
		String person = "";
		String actor[] = actorItem.actor.split("_");
		String actorPro[] = actorItem.actorPro.split("_");
		for (int i = 0; i < actorPro.length; i++) {
			if (actorPro[i].equals("person"))
				person = person.concat("_" + actor[i]);
		}
		person = person.startsWith("_") ? person.substring(1, person.length()) : person;
		person = person.endsWith("_") ? person.substring(0, person.length() - 1) : person;
		return person;
	}


	/**
	 * 获取所有存在于当前的实体列表中的实体
	 * @param dataResult
	 * @param targetActor
	 * @return
	 */
	public ActorItem getAllActorRole(Pair<String, Data> dataResult, String targetActor) {
		String actor = "";
		String actorPro = "";
		String index = "";
		String len = "";
		
//		System.out.println("查找当前的"+targetActor);
		ActorItem actorItem = new ActorItem(actor, actorPro, index, len);
		if (dataResult == null || dataResult.first == null || dataResult.second == null || targetActor == null)
			return actorItem;
		String target[] = targetActor.split("_");
		/*
		 * 原来的句子
		 */
		String sentencePrime = dataResult.first.replaceAll("\\s+", "").replaceAll("\t", "");
		/**
		 * 句子中所有的命名实体
		 */
		String ner = Ner.ner(sentencePrime);
		if(ner == null || ner.trim().equals(""))return actorItem;
//		System.out.println("ner:"+ner);
		
		String[] nerList = ner.split("\\s+");
		
		/**
		 * 对每一个target进行命名实体识别
		 */
		for (String t : target) {
			if (t == null && t.trim().length() == 0)
				return actorItem;
//			System.out.println("t"+t);

			if (sentencePrime.indexOf(t) == -1)
				return actorItem;
			
			List<Pair<String, String>> entityPro = null;
			int begin = sentencePrime.indexOf(t);
			int end = begin + t.length() - 1;

			while (begin != -1 && begin < end && (entityPro == null || entityPro.size() == 0)) {
					entityPro = argumentExtraction.getEntity(nerList, begin, end);
					begin = sentencePrime.indexOf(t, end + 1);
					end = begin + t.length() - 1;
			}

			if (entityPro != null) {
				for (int i = 0; i < entityPro.size(); i++) {
					actor = actor.equals("") ? entityPro.get(i).first : actor.concat("_" + entityPro.get(i).first);
					String actorProTemp = changeAttritubution(entityPro.get(i).second);
					actorPro = actorPro.equals("") ? actorProTemp : actorPro.concat("_" + actorProTemp);
				}
			}
		}

		actor = actor.startsWith("_") ? actor.substring(1, actor.length()) : actor;
		actorPro = actorPro.startsWith("_") ? actorPro.substring(1, actorPro.length()) : actorPro;
		actorItem.setActor(actor.endsWith("_") ? actor.substring(0, actor.length() - 1) : actor);
		actorItem.setActorPro(actorPro.endsWith("_") ? actorPro.substring(0, actorPro.length() - 1) : actorPro);
		return actorItem;
	}
	
	private String changeAttritubution(String attribute)
	{
		// TODO Auto-generated method stub
		switch (attribute)
		{
		case "nt":
			return "organization";
		case "role":
			return "role";
		case "country":
			return "country";
		case "ns":
			return "region";
		case "nr":
			return "person";
		case "dev":
			return "device";
		}
		return "";
	}

	public static void main(String[] args) {
		new LtpTool();

		FindActorandPerson findActorbyDB = new FindActorandPerson(new ReSegment(new Methods(), new GeonamesUtil()), new ArgumentExtraction());

		/**
		 * 命名实体识别结果测试
		 */
				String news_content = "据悉，该系统由越军队通信集团研发，2015年4月在部分单位试用，目前越国防部决定在全军范围使用该系统。"
				+ "据悉，该系统由越军队通信集团研发，2015年4月在部分单位试用，目前越国防部决定在全军范围使用该系统"
				+ "据悉，该系统由越军队通信集团研发，2015年4月在部分单位试用，目前美国决定在全军范围使用该系统";
		ActorItem actor = null;
			actor = Ner.ner_actionitem("武器装备：越南清除大批美军遗留炸弹");
			System.out.println("actor.actor:" + actor.actor);
			System.out.println("actor.actorPro:" + actor.actorPro);
		/**
		 * 从一句话中获取所有的命名实体测试
		 */
//		String sentence = "德国新闻电视台23日称，俄罗斯已在其接壤波兰及立陶宛的“飞地”加里宁格勒部署“堡垒”导弹发射器，可发射超音速P-800型巡航导弹";
//		Data data = new Data();
//		Pair<String, Data> dataResult = new Pair<String, Data>(sentence,data);
//		ActorItem actorItem = findActorbyDB.getAllActorRole(dataResult, "俄罗斯已");
//		actorItem.Print();
	}
}
