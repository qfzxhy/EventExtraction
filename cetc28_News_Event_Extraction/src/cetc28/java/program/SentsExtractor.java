package cetc28.java.program;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;

public class SentsExtractor {
	static EventExtractionWithoutBackGround bsl = new EventExtractionWithoutBackGround();
	public static String processSentence(String sent)
	{
		if(sent.indexOf("：")!=-1)
		{
			return sent.substring(sent.indexOf("：")+1);
		}
		if(sent.indexOf(":")!=-1)
		{
			return sent.substring(sent.indexOf(":")+1);
		}
		if(sent.indexOf(" ")!=-1)
		{
			return sent.substring(0, sent.indexOf(" "));
		}
		return sent;
	}
	public static void extraction(List<String> sents) throws SQLException, IOException
	{	
		BufferedWriter bw = new BufferedWriter(new FileWriter("traindatas.txt"));
		for(String sent : sents)
		{
			sent = processSentence(sent);
			String newsURL = null;
			String imgAddress = null;
			String newsID = null;
			String saveTime = null;
			String newsTitle = null;
			String placeEntity = null;
			boolean isSummary = false;
			Pair<String, LabelItem> result = bsl.extractbysentence(newsURL, imgAddress, newsID, saveTime, newsTitle, sent, placeEntity, isSummary);
			if(result!=null)
			{
				bw.write(sent+"\n");
				bw.write(result.getSecond().triggerWord+"\n");
				bw.write(result.getSecond().sourceActor+"\n");
				bw.write(result.getSecond().targetActor+"\n");
			}
			
		}
		bw.close();
	}
	public static List<String> getYourSents(String path) throws IOException
	{
		List<String> sents = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(path));
		String line = null;
		while((line = br.readLine())!=null)
		{
			sents.add(line);
		}
		br.close();
		return sents;
	}
	public static void process_file() throws IOException
	{
		List<String> sents = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader("traindatas.txt"));
		String line = null;
		while((line = br.readLine())!=null)
		{
			sents.add(line);
		}
		br.close();
		BufferedWriter bw = new BufferedWriter(new FileWriter("traindatas.txt"));
		int i = 0;
		while(i < sents.size())
		{
			List<String> words = LtpTool.getWords(sents.get(i));
			String wordsplit = "";
			for(String word : words)
			{
				wordsplit = wordsplit + word + " ";
			}
			bw.write(wordsplit.trim()+"\n");
			bw.write(sents.get(i+1)+"\n");
			bw.write(sents.get(i+2)+"\n");
			bw.write(sents.get(i+3)+"\n");
			i += 4;
		}
		bw.close();
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		List<String> sents = null;
			try {
				sents = getYourSents("sents.txt");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				extraction(sents);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		try {
			process_file();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}

}
