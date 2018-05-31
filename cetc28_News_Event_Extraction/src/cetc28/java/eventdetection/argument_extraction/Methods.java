package cetc28.java.eventdetection.argument_extraction;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pojava.datetime.DateTime;

import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.news.label.ActorItem;
import cetc28.java.news.label.EventActorItem;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.Pair;
/**
 * 提供一些对字符串的基本的操作方法
 * @author qianf
 *
 */
public class Methods {
	
	// 是否为中文标点
	public boolean isChinesePunctuation(char c) {
		// /"[\\pP‘’“”]"
		// if(s.length()!=1) return false;
		// char c = s.charAt(0);
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS || ub == Character.UnicodeBlock.VERTICAL_FORMS
				|| ub == Character.UnicodeBlock.ARABIC) {
			return true;
		} else if (ub.equals(" ")) {
			return true;
		} else {
			return false;
		}
	}

	public void String2date(String date) {
		DateTime dt = null;
		try {
			dt = new DateTime(date);
		} catch (Exception e) {
			System.out.println("报错");
		}
//		System.out.println("st:" + st);
	}

	/**
	 * 去掉（）
	 * @param newsInput
	 * @return
	 */
	public String PreInputTrim(String newsInput) {
		// TODO Auto-generated method stub
		String sentence = newsInput.trim();
		if (sentence.endsWith(" 　"))
			sentence = sentence.substring(0, sentence.length() - " 　".length());
		Pattern p = Pattern.compile("(.*)(（.*）)(.*)");
		Matcher m = p.matcher(sentence);
		if (m.find()) {
			sentence = sentence.replaceAll(m.group(2), "");
//			sentence = PreInputTrim(sentence);
		}
		return sentence;
	}

	/**
	 * 去掉【】
	 * @param newsInput
	 * @return
	 */
	public String PreInputTrim1(String newsInput) {
		// TODO Auto-generated method stub
		String sentence = newsInput.trim();
		if (sentence.endsWith(" 　"))
			sentence = sentence.substring(0, sentence.length() - " 　".length());
		Pattern p = Pattern.compile("(.*)(【.*】)(.*)");
		Matcher m = p.matcher(sentence);
		if (m.find()) {
			sentence = sentence.replaceAll(m.group(2), "");
//			sentence = PreInputTrim1(sentence);
		}
		return sentence;
	}
	/**
	 * 去掉()
	 * @param newsInput
	 * @return
	 */
	public String PreInputTrim2(String newsInput) {
		// TODO Auto-generated method stub
		String sentence = newsInput.trim();
		if (sentence.endsWith(" 　"))
			sentence = sentence.substring(0, sentence.length() - " 　".length());
		Pattern p = Pattern.compile("(.*)(\\(.*\\))(.*)");
		Matcher m = p.matcher(sentence);
		if (m.find()) {
			sentence = sentence.replace(m.group(2), "");
//			sentence = PreInputTrim2(sentence);
		}
		return sentence;
	}

	/**
	 * 返回字符在句子中的位置（偏移量）,从-1开始
	 * @param words0
	 * @param i
	 * @return
	 */
	public int ComputeSize(List<String> words0, int size) {
		// TODO Auto-generated method stub
		// System.out.println("words0:"+words0);
		// System.out.println("size:"+size);

		int pos = -1;
		for (int i = 0; i < size; i++) {
			// System.out.println(words0.get(i).length());
			pos += words0.get(i).length();
		}
		return pos;
	}
	

	/**
	 * 返回string在words中的索引
	 * 
	 * @param ner_pos
	 * @param string
	 * @return
	 */
	int IndexARR(List<String> words, String string) {
		// TODO Auto-generated method stub
		
		int index = -1;
		for (int i = 0; i < words.size(); i++) {
			if (string.equals((words.get(i)))) {
				index = i;
				break;
			}
		}
		return index;
	}

	/**
	 * 返回指定起始和结束位置的words串
	 * 
	 * @param words
	 * @param first
	 * @param second
	 * @return
	 */
	public String construct(List<String> words, int begin, int end) {
		// TODO Auto-generated method stub
		String word = "";
		for (int i = begin; i <= end; i++)
			word = word.concat(words.get(i));
		return word;
	}


	/*
	 * 用arr2填充arr1,原则是source和target不相同
	 */
	/**
	 * 
	 * @param arr1 依存句法分析抽取结果（source target trigger）
	 * @param arr2 语义角色标注抽取结果（source target trigger）
	 * @return best result(选择不空的)
	 */
	String[] Combine(String[] arr1, String[] arr2) {
		if ((arr1[0].equals("") && (arr1[1].equals("")) || 
				(!arr1[1].equals("") && !arr2[0].contains(arr1[1])))&& (arr2[0].equals("") ||
				(!arr2[0].equals("") && !"_".concat(arr1[1]).concat("_").contains(arr2[0]))))
			arr1[0] = arr2[0];
		
		if (arr1[1].equals("") && (arr1[0].equals("") || 
				(!arr1[0].equals("") && !arr2[1].contains(arr1[0])))&& (arr2[1].equals("") || 
				(!arr2[1].equals("") && !"_".concat(arr1[0]).concat("_").contains(arr2[1]))))
			arr1[1] = arr2[1];
		return arr1;
	}

	/**
	 * 是否全部非空，全部非空则为true
	 * 
	 * @param actor_trigger_Srl （source target trigger）
	 * @return
	 */
	boolean IsFull(String[] actor_trigger_Srl) {
		// TODO Auto-generated method stub
		boolean flag = true;
		for (int i = 0; i < 2; i++) {
			if (actor_trigger_Srl[i].equals("")) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	// 是否为中文标点
	public int firstChinesePunctuationPostion(String sentence) {
		// if(s.length()!=1) return false;
		// char c = s.charAt(0);
		char chars[] = sentence.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (isChinesePunctuation(chars[i]))
				return i;
		}
		return -1;
	}

	public int lastChinesePunctuationPostion(String sentence) {
		// TODO Auto-generated method stub
		// char c = s.charAt(0);
		char chars[] = sentence.toCharArray();
		for (int i = chars.length - 1; i >= 0; i--) {
			if (isChinesePunctuation(chars[i]))
				return i;
		}
		return -1;
	}

	/**
	 * target的实体抽取使用，从前往后，找到比如"，"等标点，找半句话
	 * @param sentence
	 * @return
	 */
	public int firstChinesePunctuationPostionExcept(String sentence) {
		// TODO Auto-generated method stub
		// if(s.length()!=1) return false;
		// char c = s.charAt(0);
		char chars[] = sentence.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (isChinesePunctuation(chars[i]) || chars[i] == '“' || chars[i] == '”' || chars[i] == '-'
					|| chars[i] == '”' || chars[i] == '、' || chars[i] == '\"' || chars[i] == '《' || chars[i] == '》'
							|| chars[i] == '（' || chars[i] == '）' || chars[i] == '(' || chars[i] == ')' || chars[i] == '―'
							|| chars[i] == '—' || chars[i] == ' ')
				return i;
		}
		return -1;
	}

	/**
	 * 	source的实体抽取使用，从后往前，找到比如"，"等标点，找半句话
	 * @param sentence
	 * @return
	 */
	public int lastChinesePunctuationPostionExcept(String sentence) {
		// TODO Auto-generated method stub
		// 不算“”
		char chars[] = sentence.toCharArray();
		for (int i = chars.length - 1; i >= 0; i--) {
			// System.out.println(chars[i]);
			if (isChinesePunctuation(chars[i]) || chars[i] == '“' || chars[i] == '”' || chars[i] == '-'
					|| chars[i] == '”' || chars[i] == '、' || chars[i] == '\"' || chars[i] == '《' || chars[i] == '》'
							|| chars[i] == '（' || chars[i] == '）' || chars[i] == '(' || chars[i] == ')' || chars[i] == '―'
							|| chars[i] == '—' || chars[i] == ' ')
				return i;
		}
		return -1;
	}

	/**
	 * 去掉所有的中文标点符号
	 * 针对第1类，公开声明，去掉target中前后的标点符号
	 * @param sentence
	 * @return
	 */
	public String removeChinese(String sentence) {
		// TODO Auto-generated method stub
		// 去掉句子前后的所有中文符号
		if (sentence == null) return "";
		sentence = sentence.trim();
		if (sentence.equals("")) return "";
		if (isNumber(sentence)) return sentence;

		while (!(sentence == null || (sentence = sentence.trim()).equals(""))
				&& (isChinesePunctuation(sentence.charAt(0)) || isPunctuation(sentence.charAt(0)))) {
			sentence = sentence.substring(1, sentence.length()).trim();
		}
		if (sentence == null)
			return "";
		sentence = sentence.trim();
		if (sentence.equals(""))
			return "";
		if (isNumber(sentence)) return sentence;
		while (!(sentence == null || (sentence = sentence.trim()).equals(""))
				&& (isChinesePunctuation(sentence.charAt(sentence.length() - 1))
						|| isPunctuation(sentence.charAt(sentence.length() - 1))
						|| isSymbol(sentence.charAt(sentence.length() - 1))))
			sentence = sentence.substring(0, sentence.length() - 1).trim();
		return sentence;
	}

	private boolean isNumber(String sentence) {
		// TODO Auto-generated method stub
		char cs[] = sentence.toCharArray();
		for (char c : cs) {
			if (c < '0' || c > '9')
				return false;
		}
		return true;
	}

	

	boolean isSymbol(char ch) {
		// 是否为数字
		if ('0' <= ch && ch <= '9')
			return true;
		if (0x2010 <= ch && ch <= 0x2017)
			return true;
		if (ch == '-')
			return true;
		if (0x2020 <= ch && ch <= 0x2027)
			return true;
		if (0x2B00 <= ch && ch <= 0x2BFF)
			return true;
		if (0xFF03 <= ch && ch <= 0xFF06)
			return true;
		if (0xFF08 <= ch && ch <= 0xFF0B)
			return true;
		if (ch == 0xFF0D || ch == 0xFF0F)
			return true;
		if (0xFF1C <= ch && ch <= 0xFF1E)
			return true;
		if (ch == 0xFF20 || ch == 0xFF65)
			return true;
		if (0xFF3B <= ch && ch <= 0xFF40)
			return true;
		if (0xFF5B <= ch && ch <= 0xFF60)
			return true;
		if (ch == 0xFF62 || ch == 0xFF63)
			return true;
		if (ch == 0x0032 || ch == 0x3000)
			return true;
		return false;

	}

	boolean isCnSymbol(char ch) {
		if (0x3004 <= ch && ch <= 0x301C)
			return true;
		if (0x3020 <= ch && ch <= 0x303F)
			return true;
		return false;
	}

	boolean isEnSymbol(char ch) {

		if (ch == 0x40)
			return true;
		if (ch == 0x2D || ch == 0x2F)
			return true;
		if (0x23 <= ch && ch <= 0x26)
			return true;
		if (0x28 <= ch && ch <= 0x2B)
			return true;
		if (0x3C <= ch && ch <= 0x3E)
			return true;
		if (0x5B <= ch && ch <= 0x60)
			return true;
		if (0x7B <= ch && ch <= 0x7E)
			return true;

		return false;
	}

	boolean isEnPunc(char ch) {
		if (0x21 <= ch && ch <= 0x22)
			return true;
		if (ch == 0x27 || ch == 0x2C)
			return true;
		if (ch == 0x2E || ch == 0x3A)
			return true;
		if (ch == 0x3B || ch == 0x3F)
			return true;

		return false;
	}

	boolean isCjkPunc(char ch) {
		if (0x3001 <= ch && ch <= 0x3003)
			return true;
		if (0x301D <= ch && ch <= 0x301F)
			return true;

		return false;
	}

	boolean isPunctuation(char ch) {
		if (isCjkPunc(ch))
			return true;
		if (isEnPunc(ch))
			return true;
		if (0x2018 <= ch && ch <= 0x201F)
			return true;
		if (ch == 0xFF01 || ch == 0xFF02)
			return true;
		if (ch == 0xFF07 || ch == 0xFF0C)
			return true;
		if (ch == 0xFF1A || ch == 0xFF1B)
			return true;
		if (ch == 0xFF1F || ch == 0xFF61)
			return true;
		if (ch == 0xFF0E)
			return true;
		if (ch == 0xFF65)
			return true;
		if (ch == '(' || ch == ')' || ch == '{' || ch == '}' || ch == '[' || ch == ']')
			return true;
		return false;
	}

	/**
	 * 为了判断是否相同加以合并，于是去空
	 */
	public void RemoveNull(Data dataResult) {
		// TODO Auto-generated method stub
		if (dataResult == null)
			return;
		LabelItem second;
		
		if ((second = dataResult.data) != null) {
			second.actorItem = second.actorItem == null ? new ActorItem("", "", "", "") : second.actorItem;
			second.eventLocation = second.eventLocation == null ? "" : second.eventLocation;
			second.eventLocationPro = second.eventLocationPro == null ? "" : second.eventLocationPro;
			second.sourceActor = second.sourceActor == null ? "" : second.sourceActor;
			second.sourceActorPro = second.sourceActorPro == null ? "" : second.sourceActorPro;
			second.targetActor = second.targetActor == null ? "" : second.targetActor;
			second.targetActorPro = second.targetActorPro == null ? "" : second.targetActorPro;
			second.triggerWord = second.triggerWord == null ? "" : second.triggerWord;
			second.eventTime = second.eventTime == null ? "" : second.eventTime;
		}
	}

	/**
	 * 去掉多个实体合并后多余的下划线
	 * @param finalactor
	 */
	public void removeLine(String[] finalactor) {
		// TODO Auto-generated method stub
		for (int i = 0; i < finalactor.length; i++) {
			if (finalactor[i].startsWith("_"))
				finalactor[i] = finalactor[i].substring(1, finalactor[i].length());
			else if (finalactor[i].endsWith("_"))
				finalactor[i] = finalactor[i].substring(0, finalactor[i].length() - 1);
			else if (finalactor[i].startsWith("_"))
				finalactor[i] = finalactor[i].substring(1, finalactor[i].length());
			else
				finalactor[i] = finalactor[i];
		}
	}

	/**
	 * 暂时给target赋属性值
	 * @param actor
	 * @return
	 */
	public String findPro(String actor) {
		// TODO Auto-generated method stub
		String pro = "";
		if (actor == null || actor.trim().equals(""))
			return pro;
		int len = actor.split("_").length;
		for (int i = 0; i < len; i++) {
			pro = pro.concat("other_");
		}
		pro = pro.endsWith("_") ? pro.substring(0, pro.length() - 1) : pro;
		return pro;
	}


	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Methods methods = new Methods();
		// System.out.println("小明".split("_").length);
		String title = "（外媒称印巴边境冲突加剧：7名巴士兵遭炮击死亡）（10）";
		title = methods.PreInputTrim(title);
//		title = methods.PreInputTrim2(title);
//		title = methods.PreInputTrim(title);
		System.out.println(title);
	}

}
