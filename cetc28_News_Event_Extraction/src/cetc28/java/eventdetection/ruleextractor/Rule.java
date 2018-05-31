package cetc28.java.eventdetection.ruleextractor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;

/**
 * 
 * @author qianf
 * 规则类           主要是rule match函数和一些rule属性
 *
 */
public class Rule
{
	class LocalParas//局部参数
	{
		String trigger = "";//当前句子的触发词
		String rule = "";//规则
		int wordId = 0;// 当前词的id
		int i = 0;// 当前structureMap id;
		int preWordId = 0;// 上一词id，用于dependency parse
		int targetNum = 0;//target出现次数
		boolean continueWord = false;//是否需要下划线和前一个词隔开
		boolean ifEnd = false;//是否return 空rule
		boolean ifContinue = false;//是否直接continue
		boolean ifbreak = false;//是否直接break
	}
	/**
	 * 匹配到的规则文件中的字符串
	 */
	public String ruleStr = null;
	/**
	 * 解析后的规则中的关键信息
	 */
	public ArrayList<HashMap<String, Integer>> ruleStructure = null;
	/**
	 * 规则对于事件类别
	 */
	public int ruleType = -1;
	/**
	 * 规则触发词（核心词）
	 */
	public HashMap<String, Integer> coreWords = null;
	//public int voice = 0;// 0:other 1:主动 2：被顶
	public Rule(String ruleStr, int ruleType)
	{
		// TODO Auto-generated constructor stub
		this.ruleStr = ruleStr;
		this.ruleType = ruleType;
		ruleStructure = new ArrayList<>();
	}
	/**
	 * 
	 * @param c 输入字符，比如','
	 * @return 是否是中文标点字符
	 */
	public boolean isChinesePunctuation(char c)
	{
		if (c == '“' || c == '”' || c == '‘' || c == '’')
		{
			return false;
		}
		Character.UnicodeBlock ub = Character.UnicodeBlock.of(c);
		if (ub == Character.UnicodeBlock.GENERAL_PUNCTUATION || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION
				|| ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
				|| ub == Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS || ub == Character.UnicodeBlock.VERTICAL_FORMS)
		{
			return true;
		} else
		{
			return false;
		}
	}

	/**
	 * 当前最短片段中是否能够覆盖规则中所有关键字
	 * 
	 * @param words	当前最短片段分词结果
	 *            
	 * @return	是否能够匹配到规则
	 */
	private boolean isMatchCoreWord(List<String> words)
	{
		int mapId = 0;
		int i = 0;
		while (i < words.size() && mapId < ruleStructure.size())
		{
			if (ruleStructure.get(mapId).containsKey(words.get(i)))
			{
				mapId++;
			} else if (ruleStructure.get(mapId).containsKey("verb") || ruleStructure.get(mapId).containsKey("V")
					|| ruleStructure.get(mapId).containsKey("VOBN") || ruleStructure.get(mapId).containsKey("target")
					|| ruleStructure.get(mapId).containsKey("source") || ruleStructure.get(mapId).containsKey("obj")
					|| ruleStructure.get(mapId).containsKey("COO"))
			{
				i--;
				mapId++;
			}
			i++;
		}
		if (mapId == ruleStructure.size())
		{
			return true;
		}
		if (mapId == ruleStructure.size() - 1
				&& (ruleStructure.get(mapId).containsKey("target") || ruleStructure.get(mapId).containsKey("obj")))
		{
			if (ruleStructure.get(mapId).containsKey("target") && ruleStructure.get(mapId).get("target") == -1)
			{
				return true;
			}
			if (ruleStructure.get(mapId).containsKey("obj") && ruleStructure.get(mapId).containsKey("other"))
			{
				return true;
			}
		}
		return false;
	}
	/**
	 * 
	 * @param testData	输入数据
	 * @return	匹配到的规则
	 */
	public String isMatchRule(Data testData)
	{
		// 先找到核心词，如果没有，则代表改规则不能匹配
		String trigger = hasTrigger(testData);
		if (trigger == null)
		{
			return null;
		}
		String title = testData.data.newsTitle;
		int triggerwordId = 0;
		while (title.indexOf(trigger, triggerwordId) != -1)
		{
			triggerwordId = title.indexOf(trigger, triggerwordId);
			String rule = isMatchRule(testData, triggerwordId, trigger);//匹配规则子函数
			if (rule != null)
				return rule;
			triggerwordId++;
		}
		return null;
	}
	/**
	 * 解析规则中Obj标签
	 * @param trigger	触发词
	 * @param depResult 依存句法结构
	 * @param words 分词结果
	 * @param tags  词性结果
	 * @param nerArrs 命名实体结果
	 * @param paras 局部参数
	 */
	private void parseObj(String trigger, Pair<List<Integer>, List<String>> depResult, List<String> words,
			List<String> tags, String[] nerArrs, LocalParas paras)
	{
		if (ruleStructure.get(paras.i).containsKey("other"))
		{
			for (int j = paras.wordId; j < words.size(); j++)
			{
				if (depResult.getFirst().get(j) == paras.wordId
						&& (depResult.getSecond().get(j).equals("VOB") || depResult.getSecond().get(j).equals("POB")))
				{
					String entity = words.get(j);
					if (j > 0)
					{
						int k = -1;
						for (k = j - 1; k >= 0; k--)
						{
							if (depResult.getSecond().get(k).equals("ATT") && depResult.getFirst().get(k) - 1 == k + 1)
							{
								entity = words.get(k) + entity;
							} else
							{
								break;
							}
						}
						if (k == paras.preWordId && paras.rule.endsWith("target"))
						{
							paras.rule = paras.rule.substring(0, paras.rule.length() - 6);
						}
					}
					paras.rule += (entity);
					paras.wordId = j;
					paras.i++;
					return;
				}
			}
			paras.i++;
			return;
		}
		boolean hasEntity = false;
		for (int j = paras.wordId; j < words.size(); j++)
		{
			if (depResult.getFirst().get(j) == paras.wordId
					&& (depResult.getSecond().get(j).equals("VOB") || depResult.getSecond().get(j).equals("POB")))
			{
				String nerLabel = nerArrs[j].split("/")[2];
				if (nerLabel.indexOf("_") != -1)
					nerLabel = nerLabel.substring(nerLabel.indexOf("_") + 1);
				if (ruleStructure.get(paras.i).containsKey(nerLabel))
				{
					hasEntity = true;
					String entity = words.get(j);// 设备entity
					
					int k = -1;
					for (k = j - 1; k >= 0 && nerArrs[k].indexOf(nerLabel) != -1; k--)
					{
						entity = words.get(k) + entity;
					}
					if (k == paras.preWordId && paras.rule.endsWith("target"))
					{
						paras.rule = paras.rule.substring(0, paras.rule.length() - 6);
					}
					for (k = j + 1; k < words.size() && nerArrs[k].indexOf(nerLabel) != -1; k++)
					{
						entity += words.get(k);
					}
					paras.rule += (entity);
					paras.wordId = k - 1;
					paras.i++;
					break;
				}
			}
		}
		if (!hasEntity)
		{
			for (int j = paras.wordId; j < words.size(); j++)
			{
				String nerLabel = nerArrs[j].split("/")[2];
				if (nerLabel.indexOf("_") != -1)
					nerLabel = nerLabel.substring(nerLabel.indexOf("_") + 1);
				if (ruleStructure.get(paras.i).containsKey(nerLabel))
				{
					hasEntity = true;
					String entity = words.get(j);
					int k = -1;
					for (k = j + 1; k < words.size() && nerArrs[k].indexOf(nerLabel) != -1; k++)
					{
						entity += words.get(k);
					}
					paras.rule += (entity);
					paras.wordId = k - 1;
					paras.i++;
					break;
				}
			}
		}
		if (!hasEntity)
		{
			paras.ifEnd = true;
		}
	}

	/**
	 * 解析规则中Coo标签
	 * @param trigger	触发词
	 * @param depResult 依存句法结构
	 * @param words 分词结果
	 * @param tags  词性结果
	 * @param nerArrs 命名实体结果
	 * @param paras 局部参数
	 */
	private void parseCoo(Pair<List<Integer>, List<String>> depResult, List<String> words, List<String> tags,
			String[] nerArrs, LocalParas paras)
	{
		// TODO Auto-generated method stub
		if (tags.get(paras.wordId).equals("v") && depResult.getSecond().get(paras.wordId).equals("COO")
				&& words.get(depResult.getFirst().get(paras.wordId) - 1).equals(words.get(paras.preWordId)))
		{
			if (nerArrs[paras.wordId].indexOf("other") == -1)
			{
				return;
			}
			if (paras.rule.endsWith("target") || paras.rule.endsWith("source"))
			{
				paras.rule += words.get(paras.wordId);
			} else
			{
				paras.rule += ("_" + words.get(paras.wordId));
			}
			++paras.i;
		}
	}

	private void parseVerb(String trigger, Pair<List<Integer>, List<String>> depResult, List<String> words,
			List<String> tags, String[] nerArrs, LocalParas paras)
	{
		for (int j = paras.wordId; j < words.size(); j++)
		{
			if (tags.get(j).equals("v") && ruleStructure.get(paras.i).containsKey("V"))
			{
				if (nerArrs[j].indexOf("other") == -1)
				{
					return;
				}
				if (paras.rule.endsWith("target") || paras.rule.endsWith("source"))
				{
					paras.rule += words.get(j);
				} else
				{
					paras.rule += ("_" + words.get(j));
				}
				paras.preWordId = j;
				paras.wordId = j;
				paras.i++;
				break;
			}
			if (tags.get(j).equals("v") && depResult.getFirst().get(j) - 1 >=0 && words.get(depResult.getFirst().get(j) - 1).equals(trigger))
			{
				if (depResult.getSecond().get(j).equals("VOB") && ruleStructure.get(paras.i).containsKey("VOB"))
				{
					if (nerArrs[j].indexOf("other") == -1)
					{
						continue;
					}
					if (paras.rule.endsWith("target") || paras.rule.endsWith("source"))
					{
						paras.rule += words.get(j);
					} else
					{
						paras.rule += ("_" + words.get(j));
					}
					paras.preWordId = j;
					paras.wordId = j;
					paras.i++;
					break;
				}
				if (depResult.getSecond().get(j).equals("COO") && ruleStructure.get(paras.i).containsKey("COO"))
				{
					if (nerArrs[j].indexOf("other") == -1)
					{
						return;
					}
					if (paras.rule.endsWith("target") || paras.rule.endsWith("source"))
					{
						paras.rule += words.get(j);
					} else
					{
						paras.rule += ("_" + words.get(j));
					}
					paras.preWordId = j;
					paras.wordId = j;
					paras.i++;
					break;
				}
			}
		}
	}
	/**
	 * 解析规则中VOBN标签
	 * @param trigger	触发词
	 * @param depResult 依存句法结构
	 * @param words 分词结果
	 * @param tags  词性结果
	 * @param nerArrs 命名实体结果
	 * @param paras 局部参数
	 */
	private void parseVobN(String trigger, Pair<List<Integer>, List<String>> depResult, List<String> words,
			List<String> tags, String[] nerArrs, LocalParas paras)
	{
		if (tags.get(paras.wordId).equals("n") && depResult.getSecond().get(paras.wordId).equals("VOB")
				&& words.get(depResult.getFirst().get(paras.wordId) - 1).equals(words.get(paras.preWordId)))
		{
			int j = paras.wordId;
			String target = words.get(j);
			if (paras.rule.endsWith("target") || paras.rule.endsWith("source"))
			{
				paras.rule += target;
			} else
			{
				paras.rule += ("_" + target);
			}
			++paras.i;
		}
	}
	/**
	 * 解析规则中V标签
	 * @param trigger	触发词
	 * @param depResult 依存句法结构
	 * @param words 分词结果
	 * @param tags  词性结果
	 * @param nerArrs 命名实体结果
	 * @param paras 局部参数
	 */
	private void parseV(String trigger, Pair<List<Integer>, List<String>> depResult, List<String> words,
			List<String> tags, String[] nerArrs, LocalParas paras)
	{
		// System.out.println(words.get(paras.wordId));
		if (tags.get(paras.wordId).equals("v"))
		{
			// if(nerArrs[paras.wordId].indexOf("other") == -1)
			// {
			// return;
			// }
			if (paras.rule.endsWith("target") || paras.rule.endsWith("source"))
			{
				paras.rule += (words.get(paras.wordId));
			} else
			{
				paras.rule += ("_" + words.get(paras.wordId));
			}
			++paras.i;
		}
	}
	/**
	 * 解析规则中source标签
	 * @param trigger	触发词
	 * @param depResult 依存句法结构
	 * @param words 分词结果
	 * @param tags  词性结果
	 * @param nerArrs 命名实体结果
	 * @param paras 局部参数
	 */
	private void parseSource(String trigger, Pair<List<Integer>, List<String>> depResult, List<String> words,
			List<String> tags, String[] nerArrs, LocalParas paras)
	{
		if (ruleStructure.get(paras.i).get("source") == 1)// 1：表示target 必须要有实体
															// （-1：target*）
		{
			paras.rule += ("source");
		} else
		{
			if (hasEntity(words, tags, paras.wordId, ruleStructure, paras.i + 1, nerArrs, depResult) != 0)
			{
				paras.rule += ("source");
			}
		}
		paras.i++;
		paras.continueWord = false;
		paras.ifContinue = true;
	}
	/**
	 * 解析规则中target标签
	 * @param trigger	触发词
	 * @param depResult 依存句法结构
	 * @param words 分词结果
	 * @param tags  词性结果
	 * @param nerArrs 命名实体结果
	 * @param paras 局部参数
	 */
	private void parseTarget(String trigger, Pair<List<Integer>, List<String>> depResult, List<String> words,
			List<String> tags, String[] nerArrs, LocalParas paras)
	{
		if (ruleStructure.get(paras.i).containsKey("target") && paras.i < ruleStructure.size() - 1)
		{
			if (paras.targetNum < 1)
			{
				String target = "";
				if (ruleStructure.get(paras.i).get("target") == 1)// taget
																	// 必须要有实体匹配
				{
					if (hasEntity(words, tags, paras.wordId, ruleStructure, paras.i + 1, nerArrs, depResult) == 0)// 没有实体
					{
						paras.ifEnd = true;
						return;
					}
					paras.rule = paras.rule + "target";// target
					paras.targetNum++;
				} else// target不一定需要实体匹配
				{
					if (hasEntity(words, tags, paras.wordId, ruleStructure, paras.i + 1, nerArrs, depResult) != 0)// 有实体
					{
						paras.rule = paras.rule + "target";// target
						paras.targetNum++;
					}
				}
			} else
			{
				String target = "";
				if (ruleStructure.get(paras.i).get("target") == 1)// taget
																	// 必须要有实体匹配
				{
					if (hasEntity(words, tags, paras.wordId, ruleStructure, paras.i + 1, nerArrs, depResult) == 0)// 没有实体
					{
						paras.ifEnd = true;
						return;
					}
					for (int j = paras.wordId; j < words.size(); j++)
					{
						if ((depResult.getSecond().get(j).equals("DBL") || depResult.getSecond().get(j).equals("VOB")
								|| depResult.getSecond().get(j).equals("POB"))
								&& depResult.getFirst().get(j) - 1 == paras.wordId - 1)
						{
							target = words.get(j);
							break;
						}
					}
					paras.rule = paras.rule + target;// target
					paras.targetNum++;
				} else// target不一定需要实体匹配
				{
					if (hasEntity(words, tags, paras.wordId, ruleStructure, paras.i + 1, nerArrs, depResult) != 0)// 有实体
					{
						for (int j = paras.wordId; j < words.size(); j++)
						{
							if ((depResult.getSecond().get(j).equals("VOB")
									|| depResult.getSecond().get(j).equals("POB"))
									&& depResult.getFirst().get(j) - 1 == paras.wordId - 1)
							{
								target = words.get(j);
								break;
							}
						}
						paras.rule = paras.rule + target;// target
						paras.targetNum++;
					}
				}
			}

			paras.i++;
			paras.continueWord = false;
			paras.ifContinue = true;
		} else if (ruleStructure.get(paras.i).containsKey("target") && paras.i == ruleStructure.size() - 1)
		{
			String target = "";
			if (paras.targetNum < 1)
			{
				if (ruleStructure.get(paras.i).get("target") == 1)// taget
																	// 必须要有实体匹配
				{
					if (hasEntity(words, tags, paras.wordId, ruleStructure, paras.i + 1, nerArrs, depResult) == 0)// 没有实体
					{
						paras.ifEnd = true;
						return;
					}
					paras.rule = paras.rule + "target";// target
					paras.targetNum++;
				} else// target不一定需要实体匹配
				{
					if (hasEntity(words, tags, paras.wordId, ruleStructure, paras.i + 1, nerArrs, depResult) != 0)// 有实体
					{
						paras.rule = paras.rule + "target";// target
						paras.targetNum++;
					} else
					{
						int j = paras.wordId;
						for (; j < words.size(); j++)
						{
							if (depResult.getSecond().get(j).equals("VOB")
									&& depResult.getFirst().get(j) - 1 == paras.wordId - 1)
							{
								target = words.get(j);
								if (j > 0)
									if (depResult.getSecond().get(j - 1).equals("ATT")
											&& depResult.getFirst().get(j - 1) - 1 == j)
									{
										target = words.get(j - 1) + target;
									}
								break;
							}
						}
						if (paras.rule.endsWith("target") || paras.rule.endsWith("source"))
						{
							paras.rule += target;// target
						} else
						{
							if (!target.equals(""))
								paras.rule += "_" + target;// target
						}
					}
				}
			} else
			{
				if (ruleStructure.get(paras.i).get("target") == 1)// taget
																	// 必须要有实体匹配
				{
					if (hasEntity(words, tags, paras.wordId, ruleStructure, paras.i + 1, nerArrs, depResult) == 0)// 没有实体
					{
						paras.ifEnd = true;
						return;
					}
				}
				int j = paras.wordId;
				for (; j < words.size(); j++)
				{
					if (depResult.getSecond().get(j).equals("VOB")
							&& depResult.getFirst().get(j) - 1 == paras.wordId - 1)
					{
						target = words.get(j);
						// System.out.println(depResult.getSecond().get(j));
						// System.out.println(target);
						// System.out.println(words);
						// System.out.println(tags);
						// System.out.println(depResult.second);
						if (j > 0)
						{
							for (int k = j - 1; k >= 0; k--)
							{
								if (depResult.getSecond().get(k).equals("ATT")
										&& depResult.getFirst().get(k) - 1 == k + 1)
								{
									target = words.get(k) + target;
								} else
								{
									break;
								}
							}
						}

						break;
					}
				}
				if (paras.rule.endsWith("target") || paras.rule.endsWith("source"))
				{
					paras.rule += target;// target
				} else
				{
					if (!target.equals(""))
						paras.rule += "_" + target;// target
				}
			}
			paras.i++;
			paras.ifbreak = true;
		}
	}
	/**
	 * 解析规则中单词
	 * @param depResult 依存句法结构
	 * @param words 分词结果
	 * @param tags  词性结果
	 * @param nerArrs 命名实体结果
	 * @param paras 局部参数
	 */
	private void parseWord(Pair<List<Integer>, List<String>> depResult, List<String> words, List<String> tags,
			String[] nerArrs, LocalParas paras)
	{
		if (ruleStructure.get(paras.i).containsKey(words.get(paras.wordId)))
		{
			if(ruleStructure.get(paras.i).containsKey("3"))
			{
				if (paras.i <= 0 || paras.wordId <= 0
						|| depResult.getFirst().get(paras.wordId) - 1 != paras.preWordId)
				{
					paras.ifEnd = true;
					return;
				}
				if (paras.rule.endsWith("target") || paras.rule.endsWith("source")
						|| paras.rule.trim().length() == 0)
				{
					paras.rule = paras.rule + words.get(paras.wordId);
				} else
				{
					paras.rule = paras.rule + "_" + words.get(paras.wordId);
				}
			}else
			// 2: 需要补齐词信息
			if (ruleStructure.get(paras.i).containsKey("2"))
			{
				String target = words.get(paras.wordId);
				for (int k = paras.wordId - 1; k >= 0; k--)
				{
					if (depResult.getSecond().get(k).equals("ATT") && depResult.getFirst().get(k) - 1 == k + 1)
					{
						target = words.get(k) + target;
					} else
					{
						break;
					}
				}
				if (paras.rule.endsWith("target") || paras.rule.endsWith("source")
						|| paras.rule.trim().length() == 0)
				{
					paras.rule = paras.rule + target;
				} else
				{
					paras.rule = paras.rule + "_" + target;
				}
			} else
			// 1: 必须要紧紧跟着前一个词
			if (ruleStructure.get(paras.i).containsKey("1"))
			{
				if (paras.i <= 0 || paras.wordId <= 0
						|| !ruleStructure.get(paras.i - 1).containsKey(words.get(paras.wordId - 1)))
				{
					paras.ifEnd = true;
					return;
				}
				paras.rule = paras.rule + words.get(paras.wordId);
			} else if (paras.rule.endsWith("target") || paras.rule.endsWith("source")
					|| paras.rule.trim().length() == 0)
			{
				paras.rule = paras.rule + words.get(paras.wordId);
			} else
			{
				paras.rule = paras.rule + "_" + words.get(paras.wordId);
			}
			paras.i++;
			paras.preWordId = paras.wordId;
		}
	}
	/**
	 * 解析规则控制台
	 * @param i	ruleStructure下标
	 * @param words 分词结果
	 * @param wordId 当前词的位置
	 * @return 第几个标签
	 */
	private int switchController(int i, List<String> words, int wordId)
	{
		if (ruleStructure.get(i).containsKey("obj"))
		{
			return 1;
		}
		if (ruleStructure.get(i).containsKey("verb"))
		{
			return 2;
		}
		if (ruleStructure.get(i).containsKey("VOBN"))
		{
			return 3;
		}
		if (ruleStructure.get(i).containsKey("V"))
		{
			return 4;
		}
		if (ruleStructure.get(i).containsKey("source"))
		{
			return 5;
		}
		if (ruleStructure.get(i).containsKey("target"))
		{
			return 6;
		}
		if (ruleStructure.get(i).containsKey(words.get(wordId)))
		{
			return 7;
		}
		return 0;
	}
/**
 * 匹配规则子问题
 * @param testData 输入数据
 * @param triggerwordId 触发词下标
 * @param trigger 触发词
 * @return 匹配到的规则
 */
	public String isMatchRule(Data testData, int triggerwordId, String trigger)// 找到空格位置，从那里开始到下一个空格
	{
		LocalParas paras = new LocalParas();
		String title = testData.data.newsTitle;
		int beginId = -1, endId = title.length();// 修改过
		for (int i = 0; i < title.length(); i++)
		{
			if (isChinesePunctuation(title.charAt(i)) || title.charAt(i) == ' ' || title.charAt(i) == ' ')
			{
				if (triggerwordId < i)
				{
					endId = i;
					break;
				} else
				{
					beginId = i;
				}
			}
		}
		String newTitle = title.substring(beginId + 1, endId);
//		System.out.println(newTitle);
		String nerResult = Ner.ner(newTitle);
		String[] ners = null;
		if (nerResult != null && nerResult.trim().length() > 0)
		{
			ners = nerResult.split("\\s+");
		}
//		 System.out.println("newtitle:"+newTitle);
		Pair<List<Integer>, List<String>> depResult = null;
		List<String> words = null;
		List<String> tags = null;
		String[] nerArrs = null;
		if (newTitle.equals(title))
		{
			words = testData.words;
			tags = testData.tags;
			depResult = testData.depResult;
			nerArrs = testData.nerArrs;
		} else
		{
			words = LtpTool.getWords(newTitle);
			tags = LtpTool.getPosTag(words);
			depResult = LtpTool.parse(words, tags);
			nerArrs = Ner.ner3(newTitle).split("\\s+");
		}
		if (!isMatchCoreWord(words))
			return null;// 当前最短片段中是否能够覆盖规则中所有关键字
		while (paras.wordId < words.size() && paras.i < ruleStructure.size())
		{
			int switchId = switchController(paras.i, words, paras.wordId);
			switch (switchId)
			{
			case 0:
				break;
			case 1:
				parseObj(trigger, depResult, words, tags, nerArrs, paras);
				break;
			case 2:
				parseVerb(trigger, depResult, words, tags, nerArrs, paras);
				break;
			case 3:
				parseVobN(trigger, depResult, words, tags, nerArrs, paras);
				break;
			case 4:
				parseV(trigger, depResult, words, tags, nerArrs, paras);
				break;
			case 5:
				parseSource(trigger, depResult, words, tags, nerArrs, paras);
				break;
			case 6:
				parseTarget(trigger, depResult, words, tags, ners, paras);
				break;
			case 7:
				parseWord(depResult, words, tags, nerArrs, paras);
				break;
			default:
				break;
			}
			if (paras.ifEnd)
				return null;
			if (paras.ifContinue)
			{
				paras.ifContinue = false;
				continue;
			}
			if (paras.ifbreak)
			{
				paras.ifbreak = false;
				break;
			}
			paras.wordId++;
		}
		if (paras.i == ruleStructure.size())
		{
			/*
			 * i == ruleStructure.size()匹配成功
			 * 
			 */
			return paras.rule;
		}
		if (paras.i == ruleStructure.size() - 1
				&& (ruleStructure.get(paras.i).containsKey("target") || ruleStructure.get(paras.i).containsKey("obj")))
		{
			if (ruleStructure.get(paras.i).containsKey("target") && ruleStructure.get(paras.i).get("target") == 1)
			{
				if (hasEntity(words, tags, paras.wordId, ruleStructure, paras.i + 1, ners, depResult) == 0)
				{
					return null;
				}
			}
			if (ruleStructure.get(paras.i).containsKey("obj") && !ruleStructure.get(paras.i).containsKey("other"))
			{

				if (hasEntity(words, tags, paras.wordId, ruleStructure, paras.i + 1, ners, depResult) == 0)
				{
					return null;
				}
			}
			return paras.rule;
		}
		return null;
	}

	private boolean hasVOB(int wordId, List<String> words, List<String> tags,
			Pair<List<Integer>, List<String>> depResult)
	{
		// TODO Auto-generated method stub
		for (int i = wordId + 1; i < words.size(); i++)
		{
			if (depResult.getFirst().get(i) - 1 == wordId && depResult.getSecond().get(i).equals("VOB"))
			{
				return true;
			}
		}
		return false;
	}

	/*
	 * 判断source或者target位置是否有实体（sourceid-下一个词在ruleStructure id） output: 1:有 0：无
	 * -1：当前规则不能匹配
	 */
	/**
	 * 判断source或者target位置是否有实体（sourceid-下一个词在ruleStructure id） output: 1:有 0：无
	 * @param words	分词
	 * @param tags 词性
	 * @param wordId 当前词在words中下标
	 * @param ruleStructure 规则解析结果
	 * @param ruleStructure i 下标
	 * @param ners 实体结果
	 * @param depResult 依存结果
	 * @return
	 */
	private int hasEntity(List<String> words, List<String> tags, int wordId,
			ArrayList<HashMap<String, Integer>> ruleStructure, int i, String[] ners,
			Pair<List<Integer>, List<String>> depResult)
	{
		// TODO Auto-generated method stub
		int j = wordId - 1;
		if (i == ruleStructure.size())
		{
			while (wordId < words.size())
			{
				if (ners[wordId].indexOf("b_") != -1)
				{
					return 1;
				}
				wordId++;
			}
		} else
		{

			while (wordId < words.size())
			{
				/*
				 * 到下一个词结束，当前词到模板下一词
				 */
				if (ruleStructure.get(i).containsKey(words.get(wordId)))
				{
					break;
				}
				/*
				 * 模板下一词可能是verb
				 */
				if (tags.get(wordId).equals("v") && depResult.getSecond().get(wordId).equals("VOB")
						&& depResult.getFirst().get(wordId) - 1 == j)
				{
					if (ruleStructure.get(i).containsKey("VOB"))
					{
						break;
					}
				}
				if (tags.get(wordId).equals("v") && depResult.getSecond().get(wordId).equals("COO")
						&& depResult.getFirst().get(wordId) - 1 == j)
				{
					if (ruleStructure.get(i).containsKey("COO"))
					{
						break;
					}
				}
				/*
				 * 模板下一词可能是verb
				 */
				if (ners[wordId++].indexOf("b_") != -1)
				{
					return 1;
				}
			} // wordId
		}
		return 0;
	}
	/**
	 * 
	 * @param data 输入数据
	 * @return 触发词下标
	 */
	private String hasTrigger(Data data)
	{
		List<String> words = data.words;
		for (int i = 0; i < words.size(); i++)
		{
			if (coreWords != null && coreWords.containsKey(words.get(i)))
			{
				return words.get(i);
			}
		}
		return null;
	}

	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		String newsTitle = "特朗普与蔡英文通电话";
		LabelItem data = new LabelItem("", "", "", newsTitle);
		Data testData = new Data(data);
		testData.setTrainData();
		System.out.println(testData.words);
		System.out.println(Ner.ner3(newsTitle));
		System.out.println(testData.depResult.getFirst());
		System.out.println(testData.depResult.getSecond());
		Rule rule = RuleParser.ruleParsing("source{与 和 同 跟}target(通){电话}", 5);
		System.out.println(rule.isMatchRule(testData));
	}
	
	public void print()
	{
		// TODO Auto-generated method stub
		System.out.println("ruleStr:" + ruleStr);
		System.out.println("ruleStructure:");
		for (HashMap<String, Integer> triggerMap : ruleStructure)
		{
			for (Entry<String, Integer> entry : triggerMap.entrySet())
			{
				System.out.print(entry.getKey() + " ");
			}
			System.out.println();
		}

		// System.out.println("ruleType:"+ruleType);
		for (Entry<String, Integer> entry : coreWords.entrySet())
		{
			System.out.print(entry.getKey() + " ");
		}
		System.out.println();
	}
}
