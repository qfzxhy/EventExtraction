package cetc28.java.news.label;
import edu.hit.ir.ltp4j.Pair;

public class ActorItem {
	/*
	 * 标注数据
	 * */
	public String actor = "";
	public String actorPro = "";
	public String index ;
	public String len;
	
	public String getActor() {
		return actor;
	}
	public void setActor(String actor) {
		this.actor = actor;
	}
	public String getActorPro() {
		return actorPro;
	}
	public void setActorPro(String actorPro) {
		this.actorPro = actorPro;
	}
	public String getIndex() {
		return index;
	}
	public void setIndex(String index) {
		this.index = index;
	}
	public String getLen() {
		return len;
	}
	public void setLen(String len) {
		this.len = len;
	}
	
	public ActorItem (String actor,String actorPro,String index,String len) {
		setActor(actor);
		setActorPro(actorPro);
		setIndex(index);
		setLen(len);
	}
	public ActorItem(){
		
	}
	public void Print()
	{
		System.out.println("actor: "+actor);
		System.out.println("actorPro: "+actorPro);
		System.out.println("index: "+index);
		System.out.println("len: "+len);
	}
	
}
