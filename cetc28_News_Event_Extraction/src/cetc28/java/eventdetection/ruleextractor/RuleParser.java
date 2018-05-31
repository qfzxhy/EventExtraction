package cetc28.java.eventdetection.ruleextractor;

import java.lang.annotation.Target;
import java.security.KeyStore.Entry;
import java.util.HashMap;

import javax.management.remote.TargetedNotification;

/**
 * 
 * @author qianf
 *
 */
public class RuleParser
{
	/**
	 * 
	 * @param ruleStr
	 *            当前规则字符串（来着文件中）比如“source在target航行”
	 * @param ruleType
	 *            对于类别
	 * @return 解析结果
	 */
	public static Rule ruleParsing(String ruleStr, int ruleType)
	{
		Rule rule = new Rule(ruleStr, ruleType);
		String[] ruleStrArr = ruleStr.split("\t+");
		//System.out.println(ruleStrArr.length);
		HashMap<String, String[]> tagmap = new HashMap<>();
		for (int i = 1; i < ruleStrArr.length; i++)
		{
			//System.out.println(ruleStrArr[i]);
			String type = ruleStrArr[i].substring(0, ruleStrArr[i].indexOf(":"));
			String[] vals = ruleStrArr[i].substring(ruleStrArr[i].indexOf(":") + 1).split("\\s+");
			tagmap.put(type, vals);
		}
		int objId = 0;
		for (int i = 0; i < ruleStrArr[0].length(); i++)
		{
			if (ruleStrArr[0].charAt(i) == 's')
			{
				if ((i + 6 < ruleStrArr[0].length()) && ruleStrArr[0].charAt(i + 6) == '*')
				{
					HashMap<String, Integer> triggerMap = new HashMap<>();
					triggerMap.put("source", -1);
					rule.ruleStructure.add(triggerMap);
					i += 6;
					continue;
				} else
				{
					HashMap<String, Integer> triggerMap = new HashMap<>();
					triggerMap.put("source", 1);
					rule.ruleStructure.add(triggerMap);
					i += 5;
					continue;
				}
			}
			if (ruleStrArr[0].charAt(i) == 't')
			{
				// rule.targetNum ++;
				if ((i + 6 < ruleStrArr[0].length()) && ruleStrArr[0].charAt(i + 6) == '*')
				{
					HashMap<String, Integer> triggerMap = new HashMap<>();
					triggerMap.put("target", -1);
					rule.ruleStructure.add(triggerMap);
					i += 6;
					continue;
				} else
				{
					HashMap<String, Integer> triggerMap = new HashMap<>();
					triggerMap.put("target", 1);
					rule.ruleStructure.add(triggerMap);
					i += 5;
					continue;
				}

			}
			if (ruleStrArr[0].charAt(i) == '(')
			{
				HashMap<String, Integer> triggerMap = new HashMap<>();
				int j = i + 1;
				String trigger = "";
				while ((j < ruleStrArr[0].length() && ruleStrArr[0].charAt(j) != ')'))
				{
					if (ruleStrArr[0].charAt(j) == ' ')
					{
						triggerMap.put(trigger, 1);
						trigger = "";
					} else
					{
						trigger += ruleStrArr[0].charAt(j);
					}
					j++;
				}
				if (ruleStrArr[0].charAt(j) == ')')
				{
					triggerMap.put(trigger, 1);
				}
				rule.ruleStructure.add(triggerMap);
				rule.coreWords = triggerMap;
				i = j;
				continue;
			}
			if (ruleStrArr[0].charAt(i) == '{')
			{
				if (i + 5 < ruleStrArr[0].length() && ruleStrArr[0].substring(i + 1, i + 5).equals("verb"))
				{
					// if(!tagmap.containsKey("verb")){System.err.println("模板格式不对");}
					HashMap<String, Integer> triggerMap = new HashMap<>();
					// for(String objType : tagmap.get("verb"))
					// {
					// triggerMap.put(objType, 1);
					// }
					triggerMap.put("VOB", 1);
					triggerMap.put("COO", 1);
					triggerMap.put("verb", 1);
					rule.ruleStructure.add(triggerMap);
					i += 4;
					continue;
				} else if (i + 4 < ruleStrArr[0].length() && ruleStrArr[0].substring(i + 1, i + 4).equals("obj"))
				{
					if (!tagmap.containsKey("obj"))
					{
						System.err.println("模板格式不对");
					}
					HashMap<String, Integer> triggerMap = new HashMap<>();
					for (String objType : tagmap.get("obj"))
					{
						triggerMap.put(objType, 1);
					}
					triggerMap.put("obj", 1);
					rule.ruleStructure.add(triggerMap);
					i += 3;
					continue;
				} else
				{
					HashMap<String, Integer> triggerMap = new HashMap<>();
					int j = i + 1;
					String trigger = "";
					while ((j < ruleStrArr[0].length() && ruleStrArr[0].charAt(j) != '}'))
					{
						if (ruleStrArr[0].charAt(j) == ' ')
						{
							triggerMap.put(trigger, 1);
							trigger = "";
						} else
						{
							trigger += ruleStrArr[0].charAt(j);
						}
						j++;
					}
					if (ruleStrArr[0].charAt(j) == '}')
					{
						triggerMap.put(trigger, 1);
					}
					rule.ruleStructure.add(triggerMap);
					i = j;
					continue;
				}

			}
			if (ruleStrArr[0].charAt(i) == '[')// (参加)target{军演 演习}[的]source
												// 的在触发词显示时不显示
			{
				HashMap<String, Integer> triggerMap = new HashMap<>();
				int j = i + 1;
				String trigger = "";
				while ((j < ruleStr.length() && ruleStr.charAt(j) != ']'))
				{
					if (ruleStr.charAt(j) == ' ')
					{
						triggerMap.put(trigger, 0);
						trigger = "";
					} else
					{
						trigger += ruleStrArr[0].charAt(j);
					}
					j++;
				}
				if (ruleStrArr[0].charAt(j) == ']')
				{
					triggerMap.put(trigger, 0);
				}
				rule.ruleStructure.add(triggerMap);
				i = j;
				continue;
			}
			rule.ruleType = ruleType;
		}
		return rule;
	}

	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		Rule rule = RuleParser.ruleParsing("source(暂停 拒绝 不愿意 不愿)target*{verb}{obj}	obj:other", 2);
		for (HashMap<String, Integer> map : rule.ruleStructure)
		{
			for (java.util.Map.Entry<String, Integer> entry : map.entrySet())
			{
				System.out.print(entry.getKey() + ":" + entry.getValue() + " ");
			}
			System.out.println();
		}
	}

}
