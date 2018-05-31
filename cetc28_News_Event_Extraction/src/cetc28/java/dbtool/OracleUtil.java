
package cetc28.java.dbtool;

import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.sql.rowset.serial.SerialBlob;
import org.apache.solr.common.SolrDocument;
import org.pojava.datetime.DateTime;
import cetc28.java.config.DBConfig;
import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.eventdetection.sentiment_extraction.CombineEvent;
import cetc28.java.eventdetection.time_location_extraction.LocationExtraction;
import cetc28.java.eventdetection.time_location_extraction.LocationExtraction;
import cetc28.java.news.label.ActorItem;
import cetc28.java.news.label.ActorProItem;
import cetc28.java.news.label.EventActorItem;
import cetc28.java.news.label.EventItem;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
/**
 * 将最终事件抽取结果保存到oracle
 * 与Oracle数据库的交互
 * @author qianf
 * 2016.11.22
 */
public class OracleUtil {
	public GeonamesUtil geonamesUtil;
	public CombineEvent combineEvent;
	private Statement psmt;
	private String queryString;
	private ResultSet rset;
	public static String sqlURL = DBConfig.getOracleUrl();// =
	public static String sqlUser = DBConfig.getOracleRoot();// = "root";
	public static String sqlPasswd = DBConfig.getOraclePassword();// = "123456";
	private Connection connectionOracle;
	public OracleUtil(Connection connectionOracle) {
		this.connectionOracle = connectionOracle;
	}
	public Connection getconnectionOracle() {
		return connectionOracle;
	}

	public void setconnectionOracle(Connection connectionOracle) {
		this.connectionOracle = connectionOracle;
	}


	public OracleUtil(GeonamesUtil geonamesUtil,CombineEvent combineEvent) {
		// TODO Auto-generated constructor stub
		try {
			this.geonamesUtil = geonamesUtil;
			this.combineEvent = combineEvent;
			LinkOracle(sqlUser, sqlPasswd, sqlURL);

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 数据库连接初始化
	 * @param databasePath
	 * @param mysqlPassword
	 * @param mysqlUser
	 * 
	 */
	public void LinkOracle(String mysqlUser, String mysqlPassword, String databasePath) throws ClassNotFoundException {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");
			connectionOracle = DriverManager.getConnection(databasePath, mysqlUser, mysqlPassword);
		} catch (Exception e) {
			System.out.println("Initialize failed");
			e.printStackTrace();
		}
	}

	/**
	 * 断开数据库连接函数
	 * 
	 */
	public void CloseOracle() {
		if (connectionOracle != null) {
			try {
				connectionOracle.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			System.out.println("Database close");
		}
	}
	
	/**
	 * 将事件列表中的事件插入到数据库中
	 * @param Neweventlist
	 * @param flag true:插入到数据库， false：打印输出到控制台
	 * @throws Exception
	 */
	public void InsertOracle(List<EventItem> Neweventlist,boolean flag) throws Exception {

		if (Neweventlist == null || Neweventlist.size() == 0)
			return;

		for (int i = 0; i < Neweventlist.size(); i++) {
			if (Neweventlist.get(i).isIf_event() != true || Neweventlist.get(i).getCon_result() == null
					|| Neweventlist.get(i).getCon_result().second.sourceActor == null
					|| Neweventlist.get(i).getCon_result().second.sourceActor.trim().equals(""))
				continue;
			/**
			 * 将当前事件抽取结果插入到数据库
			 */
			insertEventintoOracle(Neweventlist.get(i),flag);
		}
	}
	
	/**
	 * 将当前存储事件的对象中的数据插入到数据库中
	 * @param eventItem
	 * @throws Exception
	 */
	public void insertEventintoOracle(EventItem eventItem ,boolean flag) throws Exception {
		// TODO Auto-generated method stub

		Pair<String, LabelItem> con_result = eventItem.getCon_result();
		String sentimentTextContent = eventItem.getSentiment_text();
		String sentimentResultContent = eventItem.getSentiment();
		int relatedtime = eventItem.getRelatedtime();

		try {
			this.geonamesUtil.findAllProperties(con_result.second);
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("查找实体经纬度时有问题");
		}
		
		
		/*
		 * 结果插入到数据库
		 */
		if(flag == true){
			try {
				AddLabeltoOracle(con_result.second, sentimentTextContent, sentimentResultContent,relatedtime);			
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("插入oracle时有问题");
			}
		}else{
			/**
			 * 结果打印在控制台
			 */
			Print(con_result.second, sentimentTextContent, sentimentResultContent,relatedtime);
		}
		
	}

	private void Print(LabelItem eventItem, String sentimentTextContent, String sentimentResultContent, int relatedtime)
	{
		// TODO Auto-generated method stub结果输出在控制台
		eventItem.Print();
		System.out.println("情感句："+sentimentTextContent);
		System.out.println("情感极性："+sentimentResultContent);
		System.out.println("提及次数："+relatedtime);
		System.out.println("\n。。。。。。。。。。事件抽取结果结束。。。。。。。。。。。。。。");

	}
	/**
	 * 将结果写入到oracle数据库
	 * @param news_url
	 * @param news_title
	 * @param img_address
	 * @param event_id
	 * @param eventItem
	 * @param sentimentResultContent
	 * @param eventlist
	 * @throws Exception
	 */
	@SuppressWarnings("deprecation")
	public void AddLabeltoOracle(LabelItem eventItem, String sentiment_text, String sentimentResultContent,
			int relatedtime) throws Exception {
		System.out.println("插入数据" + eventItem.newsContent);
		String newsTime = eventItem.newsID.substring(0, 8);
		/**
		 * 如果当前的source为空，则不进行插入过程
		 */
		if(eventItem == null || eventItem.sourceActor == null || eventItem.sourceActor.trim().equals(""))return;
		
		//将所有null转换为""
		removeNull(eventItem);
		
		String source = eventItem.sourceActor;
		String target = eventItem.targetActor;
		String place = eventItem.eventLocation;
		String triggerWord = eventItem.triggerWord;
	
		/**
		 * 查询当前数据库中当前时间段内现有的所有事件值
		 */
		java.sql.PreparedStatement selectOracle = connectionOracle.prepareStatement("select * from EVENTGKG where EVENTID like ?");//
		selectOracle.setString(1, "%" + newsTime +"%");
		
		try
		{
			rset = selectOracle.executeQuery();
			while(rset.next()){
//				System.out.println("数据库中已经有当前事件的数据");
				String eventid = rset.getString("EVENTID");
				String sourceOracle = rset.getString("ACTOR1NAME") == null ? "" : rset.getString("ACTOR1NAME");
				String targetOracle = rset.getString("ACTOR2NAME") == null ? "" : rset.getString("ACTOR2NAME");
				String placeOracle = rset.getString("ACTIONGEO_FULLNAME") == null ? "" : rset.getString("ACTIONGEO_FULLNAME");
				String triggerWordOracle = rset.getString("ACTIONNAME") == null ? "" : rset.getString("ACTIONNAME");
				String EVENTEXT = rset.getString("EVENTEXT") == null ? "" : rset.getString("EVENTEXT");
				String ALLPERSONS = rset.getString("ALLPERSONS") == null ? "" : rset.getString("ALLPERSONS");
				String ALLNAMES = rset.getString("ALLNAMES") == null ? "" : rset.getString("ALLNAMES");
				String VIEWTEXT = rset.getString("VIEWTEXT") == null ? "" : rset.getString("VIEWTEXT");
				String AVGTONE = rset.getString("AVGTONE") == null ? "" : rset.getString("AVGTONE");
				String SOURCEURL = rset.getString("SOURCEURL") == null ? "" : rset.getString("SOURCEURL");
				String TITLE = rset.getString("TITLE") == null ? "" : rset.getString("TITLE");
				String SHARINGIMAGE = rset.getString("SHARINGIMAGE") == null ? "" : rset.getString("SHARINGIMAGE");
				
				Timestamp timeOracle = rset.getTimestamp("SQLDATE");
				int NUMARTICLES = rset.getInt("NUMARTICLES");
				
				if(sourceOracle.equals(source) && targetOracle.equals(target) 
						&& placeOracle.equals(place) && triggerWordOracle.equals(triggerWord) && 
						(timeOracle.getYear() == eventItem.saveTime.getYear() && timeOracle.getMonth() == eventItem.saveTime.getMonth()
						&& timeOracle.getDay() == eventItem.saveTime.getDay())){
					selectOracle.close();
					/*
					 * 数据库中已经有相同的事件,更新已有事件的指定字段
					 */
					java.sql.PreparedStatement updateOracle = connectionOracle.prepareStatement(
							"UPDATE EVENTGKG set EVENTEXT = ? , ALLPERSONS = ? , ALLNAMES = ? , VIEWTEXT = ? , AVGTONE = ? , SOURCEURL = ? , TITLE = ? , SHARINGIMAGE = ?, NUMARTICLES = ? where EVENTID = ?");
					updateOracle.setString(1, EVENTEXT+"___"+eventItem.newsContent);// 新闻事件句
					updateOracle.setString(2, ALLPERSONS+"___"+eventItem.allPerson);// 新闻所有人
					updateOracle.setString(3, ALLNAMES+"___"+eventItem.actorItem.actor);// 新闻的所有实体
					updateOracle.setString(4, VIEWTEXT+"___"+sentiment_text);// 新闻观点句
					updateOracle.setString(5, AVGTONE+"___"+sentimentResultContent);// 新闻观点
					updateOracle.setString(6, SOURCEURL+"___"+eventItem.newsURL);// 新闻url
					updateOracle.setString(7, TITLE+"___"+eventItem.newsTitle);// 新闻标题
					updateOracle.setString(8, SHARINGIMAGE+"___"+eventItem.imgAddress);// 新闻图片地址
					updateOracle.setLong(9, NUMARTICLES + relatedtime);// 被引用次数
					updateOracle.setString(10, eventid);// 被引用次数

					try {
						updateOracle.executeUpdate();
//						System.out.println("更新成功");
						updateOracle.close();
					} catch (Exception e) {
					   	e.printStackTrace();
					   	updateOracle.close();
						 System.out.println("更新oracle时有问题");
					}
					return;
				}
			}
		} catch (Exception e)
		{
			// TODO: handle exception
//			e.printStackTrace();
			System.out.println("查找数据库时有问题");
		}
		/**
		 * 当前数据库中还没有该事件，直接插入事件到数据库
		 */
		AddEventtoOracle(eventItem,  sentiment_text,  sentimentResultContent, relatedtime);
	}

	/**
	 * 将结果添加到oracle数据库中
	 * @param eventItem
	 * @param sentiment_text
	 * @param sentimentResultContent
	 * @param relatedtime
	 * @throws SQLException
	 */
	private void AddEventtoOracle(LabelItem eventItem, String sentiment_text, String sentimentResultContent,
			int relatedtime) throws SQLException {
		// TODO Auto-generated method stub
		/**
		 * 待插入数据的各个source、target值
		 */
		String event_id = eventItem.newsID;
		String news_url = eventItem.newsURL;
		String img_address = eventItem.imgAddress;
		String news_title = eventItem.newsTitle;
		String news_content = eventItem.newsContent;
		ActorProItem sourceActorItem = eventItem.sourceActorItem;
		ActorProItem targetActorItem = eventItem.targetActorItem;
		ActorProItem placectorItem = eventItem.placectorItem;

		String sourceRawSlr = sourceActorItem.locRawName.concat("_"+sourceActorItem.locforSolr);
		String targetRawSlr = targetActorItem.locRawName.concat("_"+targetActorItem.locforSolr);
		String placeRawSlr = placectorItem.locRawName.concat("_"+placectorItem.locforSolr);
		
		
		java.sql.PreparedStatement insertoracle = connectionOracle.prepareStatement(
				"INSERT INTO EVENTGKG VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
		//									  ?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?
		insertoracle.setString(1, event_id);// 新闻id
		insertoracle.setString(2, news_content);// 新闻事件句		
	
		insertoracle.setString(3, eventItem.sourceActor);// 发起者名称
		
		insertoracle.setString(4, sourceRawSlr);// 发起者名称.
		
		insertoracle.setString(5, sourceActorItem.sourceActor_countrycode);// 发起者国家代码
		insertoracle.setString(6, sourceActorItem.actorRole == null ? "" : sourceActorItem.actorRole);// 实体1的角色
		insertoracle.setString(7, eventItem.sourceActorPro);// 发起者类型
		insertoracle.setString(8, sourceActorItem.sourceActor_regionadm);// 发起者adm1码
		insertoracle.setDouble(9, sourceActorItem.sourceActor_latitude.equals("") ? 0.0
				: Double.valueOf(sourceActorItem.sourceActor_latitude));// 发起者维度
		insertoracle.setDouble(10, sourceActorItem.sourceActor_longitude.equals("") ? 0.0
				: Double.valueOf(sourceActorItem.sourceActor_longitude));// 发起者精度

		insertoracle.setString(11, eventItem.targetActor);// 承受者名字
		insertoracle.setString(12, targetRawSlr);// 承受者raw

		insertoracle.setString(13, targetActorItem.sourceActor_countrycode);// 承受者国家代码
		insertoracle.setString(14, targetActorItem.actorRole == null ? "" : targetActorItem.actorRole);// 实体2角色

		insertoracle.setString(15, eventItem.targetActorPro);// 承受者类型
		insertoracle.setString(16, targetActorItem.sourceActor_regionadm);// 承受者adm
		insertoracle.setDouble(17, targetActorItem.sourceActor_latitude.equals("") ? 0.0// 承受者纬度
				: Double.valueOf(targetActorItem.sourceActor_latitude));
		insertoracle.setDouble(18, targetActorItem.sourceActor_longitude.equals("") ? 0.0// 承受者经度
				: Double.valueOf(targetActorItem.sourceActor_longitude));

		insertoracle.setString(19, eventItem.triggerWord);// 触发词
		insertoracle.setString(20, eventItem.typeFour);// 四大类
		insertoracle.setString(21, String.valueOf(eventItem.eventType));// 20大类
		insertoracle.setDouble(22, eventItem.Proscore.equals("") ? -1 : Double.valueOf(eventItem.Proscore));// 事件性质得分
		insertoracle.setString(23, eventItem.eventTime);// 事件发生的时间
		insertoracle.setTimestamp(24, eventItem.saveTime);// 事件发生的时间戳

		insertoracle.setString(25, eventItem.eventLocation);// 地点全名
		insertoracle.setString(26, placeRawSlr);// 地点raw

		
		insertoracle.setString(27, placectorItem.sourceActor_countrycode);// 地点的国家编码
		insertoracle.setString(28, placectorItem.sourceActor_regionadm);// 地点的地区编码

		insertoracle.setDouble(29, placectorItem.sourceActor_latitude.equals("") ? 0.0
				: Double.valueOf(placectorItem.sourceActor_latitude));
		insertoracle.setDouble(30, placectorItem.sourceActor_longitude.equals("") ? 0.0
				: Double.valueOf(placectorItem.sourceActor_longitude));

		insertoracle.setString(31, eventItem.allPerson);// 事件中所有人
		insertoracle.setString(32, eventItem.actorItem.actor);// 事件中所有实体
		insertoracle.setString(33, sentiment_text);// 事件情感句

		insertoracle.setString(34, sentimentResultContent);// 事件情感评价

		insertoracle.setString(35, news_url);// url
		insertoracle.setString(36, news_title);// 标题
		insertoracle.setString(37, img_address);// 图片
		insertoracle.setInt(38, relatedtime);// 引用次数

		insertoracle.setInt(39, 0);// 事件是否参与打分
		insertoracle.setInt(40, 0);// 事件抽取结果得分
		insertoracle.setInt(41, 0);// 事件观点得分
		insertoracle.setInt(42, 0);// 实体正确数
		insertoracle.setInt(43, 0);// 编码得分
		insertoracle.setInt(44, 0);// 总实体数量
		// System.out.println((insertoracle).getQueryString());

		try {
			insertoracle.executeUpdate();
			System.out.println("添加成功");
			insertoracle.close();
		} catch (Exception e) {
			insertoracle.close();
			e.printStackTrace();
			// System.out.println("插入oracle时有问题");
		}
		
	}

	/**
	 * 将NULL转为""
	 * @param eventItem
	 */
	private void removeNull(LabelItem eventItem) {
		// TODO Auto-generated method stub
		eventItem.actorItem = eventItem.actorItem == null ? new ActorItem("", "", "", "") : eventItem.actorItem;
		eventItem.eventLocation = eventItem.eventLocation == null ? "" : eventItem.eventLocation;
		eventItem.eventLocationPro = eventItem.eventLocationPro == null ? "" : eventItem.eventLocationPro;
		eventItem.sourceActor = eventItem.sourceActor == null ? "" : eventItem.sourceActor;
		eventItem.sourceActorPro = eventItem.sourceActorPro == null ? "" : eventItem.sourceActorPro;
		eventItem.targetActor = eventItem.targetActor == null ? "" : eventItem.targetActor;
		eventItem.targetActorPro = eventItem.targetActorPro == null ? "" : eventItem.targetActorPro;
		eventItem.triggerWord = eventItem.triggerWord == null ? "" : eventItem.triggerWord;
		eventItem.eventTime = eventItem.eventTime == null ? "" : eventItem.eventTime;
		
		eventItem.newsContent = eventItem.newsContent == null ? "" : eventItem.newsContent;
		eventItem.newsURL = eventItem.newsURL == null ? "" : eventItem.newsURL;
		eventItem.imgAddress = eventItem.imgAddress == null ? "" : eventItem.imgAddress;
		eventItem.allPerson = eventItem.allPerson == null ? "" : eventItem.allPerson;	
	}

	




	

	public static void main(String[] args) {

		OracleUtil oracleUtil = new OracleUtil(new GeonamesUtil(),new CombineEvent());
		LabelItem labelItem = new LabelItem();
		labelItem.newsID = "12343512";
		labelItem.newsContent = "当前，俄中双边经贸关系发展势头良好，高新技术等领域合作取得积极成果";
		labelItem.newsTitle = "当前，俄中双边经贸关系发展势头良好，高新技术等领域合作取得积极成果";
		labelItem.sourceActor = "俄";
		labelItem.triggerWord = "合作";
		labelItem.targetActor = "中";
		labelItem.sourceActorPro = " country";
		labelItem.targetActorPro = "country";
		labelItem.eventTime = "当前";
//		labelItem.eventLocation = "";
		labelItem.actorItem = new ActorItem();
		
		try {
			oracleUtil.geonamesUtil.findAllProperties(labelItem);
			int i = 0;
//			while(i<1000){
				oracleUtil.AddLabeltoOracle(labelItem, "1", "1",1);
//				i++;
//			}
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
