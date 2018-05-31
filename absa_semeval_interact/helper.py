# -*- coding: utf-8 -*-

import codecs
import numpy as np
import pandas as pd
import csv
import re
import os

class dataHelper():
    def __init__(self,task_id):
        task_data_dirs = ['laptop14_data', 'res14_data', 'res15_data', 'trial_data']
        task_train_file_names = ['laptop_2014', 'res_2014', 'res_2015', 'trial']
        task_data_dir = task_data_dirs[task_id - 1]
        task_train_file_name = task_train_file_names[task_id - 1]
        self.word2id_path = os.path.join(task_data_dir,'vocabs','word2id')
        self.label2id_path = os.path.join(task_data_dir,'vocabs','label2id')
        self.char2id_path = os.path.join(task_data_dir,'vocabs','char2id')
        # self.train_infrequent_words_path = os.path.join(task_data_dir,'vocabs','infrequent_tokens_train')
        # self.test_infrequent_words_path = os.path.join(task_data_dir, 'vocabs', 'infrequent_tokens_test')
        self.train_path = os.path.join(task_data_dir,'bio_'+task_train_file_name+'_train_op')
        self.test_path = os.path.join(task_data_dir,'bio_'+task_train_file_name+'_test_op')
        self.train_postag_path = os.path.join(task_data_dir,'train_postag')

    def build_map_test(self, fname, word2id, label2id, token_items):
        if not os.path.isfile(fname):
            print('[!]%s not fount' % fname)
        if len(word2id) == 0:
            word2id['<PAD>'] = 0
            word2id['<UNK>'] = 1
            word2id['<DIGIT>'] = 2
        # if len(label2id) == 0:
        #     label2id['<PAD>'] = 0
        with codecs.open(fname, 'r', 'utf-8') as reader:
            for uline in reader.readlines():
                # print(uline)
                uline = eval(uline.strip())
                for unit in uline:
                    word = unit[0]
                    label = unit[1]
                    if word not in token_items:
                        token_items[word] = 1
                    else:
                        token_items[word] += 1
                    if word not in word2id:
                        word2id[word] = len(word2id)
                    if label not in label2id:
                        label2id[label] = len(label2id)
    def build_map_train(self, fname, word2id, label2id, token_items):
        if not os.path.isfile(fname):
            print('[!]%s not fount' % fname)
        if len(word2id) == 0:
            word2id['<PAD>'] = 0
            word2id['<UNK>'] = 1
            word2id['<DIGIT>'] = 2
        # if len(label2id) == 0:
        #     label2id['<PAD>'] = 0
        with codecs.open(fname, 'r', 'utf-8') as reader:
            for uline in reader.readlines():
                # print(uline)
                uline = eval(uline.strip())
                for unit in uline:
                    word = unit[0]
                    label = unit[1]
                    if word not in token_items:
                        token_items[word] = 1
                    else:
                        token_items[word] += 1
                    if word not in word2id:
                        word2id[word] = len(word2id)
                    if label not in label2id:
                        label2id[label] = len(label2id)

    def buildMap(self,train_fname,test_fname):
        remap_to_unk_count_threshold = 1
        infrequent_wn_indices = []
        token_items = {}
        word2id, id2word, label2id, id2label = {}, {}, {}, {}
        # only  train words
        self.build_map_train(train_fname, word2id, label2id, token_items,)

        for item in token_items.keys():
            if token_items[item] <= remap_to_unk_count_threshold:
                infrequent_wn_indices.append(word2id[item])
        # saveList(infrequent_wn_indices, self.train_infrequent_words_path)
        # for train infrequent tokens and test infrequent tokens
        token_items = {}
        infrequent_wn_indices = []
        self.build_map_test(test_fname, word2id, label2id, token_items)
        for item in token_items.keys():
            if token_items[item] <= remap_to_unk_count_threshold:
                infrequent_wn_indices.append(word2id[item])
        # saveList(infrequent_wn_indices, self.test_infrequent_words_path)
        for word in word2id:
            id2word[word2id[word]] = word
        for label in label2id:
            id2label[label2id[label]] = label

        saveMap(id2word, self.word2id_path)
        saveMap(id2label,self.label2id_path)


    def build_alphabet_map(self, fname, char2id):
        if not os.path.isfile(fname):
            print('[!]%s not fount' % fname)
        if len(char2id) == 0:
            char2id['<PAD>'] = 0
            char2id['<UNK>'] = 1
        with codecs.open(fname, 'r', 'utf-8') as reader:
            for uline in reader.readlines():
                uline = eval(uline.strip())
                for unit in uline:
                    word = unit[0]
                    for chara in word:
                        if chara not in char2id:
                            char2id[chara] = len(char2id)

    def bulid_alphabet_map(self,train_fname,test_fname):
        char2id, id2char = {}, {}
        self.build_alphabet_map(train_fname, char2id)
        self.build_alphabet_map(test_fname, char2id)
        for chara in char2id:
            id2char[char2id[chara]] = chara
        with codecs.open(self.char2id_path, 'w', 'utf-8') as writer:
            for id in id2char.keys():
                writer.write(id2char[id] + "\t" + str(id) + '\n')
        return char2id, id2char



    def get_POS_Data(self,fname):
        postags = ['o','n', 'v', 'adv', 'adj']
        pos2id, id2pos = {}, {}
        for postag in postags:
            pos2id[postag] = len(pos2id)
            id2pos[pos2id[postag]] = postag
        input_postag_indices = []
        with codecs.open(fname, 'r', 'utf-8') as reader:
            for uline in reader.readlines():
                postag_tmp = []
                uline = uline.strip()
                for unit in uline.split():
                    postag_tmp.append(pos2id[unit])
                input_postag_indices.append(postag_tmp)
        return input_postag_indices
        pass

    def get_label_indice(self,label):
        if label == 'O':
            return 0, 0
        if label == 'B-target':
            return 1, 1
        if label == 'I-target':
            return 2, 1
        if label == 'B-opword':
            return 1, 2
        if label == 'I-opword':
            return 2, 2

    def get_BIO_Data(self,fname,couple_fname, token_max_len):

        word2id, id2word = loadMap(self.word2id_path)
        char2id, id2char = loadMap(self.char2id_path)
        bio_source_datas = []
        max_token_len = 0
        with codecs.open(fname, 'r', 'utf-8') as reader:
            for uline in reader.readlines():
                uline = eval(uline.strip())
                bio_source_datas.append(uline)
        # couple_infos = load_copule_file(couple_fname)

        input_token_indices = []
        input_token_character_indices = []
        # couples = []
        y_targets = []
        y_opinions = []
        y_targets_tc = []
        y_opinions_tc = []
        y_targets_rel = []
        y_opinions_rel = []
        for bio_data_id,bio_data in enumerate(bio_source_datas):
            # couples.append(get_couple_data(bio_data,couple_infos[bio_data_id]))
            token_tmp = []
            token_character_tmp = []
            y_target_tmp = []
            y_opinion_tmp = []
            y_targets_tmp_tc = []
            y_opinions_tmp_tc = []
            y_targets_rel_tmp = []
            y_opinions_rel_tmp = []
            for i in range(len(bio_data)):
                word = bio_data[i][0]
                if word in word2id:
                    word_indice = word2id[word]
                else:
                    word_indice = word2id['<UNK>']
                if word.isdigit():
                    word_indice = word2id['<DIGIT>']

                token_tmp.append(word_indice)
                label_indice, label_type = self.get_label_indice(bio_data[i][1])
                if label_type == 0:  # other
                    y_opinion_tmp.append(label_indice)
                    y_target_tmp.append(label_indice)
                    y_opinions_tmp_tc.append(label_indice)
                    y_targets_tmp_tc.append(label_indice)
                elif label_type == 1:  # target
                    y_opinion_tmp.append(0)
                    y_target_tmp.append(label_indice)
                    y_opinions_tmp_tc.append(0)
                    y_targets_tmp_tc.append(1)
                else:  # opword
                    y_opinion_tmp.append(label_indice)
                    y_target_tmp.append(0)
                    y_opinions_tmp_tc.append(1)
                    y_targets_tmp_tc.append(0)

                rel_ta_tmp,rel_op_tmp = [],[]
                for j in range(1,4):
                    if i - j < 0:
                        rel_ta_tmp.append(0)
                        rel_op_tmp.append(0)
                    else:
                        pre_label_indice,pre_label_type = self.get_label_indice(bio_data[i-j][1])
                        if label_type == 1 and pre_label_type == 2:
                            rel_ta_tmp.append(1)
                            rel_op_tmp.append(0)
                        elif label_type == 2 and pre_label_type == 1:
                            rel_op_tmp.append(1)
                            rel_ta_tmp.append(0)
                        else:
                            rel_op_tmp.append(0)
                            rel_ta_tmp.append(0)

                y_targets_rel_tmp.append(rel_ta_tmp)
                y_opinions_rel_tmp.append(rel_op_tmp)
                token = bio_data[i][0]
                if len(token) > max_token_len:
                    max_token_len = len(token)
                charater_indice = [int(char2id['<PAD>'])] * token_max_len
                for index in range(min(len(token), token_max_len)):
                    if token[index] in char2id:
                        charater_indice[index] = char2id[token[index]]
                    else:
                        charater_indice[index] = char2id['<UNK>']
                token_character_tmp.append(charater_indice)
            input_token_character_indices.append(token_character_tmp)
            input_token_indices.append(token_tmp)
            y_targets.append(y_target_tmp)
            y_opinions.append(y_opinion_tmp)
            y_targets_tc.append(y_targets_tmp_tc)
            y_opinions_tc.append(y_opinions_tmp_tc)
            y_targets_rel.append(y_targets_rel_tmp)
            y_opinions_rel.append(y_opinions_rel_tmp)

        return np.array(input_token_indices), np.array(input_token_character_indices), \
               np.array(y_targets), np.array(y_opinions),np.array(y_targets_tc), np.array(y_opinions_tc),np.array(y_targets_rel),np.array(y_opinions_rel)

    def generate_outer_file(self):
        self.buildMap(self.train_path,self.test_path)
        self.bulid_alphabet_map(self.train_path,self.test_path)

def get_source_data(fname):
    bio_source_datas = []
    # max_sent_len = 0
    with codecs.open(fname, 'r', 'utf-8') as reader:
        for uline in reader.readlines():
            uline = eval(uline.strip())
            bio_source_datas.append(uline)
    return bio_source_datas
def load_copule_file(couple_fname):
    couple_infos = []
    with codecs.open(couple_fname, 'r', 'utf-8') as reader:
        for uline in reader.readlines():
            uline = uline.strip()
            cs = uline.split('\t')[1:]
            couple_infos.append(cs)
    return couple_infos
def get_couple_data(bio_data,couple_info):
    couple_tmp = np.zeros(shape=(len(bio_data), len(bio_data)), dtype=np.int32)
    for c in couple_info:
        c = c.lower()
        tar_words = c[1:c.index(',')].split()

        opi_words = c[c.index(',') + 1:-1].split()
        for i in range(len(bio_data)):
            if bio_data[i][0] not in tar_words:
                continue
            for j in range(len(bio_data)):
                if bio_data[j][0] in opi_words:
                    couple_tmp[i, j] = 1
                    couple_tmp[j, i] = 1
    return couple_tmp

def get_token_data(fname):
    X = []
    with codecs.open(fname, 'r', 'utf-8') as reader:
        for uline in reader.readlines():
            uline = uline.strip()
            units = eval(uline.strip())
            X.append([unit[0] for unit in units])
    return X
def loadMap(map_path):
    _2id = {}
    id2_ = {}
    with codecs.open(map_path, 'r', encoding='utf-8') as reader:
        for line in reader:
            # print(line)
            f = line.split('\t')[0]
            s = line.split('\t')[1]
            _2id[f] = int(s)
            id2_[int(s)] = f
    return _2id, id2_
def saveMap(id2item, fname):
    with codecs.open(fname, 'w', 'utf-8') as writer:
        for id in id2item.keys():
            writer.write(id2item[id] + "\t" + str(id) + '\n')


def saveList(list_items, fname):
    with codecs.open(fname, 'w', 'utf-8') as writer:
        for item in list_items:
            writer.write(str(item) + '\n')

def loadList(list_path):
    items = []
    with codecs.open(list_path, 'r', encoding='utf-8') as reader:
        for line in reader:
            items.append(int(line.strip()))
    return items
def padding(input_list,max_len,pad_id):
    # print(input_list)
    assert (isinstance(input_list,list))
    for i in range(len(input_list),max_len):
        input_list.append(pad_id)
    return input_list
    pass
def padding_char(input_list_list,max_len,pad_id):
    #这个是专门为character padding写的
    token_max_len = len(input_list_list[0])
    for i in range(len(input_list_list),max_len):
        input_list_list.append([pad_id] * token_max_len)
    return input_list_list
def padding_operate(input_data,max_seq_len,padding_indice):
    data = [padding(label_indices, max_seq_len, padding_indice) for label_indices in input_data]
    return np.array(data)
def padding_char_operate(input_data,max_seq_len,padding_indice):
    data = [padding_char(label_indices, max_seq_len, padding_indice) for label_indices in input_data]
    return np.array(data)
def get_max_seq_len(input_datas):
    max_seq_len = -1
    for i in range(len(input_datas)):
        if len(input_datas[i]) > max_seq_len:
            max_seq_len = len(input_datas[i])
    return max_seq_len
# def batch_padding(input_token_indices_train_batch,
#                   input_token_character_indices_train_batch,
#                   input_postag_indices_train_batch,
#                   y_targets,y_opinions):
#     # print(input_postag_indices_train_batch)
#     num_postag = 5#这个写的不好，需要该
#     word_pad_indice = 0
#     char_pad_indice = 0
#     label_pad_indice = 0
#     # word2id, id2word = loadMap(word2id_path)
#     # char2id, id2char = loadMap(char2id_path)
#     # label2id, id2label = loadMap(label2id_path)
#
#     #padding
#     # print('a')
#     token_indices_padding = [padding(token_indice, max_seq_len,word_pad_indice) for token_indice in
#                              input_token_indices_train_batch]
#     # print(token_indices_padding)
#     # print('b')
#     postag_indices_padding = [padding(postag_indice, max_seq_len, num_postag) for postag_indice in
#                               input_postag_indices_train_batch]
#     # print('c')
#     targets_indices_padding = [padding(label_indice, max_seq_len, label_pad_indice) for label_indice in
#                              y_targets]
#
#     opinions_indices_padding = [padding(label_indice, max_seq_len, label_pad_indice) for label_indice in
#                                y_opinions]
#
#     # print('d')
#     char_indices_padding = [padding_char(chara_indice, max_seq_len, char_pad_indice) for chara_indice in
#                             input_token_character_indices_train_batch]
#     return np.array(token_indices_padding), \
#            np.array(char_indices_padding), \
#            np.array(postag_indices_padding), \
#            np.array(targets_indices_padding),\
#            np.array(opinions_indices_padding)

def batch_iter(data_size,batch_size):
    num_iter = int((data_size - 1) / batch_size) + 1
    for iter in range(num_iter):
        start_index = iter * batch_size
        end_index = (iter+1) * batch_size
        if end_index > data_size:
            end_index = data_size
        yield start_index,end_index








def extractEntity(sentence,labels):
    entitys = []
    pattern = re.compile(r'(B-[^BIO]+)(I-[^BIO]+)*')
    m = pattern.search(labels)
    while m:
        entity_label = m.group()
        label_start_index = labels.find(entity_label)
        label_end_index = label_start_index + len(entity_label)
        word_start_index = labels[:label_start_index].count('-') + labels[:label_start_index].count('O')
        word_end_index = word_start_index + entity_label.count('-')
        entitys.append(''.join(sentence[word_start_index:word_end_index]))
        labels = list(labels)
        labels[:label_end_index] = ['O' for _ in range(word_end_index)]
        labels = ''.join(labels)
        m = pattern.search(labels)
    return entitys


    pass
if __name__ =='__main__':
    # generate_outer_file()
    DataHelper = dataHelper(3)
    input_token_indices, input_token_character_indices, \
    y_targets, y_opinions, y_targets_tc, y_opinions_tc,y1,y2,cs = DataHelper.get_BIO_Data(DataHelper.train_path, 'res15_data/couple_data_train',token_max_len=24)
    print(cs[38])
    # print(cs[37])
    # print(cs[138])
    #getTrainData(train_path='data/traindata',eval_path='1',seq_max_len=20)
    #getTestData('data.word2id',20,True)
    # getTestData('data/testdata',20,False)
    #print getEmbedding('data/vectors')
    # buildMap('bio_laptop_2014_train', 'bio_laptop_2014_test')
    # bulid_alphabet_map('bio_laptop_2014_train','bio_laptop_2014_test')
    # input_token_indices,input_token_character_indices,y_targets,y_opinions = get_BIO_Data(train_path,10)
    # print(y_targets)
    # print(y_opinions)

    # print(X[3])
    # print(y[3])
    # sentence = ['i','a','good','boy','ha']
    # labels = 'OB-PESONB-TERMI-TERMI-TERM'
    # pattern = re.compile(r'(B-[^BI]+)(I-[^BI]+)*')
    # m = pattern.search(labels)
    # print m.group(0)
    # print (extractEntity(sentence,labels))
        # m = p.search('a1b2c3')
# def left_shift_input_indice(input_token_indices,input_token_character_indices):
#     input_token_indices_left_shifit = []
#
#     pass
def save_test_data(X_test_str,X_test_label_str,output_path):
    with codecs.open(output_path,'w','utf-8') as outfile:
        for i in range(len(X_test_str)):
            for j in range(len(X_test_str[i])):
                outfile.write(X_test_str[i][j] + "\t" + X_test_label_str[i][j] + '\n')
            outfile.write('\n')

def extractEntity_type(sentence,labels,type):
    entitys = []
    pattern = re.compile(r'(B-'+type+')(I-'+type + ')*')
    m = pattern.search(labels)
    while m:
        entity_label = m.group()
        label_start_index = labels.find(entity_label)
        label_end_index = label_start_index + len(entity_label)
        word_start_index = labels[:label_start_index].count('-') + labels[:label_start_index].count('O')
        word_end_index = word_start_index + entity_label.count('-')
        entitys.append(''.join(sentence[word_start_index:word_end_index]))
        labels = list(labels)
        labels[:label_end_index] = ['O' for _ in range(word_end_index)]
        labels = ''.join(labels)
        m = pattern.search(labels)
    return entitys
def evaluate(X, y_gold, y_pred, id2word, seq_lens,label_type = 'target'):
    # y_gold = list : [batch_size,seq_len]
    # y_pred = [batch_size,num_steps]
    id2label = {0: 'O', 1: 'B-' + label_type, 2: 'I-' + label_type}
    precision = -1.0
    recall = -1.0
    f1 = -1.0
    num_pred = 0
    num_gold = 0
    num_right = 0
    for i in range(len(y_gold)):
        seq_len = seq_lens[i]
        x = [id2word[X[i][j]] for j in range(seq_len)]
        y = ''.join([id2label[y_gold[i][j]] for j in range(seq_len)])
        y_p = ''.join([id2label[y_pred[i][j]] for j in range(seq_len)])
        entity_gold = extractEntity_type(x, y,label_type)
        entity_pred = extractEntity_type(x, y_p,label_type)
        num_right += len(set(entity_gold) & set(entity_pred))
        num_gold += len(set(entity_gold))
        num_pred += len(set(entity_pred))
    if num_pred != 0:
        precision = 1.0 * num_right / num_pred
    if num_gold != 0:
        recall = 1.0 * num_right / num_gold
    if precision > 0 and recall > 0:
        f1 = 2 * (precision * recall) / (recall + precision)

    true_list = []
    pred_list = []
    for i in range(len(y_gold)):
        seq_len = seq_lens[i]
        true_list.append(list(y_gold[i][:seq_len]))
        pred_list.append(list(y_pred[i][:seq_len]))
    precision1, recall1, f11 = score_aspect(true_list,pred_list)
    precision2, recall2, f12 = get_acc(true_list, pred_list)
    return f1, f11, f12

#
def convert_indice_to_bio(fname,y,label_type = 'target'):
    id2label = {0:'O',1:'B-'+label_type,2:'I-'+label_type}
    source_datas = get_source_data(fname)
    assert (len(source_datas) == len(y))
    bio_y = []
    for i in range(len(source_datas)):
        if len(y[i]) >= len(source_datas[i]):
            labels_predict = list(map(lambda x : id2label[x],list(y[i])[:len(source_datas[i])]))
        else:
            labels_predict = list(map(lambda x: id2label[x], list(y[i])[:len(source_datas[i])]))
            labels_predict += ['O']*(len(source_datas[i]) - len(y[i]))
        bio_y.append(labels_predict)
    return bio_y


def score_aspect(true_list, predict_list):

    correct = 0
    predicted = 0
    relevant = 0

    i = 0
    j = 0
    pairs = []
    while i < len(true_list):
        true_seq = true_list[i]
        predict = predict_list[i]

        for num in range(len(true_seq)):
            if true_seq[num] == 1:
                if num < len(true_seq) - 1:
                    # if true_seq[num + 1] == '0' or true_seq[num + 1] == '1':
                    if true_seq[num + 1] != 2:
                        # if predict[num] == '1':
                        if predict[num] == 1 and predict[num + 1] != 2:
                            # if predict[num] == '1' and predict[num + 1] != '1':
                            correct += 1
                            # predicted += 1
                            relevant += 1
                        else:
                            relevant += 1

                    else:
                        if predict[num] == 1:
                            for j in range(num + 1, len(true_seq)):
                                if true_seq[j] == 2:
                                    if predict[j] == 2 and j < len(predict) - 1:
                                        # if predict[j] == '1' and j < len(predict) - 1:
                                        continue
                                    elif predict[j] == 2 and j == len(predict) - 1:
                                        # elif predict[j] == '1' and j == len(predict) - 1:
                                        correct += 1
                                        relevant += 1

                                    else:
                                        relevant += 1
                                        break

                                else:
                                    if predict[j] != 2:
                                        # if predict[j] != '1':
                                        correct += 1
                                        # predicted += 1
                                        relevant += 1
                                        break


                        else:
                            relevant += 1

                else:
                    if predict[num] == 1:
                        correct += 1
                        # predicted += 1
                        relevant += 1
                    else:
                        relevant += 1

        for num in range(len(predict)):
            if predict[num] == 1:
                predicted += 1

        i += 1

    precision = 1.0 * correct / (predicted + 1e-6)
    recall = 1.0 * correct / (relevant + 1e-6)
    f1 = 2 * precision * recall / (precision + recall + 1e-6)

    return precision, recall, f1
def get_acc(golden,predicted):
    #golden:, O:0  B:1, I:2
    for i in range(len(golden)):
        for j in range(len(golden[i])):
            if golden[i][j] == 0: #'O'
                golden[i][j] = 2
            else:
                golden[i][j] -= 1
    for i in range(len(predicted)):
        for j in range(len(predicted[i])):
            if predicted[i][j] == 0:  # 'O'
                predicted[i][j] = 2
            else:
                predicted[i][j] -= 1
    # B:0, I:1, O:2

    assert len(predicted) == len(golden)
    sum_all = 0
    sum_correct = 0
    golden_01_count = 0
    predict_01_count = 0
    correct_01_count = 0
    # print(predicted)
    # print(golden)
    for i in range(len(golden)):
        length = len(golden[i])
        # print(length)
        # print(predicted[i])
        # print(golden[i])
        golden_01 = 0
        correct_01 = 0
        predict_01 = 0
        predict_items = []
        golden_items = []
        golden_seq = []
        predict_seq = []
        for j in range(length):
            if golden[i][j] == 0:
                if len(golden_seq) > 0:  # 00
                    golden_items.append(golden_seq)
                    golden_seq = []
                golden_seq.append(j)
            elif golden[i][j] == 1:
                if len(golden_seq) > 0:
                    golden_seq.append(j)
            elif golden[i][j] == 2:
                if len(golden_seq) > 0:
                    golden_items.append(golden_seq)
                    golden_seq = []
            if predicted[i][j] == 0:
                if len(predict_seq) > 0:  # 00
                    predict_items.append(predict_seq)
                    predict_seq = []
                predict_seq.append(j)
            elif predicted[i][j] == 1:
                if len(predict_seq) > 0:
                    predict_seq.append(j)
            elif predicted[i][j] == 2:
                if len(predict_seq) > 0:
                    predict_items.append(predict_seq)
                    predict_seq = []
        if len(golden_seq) > 0:
            golden_items.append(golden_seq)
        if len(predict_seq) > 0:
            predict_items.append(predict_seq)
        golden_01 = len(golden_items)
        predict_01 = len(predict_items)
        correct_01 = sum([item in golden_items for item in predict_items])
        # print(correct_01)
        # print([item in golden_items for item in predict_items])
        # print(golden_items)
        # print(predict_items)

        golden_01_count += golden_01
        predict_01_count += predict_01
        correct_01_count += correct_01
    precision = correct_01_count/predict_01_count if predict_01_count > 0 else 0
    recall = correct_01_count/golden_01_count if golden_01_count > 0 else 0
    f1 = 2*precision*recall/(precision +recall) if (precision + recall) > 0 else 0
    return precision, recall, f1
