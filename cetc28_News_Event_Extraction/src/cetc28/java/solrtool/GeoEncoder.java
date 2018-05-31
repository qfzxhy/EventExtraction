package cetc28.java.solrtool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.beans.Field;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;


import cetc28.java.config.DBConfig;

/**
 * @author qianf
 *
 */
public class GeoEncoder {

	public GeoEncodeSearch solrSearch = null;
	public static final String alternate_ch_names = "alternate_ch_names";

	public GeoEncoder() {
		// TODO Auto-generated constructor stub
		solrSearch = new GeoEncodeSearch();
	}

	public GeoEncoder(String zkHosts, String collname) {
		// TODO Auto-generated constructor stub
		solrSearch = new GeoEncodeSearch(zkHosts, collname);
	}

	public void close() throws IOException {
		solrSearch.closeConnection();
	}

	public static void main(String[] args) {
//		GeoEncoder geo = new GeoEncoder(DBConfig.getZOOKEEPER_HOSTS(),DBConfig.getDEFAULT_COLLECTION_GEOENCODE());
//		try {
//			// SearchResultWithFacet places =
//			// geo.searchPlacesFacetForGivenName("",
//			// 20,"featureclass","featurecode","countrycode");
//			// places.printFacetMap();
//
//			System.out.println(geo.searchTopPlaceForGivenName("中国"));
//			// geo.addAlternativeName("172957", Arrays.asList(new String[] {
//			// "拉卡" }));
//			geo.close();
//		} catch (SolrServerException | IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	public Place searchTopPlaceForGivenName_exact(String name) throws SolrServerException, IOException{
		SolrQuery sq = new SolrQuery();
		sq.setParam("defType", "myqp");
		sq.setQuery("\""+name+"\"");
		sq.setStart(0);
		sq.setRows(1);
		List<Place> places = solrSearch.searchByQuery(sq);
		if(places.size()==0)
			return null;
		return places.get(0);
	}
	
	public Place searchTopPlaceForGivenName_inexact(String name) throws SolrServerException, IOException{
		SolrQuery sq = new SolrQuery();
		sq.setParam("defType", "myqp");
		sq.setQuery(""+name+"");
		sq.setStart(0);
		sq.setRows(1);
		List<Place> places = solrSearch.searchByQuery(sq);
		if(places.size()==0)
			return null;
		return places.get(0);
	}


//	public Place searchTopPlaceForGivenName(String name) throws SolrServerException, IOException{
//		SolrQuery sq = new SolrQuery();
//		sq.setParam("defType", "myqp");
//		sq.setQuery("\""+name+"\"");
//		sq.setStart(0);
//		sq.setRows(1);
//		List<Place> places = solrSearch.searchByQuery(sq);
//		if(places.size()==0)
//			return null;
//		return places.get(0);
//	}
	
	public void insertNewIndex(Place place) throws SolrServerException, IOException {
		solrSearch.insertIndex(place);
	}

	public List<Place> searchPlacesNoFacetForGivenName(String name, int maxnum)
			throws SolrServerException, IOException {
		SolrQuery sq = new SolrQuery();
		sq.setParam("defType", "myqp");
		sq.setQuery(name);
		sq.setStart(0);
		sq.setRows(maxnum);
		return solrSearch.searchByQuery(sq);
	}

	public SearchResultWithFacet searchPlacesFacetForGivenName(String name, int maxnum, String... facetFields)
			throws SolrServerException, IOException {
		SolrQuery sq = new SolrQuery();
		sq.setParam("defType", "myqp");
		sq.setQuery(name);
		sq.setStart(0);
		sq.setRows(maxnum);
		sq.setFacet(true);
		if (facetFields.length == 0) {
			sq.addFacetField("featureclass");
		} else {
			for (String field : facetFields) {
				sq.addFacetField(field);
			}
		}
		SearchResultWithFacet result = new SearchResultWithFacet();
		if (solrSearch.searchByQuery(sq, result))
			return result;
		else
			return null;
	}

	public Place addAlternativeName(String id, List<String> alternatename) throws SolrServerException, IOException {
		List<Place> doclist = solrSearch.searchByID(id);
		Place doc = doclist.get(0);
		doc.getAlternate_names().addAll(alternatename);
		doc.setNum_alternate_names(doc.getAlternate_names().size());
		solrSearch.insertIndex(doc);
		return doc;
	}

	/**
	 * @param id
	 * @param num
	 * @return
	 * @throws SolrServerException
	 * @throws IOException
	 */
	public int changeUserFocus(String id, int num) throws SolrServerException, IOException {
		List<Place> doclist = solrSearch.searchByID(id);
		Place doc = doclist.get(0);
		int focus = doc.getUser_focus_num();
		doc.setUser_focus_num(focus + num);
		UpdateResponse ur = solrSearch.insertIndex(doc);
		return ur.getStatus();
	}

	public SolrDocument getCoordinate(String name, String attri) {
		String[] fields = null;
		String[] keys = null;
		if (attri.equals("country")) {
			fields = new String[] { "alternatenames", "admin1code" };
			keys = new String[] { name, "00" };
		} else {
			fields = new String[] { alternate_ch_names };
			keys = new String[] { name };
		}
		SolrDocumentList list = solrSearch.searchByQuery(fields, keys, 0, 10, null, null);
		SolrDocument result = null;
		int maxLen = Integer.MIN_VALUE;
		for (SolrDocument doc : list) {
			String alternatenames = doc.get(alternate_ch_names).toString();
			if (alternatenames != null) {
				int len = alternatenames.split(",").length;
				maxLen = maxLen < len ? len : maxLen;
				result = doc;
			}
		}
		return result;
	}

	/**
	 * A JavaBean that represent a location which contains information about
	 * names in different languages, featureClass, latitude and longitude,etc
	 * 
	 * @author jxx
	 *
	 */
	public static class Place {
		@Field
		String id;
		@Field
		String name;
		@Field
		String asciiname;
		@Field
		ArrayList<String> alternate_names;
		@Field
		int num_alternate_names;
		@Field
		String featureclass;
		@Field
		String featurecode;
		@Field
		String latitude;
		@Field
		String longitude;
		@Field
		String location;
		@Field
		String countrycode;
		@Field
		String cc2;
		@Field
		String admin1code;
		@Field
		String admin2code;
		@Field
		String admin3code;
		@Field
		String admin4code;
		@Field
		long population;
		@Field
		String elevation;
		@Field
		String dem;
		@Field
		String timezone;
		@Field
		int user_focus_num;
		@Field
		String modificationdate;

		public int getNum_alternate_names() {
			return num_alternate_names;
		}

		public void setNum_alternate_names(int num_alternate_names) {
			this.num_alternate_names = num_alternate_names;
		}

		public String getModificationdate() {
			return modificationdate;
		}

		public void setModificationdate(String modificationdate) {
			this.modificationdate = modificationdate;
		}

		public Place() {
			this.id = UUID.randomUUID().toString();
			this.name = "";
			this.asciiname = "";
			this.alternate_names = new ArrayList<String>();
			this.featureclass = "";
			this.featurecode = "";
			this.latitude = "";
			this.longitude = "";
			this.location = "";
			this.countrycode = "";
			this.cc2 = "";
			this.admin1code = "";
			this.admin2code = "";
			this.admin3code = "";
			this.admin4code = "";
			this.population = 0;
			this.elevation = "0";
			this.dem = "0";
			this.timezone = "";
			this.user_focus_num = 0;
		}


		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return "Place [id=" + id + ", name=" + name + ", asciiname=" + asciiname + ", alternate_names="
					+ alternate_names + ", featureClass=" + featureclass + ", featureCode=" + featurecode
					+ ", latitude=" + latitude + ", longitude=" + longitude + ", location=" + location
					+ ", countrycode=" + countrycode + ", cc2=" + cc2 + ", admin1code=" + admin1code + ", admin2code="
					+ admin2code + ", admin3code=" + admin3code + ", admin4code=" + admin4code + ", population="
					+ population + ", elevation=" + elevation + ", dem=" + dem + ", timezone=" + timezone
					+ ", user_focus_num=" + user_focus_num + "]";
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getAsciiname() {
			return asciiname;
		}

		public void setAsciiname(String asciiname) {
			this.asciiname = asciiname;
		}

		public ArrayList<String> getAlternate_names() {
			return alternate_names;
		}

		public void setAlternate_names(ArrayList<String> alternate_names) {
			this.alternate_names = alternate_names;
		}

		public void appendAlternate_names(String[] names) {
			for (String name : names) {
				if (!   this.alternate_names.contains(name)) {
					this.alternate_names.add(name);
				}
			}
		}

		public void removeAlternate_names(String[] names){
			for (String name : names) {
				if (this.alternate_names.contains(name)) {
					this.alternate_names.remove(name);
				}
			}
		}
		public String getFeatureClass() {
			return featureclass;
		}

		public void setFeatureClass(String featureClass) {
			this.featureclass = featureClass;
		}

		public String getFeatureCode() {
			return featurecode;
		}

		public void setFeatureCode(String featureCode) {
			this.featurecode = featureCode;
		}

		public String getLatitude() {
			return latitude;
		}

		public void setLatitude(String latitude) {
			this.latitude = latitude;
		}

		public String getLongitude() {
			return longitude;
		}

		public void setLongitude(String longitude) {
			this.longitude = longitude;
		}

		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}

		public String getCountrycode() {
			return countrycode;
		}

		public void setCountrycode(String countrycode) {
			this.countrycode = countrycode;
		}

		public String getCc2() {
			return cc2;
		}

		public void setCc2(String cc2) {
			this.cc2 = cc2;
		}

		public String getAdmin1code() {
			return admin1code;
		}

		public void setAdmin1code(String admin1code) {
			this.admin1code = admin1code;
		}

		public String getAdmin2code() {
			return admin2code;
		}

		public void setAdmin2code(String admin2code) {
			this.admin2code = admin2code;
		}

		public String getAdmin3code() {
			return admin3code;
		}

		public void setAdmin3code(String admin3code) {
			this.admin3code = admin3code;
		}

		public String getAdmin4code() {
			return admin4code;
		}

		public void setAdmin4code(String admin4code) {
			this.admin4code = admin4code;
		}

		public long getPopulation() {
			return population;
		}

		public void setPopulation(long population) {
			this.population = population;
		}

		public String getElevation() {
			return elevation;
		}

		public void setElevation(String elevation) {
			this.elevation = elevation;
		}

		public String getDem() {
			return dem;
		}

		public void setDem(String dem) {
			this.dem = dem;
		}

		public String getTimezone() {
			return timezone;
		}

		public void setTimezone(String timezone) {
			this.timezone = timezone;
		}

		public int getUser_focus_num() {
			return user_focus_num;
		}

		public void setUser_focus_num(int user_focus_num) {
			this.user_focus_num = user_focus_num;
		}
	}

	public static class SearchResultWithFacet {
		Map<String, LinkedHashMap<String, Long>> facetMap = new LinkedHashMap<String, LinkedHashMap<String, Long>>();

		public Map<String, LinkedHashMap<String, Long>> getFacetMap() {
			return facetMap;
		}

		public void printFacetMap() {
			for (String field : facetMap.keySet()) {
				System.out.println("----------------------" + field + "------------------------");
				LinkedHashMap<String, Long> fieldmap = facetMap.get(field);
				for (Entry<String, Long> entry : fieldmap.entrySet()) {
					System.out.println("\t" + entry.getKey() + "\t" + entry.getValue());
				}
				System.out.println();
			}
		}

		public void printPlaces() {
			for (Place p : places) {
				System.out.println(p);
			}
		}

		List<Place> places = new ArrayList<Place>();

		public List<Place> getPlaces() {
			return places;
		}

		public void setPlaces(List<Place> places) {
			this.places = places;
		}

		long totalnumfound = 0;

		public void appendFacetMap(String facetField, String name, long value) {
			LinkedHashMap<String, Long> map = facetMap.get(facetField);
			if (map == null) {
				map = new LinkedHashMap<String, Long>();
				facetMap.put(facetField, map);
			}
			map.put(name, value);
		}

		public long getTotalnumfound() {
			return totalnumfound;
		}

		public void setTotalnumfound(long totalnumfound) {
			this.totalnumfound = totalnumfound;
		}

	}
}
