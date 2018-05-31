package cetc28.java.nlptools;


import java.util.Collection;
import java.util.List;
import java.io.StringReader;

import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.CoreAnnotations.ForcedSentenceEndAnnotation;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.util.IntervalTree.TreeNode;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
/**
 * 功能描述：利用NP+短语句法分析，补全句子中的发起者和承受者的字符串
 * @author qianf
 *
 */
public class Stanford_Parser {
	LexicalizedParser lp;
	public LexicalizedParser getLp() {
		return lp;
	}
	/**
	 * @param lp the lp to set
	 */
	public void setLp(LexicalizedParser lp) {
		this.lp = lp;
	}

/**
   * The main method demonstrates the easiest way to load a parser.
   * Simply call loadModel and specify the path of a serialized grammar
   * model, which can be a file, a resource on the classpath, or even a URL.
   * For example, this demonstrates loading a grammar from the models jar
   * file, which you therefore need to include on the classpath for ParserDemo
   * to work.
   *
   * Usage: {@code java ParserDemo [[model] textFile]}
   * e.g.: java ParserDemo edu/stanford/nlp/models/lexparser/chineseFactored.ser.gz data/chinese-onesent-utf8.txt
   *
   */
  public static void main(String[] args) {
//    String parserModel = "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz";
//    LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);
	  Stanford_Parser stanford_Parser = new Stanford_Parser();
	  String[] sent = { "这", "是", "一个", "简单的", "句子", "." };
	  stanford_Parser.NPcomplete(sent, 4);
	 // System.out.println
  }
  
  /**
   * 首先逆序找到node的最高层的祖先节点，祖先为NP
   * 遍历输出祖先的所有子树
   * @param root:句法树的根结点
   * @param position：核心词在句子总的位置，以0开始
   * @return 完整的句子
   */
  public  String complete(Tree root,int position) {	  
	  Tree node = root.getLeaves().get(position);
	  Tree ancestor = null;
	  for(int i=root.depth();i>=0;i--){
//		  System.out.println(i+":"+node.ancestor(i, root).label());
		  if(node.ancestor(i, root)!=null && node.ancestor(i, root).label().toString().trim().equals("NP")){
//			  System.out.println("找到NP");
			  ancestor = node.ancestor(i, root);
			  break;
		  }
	  }
	  String completevalue = "";
	  if(ancestor !=null && ancestor.getLeaves() != null ){
		  for(Tree s:ancestor.getLeaves()){
			  completevalue = completevalue.concat(s.label().toString().trim()+"_");
		  }
	  }
	  else 
		  completevalue = node.label().toString().trim();
//	  System.out.println(completevalue);
	  completevalue = completevalue == null?"":completevalue;
	  completevalue = completevalue.replaceAll("__", "_");
	  completevalue = completevalue.startsWith("_")?completevalue.substring(1, completevalue.length()):completevalue;
	  completevalue = completevalue.endsWith("_")?completevalue.substring(0, completevalue.length()-1):completevalue;
	 

	  return completevalue;
	  

  }
  /**
   * 利用NP+短语句法分析，补全句子中的实体
   * @param lp
   * @param sent：分词后的句子
   * @param position：核心词在句子中的位置
   */
  public String NPcomplete(String[]sent,int position) {
    // This option shows parsing a list of correctly tokenized words
    List<CoreLabel> rawWords = Sentence.toCoreLabelList(sent);
    Tree parse = this.getLp().apply(rawWords);
      return complete(parse,position);

  }

  public Stanford_Parser() {
	  String parserModel = "edu/stanford/nlp/models/lexparser/chinesePCFG.ser.gz";
	  LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);
	  this.setLp(lp);
  } // static methods only

}
