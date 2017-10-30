package io.github.estaro.dsv.logic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.github.estaro.dsv.bean.CachedComparison;
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
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	static public VideoMetadata captureFrame(Config config, String videoFile)
			throws FileNotFoundException, IOException {

		// 結果
		VideoMetadata metadata = new VideoMetadata();
		metadata.setFilename(videoFile);

		// 出力ディレクトリ
		String outputDir = config.getTempDir() + "/" + String.valueOf(videoFile.hashCode()).replace("-", "_");
		metadata.setFrameDirname(outputDir);
		File dir = new File(outputDir);
		if (!dir.exists()) {
			dir.mkdir();
		}
		// ------------------------------------------------------------------
		// 動画を開く
		// ------------------------------------------------------------------
		System.out.println(videoFile);
		VideoCapture capture = new VideoCapture(videoFile);
		if (!capture.isOpened()) {
			System.out.println("file not opened.");
			return null;
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
		// 特徴量のための検出器
		// ------------------------------------------------------------------
		FeatureDetector detector = FeatureDetector.create(FeatureDetector.AKAZE);
		DescriptorExtractor executor = DescriptorExtractor.create(DescriptorExtractor.AKAZE);

		// ------------------------------------------------------------------
		// 設定で指定した数のフレームを取得する
		// ------------------------------------------------------------------
		List<Mat> histList = new ArrayList<>();
		List<Mat> featureList = new ArrayList<>();
		for (int i = 1; i < config.getCaptureCount() + 1; i++) {
			int frameIndex = captureInterval * i;
			Mat orgFrame = new Mat();
			Mat resizedFrame = new Mat();
			capture.set(Videoio.CV_CAP_PROP_POS_FRAMES, frameIndex - 1);
			if (!capture.read(orgFrame)) {
				continue;
			}
			Imgproc.resize(orgFrame, resizedFrame, new Size(config.getImageWidth(), resizeHeight));
			Imgcodecs.imwrite(metadata.getImagefile(i), resizedFrame);

			// ------------------------------------------------------------------
			// ヒストグラム
			// ------------------------------------------------------------------
			List<Mat> src = new ArrayList<Mat>();
			src.add(resizedFrame);
			Mat hist = new Mat();
			Imgproc.calcHist(src, new MatOfInt(0), new Mat(), hist, new MatOfInt(256), new MatOfFloat(0, 256));
			histList.add(hist);

			// ------------------------------------------------------------------
			// 特徴量
			// ------------------------------------------------------------------
			Mat gray = new Mat();
			Imgproc.cvtColor(resizedFrame, gray, Imgproc.COLOR_RGB2GRAY);
			MatOfKeyPoint point = new MatOfKeyPoint();
			detector.detect(gray, point);
			Mat desc = new Mat();
			executor.compute(gray, point, desc);
			featureList.add(desc);
		}

		capture.release();

		metadata.setPlayTime(time);
		metadata.setHistgramImg(histList);
		metadata.setFeatureImg(featureList);

		return metadata;
	}

	/**
	 * 2つのディレクトリ内の画像をそれぞれ比較する
	 *
	 * @param config
	 * @param cache
	 * @param video1
	 * @param video2
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	static public VideoComparison compareImages(Config config, Map<String, CachedComparison> cache,
			VideoMetadata video1, VideoMetadata video2) throws FileNotFoundException, IOException {

		String key = video1.getFilename() + "<--->" + video2.getFilename();
		System.out.println(key);
		List<Double> histList = new ArrayList<Double>();
		List<Double> featureList = new ArrayList<Double>();

		// キャッシュから返す
		if (cache != null && cache.containsKey(key)) {
			VideoComparison videoComparison = new VideoComparison();
			videoComparison.setFilename1(cache.get(key).file1);
			videoComparison.setFilename2(cache.get(key).file1);
			videoComparison.setPlaytime(cache.get(key).playtimeDiff);
			videoComparison.setHist(cache.get(key).hist);
			videoComparison.setFeature(cache.get(key).feature);
			return videoComparison;
		}

		// ------------------------------------------------------------------
		// 再生時間が閾値以上異なる→比較しない
		// ------------------------------------------------------------------
		long playtime1 = video1.getPlayTime();
		long playtime2 = video2.getPlayTime();
		long playtimeDiff = Math.abs(playtime1 - playtime2);
		if ((playtimeDiff / playtime1) > 0.1) {
			return null;
		}

		// ------------------------------------------------------------------
		// 特徴量のための比較器
		// ------------------------------------------------------------------
		DescriptorMatcher macher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);

		// ------------------------------------------------------------------
		// 画像毎に比較
		// ------------------------------------------------------------------
		for (int i = 1; i < config.getCaptureCount() + 1; i++) {
			String filename1 = video1.getImagefile(i);
			String filename2 = video2.getImagefile(i);
			File file1 = new File(filename1);
			File file2 = new File(filename2);
			if (!(file1.exists() && file2.exists())) {
				continue;
			}

			// ------------------------------------------------------------------
			// ヒストグラムの比較
			// ------------------------------------------------------------------
			histList.add(
					Imgproc.compareHist(video1.getHistgramImg().get(i - 1), video2.getHistgramImg().get(i - 1), 0));

			// ------------------------------------------------------------------
			// 特徴量の比較
			// ------------------------------------------------------------------
			try {
				MatOfDMatch feature = new MatOfDMatch();
				macher.match(video1.getFeatureImg().get(i - 1), video2.getFeatureImg().get(i - 1), feature);
				List<Double> distanceList = new ArrayList<>();
				for (DMatch dMatch : feature.toList()) {
					distanceList.add(Double.valueOf(dMatch.distance));
				}
				featureList.add(calcAvg(distanceList));
			} catch (CvException e) {
				// noop
			}

		}

		VideoComparison videoComparison = new VideoComparison();
		videoComparison.setVideo1(video1);
		videoComparison.setVideo2(video2);
		videoComparison.setPlaytime(playtimeDiff);
		videoComparison.setHist(calcAvg(histList));
		videoComparison.setFeature(calcAvg(featureList));
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

	public static void storeMat(String filename, Mat mat) throws IOException {
		String jsondata = matToJson(mat);
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filename))));
		writer.write(jsondata);
		writer.close();
	}

	public static Mat loadMat(String filename) throws IOException {
		InputStream input = new FileInputStream(filename);
		int size = input.available();
		byte[] buffer = new byte[size];
		input.read(buffer);
		input.close();
		return matFromJson(new String(buffer));
	}

	public static String matToJson(Mat mat) {
		JsonObject obj = new JsonObject();

		if (mat.isContinuous()) {
			int cols = mat.cols();
			int rows = mat.rows();
			int elemSize = (int) mat.elemSize();

			byte[] data = new byte[cols * rows * elemSize];

			mat.get(0, 0, data);

			obj.addProperty("rows", mat.rows());
			obj.addProperty("cols", mat.cols());
			obj.addProperty("type", mat.type());

			// We cannot set binary data to a json object, so:
			// Encoding data byte array to Base64.
			//String dataString = new String(Base64.encode(data, Base64.DEFAULT));
			String dataString = new String(Base64.getEncoder().encodeToString(data));

			obj.addProperty("data", dataString);

			Gson gson = new Gson();
			String json = gson.toJson(obj);

			return json;
		} else {
			System.out.println("");
		}
		return "{}";
	}

	public static Mat matFromJson(String json) {
		JsonParser parser = new JsonParser();
		JsonObject JsonObject = parser.parse(json).getAsJsonObject();

		int rows = JsonObject.get("rows").getAsInt();
		int cols = JsonObject.get("cols").getAsInt();
		int type = JsonObject.get("type").getAsInt();

		String dataString = JsonObject.get("data").getAsString();
		//byte[] data = Base64.decode(dataString.getBytes(), Base64.DEFAULT);
		byte[] data = Base64.getDecoder().decode(dataString.getBytes());

		Mat mat = new Mat(rows, cols, type);
		mat.put(0, 0, data);

		return mat;
	}
}
