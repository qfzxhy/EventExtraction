package cetc28.java.news.label;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
/*
Created on 2015年10月19日 上午10:40:20

@author: GreatShang
*/
import java.sql.Timestamp;

import cetc28.java.nlptools.Pair;

public class LabelItem 
{

	public String labelID;			//数据库中该条标注的ID
	public String newsURL;		//新闻来源
	public String newsID;			//新闻ID
	public String newsTitle;		//新闻标题
	public String imgAddress;
	public String newsContent;		//新闻正文
	public boolean ifEvent = false;	//是否事件相关
	public int eventType = -1;		//事件类型（1-20）
	public String sourceActor;		//事件发出者
	public String targetActor;		//事件承受者
	public String triggerWord;		//事件触发词
	public String sourceActorPro;		//事件发出者
	public String targetActorPro;		//事件承受者
	public String eventTime;		//事件发生时间
	public String eventLocation;	//事件发生地点
	public String eventLocationPro;	//事件发生地点的Pro
	public ActorItem actorItem;		//当前句子中所有的实体属性
	public Timestamp saveTime;//当前句子爬取的时间
	
	public ActorProItem sourceActorItem;
	public ActorProItem targetActorItem;
	public ActorProItem placectorItem;
	public String typeFour;// 四大类性质得分
	public String Proscore;// 事件性质得分
	public String allPerson;

	public Pair<String, String> triggerTemplate;
	public String tempPlaceEntity;
	
	public LabelItem(String labelID,String newsURL,String newsID,String newsTitle)
	{
		//与事件无关新闻标题标注构造函数
		this.newsURL = newsURL;
		this.labelID = labelID;
		this.ifEvent = false;
		this.newsID = newsID;
		this.newsTitle = newsTitle;
	}
	
	public LabelItem(String labelID,String newsURL,String newsID,String newsTitle,
			int eventType,String sourceActor,String targetActor,String triggerWord,
			String eventTime,String eventLocation,String sourceActorPro,String targetActorPro)
	{
		//事件有关新闻标题标注构造函数
		this.newsURL = newsURL;
		this.labelID = labelID;
		this.ifEvent = true;
		this.newsID = newsID;
		this.newsTitle = newsTitle;
		this.eventType = eventType;
		this.sourceActor = sourceActor;
		this.targetActor = targetActor;
		this.triggerWord = triggerWord;
		this.eventTime = eventTime;
		this.eventLocation = eventLocation;
		this.sourceActorPro = sourceActorPro;
		this.targetActorPro = targetActorPro;
	}
	
	public LabelItem(String labelID,String newsURL,String newsID,String newsTitle,
			int eventType,String sourceActor,String targetActor,String triggerWord,
			String eventTime,String eventLocation)
	{
		//事件有关新闻标题标注构造函数
		this.newsURL = newsURL;
		this.labelID = labelID;
		this.ifEvent = true;
		this.newsID = newsID;
		this.newsTitle = newsTitle;
		this.eventType = eventType;
		this.sourceActor = sourceActor;
		this.targetActor = targetActor;
		this.triggerWord = triggerWord;
		this.eventTime = eventTime;
		this.eventLocation = eventLocation;
	}
	
	
//	by Daij
	public LabelItem(String labelID,String newsSource,String newsID,String newsTitle,
			int eventType,String sourceActor,String targetActor,String triggerWord,
			String eventTime,String eventLocation,String sourceActorPro,String targetActorPro,ActorItem actorItem)
	{
		//事件有关新闻标题标注构造函数
		this.newsURL = newsURL;
		this.labelID = labelID;
		this.ifEvent = true;
		this.newsID = newsID;
		this.newsTitle = newsTitle;
		this.eventType = eventType;
		this.sourceActor = sourceActor;
		this.targetActor = targetActor;
		this.triggerWord = triggerWord;
		this.eventTime = eventTime;
		this.eventLocation = eventLocation;
		this.actorItem = actorItem;
		this.sourceActorPro = sourceActorPro;
		this.targetActorPro = targetActorPro;
	}
	
	
	public LabelItem() {
		// TODO Auto-generated constructor stub
	}

	public void Print()
	{
		System.out.println("\n。。。。。。。。。。事件抽取结果开始。。。。。。。。。。。。。。");

		System.out.println("labelID :"+labelID);
		System.out.println("newsUrl :"+newsURL);
		System.out.println("ifEvent ："+ifEvent);
		System.out.println("newsID ："+newsID);
		System.out.println("newsTitle： "+newsTitle);
		System.out.println("newsContent： "+newsContent);

		System.out.println("eventType ："+eventType);
		
		System.out.println("********************************************发起者");
		
		System.out.println("sourceActor： "+sourceActor);
		System.out.println("sourceActorPro： "+sourceActorPro);
		sourceActorItem.print();
		
		System.out.println("********************************************承受者");
		
		System.out.println("targetActor： "+targetActor);
		System.out.println("targetActorPro： "+targetActorPro);
		targetActorItem.print();
		
		System.out.println("********************************************触发词");
		System.out.println("triggerWord："+triggerWord);
		System.out.println("********************************************时间");
		System.out.println("eventTime :"+eventTime);		
		System.out.println("saveTime:"+saveTime);	
		
		System.out.println("********************************************地点");
		System.out.println("eventLocation: "+eventLocation);
		System.out.println("eventLocationPro: "+eventLocationPro);
		placectorItem.print();
		
		System.out.println("imgAddress:"+imgAddress);	
		System.out.println("allPerson:"+allPerson);	
		System.out.println("allActor:"+actorItem.actor);	
		

//		System.out.println("。。。。。。。。。。LabelItem内容结束。。。。。。。。。。。。。。\n");

	}
	
	public void Printtemp()
	{
		System.out.println("newsTitle ："+newsTitle);
		System.out.println("eventType ："+eventType);
		System.out.println("triggerWord ："+triggerWord);
		System.out.println("sourceActor ："+sourceActor);
		System.out.println("targetActor ："+targetActor);
		System.out.println("eventTime "+eventTime);
		System.out.println("eventLocation "+eventLocation);
		System.out.println();
	}
	
	public void Write(String name) throws IOException
	{
		File file = new File(name);
		file.delete();
		file.createNewFile();
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(name), true));
		bw.write("newsTitle ："+newsTitle+"\r");
		bw.write("eventType ："+eventType+"\r");
		bw.write("triggerWord ："+triggerWord+"\r");
		bw.write("sourceActor ："+sourceActor+"\r");
		bw.write("targetActor ："+targetActor+"\r");
		bw.write("eventTime "+eventTime+"\r");
		bw.write("eventLocation "+eventLocation+"\r\n");
		bw.flush();
	}
	public String toDemoString()
	{
		if (this.ifEvent == false)
			return this.newsTitle+
					"\nNot event related.";
		else
			return this.newsTitle+
				"\nEventType: "+this.eventType+
				"\nSource: "+this.sourceActor+
				"\nTrigger:	"+this.triggerWord+
				"\nTarget: "+this.targetActor+
				"\nLocation: "+this.eventLocation+
				"\nTime: "+this.eventTime;
	}
	public String toString()
	{
		return this.labelID+this.newsURL+" "+this.newsID+" " +this.newsTitle;
	}

	public void setValues( String imgAddress, String newsURL,String newsID, String placeEntity, boolean b, String sentence) {
		// TODO Auto-generated method stub
		this.imgAddress = imgAddress;
		this.newsURL = newsURL;
		this.newsID = newsID;
		this.tempPlaceEntity = placeEntity;
		this.ifEvent = b;
		this.newsContent = sentence;
	}
	
}
