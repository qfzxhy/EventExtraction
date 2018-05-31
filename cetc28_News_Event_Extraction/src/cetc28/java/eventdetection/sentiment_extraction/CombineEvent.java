package cetc28.java.eventdetection.sentiment_extraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cetc28.java.news.label.ActorItem;
import cetc28.java.news.label.EventItem;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.Segmentor;


public class CombineEvent {
	/**
	 * 对每一句话判断其情感极性，输出合并相同的事件集
	 * @param eventlist
	 * @return
	 */
	public List<EventItem> ExtractSentiment(List<EventItem> eventlist) {
		// TODO Auto-generated method stub
		if (eventlist != null) {
			for (int i = 0; i < eventlist.size(); i++) {
				if (eventlist.get(i).isIf_event() == true) {
					if (eventlist.get(i).isIf_title() == true) {// || eventlist.get(i).getSentiment().equals("0")
						/**
						 * 标题句其自身为情感句子
						 */
						eventlist.get(i).setSentiment_text(eventlist.get(i).getNews_content());
						eventlist.get(i).setSentiment(eventlist.get(i).getSentiment());
					} else {
						/**
						 *  如果当前句子为正文，则向前\向后面抽取最近的一个情感句子
						 */
						int distence = 0;
						for (int j = i - 1, k = i + 1; j >= 0 && k < eventlist.size() && distence < 4; j--, k++) {
							distence++;
							/**
							 * 向前找到句子是标题句，则跳出循环
							 */
							if ((eventlist.get(j).isIf_title()) == true)
								break; 
							else if (eventlist.get(j).isIf_sentiment() == true) {
								eventlist.get(i).setSentiment_text(eventlist.get(j).getNews_content());
								eventlist.get(i).setSentiment(eventlist.get(j).getSentiment());
								break;
							} 
							else if (eventlist.get(k).isIf_sentiment() == true) {
								eventlist.get(i).setSentiment_text(eventlist.get(k).getNews_content());
								eventlist.get(i).setSentiment(eventlist.get(k).getSentiment());
								break;
							}
						}
					}
					if (eventlist.get(i).getSentiment_text().trim().equals("")) {
						/**
						 *  没有找到观点句，则句子本身作为其观点句
						 */
						eventlist.get(i).setSentiment_text(eventlist.get(i).getNews_content());
						eventlist.get(i).setSentiment(eventlist.get(i).getSentiment());
					}
				}
			}
			return eventlist;
		} else
			return null;
	}
	

	public static void main(String[] args) {

		CombineEvent setsentiment = new CombineEvent();
		List<EventItem> eventList = new ArrayList();
		EventItem e1 = new EventItem();
		LabelItem LabelItem1 = new LabelItem();
		LabelItem1.newsContent = "1";
		LabelItem1.sourceActor = "1";

		LabelItem1.targetActor = "1";
		LabelItem1.triggerWord = "1";
		LabelItem1.eventType = 1;
		LabelItem1.actorItem = new ActorItem();
		LabelItem1.actorItem.actor = "1";
		LabelItem1.allPerson = "1";
		LabelItem1.imgAddress = "1";
		LabelItem1.newsURL = "1";
		LabelItem1.newsTitle = "1";

		Pair<String, LabelItem> title_result1 = new Pair<String, LabelItem>("1", LabelItem1);
		e1.setCon_result(title_result1);

		e1.setIf_event(true);
		e1.setIf_sentiment(true);
		e1.setIf_title(false);
		e1.setSentiment("-1");
		e1.setSentiment_text("sentiment1");

		// e1.Print();
		EventItem e2 = new EventItem();
		LabelItem LabelItem2 = new LabelItem();
		LabelItem2.newsContent = "2";
		LabelItem2.sourceActor = "2";

		LabelItem2.targetActor = "2";
		LabelItem2.triggerWord = "2";
		LabelItem2.eventType = 2;
		LabelItem2.actorItem = new ActorItem();
		LabelItem2.actorItem.actor = "2";

		LabelItem2.allPerson = "2";
		LabelItem2.imgAddress = "2";
		LabelItem2.newsURL = "2";
		LabelItem2.newsTitle = "2";

		Pair<String, LabelItem> title_result2 = new Pair<String, LabelItem>("2", LabelItem2);
		e2.setCon_result(title_result2);

		e2.setIf_event(true);
		e2.setIf_sentiment(true);
		e2.setIf_title(false);
		e2.setSentiment("-2");
		e2.setSentiment_text("sentiment2");

		EventItem e3 = new EventItem();
		LabelItem LabelItem3 = new LabelItem();
		LabelItem3.newsContent = "3";
		LabelItem3.sourceActor = "1";

		LabelItem3.targetActor = "1";
		LabelItem3.triggerWord = "1";
		LabelItem3.eventType = 1;
		LabelItem3.actorItem = new ActorItem();
		LabelItem3.actorItem.actor = "3";

		LabelItem3.allPerson = "3";
		LabelItem3.imgAddress = "3";
		LabelItem3.newsURL = "3";
		LabelItem3.newsTitle = "3";

		Pair<String, LabelItem> title_result3 = new Pair<String, LabelItem>("3", LabelItem3);
		e3.setCon_result(title_result3);

		e3.setIf_event(true);
		e3.setIf_sentiment(true);
		e3.setIf_title(false);
		e3.setSentiment("-3");
		e3.setSentiment_text("sentiment3");

		eventList.add(e1);
		eventList.add(e2);
		eventList.add(e3);

		List<EventItem> neweventList = setsentiment.ExtractSentiment(eventList);
		for (EventItem event : neweventList) {
			event.Print();
		}

	}

	public float Type2Sentiment(LabelItem second) {
		// TODO Auto-generated method stub
		return 0;
	}
}
