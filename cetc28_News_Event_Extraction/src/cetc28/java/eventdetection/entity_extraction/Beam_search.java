package cetc28.java.eventdetection.entity_extraction;


import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




public class Beam_search 
{
	public static boolean flag = false;
	
	public static Entity deviceEntity;
	public static Entity roleEntity;
	public static Entity regionEntity;
	public static Entity orgEntity;
	public static Entity personEntity;
	public static Entity countryEntity;
	
	public Beam_search() {
		// TODO Auto-generated constructor stub
		if(!flag)
		{
			deviceEntity = new Entity("dev");
			regionEntity = new Entity("ns");
			roleEntity = new Entity("role");
			orgEntity = new Entity("nt");
			personEntity = new Entity("nr");
			countryEntity = new Entity("country");
			flag = true;
		}
	}



	/**
	 * 运行beam search
	 * @param wordListence 原始句子
	 * @param beam_size beam大小
	 * @param params double数组中[0：参数总和，1：上次更新的时间，2：参数的值]
	 * @param actions 由Term的label生成 ???
	 * @param now 第i次迭代的第j个句子序号
	 * @param mode 0表示训练，1表示测试
	 * @param featureTime 
	 * @return 
	 */
	List<Term> runBeamSearch(String sentence, int beamSize, Map<String,double[]> params, double now, int mode)
	{
		
		String[] Label = {"other", "b_nt", "i_nt", "b_role", "i_role", "b_country", "i_country", "b_ns", "i_ns", "b_nr", "i_nr", "b_dev", "i_dev"};
		Util u = new Util();
		List<String[]> wordList = u.genWordPosLabel(sentence, mode);

		//beam search
		ArrayList<Term> agenda = new ArrayList<Term>();
		ArrayList<Term> tempAgenda = new ArrayList<Term>();
		Term first = new Term(0.0, -1, null, null);
		tempAgenda.add(first);
			for(int i = 0; i < wordList.size(); ++i)//wordList....//找max路径
			{
			agenda.clear();
			for(Term term : tempAgenda)
			{
				
				String curTermLabel = term.label;
				for(String label : Label)
				{
					if(label.charAt(0) == 'i' && curTermLabel != null)
					{
						if(curTermLabel.equals("other") || !curTermLabel.substring(curTermLabel.indexOf("_")+1).equals(label.substring(label.indexOf("_")+1)))
						{
							continue;
						}
					}
					if(curTermLabel == null && label.charAt(0) == 'i')
					{
						continue;
					}
					double score = computScore(wordList, i, new Term(0.0, i, label, term), params, mode);//序列得分
					updateAgenda(agenda, new Term(term.score + score, i, label, term), beamSize);//更新权重
				}
			}
			tempAgenda.clear();
			tempAgenda.addAll(agenda);
			}
		//}
		List<Term> predictTerm = new ArrayList<>();
		if(mode == 0)
		{
			List<Term> correctTerm = u.genTerm(wordList);
			String[] correctLabels = extractLabels(correctTerm.get(correctTerm.size() - 1));
			
			Term best = agenda.get(agenda.size() - 1);
			predictTerm = best.backTrace(best);
			assert(predictTerm.size()==correctTerm.size());
			String[] predictLabels = extractLabels(agenda.get(agenda.size() - 1));

			for(int i = 0; i < wordList.size(); ++i)
			{
				List<String> correctFeatures = extractFeature(wordList, correctLabels, i);
				List<String> predictFeatures = extractFeature(wordList, predictLabels, i);
				double correctScale = 1.0;
				double predictScale = -1.0;

				lazy_update(params, correctFeatures, now, correctScale);
				lazy_update(params, predictFeatures, now, predictScale);
			}
		}
		else
		{
			Term best = agenda.get(agenda.size() - 1);
			predictTerm = best.backTrace(best);
		}
		
		return predictTerm;
	}
	


	/**
	 * 根据term的标签生成action，从后往前生成
	 * @param term 大小为sen.size
	 */
	String[] extractLabels(Term t)
	{		List<Term> term = t.backTrace(t);
		String[] labels = new String[term.size()];
		for(int i = 0; i < term.size(); ++i)
		{
			labels[i] = term.get(i).label;
		}
		return labels;
	}
	
	/**
	 * 更新beam search中的项，保证其中个数最大为beam_size
	 * @param agenda
	 * @param term
	 * @param beam_size
	 */
	void updateAgenda(List<Term> agenda, Term term, int beam_size)
	{
		Comparator<Term> cmp = new Comparator<Term>() 
		{
			public int compare(Term t1, Term t2)
			{
				if(t1.score >= t2.score)
					return 1;
				else 
					return -1;
			}
		};
		
		if(agenda.size() < beam_size)
		{
			agenda.add(term);
			Collections.sort(agenda, cmp);
		}
		else if(term.score > agenda.get(0).score)
		{
			agenda.set(0, term);
			Collections.sort(agenda, cmp);
		}
	}
	
	/** 计算生成的句子片段得分
	 * @param wordList
	 * @param i 生成sent中到i为止句子的标签
	 * @param term
	 * @param params double数组中[0：参数总和，1：上次更新的时间，2：参数的值]
	 * @param mode 0表示训练，1表示测试
	 * @return
	 */
	double computScore(List<String[]> wordList, int i, Term term, Map<String, double[]> params, int mode)
	{
		double result = 0.0;
		
		if(term.isEmpty())
		{
			return result;
		}
		
		String[] labels = extractLabels(term);
		List<String> features = extractFeature(wordList, labels, i);
		
		for(String feature : features)
		{
			if(params.containsKey(feature))
			{
				if(mode == 0)
					result += params.get(feature)[2];
				else
					result += params.get(feature)[0];
			}
		}
		return result;
	}
	
	void lazy_update(Map<String, double[]> params, List<String> features, double now, double scale)
	{
		for(String feature:features)
		{
			if(!params.containsKey(feature))
			{
				double[] temp = {scale, now, scale};//[0：参数总和，1：上次更新的时间，2：参数的值]
				params.put(feature, temp);
			}
			else
			{
				double elapsed = now - params.get(feature)[1];//
				double currVal = params.get(feature)[2];
				double currSum = params.get(feature)[0];
				
				params.get(feature)[0] = currSum + elapsed * currVal+scale;
				params.get(feature)[1] = now;
				params.get(feature)[2] = currVal + scale;//
			}
		}
	}
	void lazy_updateAll(Map<String, double[]> params, int now)//最后一次更新所有
	{
		for(Map.Entry<String, double[]> entry : params.entrySet())
		{
			double elapsed = now - entry.getValue()[1];
			double currVal = entry.getValue()[2];
			
			entry.getValue()[0] += elapsed * currVal;
			entry.getValue()[1] = now;
		}
	}

	/** 判断字符的类型，分为数字(D)、字母(L)、汉字(H)和其他符号(O) */
	String charType(String word)
	{
		String chr = "";
		
		for(int i = 0; i < word.length(); ++i)
		{
			char ch = word.charAt(i);
			
			if(	ch=='\u96f6' || ch=='\u4e00'|| ch=='\u4e8c' || ch=='\u4e09' || ch=='\u56db' || ch=='\u4e94' || ch=='\u516d' ||ch=='\u4e03' 
					|| ch=='\u516b' || ch=='\u4e5d' || ch=='\u5341' || ch=='\u767e' || ch=='\u5343' || ch=='\u4e07' || ch=='\u4ebf' )
				chr += 'D';
			else if(ch >= '\u0030' && ch <= '\u0039')
				chr += 'd';
			else if((ch >= '\u0041' && ch <= '\u005a')||(ch >= '\u0061' && ch<= '\u007a'))
				chr += 'l';
			else if(ch >= '\u4e00'&& ch<= '\u9fa5')
				chr += 'h';
			else
				chr += 'o';
		}
		return chr;
	}
	/** 判断字符的类型。分为英文字母、短横线、数字、LTP分词特点，比如“FM-32”是不分开的*/
	String getCharType(String word)
	{
		StringBuilder charType = new StringBuilder();
		String regex_digit = ".*[0-9]+.*";
		String regex_letter = ".*[a-z[A-Z]]+.*";
		String regex_strigula = ".*-.*";
		String regex_other = ".*[^a-zA-Z0-9-]+.*";
		if(word.matches(regex_letter) && word.matches(regex_strigula) && word.matches(regex_digit))
		{
			return "Y1";
		}
		if(word.matches(regex_other) && word.matches(regex_digit))
		{
			return "Y2";
		}
		if(word.matches(regex_digit))
		{
			return "Y3";
		}
		return "N";
		
	}
	
	
	/**
	 * 对某个词生成相应的特征(词性标注已加)
	 * @return
	 */
	List<String> extractFeature(List<String[]> wordList, String[] labels, int i)
	{
		List<String> features = new ArrayList<>();
		String prev_word, curr_word, next_word;
		String prevprev_word, nextnext_word;
		String prev_pos, curr_pos, next_pos;
		String prevprev_pos, nextnext_pos;
		curr_word = wordList.get(i)[0];
		curr_pos = wordList.get(i)[1];
		if(i <= 1)
		{//start sign
			if(i == 0)
			{
				prev_word = "####";
				prev_pos = "γγγγ";
			}else
			{
				prev_word = wordList.get(i-1)[0];
				prev_pos = wordList.get(i-1)[1];
			}
			prevprev_word = "####";
			prevprev_pos = "γγγγ"; 
		}
		else
		{
			prev_word = wordList.get(i-1)[0];
			prev_pos = wordList.get(i-1)[1];
			prevprev_word = wordList.get(i-2)[0];
			prevprev_pos = wordList.get(i-2)[1]; 
		}
		if(i >= wordList.size()-2)
		{//end sign
			if(i == wordList.size() - 1)
			{
				next_word = "$$$$";
				next_pos = "δδδδ";
			}else
			{
				next_word = wordList.get(i+1)[0];
				next_pos = wordList.get(i+1)[1];
			}
			nextnext_word = "$$$$";
			nextnext_pos = "δδδδ";
		}
		else
		{
			next_word = wordList.get(i+1)[0];
			next_pos = wordList.get(i+1)[1];
			nextnext_word = wordList.get(i+2)[0];
			nextnext_pos = wordList.get(i+2)[1];
		}
		String prev_cht = getCharType(prev_word);
		String curr_cht = getCharType(curr_word);
		String next_cht = getCharType(next_word);

		int featureId = 1;
		int window = 1;
		for(int ii = 0; ii<=2*window; ii++)
		{
			int Dis = -window+ii;
			String word = "";
			if(i + Dis <0)
			{
				word = "####";
			}else
			if(i + Dis >= wordList.size())
			{
				word = "$$$$";
			}else
			{
				word = wordList.get(i)[0];
			}
			features.add((featureId++)+"=word["+(Dis)+"]=" + word + "Label=" + labels[i]);
		}
			features.add((featureId++)+"=word[-1]word[0]="+ prev_word + curr_word + "Label=" + labels[i]);
			features.add((featureId++)+"=word[0]word[+1]="+ curr_word + next_word + "Label=" + labels[i]);
		for(int ii = 0; ii<=2*window; ii++)
		{
			int Dis = -window+ii;
			String postag = "";
			if(i + Dis <0)
			{
				postag = "γγγγ";
			}else
			if(i + Dis >= wordList.size())
			{
				postag = "δδδδ";
			}else
			{
				postag = wordList.get(i)[1];
			}
			features.add((featureId++)+"=pos["+(Dis)+"]=" + postag + "Label=" + labels[i]);
		}
			features.add((featureId++)+"=cht[-1]=" + prev_cht + "Label=" + labels[i]);
			
			features.add((featureId++)+"=cht[0]=" + curr_cht + "Label=" + labels[i]);
			
			features.add((featureId++)+"=cht[+1]=" + next_cht + "Label=" + labels[i]);
			features.add((featureId++)+"=pos[-1]pos[0]=" + prev_pos + curr_pos + "Label=" + labels[i]);
			features.add((featureId++)+"=pos[0]pos[+1]=" + curr_pos + next_pos + "Label=" + labels[i]);
			features.add((featureId++)+"=isRoleWord[-1]=" + roleEntity.getKeyWordScore(prev_word)  + "Label=" + labels[i]);
			features.add((featureId++)+"=isRoleWord[0]=" + roleEntity.getKeyWordScore(curr_word)  + "Label=" + labels[i]);
			features.add((featureId++)+"=isRoleWord[+1]=" + roleEntity.getKeyWordScore(next_word)  + "Label=" + labels[i]);
			
			//features.add((featureId++)+"=quantifier[-2]role[-1]=" + isQuantifier(prevprev_word)+roleEntity.getKeyWordScore(prev_word) + "Label=" + labels[i]);
			//features.add((featureId++)+"=quantifier[-1]role[0]=" + isQuantifier(prev_word)+roleEntity.getKeyWordScore(curr_word) + "Label=" + labels[i]);
			

			features.add((featureId++)+"=isDeviceWord[-1]=" + deviceEntity.getKeyWordScore(prev_word) + "Label=" + labels[i]);
			features.add((featureId++)+"=isDeviceWord[0]=" + deviceEntity.getKeyWordScore(curr_word) + "Label=" + labels[i]);
			features.add((featureId++)+"=isDeviceWord[+1]=" + deviceEntity.getKeyWordScore(next_word) + "Label=" + labels[i]);

			
			//features.add((featureId++)+"=quantifier[-2]device[-1]=" + isQuantifier(prevprev_word)+deviceEntity.getKeyWordScore(prev_word) + "Label=" + labels[i]);
			//features.add((featureId++)+"=quantifier[-1]device[0]=" + isQuantifier(prev_word)+deviceEntity.getKeyWordScore(curr_word) + "Label=" + labels[i]);
			
			features.add((featureId++)+"=isRegionWord[-1]=" + regionEntity.getKeyWordScore(prev_word) + "Label=" + labels[i]);
			features.add((featureId++)+"=isRegionWord[0]=" + regionEntity.getKeyWordScore(curr_word) + "Label=" + labels[i]);
			features.add((featureId++)+"=isRegionWord[+1]=" + regionEntity.getKeyWordScore(next_word) + "Label=" + labels[i]);
			//
			features.add((featureId++)+"=isCountryWord[-1]=" + countryEntity.getKeyWordScore(prev_word) + "Label=" + labels[i]);
			features.add((featureId++)+"=isCountryWord[0]=" + countryEntity.getKeyWordScore(curr_word) + "Label=" + labels[i]);
			features.add((featureId++)+"=isCountryWord[+1]=" + countryEntity.getKeyWordScore(next_word) + "Label=" + labels[i]);
			//
			features.add((featureId++)+"=isOrgnizationWord[-1]=" + orgEntity.getKeyWordScore(prev_word) + "Label=" + labels[i]);
			features.add((featureId++)+"=isOrgnizationWord[0]=" + orgEntity.getKeyWordScore(curr_word) + "Label=" + labels[i]);
			features.add((featureId++)+"=isOrgnizationWord[+1]=" + orgEntity.getKeyWordScore(next_word) + "Label=" + labels[i]);
			//
			features.add((featureId++)+"=isPersonWord[-1]=" + personEntity.isKeyWord(prev_word) + "Label=" + labels[i]);
			features.add((featureId++)+"=isPersonWord[0]=" + personEntity.isKeyWord(curr_word) + "Label=" + labels[i]);
			features.add((featureId++)+"=isPersonWord[+1]=" + personEntity.isKeyWord(next_word) + "Label=" + labels[i]);
		return features;
	}


	public String isCombinationWord(String curr_word) {
		// TODO Auto-generated method stub
		if(curr_word.length() == 2)
		{
			if(countryEntity.isKeyWord(curr_word.substring(0, 1)))
			{
				if(deviceEntity.isKeyWord(curr_word.substring(1, 2)))
				{
					return "b_dev";
				}
				if(orgEntity.isKeyWord(curr_word.substring(1, 2)))
				{
					return "b_nt";
				}
			}
		}
		return "0";
	}

	
}
