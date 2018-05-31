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

        self.global_step = tf.Variable(0, name='private_global_step', trainable=False)

        self.batch_size = tf.placeholder(tf.int32, name='batch_size')

        self.input_token_indices = tf.placeholder(tf.int32, [None, None], name="input_token_indices")

        self.input_token_character_indices = tf.placeholder(tf.int32, [None, None, self.token_max_len],name="input_token_indices")


        self.input_postag_indices = tf.placeholder(tf.int32, [None, None], name="input_postag_indices")

        self.input_y = tf.placeholder(tf.int32, [None, None], name='input_y')

        self.keep_dropout = tf.placeholder(dtype=tf.float32, name='keep_dropout')

        self.initializer = tf.contrib.layers.xavier_initializer(seed = 2)
        self.length = tf.reduce_sum(tf.sign(self.input_token_indices), 1)
        self.length = tf.to_int32(self.length)
        max_seq_len = tf.shape(self.input_token_indices)[1]

        #


        def lstm_layer(input_data, seq_len):
            # batchsize, step, dim
            # print(input_data.get_shape()[-1].value)
            with tf.variable_scope("bilstm_layer_1"):
                lstm_outputs_flat = bidirectional_LSTM(input_data, self.num_hiddens, self.initializer, sequence_length=seq_len, output_sequence=True)
            # batchsize*step , 2*dim

            return lstm_outputs_flat


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

        with tf.variable_scope('embedding_layer') as vs:
            if self.pretrain_emb is not None:
                self.token_embedding_weights = tf.Variable(self.pretrain_emb, trainable=True,
                                                           name='token_embedding_weights', dtype=tf.float32)
            else:
                self.token_embedding_weights = tf.get_variable('token_embedding_weights',
                                                               [self.num_words, self.token_emb_dim])
            self.postag_embdding_weights = tf.get_variable('postag_embdding_weights',
                                                           [self.num_postags, self.postag_emb_dim])
            embedded_tokens = tf.nn.embedding_lookup(self.token_embedding_weights, self.input_token_indices)
            embedded_postags = tf.nn.embedding_lookup(self.postag_embdding_weights, self.input_postag_indices)

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
        with tf.variable_scope("lstm_layer"):
            # 经过共享层句子的表示
            #b,s,v
            token_lstm_input_drop_1 = tf.nn.dropout(token_lstm_input, self.keep_dropout, seed=1,name='token_lstm_input_drop')
            with tf.variable_scope("lstm_layer_1"):
                lstm_outputs_flat = bidirectional_LSTM(token_lstm_input_drop_1, self.num_hiddens, self.initializer,sequence_length=self.length, output_sequence=True)
            with tf.variable_scope("lstm_layer_2"):
                lstm_outputs_flat = bidirectional_LSTM(lstm_outputs_flat, self.num_hiddens, self.initializer,sequence_length=self.length, output_sequence=True)






        with tf.variable_scope("ent_feedforward_after_lstm"):
            lstm_outputs_reshape = tf.reshape(lstm_outputs_flat, shape=[-1, lstm_outputs_flat.get_shape()[-1].value])
            outputs = _common_layer(lstm_outputs_reshape, 2 * self.num_hiddens, activity=tf.nn.tanh)
        with tf.variable_scope("score_layer"):
            scores = _common_layer(outputs, self.num_classess)

        # batchsize，step, num_classess
        unary_scores = tf.reshape(scores, shape=[tf.shape(self.input_token_indices)[0], -1, self.num_classess])

        with tf.variable_scope('crf_layer'):
            log_likelihood1, self.transition_params1, self.unary_scores1 = self._crf_layer(unary_scores, self.length, self.input_y)
            self.loss = tf.reduce_mean(-log_likelihood1)

        self.train_op = self.define_training_procedure(self.loss, self.global_step)


    def _crf_layer(self, unary_scores, seq_len, y):
        small_score = -1000.0
        large_score = 0.0
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



    def train_model(self, sess, x_batch, y_batch):
        token_indices_train_batch = x_batch
        input_y_eval_train_batch = y_batch
        feed_dict = {
                     self.input_token_indices: token_indices_train_batch,

                     self.input_y: input_y_eval_train_batch,

                     self.keep_dropout: 0.5}
        feed_dict[self.batch_size] = len(y_batch)
        _, loss_train, global_step \
            = sess.run([
            self.train_op,
            self.loss,
            self.global_step
        ],
            feed_dict=feed_dict)
        return global_step, loss_train

    def inference_for_single(self, sess, x_eval, y_eval):
        pred = []
        all_loss = []

        input_token_indices_eval = x_eval
        input_y_eval = y_eval
        for sample_index in range(len(input_y_eval)):
            # start_index = itor * self.batch_size
            # end_index = min(start_index + self.batch_size, len(Xtest))

            input_token_indices_eval_batch = [input_token_indices_eval[sample_index]]

            input_y_eval_batch = [input_y_eval[sample_index]]

            feed_dict = {
                         self.input_token_indices: input_token_indices_eval_batch,

                         self.input_y: input_y_eval_batch,



                         self.keep_dropout: 1}
            feed_dict[self.batch_size] = len(input_y_eval_batch)
            unary_score1, test_seq_len, transMatrix1, loss, = sess.run(
                [self.unary_scores1,
                 self.length,
                 self.transition_params1,
                 self.loss,
            ],
                feed_dict=feed_dict
            )
            pred.extend(self.viterbi_decode_batch(unary_score1, test_seq_len, transMatrix1))

            all_loss.append(loss)

        return pred, np.mean(np.array(all_loss))

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

