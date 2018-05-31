package cetc28.java.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
/**
 * 外部文件配置文件信息获取
 * @author qianf
 *
 */
public class FileConfig {
	/**
	 * 配置文件路径
	 */
	public static String fileConfigPath = "src/fileconfig.properties";
	private static Properties props = new Properties();
	private static String root = "";
	static
	{
		try {
			props.load(new FileInputStream(new File(fileConfigPath)));
			root = props.getProperty("root");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/*
	 * other tool model 
	 */
	public static String getSynonymDicPath()
	{
		return root + props.getProperty("synonymdic_path");
	}
	public static String getLtpPath()
	{
		return root + props.getProperty("ltp_path");
	}
	public static String getEmbeddingPath()
	{
		return root + props.getProperty("embedding_path");
	}
/*
 * trigger extraction model
 */
	public static String getMaxentModel1Path()
	{
		return root + props.getProperty("maxent_model1_path");
	}
	public static String getMaxentModel2Path()
	{
		return root + props.getProperty("maxent_model2_path");
	}
	public static String getEventTrainDataPath()
	{
		return root + props.getProperty("event_traindata_path");
	}
	public static String getEventTestDataPath()
	{
		return root + props.getProperty("event_testdata_path");
	}
	/*
	 * event type extraction model
	 */
	public static String getRulePath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("rule_path");
	}
	public static String getEventTypeModelPath()
	{
		return root + props.getProperty("triggerNum_path");
	}
	public static String getTriggerPath()
	{
		return root + props.getProperty("trigger_path");
	}
	public static String getThresholdPath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("thresholds_path");
	}
	
	public static String getUserDicPath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("user_dic_path");
	}
	public static String getAbbreviationPath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("abbreviation_path");
	}
	
	/*
	 * ner
	 */
	public static String getEntityPath()
	{
		return root + props.getProperty("entity_path");
	}
	public static String getNerModelPath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("ner_model_path");
	}
	public static String getNerTrainDataPath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("ner_trainData_path");
	}
	public static String getNerTestDataPath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("ner_testData_path");
	}
	
	
	//polarity
	public static String getPosPath()
	{
		return root + props.getProperty("pos_dic");
	}
	public static String getNegPath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("neg_dic");
	}
	public static String getsentPath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("sent_dic");
	}
	public static String getnotPath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("not_dic");
	}
	public static String getdegreePath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("degree_dic");
	}
		
	public static String getStopWordsPath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("stopwords_dic");
	}
	public static String gettotalActorPath() {
		// TODO Auto-generated method stub
		return  root + props.getProperty("totalActor_dic");
	}
	public static String getCountryTablePath() {
		// TODO Auto-generated method stub
		return root + props.getProperty("country_table_path");
	}
	
	/*
	 * main
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		System.out.println(FileConfig.getLtpPath());
//		System.out.println(FileConfig.getEventTypeModelPath());
		System.out.println(FileConfig.getPosPath());

	}
	public static String getLocStopWordsPath()
	{
		// TODO Auto-generated method stub
		return root + props.getProperty("location_stopword_path");
	}
	

}
