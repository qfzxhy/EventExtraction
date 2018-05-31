package cetc28.java.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;
/**
 * 数据库以及solr配置文件信息获取
 * @author qianf
 *
 */
public class DBConfig {
	public static String dbConfigPath = "src/dbconfig.properties";
	private static Properties props = new Properties();
	static
	{
		try {
			props.load(new FileInputStream(new File(dbConfigPath)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * 
	 * @return zookeeper_hosts
	 */
	public static String getZOOKEEPER_HOSTS()
	{
		return props.getProperty("ZOOKEEPER_HOSTS");
	}
	/**
	 * 
	 * @return 爬虫solr库名
	 */
	public static String getDEFAULT_COLLECTION_CRAWLER()
	{
		return props.getProperty("DEFAULT_COLLECTION_CRAWLER");
	}
	/**
	 * 
	 * @return 地理知识库solr名
	 */
	public static String getDEFAULT_COLLECTION_GEOENCODE()
	{
		return props.getProperty("DEFAULT_COLLECTION_GEOENCODE");
	}
	/**
	 * 
	 * @return mysql url
	 */
	public static String getMysqlUrl()
	{
		return props.getProperty("mysql_url");
	}
	/**
	 * 
	 * @return oracle url
	 */
	public static String getOracleUrl()
	{
		return props.getProperty("oracle_url");
	}
	/**
	 * 
	 * @return mysql password
	 */
	public static String getMysqlPassword()
	{
		return props.getProperty("mysql_password");
	}
	/**
	 * 
	 * @return mysql user
	 */
	public static String getMysqlRoot()
	{
		return props.getProperty("mysql_root");
	}
	/**
	 * 
	 * @return oracle password
	 */
	public static String getOraclePassword()
	{
		return props.getProperty("oracle_password");
	}
	/**
	 * 
	 * @return oracle user name
	 */
	public static String getOracleRoot()
	{
		return props.getProperty("oracle_root");
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println(DBConfig.getDEFAULT_COLLECTION_CRAWLER());
	}

}
