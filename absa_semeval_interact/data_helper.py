import pandas as pd
import numpy as np
np.random.seed(1024)
class DataItor():
    def __init__(self,df,if_sort=False):
        self.epoch = 0
        # self.df = df
        self.batch_time = 0
        if if_sort:
            self.df_sort = df.sort_values(by=['len']).reset_index(drop=True)
        else:
            self.df_sort = df
        self.size = len(df)

        self.cursor = 0
        pass
    def reset(self):
        self.epoch = 0
        self.cursor = 0
    def shuffle(self):
        indices = np.random.permutation(len(self.df_sort))
        # print(indices)
        self.df_sort = self.df_sort.iloc[indices].reset_index(drop=True)

        pass

    def next_batch(self,batch_size,use_couples=False,shuffle=False):
        # token_id  # entity_id#trigger_id#entity_aux_id#trigger_aux_id#
        #laptop用shuffle好一些，其它级就不要shuffle了
        if self.cursor == 0 and shuffle:
            print('shuffle-  - - - - - - - - - - - - -  -   ')
            self.shuffle()
        # print(self.df_sort)
        self.batch_time += 1
        res = self.df_sort.ix[self.cursor:self.cursor+batch_size-1]
        words = list(map(lambda x: list(map(int,x.split(','))),res['token_id']))
        targets = list(map(lambda x: list(map(int, x.split(','))), res['tar_id']))
        opwords = list(map(lambda x: list(map(int, x.split(','))), res['opi_id']))


        lens = list(map(int, res['len']))

        max_len = max(lens)
        # positon_xs = np.zeros((len(res),max_len,max_len),dtype=np.int32)
        position_xs = get_position_matrix(batch_size,max_len)
        w_xs = np.zeros((len(res),max_len),dtype=np.int32)
        ta_xs = np.zeros((len(res),max_len),dtype=np.int32)
        op_xs = np.zeros((len(res),max_len),dtype=np.int32)
        co_xs = np.tile(np.diag([1]*max_len),[len(res),1,1])
        # np.zeros((len(res), max_len, max_len), dtype=np.int32)

        if use_couples:
            couples = list(map(lambda x: list(map(int, x.split(','))), res['couples']))

            for c_id,couple in enumerate(couples):
                if couple[0] == -1:
                    continue
                for i in range(0,len(couple),2):
                    co_xs[c_id][couple[i]][couple[i+1]] = 1
                    co_xs[c_id][couple[i+1]][couple[i]] = 1
                    # co_xs[c_id][couple[i]][couple[i]] = 0
                    # co_xs[c_id][couple[i+1]][couple[i+1]] = 0



        k = 0

        for word_idxs,ta_idxs,op_idxs in zip(words,targets,opwords):
            for i in range(len(word_idxs)):
                w_xs[k][i] = word_idxs[i]
                ta_xs[k][i] = ta_idxs[i]
                op_xs[k][i] = op_idxs[i]
            k += 1
        self.cursor += batch_size
        if self.cursor >= self.size:
            self.cursor = 0
            self.epoch += 1
            #
        return w_xs,(ta_xs,op_xs),co_xs

    def next_all(self,use_couples):
        res = self.df_sort.ix[self.cursor:]
        words = list(map(lambda x: list(map(int, x.split(','))), res['token_id']))
        targets = list(map(lambda x: list(map(int, x.split(','))), res['tar_id']))
        opwords = list(map(lambda x: list(map(int, x.split(','))), res['opi_id']))
        lens = list(map(int, res['len']))
        max_len = max(lens)
        w_xs = np.zeros((len(res), max_len), dtype=np.int32)
        ta_xs = np.zeros((len(res), max_len), dtype=np.int32)
        op_xs = np.zeros((len(res), max_len), dtype=np.int32)
        co_xs = np.zeros((len(res), max_len, max_len), dtype=np.int32)
        position_xs = get_position_matrix(len(res),max_len)
        if use_couples:
            couples = list(map(lambda x: list(map(int, x.split(','))), res['couples']))

            for c_id, couple in enumerate(couples):
                if couple[0] == -1:
                    continue
                for i in range(0, len(couple), 2):
                    co_xs[c_id][couple[i]][couple[i + 1]] = 1
                    co_xs[c_id][couple[i + 1]][couple[i]] = 1
        k = 0
        for word_idxs,ta_idxs,op_idxs in zip(words,targets,opwords):
            for i in range(len(word_idxs)):
                w_xs[k][i] = word_idxs[i]
                ta_xs[k][i] = ta_idxs[i]
                op_xs[k][i] = op_idxs[i]
            k += 1
        return w_xs,(ta_xs,op_xs),co_xs,lens

    def next_all_no_padding(self,use_couples=False):
        # token_id  # entity_id#trigger_id#entity_aux_id#trigger_aux_id#
        # self.batch_time += 1
        res = self.df_sort.ix[self.cursor:]

        words = list(map(lambda x: list(map(int,x.split(','))),res['token_id']))
        targets = list(map(lambda x: list(map(int, x.split(','))), res['tar_id']))
        opwords = list(map(lambda x: list(map(int, x.split(','))), res['opi_id']))
        lens = list(map(int, res['len']))

        w_xs = []
        ta_xs = []
        op_xs = []
        co_xs = []
        position_xs = []
        if use_couples:
            couples = list(map(lambda x: list(map(int, x.split(','))), res['couples']))
            for c_id, couple in enumerate(couples):
                co_xs_tmp = np.zeros(( lens[c_id], lens[c_id]), dtype=np.int32)
                if couple[0] != -1:
                    for i in range(0, len(couple), 2):
                        co_xs_tmp[couple[i]][couple[i + 1]] = 1
                        co_xs_tmp[couple[i + 1]][couple[i]] = 1
                co_xs.append(co_xs_tmp)
                position_tmp = get_position_matrix(len(res), lens[c_id])
                position_xs.append(np.squeeze(position_tmp, axis=0))


        for word_idxs,ta_idxs,op_idxs in zip(words,targets,opwords):
            w_x = []
            ta_x = []
            op_x = []
            for i in range(len(word_idxs)):
                w_x.append(word_idxs[i])
                ta_x.append(ta_idxs[i])
                op_x.append(op_idxs[i])
            w_xs.append(w_x)
            ta_xs.append(ta_x)
            op_xs.append(op_x)
        self.cursor += 1
        return w_xs,(ta_xs,op_xs),co_xs,lens




def get_position_matrix(batch,step):
    a = np.reshape(np.array([i+1 for i in range(step)] * step,dtype=np.int32),newshape=[step,step])
    # print(a)
    b = np.zeros_like(a,dtype=np.int32)
    for i in range(len(b)):
        for j in range(len(b[i])):
            b[i,j] = i + 1
    c = np.tile(np.abs(b - a),reps=[batch,1,1])
    return c
if __name__ == '__main__':
    df = pd.read_csv('res15_data/datas/test_indice',sep='#',skip_blank_lines=False,dtype={'len':np.int32})
    # print(df.dtypes)
    d = DataItor(df)
    w_xs, (ta_xs, op_xs), co_xs, lens, position_xs= d.next_all_no_padding()
    print(position_xs)
    # w_xs, (ta_xs, op_xs), co_xs = d.next_batch(1)
    # print(w_xs[0])
    # get_position_matrix(2,5)