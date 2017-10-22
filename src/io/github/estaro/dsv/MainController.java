package io.github.estaro.dsv;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import io.github.estaro.dsv.bean.Config;
import io.github.estaro.dsv.bean.ListItem;
import io.github.estaro.dsv.bean.TableItem;
import io.github.estaro.dsv.bean.VideoComparison;
import io.github.estaro.dsv.bean.VideoMetadata;
import io.github.estaro.dsv.bean.VideoMetadataPair;
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
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.DirectoryChooser;

public class MainController {

	@FXML // ResourceBundle that was given to the FXMLLoader
	private ResourceBundle resources;

	@FXML // URL location of the FXML file that was given to the FXMLLoader
	private URL location;

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
	 */
	@FXML
	void doExecuteAction(ActionEvent event) {
		System.out.println("doExecuteAction");
		// ------------------------------------------------------------------
		// 選択されたすべてのディレクトリの動画を読み込む
		// ------------------------------------------------------------------
		List<File> fileList = new ArrayList<>();
		for (ListItem item : listview.getItems()) {
			if (!item.isOn()) {
				continue;
			}
			String dirname = item.getName();
			File dir = new File(dirname);
			if (!dir.exists()) {
				continue;
			}
			fileList.addAll(Arrays.asList(dir.listFiles()));
		}
		List<VideoMetadata> metaList = fileList.parallelStream().map(file -> OpenCvProcessor.captureFrame(config,
				file.getAbsolutePath().replace("\\", "\\\\"))).filter(s -> s != null).collect(Collectors.toList());

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
				.map(pair -> OpenCvProcessor.compareImages(config, pair.video1, pair.video2)).filter(s -> s != null)
				.collect(Collectors.toList());

		// ------------------------------------------------------------------
		// 結果をTableViewに反映
		// ------------------------------------------------------------------
		// hist基準でソート
		comparedList.sort(new Comparator<VideoComparison>() {
			@Override
			public int compare(VideoComparison arg0, VideoComparison arg1) {
				double diff = arg0.getHist() - arg1.getHist();
				if (diff < 0) {
					return -1;
				} else if (0 < diff) {
					return 1;
				}
				return 0;
			}
		});
		List<TableItem> tableItemList = comparedList.stream().map(item -> new TableItem(item))
				.collect(Collectors.toList());
		cola.setCellValueFactory(new PropertyValueFactory<>("dir1"));
		colb.setCellValueFactory(new PropertyValueFactory<>("dir2"));
		coltime.setCellValueFactory(new PropertyValueFactory<>("time"));
		colhist.setCellValueFactory(new PropertyValueFactory<>("hist"));
		colfeature.setCellValueFactory(new PropertyValueFactory<>("feature"));

		ObservableList<TableItem> data = FXCollections.observableArrayList(tableItemList);
		table.setItems(data);

	}

	@FXML // This method is called by the FXMLLoader when initialization is complete
	void initialize() {
		config = ConfigParser.load();
		for (String dirname : config.getDirList()) {
			ListItem item = new ListItem(dirname, true);
			listview.getItems().add(item);
		}
		listview.setCellFactory(CheckBoxListCell.forListView((ListItem item) -> item.onProperty()));

		table.getSelectionModel().selectedIndexProperty().addListener((ob, old, current) -> {
			TableItem item = table.getSelectionModel().getSelectedItem();
			VideoComparison comp = item.getOrg();

			img00.setImage(getImage(comp.getVideo1().getFrameDirname() + "/1.jpg"));
			img10.setImage(getImage(comp.getVideo1().getFrameDirname() + "/2.jpg"));
			img20.setImage(getImage(comp.getVideo1().getFrameDirname() + "/3.jpg"));
			img01.setImage(getImage(comp.getVideo1().getFrameDirname() + "/4.jpg"));
			img11.setImage(getImage(comp.getVideo1().getFrameDirname() + "/5.jpg"));
			img21.setImage(getImage(comp.getVideo1().getFrameDirname() + "/6.jpg"));
			img02.setImage(getImage(comp.getVideo1().getFrameDirname() + "/7.jpg"));
			img12.setImage(getImage(comp.getVideo1().getFrameDirname() + "/8.jpg"));
			img22.setImage(getImage(comp.getVideo1().getFrameDirname() + "/9.jpg"));

			img30.setImage(getImage(comp.getVideo2().getFrameDirname() + "/1.jpg"));
			img40.setImage(getImage(comp.getVideo2().getFrameDirname() + "/2.jpg"));
			img50.setImage(getImage(comp.getVideo2().getFrameDirname() + "/3.jpg"));
			img31.setImage(getImage(comp.getVideo2().getFrameDirname() + "/4.jpg"));
			img41.setImage(getImage(comp.getVideo2().getFrameDirname() + "/5.jpg"));
			img51.setImage(getImage(comp.getVideo2().getFrameDirname() + "/6.jpg"));
			img32.setImage(getImage(comp.getVideo2().getFrameDirname() + "/7.jpg"));
			img42.setImage(getImage(comp.getVideo2().getFrameDirname() + "/8.jpg"));
			img52.setImage(getImage(comp.getVideo2().getFrameDirname() + "/9.jpg"));

		});
	}

	private Image getImage(String filename) {
		return new Image(new File(filename).toURI().toString());
	}
}
