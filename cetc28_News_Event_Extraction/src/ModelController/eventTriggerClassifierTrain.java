/*
 * 最大熵分类器
 */
package ModelController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.preprocessing.LoadDataSingleton;
import cetc28.java.eventdetection.trigger_extraction.Feature;
import cetc28.java.eventdetection.trigger_extraction.FeatureController;
import cetc28.java.eventdetection.trigger_extraction.Tools;
import cetc28.java.eventdetection.triggertype_extraction.EventTypeExtraction;
import cetc28.java.news.label.LabelItem;
import opennlp.tools.ml.maxent.GIS;
import opennlp.tools.ml.maxent.io.GISModelReader;
import opennlp.tools.ml.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.tools.ml.model.AbstractModel;
import opennlp.tools.ml.model.AbstractModelWriter;
import opennlp.tools.ml.model.DataIndexer;
import opennlp.tools.ml.model.DataReader;
import opennlp.tools.ml.model.FileEventStream;
import opennlp.tools.ml.model.MaxentModel;
import opennlp.tools.ml.model.OnePassDataIndexer;
import opennlp.tools.ml.model.PlainTextFileDataReader;
/**
 * 训练最大熵触发词分类模型
 * 使用方法见main函数
 * @author qf
 *
 */
public class eventTriggerClassifierTrain {
	public MaxentModel classifier_maxent;
	public eventTriggerClassifierTrain(String modelPath) {//train feature vector file       //这块暂时使用文件
		// TODO Auto-generated constructor stub
		this.loadModel(modelPath);
	}
	public eventTriggerClassifierTrain() {
		// TODO Auto-generated constructor stub
	}
	public void loadModel(String modelPath)
	{
		try {
			DataReader modelReader = new PlainTextFileDataReader(new BufferedReader(new FileReader(modelPath)));
			this.classifier_maxent = new GISModelReader(modelReader).getModel();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public double[] predict(String[] featureArray)
	{
		double[] outcomes = this.classifier_maxent.eval(featureArray);
		return outcomes;
	}
	/*
	 * 用户可以通过这个方法，自己训练最大熵模型（训练数据格式需要一致）
	 */
	/**
	 * 用户可以通过这个方法，自己训练最大熵模型（训练数据格式需要一致）
	 * 训练最大熵分类模型1
	 * @param trainDataPath 训练数据路径
	 * @param modelPath //模型文件路径
	 */
	public void trainModel1(String trainDataPath, String modelPath)
	{
		FeatureController featureExtractor = new FeatureController();;
		EventTypeExtraction eventTypeExtractor = new EventTypeExtraction(FileConfig.getEventTypeModelPath(), FileConfig.getTriggerPath(), FileConfig.getThresholdPath());
		ArrayList<Data> trainDataList_data = new ArrayList<>();
		ArrayList<LabelItem> trainDataList_labelItem = new ArrayList<>();
		Tools.loadTrainData(trainDataPath, trainDataList_labelItem, trainDataList_data);
		ArrayList<Feature> featureList = this.loadModel1Feature(trainDataList_data,featureExtractor,eventTypeExtractor);
		this.writeFeature(featureList, "model1_features.txt");
		DataIndexer indexer = null;
		try {
			indexer = new OnePassDataIndexer(new FileEventStream("model1_features.txt"));
			MaxentModel maxEn = GIS.trainModel(20, indexer);
			AbstractModelWriter writer = new SuffixSensitiveGISModelWriter((AbstractModel) maxEn, new File(modelPath));
			writer.persist();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		finally
//		{
//			File file = new File("model1_features.txt");
//			file.delete();
//		}
		
	}
	/**
	 * 训练最大熵分类模型2
	 * @param trainDataPath 训练数据路径
	 * @param modelPath 模型文件路径
	 */
	public void trainModel2(String trainDataPath, String modelPath)
	{
		FeatureController featureExtractor = new FeatureController();;
		EventTypeExtraction eventTypeExtractor = new EventTypeExtraction(FileConfig.getEventTypeModelPath(), FileConfig.getTriggerPath(), FileConfig.getThresholdPath());
		ArrayList<Data> trainDataList_data = new ArrayList<>();
		ArrayList<LabelItem> trainDataList_labelItem = new ArrayList<>();
		Tools.loadTrainData(trainDataPath, trainDataList_labelItem, trainDataList_data);
		ArrayList<Feature> featureList = this.loadModel2Feature(trainDataList_data,featureExtractor,eventTypeExtractor);
		this.writeFeature(featureList, "model2_features.txt");
		DataIndexer indexer = null;
		try {
			indexer = new OnePassDataIndexer(new FileEventStream("model2_features.txt"));
			MaxentModel maxEn = GIS.trainModel(20, indexer);
			AbstractModelWriter writer = new SuffixSensitiveGISModelWriter((AbstractModel) maxEn, new File(modelPath));
			writer.persist();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		finally
//		{
//			File file = new File("model2_features.txt");
//			file.delete();
//		}
		
	}
	/**
	 * 抽取特征
	 * @param trainDataList_data
	 * @param featureExtractor
	 * @param eventTypeExtractor
	 * @return
	 */
	public ArrayList<Feature> loadModel1Feature(ArrayList<Data> trainDataList_data, FeatureController featureExtractor, EventTypeExtraction eventTypeExtractor) 
	{
		ArrayList<Feature> featureList = new ArrayList<>();
		for(Data trainData : trainDataList_data)
		{
			 ArrayList<Feature> features = featureExtractor.extractFeature(trainData, "training",null,eventTypeExtractor);	
			featureList.addAll(features);
		}
		return featureList;
	}
	/**
	 * 抽取特征
	 * @param trainDataList_data
	 * @param featureExtractor
	 * @param eventTypeExtractor
	 * @return
	 */
	public ArrayList<Feature> loadModel2Feature(ArrayList<Data> trainDataList_data, FeatureController featureExtractor, EventTypeExtraction eventTypeExtractor) 
	{
		ArrayList<Feature> featureList = new ArrayList<>();
		for(Data trainData : trainDataList_data)
		{
			ArrayList<Feature> features = featureExtractor.extractFeature_V(trainData,"training",null);		
			featureList.addAll(features);
		}
		return featureList;
	}
	/**
	 * 将抽取的特征写入文件
	 * @param featureList
	 * @param filePath
	 */
	public void writeFeature(ArrayList<Feature> featureList,String filePath)
	{
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(new File(filePath)));
			for(Feature feature : featureList)
			{
			//	System.out.println(feature.label);
				bw.write(feature.label+" ");
				String[] featureArray = feature.toArray();
				for(String featureItem : featureArray)
				{
					bw.write(featureItem+" ");
				}
				bw.write("\n");
			}
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		//调用trainmodel1()生成第一个模型
		//调用trainmodel2()生成第一个模型
		eventTriggerClassifierTrain mc = new eventTriggerClassifierTrain();
		//mc.trainModel1(FileConfig.getEventTrainDataPath(), FileConfig.getMaxentModel1Path());
		mc.trainModel2(FileConfig.getEventTrainDataPath(), FileConfig.getMaxentModel2Path());
	}
}
