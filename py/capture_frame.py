# -*- coding: utf-8 -*-

"""
    capture_frame.py

    Usage: python capture_frame.py {video file} {output dir}

    動画ファイルからいくつかのフレームをキャプチャして指定のディレクトリに画像ファイルを保存する。

"""

import configparser
from logging import getLogger, DEBUG
import sys

import cv2

# --------------------------------------------------------------------------
# Preprocessing
# --------------------------------------------------------------------------
logger = getLogger(__name__)
logger.setLevel(DEBUG)

# --------------------------------------------------------------------------
# constants
# --------------------------------------------------------------------------
# OpenCV
CV_CAP_PROP_POS_FRAMES = 1
CV_CAP_PROP_FRAME_WIDTH = 3
CV_CAP_PROP_FRAME_HEIGHT = 4
CV_CAP_PROP_FPS = 5
CV_CAP_PROP_FRAME_COUNT = 7

# 動画からキャプチャするフレーム数
CAPUTURE_COUNT = 9

# 保存する画像のwidth
CAPTURE_WIDTH = 200

# メタ情報の保存ファイル
METAFILE = 'meta.dat'

# --------------------------------------------------------------------------
# functions
# --------------------------------------------------------------------------
def capture_frame(video_file, output_dir):
    """動画ファイルから画像をキャプチャする

    :param video_file 動画ファイル
    :param output_dir キャプチャしたフレームを保存するディレクトリ

    :return 結果コード
        -1 error
    """
    logger.debug("start capture_frame ... ")

    capture = cv2.VideoCapture(video_file)
    if not capture.isOpened():
        logger.error("ファイルが開けませんでした")
        return -1

    # 動画の情報を取得する
    fps = capture.get(CV_CAP_PROP_FPS)
    resize_height = int(CAPTURE_WIDTH / capture.get(CV_CAP_PROP_FRAME_WIDTH) * capture.get(CV_CAP_PROP_FRAME_HEIGHT))
    all_frame_cnt = capture.get(CV_CAP_PROP_FRAME_COUNT)
    interval = all_frame_cnt / (CAPUTURE_COUNT + 1)
    all_time = all_frame_cnt / fps

    # キャプチャ
    capture_frame_list = list(map(lambda pos: interval * pos, range(1, CAPUTURE_COUNT + 1)))
    for i, capture_frame in enumerate(capture_frame_list):
        capture.set(CV_CAP_PROP_POS_FRAMES, capture_frame - 1)
        ret, frame = capture.read()
        if not ret:
            continue

        resized_frame = cv2.resize(frame, (CAPTURE_WIDTH, resize_height))
        cv2.imwrite(output_dir + "/" + str(i) + ".jpg", resized_frame)


    # メタ情報を保存する
    config = configparser.ConfigParser()
    config['metadata'] = {'filename=':video_file, 'time':str(all_time)}
    with open(output_dir + '/' + METAFILE, 'w') as configfile:
        config.write(configfile)

    logger.debug("finish capture_frame ... ")
    return 0

# --------------------------------------------------------------------------
# main
# --------------------------------------------------------------------------
if __name__ == "__main__":
    capture_frame(sys.argv[1], sys.argv[2])
