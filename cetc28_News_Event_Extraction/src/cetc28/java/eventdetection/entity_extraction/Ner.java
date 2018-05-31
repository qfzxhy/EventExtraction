package cetc28.java.eventdetection.entity_extraction;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cetc28.java.config.FileConfig;
import cetc28.java.news.label.ActorItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;

/**
 * 实体识别类
 * 
 * @author QF
 * 
 */
public class Ner
{
	public static int beamSize = 16;
	public static String trainDataPath;// 训练数据路径
	public static String entityFilePath;// 实体文件夹路径
	public static Entity deviceEntity;// 设备路径
	public static Entity roleEntity;// 角色
	public static Entity regionEntity;// region
	public static Entity orgEntity;// organization
	public static Entity personEntity;// person
	public static Entity countryEntity;// 国家
	public static boolean flag = false;
//	List<String> fileNameList = null;
	static Map<String, double[]> params = null;
	static Beam_search b = null;
	static Util u = null;

	public Ner()
	{
		// TODO Auto-generated constructor stub

	}

	static
	{
		String root = FileConfig.getEntityPath();
		if (Ner.create(FileConfig.getNerModelPath(), root, FileConfig.getNerTrainDataPath()) < 0)
		{
			System.err.println("error");
		}
	}

	// 通用model
	public static int create(String modelPath, String entityPath, String traindataPath)
	{
		entityFilePath = entityPath;
		trainDataPath = traindataPath;
		if (!flag)
		{
			deviceEntity = new Entity("dev");
			regionEntity = new Entity("ns");
			roleEntity = new Entity("role");
			orgEntity = new Entity("nt");
			personEntity = new Entity("nr");
			countryEntity = new Entity("country");
			flag = true;
		}
		b = new Beam_search();
		u = new Util();
		File file_model = new File(modelPath);
		File file_entity = new File(entityPath);
		if (file_entity.exists() && file_model.exists())
		{
			params = u.loadParams(modelPath);

		} else
		{
			System.err.println("实体文件没找到");
			return -1;
		}

		return 0;

	}

	/*
	 * input : 俄/j/other 外长/n/other ：/wp/other 不/d/other 会/v/other 对/p/other
	 * 日交/j/other 还/d/other 南/nd/b output: 俄/j/b_country 外长/n/b_role ：/wp/other
	 * 不/d/other 会/v/other 对/p/other 日交/j/ot
	 */
	public static String ner(String sentence)
	{
		// System.out.println("ner1:" + sentence);
		if (sentence == null || sentence.trim().equals(""))
			return null;
		sentence = strPreTransform(sentence);
		// System.out.println("strPreTransform:" + sentence);
		String decodeStr = decode(sentence);
		// System.out.println("decodeStr:" + decodeStr);
		// System.out.println( strPostTransform(decodeStr));
		return decodeStr;

	}

	// 返回当前句子中的所有实体，返回类型ActorItem
	/**
	 * 
	 * @param sentence  俄外长：不会对日交还南海千岛群岛
	 * @return actor: 	俄外长_日_南海千岛群岛
						actorPro: roleinfor_countryinfor_regioninfor
						index: 0_7_10
						len: 3_1_6
	 */
	public static ActorItem ner_actionitem(String sentence)
	{
		if (sentence == null || sentence.trim().equals(""))
		{
			return new ActorItem("", "", "", "");
		}
		sentence = sentence.replaceAll(" ", ",");
		sentence = sentence.replaceAll("　", ",");
		sentence = strPreTransform(sentence);
		String decodeStr = decode(sentence);
		return strPostTransform(decodeStr);
	}

	private static String getAttri(String attribute)
	{
		switch (attribute)
		{
		case "b_nt":
			return "organization";
		case "b_role":
			return "role";
		case "b_country":
			return "country";
		case "b_ns":
			return "region";
		case "b_nr":
			return "person";
		case "b_dev":
			return "device";
		}
		return null;
	}
	/**
	 * 
	 * @param decodeStr decodeResult 中/j/b_country 智/j/b_country 将/p/other 关系/n/other 提升/v/other 至/v/other 全面/a/other 战略/n/other 伙伴/n/other 关系/n/other
	 * @return          俄外长_日_南海千岛群岛
						actorPro: roleinfor_countryinfor_regioninfor
						index: 0_7_10
						len: 3_1_6
	 */
	private static ActorItem strPostTransform(String decodeStr)
	{
		// TODO Auto-generated method stub
		StringBuilder actor = new StringBuilder();
		StringBuilder actorPro = new StringBuilder();
		StringBuilder index = new StringBuilder();
		StringBuilder len = new StringBuilder();
		// System.out.println("decodeStr:"+decodeStr);
		String[] decodeStrArray = decodeStr.split("\\s+");
		// System.out.println("decodeStrArray:"+decodeStrArray.length);

		int index_temp = 0;
		String first = null;
		String second = null;
		String third = null;
		for (int i = 0; i < decodeStrArray.length; i++)
		{
			String[] wordInfor = decodeStrArray[i].split("/");
			first = wordInfor[0];
			second = wordInfor[1];
			third = wordInfor[2];
			if (wordInfor.length != 3)
			{
				second = wordInfor[wordInfor.length - 2];
				third = wordInfor[wordInfor.length - 1];
				first = decodeStrArray[i].substring(0,
						decodeStrArray[i].length() - second.length() - third.length() - 2);
			}
			if (third.startsWith("b"))// startwith "b"
			{
				actorPro.append(getAttri(third) + " ");// wwwwwwwwwwwwwwww
				index.append(String.valueOf(index_temp) + " ");// wwwwwwwwwww
				int len_temp = first.length();
				index_temp += first.length();
				StringBuilder actor_temp = new StringBuilder();
				actor_temp.append(first);
				int j = i + 1;
				if (j < decodeStrArray.length)
				{
					String[] nextWordInfor = decodeStrArray[j].split("/");
					first = nextWordInfor[0];
					second = nextWordInfor[1];
					third = nextWordInfor[2];
					if (nextWordInfor.length != 3)
					{
						second = nextWordInfor[nextWordInfor.length - 2];
						third = nextWordInfor[nextWordInfor.length - 1];
						first = decodeStrArray[j].substring(0,
								decodeStrArray[j].length() - second.length() - third.length() - 2);
					}
					while (third.startsWith("i"))
					{
						len_temp += first.length();
						actor_temp.append(first);
						index_temp += first.length();
						if (j == decodeStrArray.length - 1)
							break;
						nextWordInfor = decodeStrArray[++j].split("/");
						first = nextWordInfor[0];
						second = nextWordInfor[1];
						third = nextWordInfor[2];
						if (nextWordInfor.length != 3)
						{
							second = nextWordInfor[nextWordInfor.length - 2];
							third = nextWordInfor[nextWordInfor.length - 1];
							first = decodeStrArray[j].substring(0,
									decodeStrArray[j].length() - second.length() - third.length() - 2);
						}
					}
				}
				actor.append(actor_temp + " ");// wwwwwwwww
				len.append(String.valueOf(len_temp) + " ");// wwwwwwwwwww
				i = j - 1;
			} else
			{
				index_temp += first.length();
			}
		}

		return new ActorItem(actor.toString().trim().replaceAll(" ", "_"),
				actorPro.toString().trim().replaceAll(" ", "_"), index.toString().trim().replaceAll(" ", "_"),
				len.toString().trim().replaceAll(" ", "_"));
	}



	// 对命名实体识别工具做预处理
	private static String strPreTransform(String sentence, List<String> wordList, List<String> tagList)
	{
		// TODO Auto-generated method stub
		StringBuilder sb = new StringBuilder();
		// System.out.println("wordList:"+wordList);
		// System.out.println("tagList:"+tagList);

		for (int i = 0; i < wordList.size(); i++)
		{
			sb.append(wordList.get(i) + "/" + tagList.get(i) + "/other ");
		}

		return sb.toString().trim();
	}

	// 对命名实体识别工具做预处理
	private static String strPreTransform(String sentence)
	{
		// TODO Auto-generated method stub
		List<String> wordList = LtpTool.getWords(sentence);
		List<String> tagList = LtpTool.getPosTag(wordList);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < wordList.size(); i++)
		{
			sb.append(wordList.get(i) + "/" + tagList.get(i) + "/other ");
		}
		return sb.toString().trim();
	}

	// 命名实体识别
	public static String decode(String sentence)
	{
		if (sentence == null || sentence.trim().equals(""))
		{
			return null;
		}
		int now = 0;
		List<String[]> wordList = u.genWordPosLabel(sentence, 1);
		List<Term> predictTerm = b.runBeamSearch(sentence, beamSize, params, now, 1);
		for (int j = 0; j < predictTerm.size(); ++j)
		{
			wordList.get(j)[2] = predictTerm.get(j).label;
		}
		for (int j = 0; j < wordList.size(); j++)
		{
			String label = b.isCombinationWord(wordList.get(j)[0]);
			if (!label.equals("0"))
			{
				if (wordList.get(j)[2].equals("other"))
				{
					wordList.get(j)[2] = label;
				}
			} else
			{
				char c = wordList.get(j)[0].charAt(wordList.get(j)[0].length() - 1);
				// if((c == '船' || c == '舰' || c == '艇') &&
				// wordList.get(j)[2].equals("other"))
				// {
				// wordList.get(j)[2] = "b_dev";
				// }
			}
			if (wordList.get(j)[1].equals("ns") && wordList.get(j)[2].equals("other"))
			{
				wordList.get(j)[2] = "b_ns";
			}
			if (wordList.get(j)[1].equals("nh") && wordList.get(j)[2].equals("other")
					&& (j == 0 || wordList.get(j - 1)[2].indexOf("role") != -1
							|| (j < wordList.size() - 1 && wordList.get(j + 1)[2].indexOf("role") != -1)))
			{
				wordList.get(j)[2] = "b_nr";
			}
			if (wordList.get(j)[2].indexOf("b_role") != -1 && (j > 0 && wordList.get(j - 1)[0].equals("副")))
			{
				wordList.get(j - 1)[2] = "b_role";
				wordList.get(j)[2] = "i_role";
			}
		}
		deviceEntity.postProcessing(wordList);
		personEntity.postProcessing(wordList);
		regionEntity.postProcessing(wordList);
		roleEntity.postProcessing(wordList);
		countryEntity.postProcessing(wordList);
		orgEntity.postProcessing(wordList);
		Ner.postProcessing1(wordList);
		Ner.postProcessing2(wordList);
		Ner.postProcessing3(wordList);

		StringBuilder result = new StringBuilder();
		for (int j = 0; j < wordList.size(); j++)
		{
			result.append(wordList.get(j)[0] + "/" + wordList.get(j)[1] + "/" + wordList.get(j)[2] + " ");
		}
		return result.toString().trim();
	}

	private static void postProcessing1(List<String[]> wordList)
	{
		for (int i = 0; i < wordList.size(); i++)
		{
			if (wordList.get(i)[2].indexOf("b_dev") != -1)
			{
				if (i > 2 && wordList.get(i - 2)[1].equals("m") && wordList.get(i - 1)[0].length() == 1
						&& (wordList.get(i - 3)[2].indexOf("country") != -1
								|| wordList.get(i - 3)[2].indexOf("nt") != -1))
				{
					wordList.get(i - 2)[2] = "b_dev";
					wordList.get(i - 1)[2] = "i_dev";
					wordList.get(i)[2] = "i_dev";
					break;
				}
			}
		}
	}

	/*
	 * 后处理1 ： 美国3艘军舰
	 */
	private static void postProcessing2(List<String[]> wordList)
	{
		for (int i = 0; i < wordList.size(); i++)
		{
			if (wordList.get(i)[2].startsWith("b_dev"))
			{
				int j = i - 1;
				boolean hasV = false;
				if (j < 0)
					continue;
				if (wordList.get(j)[0].equals("号") || wordList.get(j)[0].equals("级") || wordList.get(j)[0].equals("式"))
				{
					j--;
				}
				if (j < 0)
					continue;
				// System.out.println(wordList.get(j)[0]);

				if (wordList.get(j)[0].equals("’") || wordList.get(j)[0].equals("”") || devHasColon(wordList, j + 1))
				{
					while (j >= 0 && !wordList.get(j)[0].equals("“") && !wordList.get(j)[0].equals("‘"))
					{
						if (wordList.get(j--)[1].equals("v"))
						{
							hasV = true;
							break;
						}
					}
					if (j >= 0 && !hasV)
					{
						wordList.get(j)[2] = "b_dev";
						for (int k = j + 1; k <= i; k++)
						{
							wordList.get(k)[2] = "i_dev";
						}
					}
				} else
				{
					continue;
				}

			}
		}
	}

	private static boolean devHasColon(List<String[]> wordList, int j)
	{
		// TODO Auto-generated method stub
		while (j < wordList.size() && wordList.get(j)[2].endsWith("dev"))
		{
			if (wordList.get(j)[0].equals("”") || wordList.get(j)[0].equals("’"))
			{
				return true;
			}
			j++;
		}
		return false;
	}

	private static void postProcessing3(List<String[]> wordList)
	{
		for (int i = 0; i < wordList.size(); i++)
		{
			if (wordList.get(i)[2].indexOf("b_dev") != -1)
			{
				i++;
				while (i < wordList.size() && wordList.get(i)[2].indexOf("_dev") != -1)
				{
					wordList.get(i++)[2] = "i_dev";
				}
			}
		}
	}

	private static void combine(List<String[]> wordList)
	{
		for (int i = wordList.size() - 1; i >= 0; i--)
		{
			if (!wordList.get(i)[2].equals("other"))
			{
				String type = wordList.get(i)[2].substring(2);
				int j = i;
				while (j >= 0)
				{
					if (wordList.get(j)[2].startsWith("b_"))
					{
						break;// break;
					}
					j--;
				}

				while (j >= 0)
				{
					if (!wordList.get(j)[2].equals("other"))
					{
						wordList.get(j)[2] = "i_" + type;
					} else
					{
						wordList.get(j + 1)[2] = "b_" + type;
						break;
					}
					j--;
				}
				i = j;
				if (j < 0)
				{
					wordList.get(j + 1)[2] = "b_" + type;
				}
			}
		}
	}

	/**
	 * 用于训练时预测
	 * 
	 * @param sentence
	 *            如：俄/j/other 外长/n/other ：/wp/other 不/d/other 会/v/other
	 *            对/p/other 日交/j/other
	 * @return 如：俄/b_country 外长/b_role ：/other 不/other 会/other 对/other 日交/other
	 */
	public static String ner2(String sentence)
	{
		sentence = strPreTransform(sentence);
		String decodeStr = decode2(sentence);
		return decodeStr;
	}
	/*
	 * 有合并combine
	 */
	public static String ner3(String sentence)
	{
		sentence = strPreTransform(sentence);
		String decodeStr = decode3(sentence);
		return decodeStr;
	}

	public static String decode2(String sentence)
	{
		if (sentence == null || sentence.trim().equals(""))
		{
			return null;
		}
		int now = 0;
		List<String[]> wordList = u.genWordPosLabel(sentence, 1);
		List<Term> predictTerm = b.runBeamSearch(sentence, beamSize, params, now, 1);
		for (int j = 0; j < predictTerm.size(); ++j)
		{
			wordList.get(j)[2] = predictTerm.get(j).label;
		}
		for (int j = 0; j < wordList.size(); j++)
		{
			String label = b.isCombinationWord(wordList.get(j)[0]);
			if (!label.equals("0"))
			{
				if (wordList.get(j)[2].equals("other"))
				{
					wordList.get(j)[2] = label;
				}
			} else
			{
				char c = wordList.get(j)[0].charAt(wordList.get(j)[0].length() - 1);
				// if((c == '船' || c == '舰' || c == '艇') &&
				// wordList.get(j)[2].equals("other"))
				// {
				// wordList.get(j)[2] = "b_dev";
				// }
			}
			if (wordList.get(j)[1].equals("ns") && wordList.get(j)[2].equals("other"))
			{
				wordList.get(j)[2] = "b_ns";
			}
			if (wordList.get(j)[1].equals("nh") && wordList.get(j)[2].equals("other")
					&& (j == 0 || wordList.get(j - 1)[2].indexOf("role") != -1
							|| (j < wordList.size() - 1 && wordList.get(j + 1)[2].indexOf("role") != -1)))
			{
				wordList.get(j)[2] = "b_nr";
			}
			if (wordList.get(j)[2].indexOf("b_role") != -1 && (j > 0 && wordList.get(j - 1)[0].equals("副")))
			{
				wordList.get(j - 1)[2] = "b_role";
				wordList.get(j)[2] = "i_role";
			}
		}
		// deviceEntity.postProcessing(wordList);
		// personEntity.postProcessing(wordList);
		// regionEntity.postProcessing(wordList);
		// roleEntity.postProcessing(wordList);
		// countryEntity.postProcessing(wordList);
		// orgEntity.postProcessing(wordList);
		// Ner.postProcessing1(wordList);
		// Ner.postProcessing2(wordList);
		// Ner.combine(wordList);
		StringBuilder result = new StringBuilder();
		for (int j = 0; j < wordList.size(); j++)
		{
			result.append(wordList.get(j)[0] + "/" + wordList.get(j)[2] + " ");
		}
		return result.toString().trim();
	}

	public static String decode3(String sentence)
	{
		if (sentence == null || sentence.trim().equals(""))
		{
			return null;
		}
		int now = 0;
		List<String[]> wordList = u.genWordPosLabel(sentence, 1);
		List<Term> predictTerm = b.runBeamSearch(sentence, beamSize, params, now, 1);
		for (int j = 0; j < predictTerm.size(); ++j)
		{
			wordList.get(j)[2] = predictTerm.get(j).label;
		}
		for (int j = 0; j < wordList.size(); j++)
		{
			String label = b.isCombinationWord(wordList.get(j)[0]);
			if (!label.equals("0"))
			{
				if (wordList.get(j)[2].equals("other"))
				{
					wordList.get(j)[2] = label;
				}
			} else
			{
				char c = wordList.get(j)[0].charAt(wordList.get(j)[0].length() - 1);
				// if((c == '船' || c == '舰' || c == '艇') &&
				// wordList.get(j)[2].equals("other"))
				// {
				// wordList.get(j)[2] = "b_dev";
				// }
			}
			if (wordList.get(j)[1].equals("ns") && wordList.get(j)[2].equals("other"))
			{
				wordList.get(j)[2] = "b_ns";
			}
			if (wordList.get(j)[1].equals("nh") && wordList.get(j)[2].equals("other")
					&& (j == 0 || wordList.get(j - 1)[2].indexOf("role") != -1
							|| (j < wordList.size() - 1 && wordList.get(j + 1)[2].indexOf("role") != -1)))
			{
				wordList.get(j)[2] = "b_nr";
			}
			if (wordList.get(j)[2].indexOf("b_role") != -1 && (j > 0 && wordList.get(j - 1)[0].equals("副")))
			{
				wordList.get(j - 1)[2] = "b_role";
				wordList.get(j)[2] = "i_role";
			}
		}
		deviceEntity.postProcessing(wordList);
		personEntity.postProcessing(wordList);
		regionEntity.postProcessing(wordList);
		roleEntity.postProcessing(wordList);
		countryEntity.postProcessing(wordList);
		orgEntity.postProcessing(wordList);
		Ner.postProcessing1(wordList);
		Ner.postProcessing2(wordList);
		Ner.combine(wordList);
		StringBuilder result = new StringBuilder();
		for (int j = 0; j < wordList.size(); j++)
		{
			result.append(wordList.get(j)[0] + "/" + wordList.get(j)[1] + "/" + wordList.get(j)[2] + " ");
		}
		return result.toString().trim();
	}

	
	public static void main(String[] args) throws IOException
	{
//		List<String> sents = new ArrayList<>();
//		BufferedReader br = new BufferedReader(new FileReader("src/train_sents.txt"));
//		String line = "";
//		while((line = br.readLine())!=null)
//		{
//			sents.add(line);
//		}
//		br.close();
//		BufferedWriter bw = new BufferedWriter(new FileWriter("src/train_entitys.txt"));
//		for(String sent : sents)
//		{
//			String r = Ner.ner3(sent);
//			bw.write(r + '\n');
//		}
//		bw.close();
		System.out.println(Ner.ner("第75集团军空突旅机动至西北大漠戈壁开展空地联合突击演习"));
	}
}
