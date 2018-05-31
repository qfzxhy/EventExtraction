# -*- coding:utf-8 -*-
_author_ = 'qianf'
import helper
import tensorflow as tf
import codecs
# from tensorflow.models.rnn import rnn
import math
import os
import numpy as np
import heapq
import MDAtt as mdatt
# from voc import Tri_Tag_Index
# tri_num_class = 4
# print(tri_num_class)


def bidirectional_LSTM(input, hidden_state_dimension, initializer, sequence_length=None, output_sequence=True):
    with tf.variable_scope("bidirectional_LSTM"):
        if sequence_length == None:
            batch_size = 1
            sequence_length = tf.shape(input)[1]
            sequence_length = tf.expand_dims(sequence_length, axis=0, name='sequence_length')
        else:
            batch_size = tf.shape(sequence_length)[0]

        lstm_cell = {}
        initial_state = {}
        for direction in ["forward", "backward"]:
            with tf.variable_scope(direction):
                # LSTM cell
                lstm_cell[direction] = tf.contrib.rnn.CoupledInputForgetGateLSTMCell(hidden_state_dimension,
                                                                                     forget_bias=1.0,
                                                                                     initializer=initializer,
                                                                                     state_is_tuple=True)
                # initial state: http://stackoverflow.com/questions/38441589/tensorflow-rnn-initial-state
                initial_cell_state = tf.get_variable("initial_cell_state", shape=[1, hidden_state_dimension],
                                                     dtype=tf.float32, initializer=initializer)
                initial_output_state = tf.get_variable("initial_output_state", shape=[1, hidden_state_dimension],
                                                       dtype=tf.float32, initializer=initializer)
                c_states = tf.tile(initial_cell_state, tf.stack([batch_size, 1]))
                h_states = tf.tile(initial_output_state, tf.stack([batch_size, 1]))
                initial_state[direction] = tf.contrib.rnn.LSTMStateTuple(c_states, h_states)

        # sequence_length must be provided for tf.nn.bidirectional_dynamic_rnn due to internal bug
        outputs, final_states = tf.nn.bidirectional_dynamic_rnn(lstm_cell["forward"],
                                                                lstm_cell["backward"],
                                                                input,
                                                                dtype=tf.float32,
                                                                sequence_length=sequence_length,
                                                                initial_state_fw=initial_state["forward"],
                                                                initial_state_bw=initial_state["backward"])
        if output_sequence == True:
            outputs_forward, outputs_backward = outputs
            output = tf.concat([outputs_forward, outputs_backward], axis=2, name='output_sequence')
        else:
            final_states_forward, final_states_backward = final_states
            output = tf.concat([final_states_forward[1], final_states_backward[1]], axis=1, name='output')

    return output


class BILSTM(object):
    def __init__(self, FLAGs):
        self.FLAGS = FLAGs
        # self.patients = FLAGs.patients
        self.task1_num_classess = FLAGs.task1_num_class
        self.task2_num_classess = FLAGs.task2_num_class
        self.num_corpus = FLAGs.num_corpus
        self.num_words = FLAGs.num_word
        self.token_emb_dim = FLAGs.token_edim
        self.num_postags = FLAGs.num_postag
        self.postag_emb_dim = FLAGs.postag_emb_dim
        self.num_hiddens = FLAGs.num_hidden
        self.token_max_len = FLAGs.token_max_len
        self.num_layers = 1
        self.pretrain_emb = FLAGs.pretrain_emb
        # self.is_crf = FLAGs.use_crf
        self.num_units = 100
        self.if_l2 = False
        self.learning_rate = FLAGs.init_lr

        self.max_f1 = -1.0
        # self.sess = sess
        self.sou_global_step = tf.Variable(0, name='sou_global_step', trainable=False)
        self.tri_global_step = tf.Variable(0, name='tri_global_step', trainable=False)

        self.batch_size = tf.placeholder(tf.int32, name='batch_size')

        self.input_token_indices = tf.placeholder(tf.int32, [None, None], name="input_token_indices")
        self.input_token_character_indices = tf.placeholder(tf.int32, [None, None, self.token_max_len],name="input_token_character_indices")
        self.input_postag_indices = tf.placeholder(tf.int32, [None, None], name="input_postag_indices")
        self.input_suffix_indices = tf.placeholder(tf.int32, [None, None], name="input_suffix_indices")

        self.y_entitys = tf.placeholder(tf.int32, [None, None], name='y_entitys')
        # two class
        self.y_entitys_tc = tf.placeholder(tf.int32, [None, None], name='y_entitys_tc')
        # self.y_targets = tf.placeholder(tf.int32, [None, None], name='y_targets')
        # #two class
        # self.y_targets_tc = tf.placeholder(tf.int32, [None, None], name='y_targets_tc')
        self.y_triggers = tf.placeholder(tf.int32, [None, None], name='y_triggers')
        self.y_triggers_tc = tf.placeholder(tf.int32, [None, None], name='y_triggers_tc')



        self.keep_dropout = tf.placeholder(dtype=tf.float32, name='keep_dropout')

        self.initializer = tf.contrib.layers.xavier_initializer(seed=0)
        self.length = tf.reduce_sum(tf.sign(self.input_token_indices), 1)
        self.length = tf.to_int32(self.length)
        max_seq_len = tf.shape(self.input_token_indices)[1]

        #


        def _entity_model(input_data, seq_len):
            # batchsize, step, dim
            # print(input_data.get_shape()[-1].value)
            with tf.variable_scope("bilstm_layer_1"):
                lstm_outputs_flat = bidirectional_LSTM(input_data, self.num_hiddens, self.initializer, sequence_length=seq_len, output_sequence=True)
            # batchsize*step , 2*dim
            lstm_outputs = tf.reshape(lstm_outputs_flat, shape=[-1, lstm_outputs_flat.get_shape()[-1].value])
            with tf.variable_scope("feedforward_tc"):
                scores_tc = _common_layer(lstm_outputs, 2)
            with tf.variable_scope("bilstm_layer_2"):
                lstm_outputs = bidirectional_LSTM(lstm_outputs_flat, self.num_hiddens, self.initializer, sequence_length=seq_len, output_sequence=True)
            return lstm_outputs,scores_tc

        def _trigger_model(input_data, seq_len):
            # batchsize, step, dim
            # print(input_data.get_shape()[-1].value)
            with tf.variable_scope("bilstm_layer_1"):
                lstm_outputs_flat = bidirectional_LSTM(input_data, self.num_hiddens, self.initializer, sequence_length=seq_len, output_sequence=True)
            # batchsize*step , 2*dim
            lstm_outputs = tf.reshape(lstm_outputs_flat, shape=[-1, lstm_outputs_flat.get_shape()[-1].value])
            with tf.variable_scope("feedforward_tc"):
                scores_tc = _common_layer(lstm_outputs, 2)
            with tf.variable_scope("bilstm_layer_2"):
                lstm_outputs = bidirectional_LSTM(lstm_outputs_flat, self.num_hiddens, self.initializer, sequence_length=seq_len, output_sequence=True)
            return lstm_outputs,scores_tc


        def _target_model(input_data, seq_len):
            # if step:
            #     input_data = tf.concat([input_data, share_outputs], axis=-1)
            with tf.variable_scope("bilstm_layer_1"):
                lstm_outputs_flat = bidirectional_LSTM(input_data, self.num_hiddens, self.initializer, sequence_length=seq_len, output_sequence=True)
            lstm_outputs = tf.reshape(lstm_outputs_flat, shape=[-1, lstm_outputs_flat.get_shape()[-1].value])
            with tf.variable_scope("feedforward_tc"):
                scores_tc = _common_layer(lstm_outputs, 2)

            with tf.variable_scope("bilstm_layer_2"):
                lstm_outputs = bidirectional_LSTM(lstm_outputs_flat, self.num_hiddens, self.initializer, sequence_length=seq_len, output_sequence=True)
            return lstm_outputs,scores_tc




        def _common_layer(input_data, output_size, activity=None):
            # print(input_data.get_shape().as_list()[-1])
            W = tf.get_variable(
                "W",
                shape=[input_data.get_shape().as_list()[-1], output_size],
                initializer=self.initializer)

            b = tf.Variable(tf.constant(0.0, shape=[output_size]), name="bias")
            # batchsize*step, 1 * hiddens
            outputs = tf.nn.xw_plus_b(input_data, W, b)
            if activity is not None:
                outputs = activity(outputs, name="activity")
            return outputs

        def _no_interact_layer(lstm_outputs_ent,lstm_outputs_tri):
            lstm_outputs_ent = tf.reshape(lstm_outputs_ent,shape=[-1, lstm_outputs_ent.get_shape()[-1].value])
            lstm_outputs_tri = tf.reshape(lstm_outputs_tri, shape=[-1, lstm_outputs_tri.get_shape()[-1].value])
            # lstm_outputs_tar = tf.reshape(lstm_outputs_tar,shape=[-1, lstm_outputs_tar.get_shape()[-1].value])
            with tf.variable_scope("source_feedforward_after_lstm"):
                outputs_sou = _common_layer(lstm_outputs_ent, 2 * self.num_hiddens, activity=tf.nn.tanh)
            with tf.variable_scope("trigger_feedforward_after_lstm"):
                outputs_tri = _common_layer(lstm_outputs_tri, 2 * self.num_hiddens, activity=tf.nn.tanh)
            # with tf.variable_scope("target_feedforward_after_lstm"):
            #     outputs_tar = _common_layer(lstm_outputs_tar, 2 * self.num_hiddens, activity=tf.nn.tanh)
            return outputs_sou,outputs_tri

        def _interact_layer(lstm_outputs_ent,lstm_outputs_tri,sou_prob,tri_prob):
            with tf.variable_scope('interact'):
                left_values_ent, right_values_ent, _ = _compute_interact_(lstm_outputs_ent, sou_prob)
                left_values_tri, right_values_tri, _ = _compute_interact_(lstm_outputs_tri, tri_prob)
                # left_values_tar, right_values_tar, _ = _compute_interact_(lstm_outputs_tar, tar_prob)
                lstm_outputs_ent_new = tf.concat([lstm_outputs_ent, left_values_tri, right_values_tri], axis=-1)
                # lstm_outputs_tar_new = tf.concat([lstm_outputs_tar, left_values_tri, right_values_tri], axis=-1)
                lstm_outputs_tri_new = tf.concat([lstm_outputs_tri, left_values_ent, right_values_ent], axis=-1)
            lstm_outputs_ent_new = tf.reshape(lstm_outputs_ent_new, shape=[-1, lstm_outputs_ent_new.get_shape()[-1].value])
            # lstm_outputs_tar_new = tf.reshape(lstm_outputs_tar_new,shape=[-1, lstm_outputs_tar_new.get_shape()[-1].value])
            lstm_outputs_tri_new = tf.reshape(lstm_outputs_tri_new,shape=[-1, lstm_outputs_tri_new.get_shape()[-1].value])

            with tf.variable_scope("source_feedforward_after_lstm"):
                outputs_ent = _common_layer(lstm_outputs_ent_new, 2 * self.num_hiddens, activity=tf.nn.tanh)
            with tf.variable_scope("trigger_feedforward_after_lstm"):
                outputs_tri = _common_layer(lstm_outputs_tri_new, 2 * self.num_hiddens, activity=tf.nn.tanh)
            # with tf.variable_scope("target_feedforward_after_lstm"):
            #     outputs_tar = _common_layer(lstm_outputs_tar_new, 2 * self.num_hiddens, activity=tf.nn.tanh)
            return outputs_ent,outputs_tri
        def _inter_att_layer(lstm_outputs_OP,prob_OP,lstm_outputs_TA,prob_TA,use_prob=True):
            #query
            #keys
            #values
            with tf.variable_scope('interact'):
                left_TA_values, right_TA_values, cur_values = _compute_interact_(lstm_outputs_TA,prob_TA)
                # lstm_outputs_s_p_flat = tf.concat([lstm_outputs_s,left_values,right_values],axis=-1)
                left_OP_values, right_OP_values, cur_values = _compute_interact_(lstm_outputs_OP,prob_OP)
                # lstm_outputs_p_s_flat = tf.concat([lstm_outputs_p,left_values,right_values],axis=-1)
            window = 3
            # if use_prob:
            #     lstm_outputs_prob_OP = lstm_outputs_OP * prob_OP
            #     lstm_outputs_prob_TA = lstm_outputs_TA * prob_TA
            with tf.variable_scope('attention_project'):
                op_proj_w = tf.get_variable('op_proj_w',shape=[lstm_outputs_OP.get_shape()[-1],self.num_units])
                lstm_outputs_proj_OP = tf.tensordot(lstm_outputs_OP,op_proj_w,axes=1)
                ta_proj_w = tf.get_variable('ta_proj_w', shape=[lstm_outputs_TA.get_shape()[-1], self.num_units])
                lstm_outputs_proj_TA = tf.tensordot(lstm_outputs_TA, ta_proj_w, axes=1)
            b = tf.shape(lstm_outputs_OP)[0]
            num_step = tf.shape(lstm_outputs_OP)[1]
            with tf.variable_scope('att_OP'):
                input_ta = tf.TensorArray(size=num_step,dtype=tf.float32)
                output_ta = tf.TensorArray(size=num_step,dtype=tf.float32)
                lstm_op_trans = tf.transpose(lstm_outputs_proj_OP,[1,0,2])
                input_ta = input_ta.unstack(lstm_op_trans)
                def _body(time,output_ta_t):
                    uid1 = tf.cond(time < window,lambda : tf.constant(0),lambda  : time - window)
                    uid2 = tf.cond(time+window>=num_step-1,lambda :num_step-1,lambda : time+ window)
                    tmp = [0] * 3
                    # tmp[0] = tf.zeros([b, uid1], tf.float32)
                    tmp[0] = tf.ones([b, time], tf.float32)
                    tmp[1] = tf.zeros([b, 1], tf.float32)
                    tmp[2] = tf.ones([b, num_step - 1 - time], tf.float32)
                    # tmp[4] = tf.zeros([b, num_step - 1 - uid2], tf.float32)
                    score_mask = tf.concat(tmp, axis=1)
                    s,context = _att_layer(input_ta.read(time), lstm_outputs_proj_TA, lstm_outputs_TA, score_mask)
                    att_re = tf.reduce_sum(context, axis=1)
                    output_ta_t = output_ta_t.write(time,att_re)
                    return time + 1,output_ta_t
                with tf.variable_scope('attention'):
                    v_att = tf.get_variable('v_att',shape=[self.num_units],dtype=tf.float32)
                def _condition(time,output_ta_t):
                    return time < num_step
                time = tf.constant(0)
                final_step,output_ta_final = tf.while_loop(
                    cond=_condition,
                    body=_body,
                    loop_vars=(time,output_ta)
                )
                output_final = output_ta_final.stack()
                output_final = tf.transpose(output_final,[1,0,2])
                lstm_outputs_s_p_flat = tf.concat([lstm_outputs_OP, output_final,left_TA_values,right_TA_values], axis=-1)
            with tf.variable_scope('att_TA'):
                input_ta = tf.TensorArray(size=num_step,dtype=tf.float32)
                output_ta = tf.TensorArray(size=num_step,dtype=tf.float32)
                lstm_ta_trans = tf.transpose(lstm_outputs_proj_TA,[1,0,2])
                input_ta = input_ta.unstack(lstm_ta_trans)
                def _body(time,output_ta_t):
                    uid1 = tf.cond(time < window,lambda : tf.constant(0),lambda  : time - window)
                    uid2 = tf.cond(time+window>=num_step-1,lambda :num_step-1,lambda : time+ window)
                    tmp = [0] * 3
                    # tmp[0] = tf.zeros([b, uid1], tf.float32)
                    tmp[0] = tf.ones([b, time], tf.float32)
                    tmp[1] = tf.zeros([b, 1], tf.float32)
                    tmp[2] = tf.ones([b, num_step - 1 - time], tf.float32)
                    # tmp[4] = tf.zeros([b, num_step - 1 - uid2], tf.float32)
                    score_mask = tf.concat(tmp, axis=1)
                    s,context = _att_layer(input_ta.read(time), lstm_outputs_proj_OP, lstm_outputs_OP, score_mask)
                    att_re = tf.reduce_sum(context, axis=1)
                    output_ta_t = output_ta_t.write(time,att_re)
                    return time + 1,output_ta_t
                with tf.variable_scope('attention'):
                    v_att = tf.get_variable('v_att',shape=[self.num_units],dtype=tf.float32)
                def _condition(time,output_ta_t):
                    return time < num_step
                time = tf.constant(0)
                final_step,output_ta_final = tf.while_loop(
                    cond=_condition,
                    body=_body,
                    loop_vars=(time,output_ta)
                )
                output_final = output_ta_final.stack()
                output_final = tf.transpose(output_final, [1, 0, 2])
                lstm_outputs_p_s_flat = tf.concat([lstm_outputs_TA, output_final,left_OP_values,right_OP_values], axis=-1)
            lstm_outputs_s_p = tf.reshape(lstm_outputs_s_p_flat, shape=[-1, 4 * lstm_outputs_OP.get_shape()[-1].value])
            lstm_outputs_p_s = tf.reshape(lstm_outputs_p_s_flat, shape=[-1, 4 * lstm_outputs_TA.get_shape()[-1].value])

            with tf.variable_scope("sp_feedforward_after_lstm"):
                outputs_s_p = _common_layer(lstm_outputs_s_p, 2 * self.num_hiddens, activity=tf.nn.tanh)
            with tf.variable_scope("ps_feedforward_after_lstm"):
                outputs_p_s = _common_layer(lstm_outputs_p_s, 2 * self.num_hiddens, activity=tf.nn.tanh)
            return outputs_s_p, outputs_p_s
        def _crf_interact(Op_score,Ta_score):
            ta_soft = tf.nn.softmax(Ta_score)
            op_soft = tf.nn.softmax(Op_score)
            ta_o_score = tf.split(ta_soft,self.num_classess,axis=2)[0]
            op_o_score = tf.split(op_soft,self.num_classess,axis=2)[0]
            op_score = ta_o_score * Op_score
            ta_score = op_o_score * Ta_score
            return op_score,ta_score

        def _relation_layer(lstm_outputs_s,opword_prob,lstm_outputs_p,target_prob):
            with tf.variable_scope('relation'):
                target_left_values, _, target_cur_values = _compute_interact_(lstm_outputs_p, target_prob, windows=1)
                opinion_left_values, _, opinion_cur_values = _compute_interact_(lstm_outputs_s, opword_prob, windows=1)
                lstm_outputs_OP_flat = tf.concat([target_left_values, opinion_cur_values], axis=-1)
                lstm_outputs_AS_flat = tf.concat([opinion_left_values, target_cur_values], axis=-1)
                lstm_outputs_OP = tf.reshape(lstm_outputs_OP_flat,shape=[-1, lstm_outputs_OP_flat.get_shape()[-1].value])

                lstm_outputs_AS = tf.reshape(lstm_outputs_AS_flat,shape=[-1, lstm_outputs_AS_flat.get_shape()[-1].value])

            with tf.variable_scope("feedforward_layer_OP"):
                scores_OP = _common_layer(lstm_outputs_OP, 2)
            with tf.variable_scope("feedforward_layer_AS"):
                scores_AS = _common_layer(lstm_outputs_AS, 2)
            return scores_OP,scores_AS
            pass
        def _att_layer(query,keys,memory,scores_mask):
            #compute attention energies using a feed forward
            #query: [batchsize,dim]
            #keys: [batchsize,step.dim]
            FLOAT_MIN = -1.e9
            with tf.variable_scope('attention',reuse=True):
                v_att = tf.get_variable('v_att',shape=[self.num_units],dtype=tf.float32)
            energies = tf.reduce_sum(v_att * tf.tanh(keys + tf.expand_dims(query,1)),[2])
            num_scores = tf.shape(energies)[1]
            energies = energies * scores_mask + (1.0 - scores_mask) * FLOAT_MIN
            energies = energies - tf.reduce_max(energies,axis=1,keep_dims=True)
            unnormalized_scores = tf.exp(energies) * scores_mask + 0.0001
            normalization = tf.reduce_sum(unnormalized_scores,axis=1,keep_dims=True)
            normalized_scores = unnormalized_scores / normalization
            context = tf.expand_dims(normalized_scores,2) * memory
            return normalized_scores,context
        def _compute_interact_(keys,probs,flag=True,windows = 3):

            num_step = keys.get_shape().as_list()[1]
            values = keys * probs
            values = tf.transpose(values,[1,0,2])
            append1 = tf.tile(tf.zeros_like(values[:1],dtype=tf.float32),[windows,1,1])
            append2 = tf.zeros_like(values[:1],dtype=tf.float32)
            left_values = 0
            l = []
            l.append(tf.concat([values,append1],axis=0))
            for i in range(1,windows + 1):
                l.append(tf.concat([append2,l[i-1][:-1]],axis=0))
                left_values += l[i]
            left_values = tf.transpose(left_values[:-windows],[1,0,2])

            right_values = 0
            r = []
            r.append(tf.concat([append1,values],axis=0))
            for i in range(1, windows + 1):
                r.append(tf.concat([r[i-1][1:],append2],axis=0))
                right_values += r[i]
            right_values = tf.transpose(right_values[windows:],[1,0,2])
            cur_values = tf.transpose(values,[1,0,2])
            return left_values,right_values,cur_values
        with tf.variable_scope('embedding_layer') as vs:
            if self.pretrain_emb is not None:
                self.token_embedding_weights = tf.Variable(self.pretrain_emb, trainable=True,
                                                           name='token_embedding_weights', dtype=tf.float32)
            else:
                self.token_embedding_weights = tf.get_variable('token_embedding_weights',
                                                               [self.num_words, self.token_emb_dim])
            self.postag_embdding_weights = tf.get_variable('postag_embdding_weights',
                                                           [self.num_postags, self.postag_emb_dim])
            self.suffix_embdding_weights = tf.get_variable('suffix_embdding_weights',
                                                           [FLAGs.num_suffix, FLAGs.suffix_emb_dim])
            embedded_tokens = tf.nn.embedding_lookup(self.token_embedding_weights, self.input_token_indices)
            embedded_postags = tf.nn.embedding_lookup(self.postag_embdding_weights, self.input_postag_indices)
            embedded_suffixs = tf.nn.embedding_lookup(self.suffix_embdding_weights, self.input_suffix_indices)
        if FLAGs.use_character_lstm:
            # Character-level LSTM
            # Idea: reshape so that we have a tensor [number_of_token, max_token_length, token_embeddings_size], which we pass to the LSTM
            # Character embedding layer
            with tf.variable_scope("character_embedding"):
                self.character_embedding_weights = tf.get_variable(
                    "character_embedding_weights",
                    shape=[FLAGs.alphabet_size, FLAGs.character_embedding_dimension],
                    initializer=self.initializer)
                input_character_indices = tf.reshape(self.input_token_character_indices,shape=[-1, self.token_max_len])
                embedded_characters = tf.nn.embedding_lookup(self.character_embedding_weights,
                                                             input_character_indices,
                                                             name='embedded_characters')
            self.input_token_lengths = tf.reduce_sum(tf.sign(input_character_indices), 1)
            self.input_token_lengths = tf.cast(self.input_token_lengths, tf.int32)
            # Character LSTM layer
            with tf.variable_scope('character_lstm') as vs:
                character_lstm_output = bidirectional_LSTM(embedded_characters,
                                                           FLAGs.character_lstm_hidden_state_dimension,
                                                           self.initializer,
                                                           sequence_length=self.input_token_lengths,
                                                           output_sequence=False)
                character_lstm_output = tf.reshape(character_lstm_output,shape=[self.batch_size,-1,2 * FLAGs.character_lstm_hidden_state_dimension])

        # suffix_embs = tf.one_hot(self.input_suffix_indices,depth=7,dtype=tf.float32)
        # Concatenate character LSTM outputs and token embeddings
        # print(FLAGs.use_character_lstm)
        tmp = [embedded_tokens]
        if FLAGs.use_character_lstm:
            tmp.append(character_lstm_output)
        if FLAGs.use_postag:
            tmp.append(embedded_postags)
        if FLAGs.use_suffix:
            tmp.append(embedded_suffixs)

        token_lstm_input = tf.concat(tmp, axis=2,name='token_lstm_input')

        with tf.variable_scope('add_dropout') as vs:
            # batchsize, step, dim_n
            token_lstm_input_drop = tf.nn.dropout(token_lstm_input, self.keep_dropout, name='token_lstm_input_drop')
        with tf.variable_scope("entity_moudle"):
            # 经过共享层句子的表示
            lstm_outputs_source, scores_tc_source = _entity_model(token_lstm_input_drop, self.length)
            soft_scores = tf.nn.softmax(scores_tc_source)
            _, source_prob = tf.split(soft_scores, 2, axis=1)
            source_prob = tf.reshape(source_prob, shape=[self.batch_size, -1, 1])
        with tf.variable_scope('trigger_moudle'):
            # batch,step,dim # batch*step,2
            lstm_outputs_trigger, scores_tc_trigger = _trigger_model(token_lstm_input_drop, self.length)
            soft_scores = tf.nn.softmax(scores_tc_trigger)
            _, trigger_prob = tf.split(soft_scores, 2, axis=1)
            trigger_prob = tf.reshape(trigger_prob, shape=[self.batch_size, -1, 1])

        outputs_sou = None
        outputs_tri = None
        # outputs_tar = None
        if not self.FLAGS.is_interact:
            outputs_sou, outputs_tri = _no_interact_layer(lstm_outputs_source, lstm_outputs_trigger)
        if self.FLAGS.is_interact:
            outputs_sou, outputs_tri = _interact_layer(lstm_outputs_source, lstm_outputs_trigger,source_prob,trigger_prob)

        with tf.variable_scope('source_feedforward_before_crf'):
            source_scores =  _common_layer(outputs_sou, self.task1_num_classess)
        with tf.variable_scope('trigger_feedforward_before_crf'):
            trigger_scores = _common_layer(outputs_tri, self.task2_num_classess)
        # batchsize*step, num_classess
        source_unary_scores =  tf.reshape(source_scores,shape=[tf.shape(self.input_token_indices)[0], -1, self.task1_num_classess])
        trigger_unary_scores = tf.reshape(trigger_scores, shape=[tf.shape(self.input_token_indices)[0], -1, self.task2_num_classess])
        # target_unary_scores = tf.reshape(target_scores,shape=[tf.shape(self.input_token_indices)[0], -1, self.num_classess])

        # if self.FLAGS.crf_interact:
        #     sp_scores_interact,ps_scores_interact = _crf_interact(sp_unary_scores,ps_unary_scores)
        with tf.variable_scope('source_crf'):
            log_likelihood1, self.transition_params1, self.unary_scores1 = self._crf_layer(source_unary_scores, self.length, self.y_entitys,self.task1_num_classess)
            self.source_loss = tf.reduce_mean(-log_likelihood1)
        with tf.variable_scope('trigger_crf'):
            log_likelihood2, self.transition_params2, self.unary_scores2 = self._crf_layer(trigger_unary_scores, self.length, self.y_triggers,self.task2_num_classess)
            self.trigger_loss = tf.reduce_mean(-log_likelihood2)
        with tf.variable_scope('loss'):
            #2分loss
            self.union_loss = self.source_loss + self.trigger_loss
            if self.FLAGS.is_interact:
                y_sources_tc = tf.reshape(self.y_entitys_tc, shape=[-1])
                y_triggers_tc = tf.reshape(self.y_triggers_tc, shape=[-1])
                # y_targets_tc = tf.reshape(self.y_targets_tc, shape=[-1])
                source_tc_loss = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=y_sources_tc,
                                                                                logits=scores_tc_source,
                                                                                name='source_tc_loss')
                trigger_tc_loss = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=y_triggers_tc,
                                                                                 logits=scores_tc_trigger,
                                                                                 name='trigger_tc_loss')
                # target_tc_loss = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=y_targets_tc,
                #                                                                 logits=scores_tc_target,
                #                                                                 name='target_tc_loss')
                source_tc_loss = tf.reduce_mean(source_tc_loss)
                trigger_tc_loss = tf.reduce_mean(trigger_tc_loss)
                # target_tc_loss = tf.reduce_mean(target_tc_loss)
                self.source_loss += source_tc_loss
                self.trigger_loss += trigger_tc_loss
                self.union_loss = self.union_loss + source_tc_loss + trigger_tc_loss
            self.train_op_sou = self.define_training_procedure(self.source_loss, self.sou_global_step)
            self.train_op_tri = self.define_training_procedure(self.trigger_loss, self.tri_global_step)
            self.train_op_uon = self.define_training_procedure(self.union_loss,self.sou_global_step)


    def _crf_layer(self, unary_scores, seq_len, y, num_class):
        small_score = -1000.0
        large_score = 0.0
        # batchsize , step , dim
        # self.unary_scores = tf.reshape(self.unary_scores, shape=[tf.shape(self.input_token_indices)[0], -1, self.num_classess])
        #
        # # num_steps
        # sequence_length = tf.reduce_sum(tf.sign(self.input_token_indices), 1)
        # sequence_length = tf.cast(sequence_length, tf.int32)
        # # batchsize, num_steps ,num_classes + 2
        _batchsize = tf.shape(self.input_token_indices)[0]
        batch_max_step = tf.shape(self.input_token_indices)[1]
        unary_scores_with_start_and_end = tf.concat(
            [unary_scores, tf.tile(tf.constant(small_score, shape=[1, 1, 2]), [_batchsize, batch_max_step, 1])],
            -1)
        # num_classes + 2
        start_unary_scores = tf.constant([[small_score] * num_class + [large_score, small_score]],
                                         shape=[1, 1, num_class + 2])
        start_unary_scores = tf.tile(start_unary_scores, [_batchsize, 1, 1])
        # num_classes + 2
        end_unary_scores = tf.constant([[small_score] * num_class + [small_score, large_score]],
                                       shape=[1, 1, num_class + 2])
        end_unary_scores = tf.tile(end_unary_scores, [_batchsize, 1, 1])
        # batchsize, seq + 2, num_classes + 2
        unary_scores = tf.concat([start_unary_scores, unary_scores_with_start_and_end, end_unary_scores], 1)
        start_index = num_class
        end_index = num_class + 1

        input_label_indices_flat_with_start_and_end = tf.concat(
            [tf.tile(tf.constant(start_index, shape=[1, 1]), [_batchsize, 1]), y,
             tf.tile(tf.constant(end_index, shape=[1, 1]), [_batchsize, 1])], 1)
        # Apply CRF layer
        # sequence_length = tf.shape(self.unary_scores)[0]
        # sequence_lengths = tf.expand_dims(sequence_length, axis=0, name='sequence_lengths')
        transition_parameters = tf.get_variable(
            "transitions",
            shape=[num_class + 2, num_class + 2],
            initializer=self.initializer)
        # self.unary_scores_expanded = tf.expand_dims(self.unary_scores,axis=0,name='unary_scores_expanded')
        # input_label_indices_flat_batch = tf.expand_dims(input_label_indices_flat_with_start_and_end,axis=0,name='targets_expanded')
        log_likelihood, _ = tf.contrib.crf.crf_log_likelihood(
            unary_scores, input_label_indices_flat_with_start_and_end, seq_len,
            transition_params=transition_parameters)

        return log_likelihood, transition_parameters, unary_scores

    def define_training_procedure(self, loss, global_step):
        # Define training procedure
        # self.global_step = tf.Variable(0, name='global_step', trainable=False)
        if self.FLAGS.optimizer == 'adam':
            self.optimizer = tf.train.AdamOptimizer(self.FLAGS.init_lr)
        elif self.FLAGS.optimizer == 'sgd':
            self.optimizer = tf.train.GradientDescentOptimizer(self.FLAGS.init_lr)
        elif self.FLAGS.optimizer == 'adadelta':
            self.optimizer = tf.train.AdadeltaOptimizer(self.FLAGS.init_lr)
        else:
            raise ValueError('The lr_method parameter must be either adadelta, adam or sgd.')

        grads_and_vars = self.optimizer.compute_gradients(loss)
        if self.FLAGS.gradient_clipping_value:
            for i, (grad, var) in enumerate(grads_and_vars):
                if grad is not None:
                    grads_and_vars[i] = (
                    tf.clip_by_value(grad, -self.FLAGS.gradient_clipping_value, self.FLAGS.gradient_clipping_value),
                    var)

        # By defining a global_step variable and passing it to the optimizer we allow TensorFlow handle the counting of training steps for us.
        # The global step will be automatically incremented by one every time you execute train_op.
        train_op = self.optimizer.apply_gradients(grads_and_vars, global_step=global_step)
        return train_op


    def train_model_source(self,sess,x_batch,y_batch):

        feed_dict = {
            self.input_token_indices: x_batch[0],
            self.input_postag_indices: x_batch[1],
            self.input_suffix_indices:x_batch[2],
            self.input_token_character_indices: x_batch[3],
            self.y_entitys: y_batch[0],
            # self.y_triggers: y_batch[1],
            # self.y_targets:y_batch[2],
            self.y_entitys_tc: y_batch[2],
            # self.y_triggers_tc: y_batch[3],
            # self.y_targets_tc: y_batch[5],
            self.keep_dropout: 0.5}
        feed_dict[self.batch_size] = len(x_batch[0])
        _, loss_train, global_step \
            = sess.run([
            self.train_op_sou,
            self.source_loss,
            self.sou_global_step
        ],
            feed_dict=feed_dict)
        return global_step, loss_train
        pass
    def train_model_trigger(self,sess,x_batch,y_batch):
        token_indices_train_batch = x_batch
        # tys_sou, tys_tri, tys_tar = y_batch
        feed_dict = {
            self.input_token_indices: x_batch[0],
            self.input_postag_indices: x_batch[1],
            self.input_suffix_indices: x_batch[2],
            self.input_token_character_indices: x_batch[3],
            # self.y_entitys: y_batch[0],
            self.y_triggers: y_batch[1],
            # self.y_targets:y_batch[2],
            # self.y_entitys_tc: y_batch[2],
            self.y_triggers_tc: y_batch[3],
            # self.y_targets_tc: y_batch[5],
            self.keep_dropout: 0.5}
        feed_dict[self.batch_size] = len(x_batch[0])
        _, loss_train, global_step \
            = sess.run([
            self.train_op_tri,
            self.trigger_loss,
            self.tri_global_step
        ],
            feed_dict=feed_dict)
        return global_step, loss_train
        pass

    def train_model_union(self, sess, x_batch, y_batch, task_op, loss):
        # token_indices_train_batch = x_batch
        # tys_sou, tys_tri, tys_tar = y_batch
        feed_dict = {
                    self.input_token_indices: x_batch[0],
                    self.input_postag_indices: x_batch[1],
                    self.input_suffix_indices: x_batch[2],
                    self.input_token_character_indices: x_batch[3],
                     self.y_entitys:y_batch[0],
                     self.y_triggers:y_batch[1],
                     # self.y_targets:y_batch[2],
                     self.y_entitys_tc: y_batch[2],
                     self.y_triggers_tc: y_batch[3],
                     # self.y_targets_tc: y_batch[5],
                     self.keep_dropout: 0.5}
        feed_dict[self.batch_size] = len(x_batch[0])
        _, loss_train, global_step \
            = sess.run([
            task_op,
            loss,
            self.sou_global_step
        ],
            feed_dict=feed_dict)
        return global_step, loss_train

    def inference(self, sess, x_eval,y_eval):
        source_res = []
        target_res = []
        trigger_res = []
        all_loss = []

        for sample_index in range(len(x_eval[0])):
            x_eval_batch = [wxs[sample_index] for wxs in x_eval]
            y_eval_batch = [tys[sample_index] for tys in y_eval]
            feed_dict = {
                        self.input_token_indices: [x_eval_batch[0]],
                        self.input_postag_indices: [x_eval_batch[1]],
                        self.input_suffix_indices: [x_eval_batch[2]],
                         self.y_entitys: [y_eval_batch[0]],
                         self.y_triggers: [y_eval_batch[1]],
                         # self.y_targets: [y_eval_batch[2]],
                         self.y_entitys_tc: [y_eval_batch[2]],
                         self.y_triggers_tc: [y_eval_batch[3]],
                         # self.y_targets_tc: [y_eval_batch[5]],
                         self.keep_dropout: 1}
            feed_dict[self.batch_size] = 1
            unary_score1, unary_score2,test_seq_len, transMatrix1, transMatrix2,loss = sess.run(
                [self.unary_scores1,
                 self.unary_scores2,
                 # self.unary_scores3,
                 self.length,
                 self.transition_params1,
                 self.transition_params2,
                 # self.transition_params3,
                 self.union_loss],
                feed_dict=feed_dict
            )
            source_res.extend(self.viterbi_decode_batch(unary_score1, test_seq_len, transMatrix1))
            trigger_res.extend(self.viterbi_decode_batch(unary_score2, test_seq_len, transMatrix2))
            # target_res.extend(self.viterbi_decode_batch(unary_score3, test_seq_len, transMatrix3))
            all_loss.append(loss)
        return source_res,trigger_res, np.mean(np.array(all_loss))

    def inference_single(self, sess, x,y ):
        source_res = []
        target_res = []
        trigger_res = []



        x_list = [wxs for wxs in x]
        y_list = [tys for tys in y]
        feed_dict = {
                        self.input_token_indices: [x_list[0]],
                        self.input_postag_indices: [x_list[1]],
                        self.input_suffix_indices: [x_list[2]],
                         self.y_entitys: [y_list[0]],
                         self.y_triggers: [y_list[1]],
                         # self.y_targets: [y_eval_batch[2]],
                         self.y_entitys_tc: [y_eval_batch[2]],
                         self.y_triggers_tc: [y_eval_batch[3]],
                         # self.y_targets_tc: [y_eval_batch[5]],
                         self.keep_dropout: 1}
            feed_dict[self.batch_size] = 1
            unary_score1, unary_score2,test_seq_len, transMatrix1, transMatrix2,loss = sess.run(
                [self.unary_scores1,
                 self.unary_scores2,
                 # self.unary_scores3,
                 self.length,
                 self.transition_params1,
                 self.transition_params2,
                 # self.transition_params3,
                 self.union_loss],
                feed_dict=feed_dict
            )
            source_res.extend(self.viterbi_decode_batch(unary_score1, test_seq_len, transMatrix1))
            trigger_res.extend(self.viterbi_decode_batch(unary_score2, test_seq_len, transMatrix2))
            # target_res.extend(self.viterbi_decode_batch(unary_score3, test_seq_len, transMatrix3))
            all_loss.append(loss)
        return source_res,trigger_res, np.mean(np.array(all_loss))

    def nn_decode_batch(self, unary_scores, test_seq_len):
        # unary_scores = [batch_size,num_steps,num_classes]
        # return  list: [batch_size,seq_len]

        y_preds = []
        for tf_unary_scores_, seq_len_ in zip(unary_scores, test_seq_len):
            tf_unary_scores_ = tf_unary_scores_[:seq_len_]
            y_pred = []
            for j in range(len(tf_unary_scores_)):
                id = np.where(tf_unary_scores_[j] == np.max(tf_unary_scores_[j]))
                y_pred.append(id[0][0])
            y_preds.append(y_pred)
        return y_preds

    def viterbi_decode_batch(self, unary_scores, test_seq_len, transMatrix):
        # unary_scores = [batch_size,num_steps,num_classes]
        # return  list: [batch_size,seq_len]
        y_pred = []
        for tf_unary_scores_, seq_len_ in zip(unary_scores, test_seq_len):
            # tf_unary_scores_ = tf_unary_scores_[:seq_len_]
            # viterbi_sequence = [num_steps]
            viterbi_sequence, _ = tf.contrib.crf.viterbi_decode(
                tf_unary_scores_, transMatrix)
            y_pred.append(viterbi_sequence[1:-1])
            # y_gold.append(y_)
        return y_pred


