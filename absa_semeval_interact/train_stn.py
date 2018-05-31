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
from voc import Word_Index,Opi_Tag_Index,Tar_Tag_Index
from single_task_model import BILSTM
pp = pprint.PrettyPrinter()
tf.set_random_seed(1024)
np.random.seed(1024)
emb_path = ['Amazon_Electronic_200_3.w2v.bin','yelp.vector.bin','yelp.vector.bin']
types = [
    'target','opword'
]
dirs = [
        'laptop14_data',
        'res14_data',
        'res15_data'
    ]
vob_paths = [
        'laptop14_data/vocabs/word2id',
        'res14_data/vocabs/word2id',
        'res15_data/vocabs/word2id'
    ]
data_paths = [
        ['laptop14_data/datas/train_indice', 'laptop14_data/datas/test_indice'],
        ['res14_data/datas/train_indice', 'res14_data/datas/test_indice'],
        ['res15_data/datas/train_indice', 'res15_data/datas/test_indice'],
    ]
exp_paths = [
        'laptop14_data/stl_exp',
        'res14_data/stl_exp',
        'res15_data/stl_exp'
]
flags = tf.app.flags
flags.DEFINE_integer('task_id',2,'')
flags.DEFINE_integer('num_postag',5,'the num of postag')
flags.DEFINE_integer('token_edim',200,'the dim of word embedding')
flags.DEFINE_integer('postag_emb_dim',25,'the dim of postag embedding')
flags.DEFINE_integer('character_embedding_dimension',25,'the dim of character embedding')
flags.DEFINE_integer('character_lstm_hidden_state_dimension',25,'the dim of character_lstm_hidden')
flags.DEFINE_integer('num_hidden',100,'the dim of word lstm_hidden')
flags.DEFINE_integer('batch_size',1,'the dim of word embedding')
flags.DEFINE_integer('num_epochs',100,'the dim of word embedding')
flags.DEFINE_integer('dev_rate',0.1,'the dim of word embedding')
flags.DEFINE_integer('num_corpus',3,'num_corpus')
flags.DEFINE_integer('patients',5,'early stop num')
flags.DEFINE_integer('token_max_len',24,'')
#0:评价对象1：评价词
flags.DEFINE_integer('object_id',0,'')
flags.DEFINE_float('w',1,'')

flags.DEFINE_float('init_lr',0.001,'the dim of word embedding')
flags.DEFINE_float('gradient_clipping_value',5.0,'the dim of word embedding')
flags.DEFINE_float('init_std',0.05,'the dim of word embedding')

flags.DEFINE_string('optimizer','adam','early stop num')


flags.DEFINE_string('save_dir','runs','ckpt location')
flags.DEFINE_string('best_model_dir','best_models','ckpt location')
flags.DEFINE_string('log_file','runs/log','model log path,like precision recall...')
flags.DEFINE_string('exp_file','experiments/default','')
flags.DEFINE_string('att_score_path','experiments/att','')

flags.DEFINE_boolean('use_crf',True,'')

flags.DEFINE_boolean('show_opword_eval',True,'')
flags.DEFINE_boolean('use_character_lstm',False,'')
flags.DEFINE_boolean('use_pos',False,'')
FLAGS = flags.FLAGS
task_id = FLAGS.task_id - 1
FLAGS.pretrain_file = 'G:/master4/datasets/glove.6B/'+emb_path[task_id]
FLAGS.train_file = data_paths[task_id][0]
FLAGS.test_file = data_paths[task_id][1]
FLAGS.word2id_path = vob_paths[task_id]
# FLAGS.att_score_path = os.path.join(dirs[task_id],'att_path')

def initial_embedding_from_txt(word2id):
    emb = np.random.normal(0, FLAGS.init_std, [len(word2id), FLAGS.token_edim])
    with codecs.open(FLAGS.pretrain_file, 'r', 'utf-8') as reader:
        for line in reader:
            infos = line.strip().split(' ')
            if len(infos) == 2:
                print('vec_300 first line')
                continue
            word = infos[0]
            if word in word2id:
                emb[word2id[word]] = np.array(infos[1:], dtype=float)
    return emb
    pass
def initial_embedding_yelp_bin(word2id):
    emb = np.random.normal(0,FLAGS.init_std,[len(word2id),FLAGS.token_edim])
    model = gensim.models.KeyedVectors.load_word2vec_format(FLAGS.pretrain_file,
                                                            binary=True)
    for word in word2id.keys():
        if word in model:
            emb[word2id[word]] = model[word]
    return emb
def initial_embedding_yelp_txt(word2id):
    emb = np.random.normal(0, FLAGS.init_std, [len(word2id), FLAGS.token_edim])
    with codecs.open('H:/BaiduYunDownload/opinion_extraction/yelp.vector.txt', 'r', 'utf-8') as reader:
        for line in reader:
            infos = line.strip().split(' ')
            word = infos[0]
            if word in word2id:
                emb[word2id[word]] = np.array(infos[1:], dtype=float)
    return emb
def remove_past_model():

    for file in os.listdir(FLAGS.save_dir):
        os.remove(os.path.join(FLAGS.save_dir,file))
    for file in os.listdir(FLAGS.best_model_dir):
        if os.path.isfile(os.path.join(FLAGS.best_model_dir, file)):
            os.remove(os.path.join(FLAGS.best_model_dir, file))
        else:
            shutil.rmtree(os.path.join(FLAGS.best_model_dir, file))

def main(_):
    #initial outer file
    WordIndex = Word_Index(FLAGS.word2id_path)
    Index = [Tar_Tag_Index(),Opi_Tag_Index()]
    object_tag_Index = Index[FLAGS.object_id]

    FLAGS.dev_rate = 0.1
    FLAGS.num_word = len(WordIndex.word2idex)
    FLAGS.num_class = object_tag_Index.num_class

    df_test = pd.read_csv(FLAGS.test_file, sep='#', skip_blank_lines=False, dtype={'len': np.int32})
    df_train = pd.read_csv(FLAGS.train_file, sep='#', skip_blank_lines=False, dtype={'len': np.int32})
    random_index = np.random.permutation(len(df_train))
    df_train = df_train.iloc[random_index].reset_index()
    print(random_index)
    for i in range(len(random_index)):
        print(random_index[i])

    eval_size = int(len(df_train) * FLAGS.dev_rate)
    df_eval = df_train.iloc[-eval_size:]

    df_train = df_train.iloc[:-eval_size]
    print('trainsize' + str(len(df_train)))
    train_data_itor = DataItor(df_train)
    eval_data_itor = DataItor(df_eval)
    test_data_itor = DataItor(df_test)
    FLAGS.check_every_point = int(train_data_itor.size / FLAGS.batch_size)
    word2id, id2word = helper.loadMap(FLAGS.word2id_path)
    if os.path.exists(FLAGS.pretrain_file):
        FLAGS.pretrain_emb = initial_embedding_yelp_bin(word2id)
    else:
        FLAGS.pretrain_emb = None

    myconfig = tf.ConfigProto(allow_soft_placement = True)
    with tf.Session(config=myconfig) as sess:
        model = BILSTM(FLAGS)
        sess.run(tf.global_variables_initializer())
        w_xs_eval, y_tuple_eval, _,lens_eval = eval_data_itor.next_all(False)
        w_xs_test, y_tuple_test,_, lens_test = test_data_itor.next_all_no_padding(False)
        print('eval data size %f' % len(w_xs_eval))



        # _, id2label = helper.loadMap(FLAGS.label2id_path)
        saver = tf.train.Saver(max_to_keep=2)
        previous_best_valid_f1_score = 0
        previous_best_epoch = -1
        bad_count = 0
        heap_target, heap_opword = [], []
        while train_data_itor.epoch < FLAGS.num_epochs:
            x_train_batch, y_train_batch,_= train_data_itor.next_batch(FLAGS.batch_size)
            train_step, train_loss = model.train_model(sess, x_train_batch, y_train_batch[FLAGS.object_id])
            if train_data_itor.batch_time % FLAGS.check_every_point == 0:
                print("current batch_time: %d" % (train_data_itor.batch_time))
                y_eval_pred, eval_loss = model.inference_for_single(sess, w_xs_eval, y_tuple_eval[FLAGS.object_id])
                precison, recall, target_f1_eval = helper.evaluate(w_xs_eval, y_tuple_eval[FLAGS.object_id],
                                                                   y_eval_pred, id2word=id2word,seq_lens=lens_eval,
                                                                   label_type=types[FLAGS.object_id])

                print('evalution on eval data, target_eval_loss:%.3f,precison:%.3f,recall:%.3f,fscore:%.3f' % ( eval_loss, precison, recall, target_f1_eval))



                y_test_pred, test_loss= model.inference_for_single(sess, w_xs_test, y_tuple_test[FLAGS.object_id])
                precison1, recall1, target_f1_test = helper.evaluate(w_xs_test, y_tuple_test[FLAGS.object_id],
                                                                     y_test_pred, id2word=id2word,seq_lens=lens_test,
                                                                     label_type=types[FLAGS.object_id])
                print('evalution on test data, target_eval_loss:%.3f,precison:%.3f,recall:%.3f,fscore:%.3f' % ( test_loss, precison1, recall1, target_f1_test))



                if len(heap_target) < 5:
                    heapq.heappush(heap_target, (target_f1_eval, train_data_itor.epoch, target_f1_test))
                else:
                    if target_f1_eval > heap_target[0][0]:
                        _, delete_file_epoch,_ = heapq.heappop(heap_target)
                        heapq.heappush(heap_target, (target_f1_eval, train_data_itor.epoch, target_f1_test))


                # early stop
                if target_f1_eval > previous_best_valid_f1_score:
                    previous_best_valid_f1_score = target_f1_eval
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
    writer = codecs.open(exp_paths[task_id],'a','utf-8')
    writer.write('----------------- -- -- -- -- --- -- -- ------- ------- ----- ------ ----------'+types[FLAGS.object_id]+ '\n')
    Mean_Val,Mean_Test = 0.0,0.0
    Max_Val, Max_Test = 0.0, 0.0
    for infos in heap:
        Mean_Val += infos[0]
        Mean_Test += infos[2]
        Max_Val = max(Max_Val,infos[0])
        Max_Test = max(Max_Test,infos[2])
        writer.write('Val fscore:%.4f  Test fscore:%.4f' % (infos[0], infos[2]) + '\n')
    writer.write('Val Mean fscore:%.4f  Test Mean fscore:%.4f' % (Mean_Val/len(heap), Mean_Test/len(heap)) + '\n')
    writer.write('Val Max fscore:%.4f  Test Max fscore:%.4f' % (Max_Val, Max_Test) + '\n')
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

