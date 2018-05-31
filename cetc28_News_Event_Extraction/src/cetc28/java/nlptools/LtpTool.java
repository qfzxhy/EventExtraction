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
package cetc28.java.nlptools;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cetc28.java.config.FileConfig;
import edu.hit.ir.ltp4j.Postagger;
import edu.hit.ir.ltp4j.Parser;
import edu.hit.ir.ltp4j.Segmentor;
import edu.hit.ir.ltp4j.SRL;
import edu.hit.ir.ltp4j.NER;


public class LtpTool {
	static
	{
		String root = FileConfig.getLtpPath();
		System.load(root+"dll/segmentor.dll"); 
		System.load(root+"dll/postagger.dll"); 
		System.load(root+"dll/parser.dll");  
		System.load(root+"dll/srl.dll");  
		System.load(root+"dll/ner.dll");
		if (Parser.create(root+"ltp_data/parser.model") < 0||Segmentor.create(root+"ltp_data/cws.model",FileConfig.getUserDicPath()) < 0||
				Postagger.create(root+"ltp_data/pos.model") < 0
				||NER.create(root+"ltp_data/ner.model")<0
				||SRL.create(root+"ltp_data/srl/")<0)  
		{
			System.err.println("load failed");
		}
		else System.out.println("ltp tool load succeed");
//		String predictPath = "src/Java_News_Ner/predict";
//		String testPath = "src/Java_News_Ner/testCorpus";
//		if(Ner.create("src/Java_News_Ner/params",root+ "entitys","src/Java_News_Ner/trainCorpus")<0){System.err.println("error");}
	}
	public static Pair<List<Integer>, List<String>> parse(List<String> wordList,List<String> tagList)//依存句法分析
	{
		List<Integer> heads = new ArrayList<Integer>();
	    List<String> deprels = new ArrayList<String>();
	    Parser.parse(wordList, tagList, heads, deprels);//wordlist:分词列表     tagList:词性列表         heads: 依赖关系id    deprels:依赖关系
		return new Pair<List<Integer>, List<String>>(heads, deprels);
	}
	/*
	 * for location extraction       将命名实体识别结果合并后抽取里面的地点和国家作为location
	 */
	
	
	public static List<Pair<String,String>> posTagging(String[] nerArrs) {
		// TODO Auto-generated method stub
		
		List<Pair<String, String>> re = new ArrayList<Pair<String,String>>();
		if(nerArrs == null) return re;
		String[][] labels = getLabel(nerArrs);
		String entity =  labels[0][0],entityAttr =  labels[0][1];
		for(int i = 1;i<labels.length;i++)
		{
			if(labels[i][2].equals("other"))
			{
				re.add(new Pair<String, String>(entity, entityAttr));
				entity = labels[i][0];
				entityAttr = labels[i][1];
			}else
				if(labels[i][2].startsWith("b_"))
				{
					re.add(new Pair<String, String>(entity, entityAttr));
					entity = "";
					entityAttr = "";
					entity += labels[i][0];
					entityAttr = labels[i][2].substring(labels[i][2].indexOf("_")+1);
				}else
				{
					entity += labels[i][0];
				}
		}
		if(entity.length()>0)
			re.add(new Pair<String, String>(entity, entityAttr));
		return re;
	}
	private static String[][] getLabel(String[] words)
	{
		if(words == null){return null;}
		String[][] result = new String[words.length][3];
		for(int i = 0; i < words.length; i++)
		{
			result[i][0] = words[i].split("/")[0];
			result[i][1] = words[i].split("/")[1];
			result[i][2] = words[i].split("/")[2];
		}
		return result;
	}
	public static Pair<List<Integer>, List<String>> parse(String sentence)//依存句法分析，形参和上面不一样
	{
		List<String> words = new ArrayList<String>();
		List<String> postags = new ArrayList<String>();
		Segmentor.segment(sentence, words);
		Postagger.postag(words, postags);
		List<Integer> heads = new ArrayList<Integer>();
		List<String> deprels = new ArrayList<String>();
		Parser.parse(words, postags, heads, deprels);
		return new Pair<List<Integer>, List<String>>(heads, deprels);
	}
	public static List<String> getWords(String sentence)//分词
	{
		List<String> words = new ArrayList<String>();
		Segmentor.segment(sentence, words);
		return words;
	}
	public static List<String> getPosTag(String sentence)
	{
		List<String> words = new ArrayList<String>();
		Segmentor.segment(sentence, words);
		List<String> postags = new ArrayList<String>();
		Postagger.postag(words, postags);
		return postags;
	}
	public static List<String> getPosTag(List<String> words)//词性标住
	{
		List<String> postags = new ArrayList<String>();
		Postagger.postag(words, postags);
		return postags;
	}
	/*
	 * 得到依存句法核心词
	 */
	public static Pair<String, Integer> getHeadWord(List<Integer> heads,List<String> words)
	{
		int size = heads.size();
		for(int i = 0;i<size;i++)
		{
			if(heads.get(i) == 0)
			{
				return (new Pair<>(words.get(i), i));
			}
		}
		return null;
		
	}
//	public static String getParentRoutine(String word,List<String> deprels,List<String> words)//路径
//	{
//		for(int i = 0;i<words.size();i++)
//		{
//			if(word.equals(words.get(i)))
//			{
//				return deprels.get(i);
//			}
//		}
//		return null;
//	}
//	public static String getParentWord(String word,List<String> words,List<Integer> heads,List<String> deprels,List<String> tags)
//	{
//		int id = -1;
//		int blankId = -1;
//		for(int i = 0; i<words.size();i++)
//		{
//			if(word.equals(words.get(i)))
//			{
//				id = i;
//				//break;
//			}
//			if(word.equals(","))
//			{
//				blankId = i;
//			}
//		}
//		if(id>=0&&deprels.get(id).equals("VOB"))
//		{
//			int parentId = heads.get(id)-1;
//			if(parentId>=0)
//			{
//				if(Math.abs(parentId-id)!=1&&(words.get(parentId).equals("取消")||words.get(parentId).equals("拒绝")||words.get(parentId).equals("否认")))
//				{
//					if(blankId==-1||(blankId-parentId)*(blankId-id)>0)
//						return (words.get(id)+"_"+word);
//				}
//				else
//				if(Math.abs(parentId-id)==1&&(words.get(parentId).equals("取消")||words.get(parentId).equals("拒绝")||words.get(parentId).equals("否认")))
//					return (words.get(parentId)+word);
//			}
//		}
//		if(word.equals("否认")||word.equals("取消")||word.equals("拒绝"))
//		for(int i = 0 ;i<words.size();i++)
//		{
//			if(heads.get(i)==id+1&&deprels.get(i).equals("VOB")&&tags.get(i).equals("v"))
//			{
//				
//				if(Math.abs(i-id)!=1)
//					return (word+"_"+words.get(i));
//				else
//				{
//					
//					return (word+words.get(i));
//				}
//			}
//		}
//		return word;
//	}
	public static ArrayList<Integer> getVerbList(List<Integer> heads,List<String> posList)//主动词，(暂且是第一)从句动词
	{
		int size = heads.size();
		//ArrayList<String> verbList = new ArrayList<>();
		ArrayList<Integer> verbIdList = new ArrayList<>();
		int headWordId = 0;
		boolean flag = false;
		for(headWordId = 0;headWordId<size;headWordId++)
		{
			if(heads.get(headWordId) == 0)
			{
				verbIdList.add(headWordId);
				flag = true;
				break;
			}
		}
		if(verbIdList.size() == 0)
		{
			return verbIdList;
		}
		for(int i = 0;i<size;i++)
		{
			if(heads.get(i) == headWordId+1&&posList.get(i).matches("v"))
			{
				verbIdList.add(i);
			}
		}
		return verbIdList;
	}
	public static int getPath(int i,Pair<List<Integer>, List<String>> dep,List<String> wordList,List<String> tagList,
			ArrayList<String> wordPath,ArrayList<String> relationPath,ArrayList<String> lexicalPath)
	{
		
		List<Integer> heads = dep.getFirst();
		List<String> deprels = dep.getSecond();
		int id = 0;
		int id_ = 0;
		for(int j = 0;j<heads.size();j++)
		{
			if(j == i)
			{
				wordPath.add(wordList.get(i));
				lexicalPath.add(tagList.get(i));
				relationPath.add(deprels.get(i));
				id_ = id;
				id++;
			}
			if(heads.get(j) == i+1)
			{
				wordPath.add(wordList.get(j));
				lexicalPath.add(tagList.get(j));
				relationPath.add(deprels.get(j));
				id++;
			}
		}
		return id_;
	}
	public static boolean ActorBydeprel(int triggerpos, List<Integer> heads,
			List<String> deprels, List<String> newwords, List<String> newpostags) {
			String actor = "";
			String temp0 = "";
			String temp1 = "";
//			System.out.println("依存句法分析");
			if(triggerpos!=-1){
				for(int i=0;i<heads.size();i++){
					String word = newwords.get(i);
					int position = Integer.valueOf(heads.get(i));
					if(position== triggerpos&&deprels.get(i).equals("SBV")
							&&!temp0.contains(word)&&!temp1.contains(word)){
						temp0 = temp0.concat(word+"_");
					}else if(position == triggerpos&&(deprels.get(i).equals("VOB")||deprels.get(i).equals("POB")
							||deprels.get(i).equals("IOB")||deprels.get(i).equals("FOB")||deprels.get(i).equals("DBL"))
							&&!temp1.contains(word)&&!temp0.contains(word)){
						temp1 = temp1.concat(word+"_");
					}
				}
				//在触发词前面找间接主语
				if(temp0.equals("")){
					for(int i=0;i<triggerpos;i++){
						String word = newwords.get(i);
						//int position = Integer.valueOf(heads.get(i))-1;
						if(deprels.get(i).equals("SBV")&&isPath(i,triggerpos,heads,deprels)
								&&!temp0.contains(word)&&!temp1.contains(word)){
							temp0 = temp0.concat(word+"_");
						}
					}
				}
			}
			actor = temp0.endsWith("_")?actor = temp0.substring(0, temp0.length()-1):temp0;
			if(!actor.equals(""))return true;
			return false;
		}
		
		//判断两个节点之间是否有路径
		private static boolean isPath(int start, int end, List<Integer> heads,
				List<String> deprels) {
			// TODO Auto-generated method stub
			boolean flag = false;
			int startpoint = Integer.valueOf(heads.get(start))-1;
			if(startpoint == end){
				flag = true;
				return flag;
			}else if(startpoint != -1){
				try {
					flag = isPath(startpoint,end,heads,deprels);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
			return flag;
		}

	/**@daij
	 * 对于一个包含N个词的句子，句法分析返回的父节点范围在0至N之间，
	 * 而语义角色标注的输入需要在-1至N-1之间。因此，若要在句法分析
	 * 后进行语义角色标注，需要把heads作减一操作。
	 * 谨记在：句法分析后heads从1开始
	 * @param words
	 * @param postags
	 * @param ners
	 * @param heads
	 * @param deprels
	 * @return
	 */
	public static List<edu.hit.ir.ltp4j.Pair<Integer, List<edu.hit.ir.ltp4j.Pair<String, edu.hit.ir.ltp4j.Pair
	<Integer, Integer>>>>> SRL(
			List<String> words, List<String> postags, List<String> ners, List<Integer> heads, 
			List<String> deprels){
		    ArrayList<Integer> newheads = new ArrayList<Integer>();
//
//		  
//		  Segmentor.segment("美国攻打伊拉克", words);
//		  Postagger.postag(words, postags);
//		  NER.recognize(words, postags, ners);
//		  int size = Parser.parse(words,postags,heads,deprels);
		 
//		  
		  for(int i=0;i<heads.size();i++)newheads.add(heads.get(i)-1);
//		  System.out.println("words"+words);
//		  System.out.println("postags:"+postags);
//		  System.out.println("deprels:"+deprels);
//		  System.out.println("ners:"+ners);
//		  System.out.println("newheads:"+newheads);

		 //List<edu.hit.ir.ltp4j.Pair<Integer, List<edu.hit.ir.ltp4j.Pair<String, edu.hit.ir.ltp4j.Pair<Integer, Integer>>>>> srls;
		  List<edu.hit.ir.ltp4j.Pair<Integer, List<edu.hit.ir.ltp4j.Pair<String, edu.hit.ir.ltp4j.Pair<Integer, Integer>>>>> srls = new ArrayList();
		 
		 
		
		  SRL.srl(words, postags, ners, newheads,deprels,srls);
		  
		  //System.out.println("srls:"+srls);

//		  for (int i = 0; i < srls.size(); ++i) {
//		    System.out.println(srls.get(i).first + ":");
//		      for (int j = 0; j < srls.get(i).second.size(); ++j) {
//		        System.out.println("   tpye = "+ srls.get(i).second.get(j).first + " beg = "+ srls.get(i).second.get(j).second.first + " end = "+ srls.get(i).second.get(j).second.second);
//		      }
//		    }
//		  Segmentor.release();
//		  Postagger.release();
//		  NER.release();
//		  SRL.release();
		  return srls;
	}
	/**
	 * 输入words和pos 返回heads，deprels和srls
	 * @param newwords
	 * @param newpostags
	 * @param ners
	 * @param heads
	 * @param deprels
	 * @param srls
	 * @return
	 */
	public static List<edu.hit.ir.ltp4j.Pair<Integer, List<edu.hit.ir.ltp4j.Pair<String, edu.hit.ir.ltp4j.Pair<Integer, Integer>>>>> PreprocessParser(
			List<String> newwords, List<String> newpostags, List<String> ners, List<Integer> heads,
			List<String> deprels) {
		if (newwords.size() > 0)
			NER.recognize(newwords, newpostags, ners);
		else
			ners = new ArrayList();
		
		Parser.parse(newwords, newpostags, heads, deprels);
		
		return SRL(newwords, newpostags, ners, heads, deprels);
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		
		String textString = "中央军委联合参谋部副参谋长邵元明和俄罗斯武装力量副总参谋长兼总参作战总局总局长鲁茨科伊共同主持了磋商";
		
		String words = "将 坚定 落实 习近平 主席 与 普京 总统 的 重要 共识";
		List<String> wordList = new ArrayList<>();
		for(String word : words.split("\\s+"))
		{
			wordList.add(word);
		}
		List<String> tagList = LtpTool.getPosTag(wordList);
		Pair<List<Integer>, List<String>> depResult = LtpTool.parse(textString);
//		List<String> ners0 = new ArrayList();
//		List<Integer> heads0 = new ArrayList();
//		LtpTool.PreprocessParser(wordList, tagList, ners0, heads0, depResult.second);
		System.out.println("分词结果："+wordList);
		System.out.println("词性标注结果："+tagList);
		System.out.println("依存句法分析结果："+depResult);
		String res = "";
		for(int i = 0; i < wordList.size();i++)
		{
			res = res + wordList.get(i)+"/"+tagList.get(i)+"/"+depResult.getFirst().get(i)+"/"+depResult.getSecond().get(i)+" ";
		}
		System.out.println(res);
	}
	

}
