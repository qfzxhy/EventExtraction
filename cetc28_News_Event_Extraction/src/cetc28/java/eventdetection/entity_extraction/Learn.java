package cetc28.java.eventdetection.entity_extraction;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cetc28.java.config.FileConfig;

public class Learn {
	public  static long NERTrain(String trainFile, String paramsFile)
	{
		
		Util u = new Util();
		List<String> sentenceList = u.loadFile(trainFile);
		//sentenceList = UtilExcel.loadfile(trainFile);
		long start = System.currentTimeMillis();
		NERTrain(sentenceList, paramsFile);
		long end = System.currentTimeMillis();
		return end-start;
	}
	public  static  void NERTrain(List<String> sentenceList, String paramsFile)
	{
		int beamSize = 16, mode = 0, iteration = 10;
		int window = 3;
		Map<String, double[]> params = new HashMap<>();
		Beam_search b = new Beam_search();
		//Util u = new Util();
		long start = System.currentTimeMillis();
		for(int i = 0; i < iteration; ++i)
		{
			List<Term> temp = new ArrayList<>();
			for(int j = 0; j < sentenceList.size(); ++j)
			{
				String sentence = sentenceList.get(j);
				temp = b.runBeamSearch(sentence, beamSize, params, i * sentenceList.size() + j, mode);
			}
			temp.clear();
		}
		int length = sentenceList.size();
		b.lazy_updateAll(params, length*iteration-1);
		BufferedWriter bw = null;
		try//budui
		{
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(paramsFile), "utf-8"));
			for(Map.Entry<String, double[]> entry : params.entrySet())
			{
				bw.write(entry.getKey()+ '\t' + entry.getValue()[0]/(length * iteration)+ '\n');
			}
		}
		catch(FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
		finally
		{
			try 
			{
				bw.flush();
				bw.close();
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		long end = System.currentTimeMillis();
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Learn learn = new Learn();
		learn.NERTrain(FileConfig.getNerTrainDataPath(), "paramsFile");
	}

}
