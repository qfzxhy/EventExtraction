import os
import codecs
import numpy as np
import pprint
import tensorflow as tf
import pandas as pd
import helper
import gensim
import shutil
import heapq
# from helper import dataHelper
from  data_helper import DataItor
from voc import Word_Index,Tri_Tag_Index,Ent_Tag_Index,Ptag_Index
from single_task_model import ST_Model
types = ['argument','trigger']
pp = pprint.PrettyPrinter()
flags = tf.app.flags
flags.DEFINE_integer('token_edim',64,'the dim of word embedding')
flags.DEFINE_integer('postag_emb_dim',20,'the dim of postag embedding')
flags.DEFINE_integer('num_suffix',7,'the dim of postag embedding')
flags.DEFINE_integer('suffix_emb_dim',20,'the dim of postag embedding')

flags.DEFINE_integer('character_embedding_dimension',25,'the dim of character embedding')
flags.DEFINE_integer('character_lstm_hidden_state_dimension',25,'the dim of character_lstm_hidden')
flags.DEFINE_integer('token_max_len',9,'')


flags.DEFINE_integer('num_hidden',64,'the dim of word lstm_hidden')
flags.DEFINE_integer('batch_size',1,'the dim of word embedding')
flags.DEFINE_integer('num_epochs',100,'the dim of word embedding')
flags.DEFINE_float('dev_rate',0.3,'the dim of word embedding')
# flags.DEFINE_float('test_rate',0.2,'test data num')
flags.DEFINE_integer('num_corpus',3,'num_corpus')
flags.DEFINE_integer('patients',10,'early stop num')

# flags.DEFINE_integer('check_every_point',10,'')

flags.DEFINE_float('init_lr',0.001,'the dim of word embedding')
flags.DEFINE_float('gradient_clipping_value',5.0,'the dim of word embedding')
flags.DEFINE_float('init_std',0.05,'the dim of word embedding')

flags.DEFINE_string('optimizer','adam','early stop num')



flags.DEFINE_string('pretrain_file','G:/2018/news_12g_baidubaike_20g_novel_90g_embedding_64.bin','')
flags.DEFINE_string('save_dir','runs','ckpt location')
flags.DEFINE_string('best_model_dir','best_models','ckpt location')
flags.DEFINE_string('log_file','runs/log','model log path,like precision recall...')
# flags.DEFINE_string('data_dir',task_data_dir,'....')
flags.DEFINE_boolean('use_crf',True,'')
flags.DEFINE_boolean('use_postag',False,'')
flags.DEFINE_boolean('use_suffix',False,'')
flags.DEFINE_boolean('use_character_lstm',False,'')
flags.DEFINE_boolean('use_pretrain_embedding',True,'')


flags.DEFINE_string('exp_file','datas/exp/stl_exp','')


flags.DEFINE_integer('object_id',0,'')
FLAGS = flags.FLAGS
FLAGS.num_trigger_type = 21
FLAGS.trigger_type_emb_dim = 20
# FLAGS.att_score_path = os.path.join(dirs[task_id],'att_path')

def initial_embedding_from_txt(word2id):
    emb = np.random.normal(0, FLAGS.init_std, [len(word2id), FLAGS.token_edim])
    with codecs.open(FLAGS.pretrain_file, 'r', 'utf-8') as reader:
        for line in reader:
            infos = line.strip().split(' ')
            word = infos[0]
            if word in word2id:
                emb[word2id[word]] = np.array(infos[1:], dtype=float)
    return emb
    pass
def initial_embedding(word2id):
    emb = np.random.normal(0,FLAGS.init_std,[len(word2id),FLAGS.token_edim])
    model = gensim.models.KeyedVectors.load_word2vec_format(FLAGS.pretrain_file,binary=True)
    for word in word2id.keys():
        if word in model:
            emb[word2id[word]] = model[word]
    return emb
def main(_):
    #initial outer file
    WordIndex = Word_Index()
    EntTagIndex = Ent_Tag_Index()
    TriTagIndex = Tri_Tag_Index()
    PtagIndex = Ptag_Index()
    Index = [EntTagIndex,TriTagIndex]
    object_tag_Index = Index[FLAGS.object_id]

    FLAGS.num_word = len(WordIndex.word2idex)
    FLAGS.num_postag = len(PtagIndex.ptag2id)
    FLAGS.num_class = object_tag_Index.num_class
    df_test = pd.read_csv('datas/corpus/testdata_id.txt', sep='#', skip_blank_lines=False, dtype={'len': np.int32})
    df_train = pd.read_csv('datas/corpus/traindata_id.txt', sep='#', skip_blank_lines=False, dtype={'len': np.int32})
    # eval_size = int(len(df_train) * FLAGS.dev_rate)
    # df_eval = df_train.iloc[-eval_size:]
    # df_train = df_train.iloc[:-eval_size]
    print('trainsize' + str(len(df_train)))
    train_data_itor = DataItor(df_train,False)
    # eval_data_itor = DataItor(df_eval,False)
    test_data_itor = DataItor(df_test, False)
    FLAGS.check_every_point = int(train_data_itor.size / FLAGS.batch_size)
    if os.path.exists(FLAGS.pretrain_file) and FLAGS.use_pretrain_embedding:
        print('initial!!!!!!!!!!')
        # initial_embedding(WordIndex.word2idex)
        FLAGS.pretrain_emb = initial_embedding(WordIndex.word2idex)
    else:
        FLAGS.pretrain_emb = None

    myconfig = tf.ConfigProto(allow_soft_placement=True)
    with tf.Session(config=myconfig) as sess:
        model = ST_Model(FLAGS)
        sess.run(tf.global_variables_initializer())
        # xidx_eval, tidx_eval, lens_eval = eval_data_itor.next_all()
        xidx_test, tidx_test, lens_test = test_data_itor.next_all()
        # print('eval data size %f' % len(tidx_eval[0]))



        # _, id2label = helper.loadMap(FLAGS.label2id_path)
        saver = tf.train.Saver(max_to_keep=2)
        previous_best_valid_f1_score = 0
        previous_best_epoch = -1
        bad_count = 0
        heap_source, heap_trigger, heap_target = [], [], []
        while train_data_itor.epoch < FLAGS.num_epochs:
            x_train_batch, y_train_batch = train_data_itor.next_batch(FLAGS.batch_size)

            train_step, train_loss = model.train_model(sess, x_train_batch, y_train_batch[FLAGS.object_id])

            if train_data_itor.batch_time % FLAGS.check_every_point == 0:
                print("current batch_time: %d" % (train_data_itor.batch_time))
                # y_eval_pred, eval_loss = model.inference_for_single(sess, xidx_eval, tidx_eval[FLAGS.object_id])
                y_test_pred, test_loss = model.inference_for_single(sess, xidx_test, tidx_test[FLAGS.object_id])
                if FLAGS.object_id == 0:
                    # precison, recall, f1_eval = helper.evaluate_ent(xidx_eval[0], tidx_eval[FLAGS.object_id],
                    #                                                y_eval_pred, id2word=WordIndex.id2word,id2label=EntTagIndex.id2tag,seq_lens=lens_eval)
                    # print('evalution on eval data, target_eval_loss:%.4f,precison:%.4f,recall:%.4f,fscore:%.4f' % (
                    # eval_loss, precison, recall, f1_eval))
                    precison1, recall1, f1_test = helper.evaluate_ent(xidx_test[0], tidx_test[FLAGS.object_id],
                                                                             y_test_pred, id2word=WordIndex.id2word,id2label=EntTagIndex.id2tag,seq_lens=lens_test)
                    print('evalution on test data, target_eval_loss:%.3f,precison:%.4f,recall:%.4f,fscore:%.4f' % (
                    test_loss, precison1, recall1, f1_test))
                else:
                    # precison, recall, f1_eval =  helper.evaluate_tri(xidx_eval[0], tidx_eval[FLAGS.object_id], y_eval_pred,id2word=WordIndex.id2word,seq_lens=lens_eval)
                    # print('evalution on eval data, target_eval_loss:%.3f,precison:%.4f,recall:%.4f,fscore:%.4f' % (
                    # eval_loss, precison, recall, f1_eval))
                    precison1, recall1, f1_test =  helper.evaluate_tri(xidx_test[0], tidx_test[FLAGS.object_id], y_test_pred,id2word=WordIndex.id2word,seq_lens=lens_test)
                    print('evalution on test data, target_eval_loss:%.3f,precison:%.4f,recall:%.4f,fscore:%.4f' % (
                    test_loss, precison1, recall1, f1_test))




                # early stop
                if len(heap_target) < 5:
                    heapq.heappush(heap_target, (f1_test,(precison1, recall1, f1_test)))
                else:
                    if f1_test > heap_target[0][0]:
                        heapq.heappop(heap_target)
                        heapq.heappush(heap_target, (f1_test,(precison1, recall1, f1_test)))
                if f1_test > previous_best_valid_f1_score:
                    helper.store_stl_result(xidx_test[0], tidx_test[FLAGS.object_id], y_test_pred,
                                        id2word=WordIndex.id2word, id2label=Index[FLAGS.object_id].id2tag, seq_lens=lens_test,type_id=FLAGS.object_id)

                    previous_best_valid_f1_score = f1_test
                    bad_count = 0
                else:
                    bad_count += 1

                if bad_count >= FLAGS.patients:
                    print('early stop!')
                    break
        print('Train Finished!!')
        show_result(heap_target)


    pass
def show_result(heap):
    writer = codecs.open(FLAGS.exp_file, 'a', 'utf-8')
    writer.write('--use_postag:' + str(FLAGS.use_postag)
                 + '--use_suffix:' + str(FLAGS.use_suffix)
                 + '--object:' + str(types[FLAGS.object_id]) + '\n')

    Mean_Test = [0.0,0.0,0.0]
    Max_Test = 0.0
    for infos in heap:
        Mean_Test[0] += infos[1][0]
        Mean_Test[1] += infos[1][1]
        Mean_Test[2] += infos[1][2]
        Max_Test = max(Max_Test, infos[1][2])
        writer.write('Test score:' +str(infos[1][0])+"," + str(infos[1][1])+"," + str(infos[1][2]) + '\n')
    writer.write('Test Mean fscore' + str(Mean_Test[0] / len(heap))+"," + str(Mean_Test[1] / len(heap))+"," + str(Mean_Test[2] / len(heap)) + '\n')
    writer.write('Test Max fscore:%.4f' % (Max_Test) + '\n')
    writer.close()

def store_weights(weights_list):
    with codecs.open(FLAGS.att_score_path,'w','utf-8') as writer:
        for weights in weights_list:
            for i in range(len(weights)):
                for j in range(len(weights)):
                    writer.write(str(weights[i,j])+' ')

            writer.write('\n')
    pass
if __name__ == '__main__':
    tf.app.run()

