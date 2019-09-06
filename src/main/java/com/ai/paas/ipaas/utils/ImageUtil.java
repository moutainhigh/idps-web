package com.ai.paas.ipaas.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.*;

import com.ai.paas.ipaas.image.AuthDescriptor;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.ai.paas.ipaas.ips.service.IImageService;
import com.ai.paas.ipaas.ips.service.impl.GMImageServiceImpl;

import javax.swing.*;

public class ImageUtil {
	private static final Logger log = LogManager.getLogger(ImageUtil.class);

	private IImageService imageSv = null;

	public ImageUtil(AuthDescriptor ad) {
		this.imageSv = new GMImageServiceImpl(ad);
	}

	public ImageUtil(String mongoInfo) {
		this.imageSv = new GMImageServiceImpl(mongoInfo);
	}
	
	/**
	 * 1、本地原图是否存在 2、存在时，生成缩略图；不存在，从mongodb获得原图保存在本地，并生成缩略图
	 * 
	 * @param uri
	 *            请求串
	 * @param imageName
	 *            缩放后图片的路径图片名称
	 * @param type
	 *            1为比例处理，2为大小处理，如（比例：1024x1024,大小：50%x50%）
	 * @param imageSize
	 *            缩放后的图片尺寸 如（比例：1024x1024,大小：50%x50%）
	 * @param imageType
	 *            图片类型
	 * @param isExtent
	 *            缩略图是否填充空白
	 * @throws Exception
	 */
	public String getScaleImage(String uri, String imageName, String imageSize,
			String imageType, boolean isExtent) throws Exception {
		long begin = System.currentTimeMillis();
		// 处理图片路径
		boolean localExist = imageSv.isLocalImageExist(imageName,
				imageType);
		log.debug(uri + "--------localExist---------:" + localExist + " 耗时"
				+ (System.currentTimeMillis() - begin));

		if (!localExist) {
			begin = System.currentTimeMillis();
			imageSv.getRomteImage(imageName, imageType);
			log.debug(uri + "--------getRomteImage---------" + " 耗时"
					+ (System.currentTimeMillis() - begin));

		}
		String path = null;
		if (imageSize == null) {
			// 源图
			path = imageSv.getSourceImagePath(imageName, imageType);
		} else {
			path = imageSv.scaleImage(uri, imageName, 1, imageSize,
					imageType, isExtent);
		}
		log.debug(uri + "--------getScaleImage---------:" + localExist + " 耗时"
				+ (System.currentTimeMillis() - begin));
		return path;

	}

	/**
	 * 是否使用GM模式
	 * 
	 * @return
	 * @throws Exception
	 */
	public boolean getGMMode() {
		return imageSv.isGMMode();
	}

	/**
	 * 异常时， 图片路径
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getReservePath() {
		return imageSv.getReservePath();
	}

	public boolean isSupported(String imageType) {
		return imageSv.isSupported(imageType);
	}

	/**
	 * 上传图片时，转换图片格式
	 * 
	 * @param srcImage
	 *            不带路径
	 * @param descImage
	 * @throws Exception
	 */
	public void convertType(String srcImage, String descImage) throws Exception {
		imageSv.convertType(srcImage, descImage);
	}

	public boolean judgeSize(String srcImage, int minWidth, int minHeight) {
		return imageSv.judgeSize(srcImage, minWidth, minHeight);
	}

	/**
	 * 上传图片 本地存放路径
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getUplodPath() {
		return imageSv.getUplodPath();
	}

	/**
	 * 上传图片 转换格式后的本地路径
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getDestPath() {
		return imageSv.getDestPath();
	}

	/**
	 * 支持的图片格式类型
	 * 
	 * @return
	 * @throws Exception
	 */
	public String getSupportType(String ext) {
		return imageSv.getSupportType(ext);
	}

	public static void resize(File originalFile, File resizedFile,
							  int newWidth, float quality) throws IOException {

		if (quality > 1) {
			throw new IllegalArgumentException(
					"Quality has to be between 0 and 1");
		}

		ImageIcon ii = new ImageIcon(originalFile.getCanonicalPath());
		Image i = ii.getImage();
		Image resizedImage = null;

		int iWidth = i.getWidth(null);
		int iHeight = i.getHeight(null);
		if (iWidth > iHeight) {
			resizedImage = i.getScaledInstance(newWidth, (newWidth * iHeight)
					/ iWidth, Image.SCALE_SMOOTH);
		} else {
			resizedImage = i.getScaledInstance((newWidth * iWidth) / iHeight,
					newWidth, Image.SCALE_SMOOTH);
		}

		// This code ensures that all the pixels in the image are loaded.
		Image temp = new ImageIcon(resizedImage).getImage();

		// Create the buffered image.
		BufferedImage bufferedImage = new BufferedImage(temp.getWidth(null),
				temp.getHeight(null), BufferedImage.TYPE_INT_RGB);

		// Copy image to buffered image.
		Graphics g = bufferedImage.createGraphics();

		// Clear background and paint the image.
		g.setColor(Color.white);
		g.fillRect(0, 0, temp.getWidth(null), temp.getHeight(null));
		g.drawImage(temp, 0, 0, null);
		g.dispose();

		// Soften.
		float softenFactor = 0.05f;
		float[] softenArray = { 0, softenFactor, 0, softenFactor,
				1 - (softenFactor * 4), softenFactor, 0, softenFactor, 0 };
		Kernel kernel = new Kernel(3, 3, softenArray);
		ConvolveOp cOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		bufferedImage = cOp.filter(bufferedImage, null);

		// Write the jpeg to a file.
		FileOutputStream out = new FileOutputStream(resizedFile);

		// Encodes image as a JPEG data stream
		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);

		JPEGEncodeParam param = encoder
				.getDefaultJPEGEncodeParam(bufferedImage);

		param.setQuality(quality, true);

		encoder.setJPEGEncodeParam(param);
		encoder.encode(bufferedImage);
	}

	public static void resize(File originalFile, File resizedFile,
							  String imageSize, float quality) throws IOException {

		if (quality > 1) {
			throw new IllegalArgumentException(
					"Quality has to be between 0 and 1");
		}

		ImageIcon ii = new ImageIcon(originalFile.getCanonicalPath());
		Image i = ii.getImage();
		Image resizedImage = null;

		int newWidth = Integer.valueOf(imageSize.split("x")[0]);
		int newHeight = Integer.valueOf(imageSize.split("x")[1]);
		resizedImage = i.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

		// This code ensures that all the pixels in the image are loaded.
		Image temp = new ImageIcon(resizedImage).getImage();

		// Create the buffered image.
		BufferedImage bufferedImage = new BufferedImage(temp.getWidth(null),
				temp.getHeight(null), BufferedImage.TYPE_INT_RGB);

		// Copy image to buffered image.
		Graphics g = bufferedImage.createGraphics();

		// Clear background and paint the image.
		g.setColor(Color.white);
		g.fillRect(0, 0, temp.getWidth(null), temp.getHeight(null));
		g.drawImage(temp, 0, 0, null);
		g.dispose();

		// Soften.
		float softenFactor = 0.05f;
		float[] softenArray = { 0, softenFactor, 0, softenFactor,
				1 - (softenFactor * 4), softenFactor, 0, softenFactor, 0 };
		Kernel kernel = new Kernel(3, 3, softenArray);
		ConvolveOp cOp = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
		bufferedImage = cOp.filter(bufferedImage, null);

		// Write the jpeg to a file.
		FileOutputStream out = new FileOutputStream(resizedFile);

		// Encodes image as a JPEG data stream
		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);

		JPEGEncodeParam param = encoder
				.getDefaultJPEGEncodeParam(bufferedImage);

		param.setQuality(quality, true);

		encoder.setJPEGEncodeParam(param);
		encoder.encode(bufferedImage);
	}

	public static void main(String[] args) {
		String fileStr = "/Users/liwenxian/Downloads/1103/test/dxf.jpg";
		File file = new File(fileStr);
		InputStream input = null;
		try {
			input = new FileInputStream(file);
			byte[] bytes = new byte[input.available()];
			input.read(bytes);
			// System.out.println(upLoadImage(bytes, "dxf.jpg"));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

		}

	}
}
