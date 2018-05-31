/*  
* 创建时间：2015年12月20日 下午1:01:27  
* 项目名称：Java_EventDetection_News  
* @author qianf  
* @version 1.0   
* @since JDK 1.8.0_21  
* 文件名称：GetLocation.java  
* 系统信息：Windows Server 2008
* 类说明：  
* 功能描述：
*/
package cetc28.java.eventdetection.time_location_extraction;

import java.util.List;

import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;


public class LocationExtraction {
	final static String[] locations = {"美","华","日","法","德","韩","英","京","沪","澳","俄","朝"};//有问题？？？？？
/**
 * 三种获得地名的方法，按照优先级method1>method2>method3>method4>method5
 * @param tagResult ltp.postagger返回结果
 * @param sources  发起者（实体+属性）
 * @param targets   承受者（实体+属性）
 * @return
 */
	public static Pair<String,String> getLocation(List<Pair<String,String>> tagResult,String locForSolr,String rawLoc)
	{
		Pair<String,String> location = null;
		if((location = getLocationInMethod2(tagResult)) == null &&(location = getLocationInMethod1(tagResult)) == null)
		{
			return new Pair<String, String>(locForSolr, rawLoc);
		}
		return new Pair<String, String>(location.getFirst(), location.getFirst());
	}

	/**
	 * p+{地名    或者    地名省略词}
	 * @param tagResult
	 * @return
	 */
	private static Pair<String, String> getLocationInMethod1(List<Pair<String,String>> tagResult)//p+地名
	{
		//String location = null;
		int tagId = 0;
		for(Pair<String,String> tagPair : tagResult)
		{
			String word = tagPair.getKey();
			String tag = tagPair.getValue();
			if(tag.equals("p") &&(
					word.equals("在") || 
					word.equals("从") || 
					word.equals("往") || 
					word.equals("到") || 
					word.equals("至")) )//介词
			{
				if(tagId+1>=tagResult.size()) break;
				Pair<String,String> nextTagPair = tagResult.get(tagId+1);
				if(nextTagPair.getValue().equals("ns") || nextTagPair.getValue().equals("country"))
				{
					word = nextTagPair.getKey();
					if(nextTagPair.getValue().equals("ns"))
						return new Pair<String, String>(word, "region");
					if(nextTagPair.getValue().equals("country"))
						return new Pair<String, String>(word, "country");
				}
			}
			tagId++;
		}
		return null;
	}
	/**
	 * 关键词+{地名    或者    地名省略词}
	 * @param tagResult
	 * @return
	 */
	private static Pair<String, String> getLocationInMethod2(List<Pair<String,String>> tagResult)//p+地名
	{
		int tagId = 0;
		for(Pair<String,String> tagPair : tagResult)
		{
			String word = tagPair.getKey();
			String tag = tagPair.getValue();
			if(tag.equals("v") &&(
					word.equals("出访") ||
					word.equals("到访") || 
					word.equals("访问") || 
					word.equals("去往") || 
					word.equals("来到") || 
					word.equals("抵达") ||
					word.equals("到达") || 
					word.equals("回到")) )//介词
			{
				if(tagId+1>=tagResult.size()) break;
				Pair<String,String> nextTagPair = tagResult.get(tagId+1);
				if(nextTagPair.getValue().equals("ns") || nextTagPair.getValue().equals("country"))
				{
					word = nextTagPair.getKey();
					if(nextTagPair.getValue().equals("ns"))
						return new Pair<String, String>(word, "region");
					if(nextTagPair.getValue().equals("country"))
						return new Pair<String, String>(word, "country");
				}
			}
			tagId++;
		}
		return null;
	}
	public static Pair<String, String> getLocation(List<Pair<String,String>> tagResult)
	{
		Pair<String, String> location = getLocationInMethod2(tagResult);
		if(location == null)
			location = getLocationInMethod1(tagResult);
		return location;
	}
	
	public static void main(String[] args) {
		String[] text = Ner.ner("我回到北京").split("\\s+");
		System.out.println(LocationExtraction.getLocation(LtpTool.posTagging(text)));
		
	}
}

