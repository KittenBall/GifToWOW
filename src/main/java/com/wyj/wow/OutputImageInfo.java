package com.wyj.wow;

public class OutputImageInfo {
    public int width;
    public int height;
    public int col;
    public int row;
    public int type;
    public int cellWidth;
    public int cellHeight;
    public int frameWidth;
    public int frameHeight;

    public int getPixels() {
        return width * height;
    }

    @Override
    public String toString() {
        return "OutputImage{" +
                "width=" + width +
                ", height=" + height +
                ", col=" + col +
                ", row=" + row +
                ", type=" + type +
                ", cellWidth=" + cellWidth +
                ", cellHeight=" + cellHeight +
                ", frameWidth=" + frameWidth +
                ", frameHeight=" + frameHeight +
                '}';
    }
}
