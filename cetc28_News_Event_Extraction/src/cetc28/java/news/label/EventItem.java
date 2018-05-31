package cetc28.java.news.label;

import java.util.ArrayList;

import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.nlptools.Pair;

//内存中存储一个类，存放已经爬取的事件的所有信息
public class EventItem {
	String news_content = "";//新闻句子
	public String getNews_content() {
		return news_content;
	}


	public void setNews_content(String news_content) {
		this.news_content = news_content;
	}


	public boolean isIf_event() {
		return if_event;
	}


	public void setIf_event(boolean if_event) {
		this.if_event = if_event;
	}


	public Pair<String, LabelItem> getCon_result() {
		return con_result;
	}


	public void setCon_result(Pair<String, LabelItem> con_result) {
		this.con_result = con_result;
	}


	public String getSentiment() {
		return sentiment;
	}


	public void setSentiment(String sentiment) {
		this.sentiment = sentiment;
	}


	public boolean isIf_sentiment() {
		return if_sentiment;
	}


	public void setIf_sentiment(boolean if_sentiment) {
		this.if_sentiment = if_sentiment;
	}


	public String getSentiment_text() {
		return sentiment_text;
	}


	public void setSentiment_text(String sentiment_text) {
		this.sentiment_text = sentiment_text;
	}


	public boolean isIf_title() {
		return if_title;
	}


	public void setIf_title(boolean if_title) {
		this.if_title = if_title;
	}


	public boolean isIf_newsTemplte() {
		return if_newsTemplte;
	}


	public void setIf_newsTemplte(boolean if_newsTemplte) {
		this.if_newsTemplte = if_newsTemplte;
	}


	public int getRelatedtime() {
		return relatedtime;
	}


	public void setRelatedtime(int relatedtime) {
		this.relatedtime = relatedtime;
	}


	boolean if_event = false;//新闻是否为事件
	Pair<String, LabelItem>  con_result;//新闻事件抽取结果
	String sentiment = "0";//新闻的情感
	boolean if_sentiment= false;//新闻是否为情感句
	String sentiment_text = "";//新闻的情感句
	boolean if_title = false;//新闻是否为标题句
	boolean if_newsTemplte= false;//新闻是否为一些模板
	int relatedtime = 1;
	public boolean isIf_summary() {
		return If_summary;
	}


	public void setIf_summary(boolean if_summary) {
		If_summary = if_summary;
	}


	boolean If_summary = false;
	
	
	
//	String news_url = "";//新闻的url
//	String img_address = "";//新闻的图片地址
//	String news_title = "";//新闻的title
//	String save_time;
	
	


	
	
	
	
//	public boolean isIf_newsTemplte() {
//		return if_newsTemplte;
//	}
//
//
//	public void setIf_newsTemplte(boolean if_newsTemplte) {
//		this.if_newsTemplte = if_newsTemplte;
//	}
//
//
//	boolean if_summary= false;//新闻是否为摘要句子
//	public boolean isIf_summary() {
//		return if_summary;
//	}
//
//
//	public void setIf_summary(boolean if_summary) {
//		this.if_summary = if_summary;
//	}
//
//
//	public boolean isIf_triggerTemplate() {
//		return if_triggerTemplate;
//	}
//
//
//	public void setIf_triggerTemplate(boolean if_triggerTemplate) {
//		this.if_triggerTemplate = if_triggerTemplate;
//	}
//
//
//	boolean if_triggerTemplate= false;//新闻是触发词为摘要句
//
//
//	
////	public String getNews_title() {
////		return news_title;
////	}
////
////
////	public void setNews_title(String news_title) {
////		this.news_title = news_title;
////	}
//
//
//	public boolean isIf_title() {
//		return if_title;
//	}
//
//
//	public void setIf_title(boolean if_title) {
//		this.if_title = if_title;
//	}


	
	public EventItem(String news_content, boolean if_event, Pair<String, LabelItem> con_result, String sentiment,
			String news_url, String img_address) {
		this.news_content = news_content;
		this.if_event = if_event;
		this.con_result = con_result;
		this.sentiment = sentiment;
//		this.news_url = news_url;
//		this.img_address = img_address;
	}


	public EventItem(String news_content, boolean if_newsTemplte) {
		// TODO Auto-generated constructor stub
		this.news_content = news_content;
		this.if_newsTemplte = if_newsTemplte;
	}


	public EventItem() {
		// TODO Auto-generated constructor stub
	}


//	public String getNews_content() {
//		return news_content;
//
//	}
//
//
//	public void setNews_content(String news_content) {
//		this.news_content = news_content;
//	}
//
//
//	
//	
//	public boolean isIf_sentiment() {
//		return if_sentiment;
//	}
//
//
//	public void setIf_sentiment(boolean if_sentiment) {
//		this.if_sentiment = if_sentiment;
//	}
//	
//	public boolean getIf_sentiment() {
//		return this.isIf_sentiment();
//	}
//
//
//	
//	
//	public int getRelatedtime() {
//		return relatedtime;
//	}
//
//
//	public void setRelatedtime(int relatedtime) {
//		this.relatedtime = relatedtime;
//	}
//
//
//	
//	
//	
//	public String getSentiment_text() {
//		return sentiment_text;
//
//	}
//
//
//	public void setSentiment_text(String sentiment_text) {
//		this.sentiment_text = sentiment_text;
//	}

//	public String getSave_time() {
//		return save_time;
//	}
//
//	public void setSave_time(String save_time) {
//		this.save_time = save_time;
//	}
//
//
//	
//
//	public String getNews_url() {
//		return news_url;
//
//	}
//
//	public void setNews_url(String news_url) {
//		this.news_url = news_url;
//	}
//
//	public String getImg_address() {
//		return img_address;
//	}
//
//	public void setImg_address(String img_address) {
//		this.img_address = img_address;
//	}
//
//	public Pair<String, LabelItem> getCon_result() {
//		return con_result;
//	}
//
//	public void setCon_result(Pair<String, LabelItem> title_result) {
//		this.con_result = title_result;
//		this.img_address = title_result.second.imgAddress == null?"":title_result.second.imgAddress;
//		this.news_url = title_result.second.newsURL == null?"":title_result.second.newsURL;
//		this.news_title = title_result.second.newsTitle == null?"":title_result.second.newsTitle;
//		this.save_time = title_result.second.eventTime == null?"":title_result.second.eventTime;
//		this.allActors = title_result.second.actorItem == null || title_result.second.actorItem.actor == null?"":title_result.second.actorItem.actor;
//		
//	}
//
//	public boolean getIf_event() {
//		return if_event;
//	}
//
//	public void setIf_event(boolean if_event) {
//		this.if_event = if_event;
//	}
//
//	public String getSentiment() {
//		return sentiment;
//	}
//
//	public void setSentiment(String sentiment) {
//		this.sentiment = sentiment;
//	}

	
	
	public static void main(String[] args) {
		EventItem e1 = new EventItem();
		LabelItem LabelItem1 = new LabelItem();
		LabelItem1.newsContent = "1";
		LabelItem1.sourceActor = "1";

		LabelItem1.targetActor = "1";
		LabelItem1.triggerWord = "1";
		LabelItem1.eventType = 1;
		LabelItem1.actorItem= new ActorItem();
		LabelItem1.allPerson = "1";
		LabelItem1.imgAddress = "1";
		LabelItem1.newsURL= "1";
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
		LabelItem2.actorItem= new ActorItem();
		LabelItem2.allPerson = "2";
		LabelItem2.imgAddress = "2";
		LabelItem2.newsURL= "2";
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
		LabelItem3.actorItem= new ActorItem();
		LabelItem3.allPerson = "3";
		LabelItem3.imgAddress = "3";
		LabelItem3.newsURL= "3";
		LabelItem3.newsTitle = "3";
		
		
		Pair<String, LabelItem> title_result3 = new Pair<String, LabelItem>("3", LabelItem3);
		e3.setCon_result(title_result3);

		e3.setIf_event(true);
		e3.setIf_sentiment(true);
		e3.setIf_title(false);
		e3.setSentiment("-3");
		e3.setSentiment_text("sentiment3");
		// TODO Auto-generated method stub
		EventItem eventItem = new EventItem();
		eventItem.setCon_result(e1, e3);
		e1.Print();
	}


	public void Print() {
		// TODO Auto-generated method stub
		System.out.println("。。。。。。。。。。EventItem内容开始。。。。。。。。。。。。。。");
		System.out.println("新闻句子:"+this.news_content);	
		System.out.println("是否为摘要:"+this.isIf_summary());
		System.out.println("是否为模板句:"+this.isIf_newsTemplte());	

//		System.out.println("getNews_content:"+this.getNews_content());
		
//		this.getCon_result().second.Print();
//		System.out.println("getCon_result:"+this.getCon_result());
//		System.out.println("this.getImg_address():"+this.getImg_address());
//		System.out.println("this.getNews_title():"+this.getNews_title());
//		System.out.println("this.getNews_url():"+this.getNews_url());
		System.out.println("。。。。。。。。。。EventItem内容结束。。。。。。。。。。。。。。");

	}

	//用event2 扩充 event1
	public void setCon_result(EventItem event1, EventItem event2) {
		// TODO Auto-generated method stub
		if(event1 == null || event2 ==null )return ;
		
		Pair<String, LabelItem> stringLabelItem1 = event1.con_result;
		Pair<String, LabelItem> stringLabelItem2 = event2.con_result;
		if(stringLabelItem1 == null || stringLabelItem2 == null)return;
		
		LabelItem labelItem1 = stringLabelItem1.second;
		LabelItem labelItem2 = stringLabelItem2.second;
		if(labelItem1 == null || labelItem1 == null)return;
		stringLabelItem1.first = stringLabelItem1.first+"___"+stringLabelItem2.first;//新闻正文
//		Pair<String, LabelItem> newLabelItem = new Pair<String, LabelItem>(first, second)
		labelItem1.imgAddress = labelItem1.imgAddress+"___"+labelItem2.imgAddress;//图片地址
		labelItem1.newsURL = labelItem1.newsURL+"___"+labelItem2.newsURL;//url
		labelItem1.newsTitle = labelItem1.newsTitle+"___"+labelItem2.newsTitle;//新闻标题
		labelItem1.newsContent = labelItem1.newsContent+"___"+labelItem2.newsContent;//正文
		labelItem1.actorItem.actor = labelItem1.actorItem.actor+"___"+labelItem2.actorItem.actor;//所有实体
		labelItem1.allPerson = labelItem1.allPerson+"___"+labelItem2.allPerson;//所有人名	
	}

}
