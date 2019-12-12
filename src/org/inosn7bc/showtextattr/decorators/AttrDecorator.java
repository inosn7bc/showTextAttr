/*
 * Copyright (C) 2019, inosn7bc
 */
package org.inosn7bc.showtextattr.decorators;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.eclipse.core.internal.resources.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.PaletteData;
import org.mozilla.universalchardet.UniversalDetector;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.slf4j.MDC;

/**
 *
 * jdk 1.5 or greater
 * @author inosn7bc
 */
public class AttrDecorator implements ILabelDecorator {

	//TODO isの読み込みを1回にしたい。

	private static final Map<IPath, CacheValue> cacheMap = new WeakHashMap<IPath, CacheValue>();

	private static final Logger LOGGER = Logger.getLogger(AttrDecorator.class);

	static final Map<String, FileDefine> fileDefines = new HashMap<String, FileDefine>();

	static {
		fileDefines.put("*.txt", new FileDefine("", ""));
		fileDefines.put("*.log", new FileDefine("", ""));
		fileDefines.put("*.java", new FileDefine("", ""));
		fileDefines.put("*.conf", new FileDefine("UTF-8", "LF"));
		fileDefines.put("*.xml", new FileDefine("", ""));
		fileDefines.put("*.properties", new FileDefine("", ""));
		fileDefines.put("*.js", new FileDefine("Shift-JIS", "CRLF"));
		fileDefines.put("*.jsp", new FileDefine("Shift-JIS", "CRLF"));
		fileDefines.put("*.css", new FileDefine("", ""));
		fileDefines.put("*.inc", new FileDefine("", ""));
		fileDefines.put("*.tld", new FileDefine("", ""));
		fileDefines.put("*.vm", new FileDefine("", ""));
		fileDefines.put("*.sh", new FileDefine("EUC-JP", "LF"));
		fileDefines.put("*.bat", new FileDefine("SHIFT-JIS", "CRLF"));
		fileDefines.put("*.htm*", new FileDefine("", ""));
		fileDefines.put("*.sql", new FileDefine("", ""));

		filter = new WildcardFileFilter(new ArrayList<String>(
				fileDefines.keySet()), IOCase.INSENSITIVE);
	}

	static final List<String> wildcards = Arrays.asList("*.txt", "*.log",
			"*.java", "*.conf", "*.xml", "*.properties", "*.js", "*.jsp", "*.css",
			"*.inc", "*.tld", "*.vm", "*.sh", "*.bat", "*.htm*", "*.sql");
	static final WildcardFileFilter filter;

	public Image decorateImage(Image image, Object element) {
		long threadId = Thread.currentThread().getId();

		LOGGER.info("decorateImage:" + threadId + ":" + element);

		PaletteData paletteData = new PaletteData(200, 0, 0);

		//		ImageData imageData =		new ImageData(8, 8, 256, paletteData);

		try {

			//			java.io.File cur = new java.io.File(".");
			//			System.out.println(cur.getAbsolutePath());
			//
			//			java.io.File imageFile = new java.io.File(".\\image\\not.png");
			//
			//			ImageData imageData =		new ImageData(new FileInputStream(imageFile));
			//
			//			Image image2 = new Image(null, imageData);

			//			Image image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
			//			Image image2 = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_DEC_FIELD_ERROR);
			//
			//TODO			return image2;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return image;
	}

	public String decorateText(String text, Object element) {

		String mdcKey = "mdcKey";
		String mdcValue = UUID.randomUUID().toString();

		try {
			//TODO			MDC.put(mdcKey, mdcValue);

			long threadId = Thread.currentThread().getId();

			// if (t) {
			// throw new RuntimeException("なんかera-");
			// }

			// File形式？
			if (!(element instanceof File)) {
				return text;
			}

			File f = (File) element;
			String fileName = f.getName();

			//対象ファイル？
			if (!filter.accept(null, fileName)) {
				LOGGER.debug("ファイル名が対象外:" + fileName);
				return text;
			}

			IPath location = f.getLocation();
			long localTimeStamp = f.getLocalTimeStamp();

			if (cacheMap.containsKey(location)) {
				CacheValue cache = cacheMap.get(location);
				if (localTimeStamp == cache.timestamp) {
					LOGGER.trace("キャッシュにヒット:" + fileName);
					return text + cache.addText;
				}
			}

			String cs;
			String sep = "";

			try {
				InputStream is = f.getContents();

				cs = getCharsetName(is);

				if ("UTF-8".equals(cs.toUpperCase())) {
					//BOMを調べる

					InputStream is2 = f.getContents();

					if (is2.available() >= 3) {
						byte b[] = { 0, 0, 0 };
						is2.read(b, 0, 3);
						if (b[0] != (byte) 0xEF
								|| b[1] != (byte) 0xBB
								|| b[2] != (byte) 0xBF) {
							// BOMなし
						} else {
							// BOMあり
							cs = cs + "(BOM)";
						}
					}
				}

				InputStream is3 = f.getContents();

				sep = getLineSeparater(is3);

			} catch (CoreException e) {
				LOGGER.error("", e);
				return text;
			} catch (IOException e) {
				LOGGER.error("", e);
				return text;
			}

			sep = sep.replaceAll("\r", "CR").replaceAll("\n", "LF");// 最適化する

			String addText = " " + "[" + cs + "]" + sep;
			text = text + addText;
			//			LOGGER.trace(text);

			CacheValue cache = new CacheValue(localTimeStamp, addText);

			cacheMap.put(location, cache);
			LOGGER.trace(threadId + ":" + getMemoryInfo());

			return text;
		} finally {
			MDC.remove(mdcKey);
		}
	}

	public static String getCharsetName(InputStream is) throws IOException {
		UniversalDetector detector = new UniversalDetector(null);

		byte[] buf = new byte[4096];

		int nread;
		while ((nread = is.read(buf)) > 0 && !detector.isDone()) {
			detector.handleData(buf, 0, nread);
		}

		detector.dataEnd();
		String detectedCharset = detector.getDetectedCharset();

		detector.reset();

		return detectedCharset == null ? "ASCII" : detectedCharset;
	}

	public static String getLineSeparater(InputStream is) throws IOException {

		byte[] buf = new byte[4096];

		char prev = 0;

		StringBuilder sep = new StringBuilder();

		int nread;
		file: while ((nread = is.read(buf)) > 0) {

			for (int i = 0; i < buf.length; i++) {
				byte b = buf[i];
				char x = (char) b;

				if (isLineSeparator(prev)) {

					if (isLineSeparator(x)) {
						sep.append(prev);
						if (prev != x) {
							sep.append(x);
						}
					} else {
						sep.append(prev);
					}
					break file;

				} else {
					// 前の文字が\r,\nではない。
					prev = x;
				}
			}
		}

		if (sep.length() == 0 && prev != 0) {
			sep.append(prev);
		}

		return sep.toString();
	}

	static boolean isLineSeparator(char c) {
		return c == '\r' || c == '\n';
	}

	public static String getMemoryInfo() {
		String info = "";

		DecimalFormat format_mem = new DecimalFormat("#,### KiB");
		DecimalFormat format_ratio = new DecimalFormat("##.#");

		long free = Runtime.getRuntime().freeMemory() / 1024;
		long total = Runtime.getRuntime().totalMemory() / 1024;
		long max = Runtime.getRuntime().maxMemory() / 1024;
		long used = total - free;

		double ratio = (used * 100 / (double) total);

		info += "Total   = " + format_mem.format(total);
		info += ", ";
		info += "Free    = " + format_mem.format(free);
		info += ", ";
		info += "use     = " + format_mem.format(used) + " ("
				+ format_ratio.format(ratio) + "%)";
		info += ", ";
		info += "max = " + format_mem.format(max);
		return info;
	}

	public void addListener(ILabelProviderListener listener) {
		// noop
	}

	public void dispose() {
		// noop
	}

	public boolean isLabelProperty(Object element, String property) {
		return true;
	}

	public void removeListener(ILabelProviderListener listener) {
		// noop
	}

}
