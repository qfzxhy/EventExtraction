import tensorflow as tf
tf.set_random_seed(1024)
def rel_network(opi_rep,tar_rep):
    #ent_rep,opi_rep:[b,s,v]
    ivec = opi_rep.get_shape().as_list()[-1]
    tar_rep = tf.expand_dims(tar_rep,axis=2)
    opi_rep = tf.expand_dims(opi_rep,axis=1)
    combine_rep = tf.add(tar_rep,opi_rep)
    combine_rep_flat = tf.reshape(combine_rep,shape=[-1,ivec])
    return combine_rep_flat

    pass