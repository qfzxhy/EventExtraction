package cetc28.java.eventdetection.trigger_extraction;

import java.util.ArrayList;
/**
 * 特征类
 * @author qf
 *
 */
public class Feature {
	public String label; // 标签，训练时有标签
	public String triggerWord;//触发词
	public ArrayList<WordInfo> wordFeatureList;//上下文特征
	public String headWord;//核心词
	public String semanticEntry;//语义信息
	public String left_entityType = null;//左边实体类别
	public String right_entityType = null;//右边实体类别
	public String bin1 = null;//字1
	public String bin2 = null;//字2
	public String bin1_SemanticEntry = null;//字1语义信息
	public String bin2_SemanticEntry = null;//字2语义信息
	public String triggerType = null;//触发词列表
	public Feature() {
		// TODO Auto-generated constructor stub
		wordFeatureList = new ArrayList<>();
	}
	public void setLabel(String label)
	{
		this.label = label;
	}
	public void setTrigger(String trigger)
	{
		this.triggerWord = trigger;
	}
	public void setHeadWord(String headword)
	{
		this.headWord = headword;
	}
	public void setLeftEntityType(String type){this.left_entityType = type;}
	public void setRightEntityType(String type){this.right_entityType = type;}
	public void setSemanticEntry(String entry){this.semanticEntry = entry;}
	public void setBin1(String bin1){this.bin1 = bin1;}
	public void setBin2(String bin2){this.bin2 = bin2;}
	public void setBin1_SemanticEntry(String entry){this.bin1_SemanticEntry = entry;}
	public void setBin2_SemanticEntry(String entry){this.bin2_SemanticEntry = entry;}
	public void set_triggerTypeFeature(int eventType) {this.triggerType = "triggerType:"+String.valueOf(eventType);}
		// TODO Auto-generated method stub
	public String[] toArray()
	{
		ArrayList<String> featureList = new ArrayList<>();
		for(int i = 0;i<wordFeatureList.size();i++)
		{
			WordInfo wordFeature = wordFeatureList.get(i);
			if(wordFeature.word!=null)
				featureList.add(wordFeature.word);
			if(wordFeature.lexical!=null)
				featureList.add(wordFeature.lexical);
			if(wordFeature.syntactic!=null)
				featureList.add(String.valueOf(wordFeature.syntactic));
			if(wordFeature.relation!=null)
				featureList.add(wordFeature.relation);
			if(wordFeature.semantic!=null)
				featureList.add(wordFeature.semantic);
			
		}
		if(headWord != null)
		{
			featureList.add(headWord);
		}
		if(left_entityType!=null)
		{
			featureList.add(left_entityType);
		}
		if(right_entityType!=null)
		{
			featureList.add(right_entityType);
		}
		if(semanticEntry !=null)
		{
			featureList.add(semanticEntry);
		}
		if(bin1!=null){featureList.add(bin1);featureList.add(bin2);}
		if(bin1_SemanticEntry!=null&&bin2_SemanticEntry!=null){featureList.add(bin1_SemanticEntry);featureList.add(bin2_SemanticEntry);}
		if(triggerType != null) featureList.add(triggerType);
		return featureList.toArray(new String[featureList.size()]);
	}
	
}
