package cetc28.java.eventdetection.entity_extraction;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import cetc28.java.config.FileConfig;

public class Evaluate {
	/*
	 * per : precise recall f
	 * org : precise recall f
	 * .....
	 */
	final String[] type = new String[]{"org","role","country","region","nr","device"};
	List<String> goldDataList = null;
	List<String> predictDataList = null;
	int[] goldCount = null;
	int[] rightCount = null;
	int[] predictCount = null;
	public Evaluate(String testPath, String predictPath) {
		// TODO Auto-generated constructor stub
		goldCount = new int[6];//6种实体
		rightCount = new int[6];//6种实体
		predictCount = new int[6];//6种实体
		goldDataList = new ArrayList<String>();
		predictDataList = new ArrayList<>();
		try {
			loadData(testPath,predictPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void loadData(String testPath,String predictPath) throws IOException {
		// TODO Auto-generated method stub
		BufferedReader br_gold = new BufferedReader(new FileReader(testPath));
		String str = null;
		while((str = br_gold.readLine())!=null)
		{
			goldDataList.add(str);
		}
		br_gold.close();
		BufferedReader br_pre = new BufferedReader(new FileReader(new File(predictPath)));
		while((str = br_pre.readLine())!=null)
		{
			predictDataList.add(str);
		}
		br_pre.close();
	}
	public void evaluate()
	{
//		for(int i=0;i<goldDataList.size();i++)
//		{
//			String goldStr = goldDataList.get(i);
//			String predictStr = predictDataList.get(i);
//			statisticOperate(goldStr, predictStr, goldCount, rightCount, predictCount);
//		}
		
		List<String> resultList = this.showResult();
		this.compute(resultList);
		this.display();
		//
	}
	private void compute(List<String> resultList)
	{
		for(int i = 0;i<resultList.size(); i++)
		{
			if(resultList.get(i).trim().equals("")) continue;
			String p = resultList.get(i).split("\\s+")[3];
			String g = resultList.get(i).split("\\s+")[2];
			helper_gold(g);
			helper_predict(p);
			if(p.equals(g))
			{
				if(p.equals("other")) continue;
				if(p.startsWith("b"))
				{
					String tempLabel = p;
					for(int j=i+1; ;j++)
					{
						if(j == resultList.size()){helper_right(p); i = j-1;break;}
						if(resultList.get(j).trim().equals(""))continue;
						p = resultList.get(j).split("\\s+")[3];
						g = resultList.get(j).split("\\s+")[2];
						if(p.startsWith("i") && g.startsWith("i"))
						{
							continue;
						}else
						{
							if((p.startsWith("b")|| p.startsWith("other")) && (g.startsWith("b")|| g.startsWith("other")))
							{
								helper_right(tempLabel);
								i = j-1;
								break;
							}else
							{
								i = j-1;
								break;
							}
						
						}
					}
				}
			}
		}
	}
	
	//将正确结果和预测结果整合在一起
	private List<String> showResult()
	{
		List<String> predictList = predictDataList;
		List<String> goldList =goldDataList;
		List<String> resultList = new ArrayList<>();
		for(int i=0; i<goldList.size(); i++)
		{
			String[] predictArray = predictList.get(i).split("\\s+");
			String[] goldArray = goldList.get(i).split("\\s+");
			for(int j=0;j<goldArray.length;j++)
			{
				String goldStr = goldArray[j].replaceAll("/", " ");
				goldStr = goldStr + " " + predictArray[j].substring(predictArray[j].lastIndexOf("/")+1);
				resultList.add(goldStr);
			}
			resultList.add("");
		}
		Util.store("output_standard", resultList);
		return resultList;
	}
	public void display()
	{
		DecimalFormat decimalFormat=new DecimalFormat("0.000");
		float averageF = 0.0f;
		for(int i=0;i<6;i++)
		{
			float precise = (float) (rightCount[i]*1.0/predictCount[i]);
			float recall = (float) (rightCount[i]*1.0/goldCount[i]);
			float f = precise*recall*2/(precise+recall);
			averageF += f;
//			System.out.print(decimalFormat.format(precise)+"\t");
//			System.out.print(decimalFormat.format(recall)+"\t");
//			System.out.println(decimalFormat.format(f));
		}
		System.out.println(averageF/6);
		System.out.println("┌─-------┬─----------┬─--------┬─----┐");
		System.out.println("│  type  │   precise │  recall │  f  │ ");
		System.out.println("├──------├─----------├─--------├─----┤");
		String title1 = "  type  ";
		String title2 = "   precise ";
		String title3 = "  recall ";
		String title4 = "  f  ";
		
		for(int i=0;i<6;i++)
		{
			System.out.print("│");
			String t = type[i];
			int len = title1.length() - t.length();
			int split = len/2;
			int j=0;
			for(j=0;j<split;j++){System.out.print(" ");}
			System.out.print(t);
			for(;j<len;j++){System.out.print(" ");}
			System.out.print("│");
			int fLen = 5;
			len = title2.length() - fLen;
			split = len/2;
			j=0;
			float precise = (float) (rightCount[i]*1.0/predictCount[i]);
			for(j=0;j<split;j++){System.out.print(" ");}
			System.out.print(decimalFormat.format(precise));
			for(;j<len;j++){System.out.print(" ");}
			System.out.print("│");
			len = title3.length() - fLen;
			split = len/2;
			float recall = (float) (rightCount[i]*1.0/goldCount[i]);
			for(j=0;j<split;j++){System.out.print(" ");}
			System.out.print(decimalFormat.format(recall));
			for(;j<len;j++){System.out.print(" ");}
			System.out.print("│");
			float f = precise*recall*2/(precise+recall);
			len = title4.length() - fLen;
			split = len/2;
			for(j=0;j<split;j++){System.out.print(" ");}
			System.out.print(decimalFormat.format(f));
			for(;j<len;j++){System.out.print(" ");}
			System.out.print("│");
			//System.out.println("│ "+type[i]+"  │"+rightCount[i]*1.0/predictCount[i]+" │" +rightCount[i]*1.0/goldCount[i]+"  │");
			System.out.println();
			if(i<5)
			System.out.println("├──------├─----------├─--------├─----┤");
		}
		      System.out.print("└─-------┴─----------┴─--------┴─----┘"); 
	}
	private void statisticOperate(String goldStr, String predictStr,
			int[] goldCount, int[] rightCount, int[] predictCount) {
		// TODO Auto-generated method stub
		String[] goldArray = goldStr.split("\\s+");
		String[] predictArray = predictStr.split("\\s+");
		for(int i = 0; i<goldArray.length; i++)
		{
			String goldLabel = goldArray[i].substring(goldArray[i].lastIndexOf("/")+1);
			String predictLabel = predictArray[i].substring(predictArray[i].lastIndexOf("/")+1);
			//gold
			helper_gold(goldLabel);
			//predict
			helper_predict(predictLabel);
			//right
			if(goldLabel.equals(predictLabel) && goldLabel.startsWith("b"))//xiecuo...
			{
				int j = i;
				String tempLabel = goldLabel;
				
				while(goldLabel.equals(predictLabel))
				{
					j++;
					
					if(j >= goldArray.length)
					{
						helper_right(tempLabel);
						break;
					}
					goldLabel = goldArray[j].substring(goldArray[j].lastIndexOf("/")+1);
					predictLabel = predictArray[j].substring(predictArray[j].lastIndexOf("/")+1);
//					System.out.print(goldArray[j].substring(0, goldArray[j].indexOf("/")));
					if(goldLabel.indexOf("i") == -1 && predictLabel.indexOf("i") == -1)//other
					{
						helper_right(tempLabel);
						
						i = j-1;
						break;
					}
				}
			
			}
			
		}
	}
	public void nerAll(String testfile, String predictfile)
	{
		List<String> sentenceList = Util.loadfile(testfile);
		List<String> predictList = new ArrayList<>();
		for(String sentence : sentenceList)
		{
			predictList.add(Ner.ner2(sentence));
		}
		Util.store(predictfile, predictList);
	}
	/*
	 * case "orgnizationinfor" : return "nt";
			case "roleinfor" : return "role";
			case "countryinfor" : return "country";
			case "regioninfor" : return "ns";
			case "personinfor" : return "per";
			case "deviceinfor" : return "dev";
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		String root = "D:\\28proj_nju\\News_Event_Extract\\data\\";
//		if(Ner.create("src/Java_News_Ner/params",root+ "entitys","src/Java_News_Ner/trainCorpus")<0){System.err.println("error");}
//		String testfile ="src/Java_News_Ner/testCorpus";
//		String predictfile ="src/Java_News_Ner/predict" ;
		Evaluate eval = new Evaluate(FileConfig.getNerTestDataPath(),"ner_tmp.txt" );
		
		eval.nerAll(FileConfig.getNerTestDataPath(),"ner_tmp.txt");
		eval.evaluate();
	}
	private void helper_gold(String s)
	{
		switch (s) {
		case "b_nt":
			goldCount[0]++;
			break;
		case "b_role":
			goldCount[1]++;
			break;
		case "b_country":
			goldCount[2]++;
			break;
		case "b_ns":
			goldCount[3]++;
			break;
		case "b_nr":
			goldCount[4]++;
			break;
		case "b_dev":
			goldCount[5]++;
			break;
		default:
			break;
		}
	}
	private void helper_predict(String s)
	{
		switch (s) {
		case "b_nt":
			predictCount[0]++;
			break;
		case "b_role":
			predictCount[1]++;
			break;
		case "b_country":
			predictCount[2]++;
			break;
		case "b_ns":
			predictCount[3]++;
			break;
		case "b_nr":
			predictCount[4]++;
			break;
		case "b_dev":
			predictCount[5]++;
			break;
		default:
			break;
		}
	}
	private void helper_right(String s)
	{
		switch (s) {
		case "b_nt":
			rightCount[0]++;
			break;
		case "b_role":
			rightCount[1]++;
			break;
		case "b_country":
			rightCount[2]++;
			break;
		case "b_ns":
			rightCount[3]++;
			break;
		case "b_nr":
			rightCount[4]++;
			break;
		case "b_dev":
			rightCount[5]++;
			break;
		default:
			break;
		}
	}
}
