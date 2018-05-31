package ModelController;

import cetc28.java.config.FileConfig;

public class ModelTrainer
{
	/**
	 * 事件抽取训练文件：data/event/trainDataByQianf 触发词列表文件：data/event/trigger
	 * 当修改{事件抽取训练文件} {触发词列表}中数据时，需要调用该方法重新训练模型
	 */
	public void eventModelTrain()
	{
		eventTriggerClassifierTrain mc = new eventTriggerClassifierTrain();
		mc.trainModel1(FileConfig.getEventTrainDataPath(), FileConfig.getMaxentModel1Path());
		mc.trainModel2(FileConfig.getEventTrainDataPath(), FileConfig.getMaxentModel2Path());
		eventTypeModelTrain m = new eventTypeModelTrain();
		m.eventTypeModel_train(FileConfig.getEventTrainDataPath(), FileConfig.getEventTypeModelPath());
		m.eventTypeThreshold_train(FileConfig.getTriggerPath(), FileConfig.getThresholdPath());
	}

	/**
	 * 命名实体训练文件：data/entity/trainCorpus 当修改命名实体训练文件中数据时，需要调用该方法重新训练模型
	 */
	public void nameEntityModelTrain()
	{
		nameEntityModelTrain nc = new nameEntityModelTrain();
		nc.trainNerModel(FileConfig.getNerTrainDataPath(), FileConfig.getNerModelPath());
	}

	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		long time1 = System.currentTimeMillis(); 
		ModelTrainer trainer = new ModelTrainer();
//		trainer.eventModelTrain();
		trainer.nameEntityModelTrain();
		long time2 = System.currentTimeMillis();
		System.out.println((time2 - time1)*1.0/1000);
	}

}
