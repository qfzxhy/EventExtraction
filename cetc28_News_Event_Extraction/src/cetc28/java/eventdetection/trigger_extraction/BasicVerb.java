package cetc28.java.eventdetection.trigger_extraction;

import java.util.HashMap;

public class BasicVerb {
	public String bv;
	public HashMap<String, Integer> pos;
	public HashMap<String, Integer> verbStructure;
	public HashMap<String, int[]> verbStructureType;
	//public int[] type;
	public int maxType = -1;
	public BasicVerb(String bv) {
		// TODO Auto-generated constructor stub
		this.bv = bv;
		pos = new HashMap<>();
		verbStructure = new HashMap<>();
		verbStructureType = new HashMap<>();
		//type = new int[20];
	}
	public void setPos(String pos)
	{
		this.pos.put(pos, 1);
	}
	public void setVerbStructure(String structure,int eventType)
	{
		if(verbStructureType.containsKey(structure))
		{
			verbStructureType.get(structure)[eventType-1]++;
		}else
		{
			int[] type = new int[20];
			type[eventType-1]++;
			verbStructureType.put(structure, type);
		}
		if(!verbStructure.containsKey(structure))
		{
			verbStructure.put(structure, eventType);
		}else
		{
			int type = verbStructure.get(structure);//当前最佳........类别..........good?????
			if(verbStructureType.get(structure)[type-1]<verbStructureType.get(structure)[eventType-1])
			{
				verbStructure.put(structure, eventType);
			}
		}
	}
	public int getEventType(String verbStructure)
	{
		return this.verbStructure.get(verbStructure);
	}
	public boolean isCandidate(String verbStructure,String pos)//
	{
		if(this.verbStructure.containsKey(verbStructure)&&this.pos.containsKey(pos.substring(0, 1)))
		{
			return true;
		}
		return false;
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
