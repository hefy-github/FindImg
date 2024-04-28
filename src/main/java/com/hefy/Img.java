package com.hefy;

import lombok.Data;
import org.apache.commons.codec.digest.DigestUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Data
public class Img {
    private String path; // 图片根路径
    private int[] image; // 图片色值队列数组
    private int width; // 图片宽度
    private int height; // 图片高度
    private Map<Integer, ArrayList<Integer>> colorMap; // 图片色值分布
    private int bgc; // 背景颜色
    private String md5; // 图片 md5 标识

    // public Img(){}

    public Img (String path) {
        this.path = path;
    }


    /**
     * 初始化函数，用于读取BufferedImage图像信息，并进行颜色映射的初始化。
     * @param imageBuffer 输入的BufferedImage图像对象，用于提取图像尺寸和颜色信息。
     */
    public Img init(BufferedImage imageBuffer){
        width = imageBuffer.getWidth();
        height = imageBuffer.getHeight();

        colorMap = new HashMap<>();
        image = new int[width * height];

        ArrayList<Integer> posList;
        int tempColor, maxCount = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                tempColor = imageBuffer.getRGB(x, y);
                // File cFile = new File(colorModuleDir + "/" + tempColor + ".txt");
                // if(cFile.exists()){
                //   try {
                //     tempColor = Integer.parseInt(
                //             new String(Files.readAllBytes(cFile.toPath()))
                //     );
                //   } catch (IOException e) { throw new RuntimeException(e); }
                // }

                image[width * y + x] = tempColor;

                if(!colorMap.containsKey(tempColor)){ colorMap.put(tempColor, new ArrayList<Integer>()); }
                posList = colorMap.get(tempColor);
                posList.add(width * y + x);

                if(posList.size() > maxCount){ maxCount = posList.size(); bgc = tempColor; }
            }
        }

        return this;
    }

    /**
     * 初始化函数，用于加载指定文件名的图像文件。
     * 如果文件名中不包含扩展名，则默认添加 .png 作为扩展名。
     * 如果文件已存在，则尝试使用该文件初始化图像。
     *
     * @param fileName 要加载的图像文件的名称，可以不包含扩展名。
     */
    public Img init(String fileName) {
        // 如果文件名中没有扩展名，则默认添加.png扩展名
        if(!fileName.contains(".")) { fileName += ".png"; }

        File file = new File(path + fileName);
        // 如果文件不已存在, 报错.
        if(!file.exists()){ throw new RuntimeException("文件不存在"); }

        try { init(ImageIO.read(new File(path + fileName))); } catch (IOException e) { throw new RuntimeException(e); }

        return this;
    }

    /**
     * 初始化一个图像对象，该图像对象捕获指定矩形区域的屏幕内容。
     *
     * @param rect 指定捕获屏幕内容的矩形区域。矩形的左上角为捕获的起始点，矩形的大小为捕获的区域大小。
     * @return Img 返回一个初始化后的图像对象，该对象包含指定屏幕区域的内容。
     * @throws RuntimeException 如果创建屏幕捕获时发生AWTException异常，则抛出运行时异常。
     */
    public Img init(Rectangle rect){
        try {
            // 使用Robot类创建一个屏幕捕获对象，并初始化Img对象
            init(new Robot().createScreenCapture(rect));
        } catch (AWTException e) {
            // 如果捕获到AWTException异常，将其封装并抛出为运行时异常
            throw new RuntimeException(e);
        }

        return this;
    }


    /**
     * 根据指定的行列索引获取图像中对应像素的值。
     * @param x 像素的x坐标（列索引）。
     * @param y 像素的y坐标（行索引）。
     * @return 图像中指定坐标像素的值。
     */
    public int val(int x, int y){ return image[width * y + x]; }

    /**
     * 根据指定的索引计算并返回X坐标。
     *
     * @param index 指定的索引，用于计算X坐标。
     * @return 返回计算后的X坐标，基于当前对象的宽度进行取模运算。
     */
    public int getX(int index){ return index % this.width; }


    /**
     * 根据给定的索引计算并返回对应的Y坐标。
     * 该方法通过将索引除以宽度并向下取整，来计算Y值。
     *
     * @param index 输入的索引值，代表需要计算Y坐标的点。
     * @return 返回计算出的Y坐标值，为整型。
     */
    public int getY(int index){ return (int) Math.floor(index / this.width); }

    /**
     * 查找指定文件名的文件在系统中的位置。
     * 如果文件名不存在，则尝试寻找以时间戳为后缀的备用文件名，直到找到为止。
     *
     * @param fileName 要查找的文件名
     * @return 文件在系统中的位置，如果找不到则返回-1
     */
    public int find(String fileName){
        // 尝试根据原始文件名查找文件
        int pos = find(new Img(fileName));

        int time = 0;
        // 如果找不到文件，则尝试寻找带有时间戳后缀的文件
        while (pos == -1){
            Img target = new Img(fileName + ++time);

            // 如果文件不存在（即宽度或高度为0），则终止循环
            if(0 == target.width || 0 == target.height){ break; }

            // 尝试使用带有时间戳的文件名继续查找
            pos = find(target);
        }

        return pos;
    }


    /**
     * 在图像中查找指定目标的位置。
     *
     * @param target 要查找的目标图像对象，包含图像的宽、高和图像数据。
     * @return 如果找到目标图像的位置，则返回该位置的索引；如果未找到，则返回-1。
     */
    public int find(Img target){
        int pos = -1; // 初始化位置索引为-1，表示未找到

        // 如果目标图像的宽或高为0，则直接返回-1，表示无法查找
        if(0 == target.width || 0 == target.height){ return pos; }

        int[] tImg = target.image; // 目标图像的数据数组
        int startColor = tImg[0]; // 获取目标图像的第一个颜色值
        ArrayList<Integer> poses = this.colorMap.get(startColor); // 根据起始颜色值获取可能的位置列表

        // 如果位置列表为空或者长度为0，则直接返回-1，表示未找到
        if(null == poses || 0 == poses.size()){ return pos; }

        int miss, total = target.width * target.height; // 计算目标图像的总像素数

        // 遍历所有可能的位置
        for (int i = 0; i < poses.size(); i++) {
            int validatePos = poses.get(i); // 获取当前位置

            miss = 0; // 重置丢失的像素数

            int x = this.getX(validatePos), y = this.getY(validatePos); // 计算当前位置的坐标

            // 遍历目标图像的所有像素，验证当前位置是否匹配
            for (int ti = 0; ti < tImg.length; ti++) {
                int tx = target.getX(ti), ty = target.getY(ti); // 获取目标图像当前像素的坐标

                // 如果当前位置的像素颜色与目标图像不匹配，则增加丢失的像素数
                if(tImg[ti] != this.val(tx + x, ty + y)){ miss++; }

                // 如果丢失的像素数超过总像素的20%，则终止验证当前位置
                if(20 <= (miss * 100 / total)){
                    miss = -1; break; // 直接标记为未找到，并跳出循环
                }
            }

            // 如果丢失的像素数未超过阈值，则认为找到了目标图像的位置
            if(-1 < miss){ pos = validatePos; break; }
        }

        return pos; // 返回找到的位置索引或-1
    }

    /**
     * 将图像转换为灰度图像。
     * 此方法通过加权平均法将RGB颜色空间转换为灰度颜色空间。
     * 每个像素的RGB值被转换为一个相应的灰度值，图像中的所有像素都用这个灰度值替换。
     *
     * @return Img 返回转换后的灰度图像。
     */
    public Img toGray() {
        // 初始化颜色映射表，用于存储每个灰度颜色在图像中的位置
        colorMap = new HashMap<Integer, ArrayList<Integer>>();
        ArrayList<Integer> posList;

        int rgb;
        // 遍历图像中的所有像素
        for (int ti = 0; ti < image.length; ti++) {
            rgb = image[ti];

            // 提取RGB分量
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;

            // 使用加权平均法计算灰度值
            int gray = (int) (0.21 * r + 0.72 * g + 0.07 * b);
            // 将RGB像素转换为等价的灰度像素
            image[ti] = new Color(gray, gray, gray).getRGB();

            int tempColor = image[ti];
            // 如果颜色映射表中不存在当前灰度颜色，则添加一个新的空列表
            if(!colorMap.containsKey(tempColor)){ colorMap.put(tempColor, new ArrayList<Integer>()); }
            posList = colorMap.get(tempColor);
            // 将当前像素的位置添加到颜色映射表中
            posList.add(ti);
        }

        // 返回转换后的图像对象
        return this;
    }


    /**
     * 将图像数据保存到文件中。
     * 如果文件名没有扩展名，将默认使用".png"作为扩展名。
     *
     * @param file 指定要保存的文件名，可以不包含扩展名。
     */
    public void toFile(String file) {
        // 如果文件名没有扩展名，添加".png"扩展名
        if(!file.contains(".")){ file += ".png"; }

        // 创建一个与图像尺寸相同的BufferedImage对象
        BufferedImage ib = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        int x, y;
        // 遍历图像数据，并将其设置到BufferedImage对象中
        for (int i = 0; i < image.length; i++) {
            x = getX(i);
            y = getY(i);
            ib.setRGB(x, y, image[i]);
        }

        try {
            // 将BufferedImage对象以PNG格式写入到文件中
            ImageIO.write(ib, "PNG", new File(path + file));
        } catch (IOException e) {
            // 如果写入过程中发生IO异常，抛出运行时异常
            throw new RuntimeException(e);
        }
    }


    public String getMd5(){
        if(null == md5){ md5 = DigestUtils.md5Hex(this.toString()); }
        return md5;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("{\"width\": ");
        sb
                .append(width)
                .append(", \"height\": ")
                .append(height)
                .append(", \"bgc\": ")
                .append(bgc)
                .append(", \"image\": [")
        ;

        for (int y = 0; y < height; y++) {
            sb.append("[");
            for (int x = 0; x < width; x++) {
                sb.append(this.val(x, y)).append(x == width -1 ? "": ",");
            }
            sb.append("]").append(y == height - 1 ? "": ",");

        }
        sb.append("]}");

        return sb.toString();
    }

}
