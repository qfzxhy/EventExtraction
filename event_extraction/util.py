import codecs
import os
import re
import xml.etree.ElementTree as ET
from collections import Counter
from nltk import word_tokenize
from opinionWordExtraction.nlp_tools import nlp_tokenizer
laptop14_path = ['./laptop14_data/Laptops_Train.xml','./laptop14_data/Laptops_Test_Data_phaseB.xml']

res14_path = ['./res14_data/Restaurants_Train_v2.xml','./res14_data/Restaurants_Test_Data_phaseB.xml']

res15_path = ['./res15_data/ABSA-15_Restaurants_Train_Final.xml','./res15_data/ABSA15_Restaurants_Test.xml']

bio_laptop14_path = ['./laptop14_data/bio_laptop_2014_train','./laptop14_data/bio_laptop_2014_test']

bio_res14_path = ['./res14_data/bio_res_2014_train','./res14_data/bio_res_2014_test']

bio_res15_path = ['./res15_data/bio_res_2015_train','./res15_data/bio_res_2015_test']

op_laptop14_path = ['./laptop14_data/laptop14_op_train.txt','./laptop14_data/laptop14_op_test.txt']

op_res14_path = ['./res14_data/res14_op_train.txt','./res14_data/res14_op_test.txt']

op_res15_path = ['./res15_data/res15_op_train.txt','./res15_data/res15_op_test.txt']




def count_pre_space(text):
    count = 0
    for i in range(len(text)):
        if text[i].isspace():
            count+=1
        else:
            break
    return count
def count_mid_space(text,pos):
    count = 0
    for i in range(len(text) - pos):
        if text[pos+i].isspace():
            count += 1
        else:
            break
    return count
def is_overlap(x1,x2,y1,y2):
    return x1 <= y2 and y1 <= x2

def get_token_id(from_id,to_id,text):
    pre_space_count = count_pre_space(text)
    ids = []
    tokens = text.split()
    cur_id = pre_space_count
    for i,token in enumerate(tokens):
        if is_overlap(cur_id,cur_id+len(token)-1,from_id,to_id-1):
            ids.append(i)
        cur_id = cur_id + len(token) + count_mid_space(text,cur_id + len(token))
    return ids
    pass
def get_token_id_no_space(from_id,to_id,tokens):
    ids = []
    alphabet_id = 0
    for i, token in enumerate(tokens):
        if is_overlap(alphabet_id, alphabet_id + len(token) - 1, from_id, to_id - 1):
            ids.append(i)
        alphabet_id = alphabet_id + len(token)
    return ids
def clean_string(string):
    string = re.sub(r"[^A-Za-z0-9(),!?\'\`]", " ", string)
    return string.strip().lower()
def generate_laptop_14_BIO_data(root):
    BIO_datas = []
    all_entitys = []
    for sentence in root:
        text = sentence.find('text').text.lower()
        text = clean_string(text)
        entitys = []
        if len(text.strip()) != 0:
            text = text.replace('"',"'")
            text = text.replace('/','#')
            # text = text.replace('-','#')
            nltk_tokens = word_tokenize(text)
            bio_data = [[word.lower(), 'O'] for word in nltk_tokens]
            for asp_terms in sentence.iter('aspectTerms'):
                for asp_term in asp_terms.findall('aspectTerm'):
                    from_id = int(asp_term.get('from'))
                    to_id = int(asp_term.get('to'))
                    entitys.append(asp_term.get('term').lower())
                    space_count1 = text[:from_id].count(' ')
                    space_count2 = text[:to_id].count(' ')
                    alphabet_from_id = from_id - space_count1
                    alphabet_to_id = to_id - space_count2
                    ids = get_token_id_no_space(alphabet_from_id, alphabet_to_id, nltk_tokens)
                    for id in ids:
                        if id == ids[0]:
                            bio_data[id][1] = 'B-target'
                        else:
                            bio_data[id][1] = 'I-target'
            BIO_datas.append(bio_data)
            all_entitys.append(entitys)

    return BIO_datas,all_entitys
    pass
def compute_res_15_target_num(ta_fname,op_fname):
    tree = ET.parse(ta_fname)
    root = tree.getroot()
    count = 0
    for review in root:
        for sentences in review.iter('sentences'):
            for sentence in sentences.findall('sentence'):
                text = sentence.find('text').text.lower()
                if len(text.strip()) != 0:
                    for asp_terms in sentence.iter('Opinions'):
                        ids = {}
                        for asp_term in asp_terms.findall('Opinion'):
                            target = asp_term.get('target')
                            from_id = int(asp_term.get('from'))
                            to_id = int(asp_term.get('to'))
                            if to_id != 0 and to_id not in ids:
                                count += 1
                                ids[to_id] = 1
    print(count)
    opword_num = 0
    with codecs.open(op_fname,'r','utf-8') as reader:
        for line in reader.readlines():
            line = line.strip()
            opword_tmp = []
            if '##'in line:
                opinions = line.split('##')[1].split(', ')
                for opinion in opinions:
                    opinion = opinion.strip()
                    opinionword = ' '.join(opinion.split()[:-1])
                    opword_tmp.append(opinionword.lower())
                    opword_num += 1

    print(opword_num)

    pass
def generate_res_15_BIO_data(root):
    BIO_datas = []
    for review in root:
        for sentences in review.iter('sentences'):
            for sentence in sentences.findall('sentence'):
                text = sentence.find('text').text.lower()
                text = clean_string(text)
                if len(text.strip()) != 0:
                    text = text.replace('"', "'")
                    text = text.replace('/', '#')
                    nltk_tokens = word_tokenize(text)
                    bio_data = [[word,'O'] for word in nltk_tokens]
                    for asp_terms in sentence.iter('Opinions'):
                        for asp_term in asp_terms.findall('Opinion'):
                            from_id = int(asp_term.get('from'))
                            to_id = int(asp_term.get('to'))
                            space_count1 = text[:from_id].count(' ')
                            space_count2 = text[:to_id].count(' ')
                            alphabet_from_id = from_id - space_count1
                            alphabet_to_id = to_id - space_count2

                            ids = get_token_id_no_space(alphabet_from_id,alphabet_to_id,nltk_tokens)
                            # print(ids)
                            for id in ids:
                                if id == ids[0]:
                                    bio_data[id][1] = 'B-target'
                                else:
                                    bio_data[id][1] = 'I-target'
                    BIO_datas.append(bio_data)

    return BIO_datas
def generate_res_14_BIO_data(root):
    BIO_datas = []
    for sentence in root:
        text = sentence.find('text').text.lower()
        if len(text.strip()) != 0:
            text = text.replace('"', "'")
            text = text.replace('/', '#')
            nltk_tokens = word_tokenize(text)
            bio_data = [[word.lower(),'O'] for word in nltk_tokens]
            for asp_terms in sentence.iter('aspectTerms'):
                for asp_term in asp_terms.findall('aspectTerm'):
                    from_id = int(asp_term.get('from'))
                    to_id = int(asp_term.get('to'))
                    space_count1 = text[:from_id].count(' ')
                    space_count2 = text[:to_id].count(' ')
                    alphabet_from_id = from_id - space_count1
                    alphabet_to_id = to_id - space_count2

                    ids = get_token_id_no_space(alphabet_from_id, alphabet_to_id, nltk_tokens)
                    for id in ids:
                        if id == ids[0]:
                            bio_data[id][1] = 'B-target'
                        else:
                            bio_data[id][1] = 'I-target'
            BIO_datas.append(bio_data)

    return BIO_datas

def merge_target_opininword(bio_fname,op_fname):
    opwords = []
    opword_num = 0
    with codecs.open(op_fname,'r','utf-8') as reader:
        for line in reader.readlines():
            line = line.strip()
            opword_tmp = []
            if '##'in line:
                opinions = line.split('##')[1].split(', ')
                for opinion in opinions:
                    opinion = opinion.strip()
                    opinionword = ' '.join(opinion.split()[:-1])
                    opword_tmp.append(opinionword.lower())
                    opword_num += 1
            opwords.append(opword_tmp)
    print(opword_num)
    bio_datas = []
    with codecs.open(bio_fname,'r','utf-8') as reader:
        for lineid,line in enumerate(reader.readlines()):
            print(lineid)
            print(line)
            bio_data = eval(line.strip())

            tokens = [unit[0] for unit in bio_data]
            labels = [unit[1] for unit in bio_data]
            opinions = opwords[lineid]
            fflag = False
            for opinion in opinions:
                opinion = opinion.replace(' ','')
                print(opinion)
                flag = False
                for i in range(len(tokens)):
                    for j in range(i,len(tokens)):
                        j = j + 1
                        if ''.join(tokens[i:j]) not in opinion:
                            break
                        if ''.join(tokens[i:j]) == opinion and 'B-target' not in ''.join(labels[i:j]):
                            flag = True
                            for k in range(i,j):
                                if k == i:
                                    bio_data[k][1] = 'B-opword'
                                else:
                                    bio_data[k][1] = 'I-opword'
                # if flag == False:
                #     print(bio_data)
                #     fflag = True
                #     break

            # if fflag:
            #     break
            bio_datas.append(bio_data)
    with codecs.open(bio_fname+'_op','w','utf-8') as writer:
        for bio_data in bio_datas:
            writer.write(str(bio_data) + '\n')


    pass
def merge_target_opininword_14(bio_fname,op_fname):
    opwords = []
    opword_num = 0
    with codecs.open(op_fname,'r','utf-8') as reader:
        for line in reader.readlines():
            line = line.strip()
            opword_tmp = []
            opinions = line.split(', ')
            for opinion in opinions:
                opinion = opinion.strip()
                opinionword = ' '.join(opinion.split()[:-1])
                opword_tmp.append(opinionword.lower())
                opword_num += 1
            opwords.append(opword_tmp)
    print(opword_num)
    bio_datas = []
    with codecs.open(bio_fname,'r','utf-8') as reader:
        for lineid,line in enumerate(reader.readlines()):
            # print(lineid)
            # print(line)
            bio_data = eval(line.strip())

            tokens = [unit[0] for unit in bio_data]
            labels = [unit[1] for unit in bio_data]

            opinions = opwords[lineid]
            fflag = False
            for opinion in opinions:
                opinion = opinion.replace(' ','')
                print(opinion)
                flag = False
                for i in range(len(tokens)):
                    for j in range(i,len(tokens)):
                        j = j + 1
                        if ''.join(tokens[i:j]) not in opinion:
                            break
                        if ''.join(tokens[i:j]) == opinion and 'B-target' not in ''.join(labels[i:j]):
                            flag = True

                            for k in range(i,j):
                                if k == i:
                                    bio_data[k][1] = 'B-opword'
                                else:
                                    bio_data[k][1] = 'I-opword'
                # if flag == False:
                #     print(bio_data)
                #     fflag = True
                #     break

            # if fflag:
            #     break
            bio_datas.append(bio_data)
    with codecs.open(bio_fname+'_op','w','utf-8') as writer:
        for bio_data in bio_datas:
            writer.write(str(bio_data) + '\n')


    pass

def generate_laptop_14_BIO_target_op():
    for name_index,fname in enumerate(laptop14_path):
        tree = ET.parse(fname)
        root = tree.getroot()
        # source_data, source_loc_data, target_data, target_label = list(), list(), list(), list()
        BIO_datas,all_entitys = generate_laptop_14_BIO_data(root)
        with codecs.open(bio_laptop14_path[name_index],'w','utf-8') as writer:
            for bio_data in BIO_datas:
                writer.write(str(bio_data)+'\n')
        merge_target_opininword_14(bio_laptop14_path[name_index],op_laptop14_path[name_index])
def generate_res_14_BIO_target_op():
    for name_index, fname in enumerate(res14_path):
        tree = ET.parse(fname)
        root = tree.getroot()
        # source_data, source_loc_data, target_data, target_label = list(), list(), list(), list()
        BIO_datas = generate_res_14_BIO_data(root)
        with codecs.open(bio_res14_path[name_index],'w','utf-8') as writer:
            for bio_data in BIO_datas:
                writer.write(str(bio_data)+'\n')
        merge_target_opininword_14(bio_res14_path[name_index],op_res14_path[name_index])
def generate_res_15_BIO_target_op():
    for name_index, fname in enumerate(res15_path):
        tree = ET.parse(fname)
        root = tree.getroot()
        # source_data, source_loc_data, target_data, target_label = list(), list(), list(), list()
        BIO_datas = generate_res_15_BIO_data(root)
        with codecs.open(bio_res15_path[name_index],'w','utf-8') as writer:
            for bio_data in BIO_datas:
                writer.write(str(bio_data)+'\n')

        merge_target_opininword(bio_res15_path[name_index],op_res15_path[name_index])

def extractEntity(sentence, labels):
    import re
    entitys = []
    pattern = re.compile(r'(0)(1+)*')
    m = pattern.search(labels)
    while m:
        entity_label = m.group()
        label_start_index = labels.find(entity_label)
        label_end_index = label_start_index + len(entity_label)
        word_start_index = label_start_index
        word_end_index = label_end_index
        entitys.append(' '.join(sentence[word_start_index:word_end_index]))
        labels = list(labels)
        labels[:label_end_index] = ['2' for _ in range(word_end_index)]
        labels = ''.join(labels)
        m = pattern.search(labels)
    return entitys
def convert_laptop14_label_p_to_opinion(fname):
    all_labels = []
    with codecs.open('./laptop14_data/train_labels_p.txt','r','utf-8') as reader:
        for line in reader.readlines():
            all_labels.append(''.join(line.strip().split()))
    print(all_labels)
    tree = ET.parse(fname)
    root = tree.getroot()


    writer = codecs.open('laptop14_op_train.txt','w','utf-8')
    i = 0
    for sentence in root:
        text = sentence.find('text').text.lower()
        tokens = text.strip().split()
        print(tokens)
        entitys = extractEntity(tokens,all_labels[i])
        i += 1
        for j,entity in enumerate(entitys):
            writer.write(entity)
            if j != len(entitys) - 1:
                writer.write(', ')
        writer.write('\n')

def assert_entity(fname):
    tree = ET.parse(fname)
    root = tree.getroot()
    BIO_datas, all_entitys = generate_laptop_14_BIO_data(root)
    def extract(sentence, labels):
       entitys = []
       pattern = re.compile(r'(B-[^BIO]+)(I-[^BIO]+)*')
       m = pattern.search(labels)
       while m:
           entity_label = m.group()
           # print(entity_label)
           label_start_index = labels.find(entity_label)
           label_end_index = label_start_index + len(entity_label)
           word_start_index = labels[:label_start_index].count('-') + labels[:label_start_index].count('O')
           word_end_index = word_start_index + entity_label.count('-')
           entitys.append(' '.join(sentence[word_start_index:word_end_index]))
           labels = list(labels)
           labels[:label_end_index] = ['O' for _ in range(word_end_index)]
           labels = ''.join(labels)
           m = pattern.search(labels)
       return entitys
    writer = codecs.open('laptop14_data/assert','w','utf-8')
    with codecs.open('laptop14_data/bio_laptop_2014_train_op','r','utf-8') as reader:
       for i,line in enumerate(reader.readlines()):
           line = line.strip()
           units = eval(line)

           sentence = [unit[0] for unit in units]
           # print(sentence)
           labels = ''.join([unit[1] for unit in units])
           # print(labels)
           entitys = extract(sentence,labels)
           for e in all_entitys[i]:
               if e not in entitys:
                   print(line)
                   print(entitys)
                   print(all_entitys[i])
           # for j,entity  in enumerate(entitys):
           #     writer.write(entity)
           #     if j != len(entitys) - 1:
           #         writer.write(', ')
           # writer.write('\n')
def res15_xml_data_mention_statistic():
    for name in res15_path:
        tree = ET.parse(name)
        root = tree.getroot()
        opinions_count = 0
        for review in root:
            for sentences in review.iter('sentences'):
                for sentence in sentences.findall('sentence'):
                    opinions_set = set()
                    text = sentence.find('text').text.lower()
                    if len(text.strip()) != 0:
                        for asp_terms in sentence.iter('Opinions'):
                            for asp_term in asp_terms.findall('Opinion'):
                                from_id = int(asp_term.get('from'))
                                to_id = int(asp_term.get('to'))
                                if asp_term.get('target') != 'NULL':
                                    opinions_set.add(str(from_id) + '#' + str(to_id))
                    opinions_count += len(opinions_set)
        # print(opinions_count)
        print('target:' + str(opinions_count))

    pass
def res14_xml_data_mention_statistic():
    for name in res14_path:
        tree = ET.parse(name)
        root = tree.getroot()
        opinions_count = 0
        for sentence in root:
            text = sentence.find('text').text.lower()
            opinions_set = set()
            if len(text.strip()) != 0:
                for asp_terms in sentence.iter('aspectTerms'):
                    for asp_term in asp_terms.findall('aspectTerm'):
                        from_id = int(asp_term.get('from'))
                        to_id = int(asp_term.get('to'))
                        if asp_term.get('target') != 'NULL':
                            opinions_set.add(str(from_id) + '#' + str(to_id))
                    opinions_count += len(opinions_set)
        # print(opinions_count)
        print('target:' + str(opinions_count))
def laptop14_xml_data_mention_statistic():
    for name in laptop14_path:
        tree = ET.parse(name)
        root = tree.getroot()
        opinions_count = 0
        for sentence in root:
            text = sentence.find('text').text.lower()
            if len(text.strip()) != 0:
                opinions_set = set()
                for asp_terms in sentence.iter('aspectTerms'):
                    for asp_term in asp_terms.findall('aspectTerm'):
                        from_id = int(asp_term.get('from'))
                        to_id = int(asp_term.get('to'))
                        if to_id != '0':
                            opinions_set.add(str(from_id) + '#' + str(to_id))
                opinions_count += len(opinions_set)
        print('target:' + str(opinions_count))

def bio_data_mention_statistic():
    for name in bio_laptop14_path:
        mention_num1 = 0
        mention_num2 = 0
        with codecs.open(name,'r','utf-8') as reader:
            for lineid,line in enumerate(reader.readlines()):
                line = line.strip()
                mention_num1 += line.count('B-target')
                mention_num2 += line.count('B-opword')
            print('target:'+str(mention_num1))
            print('opword:' + str(mention_num2))
    for name in bio_res14_path:
        mention_num1 = 0
        mention_num2 = 0
        with codecs.open(name,'r','utf-8') as reader:
            for lineid,line in enumerate(reader.readlines()):
                line = line.strip()
                mention_num1 += line.count('B-target')
                mention_num2 += line.count('B-opword')
            print('target:' + str(mention_num1))
            print('opword:' + str(mention_num2))
    for name in bio_res15_path:
        mention_num1 = 0
        mention_num2 = 0
        with codecs.open(name,'r','utf-8') as reader:
            for lineid,line in enumerate(reader.readlines()):
                line = line.strip()
                mention_num1 += line.count('B-target')
                mention_num2 += line.count('B-opword')
            print('target:' + str(mention_num1))
            print('opword:' + str(mention_num2))
if __name__ == '__main__':
    # read_data("G:/master4/datasets/absa_2015/ABSA-15_Restaurants_Train_Final.xml")

    # compute_res_15_target_num("G:/master4/datasets/absa_2015/ABSA15_Restaurants_Test.xml",'res15_data/res15_op_test.txt')
    # convert_laptop14_label_p_to_opinion('./laptop14_data/Laptops_Train.xml')
    generate_laptop_14_BIO_target_op()
    # generate_res_14_BIO_target_op()
    # generate_res_15_BIO_target_op()
    laptop14_xml_data_mention_statistic()
    # res14_xml_data_mention_statistic()
    # res15_xml_data_mention_statistic()
    # print()
    # bio_data_mention_statistic()
    # assert_entity('./laptop14_data/Laptops_Train.xml')