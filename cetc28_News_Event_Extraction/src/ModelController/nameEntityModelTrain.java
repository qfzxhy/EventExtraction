package ModelController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cetc28.java.config.FileConfig;
import cetc28.java.eventdetection.entity_extraction.Evaluate;
import cetc28.java.eventdetection.entity_extraction.Learn;
import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.eventdetection.entity_extraction.Util;

/**
 * 
 * @author qianf 
 * 3个功能：1、训练模型 2、命名实体识别 3、对模型进行评估
 */
public class nameEntityModelTrain {
	/**
	 * 
	 * @param learner  模型训练类
	 * @param trainDataPath  训练数据路径
	 * @param modelPath  模型保存路径
	 */
	public void trainNerModel(String trainDataPath, String modelPath)
	{
		//if(learner == null)System.out.println("learner cannot null");
		Learn.NERTrain(trainDataPath, modelPath);
	}
	/**
	 * 
	 * @param ner 命名实体工具
	 * @param sentence 测试句子
	 */
	public String ner(String sentence)
	{
		if(sentence == null){System.out.println("sentence cannot null");}
		return Ner.ner(sentence);
	}
	/**
	 * 
	 * @param evalutor 评估类
	 * @param testDataPath 测试数据路径
	 * @param predictfile 预测数据路径
	 */
	public void evaluation( String testfile,String predictfile)
	{
		
		this.nerAll(testfile, predictfile);
		Evaluate evaluator2 = new Evaluate(testfile, predictfile);
		evaluator2.evaluate();
	}
	private void nerAll(String testfile, String predictfile) {
		// TODO Auto-generated method stub
		List<String> sentenceList = Util.loadfile(testfile);
		List<String> predictList = new ArrayList<>();
		for(String sentence : sentenceList)
		{
			predictList.add(Ner.ner2(sentence));
		}
		Util.store(predictfile, predictList);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		nameEntityModelTrain nc = new nameEntityModelTrain();
		/*
		 * 模型评估
		 */
		nc.trainNerModel(FileConfig.getNerTrainDataPath(),	 FileConfig.getNerModelPath());
		nc.evaluation(FileConfig.getNerTestDataPath(), "predictfile");
		
		/*
		 * 模型训练
		 */
		
	}

}
