package cetc28.java.solrtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;

import cetc28.java.config.FileConfig;
import cetc28.java.nlptools.Pair;

public class PartCountryTable {
	HashMap<String, String[]> tables_map = null;
	ArrayList<Pair<String, String[]>> tables_list = null;
	public PartCountryTable(int lineSplitNum) {
		// TODO Auto-generated constructor stub
		tables_map = new HashMap<>();
		tables_list  = new ArrayList<>();
		try {
			intial(lineSplitNum);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void intial(int lineSplitNum) throws IOException {
		// TODO Auto-generated method stub
		String countryTablePath = FileConfig.getCountryTablePath();
		BufferedReader br = null;
		br = new BufferedReader(new FileReader(new File(countryTablePath)));
		String line = "";
		while((line = br.readLine()) != null)
		{
			String[] strs = line.split("\t");
			if(strs.length != lineSplitNum) continue;
			if(strs[0].indexOf(".*") != -1)
			{
				tables_list.add(new Pair<String, String[]>(strs[0], Arrays.copyOfRange(strs, 1, strs.length)));
			}else
				tables_map.put(strs[0], Arrays.copyOfRange(strs, 1, strs.length));
		}
		br.close();
	}
	/**
	 * 
	 * @param name 国家缩写
	 * @return 国家名
	 */

	public String getCountryName(String name)
	{
		if(name == null) return null;
		if(tables_map.containsKey(name))
		{
			return tables_map.get(name)[0];
		}
		for(int i=0;i< tables_list.size();i++)
		{
			Pair<String, String[]> entry =  tables_list.get(i);
			if(name.matches(entry.getKey()))
			{
				return entry.getSecond()[0];
			}
		}
		return null;
	}
	/**
	 * 
	 * @param name 国家、组织、设备
	 * @return	代表的角色（政府 、 反动军 、 恐怖分子）
	 */
	public String getRole(String name)
	{
		if(name == null) return null;
		if(tables_map.containsKey(name))
		{
			return tables_map.get(name)[1];
		}
		for(int i=0;i< tables_list.size();i++)
		{
			Pair<String, String[]> entry =  tables_list.get(i);
			if(name.matches(entry.getKey()))
			{
				return entry.getSecond()[1];
			}
		}
		return null;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PartCountryTable pt = new PartCountryTable(3);
		System.out.println(pt.getCountryName("平民"));
	}

}

