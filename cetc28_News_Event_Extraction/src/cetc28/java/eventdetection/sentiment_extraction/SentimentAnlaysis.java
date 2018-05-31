package cetc28.java.eventdetection.sentiment_extraction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cetc28.java.config.FileConfig;

public class SentimentAnlaysis {
	Map<String,Double> sentDicList = new HashMap<>();
	Map<String,Double> negtiveList = new HashMap<>();
	Map<String,Double> advList = new HashMap<>();

	public SentimentAnlaysis() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(FileConfig.getPosPath()));
		String s = null;
		while((s = br.readLine())!=null){
			sentDicList.put(s, 1.0);
		}
		br.close();
		br = new BufferedReader(new FileReader(FileConfig.getNegPath()));
		while((s = br.readLine())!=null){
			sentDicList.put(s, -1.0);
		}
		br.close();
		br = new BufferedReader(new FileReader(FileConfig.getsentPath()));
		while((s = br.readLine())!=null){
			sentDicList.put(s.split(" ")[0],Double.parseDouble(s.split(" ")[1]));
		}
		br.close();
		br = new BufferedReader(new FileReader(FileConfig.getnotPath()));
		while((s = br.readLine())!=null){
			negtiveList.put(s, -1.0);
		}
		br = new BufferedReader(new FileReader(FileConfig.getdegreePath()));
		while((s = br.readLine())!=null){
			sentDicList.put(s.split("\t")[0],Double.parseDouble(s.split("\t")[1]));
		}
	}
	public void getPolarity(){}
	public String classify_words(List<String> words)
	{
		
		Map<Integer, Double> sen_word = new HashMap<>();
		Map<Integer, Double> not_word = new HashMap<>();
		Map<Integer, Double> degree_word = new HashMap<>();
		List<Integer> sen_word_indexs = new ArrayList<>();
		
		for(int i = 0; i < words.size();i++)
		{
			String word = words.get(i);
			
			if(sentDicList.containsKey(word))
			{
				sen_word.put(i, sentDicList.get(word));
				sen_word_indexs.add(i);
				continue;
			}
			if(negtiveList.containsKey(word))
			{
				not_word.put(i, negtiveList.get(word));
				continue;
			}
			if(advList.containsKey(word))
			{
				degree_word.put(i, advList.get(word));
				continue;
			}
		}
		
		int W = 1;
		double score = 0.0;
		int sent_index = -1;
		for(int i = 0;i < words.size();i++)
		{
			if(sen_word.containsKey(i))
			{
				score += W * sen_word.get(i);
				sent_index += 1;
				if(sent_index < sen_word_indexs.size()-1)
				{
					int s = sen_word_indexs.get(sent_index);
					int e = sen_word_indexs.get(sent_index + 1);
					for(int j = s; j < e;j++)
					{
						if(not_word.containsKey(j))
						{
							W *= -1;
						}else
						{
							if(degree_word.containsKey(j))
							{
								W *= degree_word.get(j);
							}
						}
					}
				}
			}
		}
		if(score > 0){return "1";}
		if(score < 0){return "-1";}
		return "0";
	}
	private void loadFile(List<String> list, String filePath) throws IOException {
		// TODO Auto-generated method stub
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String s = null;
		while((s = br.readLine())!=null){
			list.add(s);
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			SentimentAnlaysis sa = new SentimentAnlaysis();
			List<String> words = new ArrayList<>();
			words.add("阿富汗");
			words.add("发生");
			words.add("地震");
			String re  = sa.classify_words(words);
			System.out.println(re);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
