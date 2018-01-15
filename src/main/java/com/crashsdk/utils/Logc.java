package com.crashsdk.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

/**
 * The type Logc.
 */
public class Logc {

    /**
     * storage_path存储卡的路径.
     */
    public static final String STORAGE_PATH = Environment
            .getExternalStorageDirectory().getPath();

    /**
     * The constant LINE_BREAK.
     */
    public static final String LINE_BREAK = "\r\n";
    private static final ThreadLocal<ReusableFormatter> thread_local_formatter = new ThreadLocal<ReusableFormatter>() {
        protected ReusableFormatter initialValue() {
            return new ReusableFormatter();
        }
    };
    /**
     * The constant isAndroid.
     */
    public static boolean isAndroid = true;
    /**
     * The constant ROOT.
     */
    public static String ROOT;// = Environment.getExternalStorageDirectory().getPath() + "/HeT/"; // SD卡中的根目录
    /**
     * The constant TAG.
     */
    public static String TAG = "uulog"; // 自定义Tag的前缀，可以是作者名

    /**
     * The constant DEBUG.
     */
    public static boolean DEBUG = true;
    /**
     * The constant allowD.
     */
// 容许打印日志的类型，默认是true，设置为false则不打印
    public static boolean allowD = DEBUG && true;
    /**
     * The constant allowE.
     */
    public static boolean allowE = DEBUG && true;
    /**
     * The constant allowI.
     */
    public static boolean allowI = DEBUG && true;
    /**
     * The constant allowV.
     */
    public static boolean allowV = DEBUG && true;
    /**
     * The constant allowW.
     */
    public static boolean allowW = DEBUG && true;
    /**
     * The constant allowWtf.
     */
    public static boolean allowWtf = DEBUG && true;
    //    private static final boolean isSaveLog = true; // 是否把保存日志到SD卡中
    private static String PATH_LOG_INFO;// = ROOT + "log/";

    static {
        String os = System.getProperty("os.name");
        System.out.println("current os System is " + os);
        if (os.toLowerCase().contains("win") || os.toLowerCase().contains("mac")) {
            isAndroid = false;
        } else {
            ROOT = Environment.getExternalStorageDirectory().getPath() + "/HeT/"; // SD卡中的根目录
            PATH_LOG_INFO = ROOT + "log/";
            isAndroid = true;
        }
    }

    public static void Init(Context context) {
        PATH_LOG_INFO = STORAGE_PATH + File.separator
                + context.getPackageName() + File.separator + "log/"; // 公共日志目录
    }


    private static void loge(String tag, String content, Throwable tr) {
        if (isAndroid) {
            Log.e(tag, content, tr);
        } else {
            System.err.println(tag + "-" + content);
        }
    }

    private static void logd(String tag, String content) {
        if (isAndroid) {
            Log.d(tag, content);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    private static void logw(String tag, String content) {
        if (isAndroid) {
            Log.w(tag, content);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    private static void logi(String tag, String content) {
        if (isAndroid) {
            Log.i(tag, content);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    private static void logv(String tag, String content) {
        if (isAndroid) {
            Log.v(tag, content);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    private static void logwtf(String tag, Throwable tr) {
        if (isAndroid) {
            Log.wtf(tag, tr);
        } else {
            if (tr != null) {
                System.out.println(tag + "-" + tr.getMessage());
            }
        }
    }

    private static void logwtf(String tag, String content) {
        if (isAndroid) {
            Log.wtf(tag, content);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    private static void logwtf(String tag, String content, Throwable tr) {
        if (isAndroid) {
            Log.wtf(tag, content, tr);
        } else {
            System.out.println(tag + "-" + content);
        }
    }

    /**
     * V.
     *
     * @param content the content
     */
    public static void v(String content) {
        if (!allowV)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logv(tag, content);
    }

    /**
     * V.
     *
     * @param content   the content
     * @param isSaveLog the is save log
     */
    public static void v(String content, boolean isSaveLog) {
        if (!allowV)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logv(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * V.
     *
     * @param uTag    the u tag
     * @param content the content
     */
    public static void v(String uTag, String content) {
        if (!allowV) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logv(tag, content);
    }

    /**
     * V.
     *
     * @param uTag      the u tag
     * @param content   the content
     * @param isSaveLog the is save log
     */
    public static void v(String uTag, String content, boolean isSaveLog) {
        if (!allowV) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logv(tag, content);

        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * D.
     *
     * @param content the content
     */
    public static void d(String content) {
        if (!allowD)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logd(tag, content);
    }

    /**
     * D.
     *
     * @param content   the content
     * @param isSaveLog the is save log
     */
    public static void d(String content, boolean isSaveLog) {
        if (!allowD)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logd(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * D.
     *
     * @param uTag    the u tag
     * @param content the content
     */
    public static void d(String uTag, String content) {
        if (!allowD) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logd(tag, content);
    }

    /**
     * D.
     *
     * @param uTag      the u tag
     * @param content   the content
     * @param isSaveLog the is save log
     */
    public static void d(String uTag, String content, boolean isSaveLog) {
        if (!allowD) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logd(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * .
     *
     * @param content the content
     */
    public static void i(String content) {
        if (!allowI)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logi(tag, content);
    }

    /**
     * .
     *
     * @param content   the content
     * @param isSaveLog the is save log
     */
    public static void i(String content, boolean isSaveLog) {
        if (!allowI)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logi(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * .
     *
     * @param uTag    the u tag
     * @param content the content
     */
    public static void i(String uTag, String content) {
        if (!allowI) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logi(tag, content);
    }

    /**
     * .
     *
     * @param uTag      the u tag
     * @param content   the content
     * @param isSaveLog the is save log
     */
    public static void i(String uTag, String content, boolean isSaveLog) {
        if (!allowI) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logi(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * W.
     *
     * @param content the content
     */
    public static void w(String content) {
        if (!allowW)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logw(tag, content);
    }

    /**
     * W.
     *
     * @param content   the content
     * @param isSaveLog the is save log
     */
    public static void w(String content, boolean isSaveLog) {
        if (!allowW)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logw(tag, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * W.
     *
     * @param uTag    the u tag
     * @param content the content
     */
    public static void w(String uTag, String content) {
        if (!allowW) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logw(tag, content);
    }

    /**
     * W.
     *
     * @param uTag      the u tag
     * @param content   the content
     * @param isSaveLog the is save log
     */
    public static void w(String uTag, String content, boolean isSaveLog) {
        if (!allowW) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        logw(tag, content);

        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * E.
     *
     * @param content the content
     */
    public static void e(String content) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        loge(tag, content, null);
    }

    /**
     * E.
     *
     * @param content   the content
     * @param isSaveLog the is save log
     */
    public static void e(String content, boolean isSaveLog) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        loge(tag, content, null);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * E.
     *
     * @param uTag    the u tag
     * @param content the content
     */
    public static void e(String uTag, String content) {
        if (!allowE) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        loge(tag, content, null);
    }

    /**
     * E.
     *
     * @param uTag      the u tag
     * @param content   the content
     * @param isSaveLog the is save log
     */
    public static void e(String uTag, String content, boolean isSaveLog) {
        if (!allowE) {
            return;
        }
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        loge(tag, content, null);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * E.
     *
     * @param content the content
     * @param tr      the tr
     */
    public static void e(String content, Throwable tr) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.e(tag, content, tr);
    }

    /**
     * E.
     *
     * @param uTag    the u tag
     * @param content the content
     * @param tr      the tr
     */
    public static void e(String uTag, String content, Throwable tr) {
        if (!allowE)
            return;
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.e(tag, content, tr);
    }

    /**
     * E.
     *
     * @param content   the content
     * @param tr        the tr
     * @param isSaveLog the is save log
     */
    public static void e(String content, Throwable tr, boolean isSaveLog) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        Log.e(tag, content, tr);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, getThrowable(tr, content));
        }
    }

    /**
     * E.
     *
     * @param tr        the tr
     * @param isSaveLog the is save log
     */
    public static void e(Throwable tr, boolean isSaveLog) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        String content = getThrowable(tr, null);
        loge(tag, content, null);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, content);
        }
    }

    /**
     * E.
     *
     * @param tr the tr
     */
    public static void e(Throwable tr) {
        if (!allowE)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);

        String content = getThrowable(tr, null);
        loge(tag, content, null);
    }

    /**
     * E.
     *
     * @param uTag      the u tag
     * @param content   the content
     * @param tr        the tr
     * @param isSaveLog the is save log
     */
    public static void e(String uTag, String content, Throwable tr, boolean isSaveLog) {
        if (!allowE)
            return;
        TAG = uTag;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        loge(tag, content, tr);

        String msg = getThrowable(tr, content);
        if (isSaveLog && isAndroid) {
            point(PATH_LOG_INFO, tag, msg);
        }
    }

    /**
     * Wtf.
     *
     * @param content the content
     */
    public static void wtf(String content) {
        if (!allowWtf)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        logwtf(tag, content);
    }

    /**
     * Wtf.
     *
     * @param content the content
     * @param tr      the tr
     */
    public static void wtf(String content, Throwable tr) {
        if (!allowWtf)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        logwtf(tag, content, tr);
    }

    /**
     * Wtf.
     *
     * @param tr the tr
     */
    public static void wtf(Throwable tr) {
        if (!allowWtf)
            return;
        StackTraceElement caller = getCallerStackTraceElement();
        String tag = generateTag(caller);
        logwtf(tag, tr);
    }

    private static StackTraceElement getCallerStackTraceElement() {
        return Thread.currentThread().getStackTrace()[4];
    }

    /**
     * Point.
     *
     * @param path the path
     * @param tag  the tag
     * @param msg  the msg
     */
    public static void point(String path, String tag, String msg) {
        if (isSDAva()) {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("",
                    Locale.SIMPLIFIED_CHINESE);
            dateFormat.applyPattern("yyyy");
            path = path + dateFormat.format(date) + "/";
            dateFormat.applyPattern("MM");
            path += dateFormat.format(date) + "/";
            dateFormat.applyPattern("dd");
            path += dateFormat.format(date) + ".log";
            dateFormat.applyPattern("[yyyy-MM-dd HH:mm:ss]");
            String time = dateFormat.format(date);
            File file = new File(path);
            if (!file.exists())
                createDipPath(path);
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(file, true)));
                out.write(time + " " + tag + " " + msg + "\r\n");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Create dip path.
     *
     * @param file the file
     */
    public static void createDipPath(String file) {
        String parentFile = file.substring(0, file.lastIndexOf("/"));
        File file1 = new File(file);
        File parent = new File(parentFile);
        if (!file1.exists()) {
            parent.mkdirs();
            try {
                file1.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Format string.
     *
     * @param msg  the msg
     * @param args the args
     * @return the string
     */
    public static String format(String msg, Object... args) {
        ReusableFormatter formatter = thread_local_formatter.get();
        return formatter.format(msg, args);
    }

    /**
     * Is sd ava boolean.
     *
     * @return the boolean
     */
    public static boolean isSDAva() {
        if (isAndroid && Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)
                || Environment.getExternalStorageDirectory().exists()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Is null boolean.
     *
     * @param strSource the str source
     * @return the boolean
     */
    public static boolean isNull(final String strSource) {
        return strSource == null || "".equals(strSource.trim());
    }

    private static String generateTag(StackTraceElement caller) {
        String tag = "(%s:%d).%s"; // 占位符
        String callerClazzName = caller.getFileName();
        tag = String.format(tag, callerClazzName, caller.getLineNumber(), caller.getMethodName()); // 替换
        tag = isNull(TAG) ? tag : TAG + ":"
                + tag;
        return tag;
    }

    private static String getThrowable(Throwable throwable, String mag) {
        /* 打印异常 */
        StringBuffer sb = new StringBuffer();
        if (!TextUtils.isEmpty(mag)) {
            sb.append(mag);
        }
        if (throwable != null) {
            sb.append(LINE_BREAK);
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            throwable.printStackTrace(printWriter);
            sb.append(stringWriter.toString());
        }
        return sb.toString();
    }

    private static class ReusableFormatter {

        private Formatter formatter;

        private StringBuilder builder;

        /**
         * Instantiates a new Reusable formatter.
         */
        public ReusableFormatter() {
            builder = new StringBuilder();
            formatter = new Formatter(builder);
        }

        /**
         * Format string.
         *
         * @param msg  the msg
         * @param args the args
         * @return the string
         */
        public String format(String msg, Object... args) {
            formatter.format(msg, args);
            String s = builder.toString();
            builder.setLength(0);
            return s;
        }

    }

}
