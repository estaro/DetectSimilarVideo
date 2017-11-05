package io.github.estaro.dsv.logic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
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

		VideoMetadata metadata = new VideoMetadata();
		String outputDir = config.getTempDir() + "/" + String.valueOf(videoFile.hashCode()).replace("-", "_");
		metadata.setFilename(videoFile);
		metadata.setFrameDirname(outputDir);

		// すでに作成されている場合は処理をしない
		File dir = new File(outputDir);
		if (dir.exists()) {
			return metadata;
		} else {
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
		long fileSize = new File(videoFile).length();
		double fps = capture.get(Videoio.CV_CAP_PROP_FPS);
		int width = (int) capture.get(Videoio.CV_CAP_PROP_FRAME_WIDTH);
		int height = (int) capture.get(Videoio.CV_CAP_PROP_FRAME_HEIGHT);
		int resizeHeight = (int) ((double)config.getImageWidth() / width * height);
		double allFrameCnt = capture.get(Videoio.CV_CAP_PROP_FRAME_COUNT);
		int captureInterval = (int) (allFrameCnt / (config.getCaptureCount() + 1));
		long time = (long) (allFrameCnt / fps);
		Properties videoProperty = new Properties();
		videoProperty.setProperty("playtime", String.valueOf(time));
		videoProperty.setProperty("width", String.valueOf(width));
		videoProperty.setProperty("height", String.valueOf(height));
		videoProperty.setProperty("fps", String.valueOf(fps));
		videoProperty.setProperty("size", String.valueOf(fileSize));
		videoProperty.store(new FileOutputStream(metadata.getMetaFilename()), "");

		// ------------------------------------------------------------------
		// 特徴量のための検出器
		// ------------------------------------------------------------------
		FeatureDetector detector = FeatureDetector.create(FeatureDetector.AKAZE);
		DescriptorExtractor executor = DescriptorExtractor.create(DescriptorExtractor.AKAZE);

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
			Imgcodecs.imwrite(metadata.getImagefile(i), resizedFrame);

			// ------------------------------------------------------------------
			// ヒストグラム
			// ------------------------------------------------------------------
			List<Mat> src = new ArrayList<Mat>();
			src.add(resizedFrame);
			Mat hist = new Mat();
			Imgproc.calcHist(src, new MatOfInt(0), new Mat(), hist, new MatOfInt(256), new MatOfFloat(0, 256));
			storeMat(metadata.getHistFilename(i), hist);

			// ------------------------------------------------------------------
			// 特徴量
			// ------------------------------------------------------------------
			Mat gray = new Mat();
			Imgproc.cvtColor(resizedFrame, gray, Imgproc.COLOR_RGB2GRAY);
			MatOfKeyPoint point = new MatOfKeyPoint();
			detector.detect(gray, point);
			Mat desc = new Mat();
			executor.compute(gray, point, desc);
			storeMat(metadata.getFeatFilename(i), desc);

			// ------------------------------------------------------------------
			// リソースの開放
			// ------------------------------------------------------------------
			orgFrame.release();
			resizedFrame.release();
			gray.release();
			point.release();
			hist.release();
			desc.release();
		}
		capture.release();

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

		VideoComparison videoComparison = new VideoComparison();
		videoComparison.setFilename1(video1.getFilename());
		videoComparison.setFilename2(video2.getFilename());
		String key = videoComparison.getKey();

		// キャッシュから返す
		if (cache != null && cache.containsKey(key)) {
			//System.out.println("cached");
			CachedComparison cachedComp = cache.get(key);
			videoComparison.setVideo1(video1);
			videoComparison.setVideo2(video2);
			videoComparison.setFilename1(cachedComp.file1);
			videoComparison.setFilename2(cachedComp.file2);
			videoComparison.setPlaytime(cachedComp.playtimeDiff);
			videoComparison.setHist(cachedComp.hist);
			videoComparison.setFeature(cachedComp.feature);
			videoComparison.setSkip(cachedComp.skip);
			return videoComparison;
		}

		Properties videoProperty1 = new Properties();
		videoProperty1.load(new FileInputStream(video1.getMetaFilename()));
		video1.setMetadataLabel(getMetaLabel(videoProperty1));
		Properties videoProperty2 = new Properties();
		videoProperty2.load(new FileInputStream(video2.getMetaFilename()));
		video2.setMetadataLabel(getMetaLabel(videoProperty2));

		videoComparison.setVideo1(video1);
		videoComparison.setVideo2(video2);

		// ------------------------------------------------------------------
		// 再生時間が閾値以上異なる→比較しない
		// ------------------------------------------------------------------
		long playtime1 = Long.parseLong((String) videoProperty1.get("playtime"));
		long playtime2 = Long.parseLong((String) videoProperty2.get("playtime"));
		long playtimeDiff = Math.abs(playtime1 - playtime2);
		videoComparison.setPlaytime(playtimeDiff);
		if (((double) playtimeDiff / playtime1) > 0.1) {
			videoComparison.setSkip(1);
			return videoComparison;
		}

		System.out.println(key);

		// ------------------------------------------------------------------
		// 特徴量のための比較器
		// ------------------------------------------------------------------
		DescriptorMatcher macher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE);

		// ------------------------------------------------------------------
		// 画像毎に比較
		// ------------------------------------------------------------------
		List<Double> histList = new ArrayList<Double>();
		List<Double> featureList = new ArrayList<Double>();
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
			Mat hist1 = loadMat(video1.getHistFilename(i));
			Mat hist2 = loadMat(video2.getHistFilename(i));
			histList.add(Imgproc.compareHist(hist1, hist2, 0));
			hist1.release();
			hist2.release();

			// ------------------------------------------------------------------
			// 特徴量の比較
			// ------------------------------------------------------------------
			try {
				MatOfDMatch feature = new MatOfDMatch();
				Mat desc1 = loadMat(video1.getFeatFilename(i));
				Mat desc2 = loadMat(video2.getFeatFilename(i));
				macher.match(desc1, desc2, feature);
				List<Double> distanceList = new ArrayList<>();
				for (DMatch dMatch : feature.toList()) {
					distanceList.add(Double.valueOf(dMatch.distance));
				}
				featureList.add(calcAvg(distanceList));

				desc1.release();
				desc2.release();
				feature.release();
			} catch (CvException e) {
				// noop
			}
		}


		videoComparison.setHist(calcAvg(histList));
		videoComparison.setFeature(calcAvg(featureList));
		return videoComparison;
	}

	private static String getMetaLabel(Properties prop) {
		String playtime = (String) prop.get("playtime");
		Double size = Double.parseDouble((String) prop.get("size"));
		String width = (String) prop.get("width");
		String height = (String) prop.get("height");
		String fps = (String) prop.get("fps");

		StringBuilder sb = new StringBuilder();
		sb.append("Play Time:" + playtime + "\n");
		sb.append(String.format("File Size:%d\n", (int) (size / 1024 / 1024)));
		sb.append("Frame Size:" + width + " * " + height + "\n");
		sb.append("FPS:" + fps + "\n");

		return sb.toString();

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

	/**
	 * Matをファイルに保存する
	 * @param filename
	 * @param mat
	 * @throws IOException
	 */
	public static void storeMat(String filename, Mat mat) throws IOException {
		String jsondata = matToJson(mat);
		PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(new File(filename))));
		writer.write(jsondata);
		writer.close();
	}

	/**
	 * Matをファイルから読み込む
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static Mat loadMat(String filename) throws IOException {
		InputStream input = new FileInputStream(filename);
		int size = input.available();
		byte[] buffer = new byte[size];
		input.read(buffer);
		input.close();
		return matFromJson(new String(buffer));
	}

	/**
	 * MatをJson形式に変換する
	 * @param mat
	 * @return
	 */
	public static String matToJson(Mat mat) {
		JsonObject obj = new JsonObject();

		if (mat.isContinuous()) {
			int cols = mat.cols();
			int rows = mat.rows();
			int elemSize = (int) mat.elemSize();
			int type = mat.type();
			String typeStr = CvType.typeToString(type);
			int dataSize = cols * rows * elemSize;
			if (rows == 0) {
				dataSize = cols * elemSize;
			}

			String dataString = "";
			if (typeStr.contains("32F")) {
				float[] data = new float[dataSize];
				String[] strData = new String[dataSize];
				mat.get(0, 0, data);
				for (int i = 0; i < data.length; i++) {
					strData[i] = String.valueOf(data[i]);
				}
				dataString = String.join(",", strData);
			} else if (typeStr.contains("8U")) {
				byte[] data = new byte[dataSize];
				String[] strData = new String[dataSize];
				mat.get(0, 0, data);
				for (int i = 0; i < data.length; i++) {
					strData[i] = String.valueOf(data[i]);
				}
				dataString = String.join(",", strData);
			}

			obj.addProperty("rows", mat.rows());
			obj.addProperty("cols", mat.cols());
			obj.addProperty("type", mat.type());
			obj.addProperty("data", dataString);

			Gson gson = new Gson();
			String json = gson.toJson(obj);

			return json;
		} else {
			System.err.println("");
		}
		return "{}";
	}

	/**
	 * Mat
	 * @param json
	 * @return
	 */
	public static Mat matFromJson(String json) {
		JsonParser parser = new JsonParser();
		JsonObject JsonObject = parser.parse(json).getAsJsonObject();

		int rows = JsonObject.get("rows").getAsInt();
		int cols = JsonObject.get("cols").getAsInt();
		int type = JsonObject.get("type").getAsInt();
		String typeStr = CvType.typeToString(type);

		String dataString = JsonObject.get("data").getAsString();
		Mat mat = new Mat(rows, cols, type);
		String[] splitedStr = dataString.split(",");
		if (typeStr.contains("32F")) {
			float[] data = new float[splitedStr.length];
			for (int i = 0; i < data.length; i++) {
				data[i] = Float.parseFloat(splitedStr[i]);
			}
			mat.put(0, 0, data);
		} else if (typeStr.contains("8U")) {
			byte[] data = new byte[splitedStr.length];
			for (int i = 0; i < data.length; i++) {
				data[i] = Byte.parseByte(splitedStr[i]);
			}
			mat.put(0, 0, data);
		} else {
			System.err.println("");
		}

		return mat;
	}
}
