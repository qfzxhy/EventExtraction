#2018-3-17
#Pseudo Multi-Dimensional Self-Attention Network
import tensorflow as tf
from functools import reduce
from operator import mul
tf.set_random_seed(1024)
VERY_BIG_NUMBER = 1e30
VERY_NEGATIVE_VALUE = -VERY_BIG_NUMBER
def psan(opi_rep_tensor,ent_rep_tensor,rep_mask,num_hiddens=100,keep_prob=1,is_train=None,initializer=None,if_fusion_gate=True,emb_positons = None):
    # print(op_rep_tensor)
    with tf.variable_scope('psan'):
        with tf.variable_scope('op_att_ta'):
            new_opi_rep,opi_att_scores = normal_attention(opi_rep_tensor,ent_rep_tensor,rep_mask,initializer=initializer,if_fusion_gate=if_fusion_gate,position_rep = emb_positons) #bl,sl,vec
        with tf.variable_scope('ta_att_op'):
            new_ent_rep,ent_att_scores = normal_attention(ent_rep_tensor,opi_rep_tensor,rep_mask,initializer=initializer,if_fusion_gate=if_fusion_gate,position_rep = emb_positons)#bl,sl,vec
    # print(new_ta_rep)
    # op_rep = tf.reshape(new_op_rep, shape=[-1, new_op_rep.get_shape()[-1].value])
    # ta_rep = tf.reshape(new_ta_rep, shape=[-1, new_ta_rep.get_shape()[-1].value])
    # with tf.variable_scope("op_rep_linear"):
    #     op_rep = dense_layer(op_rep, 2 * num_hiddens, activity=tf.nn.tanh)
    # with tf.variable_scope("ta_rep_linear"):
    #     ta_rep = dense_layer(ta_rep, 2 * num_hiddens, activity=tf.nn.tanh)
    return new_opi_rep, new_ent_rep,opi_att_scores,ent_att_scores

def multi_dimensional_attention(query_rep,value_rep,rep_mask,keep_prob=1,is_train=None):
    # print(query_rep)
    #query_rep: [b,s,v]
    #rep_mask: [b,s]
    def scale_tanh(x,scale=5.):
        return scale * tf.nn.tanh(1./scale * x)

    bs, sl, vec = tf.shape(query_rep)[0], tf.shape(query_rep)[1], tf.shape(query_rep)[2]
    ivec = query_rep.get_shape()[-1]
    value_rep_tile = tf.tile(tf.expand_dims(value_rep,1),[1,sl,1,1]) #bs,sl,sl,vec
    # print('value_rep_tile')
    # print(value_rep_tile)
    with tf.variable_scope('multi_dimensional_attention'):

        diag_mask = tf.cast(tf.diag(- tf.ones([sl],tf.int32)) + 1,tf.bool)#[sl,sl]
        diag_mask_tile = tf.tile(tf.expand_dims(diag_mask,axis=0),[bs,1,1]) #[bs,sl,sl]
        rep_mask_tile = tf.tile(tf.expand_dims(rep_mask,1),[1,sl,1]) #[bs,sl,sl]
        att_mask = tf.logical_and(diag_mask_tile,rep_mask_tile) #[bs,sl,sl]
        with tf.variable_scope('attention'):# bs,sl, sl, vec
            f_bias = tf.get_variable('f_bias',[ivec],tf.float32,tf.constant_initializer(0.))
            querys = linear(value_rep,ivec,False,scope='linear_querys')

            querys_epd = tf.expand_dims(querys,axis=2) #bs,sl,1,vec

            keys = linear(value_rep,ivec,False,scope='linear_keys')
            keys_epd = tf.expand_dims(keys,axis=1) #bs, 1 ,sl, vec

            logits = scale_tanh(querys_epd + keys_epd + f_bias,5.0) #bs,sl,sl,vec

            logits_masked = exp_mask_for_high_rank(logits,att_mask)

            scores = tf.nn.softmax(logits_masked,2)
            # print(scores)
            scores_masked = mask_for_high_rank(scores,att_mask) #bs,sl,sl,vec
            # print(scores_masked)
            att_rep = tf.reduce_sum(value_rep_tile * scores_masked,axis=2) #bs,sl,vec
        with tf.variable_scope('output'):
            o_bias = tf.get_variable('o_bias',[ivec],tf.float32,tf.constant_initializer(0.))
            fusion_gate = tf.nn.sigmoid(
                linear(att_rep, ivec, True, 0., 'linear_fusion_a', is_train, keep_prob) +
                linear(query_rep, ivec, True, 0., 'linear_fusion_b', is_train, keep_prob) +
                o_bias
            )
            output = fusion_gate * query_rep + (1 - fusion_gate) * att_rep
            # print(output)
            output = mask_for_high_rank(output,rep_mask)
            # print(output)
        return output

def normal_attention(query_rep,value_rep,rep_mask,keep_prob=1.0,is_train=None,initializer = None,if_fusion_gate=True,position_rep = None):
    bs, sl, vec = tf.shape(query_rep)[0], tf.shape(query_rep)[1], tf.shape(query_rep)[2]
    ivec = query_rep.get_shape()[-1]
    value_rep_tile = tf.tile(tf.expand_dims(value_rep, 1), [1, sl, 1, 1])  # bs,sl,sl,vec
    with tf.variable_scope('normal_attention'):
        diag_mask = tf.cast(tf.diag(- tf.ones([sl], tf.int32)) + 1, tf.bool)  # [sl,sl]
        diag_mask_tile = tf.tile(tf.expand_dims(diag_mask, axis=0), [bs, 1, 1])  # [bs,sl,sl]
        rep_mask_tile = tf.tile(tf.expand_dims(rep_mask, 1), [1, sl, 1])  # [bs,sl,sl]
        att_mask = tf.logical_and(diag_mask_tile, rep_mask_tile)  # [bs,sl,sl]
        with tf.variable_scope('attention'):
            v_att = tf.get_variable('v_att',[ivec],tf.float32,initializer=initializer)
            querys = linear(query_rep,ivec,False,scope='query_linear',initializer=initializer)#bs,sl,units
            querys_epd = tf.expand_dims(querys,axis=2)#bs,sl,1,units
            values = linear(value_rep,ivec,False,scope='value_linear',initializer=initializer)
            values_epd = tf.expand_dims(values,1)#bs,1,sl,unis
            #b,s,s,units
            # position_values = linear(position_rep,ivec,False,scope='position_value_linear',initializer=initializer)
            logits = tf.reduce_sum(v_att * tf.nn.tanh(querys_epd + values_epd),[3]) #bs,sl,sl
            logits_masked = exp_mask_for_rank(logits,att_mask) #bs, sl,sl
            scores = tf.nn.softmax(logits_masked,dim=2)
            scores_masked = mask_for_rank(scores,att_mask)#bs, sl,sl
            scores_masked_exp = tf.expand_dims(scores_masked,axis=-1)#bs, sl,sl,1
            att_rep = tf.reduce_sum(value_rep_tile*scores_masked_exp,axis=2) #bs,sl,sl,vec - bs,sl,vec
        with tf.variable_scope('output'):
            o_bias = tf.get_variable('o_bias', [ivec], tf.float32, tf.constant_initializer(0.))
            fusion_gate = tf.nn.sigmoid(
                linear(att_rep, ivec, True, 0.,'linear_fusion_a' ,is_train,keep_prob,initializer=initializer) +
                linear(query_rep, ivec, True, 0.,'linear_fusion_b' ,is_train,keep_prob,initializer=initializer) +
                o_bias
            )
            output = tf.cond(tf.convert_to_tensor(if_fusion_gate,dtype=tf.bool), lambda: fusion_gate * query_rep + (1 - fusion_gate) * att_rep, lambda: query_rep + att_rep)
            # if fusion_gate:
            #     output = fusion_gate * query_rep + (1 - fusion_gate) * att_rep
            # else:
            #     output = query_rep + att_rep
            output = mask_for_high_rank(output, rep_mask)
        return output,scores_masked

    pass

def linear(args, output_size, bias, bias_start=0. ,scope=None, is_train=None, keep_dropout=1.0,initializer=None):
    if args==None or (isinstance(args,(tuple,list)) and not args):
        raise ValueError('args must ')
    if not isinstance(args,(tuple,list)):
        args = [args]
    if keep_dropout < 1.0:
        assert (is_train is not None )
        args = [tf.cond(is_train, lambda : tf.nn.dropout(arg,keep_dropout), lambda : arg) for arg in args]
    flat_args = [flatten(arg, 1) for arg in args]  # for dense layer [(-1, d)]
    flat_out = _linear(flat_args, output_size, bias, start_bias=bias_start,initializer=initializer, scope=scope)  # dense
    out = reconstruct(flat_out, args[0], 1)  # ()
    return out

def flatten(tensor, keep):
    fixed_shape = tensor.get_shape().as_list()
    start = len(fixed_shape) - keep
    left = reduce(mul, [fixed_shape[i] or tf.shape(tensor)[i] for i in range(start)])
    out_shape = [left] + [fixed_shape[i] or tf.shape(tensor)[i] for i in range(start, len(fixed_shape))]
    flat = tf.reshape(tensor, out_shape)
    return flat


def reconstruct(tensor, ref, keep, dim_reduced_keep=None):
    dim_reduced_keep = dim_reduced_keep or keep

    ref_shape = ref.get_shape().as_list() # original shape
    tensor_shape = tensor.get_shape().as_list() # current shape
    ref_stop = len(ref_shape) - keep # flatten dims list
    tensor_start = len(tensor_shape) - dim_reduced_keep  # start
    pre_shape = [ref_shape[i] or tf.shape(ref)[i] for i in range(ref_stop)] #
    keep_shape = [tensor_shape[i] or tf.shape(tensor)[i] for i in range(tensor_start, len(tensor_shape))] #
    # pre_shape = [tf.shape(ref)[i] for i in range(len(ref.get_shape().as_list()[:-keep]))]
    # keep_shape = tensor.get_shape().as_list()[-keep:]
    target_shape = pre_shape + keep_shape
    out = tf.reshape(tensor, target_shape)
    return out
def _linear(xs,output_size,bias,start_bias=0.,initializer=None,scope=None):
    with tf.variable_scope(scope or 'linear_layer'):
        x = tf.concat(xs, -1)
        input_size = x.get_shape()[-1]
        W = tf.get_variable('W', shape=[input_size, output_size], dtype=tf.float32,initializer=initializer)

        if bias:
            bias = tf.get_variable('bias', shape=[output_size], dtype=tf.float32,
                                   initializer=tf.constant_initializer(start_bias))
            out = tf.matmul(x, W) + bias
        else:
            out = tf.matmul(x, W)
        return out

def exp_mask_for_high_rank(val,val_mask,name=None):
    val_mask = tf.expand_dims(val_mask,-1) #bs, sl,sl,1
    res = tf.add(val,(1 - tf.cast(val_mask,tf.float32))*VERY_NEGATIVE_VALUE,name=name or 'exp_mask_high_rank' )
    return res
def exp_mask_for_rank(val,val_mask,name=None):
    res = tf.add(val,(1 - tf.cast(val_mask,tf.float32))*VERY_NEGATIVE_VALUE,name=name or 'exp_mask_high_rank' )
    return res
def mask_for_high_rank(val,val_mask,name=None):
    val_mask = tf.expand_dims(val_mask,-1)
    res = tf.multiply(val,tf.cast(val_mask,tf.float32),name=name or 'mask_high_rank')
    return res
def mask_for_rank(val,val_mask,name=None):

    res = tf.multiply(val,tf.cast(val_mask,tf.float32),name=name or 'mask_high_rank')
    return res
def drop_out():
    pass


def dense_layer(input_data, output_size, activity=None):
    # print(input_data.get_shape().as_list()[-1])
    W = tf.get_variable(
        "W",
        shape=[input_data.get_shape().as_list()[-1], output_size])

    b = tf.get_variable("bias",[output_size],initializer=tf.constant_initializer(0.))
    # batchsize*step, 1 * hiddens
    outputs = tf.nn.xw_plus_b(input_data, W, b)
    if activity is not None:
        outputs = activity(outputs, name="activity")
    return outputs


