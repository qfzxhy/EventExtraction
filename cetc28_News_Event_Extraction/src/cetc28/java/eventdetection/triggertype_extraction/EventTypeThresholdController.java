package cetc28.java.eventdetection.triggertype_extraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.preprocessing.LoadDataSingleton;
/**
 * 事件类别阈值操作
 * @author worker03
 *
 */
public class EventTypeThresholdController {
	static LoadDataSingleton dataStorer = LoadDataSingleton.getInstance(); 
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String triggerPath = FileConfig.getTriggerPath();
		ArrayList<ArrayList<String>> triggerListList = new ArrayList<>();
		try {
			String[] files = new File(triggerPath).list();
			for(String file : files)
			{
				ArrayList<String> triggerList = new ArrayList<>();
				BufferedReader br = new BufferedReader(new FileReader(triggerPath+file));
				String line = "";
				while((line = br.readLine()) != null)
				{
					triggerList.add(line);
				}
				br.close();
				triggerListList.add(triggerList);
			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		updateThreshold("threshold", triggerListList);
	}

	public static double[] getThresholdfromFile(String eventTypeThresholdPath) {
		// TODO Auto-generated method stub
		double[] thresholds = new double[20];
		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(eventTypeThresholdPath)));
			String line = "";
			int id = 0;
			while((line = br.readLine())!=null)
			{
				thresholds[id++] = Double.parseDouble(line);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return thresholds;
	}
	public static void updateThreshold(String filename,ArrayList<ArrayList<String>> triggerListList)
	{
		double[] thresholds = new double[20];
		int id = 0;
		for(ArrayList<String> list : triggerListList)
		{
			thresholds[id++] = getSimilarScore(list);
		}
		writeDoubleToFile(filename, thresholds);
	}
	private static double getSimilarScore(ArrayList<String> list) {
		// TODO Auto-generated method stub
		HashMap<String, float[]> embedding = dataStorer.word2Vec;
		Random rand = new Random();
		double minScore = Double.MAX_VALUE; //important
		for(int i = 0; i< 5; i++)
		{
			int id = rand.nextInt(list.size());
			String word = list.get(id);
			ArrayList<String> compareList = new ArrayList<>();
			for(int j=0;j<10;j++)
			{
				int k =  rand.nextInt(list.size());
				if(id != k)
				{
					compareList.add(list.get(k));/////
				}
			}
			if(!embedding.containsKey(word)){continue;}
			double score = 0.0;
			int num = 0;
			for(String compareWord : compareList)
			{
				if(embedding.containsKey(compareWord))
				{
					num++;
					score += getCosinDis(embedding.get(word), embedding.get(compareWord));
				}
			}
			score /= num;
			minScore = score < minScore ? score : minScore;
		}
		return minScore;
		
	}
	private static void writeDoubleToFile(String filename,double[] thresholds)
	{
		if(thresholds == null)
			System.out.println("something error in class_EventTypeThresholdController");
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)));
			for(double threshold : thresholds)
			{
				bw.write(threshold+"\n");
			}
			bw.flush();bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static double getSimilarScore(String word, int type)
	{
		return getSimilarScore(word, dataStorer.triggerListList.get(type-1));
	}
	public static double getSimilarScore(String word, ArrayList<String> list)//SIM(word,eventType)
	{
		Random  rand = new Random();
		double minScore = Double.MAX_VALUE; //important
		ArrayList<String> compareList = new ArrayList<>();
		for(int i = 0; i< 10; i++)
		{
			int id = rand.nextInt(list.size());
			compareList.add(list.get(id));
		}
		return getDis(word, compareList);
		
	}
	private static double getDis(String word, ArrayList<String> triggerList)
	{
		double score = 0.0;
		int id = 0;
		HashMap<String, float[]> embedding = dataStorer.word2Vec;
		if(embedding.containsKey(word))
		{
			for(String trigger : triggerList)
			{
				if(embedding.containsKey(trigger))
				{
					id ++ ;
					score += getCosinDis(embedding.get(word), embedding.get(trigger));
				}
			}
		}else
		{
			return 0.0;
		}
		return score/(double)id;
	}
	private static float getCosinDis(float[] vector, float[] vec) {
		// TODO Auto-generated method stub
		double vectorNorm = 0.0d,vecNorm = 0.0d;
		for(int i = 0;i<vector.length;i++)
		{
			vectorNorm += vector[i]*vector[i];
			vecNorm += vec[i]*vec[i];
		}
		vectorNorm = Math.sqrt(vectorNorm);
		vecNorm = Math.sqrt(vecNorm);
		double cosinDis = 0.0d;
		for(int i = 0;i<vector.length;i++)
		{
			cosinDis += vector[i]*vec[i];
		}
		return (float)(cosinDis/(vecNorm*vectorNorm));
	}

}
