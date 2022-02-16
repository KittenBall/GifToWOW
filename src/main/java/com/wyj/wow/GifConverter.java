package com.wyj.wow;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_java;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgcodecs.Imgcodecs.imwrite;

public class GifConverter {

    private File mGifFile;
    private FrameGrabber mGifGrabber;
    private GifConverterListener mListener;

    static {
        Loader.load(opencv_java.class);
    }

    public GifConverter(GifConverterListener listener) {
        mListener = listener;
    }

    public void setGifFile(File file) {
        mGifFile = file;
        processGif();
    }

    private void processGif() {
        try {
            if (mGifGrabber != null) {
                mGifGrabber.stop();
            }
            if (mGifFile == null || !mGifFile.exists()) {
                return;
            }
            mListener.onProcessStart();
            mGifGrabber = new FFmpegFrameGrabber(mGifFile);
            mGifGrabber.setImageMode(FrameGrabber.ImageMode.RAW);
            mGifGrabber.start();
            mListener.onProcessComplete();
        } catch (Exception e) {
            e.printStackTrace();
            mListener.onProcessError(e);
        }
    }

    public double getFps() {
        return mGifGrabber.getFrameRate();
    }

    public double getFrameCount() {
        return mGifGrabber.getLengthInFrames();
    }

    public int getImageWidth() {
        return mGifGrabber.getImageWidth();
    }

    public int getImageHeight() {
        return mGifGrabber.getImageHeight();
    }

    public void startOutput(File desDir, float scale, boolean placeAvg, int cols) {
        if (mGifFile == null || !mGifFile.exists()) {
            mListener.onConvertError(new IllegalArgumentException("请选择Gif"));
            return;
        }
        if (mGifGrabber.getLengthInFrames() <= 0) {
            mListener.onConvertError(new IllegalArgumentException("请选择总帧数>0的Gif"));
            return;
        }
        if (!desDir.exists() || !desDir.isDirectory()) {
            mListener.onConvertError(new IllegalArgumentException("目标文件夹不存在"));
            return;
        }
        try {
            mListener.onConvertStart();
            List<Mat> srcMats = new ArrayList<>();
            int scaleWidth = (int) (getImageWidth() * scale);
            int scaleHeight = (int) (getImageHeight() * scale);
            OpenCVFrameConverter.ToOrgOpenCvCoreMat converter = new OpenCVFrameConverter.ToOrgOpenCvCoreMat();
            for (int i = 0; i < mGifGrabber.getLengthInFrames(); i++) {
                Frame frame = mGifGrabber.grab();
                if (frame == null) {
                    continue;
                }
                Mat mat = converter.convert(frame.clone());
                Mat scaledMat = new Mat();
                Size size = new Size(scaleWidth, scaleHeight);
                Imgproc.resize(mat, scaledMat, size);
                srcMats.add(scaledMat);
            }

            if (srcMats.size() <= 0) {
                mListener.onConvertError(new IllegalArgumentException("没有获取到有效帧"));
                return;
            }

            OutputImageInfo optImgInfo = calcMostSuitableImageInfo(srcMats, scaleWidth, scaleHeight, cols);
            Mat result = Mat.zeros(optImgInfo.height, optImgInfo.width, optImgInfo.type);
            for (int i = 0; i < optImgInfo.row; i++) {
                for (int j = i * optImgInfo.col; j < i * optImgInfo.col + optImgInfo.col; j++) {
                    if (j < srcMats.size()) {
                        Mat mat = srcMats.get(j);
                        int left, top;
                        if (placeAvg) {
                            left = (j % optImgInfo.col) * optImgInfo.cellWidth + (optImgInfo.cellWidth - optImgInfo.frameWidth) / 2;
                            top = i * optImgInfo.cellHeight + (optImgInfo.cellHeight - optImgInfo.frameHeight) / 2;
                        } else {
                            left = j % optImgInfo.col * optImgInfo.frameWidth;
                            top = i * optImgInfo.frameHeight;
                        }
                        mat.copyTo(result.submat(top, top + mat.height(), left, left + mat.width()));
                    }
                }
            }

            String gifFileName = mGifFile.getName().replace(".gif", "");

            File pngFile = new File(desDir, gifFileName + ".png");
            imwrite(pngFile.getAbsolutePath(), result);
            mGifGrabber.stop();

            BufferedImage pngData = ImageIO.read(pngFile);
            if (ImageIO.write(pngData, "TGA", new File(desDir, gifFileName + ".tga"))) {
                pngFile.delete();
                mListener.onConvertComplete();
            } else {
                mListener.onConvertError(new IllegalAccessError("输出图片失败！"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            mListener.onConvertError(e);
        }
    }

    private OutputImageInfo calcMostSuitableImageInfo(List<Mat> srcMats, int width, int height, int cols) {
        OutputImageInfo optImgInfo = new OutputImageInfo();
        optImgInfo.frameWidth = width;
        optImgInfo.frameHeight = height;
        optImgInfo.type = srcMats.get(0).type();

        if (cols > 0) {
            fillOptImgInfo(srcMats, width, height, cols, optImgInfo);
        } else {
            cols = Math.min(srcMats.size(), (int) Math.ceil(srcMats.size() / 2d));
            for (int i = cols; i > 0; i--) {
                fillOptImgInfo(srcMats, width, height, i, optImgInfo);
            }
        }
        return optImgInfo;
    }

    private void fillOptImgInfo(List<Mat> srcMats, int width, int height, int cols, OutputImageInfo optImgInfo) {
        int row = (int) Math.ceil(srcMats.size() / (double) cols);
        int realWidth = width * cols;
        int realHeight = height * row;
        double neededWidth = Math.pow(2, Math.ceil(Math.log(realWidth) / Math.log(2)));
        double neededHeight = Math.pow(2, Math.ceil(Math.log(realHeight) / Math.log(2)));
        double neededPixels = neededWidth * neededHeight;
        if (optImgInfo.getPixels() <= 0 || neededPixels < optImgInfo.getPixels()) {
            optImgInfo.col = cols;
            optImgInfo.row = row;
            optImgInfo.width = (int) neededWidth;
            optImgInfo.height = (int) neededHeight;
            optImgInfo.cellWidth = (int) (neededWidth / cols);
            optImgInfo.cellHeight = (int) (neededHeight / Math.floor(neededHeight / height));
        }
    }

    public interface GifConverterListener {

        void onProcessStart();

        void onProcessError(Throwable throwable);

        void onProcessComplete();

        void onConvertStart();

        void onConvertComplete();

        void onConvertError(Throwable throwable);
    }
}
