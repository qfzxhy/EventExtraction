/*  
* 创建时间：2016年4月1日 下午10:29:53  
* 项目名称：Java_EventDetection_News  
* @author GreatShang  
* @version 1.0   
* @since JDK 1.8.0_21  
* 文件名称：ltptool.java  
* 系统信息：Windows Server 2008
* 类说明：  
* 功能描述：
*/

import java.util.ArrayList;
import java.util.List;


import edu.hit.ir.ltp4j.Segmentor;



public class LtpTool {
	static
	{
		String root = "G:/MasterTwoU/28proj/data/data/";
//		System.load(root+"ltp/dll/segmentor.dll"); 
		if (Segmentor.create(root+"ltp/ltp_data/cws.model") < 0)  
		{
			System.err.println("load failed");
		}
		else System.out.println("ltp tool load succeed");
//		String predictPath = "src/Java_News_Ner/predict";
//		String testPath = "src/Java_News_Ner/testCorpus";
//		if(Ner.create("src/Java_News_Ner/params",root+ "entitys","src/Java_News_Ner/trainCorpus")<0){System.err.println("error");}
	}
	public static List<String> getWords(String sentence)//分词
	{
		
		List<String> words = new ArrayList<String>();
		Segmentor.segment(sentence, words);
		return words;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String textString = "大灯很亮而且非常帅气";
		List<String> wordList = LtpTool.getWords(textString);
		
		System.out.println("分词结果："+wordList);
		
	}
	

}
