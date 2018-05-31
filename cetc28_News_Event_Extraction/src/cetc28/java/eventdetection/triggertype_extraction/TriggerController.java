/*  
 * 创建时间：2015年10月25日 下午10:51:16  
 * 项目名称：Java_EventDetection_News  
 * @author qianf
 * @version 1.0   
 * @since JDK 1.8.0_21  
 * 文件名称：TriggerController.java  
 * 系统信息：Windows Server 2008
 * 类说明：   触发词模板的控制类
 * 功能描述： 模板的读取，修改，更新，以及根据模板进行分类
 * 当添加新的训练数据后，可以因此更新
 */
package cetc28.java.eventdetection.triggertype_extraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.trigger_extraction.Tools;
import cetc28.java.news.label.LabelItem;
/**
 * 创建时间：2015年10月25日 下午10:51:16  
 * 项目名称：Java_EventDetection_News   
 * 文件名称：TriggerController.java  
 * 系统信息：Windows Server 2008
 * 类说明：   触发词模板的控制类
 * 功能描述： 模板的读取，修改，更新，以及根据模板进行分类
 * 当添加新的训练数据后，可以因此更新
 * @author qf
 *
 */

public class TriggerController 
{
	public String templateFilePath;
	public TriggerTemplate template;
	public TriggerController() {
		// TODO Auto-generated constructor stub
	}
	public TriggerController(String filePath)
	{
		this.templateFilePath = filePath;
		this.template = new TriggerTemplate(TriggerController.getMapfromFile(filePath));
	}
	public TriggerTemplate getTemplate()
	{
		return this.template;
	}
	public void writeMaptoFile(HashMap<String,int[]> items,String filePath)
	{
		
		try {
			FileWriter writer = new FileWriter(filePath);
			BufferedWriter bw  = new BufferedWriter(writer);
			for (Entry<String, int[]> entry : items.entrySet()) 
			{
			    //System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
			    StringBuffer line = new StringBuffer(entry.getKey());
			    int[] numbers = entry.getValue();
			    for(int i =0;i<20;i++)
			    {
			    	line.append(",");
			    	line.append(numbers[i]);
			    }
			    line.append("\n");
			    bw.write(line.toString());
			}
			bw.flush();
			bw.close();
			writer.close();
		} catch (IOException e) {
			System.out.println("writing error");
			e.printStackTrace();
		}
	}
	public void updateTemplate(String trainDataFile,String triggerNumFile)
	{
		ArrayList<LabelItem> trainDatas = new ArrayList<>();
		ArrayList<Data> trainDataList = new ArrayList<>();
		Tools.loadTrainData(trainDataFile, trainDatas, trainDataList);
		updateTemplate(trainDatas,triggerNumFile);
	}
	/**
	 * 核心函数 更新触发词类别识别模型文件
	 * @param trainingData 训练数据
	 * @param triggerNumFile 触发词类别模型
	 */
	public void updateTemplate(ArrayList<LabelItem> trainingData,String triggerNumFile)
	{
		if (trainingData == null || trainingData.size()== 0)
		{
			System.out.println("TriggerController.updateTemplate: there is something null.");
		}
		HashMap<String,int[]> map = TriggerController.getTrainingData(trainingData);
		//this.template.updateTemplate(map);
		this.writeMaptoFile(map,triggerNumFile);
	}
	public static HashMap<String,int[]> getMapfromFile(String filePath)
	{
		HashMap<String,int[]> map = new HashMap<String,int[]>();
		try {
			FileReader reader = new FileReader(filePath);
		    BufferedReader br = new BufferedReader(reader);
		    String str = null;
		    while((str = br.readLine()) != null) 
		    {
//		        System.out.println(str);
		        String[] items = str.split(",");
		        int[] numbers = new int[20];
		        for(int i =0;i<20;i++)
		        	numbers[i] = Integer.parseInt(items[i+1]);
		        map.put(items[0], numbers);
		    }
		    br.close();
		    reader.close();
		} catch (FileNotFoundException e) 
		{
			System.out.println("File not found.");
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return map;
	}
	public static  HashMap<String,int[]> getTrainingData(ArrayList<LabelItem> trainingData)
	{
		if (trainingData == null || trainingData.size()==0)
		{
			System.out.println("there is something null.");
			return null;
		}
		HashMap<String,int[]> map = new HashMap<String,int[]>();
		for(LabelItem trainingItem :trainingData)
		{
			if (trainingItem.ifEvent== false) continue;
			String triggerWord = trainingItem.triggerWord;
			int eventType = trainingItem.eventType;
			if (triggerWord == null || eventType ==0) continue;
			if (map.containsKey(triggerWord))
			{
				map.get(triggerWord)[eventType-1]++;
			}
			else
			{
				int[] newNumbers = new int[20];
				newNumbers[eventType-1]++;
				map.put(triggerWord, newNumbers);
			}
		}
		return map;
	}
	
	public static void main(String[]args)
	{

		TriggerController testController = new TriggerController();
		testController.updateTemplate("trainDataByQianf","triggerNum");

	}
}
