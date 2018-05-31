import pandas as pd
import numpy as np
class DataItor():
    def __init__(self,df,is_shuffle=True):
        self.epoch = 0
        # self.df = df
        self.batch_time = 0
        if is_shuffle:
            self.df_sort = df.sort_values(by=['len']).reset_index(drop=True)
        else:
            self.df_sort = df
        self.size = len(df)

        self.cursor = 0
        pass
    def reset(self):
        self.epoch = 0
        self.cursor = 0
    def next_batch(self,batch_size):
        # token_id  # entity_id#trigger_id#entity_aux_id#trigger_aux_id#
        self.batch_time += 1
        res = self.df_sort.ix[self.cursor:self.cursor+batch_size-1]
        # charas = list(map(lambda x: list(map(lambda y: list(map(int,y.split('|'))),x.split(','))),res['char_id']))

        words = list(map(lambda x: list(map(int,x.split(','))),res['token_id']))
        ptags = list(map(lambda x: list(map(int, x.split(','))), res['postag_id']))
        suffs = list(map(lambda x: list(map(int, x.split(','))), res['suffix_id']))
        tri_types = list(map(lambda x: list(map(int, x.split(','))), res['sample_word']))
        tags_sou = list(map(lambda x:list(map(int,x.split(','))),res['entity_id']))
        tags_tri = list(map(lambda x: list(map(int, x.split(','))), res['trigger_id']))
        # tags_tar = list(map(lambda x: list(map(int, x.split(','))), res['tidx_tar']))
        # tags_sou_aux = list(map(lambda x: list(map(int, x.split(','))), res['entity_aux_id']))
        # tags_tri_aux = list(map(lambda x: list(map(int, x.split(','))), res['trigger_aux_id']))
        # tags_tar_aux = list(map(lambda x: list(map(int, x.split(','))), res['tidx_tar_aux']))
        lens = list(map(int,res['len']))
        max_len = max(lens)
        # cxs = np.zeros((len(res),max_len,9),dtype=np.int32)
        wxs = np.zeros((len(res),max_len),dtype=np.int32)
        pxs = np.zeros((len(res),max_len),dtype=np.int32)
        sxs = np.zeros((len(res),max_len),dtype=np.int32)
        tri_type_xs = np.zeros((len(res), max_len), dtype=np.int32)
        tys_sou = np.zeros((len(res),max_len),dtype=np.int32)
        tys_tri = np.zeros((len(res), max_len), dtype=np.int32)
        # tys_tar = np.zeros((len(res), max_len), dtype=np.int32)
        # tys_sou_aux = np.zeros((len(res), max_len), dtype=np.int32)
        # tys_tri_aux = np.zeros((len(res), max_len), dtype=np.int32)
        # tys_tar_aux = np.zeros((len(res), max_len), dtype=np.int32)
        k = 0
        for word_idxs, ptag_idxs, suff_idxs, tag_sou, tag_tri, tri_type in zip(words, ptags, suffs, tags_sou, tags_tri,
                                                                               tri_types):
            for i in range(len(word_idxs)):
                # for j in range(len(char_idxs[i])):
                #     cxs[k][i][j] = char_idxs[i][j]
                wxs[k][i] = word_idxs[i]
                pxs[k][i] = ptag_idxs[i]
                sxs[k][i] = suff_idxs[i]
                tri_type_xs[k][i] = tri_type[i]
                tys_sou[k][i] = tag_sou[i]
                tys_tri[k][i] = tag_tri[i]
                # tys_tar[k][i] = tag_tar[i]

                # tys_sou_aux[k][i] = tag_sou_aux[i]
                # tys_tri_aux[k][i] = tag_tri_aux[i]
                # tys_tar_aux[k][i] = tag_tar_aux[i]
            k += 1
        self.cursor += batch_size
        if self.cursor >= self.size:
            self.cursor = 0
            self.epoch += 1
        return (wxs,pxs,sxs,tri_type_xs),(tys_sou,tys_tri)

    def next_all(self):
        res = self.df_sort.ix[self.cursor:]
        # charas = list(map(lambda x: list(map(lambda y: list(map(int, y.split('|'))), x.split(','))), res['char_id']))
        words = list(map(lambda x: list(map(int, x.split(','))), res['token_id']))
        ptags = list(map(lambda x: list(map(int, x.split(','))), res['postag_id']))
        suffs = list(map(lambda x: list(map(int, x.split(','))), res['suffix_id']))
        tri_types = list(map(lambda x: list(map(int, x.split(','))), res['sample_word']))
        tags_sou = list(map(lambda x: list(map(int, x.split(','))), res['entity_id']))
        tags_tri = list(map(lambda x: list(map(int, x.split(','))), res['trigger_id']))
        # tags_tar = list(map(lambda x: list(map(int, x.split(','))), res['tidx_tar']))
        # tags_sou_aux = list(map(lambda x: list(map(int, x.split(','))), res['entity_aux_id']))
        # tags_tri_aux = list(map(lambda x: list(map(int, x.split(','))), res['trigger_aux_id']))
        lens = list(map(int, res['len']))
        max_len = max(lens)
        # cxs = np.zeros((len(res), max_len, 9), dtype=np.int32)
        wxs = np.zeros((len(res), max_len), dtype=np.int32)
        pxs = np.zeros((len(res), max_len), dtype=np.int32)
        sxs = np.zeros((len(res), max_len), dtype=np.int32)
        tri_type_xs =  np.zeros((len(res), max_len), dtype=np.int32)
        tys_sou = np.zeros((len(res), max_len), dtype=np.int32)
        tys_tri = np.zeros((len(res), max_len), dtype=np.int32)
        # tys_tar = np.zeros((len(res), max_len), dtype=np.int32)
        # tys_sou_aux = np.zeros((len(res), max_len), dtype=np.int32)
        # tys_tri_aux = np.zeros((len(res), max_len), dtype=np.int32)
        # tys_tar_aux = np.zeros((len(res), max_len), dtype=np.int32)
        k = 0
        for word_idxs,ptag_idxs,suff_idxs,tag_sou,tag_tri,tri_type in zip(words,ptags,suffs,tags_sou,tags_tri,tri_types):
            for i in range(len(word_idxs)):
                # for j in range(len(char_idxs[i])):
                #     cxs[k][i][j] = char_idxs[i][j]
                wxs[k][i] = word_idxs[i]
                pxs[k][i] = ptag_idxs[i]
                sxs[k][i] = suff_idxs[i]
                tri_type_xs[k][i] = tri_type[i]
                tys_sou[k][i] = tag_sou[i]
                tys_tri[k][i] = tag_tri[i]
                # tys_tar[k][i] = tag_tar[i]

                # tys_sou_aux[k][i] = tag_sou_aux[i]
                # tys_tri_aux[k][i] = tag_tri_aux[i]
                # tys_tar_aux[k][i] = tag_tar_aux[i]
            k += 1
        return (wxs,pxs,sxs,tri_type_xs), (tys_sou, tys_tri),lens
if __name__ == '__main__':
    df = pd.read_csv('traindata_id.txt',sep='#',skip_blank_lines=False,dtype={'len':np.int32})
    # print(df.dtypes)
    d = DataItor(df)

    cur_epoch = 0
    wxs, t = d.next_batch(10)
    print(wxs[2])



