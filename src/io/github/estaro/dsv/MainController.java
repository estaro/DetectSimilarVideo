package io.github.estaro.dsv;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import io.github.estaro.dsv.bean.CachedComparison;
import io.github.estaro.dsv.bean.Config;
import io.github.estaro.dsv.bean.ListItem;
import io.github.estaro.dsv.bean.TableItem;
import io.github.estaro.dsv.bean.VideoComparison;
import io.github.estaro.dsv.bean.VideoMetadata;
import io.github.estaro.dsv.bean.VideoMetadataPair;
import io.github.estaro.dsv.logic.CacheAccessor;
import io.github.estaro.dsv.logic.OpenCvProcessor;
import io.github.estaro.dsv.util.ConfigParser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;

public class MainController {

	static final String[] VIDEO_EXT = { ".asf", ".avi", ".flv", ".mkv", ".mp4", ".wmv" };

	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

	@FXML
	private Label label1;

	@FXML
	private Label label2;

	@FXML
	private ListView<ListItem> listview;

	@FXML
	private TableView<TableItem> table;

	@FXML
	private TableColumn<TableItem, String> cola;

	@FXML
	private TableColumn<TableItem, String> colb;

	@FXML
	private TableColumn<TableItem, Long> coltime;

	@FXML
	private TableColumn<TableItem, Double> colhist;

	@FXML
	private TableColumn<TableItem, Double> colfeature;

	@FXML
	private ProgressBar progress;

	@FXML
	private TextArea logview;

	@FXML
	private ImageView img10;

	@FXML
	private ImageView img20;

	@FXML
	private ImageView img01;

	@FXML
	private ImageView img11;

	@FXML
	private ImageView img21;

	@FXML
	private ImageView img02;

	@FXML
	private ImageView img12;

	@FXML
	private ImageView img22;

	@FXML
	private ImageView img30;

	@FXML
	private ImageView img40;

	@FXML
	private ImageView img50;

	@FXML
	private ImageView img31;

	@FXML
	private ImageView img41;

	@FXML
	private ImageView img51;

	@FXML
	private ImageView img32;

	@FXML
	private ImageView img42;

	@FXML
	private ImageView img52;

	@FXML
	private ImageView img00;

	private Config config;

	/**
	 * [追加]ボタン
	 * ディレクトリを追加する
	 * @param event
	 */
	@FXML
	void doAddAction(ActionEvent event) {
		// ------------------------------------------------------------------
		// ディレクトリ選択ダイアログ
		// ------------------------------------------------------------------
		Node node = (Node) event.getSource();
		DirectoryChooser dchooser = new DirectoryChooser();
		File selectedDir = dchooser.showDialog(node.getScene().getWindow());
		if (selectedDir != null) {
			String dir = selectedDir.getAbsolutePath();
			ListItem item = new ListItem(dir, true);
			listview.getItems().add(item);
			config.getDirList().add(dir);
			ConfigParser.save(config);
		}
	}

	/**
	 * [削除]ボタン
	 * @param event
	 */
	@FXML
	void doDeleteAction(ActionEvent event) {
		// ------------------------------------------------------------------
		// 選択されたすべてのディレクトリの削除する
		// ------------------------------------------------------------------
		List<ListItem> selected = new ArrayList<>();
		List<ListItem> noSelected = new ArrayList<>();
		List<String> newConfig = new ArrayList<>();
		for (ListItem item : listview.getItems()) {
			if (!item.isOn()) {
				noSelected.add(item);
				newConfig.add(item.getName());
				continue;
			}
			selected.add(item);
		}

		// ------------------------------------------------------------------
		// 確認ダイアログ
		// ------------------------------------------------------------------
		Alert alert = new Alert(AlertType.CONFIRMATION, "選択しているフォルダを削除します");
		alert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				// 消して作り直す
				listview.getItems().clear();
				listview.getItems().addAll(noSelected);
				config.setDirList(newConfig);
				ConfigParser.save(config);
			}
		});
	}

	/**
	 * [実行]ボタン
	 *
	 * @param event
	 * @throws SQLException
	 */
	@FXML
	void doExecuteAction(ActionEvent event) throws IOException, SQLException {
		System.out.println("doExecuteAction");

		// 前の結果のクリア
		table.getItems().clear();

		long start = System.currentTimeMillis();
		// ------------------------------------------------------------------
		// 選択されたすべてのディレクトリの動画を読み込む
		// ------------------------------------------------------------------
		List<File> fileList = new ArrayList<>();
		for (ListItem item : listview.getItems()) {
			if (!item.isOn()) {
				continue;
			}
			String dirname = item.getName();
			System.out.println(dirname);
			File dir = new File(dirname);
			if (!dir.exists()) {
				continue;
			}
			fileList.addAll(getAllChildren(dir));
		}
		System.out.println("対象ファイル数:" + fileList.size());

		List<VideoMetadata> metaList = fileList.parallelStream().map(file -> {
			try {
				return OpenCvProcessor.captureFrame(config,
						file.getAbsolutePath().replace("\\", "\\\\"));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}).filter(s -> s != null).collect(Collectors.toList());
		metaList.sort((arg0, arg1) -> {
			return arg0.getFilename().compareTo(arg1.getFilename());
		});

		// ------------------------------------------------------------------
		// キャッシュ情報を読み込む
		// ------------------------------------------------------------------
		CacheAccessor cache = CacheAccessor.getInstace(config);
		final Map<String, CachedComparison> cachedComp = cache.selectCacheData();
		System.out.println("cache size:" + cachedComp.size());

		// ------------------------------------------------------------------
		// 読み込んだ動画を比較
		// ------------------------------------------------------------------
		List<VideoMetadataPair> metadataPairList = new ArrayList<>();
		int metaSize = metaList.size();
		for (int i = 0; i < metaSize; i++) {
			for (int j = i + 1; j < metaSize; j++) {
				metadataPairList.add(new VideoMetadataPair(metaList.get(i), metaList.get(j)));
			}
		}
		List<VideoComparison> comparedList = metadataPairList.parallelStream()
				.map(pair -> {
					try {
						return OpenCvProcessor.compareImages(config, cachedComp, pair.video1, pair.video2);
					} catch (IOException e) {
						e.printStackTrace();
						return null;
					}
				})
				.filter(s -> s != null)
				.collect(Collectors.toList());

		// ------------------------------------------------------------------
		// 結果をキャッシュとして保存
		// ------------------------------------------------------------------
		cache.updateCache(comparedList);
		System.out.println("new cache size:" + comparedList.size());

		// ------------------------------------------------------------------
		// 結果をTableViewに反映
		// ------------------------------------------------------------------
		List<VideoComparison> notSkippedList = comparedList.parallelStream()
				.filter(s -> s.getSkip() == 0)
				.collect(Collectors.toList());
		System.out.println("no skip list size:" + notSkippedList.size());

		// hist基準でソート
		notSkippedList.sort((arg0, arg1) -> {
			double diff = arg0.getHist() - arg1.getHist();
			if (diff < 0)
				return 1;
			else if (0 < diff)
				return -1;
			return 0;
		});

		// 結果出力数を制限する
		int resultSize = notSkippedList.size();
		if (resultSize > 500) {
			resultSize = 500;
		}
		List<VideoComparison> resultList = notSkippedList.subList(0, resultSize);

		List<TableItem> tableItemList = resultList.stream().map(item -> new TableItem(item))
				.collect(Collectors.toList());
		cola.setCellValueFactory(new PropertyValueFactory<>("dir1"));
		colb.setCellValueFactory(new PropertyValueFactory<>("dir2"));
		coltime.setCellValueFactory(new PropertyValueFactory<>("time"));
		colhist.setCellValueFactory(new PropertyValueFactory<>("hist"));
		colfeature.setCellValueFactory(new PropertyValueFactory<>("feature"));

		ObservableList<TableItem> data = FXCollections.observableArrayList(tableItemList);
		table.setItems(data);

		// ------------------------------------------------------------------
		// 終了メッセージ
		// ------------------------------------------------------------------
		long end = System.currentTimeMillis();
		Alert alert = new Alert(AlertType.INFORMATION, "処理が完了しました (" + (end - start) + ")ms");
		alert.showAndWait();
	}

	/**
	 * 画像相違チェックボタン
	 *
	 * @param event
	 * @throws SQLException
	 */
	@FXML
	void doCheck(ActionEvent event) throws SQLException {
		TableItem item = table.getSelectionModel().getSelectedItem();
		if (item != null) {
			VideoComparison comp = item.getOrg();
			CacheAccessor cache = CacheAccessor.getInstace(config);
			cache.updateSkip(comp.getKey());
			table.getSelectionModel().selectNext();
		}
	}

	/**
	 * ファイル削除ボタン1(左)
	 * @param event
	 */
	@FXML
	void doDelFile1(ActionEvent event) {
		TableItem item = table.getSelectionModel().getSelectedItem();
		if (item != null) {
			VideoComparison comp = item.getOrg();
			File file = new File(comp.getFilename1());
			if (file.exists()) {
				Alert alert = new Alert(AlertType.CONFIRMATION, comp.getFilename1() + "を削除します。");
				alert.showAndWait().ifPresent(response -> {
					if (response == ButtonType.OK) {
						file.delete();
					}
				});
			} else {
				Alert alert = new Alert(AlertType.INFORMATION, "ファイルが存在しません。");
				alert.showAndWait();
			}
		}
	}

	/**
	 * ファイル削除ボタン2(右)
	 * @param event
	 */
	@FXML
	void doDelFile2(ActionEvent event) {
		TableItem item = table.getSelectionModel().getSelectedItem();
		if (item != null) {
			VideoComparison comp = item.getOrg();
			File file = new File(comp.getFilename2());
			if (file.exists()) {
				Alert alert = new Alert(AlertType.CONFIRMATION, comp.getFilename2() + "を削除します。");
				alert.showAndWait().ifPresent(response -> {
					if (response == ButtonType.OK) {
						file.delete();
					}
				});
			} else {
				Alert alert = new Alert(AlertType.INFORMATION, "ファイルが存在しません。");
				alert.showAndWait();
			}
		}
	}

	/**
	 * 動画再生(左)
	 * @param event
	 * @throws IOException
	 */
	@FXML
	void doPlay1(ActionEvent event) throws IOException {
		TableItem item = table.getSelectionModel().getSelectedItem();
		if (item != null) {
			VideoComparison comp = item.getOrg();
			//TODO 実行プログラムの非固定化
			Runtime.getRuntime().exec("\"C:\\Program Files\\MPC-HC\\mpc-hc64.exe\" " + comp.getFilename1());
		}
	}

	/**
	 * 動画再生(右)
	 * @param event
	 * @throws IOException
	 */
	@FXML
	void doPlay2(ActionEvent event) throws IOException {
		TableItem item = table.getSelectionModel().getSelectedItem();
		if (item != null) {
			VideoComparison comp = item.getOrg();
			//TODO 実行プログラムの非固定化
			Runtime.getRuntime().exec("\"C:\\Program Files\\MPC-HC\\mpc-hc64.exe\" " + comp.getFilename2());
		}
	}

	/**
	 * 選択↓
	 * @param event
	 */
	@FXML
	void doSelectDown(ActionEvent event) {
		table.getSelectionModel().selectNext();
	}

	/**
	 * 選択↑
	 * @param event
	 */
	@FXML
	void doSelectUp(ActionEvent event) {
		table.getSelectionModel().selectPrevious();
	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		config = ConfigParser.load();
		for (String dirname : config.getDirList()) {
			ListItem item = new ListItem(dirname, true);
			listview.getItems().add(item);
		}
		listview.setCellFactory(CheckBoxListCell.forListView((ListItem item) -> item.onProperty()));

		ContextMenu menu = new ContextMenu();
		MenuItem mi = new MenuItem("エクスプローラで開く");
		mi.setOnAction(event -> {
			TableItem item = table.getSelectionModel().getSelectedItem();
			String command1 = "explorer /select," + item.getOrg().getVideo1().getFilename().replace("\\\\", "\\");
			String command2 = "explorer /select," + item.getOrg().getVideo2().getFilename().replace("\\\\", "\\");
			try {
				Runtime.getRuntime().exec(command1);
				Runtime.getRuntime().exec(command2);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		menu.getItems().add(mi);
		table.setContextMenu(menu);

		table.getSelectionModel().selectedIndexProperty().addListener((ob, old, current) -> {
			TableItem item = table.getSelectionModel().getSelectedItem();
			if (item != null) {
				VideoComparison comp = item.getOrg();

				img00.setImage(getImage(comp.getVideo1().getImagefile(1)));
				img10.setImage(getImage(comp.getVideo1().getImagefile(2)));
				img20.setImage(getImage(comp.getVideo1().getImagefile(3)));
				img01.setImage(getImage(comp.getVideo1().getImagefile(4)));
				img11.setImage(getImage(comp.getVideo1().getImagefile(5)));
				img21.setImage(getImage(comp.getVideo1().getImagefile(6)));
				img02.setImage(getImage(comp.getVideo1().getImagefile(7)));
				img12.setImage(getImage(comp.getVideo1().getImagefile(8)));
				img22.setImage(getImage(comp.getVideo1().getImagefile(9)));

				img30.setImage(getImage(comp.getVideo2().getImagefile(1)));
				img40.setImage(getImage(comp.getVideo2().getImagefile(2)));
				img50.setImage(getImage(comp.getVideo2().getImagefile(3)));
				img31.setImage(getImage(comp.getVideo2().getImagefile(4)));
				img41.setImage(getImage(comp.getVideo2().getImagefile(5)));
				img51.setImage(getImage(comp.getVideo2().getImagefile(6)));
				img32.setImage(getImage(comp.getVideo2().getImagefile(7)));
				img42.setImage(getImage(comp.getVideo2().getImagefile(8)));
				img52.setImage(getImage(comp.getVideo2().getImagefile(9)));

				label1.setText(comp.getVideo1().getMetadataLabel());
				label2.setText(comp.getVideo2().getMetadataLabel());
			}
		});
	}

	private Image getImage(String filename) {
		return new Image(new File(filename).toURI().toString());
	}

	private List<File> getAllChildren(File dir) {
		List<File> result = new ArrayList<File>();
		LinkedList<File> remains = new LinkedList<File>(Arrays.asList(dir.listFiles()));
		while (!remains.isEmpty()) {
			File f = remains.pollFirst();
			if (f.isDirectory()) {
				remains.addAll(0, Arrays.asList(f.listFiles()));
			} else {
				for (int i = 0; i < VIDEO_EXT.length; i++) {
					if (f.getName().toLowerCase().endsWith(VIDEO_EXT[i])) {
						result.add(f);
						break;
					}
				}
			}
		}
		return result;
	}
}
