package cetc28.java.eventdetection.preprocessing;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyStore.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.trigger_extraction.BasicVerb;
import cetc28.java.eventdetection.triggertype_extraction.TriggerTemplate;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
/**
 * 加载模型类
 * @author qf
 *
 */
public class LoadDataSingleton {
	private static LoadDataSingleton instance = new LoadDataSingleton();
	private static boolean hasLoaded = false;
	private LoadDataSingleton(){}
	public static LoadDataSingleton getInstance()
	{
		if(!hasLoaded)//data whether has loaded
		{
			loadData();
			hasLoaded = true;
		}
		return instance;
	}
	public static ArrayList<ArrayList<String>> triggerListList;//触发词列表
	//public static TriggerTemplate triggerTemplate;//
	public static HashMap<String, String> wordSemanticInfo;//词的语义信息
	public static HashMap<String, Integer> stopwords;//停用词
	public static HashMap<String, float[]> triggerWord2Vec;//触发词embedding
	public static HashMap<String, float[]> word2Vec;//所有词embedding
	
	public static void loadData()
	{
		load_embedding();//加载embedding
		load_triggerList();//加载触发词列表
		load_wordSemanticInfo();//加载词的字组词语义信息同义词林
	}
	private static void load_embedding() {
		// TODO Auto-generated method stub
		word2Vec = new HashMap<>();
		String word2vecPath = FileConfig.getEmbeddingPath();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(word2vecPath));
			String dimensionInfo = br.readLine();
			int featureSize = Integer.parseInt(dimensionInfo.split("\\s+")[1]);
			String line = "";
			while((line = br.readLine())!=null)
			{
				String[] array = line.split("\\s+");
				String trigger = array[0];
				List<String> posList = LtpTool.getPosTag(trigger);
				if(posList.size()>0 && posList.get(0).equals("v"))//vNerExtract.getN_V(trigger).get(0).getSecond() == 1
				{
					float[] vector = new float[featureSize];
					for(int i = 0;i<vector.length;i++)
					{
						vector[i] = Float.parseFloat(array[i+1]);
					}
					word2Vec.put(trigger, vector);
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@SuppressWarnings("resource")
	private static void load_wordSemanticInfo() {
		// TODO Auto-generated method stub
		wordSemanticInfo = new HashMap<>();
		BufferedReader br = null;
		
			try {
				br = new BufferedReader(new FileReader(FileConfig.getSynonymDicPath()));
				String line = "";
				while((line = br.readLine())!=null)
				{
					String[] splitByBlank = line.split("\\s+");
					String semanticInfo = splitByBlank[0].substring(0, 4);
					for(int i = 1;i<splitByBlank.length;i++)
					{
						wordSemanticInfo.put(splitByBlank[i], semanticInfo);
					}
				}
				br.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	private static void load_triggerList() {
		// TODO Auto-generated method stub
		String triggerListPath = FileConfig.getTriggerPath();
		triggerListList = new ArrayList<>(20);
		BufferedReader br = null;
		for(int id = 1; id <= 20; id++)
		{
			ArrayList<String> triggers = new ArrayList<>();
			try {
				br = new BufferedReader(new FileReader(new File(triggerListPath + id + ".txt")));
				String trigger = null;
				while((trigger = br.readLine())!=null)
				{
					triggers.add(trigger);
				}
				br.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			triggerListList.add(triggers);
		}
	}
	private static int getCount(ArrayList<String> wordList,
			HashMap<String, int[]> triggerNumber) {
		// TODO Auto-generated method stub
		int num = 0;
		for(String word : wordList)
		{
			if(triggerNumber.containsKey(word))
			{
				num++;
			}
		}
		return num;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		LoadDataSingleton dataStorer = LoadDataSingleton.getInstance();
		boolean isCandidate = false;
		String word = "追杀";
		String wordPos = LtpTool.getPosTag(word).get(0);
	
	}

}
