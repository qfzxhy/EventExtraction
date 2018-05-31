package cetc28.java.eventdetection.entity_extraction;



import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;



public class Entity {
	public static HashMap<String, Float> scoreMap;
	public static List<String> sentenceList;
	public  HashMap<String, Boolean> entity_keyWord;
	public  HashMap<String, Boolean> entity_all;
	public  int entityMaxLen = -1;
	
	public static boolean flag_dev = false;
	public static HashMap<String, Boolean> dev_keyWord;
	public static HashMap<String, Boolean> dev_all;
	private static int devEntityMaxLen = -1;
	
	public static boolean flag_role = false;
	public static HashMap<String, Boolean> role_keyWord;
	public static HashMap<String, Boolean> role_all;
	private static int roleEntityMaxLen = -1;
	
	public static boolean flag_person = false;
	public static HashMap<String, Boolean> person_keyWord;
	public static HashMap<String, Boolean> person_all;
	private static int personEntityMaxLen = -1;
	
	public static boolean flag_region = false;
	public static HashMap<String, Boolean> region_keyWord;
	public static HashMap<String, Boolean> region_all;
	private static int regionEntityMaxLen = -1;
	
	public static boolean flag_country = false;
	public static HashMap<String, Boolean> country_keyWord;
	public static HashMap<String, Boolean> country_all;
	private static int countryEntityMaxLen = -1;
	
	public static boolean flag_org = false;
	public static HashMap<String, Boolean> org_keyWord;
	public static HashMap<String, Boolean> org_all;
	private static int orgEntityMaxLen = -1;
	private String entityType;
	public Entity(String entityType) {
		// TODO Auto-generated constructor stub

		this.entityType = entityType;
		if(!flag_dev && !flag_country && !flag_org && !flag_person && !flag_role && !flag_region)
		{
			scoreMap = new HashMap<>();
			System.out.println(Ner.trainDataPath);
			sentenceList = Util.loadfile(Ner.trainDataPath);
		}
		if(entityType.equals("dev")){
			
			
			if(!flag_dev){
				String keyWordPath = Ner.entityFilePath + "/" + "entity_keyword/device.txt";
				String entityPath = Ner.entityFilePath + "/" + "entity_dic/device_entity.txt";
				System.out.println("device loaded");
				dev_keyWord = new HashMap<>();
				dev_all = new HashMap<>();
				devEntityMaxLen = loadData(keyWordPath,entityPath,dev_keyWord, dev_all);
				flag_dev = true;
			}
			entity_keyWord = dev_keyWord;
			entity_all = dev_all;
			entityMaxLen = devEntityMaxLen;
		}
		if(entityType.equals("role")){
			
			if(!flag_role){
				String keyWordPath = Ner.entityFilePath + "/" + "entity_keyword/role.txt";
				String entityPath = Ner.entityFilePath + "/" + "entity_dic/role_entity.txt";
				System.out.println("role loaded");
				role_keyWord = new HashMap<>();
				role_all = new HashMap<>();
				roleEntityMaxLen = loadData(keyWordPath,entityPath,role_keyWord, role_all);
				flag_role = true;
			}
			entity_keyWord = role_keyWord;
			entity_all = role_all;
			entityMaxLen = roleEntityMaxLen;
		}
		if(entityType.equals("nr")){
			
			if(!flag_person){
				String keyWordPath = Ner.entityFilePath + "/" + "entity_keyword/person.txt";
				String entityPath = Ner.entityFilePath + "/" + "entity_dic/person_entity.txt";
				System.out.println("person loaded");
				person_keyWord = new HashMap<>();
				person_all = new HashMap<>();
				personEntityMaxLen = loadData(keyWordPath,entityPath,person_keyWord, person_all);
				flag_person = true;
			}
			entity_keyWord = person_keyWord;
			entity_all = person_all;
			entityMaxLen = personEntityMaxLen;
		}
		if(entityType.equals("ns")){
			if(!flag_region){
				String keyWordPath = Ner.entityFilePath + "/" + "entity_keyword/region.txt";
				String entityPath = Ner.entityFilePath + "/" + "entity_dic/region_entity.txt";
				System.out.println("region loaded");
				region_keyWord = new HashMap<>();
				region_all = new HashMap<>();
				regionEntityMaxLen = loadData(keyWordPath,entityPath,region_keyWord, region_all);
				flag_region = true;
			}
			entity_keyWord = region_keyWord;
			entity_all =region_all;
			entityMaxLen = regionEntityMaxLen;
		}		
		if(entityType.equals("country")){
			if(!flag_country){
				String keyWordPath = Ner.entityFilePath + "/" + "entity_keyword/country.txt";
				String entityPath = Ner.entityFilePath + "/" + "entity_dic/country_entity.txt";
				System.out.println("country loaded");
				country_keyWord = new HashMap<>();
				country_all = new HashMap<>();
				countryEntityMaxLen = loadData(keyWordPath,entityPath,country_keyWord, country_all);
				flag_country = true;
			}
			entity_keyWord = country_keyWord;
			entity_all =country_all;
			entityMaxLen = countryEntityMaxLen;
		}		
		if(entityType.equals("nt")){
			if(!flag_org){
				String keyWordPath = Ner.entityFilePath + "/" + "entity_keyword/orgnization.txt";
				String entityPath = Ner.entityFilePath + "/" + "entity_dic/orgnization_entity.txt";
				System.out.println("org loaded");
				org_keyWord = new HashMap<>();
				org_all = new HashMap<>();
				orgEntityMaxLen = loadData(keyWordPath,entityPath,org_keyWord, org_all);
				//System.out.println();
				flag_org = true;
			}
			entity_keyWord = org_keyWord;
			entity_all = org_all;
			entityMaxLen = orgEntityMaxLen;
		}	
		
			
	}
	private int loadData(String path1, String path2, HashMap<String, Boolean> keyWordMap, HashMap<String, Boolean> allMap) {
		// TODO Auto-generated method stub
		
		Util.loadfileMap(path1, keyWordMap);
		Util.loadfileMap(path2, allMap);
		if(path1.endsWith("device.txt"))
			for(Map.Entry<String, Boolean> entry : keyWordMap.entrySet())
			{
				allMap.put(entry.getKey(), true);
			}
		int max = -1;
		for(Entry<String, Boolean> entry : allMap.entrySet())
		{
			if(entry.getKey().length() > max)
			{
				max = entry.getKey().length();
			}
		}
		return max;
	}
	/**
	 * 后处理 （最大正相匹配 并且 实体表中实体能够覆盖当前标签所对应的实体）
	 * @param wordList
	 */
	public void postProcessing(List<String[]> wordList)
	{
		if(entityMaxLen <= 0 ){
			System.out.println(entityType+entityMaxLen);
			System.err.println("entityMaxLen error");
		}
		for(int i = 0; i < wordList.size(); i++)
		{
			if(wordList.get(i)[2].startsWith("i")){
				continue;
			}
			String entity = "";
			int j = i;
			while(j < wordList.size() && entity.length() < entityMaxLen){
				if(entity.length() + wordList.get(j)[0].length() > entityMaxLen){
					break;
				}
				entity += wordList.get(j++)[0];
			}
			if(j-- == i)continue;
			if(entity_all.containsKey(entity) && (j == wordList.size()-1 || !wordList.get(j+1)[2].startsWith("i"))){
				for(int k=i; k<=j; k++)
				{
					if(k == i){
						wordList.get(k)[2] = "b_"+entityType;
					}else
						wordList.get(k)[2] = "i_"+entityType;
				}
				i = j;//很重important
				continue;
			}
			String entity_temp = entity;
			for(int k = j; k>i; k--)
			{
				entity_temp = entity_temp.substring(0, entity_temp.length()-wordList.get(k)[0].length());
				if(entity_all.containsKey(entity_temp) && (!wordList.get(k)[2].startsWith("i"))){
					//System.out.println(entity_temp);
					for(int kk=i; kk<k; kk++)
					{
						if(kk == i){
							wordList.get(kk)[2] = "b_"+entityType;
						}else
							wordList.get(kk)[2] = "i_"+entityType;
					}
					i = k - 1;//很重
					break;
				}
				
			}
		}
	}
	/**
	 * 
	 * @param word 当前词
	 * @return 当前词是核心词的概率。
	 */
	public String getKeyWordScore(String word)
	{
		
		float score = 0.0f;
		if(entity_keyWord.containsKey(word))
		{
			return "Y";
		}
		for(int i = 1; i<word.length(); i++)
		{
			if(entity_keyWord.containsKey(word.substring(i)))
			{
				score = getScore(word.substring(i),word);
				break;
			}
		}
		
		if(score > 0.0)//H M L
		{
			if(score > 0.5)return "H";
			else
				if(score > 0.3) return "M";
				else
					return "L";
		}else
			return "N";
	}
	private float  getScore(String wordSubstr, String word) {//log(N+2)/log(N_+2)
		// TODO Auto-generated method stub
		float score = (float) (wordSubstr.length()*1.0/word.length());
		int N = 0, N_ = 0;
		if(!scoreMap.containsKey(wordSubstr))
		{
			for(String sentence : sentenceList)
			{
				String[] wordInforArray = sentence.split("\\s+");
				for(String wordInfor : wordInforArray)
				{
					String wordStr = wordInfor.split("/")[0];
					if(wordStr.endsWith(wordSubstr))
					{
						++N_;
						if(wordInfor.split("/")[2].endsWith(entityType))
						{
							++N;
						}
					}
				}
			}
			score *= (Math.log(N+2)/Math.log(N_+2));
			scoreMap.put(wordSubstr, score);
		}
		return score;
	}
	/**
	 * 当前词是不是核心词
	 * @param word 
	 * @return true or false
	 */
	public boolean isKeyWord(String word)
	{
		if(entity_keyWord.containsKey(word)){
			return true;
		}
		return false;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		Entity perEntity = new Entity("per");
		
		List<String[]> wordList = new ArrayList<String[]>();;//范长/b_per 龙晤印/i_per 防长/b_role
		String[] s1 = new String[]{"范长","ns","b_country"};
		String[] s2 = new String[]{"龙晤印","ns","b_country"};
		String[] s3 = new String[]{"防长","ns","b_country"};
		String[] s4 = new String[]{"习","ns","b_country"};
		String[] s5 = new String[]{"主席","ns","b_dev"};
		String[] s6 = new String[]{"习近平","ns","b_dev"};
		wordList.add(s1);
		wordList.add(s2);
		wordList.add(s3);
		wordList.add(s4);
		wordList.add(s5);
		wordList.add(s6);
		perEntity.postProcessing(wordList);
		for(String[] s : wordList)
		{
			System.out.println(s[2]);
		}
		//deviceEntity.print();
	}
	private void print() {
		// TODO Auto-generated method stub
		Iterator<Entry<String, Boolean>> iter = entity_keyWord.entrySet().iterator();
		while(iter.hasNext())
		{
			System.out.println(iter.next().getKey());
		}
	}

}
