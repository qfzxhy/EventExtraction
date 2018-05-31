package cetc28.java.news.label;

public class EventActorItem {
	public String sourceActor;
	public String getSourceActor() {
		return sourceActor;
	}
	public void setSourceActor(String sourceActor) {
		this.sourceActor = sourceActor;
	}
	public String getSourceActorPro() {
		return sourceActorPro;
	}
	public void setSourceActorPro(String sourceActorPro) {
		this.sourceActorPro = sourceActorPro;
	}
	public String getTargetActor() {
		return targetActor;
	}
	public void setTargetActor(String targetActor) {
		this.targetActor = targetActor;
	}
	public String getTargetActorPro() {
		return targetActorPro;
	}
	public void setTargetActorPro(String targetActorPro) {
		this.targetActorPro = targetActorPro;
	}
	public String getTriggerWord() {
		return triggerWord;
	}
	public void setTriggerWord(String triggerWord) {
		this.triggerWord = triggerWord;
	}
	public ActorItem getAllActor() {
		return allActor;
	}
	public void setAllActor(ActorItem allActor) {
		this.allActor = allActor;
	}
	ActorItem allActor;
	public String sourceActorPro;
	public String targetActor;
	public String targetActorPro;
	String triggerWord;
	public void print() {
		// TODO Auto-generated method stub
		System.out.println("EventActorItem结果：");
		System.out.println("getSourceActor:"+this.getSourceActor());
		System.out.println("getSourceActorPro:"+this.getSourceActorPro());
		System.out.println("getTargetActor:"+this.getTargetActor());
		System.out.println("getTargetActorPro:"+this.getTargetActorPro());
		System.out.println("triggerWord:"+this.triggerWord);
	}


}
