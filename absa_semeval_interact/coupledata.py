import re
import codecs
import xml.etree.ElementTree as ET
res15_path = ['./res15_data/ABSA-15_Restaurants_Train_Final.xml','./res15_data/ABSA15_Restaurants_Test.xml']
op_res15_path = ['./res15_data/res15_op_train.txt','./res15_data/res15_op_test.txt']

def clean_string(string):
    string = re.sub(r"[^A-Za-z0-9(),!?\'\`]", " ", string)
    return string.strip().lower()
def generate_res_15_BIO_data(root):
    sents = []
    aspects = []
    for review in root:
        for sentences in review.iter('sentences'):
            for sentence in sentences.findall('sentence'):
                text = sentence.find('text').text.lower()

                if len(text.strip()) != 0:
                    sents.append(text.strip())
                    aspects_per_sent = []
                    for asp_terms in sentence.iter('Opinions'):
                        for asp_term in asp_terms.findall('Opinion'):
                            aspect = asp_term.get('target')
                            if aspect != 'NULL':

                                aspects_per_sent.append(aspect)
                    aspects.append(aspects_per_sent)


    return sents,aspects

def get_opinions():
    opwords = []
    sents = []
    with codecs.open(op_res15_path[1], 'r', 'utf-8') as reader:
        for line in reader.readlines():
            line = line.strip()
            sents.append(line.lower().split('##')[0])
            opword_tmp = []
            if '##' in line:
                opinions = line.split('##')[1].split(', ')
                for opinion in opinions:

                    opinion = opinion.strip()
                    opinionword = ' '.join(opinion.split()[:-1])

                    opword_tmp.append(opinionword)
            opwords.append(opword_tmp)
    return sents,opwords

def coupleing():
    tree = ET.parse(res15_path[1])
    root = tree.getroot()
    sents, aspects = generate_res_15_BIO_data(root)
    sents, opinions = get_opinions()
    with codecs.open('res15_data/couple_data','w','utf-8') as writer:
        for sent_id ,sent in enumerate(sents):
            s = sent
            print(sent)
            targets = aspects[sent_id]
            print(targets)
            targets_id = [sent.index(target.lower()) for target in targets]
            opwords = opinions[sent_id]
            print(opwords)
            opwords_id = [sent.index(target.lower()) for target in opwords]
            for i in range(len(targets_id)):
                min_v = 1000
                k = -1
                for j in range(len(opwords_id)):
                    if abs(opwords_id[j] - targets_id[i]) < min_v:
                        min_v = abs(opwords_id[j] - targets_id[i])
                        k = j
                if k != -1:
                    s = s + '\t' + '('+targets[i]+','+opwords[k] + ')'
            writer.write(s + '\n')

tree = ET.parse(res15_path[1])
root = tree.getroot()
sents, aspects = generate_res_15_BIO_data(root)
with codecs.open('res15_data/raw_data', 'w', 'utf-8') as writer:
    for sent_id, sent in enumerate(sents):
        s = sent
        targets = aspects[sent_id]
        writer.write(s + '\t' + ', '.join(targets) + '\n')
coupleing()