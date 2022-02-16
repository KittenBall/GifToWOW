package com.wyj.wow.ui;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXSlider;
import com.wyj.wow.GifConverter;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class App extends Application implements GifConverter.GifConverterListener {

    public static final double WINDOW_HEIGHT = 550;
    public static final double WINDOW_WIDTH = 800;
    private Window mWindow;
    private AnchorPane mRoot;
    private File mGifFile;
    private File mDesDir = FileSystemView.getFileSystemView().getHomeDirectory();
    private GifConverter mGifConverter;
    private ImageView mGifView;
    private Text mInfo;
    private Timer mCancelInfoTimer;
    private StackPane mGifContainer;
    private Text mGifInfo;
    private Text mOutputDir;
    private JFXSlider mScaleSlider;
    private JFXCheckBox mPlaceAvgCheckBox;
    private JFXSlider mColsSlider;

    public App() {
        mGifConverter = new GifConverter(this);
    }

    @Override
    public void start(Stage stage) throws Exception {
        mWindow = stage;
        stage.setTitle("Gif Converter 1.0");
        stage.getIcons().add(new Image("/logo.png"));
        Scene scene = new Scene(createContent(), WINDOW_WIDTH, WINDOW_HEIGHT);
        scene.getStylesheets().add("css.css");
        stage.setScene(scene);
        stage.show();
    }

    private Parent createContent() {
        mRoot = new AnchorPane();
        mRoot.setBackground(new Background(new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, Insets.EMPTY)));
        mRoot.setPadding(new Insets(20));
        createGifFrame();
        createInfos();
        createButtons();
        return mRoot;
    }

    private void createButtons() {
        Button chooseGifButton = new Button("选择Gif");
        chooseGifButton.setPrefHeight(24);
        chooseGifButton.setPrefWidth(144);
        chooseGifButton.setAlignment(Pos.CENTER);
        chooseGifButton.setOnAction(actionEvent -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("选择Gif");
            fileChooser.setInitialDirectory(mGifFile == null ? FileSystemView.getFileSystemView().getHomeDirectory() : mGifFile.getParentFile());
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Gif(*.gif)", "*.gif"));
            onGifSelected(fileChooser.showOpenDialog(mWindow));
        });

        AnchorPane.setBottomAnchor(chooseGifButton, 0d);
        AnchorPane.setRightAnchor(chooseGifButton, 0d);
        mRoot.getChildren().add(chooseGifButton);

        Button chooseDesButton = new Button("选择输出文件夹");
        chooseDesButton.setPrefHeight(24);
        chooseDesButton.setPrefWidth(144);
        chooseDesButton.setAlignment(Pos.CENTER);
        chooseDesButton.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setInitialDirectory(mDesDir);
            directoryChooser.setTitle("选择输出文件夹");
            onDesDirSelected(directoryChooser.showDialog(mWindow));
        });

        AnchorPane.setBottomAnchor(chooseDesButton, 0d);
        AnchorPane.setRightAnchor(chooseDesButton, 155d);
        mRoot.getChildren().add(chooseDesButton);

        Button outputButton = new Button("转换");
        outputButton.setPrefHeight(24);
        outputButton.setPrefWidth(144);
        outputButton.setAlignment(Pos.CENTER);
        outputButton.setOnAction(actionEvent -> {
            mGifConverter.startOutput(mDesDir, (float) (mScaleSlider.getValue() / 100), mPlaceAvgCheckBox.isSelected(), (int) mColsSlider.getValue());
        });

        AnchorPane.setBottomAnchor(outputButton, 36d);
        AnchorPane.setRightAnchor(outputButton, 0d);
        mRoot.getChildren().add(outputButton);

        VBox scaleContainer = new VBox();
        scaleContainer.setSpacing(10);
        AnchorPane.setBottomAnchor(scaleContainer, 36d);
        AnchorPane.setRightAnchor(scaleContainer, 155d);
        mRoot.getChildren().add(scaleContainer);

        Label scaleLabel = new Label("缩放比例（百分比）");
        scaleContainer.getChildren().add(scaleLabel);

        mScaleSlider = new JFXSlider();
        mScaleSlider.setPrefHeight(24);
        mScaleSlider.setPrefWidth(144);
        mScaleSlider.setValue(60);
        mScaleSlider.setMin(1);
        mScaleSlider.setMax(100);
        scaleContainer.getChildren().add(mScaleSlider);

        mPlaceAvgCheckBox = new JFXCheckBox("均匀放置");
        mPlaceAvgCheckBox.setTextFill(Color.valueOf("#63a4ff"));
        AnchorPane.setBottomAnchor(mPlaceAvgCheckBox, 72d);
        AnchorPane.setRightAnchor(mPlaceAvgCheckBox, 72d);
        mRoot.getChildren().add(mPlaceAvgCheckBox);

        VBox colsContainer = new VBox();
        colsContainer.setSpacing(10);
        AnchorPane.setBottomAnchor(colsContainer, 36d);
        AnchorPane.setRightAnchor(colsContainer, 320d);
        mRoot.getChildren().add(colsContainer);

        Label colsLabel = new Label("列数（为0则自动计算）");
        colsContainer.getChildren().add(colsLabel);

        mColsSlider = new JFXSlider();
        mColsSlider.setPrefHeight(24);
        mColsSlider.setPrefWidth(144);
        mColsSlider.setValue(0);
        mColsSlider.setMin(0);
        mColsSlider.setMax(20);
        colsContainer.getChildren().add(mColsSlider);
    }

    private void createInfos() {
        mGifInfo = new Text();
        AnchorPane.setLeftAnchor(mGifInfo, 0d);
        mRoot.getChildren().add(mGifInfo);

        mInfo = new Text();
        mInfo.setVisible(false);
        AnchorPane.setRightAnchor(mInfo, 0d);
        mRoot.getChildren().add(mInfo);

        mGifContainer.heightProperty().addListener((observable, oldValue, newValue) -> {
            AnchorPane.setTopAnchor(mGifInfo, newValue.doubleValue() + 20);
            AnchorPane.setTopAnchor(mInfo, newValue.doubleValue() + 20);
        });

        mOutputDir = new Text();
        mOutputDir.setText(mDesDir.getAbsolutePath());
        mOutputDir.maxWidth(250);
        mOutputDir.setTextAlignment(TextAlignment.RIGHT);
        AnchorPane.setRightAnchor(mOutputDir, 320d);
        AnchorPane.setBottomAnchor(mOutputDir, 5d);
        mRoot.getChildren().add(mOutputDir);
    }

    private void createGifFrame() {
        mGifContainer = new StackPane();
        mGifContainer.setBorder(new Border(new BorderStroke(Color.valueOf("#dedede"), BorderStrokeStyle.SOLID, new CornerRadii(4), new BorderWidths(1))));
        AnchorPane.setLeftAnchor(mGifContainer, 0d);
        AnchorPane.setRightAnchor(mGifContainer, 0d);
        AnchorPane.setTopAnchor(mGifContainer, 0d);
        AnchorPane.setBottomAnchor(mGifContainer, 150d);

        mGifView = new ImageView();
        StackPane.setAlignment(mGifView, Pos.CENTER);
        mGifContainer.getChildren().add(mGifView);
        mRoot.getChildren().add(mGifContainer);
    }

    private void onGifSelected(File gifFile) {
        if (gifFile == null) {
            return;
        }
        mGifFile = gifFile;
        mGifConverter.setGifFile(gifFile);
        Image image = new Image("file:///" + gifFile.getAbsolutePath(), mGifContainer.getWidth() - 20, mGifContainer.getHeight() - 20, true, true, true);
        image.exceptionProperty().addListener((observable, oldValue, newValue) -> {
            newValue.printStackTrace();
        });
        mGifView.setImage(image);
    }

    private void onDesDirSelected(File desDir) {
        if (desDir == null || !desDir.isDirectory()) {
            return;
        }
        mDesDir = desDir;
        mOutputDir.setText(mDesDir.getAbsolutePath());
    }

    private void showError(String error) {
        if (mCancelInfoTimer != null) {
            mCancelInfoTimer.cancel();
        }
        mCancelInfoTimer = new Timer();
        if (isEmptyText(error)) {
            mInfo.setVisible(false);
        } else {
            mCancelInfoTimer.schedule(new CancelInfoTask(), 3000);
            mInfo.setFill(Color.RED);
            mInfo.setText(error);
            mInfo.setVisible(true);
        }
    }

    private void showInfo(String info) {
        if (mCancelInfoTimer != null) {
            mCancelInfoTimer.cancel();
        }
        mCancelInfoTimer = new Timer();
        if (isEmptyText(info)) {
            mInfo.setVisible(false);
        } else {
            mCancelInfoTimer.schedule(new CancelInfoTask(), 3000);
            mInfo.setFill(Color.valueOf("#63a4ff"));
            mInfo.setText(info);
            mInfo.setVisible(true);
        }
    }

    private boolean isEmptyText(String text) {
        return text == null || text.length() <= 0;
    }

    @Override
    public void onProcessStart() {
        showInfo("正在处理");
    }

    @Override
    public void onProcessError(Throwable throwable) {
        showError(throwable.getLocalizedMessage());
    }

    @Override
    public void onProcessComplete() {
        showInfo("处理完成");
        StringBuilder sb = new StringBuilder();
        sb.append("每秒帧数:").append(mGifConverter.getFps()).append("\n");
        sb.append("总帧数：").append(mGifConverter.getFrameCount()).append("\n");
        sb.append("图片宽高：").append(mGifConverter.getImageWidth()).append("x").append(mGifConverter.getImageHeight());
        mGifInfo.setText(sb.toString());
    }

    @Override
    public void onConvertStart() {
        showInfo("正在转换");
    }

    @Override
    public void onConvertComplete() {
        showInfo("转换成功");
        mGifInfo.setText(null);
        mGifConverter.setGifFile(null);
        mGifView.setImage(null);
    }

    @Override
    public void onConvertError(Throwable throwable) {
        showError(throwable.getLocalizedMessage());
    }

    private class CancelInfoTask extends TimerTask {

        @Override
        public void run() {
            mInfo.setVisible(false);
        }
    }
}
