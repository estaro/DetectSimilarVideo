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

/**
 * キャッシュの管理
 *
 * SQLiteで保存します。
 */
public class CacheAccessor {

	public static final String DB_FILE_NAME = "dsv.db";

	public static final String RESULT_TABLE = "compare_video";

	private Connection conn = null;

	private static CacheAccessor instance = null;

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
		String dbFile = config.getTempDir() + "/" + DB_FILE_NAME;
		conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
		conn.setAutoCommit(false);
		createTableIfNotExist();
	}

	/**
	 * テーブルを作成する
	 *
	 * @throws SQLException
	 */
	private void createTableIfNotExist() throws SQLException {
		String sql = "CREATE TABLE IF NOT EXISTS " + RESULT_TABLE + " ( "
				+ " key text PRIMARY KEY, "
				+ " file1 text, "
				+ " file2 text, "
				+ " timediff integer, "
				+ " hist real, "
				+ " feature real, "
				+ " skip integer "
				+ " );";
		Statement stmt = conn.createStatement();
		stmt.execute(sql);
	}

	/**
	 * 全データを取得する
	 * @param string
	 *
	 * @return
	 * @throws SQLException
	 */
	public Map<String, CachedComparison> selectCacheData() throws SQLException {
		Map<String, CachedComparison> result = new HashMap<>();
		String sql = "SELECT * FROM " + RESULT_TABLE;
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
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

	public Map<String, CachedComparison> selectCacheData(String file1) throws SQLException {
		Map<String, CachedComparison> result = new HashMap<>();
		String sql = "SELECT * FROM " + RESULT_TABLE + " WHERE file1 = ?";
		PreparedStatement prep = conn.prepareStatement(sql);
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
	 * PKで取得する
	 *
	 * @param key
	 * @return
	 * @throws SQLException
	 */
	public CachedComparison selectCacheDataByPk(String key) throws SQLException {
		String sql = "SELECT * FROM " + RESULT_TABLE + " WHERE key = ?";
		PreparedStatement prep = conn.prepareStatement(sql);
		prep.setString(1, key);

		ResultSet rs = prep.executeQuery();
		CachedComparison cc = new CachedComparison();
		boolean exists = false;
		while (rs.next()) {
			exists = true;
			cc.file1 = rs.getString(2);
			cc.file2 = rs.getString(3);
			cc.playtimeDiff = rs.getLong(4);
			cc.hist = rs.getDouble(5);
			cc.feature = rs.getDouble(6);
			cc.skip = rs.getInt(7);
		}
		if (!exists) {
			return null;
		}
		return cc;
	}

	/**
	 * スキップフラグを立てる
	 *
	 * @param key
	 * @throws SQLException
	 */
	public void updateSkip(String key) throws SQLException {
		String sql = "UPDATE " + RESULT_TABLE + " SET skip = 2 WHERE key = ?";
		PreparedStatement prep = conn.prepareStatement(sql);
		prep.setString(1, key);
		System.out.println("upddated");
		//Statement stmt = conn.createStatement();
		prep.execute();
		conn.commit();
	}

	/**
	 * データを更新する
	 *
	 * @param input
	 * @throws SQLException
	 */
	public void updateCache(List<VideoComparison> input) throws SQLException {

		String sql = "INSERT OR IGNORE INTO " + RESULT_TABLE + " VALUES ( ?, ?, ?, ?, ?, ?, ? ) ";
		PreparedStatement prep = conn.prepareStatement(sql);

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
				conn.commit();
				i = 0;
			}

		}
		prep.executeBatch();
		conn.commit();
	}

	public void close() throws SQLException {
		this.conn.close();
	}

	public List<VideoComparison> selectActive() throws SQLException {
		List<VideoComparison> resultList = new ArrayList<>();
		List<VideoComparison> deleteList = new ArrayList<>();
		String sql = "SELECT * FROM " + RESULT_TABLE + " WHERE skip = 0 ORDER BY hist DESC LIMIT 1000";
		PreparedStatement prep = conn.prepareStatement(sql);
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
				resultList.add(cc);
			} else {
				deleteList.add(cc);
			}
		}
		deleteCache(deleteList);

		return resultList;
	}

	public void deleteCache(List<VideoComparison> input) throws SQLException {
		System.out.println("delete size:" + input.size());
		String sql = "DELETE FROM " + RESULT_TABLE + " WHERE KEY = ? ";
		PreparedStatement prep = conn.prepareStatement(sql);
		int i = 0;
		for (VideoComparison comp : input) {
			prep.setString(1, comp.getKey());
			prep.addBatch();
			if (i++ > 900) {
				System.out.println("commit");
				prep.executeBatch();
				conn.commit();
				i = 0;
			}
		}
		prep.executeBatch();
		conn.commit();
	}

}
