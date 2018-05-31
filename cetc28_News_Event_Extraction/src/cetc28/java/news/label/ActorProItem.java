package cetc28.java.news.label;
//存储实体经纬度、实体的国家代码、地区代码、国家adm1、地点的complete值
public class ActorProItem {
	
	public String sourceActor_countrycode = "";//和adm是一个东西
	public String sourceActor_adm1 = "";
	public String sourceActor_longitude = "";
	public String sourceActor_latitude = "";
	public String sourceActor_countryadm = "";//国家编码
	public String sourceActor_regionadm = "";//地区编码
	public String completename = "";
	public String actorRole = "";
	
	/**
	 * 查找solr的关键词
	 */
	public String locforSolr = "";//
	/**
	 * 用来查找经纬度actor的原始值
	 */
	public String locRawName = "";//
	
	
	public void print(){
		System.out.println("countrycode:"+this.sourceActor_countrycode);
		System.out.println("adm1:"+this.sourceActor_adm1);
		System.out.println("longitude:"+this.sourceActor_longitude);
		System.out.println("latitude:"+this.sourceActor_latitude);
		System.out.println("countryadm:"+this.sourceActor_countryadm);
		System.out.println("regionadm:"+this.sourceActor_regionadm);
		System.out.println("actorRole:"+this.actorRole);
		System.out.println("completename:"+this.completename);
		System.out.println("locforSolr:" + this.locforSolr);
		System.out.println("locRawName:"+this.locRawName);
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	public void removeNull(ActorProItem actorProItem) {
		// TODO Auto-generated method stub
		if (actorProItem != null) {
			actorProItem.sourceActor_countryadm = actorProItem.sourceActor_countryadm == null ? ""
					: actorProItem.sourceActor_countryadm;
			actorProItem.sourceActor_countrycode = actorProItem.sourceActor_countrycode == null ? ""
					: actorProItem.sourceActor_countrycode;
			actorProItem.sourceActor_regionadm = actorProItem.sourceActor_regionadm == null ? ""
					: actorProItem.sourceActor_regionadm;
			actorProItem.sourceActor_adm1 = actorProItem.sourceActor_adm1 == null ? "" : actorProItem.sourceActor_adm1;
			actorProItem.sourceActor_latitude = actorProItem.sourceActor_latitude == null ? ""
					: actorProItem.sourceActor_latitude;
			actorProItem.sourceActor_longitude = actorProItem.sourceActor_longitude == null ? ""
					: actorProItem.sourceActor_longitude;
			actorProItem.actorRole = actorProItem.actorRole == null ? ""
					: actorProItem.actorRole;
		}
	}

}
