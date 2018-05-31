/*  
*/
package cetc28.java.eventdetection.preprocessing;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;


public class Data implements Cloneable{
	
	public LabelItem data;//
	public Pair<List<Integer>, List<String>> depResult;//依存句法分析结果
	public ArrayList<Integer> verbIdList;//句子中的所有动词
	public List<String> words;//LTp 分词结果
	public List<String> tags;//LTp 词性标注结果
//	public ArrayList<String> wordList_stopWord;
	public String[] nerArrs;//实体识别结果
	public String HeadWord;//headword fROM 依存句法
	public int headWordId;//核心词下标
	public int triggerPos;//触发词下标
	public String PrimeSentence;

	public Data(LabelItem data)
	{
		this.data = data;
	}
	public Data() {
		// TODO Auto-generated constructor stub
	}
	/**
	 * 预处理（分词+词性标注+依存分析。。。）
	 */
	public void setTrainData()
	{
		words = LtpTool.getWords(data.newsTitle);
		tags = LtpTool.getPosTag(words);
		this.depResult = LtpTool.parse(words, tags);
		Pair<String, Integer> headwordInfo = LtpTool.getHeadWord(depResult.getFirst(), words);
		if(headwordInfo != null)
		{
			this.HeadWord = headwordInfo.getFirst();
			this.headWordId = headwordInfo.getSecond();
		}
		this.verbIdList = LtpTool.getVerbList(this.depResult.getFirst(),this.tags );//begin -銆� end
		String nerArr = Ner.ner3(data.newsTitle);
		if(nerArr == null || nerArr.trim().equals(""))return;
		this.nerArrs = nerArr.split("\\s+");
	}
	
	/**
	 * 加入停用词的预处理
	 * @param stopwords 停用词
	 */
	public void setTrainData(HashMap<String, Integer> stopwords)
	{
		words = LtpTool.getWords(data.newsTitle);
		tags = LtpTool.getPosTag(words);
		this.depResult = LtpTool.parse(words, tags);
		Pair<String, Integer> headwordInfo = LtpTool.getHeadWord(depResult.getFirst(), words);
		this.HeadWord = headwordInfo.getFirst();
		this.headWordId = headwordInfo.getSecond();
		this.verbIdList = LtpTool.getVerbList(this.depResult.getFirst(),this.tags );
		this.nerArrs = Ner.ner3(data.newsTitle).split("\\s+");
	}
	@Override
	public Object clone() throws CloneNotSupportedException {
		// TODO Auto-generated method stub
		return super.clone();
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String title = "欧洲忐忑看俄导弹压境";
		Data data = new Data(new LabelItem("", "", "", title));
		data.setTrainData();
		System.out.println("分词结果："+data.words);
		System.out.println("词性标注："+data.tags);
		System.out.println("依存句法分析结果："+data.depResult);
		System.out.println("核心词："+data.HeadWord);
		System.out.println("实体识别结果："+Arrays.asList(data.nerArrs));
	}
	public void Print() {
		// TODO Auto-generated method stub
		System.out.println("this.HeadWord:"+this.HeadWord);
		System.out.println("this.headWordId:"+this.headWordId);
		System.out.println("this.depResult:"+this.depResult);
		System.out.println("this.triggerPos:"+this.triggerPos);
		System.out.println("this.words:"+this.words);
		System.out.println("this.tags:"+this.tags);
		System.out.println("this.nerArrs:"+this.nerArrs);
		System.out.println("this.depResult:"+this.depResult);
		this.data.Print();

	}


}

