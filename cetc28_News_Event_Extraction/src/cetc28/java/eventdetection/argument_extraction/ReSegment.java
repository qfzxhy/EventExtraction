package cetc28.java.eventdetection.argument_extraction;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.PrimitiveIterator.OfDouble;
import java.util.regex.Pattern;
import cetc28.java.config.FileConfig;
import cetc28.java.dbtool.GeonamesUtil;
import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import edu.hit.ir.ltp4j.Parser;
import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.Segmentor;
/**
 * 提供和、与、遭的规则的支持词典
 * @author qianf
 */
public class ReSegment {
	GeonamesUtil geonamesUtil;
	private Methods methods;
	private ArrayList<String> T1paticular = new ArrayList<String>();
	private ArrayList<String> Ltpposvalue = new ArrayList<String>();
	private ArrayList<String> Pand = new ArrayList<String>();
	private ArrayList<String> Pdui = new ArrayList<String>();
	private ArrayList<String> Pbei = new ArrayList<String>();
	public ReSegment() {
		// TODO Auto-generated constructor stub
	}
	public ReSegment( Methods methods,GeonamesUtil geonamesUtil) {
		this.geonamesUtil = geonamesUtil;
		this.methods = methods;
		SetPword();
	}
	


	/**
	 * 判断word是否在wordlist中
	 * 
	 * @param word
	 * @param wordlist
	 * @return
	 */

	public boolean Settag(String word, ArrayList<String> wordlist) {
		boolean tag = false;
		for (int i = 0; i < wordlist.size(); i++) {
			if (wordlist.get(i).trim().equals(word.trim())) {
				tag = true;
				break;
			}
		}
		return tag;
	}

	public void SetPword() {
		String[] and = { "和", "与", "同", "及" };
		String[] dui = { "对", "向", "令", "使", "把", "从", "为", "拟对" };// ,"将"
		String[] bei = { "被", "遭", "受", "获", "引", "遭受","遭到","招致"};
		String[] t1 = { "说，", "宣布，", "回应，", "发表，", "指出，", "称，", "曝，", "表示，", "指出，", "发布，", "声称，", "说", "宣布，", "回应称", "发表称",
				"指出", "称", "曝", ":", "：", "表示", "指出", "发布", "声称" ,"说", "宣布", "指出", "称", "曝", ":", "：", "计划", "打算", "声称","报道","曝光","表示"
				,"称赞说","回答到"};
		Fill(t1, T1paticular);
		Fill(and, Pand);
		Fill(dui, Pdui);
		Fill(bei, Pbei);
	}

	/**
	 * 用words[]填充wordlist
	 * 
	 * @param words
	 * @param wordlist
	 */
	public void Fill(String[] words, ArrayList<String> wordlist) {
		for (int i = 0; i < words.length; i++) {
			wordlist.add(words[i]);
		}

	}

	/**
	 * 在wordlist中寻找word
	 * 
	 * @param word
	 * @param wordlist
	 * @return
	 */
	public int Findpos(String word, ArrayList<String> wordlist) {
		int pos = -1;
		for (int i = 0; i < wordlist.size(); i++) {
			if (wordlist.get(i).equals(word)) {
				pos = i;
				break;
			}
		}
		return pos;
	}

	/**
	 * List2ArrayList,将联合数组分别复制给两个单独的数组
	 * 
	 * @param seg_tag_before
	 * @param newseg
	 * @param newspos
	 */
	public void List2ArryList(List<Pair<String, String>> seg_tag_before, ArrayList<String> newseg,
			ArrayList<String> newspos) {
		for (int i = 0; i < seg_tag_before.size(); i++) {
			newspos.add(seg_tag_before.get(i).second);
			newseg.add(seg_tag_before.get(i).first);
		}
	}

	/**
	 * 动态链表复制给数组
	 * 
	 * @param seg_tag
	 * @param newseg
	 * @param newspos
	 */
	public void List2Arry(List<Pair<String, String>> seg_tag, String[] newseg, String[] newspos) {
		for (int i = 0; i < seg_tag.size(); i++) {
			newspos[i] = seg_tag.get(i).second;
			newseg[i] = seg_tag.get(i).first;
		}
	}

	/**
	 * 动态数组转为数组
	 * 
	 * @param newseg
	 * @param seg
	 */
	public void Arraylist2Array(ArrayList<String> newseg, String[] seg) {
		// TODO Auto-generated method stub
		for (int i = 0; i < newseg.size(); i++) {
			seg[i] = newseg.get(i);
		}
	}

	public ArrayList<String> getT1paticular() {
		return T1paticular;
	}

	public void setT1paticular(ArrayList<String> t1paticular) {
		T1paticular = t1paticular;
	}

	public ArrayList<String> getLtpposvalue() {
		return Ltpposvalue;
	}

	public void setLtpposvalue(ArrayList<String> ltpposvalue) {
		Ltpposvalue = ltpposvalue;
	}

	public ArrayList<String> getPand() {
		return Pand;
	}

	public void setPand(ArrayList<String> pand) {
		Pand = pand;
	}

	public ArrayList<String> getPdui() {
		return Pdui;
	}

	public void setPdui(ArrayList<String> pdui) {
		Pdui = pdui;
	}
	
	public ArrayList<String> getPbei() {
		return Pbei;
	}

	public void setPbei(ArrayList<String> pbei) {
		Pbei = pbei;
	}

	public static void main(String[] args) throws SQLException, IOException {
		LtpTool ltpTool = new LtpTool();
		ReSegment preprocess = new ReSegment( new Methods(), new GeonamesUtil());
		String predictPath = "src/cetc28/java/eventdetection/entity_extraction/predict";
		String testPath = FileConfig.getNerTestDataPath();
		String text = "日本将引进美国“全球鹰”无人机。";
		List<String> words = new ArrayList<String>();
		List<String> postags = new ArrayList<String>();
		List<String> newwords = new ArrayList<String>();
		List<String> newpostags = new ArrayList<String>();
		

	}
}
