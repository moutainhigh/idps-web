package com.ai.paas.ipaas.ips.service.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;

import com.ai.paas.ipaas.image.AuthDescriptor;
import com.ai.paas.ipaas.utils.ImageUtil;
import com.x.sdk.dss.DSSBaseFactory;
import com.x.sdk.dss.interfaces.IDSSClient;
import com.x.sdk.exception.GeneralException;
import com.x.sdk.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.rpc.service.GenericException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class GMClient {
    private static Logger log = LoggerFactory.getLogger(GMClient.class);

    private static final String IMAGE_SRC_ROOT = "srcPath";
    private static final String IMAGE_TARGET_ROOT = "targetPath";
    private static final String IMAGE_NAME_SPLIT = "imageNameSplit";
    private static final String EXTENT_KEY = "extent";
    private static final String QUALITY_KEY = "quality";

    // 本地保存图片的路径 源图
    private String imageSrcRoot = null;
    // 图片名分隔符 _
    private String imageNameSplit = null;
    // 本地保存图片的路径 缩略图
    private String imageTargetRoot = null;
    // 是否补白尺寸图
    private boolean extent = true;
    // 图片质量
    private float quality = 0.75f;

    private Gson gson = new Gson();
    private IDSSClient dc = null;

    /**
     * 使用服务认证模式，初始化GMClient，获取IDSSClient。
     * 
     * @param parameter
     * @param ad
     */
    public GMClient(String parameter, AuthDescriptor ad) {
        try {
//            dc = DSSBaseFactory.getClient(ad);
            JsonObject paras = gson.fromJson(parameter, JsonObject.class);

            if (paras != null) {

                this.imageSrcRoot = paras.get(IMAGE_SRC_ROOT).getAsString();
                this.imageNameSplit = paras.get(IMAGE_NAME_SPLIT).getAsString();
                this.imageTargetRoot = paras.get(IMAGE_TARGET_ROOT).getAsString();
                this.extent = paras.get(EXTENT_KEY).getAsBoolean();
                this.quality = paras.get(QUALITY_KEY).getAsInt();
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * 使用sdk模式，初始化GMClient，通过mongoInfo信息，获取IDSSClient。
     * 
     * @param props
     * @param mongoInfo
     */
    public GMClient(String props, String mongoInfo) {
        try {
            dc = DSSBaseFactory.getClient(mongoInfo);
            JsonObject paras = gson.fromJson(props, JsonObject.class);

            if (paras != null) {

                this.imageSrcRoot = paras.get(IMAGE_SRC_ROOT).getAsString();
                this.imageNameSplit = paras.get(IMAGE_NAME_SPLIT).getAsString();
                this.imageTargetRoot = paras.get(IMAGE_TARGET_ROOT).getAsString();
                this.extent = paras.get(EXTENT_KEY).getAsBoolean();
                this.quality = paras.get(QUALITY_KEY).getAsFloat();
            }
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * @param imageName 源图名
     * @return
     * @throws Exception
     */
    public boolean isLocalImageExist(String imageName, String imageType) throws Exception {
        if (imageName == null)
            return false;
        String localPath = imageSrcRoot + (imageSrcRoot.endsWith(File.separator) ? "" : File.separator)
                + getFirstPath(imageName) + File.separator + getSecondPath(imageName);
        forceMkdir(new File(localPath));
        log.debug("------------------------localPath----------------------{}", localPath);
        return new File(localPath + File.separator + imageName + imageType).exists();
    }

    public String getSourceImagePath(String imageName, String imageType) throws Exception {
        return (imageSrcRoot + (imageSrcRoot.endsWith(File.separator) ? "" : File.separator) + imageName + imageType);
    }

    /**
     * @param imageId 源图名
     * @throws Exception
     */
    public void getRomteImage(String imageId, String imageType) throws Exception {
        String imageName = imageSrcRoot + (imageSrcRoot.endsWith(File.separator) ? "" : File.separator)
                + getFirstPath(imageId) + File.separator + getSecondPath(imageId) + File.separator + imageId
                + imageType;
        try (FileOutputStream fos = new FileOutputStream(new File(imageName));) {
            byte[] readin = dc.read(imageId);
            fos.write(readin);
            fos.flush();
        } catch (Exception e) {
            log.error("get Image:" + imageName + " from dss error!", e);
        }
    }

    public String scaleImage(String uri, String imageName, int type, String imageSize, String imageType,
            boolean isExtent) throws Exception {
        long begin = System.currentTimeMillis();
        log.debug("{}----GraphicsImage----scaleImage---------begin", uri);
        if (imageSize != null && imageSize.contains("X")) {
            imageSize = imageSize.replace("X", "x");
        }
        String targetPath = imageTargetRoot + (imageTargetRoot.endsWith(File.separator) ? "" : File.separator)
                + getFirstPath(imageName) + File.separator + getSecondPath(imageName) + File.separator + imageName
                + imageNameSplit + imageSize + imageType;
        forceMkdir(new File(imageTargetRoot + (imageTargetRoot.endsWith(File.separator) ? "" : File.separator)
                + getFirstPath(imageName) + File.separator + getSecondPath(imageName)));

        String localPath = imageSrcRoot + File.separator + imageName + imageType;

        ImageUtil.resize(new File(localPath), new File(targetPath), imageSize, this.quality);

        log.debug("{}----GraphicsImage----scaleImage---------end 耗时{}", uri, (System.currentTimeMillis() - begin));
        log.debug("{}----GraphicsImage----targetPath---------{}", uri, targetPath);
        return targetPath;
    }

    /**
     * 图片上增加水印
     * 
     * @param srcPath
     * @param targetPath
     * @throws Exception
     */
    public void addImgText(String srcPath, String targetPath) throws Exception {
        // 暂时不实现
    }

    /**
     * 删除本地文件，以节省空间
     * 
     * @param path
     * @throws Exception
     */
    public void removeLocalSizedImage(String path) throws Exception {
        File file = new File(path);
        if (file.exists()) {
            if (file.isFile()) {
                Files.delete(file.toPath());
            }
            Files.delete(file.toPath());
        } else {
            log.info("所删除的文件不存在！");
        }
    }

    private String getCommand(String imageName, int type, String imageSize, String targetPath, String imageType,
            boolean isExtent, int quality) throws Exception {
        StringBuilder cmd = new StringBuilder();
        String width = imageSize.substring(0, imageSize.indexOf("x"));

        cmd.append(" convert ");
        cmd.append(" -scale ").append(imageSize.trim());
        if (!(extent || isExtent)) {
            if (Integer.valueOf(width) < 250)
                cmd.append("^ ");
        }
        // 去杂质，对于小图片质量百分百
        if (Integer.valueOf(width) < 250) {
            if (imageType.lastIndexOf(".jpg") >= 0) {
                cmd.append(" -strip -define jpeg:preserve-settings ");
            }
            cmd.append(" -quality 100 ");
        } else {
            if (50 < quality && quality < 101) {
                cmd.append(" -quality ").append(quality);
            } else {
                cmd.append(" -quality 100 ");
            }
        }

        if (extent || isExtent) {
            cmd.append(" -background white ");
            cmd.append(" -gravity center ");
            cmd.append(" -extent ").append(imageSize);
        }

        cmd.append(" ").append(imageSrcRoot).append(imageSrcRoot.endsWith(File.separator) ? "" : File.separator)
                .append(getFirstPath(imageName)).append(File.separator).append(getSecondPath(imageName))
                .append(File.separator).append(imageName).append(imageType);
        cmd.append(" ").append(targetPath);
        return cmd.toString();
    }

    /**
     * 获得一级目录名称
     *
     * @param path
     * @return
     */
    private String getFirstPath(String path) {
        if (path == null || path.length() < 6)
            return null;
        return path.substring(0, 6);
    }

    /**
     * 获得二级目录名称
     *
     * @param path
     * @return
     */
    private String getSecondPath(String path) {
        if (path == null || path.length() < 8)
            return null;
        return path.substring(6, 7);
    }

    /**
     * 创建目录
     *
     * @param directory
     * @throws IOException
     */
    private void forceMkdir(File directory) throws IOException {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                String message = "文件 " + directory + " 存在，不是目录。不能创建该目录。 ";
                throw new IOException(message);
            }
        } else {
            if (!directory.mkdirs()) {
                if (!directory.isDirectory()) {
                    String message = "不能创建该目录 " + directory;
                    throw new IOException(message);
                }
            }
        }
    }

    public boolean judgeSize(String srcImage, int minWidth, int minHeight) {
        if (minWidth <= 0 && minHeight <= 0){
            return true;
        }
        return false;
    }
}
