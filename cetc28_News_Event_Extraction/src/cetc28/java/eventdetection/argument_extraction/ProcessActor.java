package cetc28.java.eventdetection.argument_extraction;

import cetc28.java.news.label.LabelItem;
/**
 *  根据以下规则对事件抽取结果中的实体做后处理
 * 	source中有多个实体，且实体以组织、人物、角色开头时，保留至多两个实体；
 *  去除source、target中的重复项
 * @author qianf
 *
 */
public class ProcessActor {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	/**
	 * source、target 只保留一位实体,并去掉属性中的infor
	 * @param eventItem
	 */
	public void Finalprocess(LabelItem eventItem) {
		// TODO Auto-generated method stub
		if (eventItem == null)
			return;
		if (eventItem.sourceActor != null && eventItem.sourceActorPro != null) {
			String sourcePro[] = eventItem.sourceActorPro.split("_");
			String sourceAc[] = eventItem.sourceActor.split("_");
			removeSameActor(sourceAc, sourcePro);// 去掉source中的重复项

			eventItem.sourceActorPro = List2String(sourcePro);
			eventItem.sourceActor = List2String(sourceAc);
			sourcePro = eventItem.sourceActorPro.split("_");
			sourceAc = eventItem.sourceActor.split("_");

			while (sourcePro != null && sourcePro.length > 2
					&& (eventItem.sourceActorPro.startsWith("role") || eventItem.sourceActorPro.startsWith("person"))) {				
				if (sourcePro[0].trim().equals("role") || sourcePro[0].trim().equals("person")
						|| sourceAc[0].matches(".*媒") || sourcePro[0].trim().equals("organization")) {
					sourceAc[0] = "";
					sourcePro[0] = "";
				}
				eventItem.sourceActorPro = List2String(sourcePro);
				eventItem.sourceActor = List2String(sourceAc);
				sourcePro = eventItem.sourceActorPro.split("_");
				sourceAc = eventItem.sourceActor.split("_");
			}
		}
		
		if (eventItem.targetActorPro != null && eventItem.targetActor != null) {
			String targetPro[] = eventItem.targetActorPro.split("_");
			String targetAc[] = eventItem.targetActor.split("_");
			removeSameActor(targetAc, targetPro);// 去掉source中的重复项
			eventItem.targetActorPro = List2String(targetPro);
			eventItem.targetActor = List2String(targetAc);

			if (eventItem.sourceActor != null && eventItem.targetActorPro != null && eventItem.targetActor != null
					&& eventItem.sourceActor.trim().equals(eventItem.targetActor.trim())) {
				eventItem.sourceActor = null;
				eventItem.sourceActorPro = null;
				return;
			}
		}
	}

	/*
	 * 去掉实体串中重复的实体
	 */
	private void removeSameActor(String[] sourceAc, String[] sourcePro) {
		if (sourcePro == null || sourceAc == null || sourcePro.length <= 1 || sourceAc.length <= 1)
			return;

		for (int i = sourceAc.length - 1; i >= 1; i--) { // 从后往前逐个比较
			for (int j = 0; j < i; j++) {
				if (!sourceAc[i].trim().equals("") && sourceAc[i].trim().equals(sourceAc[j].trim())) {
					sourceAc[i] = "";
					sourcePro[i] = "";
					break;
				}
			}
		}

		String newsourceAc = List2String(sourceAc);
		String newsourcePro = List2String(sourcePro);
		if (newsourceAc != null)
			sourceAc = newsourceAc.split("_");
		if (newsourcePro != null)
			sourcePro = newsourcePro.split("_");
	}

	private String List2String(String[] Actor) {
		// TODO Auto-generated method stub
		String resultString = "";
		for (String s : Actor) {
			resultString = s.equals("") ? resultString : resultString.concat(s + "_");
		}
		resultString = resultString.startsWith("_") ? resultString.substring(1) : resultString;
		return resultString.endsWith("_") ? resultString.substring(0, resultString.length() - 1) : resultString;
	}


}
