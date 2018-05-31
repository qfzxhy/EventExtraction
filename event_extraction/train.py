import os
import codecs
import numpy as np
import pprint
import tensorflow as tf
import pandas as pd
from data_helper import DataItor
from voc import Word_Index,Ent_Tag_Index,Tri_Tag_Index,Ptag_Index,Char_Index
import helper
import gensim
import shutil
import heapq
# tf.set_random_seed(1024)
from mt_model import MT_Model
pp = pprint.PrettyPrinter()
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
flags.DEFINE_boolean('use_pretrain_embedding',True,'')

flags.DEFINE_boolean('use_postag',True,'')
flags.DEFINE_boolean('use_suffix',True,'')
flags.DEFINE_boolean('use_character_lstm',False,'')

flags.DEFINE_boolean('multi_task',True,'')
flags.DEFINE_boolean('use_couple',False,'')

flags.DEFINE_string('exp_file','datas/exp/mtl_exp','')
flags.DEFINE_integer('object_id',1,'')

FLAGS = flags.FLAGS
FLAGS.num_trigger_type = 21
FLAGS.trigger_type_emb_dim = 20
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
def remove_past_model():

    for file in os.listdir(FLAGS.save_dir):
        os.remove(os.path.join(FLAGS.save_dir,file))
    for file in os.listdir(FLAGS.best_model_dir):
        if os.path.isfile(os.path.join(FLAGS.best_model_dir, file)):
            os.remove(os.path.join(FLAGS.best_model_dir, file))
        else:
            shutil.rmtree(os.path.join(FLAGS.best_model_dir, file))

def main(_):
    CharIndex = Char_Index()
    WordIndex = Word_Index()
    PtagIndex = Ptag_Index()
    EntTagIndex = Ent_Tag_Index()
    TriTagIndex = Tri_Tag_Index()

    FLAGS.num_word = len(WordIndex.word2idex)
    FLAGS.num_postag = len(PtagIndex.ptag2id)
    FLAGS.alphabet_size = len(CharIndex.char2idex)
    FLAGS.task1_num_class = EntTagIndex.num_class
    FLAGS.task2_num_class = TriTagIndex.num_class
    print(FLAGS.task1_num_class)
    print(FLAGS.task2_num_class)
    df_test = pd.read_csv('datas/corpus/testdata_id.txt', sep='#', skip_blank_lines=False, dtype={'len': np.int32})
    df_train = pd.read_csv('datas/corpus/traindata_id.txt', sep='#', skip_blank_lines=False, dtype={'len': np.int32})
    # eval_size = int(len(df_train) * FLAGS.dev_rate)
    # df_eval = df_train.iloc[-eval_size:]
    # df_train = df_train.iloc[:-eval_size]
    print('trainsize'+str(len(df_train)))
    train_data_itor = DataItor(df_train)
    # eval_data_itor = DataItor(df_eval)
    test_data_itor = DataItor(df_test,False)
    FLAGS.check_every_point = int(train_data_itor.size / FLAGS.batch_size)
    if os.path.exists(FLAGS.pretrain_file) and FLAGS.use_pretrain_embedding:
        print('initial!!!!!!!!!!')
        # initial_embedding(WordIndex.word2idex)
        FLAGS.pretrain_emb = initial_embedding(WordIndex.word2idex)
    else:
        FLAGS.pretrain_emb = None

    myconfig = tf.ConfigProto(allow_soft_placement = True)
    with tf.Session(config=myconfig) as sess:

        model = MT_Model(FLAGS)
        sess.run(tf.global_variables_initializer())
        # xidx_eval,tidx_eval,lens_eval = eval_data_itor.next_all()
        xidx_test,tidx_test,lens_test = test_data_itor.next_all()
        # print('eval data size %f' % len(tidx_eval[0]))
        saver = tf.train.Saver(max_to_keep=2)
        prev_best_f1_entity,prev_best_f1_trigger = 0,0
        task_continued = [True,True]
        bad_count_entity,bad_count_trigger = 0,0
        heap_source, heap_trigger, heap_target = [], [], []
        heap = []
        while train_data_itor.epoch < FLAGS.num_epochs:

            x_train_batch,y_train_batch = train_data_itor.next_batch(FLAGS.batch_size)
            step, loss = model.train_model_union(sess, x_train_batch, y_train_batch)


            if train_data_itor.batch_time % FLAGS.check_every_point == 0:
                print("current batch_time: %d" % (train_data_itor.batch_time))
                # source_res_eval, trigger_res_eval, eval_loss = model.inference(sess, xidx_eval,tidx_eval)
                source_res_test, trigger_res_test, test_loss = model.inference(sess, xidx_test,tidx_test)
                # prec_sou, rec_sou, fscore_sou_eval = helper.evaluate_ent(xidx_eval[0], tidx_eval[0], source_res_eval,id2word=WordIndex.id2word,id2label=EntTagIndex.id2tag,seq_lens=lens_eval)
                # print('evalution on eval data, source_eval_loss:%.4f,precison:%.4f,recall:%.4f,fscore:%.4f' % (eval_loss, prec_sou, rec_sou, fscore_sou_eval))

                prec_sou, rec_sou, fscore_sou_test = helper.evaluate_ent(xidx_test[0], tidx_test[0], source_res_test,id2word=WordIndex.id2word, id2label=EntTagIndex.id2tag,seq_lens=lens_test)
                print('evalution on test data, source_test_loss:%.4f,precison:%.4f,recall:%.4f,fscore:%.4f' % (test_loss, prec_sou, rec_sou, fscore_sou_test))

                saver.save(sess, os.path.join('runs', 'model_{0:05d}.ckpt'.format(train_data_itor.epoch)))
                # prec_tri, rec_tri, fscore_tri_eval = helper.evaluate_tri(xidx_eval[0], tidx_eval[1], trigger_res_eval,id2word=WordIndex.id2word,seq_lens=lens_eval)
                # print('evalution on eval data, trigger_eval_loss:%.4f,precison:%.4f,recall:%.4f,fscore:%.4f' % (eval_loss, prec_tri, rec_tri, fscore_tri_eval))

                prec_tri, rec_tri, fscore_tri_test = helper.evaluate_tri(xidx_test[0], tidx_test[1], trigger_res_test,id2word=WordIndex.id2word,seq_lens=lens_test)
                print('evalution on test data, trigger_test_loss:%.4f,precison:%.4f,recall:%.4f,fscore:%.4f' % (test_loss, prec_tri, rec_tri, fscore_tri_test))

                # save each model, store 5 models by time
                # saver.save(sess, os.path.join('runs', 'model_{0:05d}.ckpt'.format(train_data_itor.batch_time)))
                # writer_log.write('evalution on eval data, eval_loss:%.3f,precison:%.3f,recall:%.3f,fscore:%.3f' % (target_eval_loss, precison, recall, valid_f1_score) + '\n')

                # save best model, store 3 models by fscore
                # if bad_count_entity < FLAGS.patients:
                if FLAGS.object_id == 0:
                    head_fscore = fscore_sou_test
                else:
                    head_fscore = fscore_tri_test
                k_top_result(heap_source,(head_fscore,(prec_sou, rec_sou, fscore_sou_test),(prec_tri, rec_tri, fscore_tri_test)))
                # if bad_count_trigger < FLAGS.patients:
                # k_top_result(heap_trigger, (fscore_tri_eval,fscore_tri_test))
                # k_top_result(heap_target, fscore_tar)
                epoch = train_data_itor.epoch
                saver.save(sess, os.path.join('runs', 'model_{0:05d}.ckpt'.format(epoch)))


                # save best model, store 3 models by fscore
                zipname = "{}{}.zip".format('lstm', '{0:05d}'.format(epoch))
                if len(heap) < 1:
                    heapq.heappush(heap, (head_fscore, epoch))
                    os.system("winrar a {best_ckptdir}/{zipname} {checkpoint} {ckptdir}/*{global_step}*".format(
                        best_ckptdir='best_models',
                        zipname=zipname,
                        checkpoint=os.path.join('runs', 'checkpoint'),
                        ckptdir='runs',
                        global_step=epoch))
                else:
                    if head_fscore > heap[0][0]:
                        _, delete_file_epoch = heapq.heappop(heap)
                        heapq.heappush(heap, (head_fscore, epoch))
                        os.remove("{}/{}{}.zip".format('best_models', 'lstm', '{0:05d}'.format(delete_file_epoch)))
                        os.system("winrar a {best_ckptdir}/{zipname} {checkpoint} {ckptdir}/*{global_step}*".format(
                            best_ckptdir='best_models',
                            zipname=zipname,
                            checkpoint=os.path.join('runs', 'checkpoint'),
                            ckptdir='runs',
                            global_step=epoch))
                # early stop
                if head_fscore > prev_best_f1_entity:
                    helper.store_result(xidx_test[0], tidx_test[0], source_res_test,tidx_test[1], trigger_res_test,id2word=WordIndex.id2word, id2label=EntTagIndex.id2tag,id2label1=TriTagIndex.id2tag,seq_lens=lens_test)
                    prev_best_f1_entity = head_fscore
                    bad_count_entity = 0
                else:
                    bad_count_entity += 1

                if bad_count_entity >= FLAGS.patients:
                    task_continued[0] = False

                # if fscore_tri_eval > prev_best_f1_trigger:
                #     prev_best_f1_trigger = fscore_tri_eval
                #     bad_count_trigger = 0
                # else:
                #     bad_count_trigger += 1
                #
                # if bad_count_trigger >= FLAGS.patients:
                #     task_continued[1] = False

                if not task_continued[0]:
                    print('early stop!!!')
                    break

        print('Train Finished!!')
        show_result(heap_source)
        # show_result(heap_trigger)
        # show_result(heap_target)


    pass

def k_top_result(heap,fscore):
    K = 5
    if len(heap) < K:
        heapq.heappush(heap, fscore)
    else:
        if fscore[0] > heap[0][0]:
            heapq.heappop(heap)
            heapq.heappush(heap, fscore)


def show_result(heap):
    writer = codecs.open(FLAGS.exp_file,'a','utf-8')
    writer.write('--use_postag:'+str(FLAGS.use_postag)
                 +'--use_suffix:'+str(FLAGS.use_suffix)
                 +'--use_character_lstm:'+str(FLAGS.use_character_lstm)
                 + '--object:' + str(types[FLAGS.object_id]) + '\n')

    Mean_Test_TA, Mean_Test_OP = [0.0,0.0,0.0], [0.0,0.0,0.0]
    ta_test_max, op_test_max = 0.0, 0.0
    for infos in heap:
        Mean_Test_TA[0] += infos[1][0]
        Mean_Test_TA[1] += infos[1][1]
        Mean_Test_TA[2] += infos[1][2]

        Mean_Test_OP[0] += infos[2][0]
        Mean_Test_OP[1] += infos[2][1]
        Mean_Test_OP[2] += infos[2][2]

        if infos[1][-1] > ta_test_max:
            ta_test_max = infos[1][-1]
        if infos[2][-1] > op_test_max:
            op_test_max = infos[2][-1]
        writer.write('Test_sou score:' + str(infos[1][0])+','+str(infos[1][1])+','+str(infos[1][2]))
        writer.write('Test_tri score:' + str(infos[2][0]) + ',' + str(infos[2][1]) + ',' + str(infos[2][2]) + '\n')

    writer.write('Test_sou Mean fscore:' + str( Mean_Test_TA[0] / len(heap))+','+str( Mean_Test_TA[1] / len(heap))+','+str( Mean_Test_TA[2] / len(heap)))
    writer.write('Test_tri Mean fscore:' + str( Mean_Test_OP[0] / len(heap))+','+str( Mean_Test_OP[1] / len(heap))+','+str( Mean_Test_OP[2] / len(heap)) + '\n')

    writer.write('Test_ta fscore:%.4f  Test_tri fscore:%.4f' % (ta_test_max, op_test_max) + '\n')

    writer.write('two task average fscore:%.4f' % ((ta_test_max + op_test_max) / 2) + '\n')
    writer.close()
if __name__ == '__main__':
    tf.app.run()

