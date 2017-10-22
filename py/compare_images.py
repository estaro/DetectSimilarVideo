# -*- coding: utf-8 -*-

"""
    compare_image.py

    Usage: python compare_image.py {dir1} {dir2}


"""

import configparser
from logging import getLogger, DEBUG, basicConfig
import os
from statistics import mean
import sys

import cv2


# --------------------------------------------------------------------------
# Preprocessing
# --------------------------------------------------------------------------
basicConfig(level=DEBUG)
logger = getLogger(__name__)
logger.setLevel(DEBUG)

# --------------------------------------------------------------------------
# constants
# --------------------------------------------------------------------------
# 動画からキャプチャするフレーム数
CAPUTURE_COUNT = 9

# メタ情報の保存ファイル
METAFILE = 'meta.dat'

# 許容する再生時間のずれ（%）
PERMIT_TIME_DIFF_RATE = 15

# --------------------------------------------------------------------------
# functions
# --------------------------------------------------------------------------
def compare_images(dir1, dir2, output_dir):
    """2つのディレクトリについて、同名画像ファイルを比較する

    :param
    :param

    :return 結果コード
        -1 error
    """
    logger.debug("start capture_frame ... ")

    config1 = configparser.ConfigParser()
    config1.read(dir1 + '/' + METAFILE)
    config2 = configparser.ConfigParser()
    config2.read(dir2 + '/' + METAFILE)

    #
    time1 = float(config1['metadata']['time'])
    time2 = float(config2['metadata']['time'])
    res_time_diff = abs(time1 - time2)

    # 明らかに再生時間が異なる
    if res_time_diff >= time1 * PERMIT_TIME_DIFF_RATE / 100:
        logger.warn("time1:" + str(time1) + ", time2:" + str(time2) + " , diff rate:" + str(time1 * PERMIT_TIME_DIFF_RATE / 100))
        return

    # 画像の比較
    hist_sum = []
    feature_sum = []
    bf = cv2.BFMatcher(cv2.NORM_HAMMING)
    detector = cv2.AKAZE_create()
    for i in range(CAPUTURE_COUNT):
        dir1_file = dir1 + "/" + str(i) + ".jpg"
        dir2_file = dir2 + "/" + str(i) + ".jpg"

        # 対象ファイルの存在チェック
        if not (os.path.exists(dir1_file) and os.path.exists(dir2_file)):
            continue;

        # 画像の読み込み
        dir1_img = cv2.imread(dir1_file)
        dir2_img = cv2.imread(dir2_file)

        # ヒストグラムを計算する
        dir1_hist = cv2.calcHist([dir1_img], [0], None, [256], [0, 256])
        dir2_hist = cv2.calcHist([dir2_img], [0], None, [256], [0, 256])
        hist_diff = cv2.compareHist(dir1_hist, dir2_hist, 0)
        hist_sum.append(hist_diff)
        logger.debug(hist_diff)

        # グレースケール化して特長量を抽出
        dir1_img_g = cv2.cvtColor(dir1_img, cv2.COLOR_RGB2GRAY)
        dir2_img_g = cv2.cvtColor(dir2_img, cv2.COLOR_RGB2GRAY)
        (dir1_kp, dir1_des) = detector.detectAndCompute(dir1_img_g, None)
        (dir2_kp, dir2_des) = detector.detectAndCompute(dir2_img_g, None)

        # 比較
        matches = bf.match(dir1_des, dir2_des)
        dist = [m.distance for m in matches]
        if len(dist) != 0:
            feature_sum.append(sum(dist) / len(dist))
        else:
            feauture = ""





    logger.debug(compare_result)
    logger.debug("finish compare_images ... ")
    return 0

# --------------------------------------------------------------------------
# main
# --------------------------------------------------------------------------
if __name__ == "__main__":
    compare_images(sys.argv[1], sys.argv[2], sys.argv[3])
