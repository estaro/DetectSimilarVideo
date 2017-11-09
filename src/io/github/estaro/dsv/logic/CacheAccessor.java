package io.github.estaro.dsv.logic;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.estaro.dsv.bean.CachedComparison;
import io.github.estaro.dsv.bean.Config;
import io.github.estaro.dsv.bean.VideoComparison;
import io.github.estaro.dsv.bean.VideoMetadata;

/**
 * キャッシュの管理
 *
 * SQLiteで保存します。
 */
public class CacheAccessor {

	public static final String DB_FILE_NAME = "dsv_%s.db";

	public static final String RESULT_TABLE = "compare_video";

	private Map<String, Connection> conMap = null;

	private static CacheAccessor instance = null;

	private Config config = null;

	/**
	 * シングルトンで管理する
	 * @param config
	 * @return
	 * @throws SQLException
	 */
	public static synchronized CacheAccessor getInstace(Config config) throws SQLException {
		if (instance == null) {
			instance = new CacheAccessor(config);
		}
		return instance;
	}

	/**
	 * コンストラクタ
	 *
	 * DBへの接続を確保してテーブルがなければここで作成する
	 *
	 * @param config
	 * @throws SQLException
	 */
	private CacheAccessor(Config config) throws SQLException {
		this.conMap = new HashMap<String, Connection>();
		this.config = config;
	}

	private Connection getConnection(String filename) throws SQLException {
		String part = getPartition(filename);
		if (!conMap.containsKey(part)) {
			createConnection(part);
		}
		return conMap.get(part);
	}

	private void createConnection(String name) throws SQLException {
		String dbFile = config.getTempDir() + "/" + String.format(DB_FILE_NAME, name);
		boolean isExist = new File(dbFile).exists();

		Connection con = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
		con.setAutoCommit(false);
		if (!isExist) {
			createTableIfNotExist(con);
		}

		this.conMap.put(name, con);
	}

	/**
	 * テーブルを作成する
	 *
	 * @throws SQLException
	 */
	private void createTableIfNotExist(Connection con) throws SQLException {
		Statement stmt = con.createStatement();

		String sql1 = "CREATE TABLE IF NOT EXISTS " + RESULT_TABLE + " ( "
				+ " key text PRIMARY KEY, "
				+ " file1 text, "
				+ " file2 text, "
				+ " timediff integer, "
				+ " hist real, "
				+ " feature real, "
				+ " skip integer "
				+ " );";
		stmt.execute(sql1);

		String sql2 = "CREATE INDEX file1index ON " + RESULT_TABLE + "(file1)";
		stmt.execute(sql2);

		String sql3 = "CREATE INDEX skipindex ON " + RESULT_TABLE + "(skip)";
		stmt.execute(sql3);
	}

	public Map<String, CachedComparison> selectCacheData(String file1) throws SQLException {
		Connection con = getConnection(file1);
		Map<String, CachedComparison> result = new HashMap<>();
		String sql = "SELECT * FROM " + RESULT_TABLE + " WHERE file1 = ?";
		PreparedStatement prep = con.prepareStatement(sql);
		prep.setString(1, file1);
		ResultSet rs = prep.executeQuery();
		while (rs.next()) {
			CachedComparison cc = new CachedComparison();
			cc.file1 = rs.getString(2);
			cc.file2 = rs.getString(3);
			cc.playtimeDiff = rs.getLong(4);
			cc.hist = rs.getDouble(5);
			cc.feature = rs.getDouble(6);
			cc.skip = rs.getInt(7);
			result.put(rs.getString(1), cc);
		}
		return result;
	}

	/**
	 * スキップフラグを立てる
	 *
	 * @param key
	 * @throws SQLException
	 */
	public void updateSkip(String key, String filename1) throws SQLException {
		Connection con = getConnection(filename1);
		String sql = "UPDATE " + RESULT_TABLE + " SET skip = 2 WHERE key = ?";
		PreparedStatement prep = con.prepareStatement(sql);
		prep.setString(1, key);
		System.out.println("upddated");
		//Statement stmt = conn.createStatement();
		prep.execute();
		con.commit();
	}

	/**
	 * データを更新する
	 *
	 * @param input
	 * @throws SQLException
	 */
	public void updateCache(String filename1, List<VideoComparison> input) throws SQLException {

		if (input == null || input.size() <= 0) {
			return;
		}

		String sql = "INSERT OR IGNORE INTO " + RESULT_TABLE + " VALUES ( ?, ?, ?, ?, ?, ?, ? ) ";
		Connection con = getConnection(filename1);
		PreparedStatement prep = con.prepareStatement(sql);

		int i = 0;
		for (VideoComparison comp : input) {

			prep.setString(1, comp.getKey());
			prep.setString(2, comp.getFilename1());
			prep.setString(3, comp.getFilename2());
			prep.setLong(4, comp.getPlaytime());
			prep.setDouble(5, comp.getHist());
			prep.setDouble(6, comp.getFeature());
			prep.setInt(7, comp.getSkip());
			prep.addBatch();

			if (i++ > 900) {
				System.out.println("commit");
				prep.executeBatch();
				con.commit();
				i = 0;
			}

		}
		prep.executeBatch();
		con.commit();
	}

	public void close() throws SQLException {
		//this.conn.close();
	}

	public List<VideoComparison> selectActive() throws SQLException {
		List<VideoComparison> activeList = new ArrayList<>();
		for (String key : conMap.keySet()) {
			Connection con = conMap.get(key);
			List<VideoComparison> deleteList = new ArrayList<>();
			String sql = "SELECT * FROM " + RESULT_TABLE + " WHERE skip = 0 ORDER BY hist DESC LIMIT 300";
			PreparedStatement prep = con.prepareStatement(sql);
			ResultSet rs = prep.executeQuery();
			while (rs.next()) {
				VideoComparison cc = new VideoComparison();
				cc.setFilename1(rs.getString(2));
				cc.setFilename2(rs.getString(3));
				cc.setPlaytime(rs.getLong(4));
				cc.setHist(rs.getDouble(5));
				cc.setFeature(rs.getDouble(6));
				cc.setSkip(rs.getInt(7));

				File file1 = new File(cc.getFilename1());
				File file2 = new File(cc.getFilename2());
				if ((file1.exists() && file2.exists())) {
					VideoMetadata meta1 = new VideoMetadata();
					meta1.setFilename(cc.getFilename1());
					meta1.setFrameDirname(
							config.getTempDir() + "/" + String.valueOf(cc.getFilename1().hashCode()).replace("-", "_"));
					cc.setVideo1(meta1);
					VideoMetadata meta2 = new VideoMetadata();
					meta2.setFilename(cc.getFilename2());
					meta2.setFrameDirname(
							config.getTempDir() + "/" + String.valueOf(cc.getFilename2().hashCode()).replace("-", "_"));
					cc.setVideo2(meta2);

					activeList.add(cc);
				} else {
					deleteList.add(cc);
				}
			}
			deleteCache(deleteList, con);
		}
		// hist基準でソート
		activeList.sort((arg0, arg1) -> {
			double diff = arg0.getHist() - arg1.getHist();
			if (diff < 0)
				return 1;
			else if (0 < diff)
				return -1;
			return 0;
		});
		return activeList;
	}

	public void deleteCache(List<VideoComparison> input, Connection con) throws SQLException {
		System.out.println("delete size:" + input.size());
		String sql = "DELETE FROM " + RESULT_TABLE + " WHERE KEY = ? ";
		PreparedStatement prep = con.prepareStatement(sql);
		int i = 0;
		for (VideoComparison comp : input) {
			prep.setString(1, comp.getKey());
			prep.addBatch();
			if (i++ > 900) {
				System.out.println("commit");
				prep.executeBatch();
				con.commit();
				i = 0;
			}
		}
		prep.executeBatch();
		con.commit();
	}

	private String getPartition(String filename) {
		return String.valueOf(filename.hashCode()).replace("-", "_").substring(0, 2);
	}

}
