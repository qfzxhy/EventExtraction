package cetc28.java.eventdetection.time_location_extraction;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.pojava.datetime.DateTime;

import cetc28.java.eventdetection.argument_extraction.Methods;
import cetc28.java.eventdetection.argument_extraction.ReSegment;
import cetc28.java.eventdetection.entity_extraction.FindActorandPerson;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.news.label.ActorItem;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import edu.hit.ir.ltp4j.NER;
import edu.hit.ir.ltp4j.Parser;
import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.Segmentor;

public class TimeExtraction {
	Methods methods = new Methods();
	LtpTool ltpTool = new LtpTool();
	FindActorandPerson findActorbyDB;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public TimeExtraction(FindActorandPerson findActorbyDB) {
		this.findActorbyDB = findActorbyDB;
	}

	/**
	 * 利用依存句法从新闻文本中抽取时间
	 * @param saveTime 
	 */
	public void setTimebyrule(List<String> words, List<String> postags,LabelItem labelresult, String saveTime) throws SQLException {
		if(words == null || postags == null || words.size() == 0 || postags.size() == 0) return;
		
		String time = "";
		List<String> ners = new ArrayList();
		List<Integer> heads = new ArrayList();
		List<String> deprels = new ArrayList();
		List<edu.hit.ir.ltp4j.Pair<Integer, List<edu.hit.ir.ltp4j.Pair<String, edu.hit.ir.ltp4j.Pair<Integer, Integer>>>>> srls = new ArrayList();
		srls = ltpTool.PreprocessParser(words, postags, ners, heads, deprels);

		for (int i = 0; i < srls.size(); i++) {
			for (int j = 0; j < srls.get(i).second.size(); ++j) {

				String Aflag = srls.get(i).second.get(j).first;
				String act = methods.construct(words, srls.get(i).second.get(j).second.first,
						srls.get(i).second.get(j).second.second);
				 if (Aflag.equals("TMP"))
					time = (act);
			}
		}

		if( time != null){
			labelresult.eventTime = time.startsWith("_") ? time.substring(1) : time;
			labelresult.eventTime = methods.removeChinese(labelresult.eventTime);
		}
		if (labelresult.eventTime == null || labelresult.eventTime.trim().equals(""))
			labelresult.eventTime = saveTime;
	}
	
	/**
	 * 转换为时间戳
	 * @param eventTime
	 * @param saveTime
	 * @return
	 */
	public Timestamp String2Time(String eventTime, String saveTime) {
		Timestamp st;
		// TODO Auto-generated method stub
		// 字符串转为时间戳
		DateTime dt = null;
		// Timestamp st = null;
		try {
			if (eventTime.length() == 14)
				eventTime = eventTime.substring(0, 8);
			dt = new DateTime(eventTime);
			st = dt.toTimestamp();
		} catch (Exception e) {
			try {
				if (saveTime.length() == 14)
					saveTime = saveTime.substring(0, 8);
				dt = new DateTime(saveTime);
				st = dt.toTimestamp();
			} catch (Exception e1) {
				java.util.Date date = new java.util.Date();
				st = new java.sql.Timestamp(date.getTime());
			}
		}
		return st;
	}
}
