package cetc28.java.eventdetection.argument_extraction;

import java.util.ArrayList;
import java.util.List;

import cetc28.java.nlptools.Pair;

/**
 * 从source和target中抽取实体，返回字符串实体和字符串属性
 * @author qianf
 *
 */
public class ArgumentExtraction {
	/**
	 * 查找句子指定位置之间的所有实体
	 * @param words 我/n/other 爱/v/other 中国/ns/b_country 
	 * @param begin source_begin：source短语的起始位置（以汉字为单位）
	 * @param end source_end：source短语的结束位置（以汉字为单位）
	 * @return entity：结果
	 */
	public List<Pair<String, String>> getEntity(String[] words, int begin, int end)
	{
		/*
		 * 该方法只考虑source或者target是连在一起词组成
		 */
		List<Pair<String, String>> list = new ArrayList<>();
		if(begin > end || begin < 0)
		{
			System.out.println("something is error in method getEntity of ArgumentExtraction0");
		}
		//找到对应词的id
		int wordBegin = -1, wordEnd = -1;
		String[][] wordLabels = getLabel(words);
		int curPos = 0;
		for(int i = 0; i < wordLabels.length; i++)
		{
			if(curPos == begin)
			{
				wordBegin = i;
			}
			curPos += wordLabels[i][0].length();
			if(curPos - 1 == end)
			{
				wordEnd = i;
				break;
			}
			
		}
		if(wordBegin == -1 || wordEnd == -1)
		{
			System.out.println("something is error in method getEntity of ArgumentExtraction1");
		}
		/*
		 * 情况1：从前往后找
		 */
		String entity = "";
		String entityAttribution = "";
		for(int i = wordBegin; i <= wordEnd; i++)
		{
			if(i == -1)break;
			if(wordLabels[i][2].startsWith("b"))
			{
				entity += wordLabels[i][0];
				entityAttribution = wordLabels[i][2].substring(2);
				int j = i+1;
				while(j < wordLabels.length && wordLabels[j][2].startsWith("i"))
				{
					entityAttribution = wordLabels[j][2].substring(2);
					entity += wordLabels[j++][0];
				}
				list.add(new Pair<String, String>(entity, entityAttribution));
				entity = "";
				entityAttribution = "";
				i = j -1;
			}
		}
		/*
		 * 情况2：如果从前往后找没找到，那么从后往前找
		 */
		if(list.size() == 0)
		{
			for(int i = wordEnd; i >= wordBegin; i--)
			{
				if(i == -1)break;
				if(wordLabels[i][2].startsWith("i"))
				{
					entityAttribution = wordLabels[i][2].substring(2);
					entity = wordLabels[i][0];
					int j = i - 1;
					while(j >= 0 && wordLabels[j][2].startsWith("i"))
					{
						entity = wordLabels[j--][0] + entity;
					}
					if(wordLabels[j][2].startsWith("b"))
					{
						entity = wordLabels[j][0] + entity;
					}
					list.add(new Pair<String, String>(entity, entityAttribution));
					entity = "";
					entityAttribution = "";
					i = j +1;
				}
			}
		}
		return list;
	}
	private String[][] getLabel(String[] words)
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
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		ArgumentExtraction ae = new ArgumentExtraction();
		String str = "美国/ns/b_country 袭击/v/other 伊拉克/ns/b_nt 海军/n/i_nt 基地/n/other";
		String[] words = str.split("\\s+");
		int begin = 0;
		int end =5 ;

		System.out.println(ae.getEntity(words, begin, end));;
	}

}