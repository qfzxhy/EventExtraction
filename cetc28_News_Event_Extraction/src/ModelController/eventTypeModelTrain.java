/*  
 * 创建时间：2015年10月25日 下午10:51:16  
 * 项目名称：Java_EventDetection_News  
 * @author qianf
 * @version 1.0   
 * @since JDK 1.8.0_21  
 * 文件名称：ModelController.java  
 * 系统信息：Windows Server 2008
 * 类说明：   关于事件类别模型学习更新的控制类
 * 功能描述： triggerNum更新，eventTypeThreshold（触发词列表阈值）更新
 * 当添加新的训练数据后，可以因此更新
 */
package ModelController;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.triggertype_extraction.EventTypeThresholdController;
import cetc28.java.eventdetection.triggertype_extraction.TriggerController;

public class eventTypeModelTrain {
	/**
	 *  触发词事件类别模型生成
	 * @param eventTrainDataPath 事件抽取训练数据路径 见trainDataByQianf
	 * @param eventTypeModelPath 事件类别模型存储路径
	 */
	 
	public void eventTypeModel_train(String eventTrainDataPath, String eventTypeModelPath)
	{
		TriggerController testController = new TriggerController();
		/*
		 * 慎重使用，最好事先备份eventTypeModel文件
		 */
		testController.updateTemplate(eventTrainDataPath, eventTypeModelPath);
	}
	/**
	 * 事件类别阈值模型生成
	 * @param triggerPath  触发词文件夹路径
	 * @param thresholdModelPath 事件类别阈值模型存储路径
	 */
	 
	public void eventTypeThreshold_train(String triggerPath, String thresholdModelPath)
	{
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
		EventTypeThresholdController.updateThreshold(thresholdModelPath, triggerListList);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//
		eventTypeModelTrain m = new eventTypeModelTrain();
		m.eventTypeModel_train(FileConfig.getEventTrainDataPath(), FileConfig.getEventTypeModelPath());
		m.eventTypeThreshold_train(FileConfig.getTriggerPath(), FileConfig.getThresholdPath());
	}

}
