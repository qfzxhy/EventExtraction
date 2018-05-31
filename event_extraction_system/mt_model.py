# -*- coding:utf-8 -*-
_author_ = 'qianf'
import tensorflow as tf
import codecs
# from tensorflow.models.rnn import rnn
import math
import os
import numpy as np
import heapq
import MDAtt as mdatt

import MDAtt as mdatt

# tf.set_random_seed(1024)
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


class MT_Model(object):
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
        self.input_postag_indices = tf.placeholder(tf.int32, [None, None], name="input_postag_indices")
        self.input_suffix_indices = tf.placeholder(tf.int32, [None, None], name="input_suffix_indices")
        self.input_token_character_indices = tf.placeholder(tf.int32, [None, None, self.token_max_len],name="input_token_indices")
        self.input_trigger_type_indices = tf.placeholder(tf.int32, [None, None],name="input_trigger_type_indices")

        self.y_triggers = tf.placeholder(tf.int32, [None, None], name='y_triggers')
        self.y_entitys = tf.placeholder(tf.int32, [None, None], name='y_entitys')




        self.keep_dropout = tf.placeholder(dtype=tf.float32, name='keep_dropout')

        self.initializer = tf.contrib.layers.xavier_initializer(seed=1)
        self.length = tf.reduce_sum(tf.sign(self.input_token_indices), 1)
        self.length = tf.to_int32(self.length)
        max_seq_len = tf.shape(self.input_token_indices)[1]

        def concat(tensors, output_size):
            tensor_concat = tf.concat(tensors,axis=-1)
            return _common_layer(tensor_concat,output_size, activity=tf.nn.tanh)
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

        with tf.variable_scope('embedding_layer') as vs:
            if self.pretrain_emb is not None:
                self.token_embedding_weights = tf.Variable(self.pretrain_emb, trainable=True,
                                                           name='token_embedding_weights', dtype=tf.float32)
            else:
                self.token_embedding_weights = tf.get_variable('token_embedding_weights',
                                                               [self.num_words, self.token_emb_dim],initializer=self.initializer)


            # self.trigger_type_weights = tf.get_variable('trigger_type_weights', [FLAGs.num_trigger_type, FLAGs.trigger_type_emb_dim],initializer=self.initializer)
            embedded_tokens = tf.nn.embedding_lookup(self.token_embedding_weights, self.input_token_indices)

            # embedded_trigger_types = tf.nn.embedding_lookup(self.trigger_type_weights, self.input_trigger_type_indices)
            embedded_trigger_types = tf.one_hot(self.input_trigger_type_indices, FLAGs.trigger_type_emb_dim, on_value=1.0, off_value=0.0,axis=-1)
        tmp_left = [embedded_tokens]
        tmp_right = []
        if FLAGs.use_suffix or FLAGs.use_postag:

            with tf.variable_scope('high_features'):
                postag_embdding_weights_left = tf.get_variable('postag_embdding_weights_left',
                                                               [self.num_postags, self.postag_emb_dim],
                                                               initializer=self.initializer)
                embedded_postags = tf.nn.embedding_lookup(postag_embdding_weights_left, self.input_postag_indices)
                suffix_embdding_weights = tf.get_variable('suffix_embdding_weights',
                                                          [FLAGs.num_suffix, FLAGs.suffix_emb_dim],
                                                          initializer=self.initializer)
                embedded_suffixs = tf.nn.embedding_lookup(suffix_embdding_weights, self.input_suffix_indices)
                # if FLAGs.use_character_lstm:
                #     tmp.append(character_lstm_output)
                if FLAGs.use_postag:
                    tmp_left.append(embedded_postags)
                if FLAGs.use_suffix:
                    tmp_left.append(embedded_suffixs)
                input_embbed = tf.concat(tmp_left, axis=-1, name='features')
                input_embbed = tf.reshape(input_embbed, shape=[-1, input_embbed.get_shape()[-1].value])
                input_embbed = _common_layer(input_embbed, self.num_hiddens, activity=tf.nn.tanh)
                input_embbed = tf.reshape(input_embbed, shape=[tf.shape(self.input_token_indices)[0],tf.shape(self.input_token_indices)[1],self.num_hiddens])
                # outputs_ent = tf.concat([outputs_ent, output_features], axis=-1)
            # with tf.variable_scope('tri_high_features'):
            #     postag_embdding_weights_right = tf.get_variable('postag_embdding_weights_right',
            #                                                     [self.num_postags, self.postag_emb_dim],
            #                                                     initializer=self.initializer)
            #     embedded_postags = tf.nn.embedding_lookup(postag_embdding_weights_right, self.input_postag_indices)
            #     suffix_embdding_weights = tf.get_variable('suffix_embdding_weights',
            #                                               [FLAGs.num_suffix, FLAGs.suffix_emb_dim],
            #                                               initializer=self.initializer)
            #     embedded_suffixs = tf.nn.embedding_lookup(suffix_embdding_weights, self.input_suffix_indices)
            #     # if FLAGs.use_character_lstm:
            #     #     tmp.append(character_lstm_output)
            #     if FLAGs.use_postag:
            #         tmp_right.append(embedded_postags)
            #         # tmp_right.append(embedded_trigger_types)
            #     if FLAGs.use_suffix:
            #         tmp_right.append(embedded_suffixs)
            #     input_feature = tf.concat(tmp_right, axis=-1, name='features')
            #     input_feature = tf.reshape(input_feature, shape=[-1, input_feature.get_shape()[-1].value])
            #     output_features = _common_layer(input_feature, self.num_hiddens, activity=tf.nn.tanh)
            #     outputs_tri = tf.concat([outputs_tri, output_features], axis=-1)

        # if FLAGs.use_character_lstm:
        #     # Character-level LSTM
        #     # Idea: reshape so that we have a tensor [number_of_token, max_token_length, token_embeddings_size], which we pass to the LSTM
        #     # Character embedding layer
        #     with tf.variable_scope("character_embedding"):
        #         self.character_embedding_weights = tf.get_variable(
        #             "character_embedding_weights",
        #             shape=[FLAGs.alphabet_size, FLAGs.character_embedding_dimension],
        #             initializer=self.initializer)
        #         input_character_indices = tf.reshape(self.input_token_character_indices,shape=[-1, self.token_max_len])
        #         embedded_characters = tf.nn.embedding_lookup(self.character_embedding_weights,
        #                                                      input_character_indices,
        #                                                      name='embedded_characters')
        #     self.input_token_lengths = tf.reduce_sum(tf.sign(input_character_indices), 1)
        #     self.input_token_lengths = tf.cast(self.input_token_lengths, tf.int32)
        #     # Character LSTM layer
        #     with tf.variable_scope('character_lstm') as vs:
        #         character_lstm_output = bidirectional_LSTM(embedded_characters,
        #                                                    FLAGs.character_lstm_hidden_state_dimension,
        #                                                    self.initializer,
        #                                                    sequence_length=self.input_token_lengths,
        #                                                    output_sequence=False)
        #         character_lstm_output = tf.reshape(character_lstm_output,shape=[self.batch_size,-1,2 * FLAGs.character_lstm_hidden_state_dimension])
        # Concatenate character LSTM outputs and token embeddings
        # print(FLAGs.use_character_lstm)
        # suffix_embs = tf.one_hot(self.input_suffix_indices, depth=7, dtype=tf.float32)
        # Concatenate character LSTM outputs and token embeddings
        # print(FLAGs.use_character_lstm)

        if FLAGs.use_suffix or FLAGs.use_postag:
            token_lstm_input = input_embbed
        else:
            token_lstm_input = embedded_tokens


        with tf.variable_scope("entity_moudle"):
            with tf.variable_scope('add_dropout') as vs:
                # batchsize, step, dim_n
                token_lstm_input_drop_1 = tf.nn.dropout(token_lstm_input, self.keep_dropout, name='token_lstm_input_drop_1')
            # 经过共享层句子的表示
            # lstm_outputs_source, scores_tc_source = _entity_model(token_lstm_input_drop, self.length)
            with tf.variable_scope("bilstm_layer_1"):
                #b,l,2h
                ent_lstm1_outputs = bidirectional_LSTM(token_lstm_input_drop_1, self.num_hiddens, self.initializer, sequence_length=self.length, output_sequence=True)
        with tf.variable_scope('trigger_moudle'):
            with tf.variable_scope('add_dropout') as vs:
                # batchsize, step, dim_n
                token_lstm_input_drop_2 = tf.nn.dropout(token_lstm_input, self.keep_dropout, name='token_lstm_input_drop_2')
            # batch,step,dim # batch*step,2
            with tf.variable_scope("bilstm_layer_1"):
                # b,l,2h
                tri_lstm1_outputs = bidirectional_LSTM(token_lstm_input_drop_2, self.num_hiddens, self.initializer, sequence_length=self.length, output_sequence=True)


        outputs_ent = None
        outputs_tri = None
        rep_mask = tf.sequence_mask(self.length, max_seq_len, tf.bool)
        new_tri_rep, new_ent_rep,tri_att_score,ent_att_score = mdatt.psan(tri_lstm1_outputs, ent_lstm1_outputs, rep_mask,initializer=self.initializer)
        with tf.variable_scope('ent_tri_interact_layer'):
            # b,2l,2h
            # ent_tri_concat = tf.concat([ent_lstm1_outputs,context_in_trigger],axis=2)

            ##b,l,2h
            ent_lstm2_outputs = bidirectional_LSTM(new_ent_rep,self.num_hiddens, self.initializer, sequence_length=self.length, output_sequence=True)

        with tf.variable_scope('tri_ent_interact_layer'):
            # tri_ent_concat = tf.concat([tri_lstm1_outputs,context_in_entity],axis=2)
            # b,l,2h
            tri_lstm2_outputs = bidirectional_LSTM(new_tri_rep,self.num_hiddens, self.initializer, sequence_length=self.length, output_sequence=True)

        lstm_outputs_ent = tf.reshape(ent_lstm2_outputs, shape=[-1, ent_lstm2_outputs.get_shape()[-1].value])
        lstm_outputs_tri = tf.reshape(tri_lstm2_outputs, shape=[-1, tri_lstm2_outputs.get_shape()[-1].value])
        # lstm_outputs_tar = tf.reshape(lstm_outputs_tar,shape=[-1, lstm_outputs_tar.get_shape()[-1].value])
        with tf.variable_scope("ent_feedforward_after_lstm"):
            outputs_ent = _common_layer(lstm_outputs_ent, 2 * self.num_hiddens, activity=tf.nn.tanh)
        with tf.variable_scope("tri_feedforward_after_lstm"):
            outputs_tri = _common_layer(lstm_outputs_tri, 2 * self.num_hiddens, activity=tf.nn.tanh)



        with tf.variable_scope('ent_feedforward_before_crf'):
            ent_scores = _common_layer(outputs_ent, self.task1_num_classess)
        with tf.variable_scope('tri_feedforward_before_crf'):
            #b*s,c
            tri_scores = _common_layer(outputs_tri, self.task2_num_classess)
            #
            # self.predicitons = tf.argmax(tri_scores,axis=-1)
            # mask_cast = tf.cast(self.sample_word_mask,tf.int64)
            # self.predicitons = tf.multiply(self.predicitons,mask_cast)


        # batchsize*step, num_classess
        ent_unary_scores = tf.reshape(ent_scores, shape=[tf.shape(self.input_token_indices)[0], -1, self.task1_num_classess])
        tri_unary_scores = tf.reshape(tri_scores, shape=[tf.shape(self.input_token_indices)[0], -1, self.task2_num_classess])

        with tf.variable_scope('ent_crf'):
            log_likelihood1, self.transition_params1, self.unary_scores1 = self._crf_layer(ent_unary_scores, self.length, self.y_entitys,self.task1_num_classess)
            self.source_loss = tf.reduce_mean(-log_likelihood1)
        with tf.variable_scope('tri_crf'):
            log_likelihood2, self.transition_params2, self.unary_scores2 = self._crf_layer(tri_unary_scores, self.length, self.y_triggers,self.task2_num_classess)
            self.trigger_loss = tf.reduce_mean(-log_likelihood2)
            #b*s
            # y_triggers = tf.reshape(self.y_triggers,shape=[-1])
            # #b*s
            # tri_loss = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=y_triggers,logits=tri_scores,name='tri_loss')
            # sample_word_mask = tf.reshape(self.sample_word_mask,shape=[-1])
            # self.trigger_loss = tf.reduce_sum(tf.multiply(tri_loss,tf.cast(sample_word_mask,tf.float32)))

        with tf.variable_scope('loss'):
            #2分loss
            self.union_loss =self.source_loss + self.trigger_loss
            self.train_op_sou = self.define_training_procedure(self.source_loss, self.sou_global_step)
            self.train_op_tri = self.define_training_procedure(self.trigger_loss, self.tri_global_step)
            self.train_op_uon = self.define_training_procedure(self.union_loss,self.sou_global_step)

    def _crf_layer(self, unary_scores, seq_len, y, num_class):
        small_score = -1000.0
        large_score = 0.0
        # batchsize , step , dim
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

    def train_model_union(self, sess, x_batch, y_batch):
        # token_indices_train_batch = x_batch
        # tys_sou, tys_tri, tys_tar = y_batch
        feed_dict = {
                    self.input_token_indices: x_batch[0],
                    self.input_postag_indices: x_batch[1],
                    self.input_suffix_indices: x_batch[2],
                    self.input_trigger_type_indices: x_batch[3],
                    # self.input_token_character_indices: x_batch[3],
                     self.y_entitys:y_batch[0],
                     self.y_triggers:y_batch[1],
                     # self.y_targets:y_batch[2],
                     # self.y_entitys_tc: y_batch[2],
                     # self.y_triggers_tc: y_batch[3],
                     # self.y_targets_tc: y_batch[5],
                     self.keep_dropout: 0.5}
        feed_dict[self.batch_size] = len(x_batch[0])
        _, loss_train, global_step \
            = sess.run([
            self.train_op_uon,
            self.union_loss,
            self.sou_global_step
        ],
            feed_dict=feed_dict)
        return global_step, loss_train
    def inference(self, sess, x_eval,y_eval):
        source_res = []
        trigger_res = []
        all_loss = []

        for sample_index in range(len(x_eval[0])):
            x_eval_batch = [wxs[sample_index] for wxs in x_eval]
            y_eval_batch = [tys[sample_index] for tys in y_eval]
            feed_dict = {
                        self.input_token_indices: [x_eval_batch[0]],
                        self.input_postag_indices: [x_eval_batch[1]],
                        self.input_suffix_indices: [x_eval_batch[2]],
                        self.input_trigger_type_indices: [x_eval_batch[3]],
                         self.y_entitys: [y_eval_batch[0]],
                         self.y_triggers: [y_eval_batch[1]],
                         # self.y_targets: [y_eval_batch[2]],
                         # self.y_entitys_tc: [y_eval_batch[2]],
                         # self.y_triggers_tc: [y_eval_batch[3]],
                         # self.y_targets_tc: [y_eval_batch[5]],
                         self.keep_dropout: 1}
            feed_dict[self.batch_size] = 1
            unary_score1, unary_score2,test_seq_len,transMatrix1, transMatrix2,loss = sess.run(
                [self.unary_scores1,
                 # self.predicitons,
                 self.unary_scores2,
                 self.length,
                 self.transition_params1,

                 self.transition_params2,
                 self.union_loss],
                feed_dict=feed_dict
            )

            source_res.extend(self.viterbi_decode_batch(unary_score1, test_seq_len, transMatrix1))
            # for i in range(len(tri_pred)):
            #     trigger_res.append(list(tri_pred[i]))

            trigger_res.extend(self.viterbi_decode_batch(unary_score2, test_seq_len, transMatrix2))
            all_loss.append(loss)
        return source_res,trigger_res, np.mean(np.array(all_loss))
    def inference_single(self, sess, x_list):


        feed_dict = {
                        self.input_token_indices: [x_list[0]],
                        self.input_postag_indices: [x_list[1]],
                        self.input_suffix_indices: [x_list[2]],
                        self.input_trigger_type_indices: [x_list[3]],
                         self.y_entitys: np.zeros((1,len(x_list[0])),dtype=np.int32),
                         self.y_triggers: np.zeros((1,len(x_list[0])),dtype=np.int32),

                         self.keep_dropout: 1}
        feed_dict[self.batch_size] = 1
        unary_score1, unary_score2,test_seq_len,transMatrix1, transMatrix2 = sess.run(
                [self.unary_scores1,
                 # self.predicitons,
                 self.unary_scores2,
                 self.length,
                 self.transition_params1,

                 self.transition_params2,],
                feed_dict=feed_dict
            )

        source_res = self.viterbi_decode_batch(unary_score1, test_seq_len, transMatrix1)[0]
            # for i in range(len(tri_pred)):
            #     trigger_res.append(list(tri_pred[i]))

        trigger_res = self.viterbi_decode_batch(unary_score2, test_seq_len, transMatrix2)[0]

        return source_res,trigger_res

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



    def _interact_attention_layer(self,lstm_outputs_entity, lstm_outputs_trigger):
        # query
        # keys
        # values
        window =3
        with tf.variable_scope('attention_project'):
            op_proj_w = tf.get_variable('op_proj_w', shape=[lstm_outputs_entity.get_shape()[-1], self.num_units])
            lstm_outputs_proj_OP = tf.tensordot(lstm_outputs_entity, op_proj_w, axes=1)
            ta_proj_w = tf.get_variable('ta_proj_w', shape=[lstm_outputs_trigger.get_shape()[-1], self.num_units])
            lstm_outputs_proj_TA = tf.tensordot(lstm_outputs_trigger, ta_proj_w, axes=1)
        b = tf.shape(lstm_outputs_entity)[0]
        num_step = tf.shape(lstm_outputs_entity)[1]
        with tf.variable_scope('att_OP'):
            input_ta = tf.TensorArray(size=num_step, dtype=tf.float32)
            output_ta = tf.TensorArray(size=num_step, dtype=tf.float32)
            lstm_op_trans = tf.transpose(lstm_outputs_proj_OP, [1, 0, 2])
            input_ta = input_ta.unstack(lstm_op_trans)

            def _body(time, output_ta_t):
                uid1 = tf.cond(time < window, lambda: tf.constant(0), lambda: time - window)
                uid2 = tf.cond(time + window >= num_step - 1, lambda: num_step - 1, lambda: time + window)
                tmp = [0] * 3
                # tmp[0] = tf.zeros([b, uid1], tf.float32)
                tmp[0] = tf.ones([b, time], tf.float32)
                tmp[1] = tf.zeros([b, 1], tf.float32)
                tmp[2] = tf.ones([b, num_step - 1 - time], tf.float32)
                # tmp[4] = tf.zeros([b, num_step - 1 - uid2], tf.float32)
                score_mask = tf.concat(tmp, axis=1)
                s, context = self._att_layer(input_ta.read(time), lstm_outputs_proj_TA, lstm_outputs_trigger, score_mask)
                att_re = tf.reduce_sum(context, axis=1)
                output_ta_t = output_ta_t.write(time, att_re)
                return time + 1, output_ta_t

            with tf.variable_scope('attention'):
                v_att = tf.get_variable('v_att', shape=[self.num_units], dtype=tf.float32)

            def _condition(time, output_ta_t):
                return time < num_step

            time = tf.constant(0)
            final_step, output_ta_final = tf.while_loop(
                cond=_condition,
                body=_body,
                loop_vars=(time, output_ta)
            )
            output_final = output_ta_final.stack()
            output_final = tf.transpose(output_final, [1, 0, 2])
            context_in_trigger = output_final

        with tf.variable_scope('att_TA'):
            input_ta = tf.TensorArray(size=num_step, dtype=tf.float32)
            output_ta = tf.TensorArray(size=num_step, dtype=tf.float32)
            lstm_ta_trans = tf.transpose(lstm_outputs_proj_TA, [1, 0, 2])
            input_ta = input_ta.unstack(lstm_ta_trans)

            def _body(time, output_ta_t):
                uid1 = tf.cond(time < window, lambda: tf.constant(0), lambda: time - window)
                uid2 = tf.cond(time + window >= num_step - 1, lambda: num_step - 1, lambda: time + window)
                tmp = [0] * 3
                # tmp[0] = tf.zeros([b, uid1], tf.float32)
                tmp[0] = tf.ones([b, time], tf.float32)
                tmp[1] = tf.zeros([b, 1], tf.float32)
                tmp[2] = tf.ones([b, num_step - 1 - time], tf.float32)
                # tmp[4] = tf.zeros([b, num_step - 1 - uid2], tf.float32)
                score_mask = tf.concat(tmp, axis=1)
                s, context = self._att_layer(input_ta.read(time), lstm_outputs_proj_OP, lstm_outputs_entity, score_mask)
                att_re = tf.reduce_sum(context, axis=1)
                output_ta_t = output_ta_t.write(time, att_re)
                return time + 1, output_ta_t

            with tf.variable_scope('attention'):
                v_att = tf.get_variable('v_att', shape=[self.num_units], dtype=tf.float32)

            def _condition(time, output_ta_t):
                return time < num_step

            time = tf.constant(0)
            final_step, output_ta_final = tf.while_loop(
                cond=_condition,
                body=_body,
                loop_vars=(time, output_ta)
            )
            output_final = output_ta_final.stack()
            output_final = tf.transpose(output_final, [1, 0, 2])
            context_in_entity = output_final

        return context_in_entity, context_in_trigger

    def _att_layer(self, query, keys, memory, scores_mask):
        # compute attention energies using a feed forward
        # query: [batchsize,dim]
        # keys: [batchsize,step.dim]
        FLOAT_MIN = -1.e9
        with tf.variable_scope('attention', reuse=True):
            v_att = tf.get_variable('v_att', shape=[self.num_units], dtype=tf.float32)
        energies = tf.reduce_sum(v_att * tf.tanh(keys + tf.expand_dims(query, 1)), [2])
        num_scores = tf.shape(energies)[1]
        energies = energies * scores_mask + (1.0 - scores_mask) * FLOAT_MIN
        energies = energies - tf.reduce_max(energies, axis=1, keep_dims=True)
        unnormalized_scores = tf.exp(energies) * scores_mask + 0.0001
        normalization = tf.reduce_sum(unnormalized_scores, axis=1, keep_dims=True)
        normalized_scores = unnormalized_scores / normalization
        context = tf.expand_dims(normalized_scores, 2) * memory
        return normalized_scores, context
