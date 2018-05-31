package cetc28.java.eventdetection.sentiment_extraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import cetc28.java.config.FileConfig;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;


import edu.hit.ir.ltp4j.Segmentor;
/**
 * 对情感句抽取
 * @author qianf
 *
 */
public class PolarityBasic {
	List<String>posList = new ArrayList();
	List<String>negList = new ArrayList();

	public PolarityBasic() {
		try {
			loadFile(posList,FileConfig.getPosPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			loadFile(negList,FileConfig.getNegPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	//从文件中加载数据到List
	private void loadFile(List<String> list, String filePath) throws IOException {
		// TODO Auto-generated method stub
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String s = null;
		while((s = br.readLine())!=null){
			list.add(s);
		}
	}

	/**
	 * 输入句子，判断句子的情感极性
	 * @param sentence
	 * @param words
	 * @return
	 */
	public String findsentiment(String sentence, List<String> words){		
		int posCount = countPolarity(sentence,this.posList);
		int negCount = countPolarity(sentence,this.negList);
		/**
		 * 负性词比正性词多或相等，且负性词至少为一个是，返回-1
		 */
		if(negCount - posCount >= 0 && negCount > 0 )return "-1";
		/**
		 * 正性词比负性词多，且正性词至少为一个是，返回-1
		 */
		else if(posCount - negCount > 0 && posCount > 0)return "1";
		else return "0";
	}
	
	/**
	 * 当前句子中，在指定情感列表中出现的词的个数
	 * @param sentence
	 * @param polarityList
	 * @return
	 */
	private int countPolarity(String sentence, List<String> polarityList) {
		// TODO Auto-generated method stub
		int count = 0;
		if(sentence == null || sentence.trim().equals(""))return count;
		for(String pl:polarityList){
			pl = pl.trim();
			if(sentence.indexOf(pl) > -1){
				count++;
				break;
			}
		}
		return count;
	}

	/**
	 * 利用情感词表求出句子的情感词
	 * @param extractResult
	 * @return
	 */
	public String findSentiment(LabelItem extractResult) {
		// TODO Auto-generated method stub
		if (extractResult == null)return "0";
		if (extractResult.newsContent == null || extractResult.newsContent.trim().equals(""))return "0";
		List<String> words = new ArrayList();
		Segmentor.segment(extractResult.newsContent.trim(), words);
		String sentiment = findsentiment(extractResult.newsContent.trim(), words);
		return sentiment;
	}
	
	public static void main(String[] args) {
		LtpTool ltpTool = new LtpTool();
		String sentence = "“我想过去几十年的事实已经说明，中美之间的贸易给双方人民都带来了好处，增加了就业，而不是相反。”";
		List<String> words = new ArrayList();
		Segmentor.segment(sentence, words);	
		System.out.println(words);
		PolarityBasic polarityBasic = new PolarityBasic();
		System.out.println(polarityBasic.findsentiment(sentence,words));
		System.out.println(polarityBasic.findsentiment("美国对英国表示满意", polarityBasic.posList));
	}
}
