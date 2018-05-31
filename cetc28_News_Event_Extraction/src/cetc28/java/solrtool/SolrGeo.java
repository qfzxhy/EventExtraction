package cetc28.java.solrtool;

import java.io.IOException;
import java.util.List;

import org.apache.solr.client.solrj.SolrServerException;

import cetc28.java.config.DBConfig;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import cetc28.java.solrtool.GeoEncoder.Place;

public class SolrGeo {
	public GeoEncoder geoSolrSearch = null;//地理经纬度solr搜索
	public CountryNameTable abbreviationTable = null;
	public PartCountryTable partCountryTable = null;
	public SolrGeo() {
		// TODO Auto-generated constructor stub
		geoSolrSearch = new GeoEncoder(DBConfig.getZOOKEEPER_HOSTS(),DBConfig.getDEFAULT_COLLECTION_GEOENCODE());;
		abbreviationTable = new CountryNameTable(3);
		partCountryTable = new PartCountryTable(3);
	}
	/**
	 * 
	 * @param name
	 * @param i	0:精确匹配 1：模糊匹配
	 * @return
	 */
	private Place getCoordinate(String name,int i)
	{
		Place result = null;
		if(name == null || name.trim().length() == 0) return null;
		if(i == 0)
		{
			try {
				result = geoSolrSearch.searchTopPlaceForGivenName_exact(name);
			} catch (SolrServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else
		if(i == 1)
		{
			try {
				result = geoSolrSearch.searchTopPlaceForGivenName_inexact(name);
			} catch (SolrServerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return result;
	}
	public Place getCoordinate(String name,String attri, Pair<String, String> altenatenames)
	{
		Place place = null;
		if(attri.equals("person"))
		{
			String name_temp = partCountryTable.getCountryName(name);
			if(name_temp != null)
			{
				place = getCoordinate(name_temp,0);
				if(place != null)
				{
					altenatenames.first = name_temp;
					altenatenames.second = name;
				}
			}
		}else
		{
			String name_temp = partCountryTable.getCountryName(name);
			if(name_temp == null){
				if(attri.equals("organization") || attri.equals("role") || attri.equals("device") || (attri.equals("country") && name.length() == 1)) 
				name_temp = abbreviationTable.getCountryName(name);
				}
			if(name_temp != null)
			{
				place = getCoordinate(name_temp,0);
				if(place != null)
				{
					altenatenames.first = name_temp;
					altenatenames.second = name;
				}
			}else
			{
				String regionName = extract_location_helper1(name);
				if(regionName != null)//如果实体中有地名------》精确匹配
				{
					String regionName_temp  = partCountryTable.getCountryName(regionName);
					if(regionName_temp != null)
						place = getCoordinate(regionName_temp,0);
					else
						place = getCoordinate(regionName,1);
					if(place != null)
					{
						altenatenames.first = regionName_temp != null ? regionName_temp : regionName;
						altenatenames.second = name;
					}
				}else
				{
					if(attri.equals("country") || attri.equals("region"))
					{
						place = getCoordinate(name,1);
//						System.out.println("place.alternate_names:"+place.alternate_names);
						if(place != null)
						{
							altenatenames.first = name;
							altenatenames.second = name;
						}
					}
				}
			}
		}
		return place;
		
	}
	private static String extract_location_helper1(String entity)
	{
		if(entity == null) return null;
		List<String> words = LtpTool.getWords(entity);
		List<String> tags = LtpTool.getPosTag(words);
		for(int i=0;i<words.size();i++)
		{
			if(tags.get(i).equals("ns"))
			{
				return words.get(i);
			}
		}
		return null;
	}
	public void close(){
		try {
			geoSolrSearch.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		SolrGeo solrUtil = new SolrGeo();
		Pair<String, String> altenatenames = new Pair<String, String>("","");
		Place doc = solrUtil.getCoordinate("俄特种部队", "organization", altenatenames);
		System.out.println(doc);
		solrUtil.close();
	}
}
