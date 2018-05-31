import pandas as pd
import numpy as np
import codecs
import csv
from collections import defaultdict
class Char_Index():
    def __init__(self):
        self.char2idex = defaultdict()
        self.char2idex['<PAD>'] = 0
        self.id2char = defaultdict(int)
        self.load_index()
    def load_index(self):
        with codecs.open('G:/2018/code/event_extraction_system/datas/c_voc.txt','r','utf-8') as reader:
            for chara in reader:
                self.char2idex[chara.strip()] = len(self.char2idex)
        for word in self.char2idex.keys():
            self.id2char[self.char2idex[word]] = word
class Word_Index():
    def __init__(self):
        self.word2idex = defaultdict()
        self.word2idex['<PAD>'] = 0
        self.id2word = defaultdict()
        self.load_index()
    def load_index(self):
        with codecs.open('G:/2018/code/event_extraction_system/datas/w_voc.txt','r','utf-8') as reader:
            for word in reader:
                self.word2idex[word.strip()] = len(self.word2idex)
        for word in self.word2idex.keys():
            self.id2word[self.word2idex[word]] = word
    def get_index(self,word):
        if word not in self.word2idex:
            return 0
        return self.word2idex[word]
class Ptag_Index():
    def __init__(self):
        self.ptag2id = defaultdict(int)
        self.ptag2id['<PAD>'] = 0
        self.id2ptag = defaultdict()
        self.load_index()
    def load_index(self):
        self.ptag2id['a'] = len(self.ptag2id)
        self.ptag2id['b'] = len(self.ptag2id)
        self.ptag2id['v'] = len(self.ptag2id)
        self.ptag2id['n'] = len(self.ptag2id)
        self.ptag2id['nd'] = len(self.ptag2id)
        self.ptag2id['nh'] = len(self.ptag2id)
        self.ptag2id['ni'] = len(self.ptag2id)
        self.ptag2id['ns'] = len(self.ptag2id)
        self.ptag2id['nl'] = len(self.ptag2id)
        # with codecs.open('datas/p_voc.txt','r','utf-8') as reader:
        #     for postag in reader:
        #         self.ptag2id[postag.strip()] = len(self.ptag2id)
        for postag in self.ptag2id.keys():
            self.id2ptag[self.ptag2id[postag]] = postag

class Trigger_Index():
    def __init__(self):
        self.trigger_list = []
        for i in range(20):
            self.trigger_list.append(self.load_dic('G:/2018/code/event_extraction_system/datas/trigger/'+ str(i+1)+'.txt'))
    def load_dic(self,path):
        with codecs.open(path,'r','utf-8') as reader:
            ents = [ent.strip() for ent in reader]
        return set(ents)
    def get_id(self,word):
        for ent_id, ent in enumerate(self.trigger_list):
            if word in ent:
                return ent_id + 1
        return 0
class Sample_Word_Index():
    def __init__(self):
        self.sample_words = self.load_dic('G:/2018/code/event_extraction_system/datas/trigger/sample_words')
    def load_dic(self,path):
        with codecs.open(path,'r','utf-8') as reader:
            ents = [ent.strip() for ent in reader]
        return set(ents)
    def get_id(self,word):
        if word in self.sample_words:
            return 1
        return 0
class Trigger_Word_Index():
    def __init__(self):
        self.sample_words = self.load_dic('G:/2018/code/event_extraction_system/datas/trigger/triggers')
    def load_dic(self,path):
        with codecs.open(path,'r','utf-8') as reader:
            ents = [ent.strip() for ent in reader]
        return set(ents)
    def get_id(self,word):
        if word in self.sample_words:
            return 1
        return 0
class Suffix_Index():
    def __init__(self):
        countrys = self.load_dic('G:/2018/code/event_extraction_system/datas/entity_keyword/country.txt')
        devices = self.load_dic('G:/2018/code/event_extraction_system/datas/entity_keyword/device.txt')
        orgs = self.load_dic('G:/2018/code/event_extraction_system/datas/entity_keyword/orgs.txt')
        nrs = self.load_dic('G:/2018/code/event_extraction_system/datas/entity_keyword/person.txt')
        roles = self.load_dic('G:/2018/code/event_extraction_system/datas/entity_keyword/role.txt')
        nss = self.load_dic('G:/2018/code/event_extraction_system/datas/entity_keyword/region.txt')
        tirs = self.load_dic('G:/2018/code/event_extraction_system/datas/trigger/triggers')
        self.ents = [countrys,devices,orgs,nrs,roles,nss,tirs]
    def load_dic(self,path):
        with codecs.open(path,'r','utf-8') as reader:
            ents = [ent.strip() for ent in reader]
        return set(ents)

    def get_id(self,word):
        for ent_id, ent in enumerate(self.ents):
            if word in ent:
                return ent_id + 1
        return 0
    def get_sent_suffx_indice(self,words):
        indices = []
        for word in words:
            indices.append(self.get_id(word))
        return indices
class Ent_Tag_Index():
    def __init__(self):
        self.tag2index = defaultdict(int)

        self.load_index()
        self.id2tag = {}
        for tag in self.tag2index.keys():
            self.id2tag[self.tag2index[tag]] = tag
        self.num_class = len(self.tag2index)
    def load_index(self):
        self.tag2index['O'] = 0
        self.tag2index['B-TERM'] = 1
        self.tag2index['I-TERM'] = 2

class Tri_Tag_Index():
    def __init__(self):
        self.tag2index = defaultdict(int)

        self.load_index()
        self.id2tag = {}
        for tag in self.tag2index.keys():
            self.id2tag[self.tag2index[tag]] = tag
        self.num_class = len(self.tag2index)
    def load_index(self):
        self.tag2index['O'] = 0
        self.tag2index['B-TRIG'] = 1

def trans_to_index(input_path,output_path,type='train'):
    CharIndex = Char_Index()
    wordIndex = Word_Index()
    ptagIndex = Ptag_Index()
    suffIdex = Suffix_Index()
    if type == 'train':
        sampleWordIndex = Sample_Word_Index()
    else:
        sampleWordIndex = Trigger_Word_Index()
    if type == 'trigger_as_feature':
        sampleWordIndex = Trigger_Index()
    entTagIndex = Ent_Tag_Index()
    triTagIndex = Tri_Tag_Index()
    # tarTagIndex = Tar_Tag1_Index()
    df = pd.read_csv(input_path, delimiter='#', skip_blank_lines=False, names=['word', 'postag','tag', 'len'])
    output_id = csv.writer(codecs.open(output_path,'w','utf-8'),delimiter='#')
    # output_auxi_id = csv.writer(codecs.open('data_auxi_id.txt', 'w', 'utf-8'), delimiter='#')
    output_id.writerow(['token_id','postag_id','suffix_id','sample_word','entity_id','trigger_id','len'])
    # output_auxi_id.writerow(['widx', 'tidx_sou', 'tidx_tri', 'tidx_tar', 'len'])
    cidx_seq = ''
    widx_seq = ''
    pidx_seq = ''
    #suffix_feature
    sidx_seq = ''
    tri_type_seq = ''
    sou_lidx_seq = ''
    tri_lidx_seq = ''
    # tar_lidx_seq = ''
    #
    sou_lidx_seq_tc = ''
    tri_lidx_seq_tc = ''
    # tar_lidx_seq_tc = ''
    for idx,word in enumerate(list(df['word'])):

        if str(df['tag'][idx]) == 'nan':
            output_id.writerow(
                [widx_seq[:-1],pidx_seq[:-1],sidx_seq[:-1],tri_type_seq[:-1],sou_lidx_seq[:-1],tri_lidx_seq[:-1], int(df['len'][idx - 1])])
            cidx_seq = ''
            widx_seq = ''
            pidx_seq = ''
            sidx_seq = ''
            sou_lidx_seq = ''
            tri_lidx_seq = ''
            tri_type_seq = ''
            sou_lidx_seq_tc = ''
            tri_lidx_seq_tc = ''
            # tar_lidx_seq_tc = ''
            continue
        tmp = []
        for chara in word:
            print(chara)
            tmp.append(str(CharIndex.char2idex[chara]))

        cidx_seq = cidx_seq + str('|'.join(tmp)) + ','
        widx_seq = widx_seq + str(wordIndex.word2idex[word]) + ','
        pidx_seq = pidx_seq + str(ptagIndex.ptag2id[df['postag'][idx]]) + ','
        sidx_seq = sidx_seq + str(suffIdex.get_id(word)) + ','
        tri_type_seq = tri_type_seq + str(sampleWordIndex.get_id(word)) + ','
        sou_lidx_seq = sou_lidx_seq + str(entTagIndex.tag2index[df['tag'][idx]]) + ','
        tri_lidx_seq = tri_lidx_seq + str(triTagIndex.tag2index[df['tag'][idx]]) + ','
        # tar_lidx_seq = tar_lidx_seq + str(tarTagIndex.tag2index[df['tag'][idx]]) + ','
        #
        sou_lidx_seq_tc = sou_lidx_seq_tc + str(1 if entTagIndex.tag2index[df['tag'][idx]] > 0 else 0) + ','
        tri_lidx_seq_tc = tri_lidx_seq_tc + str(1 if triTagIndex.tag2index[df['tag'][idx]] > 0 else 0) + ','
        # tar_lidx_seq_tc = tar_lidx_seq_tc + str(1 if tarTagIndex.tag2index[df['tag'][idx]] > 0 else 0) + ','


def generate_word_voc(input,output):
    words = set()
    with codecs.open(input,'r','utf-8') as reader:
        for line in reader:
            units = line.strip().split('#')
            words.add(units[0])

    with codecs.open(output,'r','utf-8') as reader:
        for line in reader:
            units = line.strip().split('#')
            words.add(units[0])
    with codecs.open('datas/w_voc.txt','w','utf-8') as writer:
        for word in words:
            writer.write(word+'\n')
def generate_postag_voc(input,output):
    ptags = set()
    with codecs.open(input,'r','utf-8') as reader:
        for line in reader:
            if '#' in line.strip():
                units = line.strip().split('#')
                ptags.add(units[1])
    with codecs.open(output,'r','utf-8') as reader:
        for line in reader:
            if '#' in line.strip():
                units = line.strip().split('#')
                ptags.add(units[1])
    with codecs.open('datas/p_voc.txt','w','utf-8') as writer:
        for postag in ptags:
            writer.write(postag+'\n')
def generate_chara_voc(input,output):
    max_len = 0
    charas = set()
    with codecs.open(input,'r','utf-8') as reader:
        for line in reader:
            units = line.strip().split('#')

            for chara in units[0]:
                charas.add(chara)

    with codecs.open(output,'r','utf-8') as reader:
        for line in reader:
            units = line.strip().split('#')
            for chara in units[0]:
                charas.add(chara)
    with codecs.open('datas/c_voc.txt','w','utf-8') as writer:
        for chara in charas:
            writer.write(chara+'\n')



if __name__ == '__main__':

    generate_chara_voc('datas/corpus/train_col','datas/corpus/test_col')
    generate_word_voc('datas/corpus/train_col','datas/corpus/test_col')
    generate_postag_voc('datas/corpus/train_col','datas/corpus/test_col')
    trans_to_index(input_path='datas/corpus/train_col',output_path='datas/corpus/traindata_id.txt',type='trigger_as_feature')
    trans_to_index(input_path='datas/corpus/test_col', output_path='datas/corpus/testdata_id.txt',type='trigger_as_feature')
    # si = Suffix_Index()
    # words = ['佳得乐','航母','和','脉动','火箭']
    # print(si.get_sent_suffx_indice(words))
    # ti = Trigger_Index()
    # print(ti.trigger_list)
