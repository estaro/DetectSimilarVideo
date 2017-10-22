package io.github.estaro.dsv.logic;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import io.github.estaro.dsv.bean.Config;
import io.github.estaro.dsv.bean.VideoComparison;
import io.github.estaro.dsv.bean.VideoMetadata;

/**
 * OpenCVを使用した画像処理を提供するクラス
 *
 */
public class OpenCvProcessor {

	static {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}

	/**
	 * 映像ファイルからフレームをキャプチャして画像として保存する
	 *
	 * @param config
	 * @param videoFile
	 * @param outputDir
	 * @return
	 */
	static public VideoMetadata captureFrame(Config config, String videoFile) {
		// ------------------------------------------------------------------
		// 動画を開く
		// ------------------------------------------------------------------
		System.out.println(videoFile);
		VideoCapture capture = new VideoCapture(videoFile);
		if (!capture.isOpened()) {
			System.out.println("file not opened.");
			return null;
		}

		String outputDir = config.getTempDir() + "/" + String.valueOf(videoFile.hashCode()).replace("-", "_");
		File dir = new File(outputDir);
		if (!dir.exists()) {
			dir.mkdir();
		}

		// ------------------------------------------------------------------
		// 動画情報の取得
		// ------------------------------------------------------------------
		double fps = capture.get(Videoio.CV_CAP_PROP_FPS);
		int resizeHeight = (int) (config.getImageWidth() / capture.get(Videoio.CV_CAP_PROP_FRAME_WIDTH)
				* capture.get(Videoio.CV_CAP_PROP_FRAME_HEIGHT));
		double allFrameCnt = capture.get(Videoio.CV_CAP_PROP_FRAME_COUNT);
		int captureInterval = (int) (allFrameCnt / (config.getCaptureCount() + 1));
		long time = (long) (allFrameCnt / fps);

		// ------------------------------------------------------------------
		// 設定で指定した数のフレームを取得する
		// ------------------------------------------------------------------
		for (int i = 1; i < config.getCaptureCount() + 1; i++) {
			int frameIndex = captureInterval * i;
			Mat orgFrame = new Mat();
			Mat resizedFrame = new Mat();
			capture.set(Videoio.CV_CAP_PROP_POS_FRAMES, frameIndex - 1);
			if (!capture.read(orgFrame)) {
				continue;
			}
			Imgproc.resize(orgFrame, resizedFrame, new Size(config.getImageWidth(), resizeHeight));
			Imgcodecs.imwrite(outputDir + "/" + i + ".jpg", resizedFrame);

		}

		VideoMetadata metadata = new VideoMetadata();
		metadata.setFilename(videoFile);
		metadata.setFrameDirname(outputDir);
		metadata.setPlayTime(time);
		return metadata;
	}

	/**
	 * 2つのディレクトリ内の画像をそれぞれ比較する
	 *
	 * @param config
	 * @param video1
	 * @param video2
	 * @return
	 */
	static public VideoComparison compareImages(Config config, VideoMetadata video1, VideoMetadata video2) {

		List<Double> histList = new ArrayList<Double>();
		List<Double> featureList = new ArrayList<Double>();

		// ------------------------------------------------------------------
		// 特徴量のための検出器
		// ------------------------------------------------------------------
		FeatureDetector detector = FeatureDetector.create(FeatureDetector.AKAZE);
		DescriptorMatcher macher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_L1);

		// ------------------------------------------------------------------
		// 画像毎に比較
		// ------------------------------------------------------------------
		for (int i = 1; i < config.getCaptureCount() + 1; i++) {
			String filename1 = video1.getFrameDirname() + "/" + i + ".jpg";
			String filename2 = video2.getFrameDirname() + "/" + i + ".jpg";
			File file1 = new File(filename1);
			File file2 = new File(filename2);
			if (!(file1.exists() && file2.exists())) {
				continue;
			}

			// ------------------------------------------------------------------
			// ヒストグラム
			// ------------------------------------------------------------------
			Mat img1 = Imgcodecs.imread(filename1);
			List<Mat> src1 = new ArrayList<Mat>();
			src1.add(img1);
			Mat hist1 = new Mat();
			Imgproc.calcHist(src1, new MatOfInt(0), new Mat(), hist1, new MatOfInt(256), new MatOfFloat(0, 256));

			Mat img2 = Imgcodecs.imread(filename2);
			List<Mat> src2 = new ArrayList<Mat>();
			src2.add(img2);
			Mat hist2 = new Mat();
			Imgproc.calcHist(src2, new MatOfInt(0), new Mat(), hist2, new MatOfInt(256), new MatOfFloat(0, 256));

			histList.add(Imgproc.compareHist(hist1, hist2, 0));

			// ------------------------------------------------------------------
			// 特徴量
			// ------------------------------------------------------------------
			/*
			Mat gray1 = new Mat();
			Imgproc.cvtColor(img1, gray1, Imgproc.COLOR_RGB2GRAY);
			MatOfKeyPoint point1 = new MatOfKeyPoint();
			detector.detect(gray1, point1);

			Mat gray2 = new Mat();
			Imgproc.cvtColor(img2, gray2, Imgproc.COLOR_RGB2GRAY);
			MatOfKeyPoint point2 = new MatOfKeyPoint();
			detector.detect(gray2, point2);

			MatOfDMatch feature = new MatOfDMatch();
			macher.match(point1, point2, feature);
			List<Double> distanceList = new ArrayList<>();
			for (DMatch dMatch : feature.toList()) {
				distanceList.add(Double.valueOf(dMatch.distance));
			}
			featureList.add(calcAvg(distanceList));
			*/

		}

		VideoComparison videoComparison = new VideoComparison();
		videoComparison.setVideo1(video1);
		videoComparison.setVideo2(video2);
		videoComparison.setHist(calcAvg(histList));
		videoComparison.setFeature(0.0);
		return videoComparison;
	}

	/**
	 * リストの平均値を取得する
	 *
	 * @param inputList
	 * @return
	 */
	private static double calcAvg(List<Double> inputList) {
		if (inputList == null || inputList.size() == 0) {
			return 0.0;
		}
		double sum = 0.0;
		for (double val : inputList) {
			sum += val;
		}
		return sum / inputList.size();
	}
}
