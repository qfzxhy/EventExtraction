# -*- coding: utf-8 -*-

import codecs
import numpy as np
import pandas as pd
import csv
import re
import os
from voc import Word_Index

def trans_id_to_label(X,entity_res,trigger_res,lens):
    id2word = Word_Index().id2word
    output = codecs.open('runs/output','w','utf-8')
    id2label_ent = {0: 'O', 1: 'B-TERM', 2: 'I-TERM'}
    id2label_tri = {0: 'O', 1: 'B-TRIG', 2: 'I-TRIG'}
    for i in range(len(entity_res)):
        seq_len = lens[i]
        x = [id2word[X[i][j]] for j in range(seq_len)]
        y_ent = ''.join([id2label_ent[entity_res[i][j]] for j in range(seq_len)])
        y_tri = ''.join([id2label_tri[trigger_res[i][j]] for j in range(seq_len)])
        ents = extractEntity_type(x, y_ent, 'TERM')
        tris = extractEntity_type(x, y_tri, 'TRIG')
        output.write(''.join(x)+'\t'+'#'.join(ents)+'\t'+'#'.join(tris)+'\n')
    pass

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
    pass


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




def evaluate_tri(X, y_gold, y_pred, id2word,seq_lens):
    id2label = {0:'O',1:'B-TRI'}
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
        entity_gold = extractEntity_type(x, y,'TRI')
        entity_pred = extractEntity_type(x, y_p,'TRI')
        num_right += len(set(entity_gold) & set(entity_pred))
        num_gold += len(set(entity_gold))
        num_pred += len(set(entity_pred))
    if num_pred != 0:
        precision = 1.0 * num_right / num_pred
    if num_gold != 0:
        recall = 1.0 * num_right / num_gold
    if precision > 0 and recall > 0:
        f1 = 2 * (precision * recall) / (recall + precision)
    return precision, recall, f1

    pass
def evaluate_ent(X, y_gold, y_pred, id2word, id2label,seq_lens):
    # y_gold = list : [batch_size,seq_len]
    # y_pred = [batch_size,num_steps]

    types = ['TERM']
    num_class = len(types) + 1
    precision = [-1.0] * num_class
    recall = [-1.0] * num_class
    f1 = [-1.0] * num_class
    num_pred = [0] * num_class
    num_gold = [0] * num_class
    num_right = [0] * num_class
    for i in range(len(y_gold)):
        seq_len = seq_lens[i]
        x = [id2word[X[i][j]] for j in range(seq_len)]
        y = ''.join([id2label[y_gold[i][j]] for j in range(seq_len)])
        y_p = ''.join([id2label[y_pred[i][j]] for j in range(seq_len)])
        for typeid,type in enumerate(types):
            entity_gold = extractEntity_type(x, y,type)
            entity_pred = extractEntity_type(x, y_p,type)
            num_right[typeid] += len(set(entity_gold) & set(entity_pred))
            num_gold[typeid] += len(set(entity_gold))
            num_pred[typeid] += len(set(entity_pred))
            num_right[num_class - 1] += len(set(entity_gold) & set(entity_pred))
            num_gold[num_class - 1] += len(set(entity_gold))
            num_pred[num_class - 1] += len(set(entity_pred))
    for typeid in range(num_class):
        if num_pred[typeid] != 0:
            precision[typeid] = 1.0 * num_right[typeid] / num_pred[typeid]
        if num_gold[typeid] != 0:
            recall[typeid] = 1.0 * num_right[typeid] / num_gold[typeid]
        if precision[typeid] > 0 and recall[typeid] > 0:
            f1[typeid] = 2 * (precision[typeid] * recall[typeid]) / (recall[typeid] + precision[typeid])
    print(f1)

    return precision[num_class-1], recall[num_class-1], f1[num_class-1]
def store_result(X, y_gold, y_pred,y_gold1, y_pred1, id2word, id2label,id2label1,seq_lens):
    writer = codecs.open('result_mtl','w','utf-8')
    types = ['TERM','B-TRIG']
    for i in range(len(y_gold)):
        seq_len = seq_lens[i]
        x = [id2word[X[i][j]] for j in range(seq_len)]
        y = ''.join([id2label[y_gold[i][j]] for j in range(seq_len)])
        y_p = ''.join([id2label[y_pred[i][j]] for j in range(seq_len)])
        y1 = ''.join([id2label1[y_gold1[i][j]] for j in range(seq_len)])
        tri_labels_pred = [id2label1[y_pred1[i][j]] for j in range(seq_len)]

        entity_gold = extractEntity_type(x, y,types[0])
        entity_pred = extractEntity_type(x, y_p,types[0])
        tri_pred = [x[i] for i,label in enumerate(tri_labels_pred) if label==types[1]]
        writer.write(' '.join(entity_pred) + '\t'+ ' '.join(tri_pred))
        writer.write('\n')
    writer.close()
def store_stl_result(X, y_gold, y_pred, id2word, id2label,seq_lens,type_id):
    writer = codecs.open('result_stl','w','utf-8')
    types = ['TERM','B-TRIG']
    for i in range(len(y_gold)):
        seq_len = seq_lens[i]
        x = [id2word[X[i][j]] for j in range(seq_len)]


        if type_id == 0:
            y = ''.join([id2label[y_gold[i][j]] for j in range(seq_len)])
            y_p = ''.join([id2label[y_pred[i][j]] for j in range(seq_len)])
            entity_gold = extractEntity_type(x, y,types[0])
            entity_pred = extractEntity_type(x, y_p,types[0])
            writer.write(' '.join(entity_pred))
            writer.write('\n')
        else:
            y1 = ''.join([id2label[y_gold[i][j]] for j in range(seq_len)])
            tri_labels_pred = [id2label[y_pred[i][j]] for j in range(seq_len)]
            tri_pred = [x[i] for i,label in enumerate(tri_labels_pred) if label==types[1]]
            writer.write(' '.join(tri_pred))
            writer.write('\n')
    writer.close()
if __name__ == '__main__':
    # 解放军  # B-SOU#13
    # 某  # O#13
    # 旅  # B-SOU#13
    # 8  # O#13
    # 名  # O#13
    # 官兵  # B-SOU#13
    # 体能  # O#13
    # 不  # O#13
    # 过关  # O#13
    # 取消  # B-TRI#13
    # 考学  # O#13
    # 提升  # I-TRI#13
    # 资格  # E-TRI#13
    words = ['解放军','某','旅','8','名','官兵','体能','不','过关','取消','考学','提升','资格']
    tags = ['B-SOU','O','B-SOU','O','O','O','O','O','O','B-TRI','I-TRI','I-TRI','B-TRI']
    print(extract_trigger(words,tags))