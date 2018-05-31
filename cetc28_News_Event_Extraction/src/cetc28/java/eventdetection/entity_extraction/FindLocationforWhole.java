package cetc28.java.eventdetection.entity_extraction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cetc28.java.config.FileConfig;
import cetc28.java.dbtool.GeonamesUtil;
import cetc28.java.eventdetection.argument_extraction.ArgumentExtraction;
import cetc28.java.eventdetection.argument_extraction.Methods;
import cetc28.java.eventdetection.argument_extraction.ReSegment;
import cetc28.java.eventdetection.textrank.TextRankSummary;
import cetc28.java.eventdetection.time_location_extraction.LocationExtraction;
import cetc28.java.news.label.ActorItem;
import cetc28.java.news.label.EventItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import cetc28.java.solrtool.GeoEncoder.Place;
import cetc28.java.solrtool.SolrGeo;
import oracle.net.aso.l;
/**
 * 查所有句子中出现频率最高的地点名
 */
public class FindLocationforWhole {
	public SolrGeo solrGeo;
	public HashMap<String, Boolean> stopwords;
	public FindActorandPerson findActor; 
	public FindLocationforWhole(FindActorandPerson findActor) {
		super();
		this.findActor = findActor;
		
		stopwords = new HashMap<>();
		loadStopword(FileConfig.getLocStopWordsPath());
	}
	private void loadStopword(String stopwordPath)
	{
		// TODO Auto-generated method stub
		Util.loadfileMap(stopwordPath, stopwords);
	}
	/**
	 * 查所有句子中出现频率最高的地点名
	 * @param content
	 * @return
	 * @throws SQLException
	 */
	public String FindAllActorforWhole(List<String> content) throws SQLException {
		// TODO Auto-generated method stub
		if (content == null || content.size() == 0)
			return null;
		ArrayList<Pair<String, Integer>> EntityCount = new ArrayList<Pair<String, Integer>>();
	
		/**
		 * 调用接口，抽取每一句中的地点
		 */
		solrGeo = new SolrGeo();
		for (String sentence : content) {
			sentence = sentence.trim();
			if(sentence.equals(""))continue;
			String []nerArrs = Ner.ner(sentence).split("\\s");
			Pair<String, String> location = LocationExtraction.getLocation(LtpTool.posTagging(nerArrs));
			String place = location != null ? location.getFirst() : null;
			if(place == null || place.trim().equals(""))continue;
			Place place_solr  = solrGeo.getCoordinate(location.getFirst(), location.getSecond(), new Pair<String, String>("",""));
			if(place_solr == null)continue;
			Set(EntityCount, place, 1);
		}
		solrGeo.close();
		String place = maxEntity(EntityCount);
		if(!place.trim().equals(""))return place;
		
		/**
		 * 如果地点为空，直接统计全文中的所有地点
		 */
		EntityCount = new ArrayList<Pair<String, Integer>>();
		for (String sentence : content) {
			sentence = sentence.trim();
			if(sentence.equals(""))continue;
			if (!sentence.equals("")) {
				ActorItem actorItem = Ner.ner_actionitem(sentence);
				if (actorItem == null)continue;

				String actorArr[] = actorItem.getActor().split("_");
				String actorProArr[] = actorItem.getActorPro().split("_");

				for (int i = 0; i < actorProArr.length; i++) {
					if ((actorProArr[i].equals("country") || actorProArr[i].equals("region")) 
							&&  ! (stopwords.containsKey(actorArr[i]))) {
					
						int weight = 1;
//
//						if (ifhasPBefore(sentence, actorArr[i]))
//							weight = 4;
						Set(EntityCount, actorArr[i], weight);
					}

				}
			}
		}
		return maxEntity(EntityCount);
	}
	
//	/**
//	 * 词之前是否有指定介词
//	 * @param sentence
//	 * @param actor
//	 * @return
//	 */
//	private boolean ifhasPBefore(String sentence, String actor) {
//		// TODO Auto-generated method stub
//		if (sentence == null || actor == null)
//			return false;
//		int pPosition = sentence.indexOf(actor) - 1;
//		if (pPosition == -1)
//			return false;
//		char pWord = sentence.charAt(pPosition);
//		if (pWord == '在')
//			return true;
//		return false;
//	}

	
	private void Set(ArrayList<Pair<String, Integer>> entityCount, String entity, int weight) {
		// TODO Auto-generated method stub
		if (entityCount == null)
			return;
		boolean flag = false;
		for (Pair<String, Integer> entitycount : entityCount) {
			if (entitycount.first.trim().equals(entity)) {
				entitycount.second = entitycount.second + weight;
				flag = true;
				break;
			}
		}
		if (flag == false)
			entityCount.add(new Pair<String, Integer>(entity, weight));
	}

	/**
	 * 返回次数最多的实体
	 * @param entityCount
	 * @return
	 */
	private String maxEntity(ArrayList<Pair<String, Integer>> entityCount) {
		// TODO Auto-generated method stub
		String placeEntity = "";
		if (entityCount == null)
			return placeEntity;
		int maxNum = 0;
		for (Pair<String, Integer> entitycount : entityCount) {
			if (entitycount.second >= maxNum) {
				placeEntity = entitycount.first.trim();
				maxNum = entitycount.second;
			}
		}
		// System.out.println(placeEntity);
		return placeEntity == null ? "" : placeEntity;
	}
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		FindLocationforWhole FindAllActorforWhole = new FindLocationforWhole(new FindActorandPerson(new ReSegment(new Methods(),new GeonamesUtil()), new ArgumentExtraction()));
		new LtpTool();
		
		TextRankSummary textRankSummary = new TextRankSummary(new FindActorandPerson(new ReSegment(new Methods(),new GeonamesUtil()), new ArgumentExtraction()));
//		String news_content = "（八打灵再也综合讯）今年净选盟5.0集会出席人数远逊去年的4.0集会，马来西亚首相纳吉对此指出，这显示马国民众已经厌倦净选盟，也认为这类集会对国家无益。当局在净选盟5.0集会前夕及集会当天逮捕了24人，其中八人已在20日傍晚获释。 纳吉20日在秘鲁首都利马出席亚太经合组织领导人会议后对记者表示，通过类似方式推翻政府是违法的。“马国公民必须捍卫法治原则，否则国家将陷入混乱，人民将因此受苦。” 马国经济评级未下调 纳吉：马哈迪评论无根据 纳吉表示，马哈迪对国家经济的评论只是政治言论，这些言论没有任何根据，也没有评级机构或国际货币基金组织或世界银行等国际组织的支持。“若我们有经济危机，评级机构肯定将下调评级展望。国际货币基金组织及世界银行也将批评及与政府商讨对策。” 纳吉认为，这些国际组织并没对马国有任何行动，证明马国的管理良好，因此人民没必要恐慌。 关于今年净选盟5.0集会出席人数，主办方净选盟20日在记者会上表示约有12万人。吉隆坡警方认为只有1万5500人，“当今大马”则指有4万1000人。 不过，今年净选盟5.0集会历时9小时，比去年的34小时短许多。在参与者当中，马来人的比例也增加了。 马国通讯及多媒体部长莫哈末沙烈表示，出席净选盟5.0集会的人数不多，对前首相马哈迪的土著团结党是沉重打击。 莫哈末沙烈19日发表声明中说，马哈迪新成立的土团党原本应该为集会带来大批新的马来人支持者，结果却并非如此。 他说，去年净选盟4.0集会有50万人出席，今年出席的1万5500人，这只是去年的3%。他表示，即使根据当今大马的数据，今年的出席人数也不到去年的一成。他认为是马哈迪参与集会而导致出席人数大减。“大多数马国人已拒绝净选盟及土团党。土团党是个失败的政党，纯粹是马哈迪为了让儿子当首相而成立的。” 昨天，马国警方释放净选盟2.0秘书处成员曼迪星、民主行动党党员黄志伟与李凯鸣及雪州州委刘天球、社会主义党中委阿鲁、东海岸净选盟副主席莫哈末沙夫安、学运分子阿妮斯莎菲卡和鲁曼努哈京等八名被捕者。 安全罪行法令下被扣 玛丽亚陈或面对死刑 捍卫自由律师团（LFL）证实，警方是援引2012年安全罪行（特别措施）法令（SOSMA）扣留净选盟2.0主席玛丽亚陈。若当局援引该条文提控且罪名成立，她面对的最高刑罚将是死刑。 律师团19日在推特表示，根据该法令，警方可在玛丽亚陈没有代表律师的情况下，将她扣留长达28天。玛丽亚陈的代表律师艾力鲍尔森表示：“这项法令早在国会辩论时已将净选盟集会排除在外，没想到当局竟用它来对付玛丽亚陈。”";
		String news_content = "不安全感挥之难去 在乌克兰、格鲁吉亚和叙利亚战争表明，俄罗斯有能力也有能力利用军事力量实现政治目的。但这些战争不是俄罗斯实力的标志，相反，这显示了俄罗斯深深的不安全感。正如凯南所写到的：“在克里姆林宫对世界事务神经质的看法背后是俄罗斯传统和本能的不安全感……这个论点为俄罗斯加强军事和警察力量提供了正当理由……从根本上说，这只是不安定的俄罗斯民族主义的逐步发展。在这场数世纪之久的传统行为中，攻击和防守的概念纠缠不清地被混淆。”这种民族主义继续影响着俄罗斯当下的行为。 普京认为俄罗斯参与的这几场战争是一种自卫，是由阻止西方的必要性所驱动的。这就是他在2014年3月18日把全国精英召集到克里姆林宫金碧辉煌的大厅，宣布俄罗斯与克里米亚“重新统一”所要表达的意思。他说：“宛如一面镜子，乌克兰形势反映了世界在过去几十年都一直在发生的情况。我们的西方伙伴在美国的领导下，不愿意受国际法引导，而是受枪杆子的引导。”他说，在乌克兰，西方越过了红线。西方的行动让俄罗斯别无选择，只能向克里米亚派兵。 可是就在几天前，普京还对德国总理默克尔说，在克里米亚没有俄军。据报道，默克尔对奥巴马说：“他生活在另一个世界。”在他的世界中，西方试图摧毁俄罗斯。在2011至2012年冬季席卷独联体的颜色革命和在俄罗斯的抗议活动都是西方的阴谋。 但正如许多人所说的，他最开始并没有把西方视作威胁，他的看法是对俄罗斯和前苏联加盟共和国内部变化的反应。在2000年成为总统时，普京没有表现出对美国和西方的公然敌意，尽管那时在没有联合国授权的情况下北约刚刚轰炸了贝尔格莱德并引发尖锐的反美反应。他在首次接受英国广播公司的专访时说：“我无法想象俄罗斯孤立于欧洲，所以我难以把北约视作敌人。”他说，如果获得平等对待，俄罗斯可能加入北约。即便在3个波罗的海国家在2004年春加入北约后，普京仍坚称与北约的关系正在“积极发展”而且他“不担心北约扩张”。 普京与西方关系的转折点出现在那年即将结束之际，当时几件看似不相关的事情碰在了一起。第一件是在北高加索别斯兰市的一所学校遭遇恐怖袭击。在这起事件中，有1200人（其中大多数是儿童）被扣为人质。俄特种部队对学校发动攻击，造成333人丧生。普京随后指责西方试图摧毁俄罗斯。他取消地方选举，赋予安全部门更多权力。 下一个关键事件是尤科斯石油公司的解体和被没收。此事进一步给根植于苏联克格勃的强力集团壮了胆和提供了资金。它们因西方阴谋论和西方是敌人的夸大感觉而蓬勃发展。 点击图片进入下一页 资料图：俄格战争中挺进中的俄军装甲部队。美联社";
		List<String> content = textRankSummary .spiltSentence(news_content);
		List<EventItem> tempEventList = new ArrayList<EventItem>() ;
//		System.out.println(content);
		textRankSummary.RemoveNoise(content, tempEventList );
		
		try {
			System.out.println(FindAllActorforWhole.FindAllActorforWhole(content));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
