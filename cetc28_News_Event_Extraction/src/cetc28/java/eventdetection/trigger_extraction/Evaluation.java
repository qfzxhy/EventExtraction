package cetc28.java.eventdetection.trigger_extraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.triggertype_extraction.EventTypeExtraction;
import cetc28.java.news.label.LabelItem;
/**
 * 评估模型准确率和召回率
 * @author qf
 *
 */
public class Evaluation {
	public void test(String testDataFile)
	{
		extractAll(testDataFile);
		System.out.println("预测完成");
	}
	/**
	 * 控制台测试每一条（用来测试代码）
	 */
	public void testByGUI()
	{
		TriggerExtractor e = new TriggerExtractor();
		Scanner s = new Scanner(System.in);
		
		while(true)
		{
			String newsTitle = s.nextLine();
			LabelItem data = new LabelItem("", "", "", newsTitle);
			Data testData = new Data(data);
			testData.setTrainData();
			e.extract(testData);
			data.Print();
		}
	}
	/**
	 * 单独测试一条数据
	 * @param title
	 */
	private void testSingleDemo(String sentence)
	{
		TriggerExtractor e = new TriggerExtractor();
		LabelItem data = new LabelItem("", "", "", sentence);
		Data testData = new Data(data);
		testData.setTrainData();
		e.extract(testData);
		data.Print();
	}
	/**
	 * 抽取测试文件中所有句子的触发词
	 * @param testDataFile 测试文件路径
	 */
	public void extractAll(String testDataFile) {
		TriggerExtractor triggerExtractor = new TriggerExtractor();
		PostProcessing postProcessing = new PostProcessing();
		String triggerNumModelPath = FileConfig.getEventTypeModelPath();
		String eventTypeThresholdPath = FileConfig.getThresholdPath();
		String triggerPath = FileConfig.getTriggerPath();
		// TODO Auto-generated method stub、
		EventTypeExtraction ee = new EventTypeExtraction(triggerNumModelPath, triggerPath, eventTypeThresholdPath);
		ArrayList<Data> testDataList_data = new ArrayList<>();
		ArrayList<LabelItem> testDataList_labelitem = new ArrayList<>();
		Tools.loadTrainData(testDataFile, testDataList_labelitem, testDataList_data);
		for(Data testData : testDataList_data)
		{
			testData.data.triggerWord = null;
			testData.data.eventType = 0;
		}
		for(Data testData : testDataList_data)
		{
			 	triggerExtractor.extract(testData);
				ee.eventTypeExtract(testData);
				postProcessing.postProcessing(testData);
		}
		//evaluate1(testDataList_data,testDataList_labelitem);;
		//System.out.println();
		evaluate2(testDataList_data,testDataList_labelitem);;
//		System.out.println("right:"+right+"wrong:"+wrong);
	}
	public void evaluate1(ArrayList<Data> testDataList_data,ArrayList<LabelItem> testDataList_labelitem)
	{
		float t_p = 0.0f;
		float e_p = 0.0f;
		int t_time = 0;
		int e_time = 0;
		for(int i = 0;i<testDataList_data.size();i++){
			
			if(testDataList_data.get(i).data.triggerWord != null)
			{
				//dataStorer.testDataList.get(i).data.triggerWord.indexOf(dataStorer.testDataWithResultList.get(i).data.triggerWord)!=-1
				if(testDataList_data.get(i).data.triggerWord.indexOf(testDataList_labelitem.get(i).triggerWord)!=-1)
				{
					t_p++;
				}
				t_time++;
			}
			if(testDataList_data.get(i).data.triggerWord != null)
			{
				if(testDataList_data.get(i).data.eventType == testDataList_labelitem.get(i).eventType)
				{
					e_p++;
				}else
				if(
						testDataList_data.get(i).data.triggerWord.indexOf(testDataList_labelitem.get(i).triggerWord)!=-1)
				{
					e_p++;
				}
				e_time++;
			}
		}
		System.out.println("triggerPrecise: "+t_p/t_time);
		System.out.println("eventPrecise: "+e_p/e_time);
		System.out.println("triggerRecall: "+t_p/testDataList_labelitem.size());
		System.out.println("eventRecall: "+e_p/testDataList_labelitem.size());
	
	}
	/**
	 * 输出准确率和召回率
	 * @param testDataList_data 测试数据 预测结果
	 * @param testDataList_labelitem 测试数据 标准结果
	 */
	public void evaluate2(ArrayList<Data> testDataList_data,ArrayList<LabelItem> testDataList_labelitem)
	{
		float t_p = 0.0f;
		float e_p = 0.0f;
		int t_time = 0;
		int e_time = 0;
		for(int i = 0;i<testDataList_data.size();i++){
			
			if(testDataList_data.get(i).data.triggerWord != null)
			{
				if(testDataList_data.get(i).data.triggerWord.equals(testDataList_labelitem.get(i).triggerWord))
				{
					t_p++;
				}
				t_time++;
			}
				
			if(testDataList_data.get(i).data.triggerWord != null)
			{
				if(testDataList_data.get(i).data.eventType == testDataList_labelitem.get(i).eventType
					)
				{
					e_p++;
				}
				e_time++;
			}
		}
		System.out.println("triggerPrecise: "+t_p/t_time);
		System.out.println("eventPrecise: "+e_p/e_time);
		System.out.println("triggerRecall: "+t_p/testDataList_labelitem.size());
		System.out.println("eventRecall: "+e_p/testDataList_labelitem.size());
	
	}
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		Evaluation t = new Evaluation();
		t.test(FileConfig.getEventTestDataPath());
		//t.testByGUI();
		//	t.testSingleDemo("中方愿与各方在应对气候变化问题上继续加强合作");
		//t.crossvalidte(5);
		/*
		 * triggerPrecise: 0.742236
eventPrecise: 0.72981364
triggerRecall: 0.73200613
eventRecall: 0.719755
		 */
	}

}
