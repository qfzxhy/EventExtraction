import re
import gensim
import numpy as np
import codecs
import time
from mt_model import MT_Model
import tensorflow as tf
from voc import Trigger_Index,Word_Index,Ptag_Index,Suffix_Index,Ent_Tag_Index,Tri_Tag_Index
model_checkpoint_path = "G:/2018/code/event_extraction_system/runs/model_00005.ckpt"
sampleWordIndex = Trigger_Index()
wordIndex = Word_Index()
ptagIndex = Ptag_Index()
suffIdex = Suffix_Index()
entTagIndex = Ent_Tag_Index()
triTagIndex = Tri_Tag_Index()
flags = tf.app.flags
types = ['argument','trigger']
flags.DEFINE_integer('token_edim',64,'the dim of word embedding')
flags.DEFINE_integer('postag_emb_dim',20,'the dim of postag embedding')
flags.DEFINE_integer('num_suffix',8,'the dim of postag embedding')
flags.DEFINE_integer('suffix_emb_dim',20,'the dim of postag embedding')
flags.DEFINE_integer('character_embedding_dimension',25,'the dim of character embedding')
flags.DEFINE_integer('character_lstm_hidden_state_dimension',25,'the dim of character_lstm_hidden')
flags.DEFINE_integer('token_max_len',9,'')
flags.DEFINE_integer('num_hidden',64,'the dim of word lstm_hidden')
flags.DEFINE_integer('batch_size',1,'the dim of word embedding')
flags.DEFINE_integer('num_epochs',100,'the dim of word embedding')
flags.DEFINE_float('dev_rate',0.3,'the dim of word embedding')
flags.DEFINE_integer('num_corpus',3,'num_corpus')
flags.DEFINE_integer('patients',10,'early stop num')
flags.DEFINE_float('init_lr',0.001,'the dim of word embedding')
flags.DEFINE_float('gradient_clipping_value',5.0,'the dim of word embedding')
flags.DEFINE_float('init_std',0.05,'the dim of word embedding')
flags.DEFINE_string('optimizer','adam','early stop num')
flags.DEFINE_string('pretrain_file','G:/2018/news_12g_baidubaike_20g_novel_90g_embedding_64.bin','')
flags.DEFINE_string('save_dir','runs','ckpt location')
flags.DEFINE_string('best_model_dir','best_models','ckpt location')
flags.DEFINE_boolean('use_pretrain_embedding',True,'')
flags.DEFINE_boolean('use_postag',True,'')
flags.DEFINE_boolean('use_suffix',True,'')
flags.DEFINE_boolean('use_character_lstm',False,'')
flags.DEFINE_boolean('multi_task',True,'')
flags.DEFINE_boolean('use_couple',False,'')
flags.DEFINE_integer('object_id',1,'')
FLAGS = flags.FLAGS
FLAGS.task1_num_class = 3
FLAGS.task2_num_class = 2
FLAGS.num_word = len(wordIndex.word2idex)
FLAGS.num_postag = len(ptagIndex.ptag2id)
FLAGS.alphabet_size = 10000
FLAGS.num_trigger_type = 21
FLAGS.trigger_type_emb_dim = 20
wordseq_list = []
posseq_list = []
with codecs.open('G:\\MasterTwoU\\28proj\\WordCloud\\temp.txt','r','utf-8') as reader:
    for line in reader.readlines():
        line = line.strip()
        wordseq_list.append(line.split('#')[0])
        posseq_list.append(line.split('#')[1])

def initial_embedding(word2id):
    emb = np.random.normal(0,FLAGS.init_std,[len(word2id),FLAGS.token_edim])
    model = gensim.models.KeyedVectors.load_word2vec_format(FLAGS.pretrain_file,binary=True)
    for word in word2id.keys():
        if word in model:
            emb[word2id[word]] = model[word]
    return emb
FLAGS.pretrain_emb = None
model = MT_Model(FLAGS)
sess =  tf.Session()
saver = tf.train.Saver()
saver.restore(sess, model_checkpoint_path)
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
def main(wordseq, postagseq):
    words = wordseq.split()
    postags = postagseq.split()


    x = []
    x.append([wordIndex.get_index(word) for word in words])
    x.append([ptagIndex.ptag2id[tag] for tag in postags])
    x.append([suffIdex.get_id(word) for word in words])
    x.append([sampleWordIndex.get_id(word) for word in words])


    source_res, trigger_res = model.inference_single(sess, x)

    y_p = ''.join([entTagIndex.id2tag[source_res[j]] for j in range(len(source_res))])
    print(y_p)
    arg_entitys = extractEntity_type(words,y_p,'TERM')
        # print(arg_entitys)
    y_p = ''.join([triTagIndex.id2tag[trigger_res[j]] for j in range(len(trigger_res))])
    print(y_p)
    tri_entitys = extractEntity_type(words, y_p, 'TRI')
        # print(tri_entitys)
    return arg_entitys,tri_entitys

if __name__ == '__main__':
    import datetime
    import sys
    import codecs
    # begin1 = datetime.datetime.now()
    # print('java 调用成功！')

    # argv1 = sys.argv[sys1]
    # argv2= sys.argv[2]argv

    # arg_entitys, tri_entitys = main("美国 袭击 日本", "ns v ns")

    with codecs.open('G:\\MasterTwoU\\28proj\\WordCloud\\temp1.txt', 'w', 'utf-8') as writer:
        for wordseq,postagseq in zip(wordseq_list,posseq_list):
            print(wordseq)
            print(postagseq)
            arg_entitys,tri_entitys = main(wordseq,postagseq)
            writer.write(','.join(arg_entitys) + "\t" + ",".join(tri_entitys) + "\n")

    # begin2 = datetime.datetime.now()
    # print(begin2 - begin1)


