package cetc28.java.program;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cetc28.java.dbtool.GeonamesUtil;
import cetc28.java.dbtool.OracleUtil;
import cetc28.java.eventdetection.argument_extraction.ArgumentExtraction;
import cetc28.java.eventdetection.argument_extraction.Methods;
import cetc28.java.eventdetection.argument_extraction.ProcessActor;
import cetc28.java.eventdetection.argument_extraction.ReSegment;
import cetc28.java.eventdetection.argument_extraction.RoleExtract;
import cetc28.java.eventdetection.entity_extraction.FindActorandPerson;
import cetc28.java.eventdetection.entity_extraction.FindLocationforWhole;
import cetc28.java.eventdetection.entity_extraction.Ner;
import cetc28.java.eventdetection.preprocessing.Data;
import cetc28.java.eventdetection.sentiment_extraction.CombineEvent;
import cetc28.java.eventdetection.sentiment_extraction.PolarityBasic;
import cetc28.java.eventdetection.textrank.TextRankSummary;
import cetc28.java.eventdetection.time_location_extraction.TimeExtraction;
import cetc28.java.news.label.EventItem;
import cetc28.java.news.label.LabelItem;
import cetc28.java.nlptools.LtpTool;
import cetc28.java.nlptools.Pair;
import cetc28.java.nlptools.Stanford_Parser;
import edu.hit.ir.ltp4j.Segmentor;
//事件抽取的主类
public class EventExtract {
	public RoleExtract roleExtract;
	public OracleUtil oracleUtil;
	public TextRankSummary textRankSummary;
	public PolarityBasic polarityBasic;
	public RunDetection runDetection;
	public TimeExtraction timeExraction;
	public ReSegment preprocess;
	public Stanford_Parser stanford_Parser;
	public FindActorandPerson findActorbyDB;
	public Methods methods;
	public GeonamesUtil geonamesUtil;
	public CombineEvent combineEvent;	
	public LtpTool ltpTool;
	public ArgumentExtraction argumentExtraction;
	public FindLocationforWhole findActorforWhole;
	public ProcessActor processActor;
	public EventExtract( ) {
		// TODO Auto-generated constructor stub
		this.runDetection = new RunDetection();
		this.methods = new Methods();
		this.ltpTool = new LtpTool();
		this.combineEvent = new CombineEvent();
		this.polarityBasic = new PolarityBasic();
//		this.geonamesUtil = new GeonamesUtil();
		this.processActor = new ProcessActor();
		this.argumentExtraction = new ArgumentExtraction();
		this.runDetection = runDetection;
		this.stanford_Parser = new Stanford_Parser();
//		this.oracleUtil = new OracleUtil(this.geonamesUtil,this.combineEvent);
		this.preprocess = new ReSegment(this.methods, this.geonamesUtil);
		this.findActorbyDB = new FindActorandPerson(this.preprocess, this.argumentExtraction);
		this.findActorforWhole = new FindLocationforWhole(findActorbyDB);
		this.timeExraction = new TimeExtraction(this.findActorbyDB);
		this.textRankSummary = new TextRankSummary(this.findActorbyDB);
		this.roleExtract = new RoleExtract(this.ltpTool, this.preprocess,
				this.stanford_Parser, this.findActorbyDB, this.methods,  this.geonamesUtil);
	}

	
	

	/**
	 * 抽取整篇新闻中的所有事件
	 * @param newsID
	 * @param newsURL
	 * @param imgAddress
	 * @param saveTime
	 * @param newsTitle
	 * @param news_content
	 * @return 该篇新闻中每一句话的事件抽取结果的列表
	 * @throws SQLException
	 */
	public List<EventItem> eventExtractforWhole(String newsID, String newsURL, String imgAddress, String saveTime,
			String newsTitle, String news_content) throws SQLException {
		/*
		 * 用于存储新闻中所有的事件抽取结果
		 */
		List<EventItem> EventList = new ArrayList();
		/*
		 * 用于存储新闻正文的事件抽取结果
		 */
		List<EventItem> tempEventList = new ArrayList();

		// System.out.println("当前title"+news_title);
		if (newsTitle == null || newsTitle.equals(""))
			return EventList;
		int comtentIndex = 0;// 为新闻中的每个事件构建一个idIndex
//		System.out.println("news_title:" + newsTitle);

		/*
		 * 对正文分句和去噪声,并且对正文做摘要，只保留摘要中前20条句子,将处理后的句子放入列表tempEventList
		 */
		List<String> content = textRankSummary.spiltSentence(news_content);
		textRankSummary.RemoveNoise(content, tempEventList);
		
		/*
		 * 为当前新闻标题新建建一个事件类对象
		 */
		EventItem titleEventItem = new EventItem(newsTitle, false);

		/**
		 * 找到新闻中出现频率最高的地点
		 */
		String placeEntity = "";
//		String placeEntity = findActorforWhole.FindAllActorforWhole(content);
//		System.out.println(".............................................placeEntity:"+placeEntity);
		if (newsTitle.contains(" ") || (newsTitle.contains("\\s") || newsTitle.contains("\\t"))) {
			titleEventItem.setIf_event(false);
		} else {
			/**
			 * 对新闻标题进行事件抽取
			 */
			Pair<String, LabelItem> titleResult = extractbysentence(newsURL, imgAddress, newsID, saveTime, newsTitle,newsTitle,
					placeEntity, true);

			/**
			 * 处理对标题进行事件抽取的结果
			 */

			if (titleResult != null && titleResult.second != null) {
				/*
				 * 新闻标题依然是标题本身
				 */
				titleResult.second.newsTitle = newsTitle;

				/*
				 * 标题情感句为标题本身
				 */
				String sentimentText = newsTitle;
				/**
				 * 新闻情感
				 */
				String sentimentResult = this.polarityBasic.findSentiment(titleResult.second);

				/*
				 * 将标题事件抽取结果加入到eventList列表
				 */
				titleEventItem = new EventItem(titleResult.first, true);
				titleResult.second.newsID = newsID + "_" + comtentIndex;
				titleEventItem.setCon_result(titleResult);// 新闻处理结果
				titleEventItem.setIf_event(true);
				titleEventItem.setRelatedtime(1);// 新闻引用次数
				titleEventItem.setSentiment(sentimentResult);// 新闻情感极性
				titleEventItem.setSentiment_text(newsTitle);// 新闻情感句子
				titleEventItem.setIf_title(true);// 新闻是否为标题
				comtentIndex++;
			}
		}

		EventList.add(titleEventItem);
		/**
		 * 处理正文中所有的句子,对正文每一句话抽取事件
		 */
		if (tempEventList == null || tempEventList.size() == 0)
			return EventList;

		extractEventforContent(newsID, comtentIndex, newsURL, imgAddress, saveTime, newsTitle, placeEntity,
				tempEventList);

		/**
		 * 正文中的每一句话的事件处理结果放入最终的事件List
		 */
		for (EventItem event : tempEventList)
			EventList.add(event);

//		System.out.println("抽取结束后当前新闻中事件数量：" + EventList.size());
		/**
		 * 对每一个事件抽取情感
		 */
		return this.combineEvent.ExtractSentiment(EventList);
	}

	/**
	 *  从所有正文中抽取事件句，已经保证当前输入的每个句子中至少有一个实体
	 * @param newsID
	 * @param comtentIndex
	 * @param newsUrl
	 * @param imgAddress
	 * @param saveTime
	 * @param newsTitle
	 * @param placeEntity
	 * @param eventList
	 * @throws SQLException
	 */
	public void extractEventforContent(String newsID, int comtentIndex, String newsUrl, String imgAddress,
			String saveTime, String newsTitle, String placeEntity, List<EventItem> eventList) throws SQLException {
		// TODO Auto-generated method stub
		for (int i = 0; i < eventList.size(); i++) {
			/**
			 * 新建对象，存储事件抽取的结果
			 */
			EventItem eventItem = eventList.get(i);
			
			String sentence = eventItem.getNews_content();
			try {
				/**
				 *  对正文中的每一句话做事件抽取;当前句子是摘要句，且当前句子不是新闻格式句时，作为候选事件句
				 */
				if (eventList.get(i).isIf_newsTemplte() == false && !(sentence == null || sentence.trim().equals(""))) {
					/**
					 * 对每一句话，使用新的类别判断来判断其是否为第一类，若是，则丢情感极性判断，
					 */
					if (this.runDetection.eventTypeExtractor.hasFirstType(sentence)) {
						List<String> words = new ArrayList();
						Segmentor.segment(sentence, words);
						eventItem.setSentiment(this.polarityBasic.findsentiment(sentence, words));
						eventItem.setIf_sentiment(true);
					}

					/**
					 *  对每一句话，做事件抽取
					 */
					Pair<String, LabelItem> conresult = extractbysentence(newsUrl, imgAddress,
							newsID.concat("_" + String.valueOf(++comtentIndex)), saveTime,newsTitle, sentence, placeEntity,
							eventList.get(i).isIf_summary());

					if (!(conresult == null || conresult.second == null || conresult.second.eventType < 1
							|| conresult.second.eventType > 20 || conresult.second.triggerWord == null
							|| conresult.second.triggerWord.trim().equals(""))) {
						
						eventItem.setIf_title(false);// 当前新闻句子不是标题句
						eventItem.setIf_event(true);//  当前新闻句子为事件句
						eventItem.setRelatedtime(1);//  当前新闻事件引用次数
						eventItem.setCon_result(conresult); //当前新闻事件的抽取结果
						eventItem.setSentiment(this.polarityBasic.findSentiment(conresult.second));//当前新闻事件的情感抽取结果
					} else {
						eventList.get(i).setIf_event(false);
					}
				}

			} catch (Exception e) {
				continue;
			}
		}
	}

	/**
	 *
	 * 
	 * @param newsURL
	 * @param imgAddress
	 * @param newsID
	 * @param saveTime
	 * @param newsTitle
	 * @param sentence
	 * @param placeEntity
	 * @param placeEntity2 
	 * @param isSummary
	 * @return Pair<String, LabelItem> ("当前事件句"，"事件抽取结果");
	 * @throws SQLException
	 */
	public Pair<String, LabelItem> extractbysentence(String newsURL, String imgAddress, String newsID, String saveTime,
			String newsTitle, String sentence, String placeEntity, boolean isSummary) throws SQLException {
//		System.out.println("处理的句子:"+sentence);
		/**
		 * 判空
		 */
		if (sentence == null || sentence.trim().equals(""))
			return null;

		sentence = methods.PreInputTrim(sentence);// 去括号（）
		sentence = methods.PreInputTrim1(sentence);// 去括号【】
		sentence = methods.PreInputTrim2(sentence);// 去括号()
		sentence = methods.removeChinese(sentence);

		if (sentence == null || sentence.trim().equals(""))
			return null;

		boolean isTrigerTemplate = false;

		/**
		 * 抽取触发词和事件类别
		 */
		Data dataResult = this.runDetection.GetEventInforfromNews_Rule(newsURL, newsID, saveTime, sentence);

		if (dataResult != null && dataResult.data != null && dataResult.data.triggerTemplate != null
				&& dataResult.data.triggerTemplate.second != null
				&& !dataResult.data.triggerTemplate.second.trim().equals(""))
			isTrigerTemplate = true;

		/**
		 * 当前既不是模板，也不是摘要，则不作进一步处理
		 */
		if (isTrigerTemplate == false && isSummary == false)
			return null;

		/**
		 * 进一步使用模板和模型进行事件抽取
		 */
		dataResult = this.runDetection.GetEventInforfromNews_MLearning(dataResult);

		if (dataResult == null || dataResult.data == null || dataResult.data.triggerWord == null
				|| dataResult.data.triggerWord.equals(""))
			return null;

		if (dataResult.data.eventType < 1 || dataResult.data.eventType > 20)
			return null;

		Pair<String, Data> title_result = new Pair<String, Data>(sentence, dataResult);

		/**
		 * 抽取source、target
		 */
		this.roleExtract.extractactor(title_result);

		if (title_result == null || title_result.second == null || title_result.second.data == null)
			return null;
		/**
		 * 如果当前source为空的话，不作为一个事件
		 */
		if (title_result.second.data.sourceActor == null || title_result.second.data.sourceActor.trim().equals(""))
			return null;

		/**
		 * 去掉target和source中重复的实体
		 */
		this.processActor.Finalprocess(title_result.second.data);

		/**
		 * 处理事件发生的时间
		 */
		this.timeExraction.setTimebyrule(title_result.second.words, title_result.second.tags,
				title_result.second.data, saveTime);
		/**
		 * 事件的发生时间的时间戳表示
		 */
		title_result.second.data.saveTime = this.timeExraction.String2Time(title_result.second.data.eventTime, saveTime);

		/**
		 * 事件句子中所有的命名实体和人名
		 */
		title_result.second.data.actorItem = Ner.ner_actionitem(sentence);
		title_result.second.data.allPerson = findActorbyDB.findallperson(title_result.second.data.actorItem);

		/**
		 * 对事件抽取结果补全赋值
		 */
		title_result.second.data.newsTitle = newsTitle;
		title_result.first = sentence;
		title_result.second.data.setValues(imgAddress, newsURL, newsID, placeEntity, true, sentence);
		methods.RemoveNull(title_result.second);
		return new Pair<String, LabelItem>(title_result.first, title_result.second.data);
	}
	public Pair<String, LabelItem> extractbysentence(String sentence) throws SQLException {
//		System.out.println("处理的句子:"+sentence);
		/**
		 * 判空
		 */
		String newsURL = null;
		String imgAddress = null;
		String newsID = null;
		String saveTime = null;
		String newsTitle = null;
		String placeEntity = null;
		boolean isSummary = false;
		if (sentence == null || sentence.trim().equals(""))
			return null;

		sentence = methods.PreInputTrim(sentence);// 去括号（）
		sentence = methods.PreInputTrim1(sentence);// 去括号【】
		sentence = methods.PreInputTrim2(sentence);// 去括号()
		sentence = methods.removeChinese(sentence);

		if (sentence == null || sentence.trim().equals(""))
			return null;

		boolean isTrigerTemplate = false;

		/**
		 * 抽取触发词和事件类别
		 */
		Data dataResult = this.runDetection.GetEventInforfromNews_Rule(newsURL, newsID, saveTime, sentence);

		if (dataResult != null && dataResult.data != null && dataResult.data.triggerTemplate != null
				&& dataResult.data.triggerTemplate.second != null
				&& !dataResult.data.triggerTemplate.second.trim().equals(""))
			isTrigerTemplate = true;

		/**
		 * 当前既不是模板，也不是摘要，则不作进一步处理
		 */
		if (isTrigerTemplate == false && isSummary == false)
			return null;

		/**
		 * 进一步使用模板和模型进行事件抽取
		 */
		dataResult = this.runDetection.GetEventInforfromNews_MLearning(dataResult);

		if (dataResult == null || dataResult.data == null || dataResult.data.triggerWord == null
				|| dataResult.data.triggerWord.equals(""))
			return null;

		if (dataResult.data.eventType < 1 || dataResult.data.eventType > 20)
			return null;

		Pair<String, Data> title_result = new Pair<String, Data>(sentence, dataResult);

		/**
		 * 抽取source、target
		 */
		this.roleExtract.extractactor(title_result);

		if (title_result == null || title_result.second == null || title_result.second.data == null)
			return null;
		/**
		 * 如果当前source为空的话，不作为一个事件
		 */
		if (title_result.second.data.sourceActor == null || title_result.second.data.sourceActor.trim().equals(""))
			return null;

		/**
		 * 去掉target和source中重复的实体
		 */
		this.processActor.Finalprocess(title_result.second.data);

		/**
		 * 处理事件发生的时间
		 */
		this.timeExraction.setTimebyrule(title_result.second.words, title_result.second.tags,
				title_result.second.data, saveTime);
		/**
		 * 事件的发生时间的时间戳表示
		 */
		title_result.second.data.saveTime = this.timeExraction.String2Time(title_result.second.data.eventTime, saveTime);

		/**
		 * 事件句子中所有的命名实体和人名
		 */
		title_result.second.data.actorItem = Ner.ner_actionitem(sentence);
		title_result.second.data.allPerson = findActorbyDB.findallperson(title_result.second.data.actorItem);

		/**
		 * 对事件抽取结果补全赋值
		 */
		title_result.second.data.newsTitle = newsTitle;
		title_result.first = sentence;
		title_result.second.data.setValues(imgAddress, newsURL, newsID, placeEntity, true, sentence);
		methods.RemoveNull(title_result.second);
		return new Pair<String, LabelItem>(title_result.first, title_result.second.data);
	}
	public static void main(String[] args) throws SQLException {
		// TODO Auto-generated method stub	
		
		String newsID = "1234564676";
		String newsURL = "sa";
		String imagAddress = "wegtaw";
		String saveTime = "rywety";
		String newsTitle = "欧洲忐忑看俄导弹压境";
		String newsContent =  "23日，美军士兵在立陶宛首都维尔纽斯参加北约“铁剑2016”军事演习的阅兵式。 　　“俄罗斯的新举动惹恼了北约。”22日，针对俄在波罗的海部署导弹，北约抨击俄举动是“侵略性军事姿态”。俄罗斯反唇相讥称，正是北约不断在该地区加强军力部署，俄才不得不有所回应。观察人士认为，这是俄罗斯展开的一场“心理战”，希望让北约处于压力之下。22日，欧洲议会投票通过加强成员国间防务合作的计划，呼吁建立“欧洲防务联盟”。当天议会还讨论一项议案，打算把俄罗斯和“伊斯兰国”的宣传威胁相等同。眼下的欧洲有些忐忑，一大原因就是北约“盟主”美国要换领导人了——美国当选总统特朗普曾表示北约已经“过时”，他同普京的“惺惺相惜”引发外界对俄美快速和好的猜想。“特朗普与普京的关系将遭遇北约考验”，美国全国广播公司援引专家的话称，一旦特朗普对俄太过友善，或对盟国的承诺有所放松，俄就将步步进逼，测试北约的决心。 　　北约与俄罗斯大打嘴仗 　　“俄罗斯在叙利亚使用过的导弹现在对准了欧洲大陆目标。”德国新闻电视台23日称，俄罗斯已在其接壤波兰及立陶宛的“飞地”加里宁格勒部署“堡垒”导弹发射器，可发射超音速P-800型巡航导弹。上周，俄军空袭叙利亚境内的极端分子时，曾利用“堡垒”发射导弹攻击目标。另外，俄还将在加里宁格勒部署“伊斯坎德尔”战术弹道导弹和S-400防空导弹系统，并在西部和南部军区创建新部队。 　　俄罗斯的动作令欧美震惊。22日下午，北约发表声明，批评俄罗斯的举动不利于缓解紧张局势，同时敦促俄加大军事透明度，避免发生误判事件。英国《每日电讯报》称，北约称俄举动是“侵略性军事姿态”。英国广播公司称，“俄导弹部署惹怒了北约”。此前，美国国务院批评俄“破坏欧洲安全稳定”。 　　奥地利《新闻报》23日称，俄罗斯的部署是永久性的。立陶宛和波兰之间的1.5万平方公里区域都将处于加里宁格勒防空系统下。“波罗的海国家担心自己的安全”，德国《南德意志报》23日评论道。 　　俄罗斯迅速做出回击。“在北约向俄罗斯边界扩张的背景下，俄正在做一切必要的事情来保护自己”，俄总统新闻秘书佩斯科夫22日说。 　　“今日俄罗斯”(RT)电视台22日称，华盛顿称北约是一个“防御性联盟”，对莫斯科没有威胁。但与此同时，更多坦克和军队被部署到了波罗的海。从20日开始，北约在立陶宛举行“铁剑2016”军事演习，有4000名士兵参加，是目前为止此类军演中规模最大的。“在俄边界附近举行军演之际，北约还打算向波兰和波罗的海三国派驻4000人的多国部队”，报道称。 　　今年7月北约作出上述驻军决定，现在部署已经开始，在波兰有1200人，其中1000人是美军。他们驻扎在距俄罗斯边界60公里处的奥日什。今年5月，美国在罗马尼亚启动第一阶段陆基导弹防御系统，另一阶段将于2018年在波兰开始运作。 　　“心理战，新冷战。”《柏林日报》称，许多观察家认为，这是俄罗斯的一场“心理战”，希望让北约处于压力之下。随着北约与俄相互升级军事行动，新冷战越来越近。 　　长期困扰俄罗斯与西方关系的乌克兰问题也有新事态。据俄罗斯《观点报》22日报道，俄国防部指责乌安全人员非法在克里米亚拘捕两名俄罗斯合同兵，并试图针对他们捏造刑事案件。莫斯科认为，绑架行为是粗野的挑衅，要求乌方立即将他们送回。 　　英国《每日电讯报》称，绑架问题让俄乌紧张关系升级。基辅方面称，这两人曾在乌军服役，2014年3月叛逃俄罗斯。 　　欧洲在忧虑中等待 　　俄罗斯的军事部署并非突然之举。俄罗斯“雨”电视台22日以“普京允许俄导弹系统瞄准北约设施”为题报道称，著名导演奥利弗·斯通拍摄的《火焰中的乌克兰》21日在俄电视台首播，普京在片中接受采访称，“当一个国家成为北约成员国，这个国家将非常难以抵抗来自北约领袖美国这样的大国的压力，于是北约可以在那里随意部署任何东西，比如反导系统和新基地，如果需要，还可以部署攻击系统。我们怎么办？我们应该在这方面回击，也就是说，把那些在我们看来开始威胁我们的设施置于我国导弹系统打击之下。” 　　澳大利亚新闻网23日称，俄罗斯在加里宁格勒这个欧洲要害位置部署导弹，目的是为在与美国新当选总统特朗普政府的谈判中获得一个筹码，并树立俄令人敬畏大国的形象。英国皇家联合国防研究所的苏佳金认为，部署导弹本身从军事上看不会太多改变现状，但时机的选择有政治意义。 　　英国《卫报》22日称，欧洲正带着忧虑等着看特朗普如何影响欧洲大陆的防务安排。特朗普曾表示，美国为北约承担了太多军费，这个盟友已经“过时”，美俄在叙利亚问题上形成联盟是可能的。这些表态让许多欧洲国家惊恐不已，尤其是与俄罗斯接壤的3个波罗的海国家。 　　鉴于此，欧盟决定加快发展自己的力量。22日，欧洲议会以369：255的投票通过一个在未来1年内组建欧盟自主军事防御体系的非立法性决议，支持组建欧盟联合军队和欧盟军事司令部。据了解，英国首相梅将于23日在唐宁街会见北约秘书长斯托尔滕贝格，预计会让他去说服欧洲成员国践行对北约的承诺，将国民收入的2％用于国防。 　　22日，欧洲议会还针对一项如何抵制俄罗斯和“伊斯兰国”（IS）宣传威胁的决议草案展开讨论。这份文件称，俄对欧盟国家实施敌对宣传，普京当局拨款资助欧盟国家的政治反对派、社会组织等。文件称，欧盟各国应增加对媒体拨款，支持俄公民社会以及同北约和东欧伙伴国更紧密合作。 　　“美国之音”称，有关文件特别提到俄罗斯一些宣传媒体和组织的危害，包括：俄罗斯卫星传媒平台，RT电视台，俄罗斯的一些社交媒体，智库研究中心以及网络五毛等。议员们将在周三针对这项决议草案投票，预计将会通过。 　　北约将成为特朗普和普京争执的源头？ 　　“特朗普对北约的态度到底如何，不得而知。”澳大利亚新闻网称，竞选期间特朗普说了很多，但当选后，他的立场似乎在软化。特朗普已经与北约秘书长斯托尔滕伯格交谈过，双方都强调美国和北约的盟友关系有着持久重要性。 　　加拿大《环球邮报》23日称，特朗普与普京达成谅解协定，让普京控制自己的势力范围，而换取在其他很多方面的合作和和平，这有何不可？当然，很多人会对特朗普和普京达成和解协议表示怀疑。美俄已经在乌克兰、北约和叙利亚问题上冲突良久。 　　英国《金融时报》日前发表的社评分析了特朗普面临的选择。文章称，对特朗普来说，与普京达成某种协议的吸引力又大又明显。从政治上说，这将大大有助于特朗普从一个大亨、明星和政治局外人转变成一位政治家。但全世界一定希望特朗普还能看到与俄罗斯达成任何大妥协必然伴随的巨大风险。 　　“上周，普京表示，如果北约继续东扩，俄罗斯将让其窒息。现在，俄罗斯又宣布计划在加里宁格勒部署导弹防御系统”，美国全国广播公司22日称，北约将考验特朗普和普京之间的关系，“这个组织将成为二人争执的源头。” 　　报道称，英国智库学者钱伯斯分析说，特朗普将和俄罗斯再次“重启”两国关系，并和奥巴马一样不愿介入乌克兰危机，但不会在对北约和欧盟的立场上后撤太多。国际咨询公司IHS高级研究员努尔金认为，如果美国对北约减少承诺，将促使俄罗斯进行试探。“一旦美国对俄过于友好，或放弃对北约、东欧和波罗的海盟友的承诺，我们将看到，俄罗斯会步步进逼，测试北约的决心”。 　　对于外界的关注，特朗普22日给出部分回应。据《纽约时报》报道，特朗普表示，他希望与普京和睦相处，但不支持“重启”两国间关系的概念。 　　中国社科院俄罗斯问题专家吴恩远23日对《环球时报》说，俄罗斯部署导弹的行动是在特朗普上台以前就计划好的，是对俄美、俄欧紧张关系的延续和继承。特朗普上台以后，不排除俄罗斯会重新考虑美俄关系。但无论谁执政，美俄关系有一些根本性问题尚未解决。 　　复旦大学国际问题研究院常务副院长吴心伯告诉《环球时报》记者，目前来看，特朗普很有可能会改善美俄关系。美军在北欧和东欧的部署计划是奥巴马政府做出来的，如果特朗普寻求改善美俄关系的话，会在乌克兰问题和北欧问题上有所调整。可以设想特朗普可能会将乌克兰问题交给欧洲国家，如德国或法国。 　　“和俄罗斯合作是可能的，但取决于特朗普想实现什么目标。”美国《大西洋月刊》22日回顾了苏联解体以来美俄的交往，特别提到双方合作与对抗的过程。文章称，同普京的俄罗斯打交道，特朗普必须认真思考他想要避免什么，想要实现什么，当然，前提是不危及欧洲享受了71年的和平与繁荣。";
		String sentence = "中智将关系提升至全面战略伙伴关系";
		String placeEntity = "菲律宾";
		//new object
		EventExtract bsl = new EventExtract();
		/**
		 *全文事件抽取结果
		 */
		List<EventItem> eventList = bsl.eventExtractforWhole(newsID, newsURL, imagAddress, saveTime, newsTitle,newsContent);
		for(EventItem event : eventList)
		{
			Pair<String, LabelItem> result = event.getCon_result();
			if(result != null && result.getSecond() != null)
			{
				System.out.println(result.getFirst());
				String eventResult = result.getSecond().triggerWord+","+result.getSecond().sourceActor+','+result.getSecond().targetActor
						+result.getSecond().eventType;
				System.out.println(eventResult);
			}
			
			
		}
		
		
		/**
//		 * 单个句子抽取事件结果
//		 */
//		Pair<String, LabelItem> result = bsl.extractbysentence(newsURL, imagAddress, newsID, saveTime, newsTitle, sentence, placeEntity, true);
//		try
//		{
//			/**
//			 * 找地点及各个经纬度
//			 */
//			bsl.geonamesUtil.findAllProperties(result.second);
//			result.second.Print();
//		} catch (Exception e1)
//		{
//			// TODO Auto-generated catch block
//			System.out.println("当前非事件句");
//		}
//		
		
	}
}
