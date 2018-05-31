package cetc28.java.eventdetection.trigger_extraction;

import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.nlptools.LtpTool;

public class PostProcessing {
	public void postProcessing(Data testData)//加入“不”等
	{
		String predictedTrigger = testData.data.triggerWord;
		int triggerPos = testData.triggerPos;
		//System.out.println("triggerPos:"+triggerPos);
		if(predictedTrigger != null)
		{
			if(triggerPos>0 && triggerPos < testData.words.size())
			{
				if(testData.words.get(triggerPos-1).equals("不") || testData.words.get(triggerPos-1).equals("不曾") || testData.words.get(triggerPos-1).equals("怎能") )//
				{
					predictedTrigger = testData.words.get(triggerPos-1)+predictedTrigger;
					int eventType = 11;
					testData.data.triggerWord = predictedTrigger;
					testData.data.eventType = eventType;
				}
				if(testData.words.get(triggerPos-1).equals("没有") || testData.words.get(triggerPos-1).equals("没") || testData.words.get(triggerPos-1).equals("无需"))
				{
					predictedTrigger = testData.words.get(triggerPos-1)+predictedTrigger;
					int eventType = 12;
					testData.data.triggerWord = predictedTrigger;
					testData.data.eventType = eventType;
				}
			}
		}
	}
	public static void main(String[] args) {
		// TODO Auto-generated method stub
//		String s = "对于军事现代化，目前印尼已经列装了俄罗斯的苏伊霍战机和中国的便携式导弹和舰对舰导弹，并试图在印尼国内装配C-705型反舰导弹。";
//		String[] ners = Ner.ner1(s).split("\\s+");
//		int len = 0;
//		for(String ner : ners)
//		{
//			if(ner.indexOf("b_")  !=-1|| ner.indexOf("other") !=-1)
//			{
//				len ++;
//			}
//		}
//		System.out.println(LtpTool.getWords(s).size()+","+len);
	}

}
