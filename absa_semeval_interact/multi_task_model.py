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
from MyLstmCell import MyLSTMCell
tf.set_random_seed(1024)
def bidirectional_LSTM( input, hidden_state_dimension, initializer, sequence_length=None, output_sequence=True, cell_choose = None):
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
                if cell_choose == None:
                    lstm_cell[direction] = tf.contrib.rnn.CoupledInputForgetGateLSTMCell(hidden_state_dimension, forget_bias=1.0,initializer=initializer, state_is_tuple=True)
                else:
                    lstm_cell[direction] = MyLSTMCell(hidden_state_dimension,initializer=initializer)
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
                                                                initial_state_bw=initial_state["backward"],
                                                                )
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
        self.num_classess = FLAGs.num_class
        self.num_corpus = FLAGs.num_corpus
        self.num_words = FLAGs.num_word
        self.token_emb_dim = FLAGs.token_edim
        self.num_postags = FLAGs.num_postag
        self.postag_emb_dim = FLAGs.postag_emb_dim
        self.num_hiddens = FLAGs.num_hidden
        self.token_max_len = FLAGs.token_max_len
        self.num_layers = 1
        self.pretrain_emb = FLAGs.pretrain_emb
        self.is_crf = FLAGs.use_crf
        self.num_units = 100
        self.if_l2 = False
        self.learning_rate = FLAGs.init_lr

        self.max_f1 = -1.0

        self.global_step = tf.Variable(0, name='share_global_step', trainable=False)

        self.batch_size = tf.placeholder(tf.int32, name='batch_size')

        self.input_token_indices = tf.placeholder(tf.int32, [None, None], name="input_token_indices")
        #b,s,s
        self.input_position_indices = tf.placeholder(tf.int32,[None,None,None],name="input_position_indices")
        self.input_token_character_indices = tf.placeholder(tf.int32, [None, None, self.token_max_len],name="input_token_indices")


        self.input_postag_indices = tf.placeholder(tf.int32, [None, None], name="input_postag_indices")

        self.y_targets = tf.placeholder(tf.int32, [None, None], name='y_targets')
        #two class

        self.y_opinions = tf.placeholder(tf.int32, [None, None], name='y_opinions')


        # if FLAGs.use_couple:
        self.couples = tf.placeholder(tf.int32, [None, None, None],name='couples')

        self.keep_dropout = tf.placeholder(dtype=tf.float32, name='keep_dropout')

        self.initializer = tf.contrib.layers.xavier_initializer(seed = 2)
        self.length = tf.reduce_sum(tf.sign(self.input_token_indices), 1)
        self.length = tf.to_int32(self.length)
        max_seq_len = tf.shape(self.input_token_indices)[1]

        #


        def _opi_lstm_layer1(input_data, seq_len):
            # batchsize, step, dim
            # print(input_data.get_shape()[-1].value)
            with tf.variable_scope("bilstm_layer_1"):
                lstm_outputs_flat = bidirectional_LSTM(input_data, self.num_hiddens, self.initializer, sequence_length=seq_len, output_sequence=True)
            # batchsize*step , 2*dim
            lstm_outputs = tf.reshape(lstm_outputs_flat, shape=[-1, lstm_outputs_flat.get_shape()[-1].value])
            with tf.variable_scope("feedforward_tc"):
                scores_tc = _common_layer(lstm_outputs, 2)

            return lstm_outputs_flat,scores_tc




        def _ent_lstm_layer1(input_data, seq_len):
            # if step:
            #     input_data = tf.concat([input_data, share_outputs], axis=-1)
            with tf.variable_scope("bilstm_layer_1"):
                lstm_outputs_flat = bidirectional_LSTM(input_data, self.num_hiddens, self.initializer, sequence_length=seq_len, output_sequence=True)
            lstm_outputs = tf.reshape(lstm_outputs_flat, shape=[-1, lstm_outputs_flat.get_shape()[-1].value])
            with tf.variable_scope("feedforward_tc"):
                scores_tc = _common_layer(lstm_outputs, 2)


            return lstm_outputs_flat,scores_tc




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

        def gate_ineract_layer(lstm_outputs_opi,lstm_outputs_ent):
            with tf.variable_scope('ent_opi_interact_layer'):
                # b,2l,2h
                ent_tri_concat = tf.concat([lstm_outputs_ent, lstm_outputs_opi], axis=2)

                ##b,l,2h
                ent_lstm2_outputs = bidirectional_LSTM(ent_tri_concat, self.num_hiddens, self.initializer,
                                                       sequence_length=self.length, output_sequence=True,
                                                       cell_choose=True)

            with tf.variable_scope('opi_ent_interact_layer'):
                opi_ent_concat = tf.concat([lstm_outputs_opi, lstm_outputs_ent], axis=2)
                # b,l,2h
                opi_lstm2_outputs = bidirectional_LSTM(opi_ent_concat, self.num_hiddens, self.initializer,
                                                       sequence_length=self.length, output_sequence=True,
                                                       cell_choose=True)

            interact_outputs_ent = tf.reshape(ent_lstm2_outputs, shape=[-1, ent_lstm2_outputs.get_shape()[-1].value])
            interact_outputs_opi = tf.reshape(opi_lstm2_outputs, shape=[-1, opi_lstm2_outputs.get_shape()[-1].value])
            # lstm_outputs_tar = tf.reshape(lstm_outputs_tar,shape=[-1, lstm_outputs_tar.get_shape()[-1].value])
            with tf.variable_scope("ent_feedforward_after_lstm"):
                outputs_ent = _common_layer(interact_outputs_ent, 2 * self.num_hiddens, activity=tf.nn.tanh)
            with tf.variable_scope("opi_feedforward_after_lstm"):
                outputs_opi = _common_layer(interact_outputs_opi, 2 * self.num_hiddens, activity=tf.nn.tanh)
            return outputs_ent,outputs_opi

        def _no_interact_layer(lstm_outputs_opi,lstm_outputs_ent):
            with tf.variable_scope("ent_lstm_layer_2"):
                ent_lstm_outputs = bidirectional_LSTM(lstm_outputs_ent, self.num_hiddens, self.initializer, sequence_length=self.length, output_sequence=True)
            with tf.variable_scope("opi_lstm_layer_2"):
                opi_lstm_outputs = bidirectional_LSTM(lstm_outputs_opi, self.num_hiddens, self.initializer, sequence_length=self.length, output_sequence=True)

            ent_lstm_outputs = tf.reshape(ent_lstm_outputs,shape=[-1, ent_lstm_outputs.get_shape()[-1].value])
            opi_lstm_outputs = tf.reshape(opi_lstm_outputs,shape=[-1, opi_lstm_outputs.get_shape()[-1].value])

            with tf.variable_scope("ent_feedforward_after_lstm"):
                outputs_ent = _common_layer(ent_lstm_outputs, 2 * self.num_hiddens, activity=tf.nn.tanh)
            with tf.variable_scope("opi_feedforward_after_lstm"):
                outputs_opi = _common_layer(opi_lstm_outputs, 2 * self.num_hiddens, activity=tf.nn.tanh)
            return outputs_ent,outputs_opi

        def _interact_layer(lstm_outputs_opi,opword_prob,lstm_outputs_ent,target_prob):
            with tf.variable_scope("ent_lstm_layer_2"):
                ent_lstm_outputs = bidirectional_LSTM(lstm_outputs_ent, self.num_hiddens, self.initializer,
                                                      sequence_length=self.length, output_sequence=True)
            with tf.variable_scope("opi_lstm_layer_2"):
                opi_lstm_outputs = bidirectional_LSTM(lstm_outputs_opi, self.num_hiddens, self.initializer,
                                                      sequence_length=self.length, output_sequence=True)
            with tf.variable_scope('interact'):
                left_values, right_values, cur_values = _compute_interact_(ent_lstm_outputs,target_prob)
                lstm_outputs_opi_flat = tf.concat([opi_lstm_outputs,left_values,right_values],axis=-1)
                left_values, right_values, cur_values = _compute_interact_(opi_lstm_outputs,opword_prob)
                lstm_outputs_ent_flat = tf.concat([ent_lstm_outputs,left_values,right_values],axis=-1)
            lstm_outputs_opi_interacted = tf.reshape(lstm_outputs_opi_flat, shape=[-1, lstm_outputs_opi_flat.get_shape()[-1].value])
            lstm_outputs_ent_interacted = tf.reshape(lstm_outputs_ent_flat, shape=[-1, lstm_outputs_ent_flat.get_shape()[-1].value])
            with tf.variable_scope("ent_feedforward_after_lstm"):
                outputs_ent = _common_layer(lstm_outputs_ent_interacted, 2 * self.num_hiddens, activity=tf.nn.tanh)
            with tf.variable_scope("opi_feedforward_after_lstm"):
                outputs_opi = _common_layer(lstm_outputs_opi_interacted, 2 * self.num_hiddens, activity=tf.nn.tanh)
            return outputs_ent,outputs_opi

        def _inter_att_layer(lstm_outputs_opi,prob_OP,lstm_outputs_ent,prob_TA,use_prob=True):
            #query
            #keys
            #values
            with tf.variable_scope("ent_lstm_layer_2"):
                ent_lstm_outputs = bidirectional_LSTM(lstm_outputs_ent, self.num_hiddens, self.initializer,
                                                      sequence_length=self.length, output_sequence=True)
            with tf.variable_scope("opi_lstm_layer_2"):
                opi_lstm_outputs = bidirectional_LSTM(lstm_outputs_opi, self.num_hiddens, self.initializer,
                                                      sequence_length=self.length, output_sequence=True)
            with tf.variable_scope('interact'):
                left_TA_values, right_TA_values, cur_values = _compute_interact_(ent_lstm_outputs,prob_TA)
                # lstm_outputs_s_p_flat = tf.concat([lstm_outputs_s,left_values,right_values],axis=-1)
                left_OP_values, right_OP_values, cur_values = _compute_interact_(opi_lstm_outputs,prob_OP)
                # lstm_outputs_p_s_flat = tf.concat([lstm_outputs_p,left_values,right_values],axis=-1)
            window = 3
            # if use_prob:
            #     lstm_outputs_prob_OP = lstm_outputs_OP * prob_OP
            #     lstm_outputs_prob_TA = lstm_outputs_TA * prob_TA
            with tf.variable_scope('attention_project'):
                op_proj_w = tf.get_variable('op_proj_w',shape=[opi_lstm_outputs.get_shape()[-1],self.num_units])
                lstm_outputs_proj_OP = tf.tensordot(opi_lstm_outputs,op_proj_w,axes=1)
                ta_proj_w = tf.get_variable('ta_proj_w', shape=[ent_lstm_outputs.get_shape()[-1], self.num_units])
                lstm_outputs_proj_TA = tf.tensordot(ent_lstm_outputs, ta_proj_w, axes=1)
            b = tf.shape(lstm_outputs_opi)[0]
            num_step = tf.shape(lstm_outputs_opi)[1]
            with tf.variable_scope('att_OP'):
                input_ta = tf.TensorArray(size=num_step,dtype=tf.float32)
                output_ta = tf.TensorArray(size=num_step,dtype=tf.float32)
                lstm_op_trans = tf.transpose(lstm_outputs_proj_OP,[1,0,2])
                input_ta = input_ta.unstack(lstm_op_trans)
                def _body(time,output_ta_t):
                    uid1 = tf.cond(time < window,lambda : tf.constant(0),lambda  : time - window)
                    uid2 = tf.cond(time+window>=num_step-1,lambda :num_step-1,lambda : time+ window)
                    tmp = [0] * 3
                    tmp[0] = tf.ones([b, time], tf.float32)
                    tmp[1] = tf.zeros([b, 1], tf.float32)
                    tmp[2] = tf.ones([b, num_step - 1 - time], tf.float32)
                    score_mask = tf.concat(tmp, axis=1)
                    s,context = _att_layer(input_ta.read(time), lstm_outputs_proj_TA, ent_lstm_outputs, score_mask)
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
                lstm_outputs_opi_flat = tf.concat([opi_lstm_outputs, output_final,left_TA_values,right_TA_values], axis=-1)
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
                    s,context = _att_layer(input_ta.read(time), lstm_outputs_proj_OP, opi_lstm_outputs, score_mask)
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
                lstm_outputs_ent_flat = tf.concat([ent_lstm_outputs, output_final,left_OP_values,right_OP_values], axis=-1)
            lstm_outputs_opi_ineracted = tf.reshape(lstm_outputs_opi_flat, shape=[-1, 4 * opi_lstm_outputs.get_shape()[-1].value])
            lstm_outputs_ent_ineracted = tf.reshape(lstm_outputs_ent_flat, shape=[-1, 4 * ent_lstm_outputs.get_shape()[-1].value])

            with tf.variable_scope("ent_feedforward_after_lstm"):
                outputs_ent = _common_layer(lstm_outputs_ent_ineracted, 2 * self.num_hiddens, activity=tf.nn.tanh)
            with tf.variable_scope("opi_feedforward_after_lstm"):
                outputs_opi = _common_layer(lstm_outputs_opi_ineracted, 2 * self.num_hiddens, activity=tf.nn.tanh)
            return outputs_ent, outputs_opi
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
                self.token_embedding_weights = tf.get_variable('token_embedding_weights', [self.num_words, self.token_emb_dim])

            self.postag_embdding_weights = tf.get_variable('postag_embdding_weights',[self.num_postags, self.postag_emb_dim])
            self.poisiton_embedding_weights = tf.get_variable('poisiton_embedding_weights',[self.num_postags, self.postag_emb_dim])
            embedded_tokens = tf.nn.embedding_lookup(self.token_embedding_weights, self.input_token_indices)
            embedded_postags = tf.nn.embedding_lookup(self.postag_embdding_weights, self.input_postag_indices)
            #b,s,s,d
            embedded_positions = tf.nn.embedding_lookup(self.poisiton_embedding_weights,self.input_position_indices)
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
        # Concatenate character LSTM outputs and token embeddings
        print(FLAGs.use_character_lstm)
        if FLAGs.use_character_lstm:
            with tf.variable_scope("concatenate_token_and_character_vectors"):
                temp = []
                # POS
                if FLAGs.use_pos:
                    temp.append(embedded_postags)
                token_lstm_input = tf.concat([character_lstm_output, embedded_tokens] + temp, axis=2,name='token_lstm_input')

        else:
            token_lstm_input = embedded_tokens
        # with tf.variable_scope('add_dropout') as vs:
        #     # batchsize, step, dim_n
        #     token_lstm_input_drop = tf.nn.dropout(token_lstm_input, self.keep_dropout, seed=2,name='token_lstm_input_drop')
        with tf.variable_scope("opinion_layer"):
            # 经过共享层句子的表示
            token_lstm_input_drop_1 = tf.nn.dropout(token_lstm_input, self.keep_dropout, seed=1,name='token_lstm_input_drop_1')

            lstm_outputs_opi, scores_tc_opinion = _opi_lstm_layer1(token_lstm_input_drop_1, self.length)
            soft_scores = tf.nn.softmax(scores_tc_opinion)
            _, opword_prob = tf.split(soft_scores, 2, axis=1)
            opword_prob = tf.reshape(opword_prob, shape=[self.batch_size, -1, 1])
        with tf.variable_scope('entity_layer'):
            # batch,step,dim # batch*step,2
            token_lstm_input_drop_2 = tf.nn.dropout(token_lstm_input, self.keep_dropout, seed=2,name='token_lstm_input_drop_2')

            lstm_outputs_ent, scores_tc_target = _ent_lstm_layer1(token_lstm_input_drop_2, self.length)
            soft_scores = tf.nn.softmax(scores_tc_target)
            _, target_prob = tf.split(soft_scores, 2, axis=1)
            target_prob = tf.reshape(target_prob, shape=[self.batch_size, -1, 1])



        outputs_ent, outputs_opi = None, None

        # if self.FLAGS.use_label_distribute and not self.FLAGS.use_attention:
        #     outputs_ent, outputs_opi = _interact_layer(lstm_outputs_opi,opword_prob,lstm_outputs_ent,target_prob)

        if self.FLAGS.multi_task:
            # outputs_ent, outputs_opi = _inter_att_layer(lstm_outputs_opi, opword_prob, lstm_outputs_ent, target_prob)
            rep_mask = tf.sequence_mask(self.length, max_seq_len, tf.bool)
            new_opi_rep, new_ent_rep,self.opi_att_scores,self.ent_att_scores = mdatt.psan(lstm_outputs_opi, lstm_outputs_ent, rep_mask,initializer=self.initializer,
                                                                                          if_fusion_gate=FLAGs.fusion_gate,emb_positons = embedded_positions)
            # from rel_network import rel_network
            # combine_rep = rel_network(new_opi_rep, lstm_outputs_ent)
            # with tf.variable_scope('rel_ff'):
            #     rel_logits = _common_layer(combine_rep, 2)
            with tf.variable_scope('ent_opi_interact_layer'):
                ent_lstm2_outputs = bidirectional_LSTM(new_ent_rep, self.num_hiddens, self.initializer,sequence_length=self.length, output_sequence=True)
                # ent_lstm2_outputs = new_ent_rep
            with tf.variable_scope('opi_ent_interact_layer'):
                opi_lstm2_outputs = bidirectional_LSTM(new_opi_rep, self.num_hiddens, self.initializer,sequence_length=self.length, output_sequence=True)
                # opi_lstm2_outputs = new_opi_rep
            lstm_outputs_ent = tf.reshape(ent_lstm2_outputs, shape=[-1, ent_lstm2_outputs.get_shape()[-1].value])
            lstm_outputs_opi = tf.reshape(opi_lstm2_outputs, shape=[-1, opi_lstm2_outputs.get_shape()[-1].value])
            with tf.variable_scope("ent_feedforward_after_lstm"):
                outputs_ent = _common_layer(lstm_outputs_ent, 2 * self.num_hiddens, activity=tf.nn.tanh)
            with tf.variable_scope("opi_feedforward_after_lstm"):
                outputs_opi = _common_layer(lstm_outputs_opi, 2 * self.num_hiddens, activity=tf.nn.tanh)
        with tf.variable_scope('ent_feedforward_before_crf'):
            ent_scores = _common_layer(outputs_ent, self.num_classess)
        with tf.variable_scope('opi_feedforward_before_crf'):
            opi_scores = _common_layer(outputs_opi, self.num_classess)



        # if self.FLAGS.crf_interact:
        #     sp_scores_interact,ps_scores_interact = _crf_interact(sp_unary_scores,ps_unary_scores)
        if self.is_crf:
            # batchsize*step, num_classess
            ent_unary_scores = tf.reshape(ent_scores, shape=[tf.shape(self.input_token_indices)[0], -1, self.num_classess])
            opi_unary_scores = tf.reshape(opi_scores,  shape=[tf.shape(self.input_token_indices)[0], -1, self.num_classess])
            with tf.variable_scope('opi_crf'):
                log_likelihood1, self.transition_params1, self.unary_scores1 = self._crf_layer(opi_unary_scores, self.length, self.y_opinions)
                self.opi_loss = tf.reduce_mean(-log_likelihood1)
            with tf.variable_scope('ent_crf'):
                log_likelihood2, self.transition_params2, self.unary_scores2 = self._crf_layer(ent_unary_scores, self.length, self.y_targets)
                self.ent_loss = tf.reduce_mean(-log_likelihood2)
        else:
            targets_reshaped = tf.reshape(self.y_targets,shape=[-1])
            opwords_reshaped = tf.reshape(self.y_opinions,shape=[-1])
            self.ent_predictions = tf.reshape(tf.argmax(ent_scores, axis=-1),shape=[tf.shape(self.input_token_indices)[0], -1])
            self.opi_predictions = tf.reshape(tf.argmax(opi_scores, axis=-1),shape=[tf.shape(self.input_token_indices)[0], -1])

            self.ent_loss = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=targets_reshaped,logits=ent_scores,name='ent_loss')
            self.ent_loss = tf.reduce_mean(self.ent_loss)
            self.opi_loss = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=opwords_reshaped, logits=opi_scores,name='opi_loss')
            self.opi_loss = tf.reduce_mean(self.opi_loss)
        with tf.variable_scope('loss'):
            #2分loss
            self.union_loss = self.ent_loss + FLAGs.w * self.opi_loss
            if FLAGs.use_couple:
                size = tf.cast(tf.reduce_sum(tf.sign(self.couples)),tf.float32)
                self.p1 = tf.reduce_sum(tf.multiply(tf.log(self.opi_att_scores + 1e-7),tf.cast(self.couples,tf.float32)))
                self.p2 = tf.reduce_sum(tf.multiply(tf.log(self.ent_att_scores + 1e-7),tf.cast(self.couples,tf.float32)))

                # ent_couples_flat = tf.reshape(self.couples,shape=[-1])
                # opi_couples_flat = tf.reshape(tf.transpose(self.couples,[0,2,1]),shape=[-1])
                # opi_att_scores = tf.reshape(self.opi_att_scores,shape=[-1,1])
                # opi_att_scores = tf.concat([1 - opi_att_scores,opi_att_scores],axis=-1)
                # ent_att_scores = tf.reshape(self.ent_att_scores,shape=[-1,1])
                # ent_att_scores = tf.concat([1 - ent_att_scores, ent_att_scores], axis=-1)
                # opi_att_loss = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=opi_couples_flat,logits=opi_att_scores)
                # # tf.multiply(tf.cast(self.couples,tf.float32),opi_att_scores)
                # opi_att_loss = tf.reduce_mean(opi_att_loss)
                #
                # ent_att_loss = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=ent_couples_flat, logits=ent_att_scores)
                # # tf.multiply(tf.cast(self.couples,tf.float32),opi_att_scores)
                # ent_att_loss = tf.reduce_mean(ent_att_loss)

                self.union_loss = self.union_loss - FLAGs.w * (self.p1 + self.p2)

            self.train_op = self.define_training_procedure(self.union_loss, self.global_step)


    def _crf_layer(self, unary_scores, seq_len, y):
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
        start_unary_scores = tf.constant([[small_score] * self.num_classess + [large_score, small_score]],
                                         shape=[1, 1, self.num_classess + 2])
        start_unary_scores = tf.tile(start_unary_scores, [_batchsize, 1, 1])
        # num_classes + 2
        end_unary_scores = tf.constant([[small_score] * self.num_classess + [small_score, large_score]],
                                       shape=[1, 1, self.num_classess + 2])
        end_unary_scores = tf.tile(end_unary_scores, [_batchsize, 1, 1])
        # batchsize, seq + 2, num_classes + 2
        unary_scores = tf.concat([start_unary_scores, unary_scores_with_start_and_end, end_unary_scores], 1)
        start_index = self.num_classess
        end_index = self.num_classess + 1

        input_label_indices_flat_with_start_and_end = tf.concat(
            [tf.tile(tf.constant(start_index, shape=[1, 1]), [_batchsize, 1]), y,
             tf.tile(tf.constant(end_index, shape=[1, 1]), [_batchsize, 1])], 1)
        # Apply CRF layer
        # sequence_length = tf.shape(self.unary_scores)[0]
        # sequence_lengths = tf.expand_dims(sequence_length, axis=0, name='sequence_lengths')
        transition_parameters = tf.get_variable(
            "transitions",
            shape=[self.num_classess + 2, self.num_classess + 2],
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



    def train_model_union(self, sess, x_batch, y_batch, couples=None):
        token_indices_train_batch = x_batch
        y_targets_batch, y_opinions_batch = y_batch
        feed_dict = {
                     self.input_token_indices: token_indices_train_batch,
                     self.y_targets: y_targets_batch,
                     self.y_opinions:y_opinions_batch,
                     self.keep_dropout: 0.5}
        if self.FLAGS.use_couple:
            feed_dict[self.couples] = couples
        feed_dict[self.batch_size] = len(y_opinions_batch)
        _, loss_train, global_step \
            = sess.run([
            self.train_op,
            self.union_loss,
            self.global_step
        ],
            feed_dict=feed_dict)
        return global_step, loss_train

    def inference_no_crf(self,sess,x_eval,y_eval, couples=None):
        target_pred = []
        opword_pred = []
        all_loss = []
        ent_loss = []
        opi_loss = []
        all_ent_att_scores = []
        all_opi_att_scores = []
        input_token_indices_eval = x_eval
        y_targets_eval, y_opinions_eval = y_eval
        for sample_index in range(len(y_targets_eval)):
            input_token_indices_eval_batch = [input_token_indices_eval[sample_index]]

            y_targets_eval_batch = [y_targets_eval[sample_index]]
            y_opinions_eval_batch = [y_opinions_eval[sample_index]]

            feed_dict = {
                self.input_token_indices: input_token_indices_eval_batch,
                self.y_targets: y_targets_eval_batch,
                self.y_opinions: y_opinions_eval_batch,
                self.keep_dropout: 1}
            if self.FLAGS.use_couple:
                couples_eval_batch = [couples[sample_index]]
                feed_dict[self.couples] = couples_eval_batch
            feed_dict[self.batch_size] = len(y_opinions_eval_batch)
            ent_pred, opi_pred, test_seq_len, loss, op_att_score, en_att_score = sess.run(
                [self.ent_predictions,
                 self.opi_predictions,
                 self.length,
                 self.union_loss,
                 self.opi_att_scores,
                 self.ent_att_scores
                 ],
                feed_dict=feed_dict
            )
            for pred in opi_pred:
                opword_pred.append(list(pred))
            for pred in ent_pred:
                target_pred.append(list(pred))
            all_loss.append(loss)
            if self.FLAGS.use_couple:
                loss1,loss2 = sess.run([
                    self.p1,
                    self.p2,
                ],feed_dict=feed_dict)
                ent_loss.append(loss1)
                opi_loss.append(loss2)
            all_opi_att_scores.append(op_att_score[0])
            all_ent_att_scores.append(en_att_score[0])

        return opword_pred, target_pred, np.mean(np.array(all_loss)), all_ent_att_scores, all_opi_att_scores
        pass
    def inference_for_cpu(self, sess, x_eval, y_eval, couples=None):
        if self.is_crf:
            return self.inference_with_crf(sess, x_eval, y_eval, couples)
        else:
            return self.inference_no_crf(sess, x_eval, y_eval, couples)

    def inference_with_crf(self, sess, x_eval, y_eval, couples=None):

        target_pred = []
        opword_pred = []
        all_loss = []
        ent_loss = []
        opi_loss = []
        all_ent_att_scores = []
        all_opi_att_scores = []
        input_token_indices_eval = x_eval
        y_targets_eval, y_opinions_eval = y_eval
        for sample_index in range(len(y_targets_eval)):
            # start_index = itor * self.batch_size
            # end_index = min(start_index + self.batch_size, len(Xtest))

            input_token_indices_eval_batch = [input_token_indices_eval[sample_index]]

            y_targets_eval_batch = [y_targets_eval[sample_index]]
            y_opinions_eval_batch = [y_opinions_eval[sample_index]]

            feed_dict = {
                         self.input_token_indices: input_token_indices_eval_batch,

                         self.y_targets: y_targets_eval_batch,
                         self.y_opinions: y_opinions_eval_batch,


                         self.keep_dropout: 1}
            if self.FLAGS.use_couple:
                couples_eval_batch = [couples[sample_index]]
                feed_dict[self.couples] = couples_eval_batch
            feed_dict[self.batch_size] = len(y_opinions_eval_batch)
            unary_score1, unary_score2, test_seq_len, transMatrix1, transMatrix2, loss,op_att_score,en_att_score = sess.run(
                [self.unary_scores1,
                 self.unary_scores2,
                 self.length,
                 self.transition_params1,
                 self.transition_params2,
                 self.union_loss,
                 self.opi_att_scores,
                 self.ent_att_scores
            ],
                feed_dict=feed_dict
            )
            if self.FLAGS.use_couple:
                loss1,loss2 = sess.run([
                    self.p1,
                    self.p2,
                ],feed_dict=feed_dict)
                ent_loss.append(loss1)
                opi_loss.append(loss2)

            opword_pred.extend(self.viterbi_decode_batch(unary_score1, test_seq_len, transMatrix1))
            target_pred.extend(self.viterbi_decode_batch(unary_score2, test_seq_len, transMatrix2))
            all_loss.append(loss)

            all_opi_att_scores.append(op_att_score[0])
            all_ent_att_scores.append(en_att_score[0])

        return opword_pred, target_pred, np.mean(np.array(all_loss)),all_ent_att_scores, all_opi_att_scores

    def inference(self, sess, x_eval,y_eval):
        target_pred = []
        opword_pred = []
        all_loss = []
        ent_loss = []
        opi_loss = []
        # ent_tc_loss = []
        # opi_tc_loss = []
        input_token_indices_eval= x_eval
        y_targets_eval, y_opinions_eval = y_eval

        feed_dict = {
                         self.input_token_indices: input_token_indices_eval,
                         self.y_targets : y_targets_eval,
                         self.y_opinions: y_opinions_eval,
                         self.keep_dropout: 1}
        feed_dict[self.batch_size] = len(y_opinions_eval)
        unary_score1, unary_score2,test_seq_len, transMatrix1, transMatrix2, loss,loss1,loss2 = sess.run(
                [self.unary_scores1,
                 self.unary_scores2,
                 self.length,
                 self.transition_params1,
                 self.transition_params2,
                 self.union_loss,
                 self.ent_loss,
                 self.opi_loss,],
                feed_dict=feed_dict
            )
        opword_pred.extend(self.viterbi_decode_batch(unary_score1, test_seq_len, transMatrix1))
        target_pred.extend(self.viterbi_decode_batch(unary_score2, test_seq_len, transMatrix2))
        all_loss.append(loss)
        ent_loss.append(loss1)
        opi_loss.append(loss2)
        return opword_pred,target_pred, np.mean(np.array(all_loss)), np.mean(np.array(ent_loss)), np.mean(np.array(opi_loss)),


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

    def test(self, sess, x_eval,y_eval):
        target_pred = []
        opword_pred = []
        all_loss = []
        ent_loss = []
        opi_loss = []
        all_ent_att_scores = []
        all_opi_att_scores = []
        input_token_indices_eval = x_eval
        y_targets_eval, y_opinions_eval = y_eval
        for sample_index in range(len(y_targets_eval)):
            # start_index = itor * self.batch_size
            # end_index = min(start_index + self.batch_size, len(Xtest))

            input_token_indices_eval_batch = [input_token_indices_eval[sample_index]]

            y_targets_eval_batch = [y_targets_eval[sample_index]]
            y_opinions_eval_batch = [y_opinions_eval[sample_index]]

            feed_dict = {
                self.input_token_indices: input_token_indices_eval_batch,

                self.y_targets: y_targets_eval_batch,
                self.y_opinions: y_opinions_eval_batch,

                self.keep_dropout: 1}
            feed_dict[self.batch_size] = len(y_opinions_eval_batch)
            unary_score1, unary_score2, test_seq_len, transMatrix1, transMatrix2, loss, loss1, loss2, ent_scores, opi_scores = sess.run(
                [self.unary_scores1,
                 self.unary_scores2,
                 self.length,
                 self.transition_params1,
                 self.transition_params2,
                 self.union_loss,
                 self.ent_loss,
                 self.opi_loss,
                 self.ent_att_scores,
                 self.opi_att_scores
                 ],
                feed_dict=feed_dict
            )
            opword_pred.extend(self.viterbi_decode_batch(unary_score1, test_seq_len, transMatrix1))
            target_pred.extend(self.viterbi_decode_batch(unary_score2, test_seq_len, transMatrix2))
            all_loss.append(loss)
            ent_loss.append(loss1)
            opi_loss.append(loss2)
            all_ent_att_scores.append(ent_scores[0])
            all_opi_att_scores.append(opi_scores[0])

        return opword_pred, target_pred, np.mean(np.array(all_loss)), np.mean(np.array(ent_loss)), np.mean(
            np.array(opi_loss)), all_ent_att_scores, all_opi_att_scores

