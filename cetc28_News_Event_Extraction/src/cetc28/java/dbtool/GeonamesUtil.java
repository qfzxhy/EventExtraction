package cetc28.java.dbtool;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.solr.common.SolrDocument;


import cetc28.java.config.DBConfig;
import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.eventdetection.time_location_extraction.LocationExtraction;
import cetc28.java.news.label.ActorItem;
import cetc28.java.news.label.ActorProItem;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import cetc28.java.solrtool.GeoEncoder.Place;
import cetc28.java.solrtool.SolrGeo;
import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.Segmentor;

/**
 * 修改时间时间：2016年2月28日
 * 为每句话中抽取到的source target place抽取经纬度和角色
 * @author qianf
 * @version 1.0
 * @since JDK 1.6 文件名称：Actor_Db_Methods.java 系统信息：Win10 类说明： 功能描述：与GEONAMES的交互
 */

public class GeonamesUtil {
	public SolrGeo coordinateQuery = new SolrGeo();
	public static ArrayList<Pair<String, String>> ner_table = new ArrayList();

	public GeonamesUtil() {
	}

	/**
	 * 抽取source、target的经纬度、Role、adm码；抽取事件发生的地点及其经纬度、role、adm1码
	 * @param eventItem
	 * @throws Exception
	 */
	public void findAllProperties(LabelItem eventItem) throws Exception {
		String sourceActor = eventItem.sourceActor;
		String sourceActorPro = eventItem.sourceActorPro;
		String targetActor = eventItem.targetActor;
		String targetActorPro = eventItem.targetActorPro;
		String eventLocation = "";
		String eventLocationPro = "";
		/*
		 * 抽取全篇出现次数最多的地点经纬度
		 */
		ActorProItem tempPlaceItem = findallItem(eventItem.tempPlaceEntity, "region");
		/*
		 * 抽取source经纬度和角色
		 */
		eventItem.sourceActorItem = findallItem(sourceActor, sourceActorPro);
		//用全文地点经纬度填充
		if (eventItem.sourceActorItem.sourceActor_latitude == null
				|| eventItem.sourceActorItem.sourceActor_latitude.trim().equals("")) {
			eventItem.sourceActorItem = tempPlaceItem;
		}
		/**
		 * 找地点
		 */
//		String locForSolr = eventItem.sourceActorItem.locforSolr;//source地点
//		String locRawName = eventItem.sourceActorItem.locRawName;//source地点 souce可能用全篇地点的。。
		String locForSolr = tempPlaceItem.locforSolr;//全篇地点
		String locRawName = tempPlaceItem.locRawName;//全篇地点
		String[] nerArrs = Ner.ner(eventItem.newsContent).split("\\s+");
		/*
		 * 找地点 1：先找句子中地点（在+ns,抵达+地名） 找不到2：用全篇
		 */
		Pair<String, String> location = LocationExtraction.getLocation(LtpTool.posTagging(nerArrs), locForSolr, locRawName);//句子中的地点（用来solr查询的字符串）
		eventLocation = location.getFirst();
		eventItem.eventLocation = location.getFirst();//非常重要
		
		/*
		 * 抽取target经纬度和角色
		 */
		eventItem.targetActorItem = findallItem(targetActor, targetActorPro);
		/**
		 * 用全文出现最多的地点经纬度填充
		 */
		if (!(targetActor == null || targetActor.trim().equals(""))
				&& (eventItem.targetActorItem.sourceActor_latitude == null
						|| eventItem.targetActorItem.sourceActor_latitude.trim().equals("")))
			eventItem.targetActorItem = tempPlaceItem;
		
		/*
		 * 抽取句子地点的经纬度和角色
		 */
		eventItem.placectorItem = findallItem(eventLocation, "country");
		eventItem.placectorItem.locforSolr = location.getFirst();
		eventItem.placectorItem.locRawName = location.getSecond();
		eventItem.eventLocation = eventItem.placectorItem.completename;
		
//		 System.out.println(" source、target、place属性填充完");
//		 System.out.println("地点填充结果："+eventItem.eventLocation);
//
//		 System.out.println("source、target、place属性填充完");

		 
		eventItem.typeFour = findTypePro(eventItem.eventType);// 四大类性质得分
		eventItem.Proscore = findProScore(eventItem.eventType);// 事件性质得分
	}
	
	/*
	 * 根据实体名和实体属性，返回实体的GEONAMES查找 
	 */
	/**
	 * 
	 * @param actors 欧洲_反对派
	 * @param actorPros org_role
	 * @return 
	 * @throws Exception
	 */
	ActorProItem findallItem(String actors, String actorPros) throws Exception {
		// TODO Auto-generated method stub
		ActorProItem actorProItem = new ActorProItem();
		// System.out.println(actors+";"+actorPros);
		if (actors != null && actorPros != null) {
			String actor[] = actors.split("_");
			String actorPro[] = actorPros.split("_");

			if (actor.length != actorPro.length) {
				actorProItem.removeNull(actorProItem);
				return actorProItem;
			}
			

			// 找solrgeo里面有的属性
			for (int i = 0; i < actor.length; i++) {
//				System.out.println("寻找Role：" + actor[i]);
				String name = actor[i];
				String nameAttri = actorPro[i];
				if (name != null && name.trim().length() != 0) {
					actorProItem = findPro(name, nameAttri);
					if (actorProItem.completename.trim().length() != 0
							|| actorProItem.sourceActor_adm1.trim().length() != 0
							|| actorProItem.sourceActor_latitude.trim().length() != 0) {
						break;
					}
				}
			}
			// 找actor的角色
			for (int i = 0; i < actor.length; i++) {
				String actorRole = this.coordinateQuery.partCountryTable.getRole(actor[i]);
				if (actorRole == null) {
					if (!actorPro[i].equals("person")) {
						actorRole = this.coordinateQuery.abbreviationTable.getRole(actor[i]);
					}
				}
				if (actorRole != null) {
					actorProItem.actorRole = actorRole;
					break;
				}
			}
			//找actor中role的属性
			for (int i = 0; i < actor.length; i++) {
				if(actorPro[i].equals("role"))
				{
					ActorProItem temp = findPro(actor[i], actorPro[i]);
					if(temp.completename.trim().length() != 0
							|| temp.sourceActor_adm1.trim().length() != 0
							|| temp.sourceActor_latitude.trim().length() != 0)
					{
						actorProItem = temp;
					}	
					String actorRole = this.coordinateQuery.partCountryTable.getRole(actor[i]);
					if (actorRole != null && actorRole.trim().length() > 0) {
						actorProItem.actorRole = actorRole;
						break;
					}
				}
			}
		}

		// System.out.println("寻找编码结束");
		actorProItem.removeNull(actorProItem);
		// 国家编码和国家adm码一致

		return actorProItem;
	}
	/**
	 * 查找国家的经纬度
	 * @param actor
	 *            实体string
	 * @param actorPro
	 *            实体属性
	 * @return 经纬度、国家admin1、countrycode、role
	 * @throws Exception
	 */
	public ActorProItem findPro(String actor, String actorPro) throws Exception {
		// TODO Auto-generated method stub
		// System.out.println("进入findPro" + actor + ";" + actorPro);
		ActorProItem actorProItem = new ActorProItem();
		// if(! "country_region".contains(actorPro))return actorProItem;
		coordinateQuery = new SolrGeo();
		Pair<String, String> altenatenames = new Pair<String, String>("", "");//first : solr second : raw
		Place result = coordinateQuery.getCoordinate(actor, actorPro, altenatenames);// 得到经纬度
		// System.out.println("result:"+result);
		if (result != null) {
			// System.out.println(result);
			actorProItem.locforSolr = altenatenames.getFirst();
			actorProItem.locRawName = altenatenames.getSecond();
			actorProItem.sourceActor_countrycode = (String) result.getCountrycode();// 国家码
			actorProItem.sourceActor_countryadm = ((String) result.getCountrycode()
					.concat(((String) result.getAdmin1code())));// 国家adm1码
			actorProItem.sourceActor_regionadm = actorProItem.sourceActor_countrycode
					.concat((String) result.getAdmin1code());// adm1
			actorProItem.sourceActor_longitude = (String) result.getLongitude();
			actorProItem.sourceActor_latitude = (String) result.getLatitude();
			if (actorPro.equals("country")) {
				actorProItem.completename = findCompletename((ArrayList<String>) result.getAlternate_names()).equals("")
						? actor : findCompletename((ArrayList<String>) result.getAlternate_names());
				actorProItem.actorRole = (actorProItem.actorRole == null) || actorProItem.actorRole.trim().equals("")
						? "政府" : actorProItem.actorRole;
			} else
				actorProItem.completename = actor;
			// actorProItem.print();
		}
		coordinateQuery.close();
		// actorProItem.print();
		return actorProItem;
	}

	/**
	 * 利用GEONAMES返回地点的最大值
	 * @param alterNames
	 * @return
	 */
	public String findCompletename(ArrayList<String> alterNames) {
		// TODO Auto-generated method stub
		String completename = "";
		if (alterNames != null && alterNames.size() > 0) {
			ArrayList<String> locatename = new ArrayList<String>();
			for (String name : alterNames) {
				// System.out.println("name:"+name);
				if (ifChinese(name)) {
					locatename.add(name);
				}
			}
			for (String word : locatename) {
				if (word.length() >= completename.length())
					completename = word;
			}
		}
		return completename;
	}

	
	
	/*
	 * 根据事件类别返回事件四大类划分
	 */
	private String findTypePro(int eventType) {
		if (eventType >= 1 && eventType <= 5)
			return "1";
		else if (eventType >= 6 && eventType <= 9)
			return "2";
		else if (eventType >= 10 && eventType <= 14)
			return "3";
		else
			return "4";
	}

	// 返回事件性质得分
	private String findProScore(int eventType) {
		// TODO Auto-generated method stub
		String set[] = { "0", "3", "4", "1", "3.5", "6", "7", "5", "-2", "-5", "-2", "-4", "-6", "-6.5", "-7.2", "-4",
				"-7", "-9", "-10", "-10" };
		if (eventType - 1 >= 0 && eventType - 1 < set.length)
			return set[eventType - 1];
		else
			return "-1";
	}
	
	public boolean ifChinese(String word) {
		if (word.matches("[\u4E00-\u9FA5]*"))
			return true;
		else
			return false;
	}
	public static void main(String[] args) throws Exception {
		// 例子
		java.util.Date date = new java.util.Date();
		java.sql.Timestamp timeOracle = new java.sql.Timestamp(date.getTime());
		java.util.Date date1 = new java.util.Date();
		java.sql.Timestamp eventItem = new java.sql.Timestamp(date.getTime());
		System.out.println(timeOracle.getYear() == eventItem.getYear() && timeOracle.getMonth() == eventItem.getMonth()
				&& timeOracle.getDay() == eventItem.getDay());
		
		GeonamesUtil adm = new GeonamesUtil();
		
		try {
			// String actor = "美媒";
			ActorProItem ActorItem = adm.findallItem("美国_平民", "country_role");
			// adm.findallItem("美军基地", "country").print();
			ActorItem.print();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
}
