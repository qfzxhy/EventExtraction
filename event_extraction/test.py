from train import FLAGS,initial_embedding
import helper
import time
from mt_model import MT_Model
import tensorflow as tf
import codecs
import os
from voc import Trigger_Index,Word_Index,Ptag_Index,Suffix_Index,Ent_Tag_Index,Tri_Tag_Index
model_checkpoint_path = "runs/model_00001.ckpt"
sampleWordIndex = Trigger_Index()
wordIndex = Word_Index()
ptagIndex = Ptag_Index()
suffIdex = Suffix_Index()
entTagIndex = Ent_Tag_Index()
triTagIndex = Tri_Tag_Index()
FLAGS.task1_num_class = 3
FLAGS.task2_num_class = 2
FLAGS.num_word = len(wordIndex.word2idex)
FLAGS.num_postag = len(ptagIndex.ptag2id)
FLAGS.alphabet_size = 10000
FLAGS.pretrain_emb = initial_embedding(wordIndex.word2idex)
model = MT_Model(FLAGS)
import re
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
def predict(wordseq, postagseq):
    words = wordseq.split()
    postags = postagseq.split()


    x = []
    x.append([wordIndex.word2idex[word] for word in words])
    x.append([ptagIndex.ptag2id[tag] for tag in postags])
    x.append([suffIdex.get_id(word) for word in words])
    x.append([sampleWordIndex.get_id(word) for word in words])
    print(x)
    with tf.Session() as sess:


        saver = tf.train.Saver()
        saver.restore(sess, model_checkpoint_path)
        source_res, trigger_res = model.inference_single(sess, x)

        y_p = ''.join([entTagIndex.id2tag[source_res[j]] for j in range(len(source_res))])
        arg_entitys = extractEntity_type(words,y_p,'TERM')
        print(arg_entitys)
        y_p = ''.join([triTagIndex.id2tag[trigger_res[j]] for j in range(len(trigger_res))])
        tri_entitys = extractEntity_type(words, y_p, 'TRI')
        print(tri_entitys)

if __name__ == '__main__':
    import datetime
    begin1 = datetime.datetime.now()
    predict("美国 袭击 朝鲜","ns v ns")
    begin2 = datetime.datetime.now()
    print(begin2 - begin1)
    predict("美国 袭击 朝鲜","ns v ns")
    begin3 = datetime.datetime.now()
    print(begin3 - begin2)
    predict("美国 袭击 朝鲜", "ns v ns")
    begin4 = datetime.datetime.now()
    print(begin4 - begin3)

