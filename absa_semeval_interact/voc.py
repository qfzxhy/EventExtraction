import pandas as pd
import numpy as np
import codecs
import csv
from collections import defaultdict
class Char_Index():
    def __init__(self):
        self.char2idex = defaultdict()
        self.char2idex['<PAD>'] = 0
        self.id2char = defaultdict()
        self.load_index()
    def load_index(self):
        with codecs.open('datas/c_voc.txt','r','utf-8') as reader:
            for chara in reader:
                self.char2idex[chara.strip()] = len(self.char2idex)
        for word in self.char2idex.keys():
            self.id2char[self.char2idex[word]] = word
class Word_Index():
    def __init__(self,vob_path):
        self.vob_path = vob_path
        self.word2idex = defaultdict()
        self.word2idex['<PAD>'] = 0
        self.id2word = defaultdict()
        self.load_index()
    def load_index(self):
        with codecs.open(self.vob_path,'r','utf-8') as reader:
            for word in reader:
                self.word2idex[word.strip().split('\t')[0]] = len(self.word2idex)
        for word in self.word2idex.keys():
            self.id2word[self.word2idex[word]] = word
class Ptag_Index():
    def __init__(self):
        self.ptag2id = defaultdict()
        self.ptag2id['<PAD>'] = 0
        self.id2ptag = defaultdict()
        self.load_index()
    def load_index(self):
        with codecs.open('datas/p_voc.txt','r','utf-8') as reader:
            for postag in reader:
                self.ptag2id[postag.strip()] = len(self.ptag2id)
        for postag in self.ptag2id.keys():
            self.id2ptag[self.ptag2id[postag]] = postag

class Tar_Tag_Index():
    def __init__(self):
        self.tag2index = defaultdict(int)
        self.load_index()
        self.id2tag = {}
        for tag in self.tag2index.keys():
            self.id2tag[self.tag2index[tag]] = tag
        self.num_class = len(self.tag2index)
    def load_index(self):
        self.tag2index['O'] = 0
        self.tag2index['B-target'] = 1
        self.tag2index['I-target'] = 2

class Opi_Tag_Index():
    def __init__(self):
        self.tag2index = defaultdict(int)

        self.load_index()
        self.id2tag = {}
        for tag in self.tag2index.keys():
            self.id2tag[self.tag2index[tag]] = tag
        self.num_class = len(self.tag2index)
    def load_index(self):
        self.tag2index['O'] = 0
        self.tag2index['B-opword'] = 1
        self.tag2index['I-opword'] = 2

def generate_col_type_datas():
    input_paths = ['laptop14_data/bio_laptop_2014_train_op','laptop14_data/bio_laptop_2014_test_op',
                   'res14_data/bio_res_2014_train_op', 'res14_data/bio_res_2014_test_op',
                   'res15_data/bio_res_2015_train_op', 'res15_data/bio_res_2015_test_op',]
    output_paths = [
        'laptop14_data/datas/train_data', 'laptop14_data/datas/test_data',
        'res14_data/datas/train_data', 'res14_data/datas/test_data',
        'res15_data/datas/train_data', 'res15_data/datas/test_data',
    ]
    for i in range(len(input_paths)):
        output = csv.writer(codecs.open(output_paths[i], 'w', 'utf-8'), delimiter='#')
        with codecs.open(input_paths[i], 'r', 'utf-8') as reader:
            for line in reader.readlines():
                line = line.strip()
                units = eval(line)
                for unit in units:
                    output.writerow([unit[0], unit[1],str(len(units))])
                output.writerow([])
def load_copule_file(couple_fname,bio_fname):
    couple_infos = []
    bio_source_datas = []
    with codecs.open(bio_fname, 'r', 'utf-8') as reader:
        for uline in reader.readlines():
            uline = eval(uline.strip())
            bio_source_datas.append(uline)
    with codecs.open(couple_fname, 'r', 'utf-8') as reader:
        for uline in reader.readlines():
            uline = uline.strip()
            cs = uline.split('\t')[1:]
            couple_infos.append(cs)
    return couple_infos,bio_source_datas
def get_couple_data(bio_data,couple_info):
    couple_tmp = []
    for c in couple_info:
        c = c.lower()
        tar_words = c[1:c.index(',')].split()
        opi_words = c[c.index(',') + 1:-1].split()
        for i in range(len(bio_data)):
            if bio_data[i][0] not in tar_words:
                continue
            for j in range(len(bio_data)):
                if bio_data[j][0] in opi_words:
                    couple_tmp.append(str(i))
                    couple_tmp.append(str(j))
    if len(couple_tmp) == 0:
        couple_tmp.append(str(-1))
    return couple_tmp
def tran_coltypedata_to_indice_all():
    bio_paths = ['res15_data/bio_res_2015_train_op', 'res15_data/bio_res_2015_test_op',
                   ]
    input_paths = [
        ['laptop14_data/datas/train_data', 'laptop14_data/datas/test_data'],
        ['res14_data/datas/train_data', 'res14_data/datas/test_data'],
        ['res15_data/datas/train_data', 'res15_data/datas/test_data'],
    ]
    vob_paths = [
        'laptop14_data/vocabs/word2id',
        'res14_data/vocabs/word2id',
        'res15_data/vocabs/word2id'
    ]
    output_paths = [
        ['laptop14_data/datas/train_indice', 'laptop14_data/datas/test_indice'],
        ['res14_data/datas/train_indice', 'res14_data/datas/test_indice'],
        ['res15_data/datas/train_indice', 'res15_data/datas/test_indice'],
    ]
    couple_path_train = 'res15_data/couple_data_train'
    couple_path_test = 'res15_data/couple_data_test'
    for i in range(len(input_paths)):
        if i == 2:
            raw_couple_infos, bio_source_datas = load_copule_file(couple_path_train,bio_paths[0])
            couple_infos = [get_couple_data(bio_source_datas[m],raw_couple_info) for m,raw_couple_info in enumerate(raw_couple_infos)]
            tran_coltypedata_to_indice(input_paths[i][0],output_paths[i][0],vob_paths[i],couple_infos)
            raw_couple_infos, bio_source_datas = load_copule_file(couple_path_test, bio_paths[1])
            couple_infos = [get_couple_data(bio_source_datas[m], raw_couple_info) for m, raw_couple_info in
                            enumerate(raw_couple_infos)]
            tran_coltypedata_to_indice(input_paths[i][1], output_paths[i][1], vob_paths[i],couple_infos)
        else:
            tran_coltypedata_to_indice(input_paths[i][0], output_paths[i][0], vob_paths[i])
            tran_coltypedata_to_indice(input_paths[i][1], output_paths[i][1], vob_paths[i])

def tran_coltypedata_to_indice(input_path,output_path,vob_path,couple_infos=None):
    wordIndex = Word_Index(vob_path)
    tarIndex = Tar_Tag_Index()
    opiIndex = Opi_Tag_Index()

    df = pd.read_csv(input_path, delimiter='#', skip_blank_lines=False, names=['word','tag', 'len'])
    output_id = csv.writer(codecs.open(output_path,'w','utf-8'),delimiter='#')
    if couple_infos == None:
        output_id.writerow(['token_id','tar_id','opi_id','len'])
    else:
        output_id.writerow(['token_id', 'tar_id', 'opi_id', 'len','couples'])
    widx_seq = ''
    tar_seq = ''
    opi_seq = ''
    sent_id = 0
    for idx,word in enumerate(list(df['word'])):

        if str(df['tag'][idx]) == 'nan':
            if couple_infos != None:
                couple_info = couple_infos[sent_id]
                output_id.writerow([widx_seq[:-1],tar_seq[:-1], opi_seq[:-1], int(df['len'][idx - 1]),','.join(couple_info)])
            else:
                output_id.writerow([widx_seq[:-1], tar_seq[:-1], opi_seq[:-1], int(df['len'][idx - 1])])

            widx_seq = ''
            tar_seq = ''
            opi_seq = ''
            sent_id += 1
            continue

        widx_seq = widx_seq + str(wordIndex.word2idex[word]) + ','
        tar_seq = tar_seq + str(tarIndex.tag2index[df['tag'][idx]]) + ','
        opi_seq = opi_seq + str(opiIndex.tag2index[df['tag'][idx]]) + ','

def generate_word_voc(input,output):
    words = set()
    with codecs.open(input,'r','utf-8') as reader:
        for line in reader:
            units = eval(line.strip())
            for unit in units:
                words.add(unit[0])
    with codecs.open(output,'r','utf-8') as reader:
        for line in reader:
            units = eval(line.strip())
            for unit in units:
                words.add(unit[0])
    with codecs.open('datas/w_voc.txt','w','utf-8') as writer:
        for word in words:
            writer.write(word+'\n')

def generate_postag_voc(input,output):
    ptags = set()
    with codecs.open(input,'r','utf-8') as reader:
        for line in reader:
            units = eval(line.strip())
            for unit in units:
                ptags.add(unit[1])
    with codecs.open(output,'r','utf-8') as reader:
        for line in reader:
            units = eval(line.strip())
            for unit in units:
                ptags.add(unit[1])
    with codecs.open('datas/p_voc.txt','w','utf-8') as writer:
        for postag in ptags:
            writer.write(postag+'\n')
def generate_chara_voc(input,output):
    max_len = 0
    charas = set()
    with codecs.open(input,'r','utf-8') as reader:
        for line in reader:
            units = eval(line.strip())
            for unit in units:
                max_len = max(max_len,len(unit[0]))
                for chara in unit[0]:
                    charas.add(chara)
    with codecs.open(output,'r','utf-8') as reader:
        for line in reader:
            units = eval(line.strip())
            for unit in units:
                max_len = max(max_len, len(unit[0]))
                for chara in unit[0]:
                    charas.add(chara)
    with codecs.open('datas/c_voc.txt','w','utf-8') as writer:
        for chara in charas:
            writer.write(chara+'\n')
    print(max_len)


if __name__ == '__main__':
    # generate_col_type_datas()
    tran_coltypedata_to_indice_all()
    # generate_chara_voc('datas/train_tmp.txt','datas/test_tmp.txt')
    # generate_word_voc('datas/train_tmp.txt','datas/test_tmp.txt')
    # generate_postag_voc('datas/train_tmp.txt','datas/test_tmp.txt')
    # trans_to_index(input_path='datas/train_tmp_col.txt',output_path='traindata_id.txt')
    # trans_to_index(input_path='datas/test_tmp_col.txt', output_path='testdata_id.txt')
    # si = Suffix_Index()
    # words = ['佳得乐','航母','和','脉动','火箭']
    # print(si.get_sent_suffx_indice(words))
