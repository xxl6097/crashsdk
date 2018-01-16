package com.crashsdk;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.crashsdk.callback.OnCrashCallBack;
import com.crashsdk.callback.OnEmailEvents;
import com.crashsdk.net.EmailBean;
import com.crashsdk.ui.DefaultErrorActivity;
import com.crashsdk.utils.Logc;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CrashManager {
    //Extras passed to the error activity
    private static final String EXTRA_RESTART_ACTIVITY_CLASS = "uuxia.com.customactivityoncrash.EXTRA_RESTART_ACTIVITY_CLASS";
    private static final String EXTRA_SHOW_ERROR_DETAILS = "uuxia.com.customactivityoncrash.EXTRA_SHOW_ERROR_DETAILS";
    private static final String EXTRA_STACK_TRACE = "uuxia.com.customactivityoncrash.EXTRA_STACK_TRACE";
    private static final String EXTRA_IMAGE_DRAWABLE_ID = "uuxia.com.customactivityoncrash.EXTRA_IMAGE_DRAWABLE_ID";

    //General constants
    private final static String TAG = "CrashManager";
    private static final String INTENT_ACTION_ERROR_ACTIVITY = "uuxia.com.customactivityoncrash.ERROR";
    private static final String INTENT_ACTION_RESTART_ACTIVITY = "uuxia.com.customactivityoncrash.RESTART";
    private static final String CAOC_HANDLER_PACKAGE_NAME = "uuxia.com.customactivityoncrash";
    private static final String DEFAULT_HANDLER_PACKAGE_NAME = "com.android.internal.os";
    private static final int MAX_STACK_TRACE_SIZE = 131071; //128 KB - 1

    //Internal variables
    private static Application application;
    private static WeakReference<Activity> lastActivityCreated = new WeakReference<>(null);
    private static boolean isInBackground = false;

    //Settable properties and their defaults
    private static boolean launchErrorActivityWhenInBackground = true;
    private static boolean showErrorDetails = true;
    private static boolean enableAppRestart = true;
    private static int defaultErrorActivityDrawableId = R.mipmap.ic_launcher;
    private static Class<? extends Activity> errorActivityClass = null;
    private static Class<? extends Activity> restartActivityClass = null;

    //mail instance
    private static Object mEmail = null;
    private static boolean bAutoSendMail = false;
    private static boolean bWriteFile = true;

    public static OnCrashCallBack onCrashCallBack;

    /**
     * Install.
     *
     * @param context the context
     */
    public static void install(final Context context) {
        try {
            if (context == null) {
                Log.e(TAG, "Install failed: context is null!");
            } else {
                Logc.Init(context);
                if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    Log.w(TAG, "CrashManager will be installed, but may not be reliable in API lower than 14");
                }

                //INSTALL!
                Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

                if (oldHandler != null && oldHandler.getClass().getName().startsWith(CAOC_HANDLER_PACKAGE_NAME)) {
                    Log.e(TAG, "You have already installed CrashManager, doing nothing!");
                } else {
                    if (oldHandler != null && !oldHandler.getClass().getName().startsWith(DEFAULT_HANDLER_PACKAGE_NAME)) {
                        Log.e(TAG, "IMPORTANT WARNING! You already have an UncaughtExceptionHandler, are you sure this is correct? If you use ACRA, Crashlytics or similar libraries, you must initialize them AFTER CrashManager! Installing anyway, but your original handler will not be called.");
                    }

                    application = (Application) context.getApplicationContext();

                    //We define a default exception handler that does what we want so it can be called from Crashlytics/ACRA
                    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                        @Override
                        public void uncaughtException(Thread thread, final Throwable throwable) {
                            Log.e(TAG, "App has crashed, executing CrashManager's UncaughtExceptionHandler", throwable);

                            if (errorActivityClass == null) {
                                errorActivityClass = guessErrorActivityClass(application);
                            }

                            if (isStackTraceLikelyConflictive(throwable, errorActivityClass)) {
                                Log.e(TAG, "Your application class or your error activity have crashed, the custom activity will not be launched!");
                            } else {
                                if (launchErrorActivityWhenInBackground || !isInBackground) {
                                    final Intent intent = new Intent(application, errorActivityClass);
                                    StringWriter sw = new StringWriter();
                                    PrintWriter pw = new PrintWriter(sw);
                                    throwable.printStackTrace(pw);
                                    String stackTraceString = sw.toString();
                                    if (onCrashCallBack != null) {
                                        onCrashCallBack.onCrashErrorMessage(stackTraceString);
                                    }

                                    //Reduce data to 128KB so we don't get a TransactionTooLargeException when sending the intent.
                                    //The limit is 1MB on Android but some devices seem to have it lower.
                                    //See: http://developer.android.com/reference/android/os/TransactionTooLargeException.html
                                    //And: http://stackoverflow.com/questions/11451393/what-to-do-on-transactiontoolargeexception#comment46697371_12809171
                                    if (stackTraceString.length() > MAX_STACK_TRACE_SIZE) {
                                        String disclaimer = " [stack trace too large]";
                                        stackTraceString = stackTraceString.substring(0, MAX_STACK_TRACE_SIZE - disclaimer.length()) + disclaimer;
                                    }

                                    if (enableAppRestart && restartActivityClass == null) {
                                        //We can set the restartActivityClass because the app will terminate right now,
                                        //and when relaunched, will be null again by default.
                                        restartActivityClass = guessRestartActivityClass(application);
                                    } else if (!enableAppRestart) {
                                        //In case someone sets the activity and then decides to not restart
                                        restartActivityClass = null;
                                    }

                                    if (bWriteFile) {
                                        Logc.i(stackTraceString, true);
                                    }
//                                    sendErrorEmail(context, stackTraceString);

                                    intent.putExtra(EXTRA_STACK_TRACE, stackTraceString);
                                    intent.putExtra(EXTRA_RESTART_ACTIVITY_CLASS, restartActivityClass);
                                    intent.putExtra(EXTRA_SHOW_ERROR_DETAILS, showErrorDetails);
                                    intent.putExtra(EXTRA_IMAGE_DRAWABLE_ID, defaultErrorActivityDrawableId);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    application.startActivity(intent);
                                }
                            }
                            final Activity lastActivity = lastActivityCreated.get();
                            if (lastActivity != null) {
                                //We finish the activity, this solves a bug which causes infinite recursion.
                                //This is unsolvable in API<14, so beware!
                                //See: https://github.com/ACRA/acra/issues/42
                                lastActivity.finish();
                                lastActivityCreated.clear();
                            }
                            killCurrentProcess();
                        }
                    });
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                            int currentlyStartedActivities = 0;

                            @Override
                            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                                if (activity.getClass() != errorActivityClass) {
                                    // Copied from ACRA:
                                    // Ignore activityClass because we want the last
                                    // application Activity that was started so that we can
                                    // explicitly kill it off.
                                    lastActivityCreated = new WeakReference<>(activity);
                                }
                            }

                            @Override
                            public void onActivityStarted(Activity activity) {
                                currentlyStartedActivities++;
                                isInBackground = (currentlyStartedActivities == 0);
                                //Do nothing
                            }

                            @Override
                            public void onActivityResumed(Activity activity) {
                                //Do nothing
                            }

                            @Override
                            public void onActivityPaused(Activity activity) {
                                //Do nothing
                            }

                            @Override
                            public void onActivityStopped(Activity activity) {
                                //Do nothing
                                currentlyStartedActivities--;
                                isInBackground = (currentlyStartedActivities == 0);
                            }

                            @Override
                            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
                                //Do nothing
                            }

                            @Override
                            public void onActivityDestroyed(Activity activity) {
                                //Do nothing
                            }
                        });
                    }

                    Log.i(TAG, "CrashManager has been installed.");
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "An unknown error occurred while installing CrashManager, it may not have been properly initialized. Please report this as a bug if needed.", t);
        }
    }

    public static void sendErrorEmail(Context context, String error) {
        EmailBean emailBean = new EmailBean();
        final String taskIsName = "fuck";
        OnEmailEvents callback = new OnEmailEvents(taskIsName) {
            @Override
            public void sendSucessfull(String taskIsClassName) {
            }

            @Override
            public void sendFaid(String taskIsClassName) {
            }
        };
        emailBean.setOnEmailEvents(callback);
        CrashManager.createEmail(emailBean);
        sendMail(context, error, false, mEmail);
    }

    /**
     * Is show error details from intent boolean.
     *
     * @param intent the intent
     * @return the boolean
     */
    public static boolean isShowErrorDetailsFromIntent(Intent intent) {
        return intent.getBooleanExtra(CrashManager.EXTRA_SHOW_ERROR_DETAILS, true);
    }

    /**
     * Gets default error activity drawable id from intent.
     *
     * @param intent the intent
     * @return the default error activity drawable id from intent
     */
    public static int getDefaultErrorActivityDrawableIdFromIntent(Intent intent) {
        return intent.getIntExtra(CrashManager.EXTRA_IMAGE_DRAWABLE_ID, R.mipmap.ic_launcher);
    }

    /**
     * Gets stack trace from intent.
     *
     * @param intent the intent
     * @return the stack trace from intent
     */
    public static String getStackTraceFromIntent(Intent intent) {
        return intent.getStringExtra(CrashManager.EXTRA_STACK_TRACE);
    }

    /**
     * Gets all error details from intent.
     *
     * @param context the context
     * @param intent  the intent
     * @return the all error details from intent
     */
    public static String getAllErrorDetailsFromIntent(Context context, Intent intent) {
        //I don't think that this needs localization because it's a development string...

        Date currentDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        //Get build date
        String buildDateAsString = getBuildDateAsString(context, dateFormat);

        //Get app version
        String versionName = getVersionName(context);

        String packageName = context.getPackageName();

        String errorDetails = "";

        errorDetails += "Build version: " + versionName + " \n";
        errorDetails += "Build date: " + buildDateAsString + " \n";
        errorDetails += "Current date: " + dateFormat.format(currentDate) + " \n";
        errorDetails += "package Name: " + packageName + " \n";
        errorDetails += "Device: " + getDeviceModelName() + " \n";
        errorDetails += "System Version: " + android.os.Build.VERSION.RELEASE + " \n\n";
        errorDetails += "Stack trace:  \n";
        errorDetails += getStackTraceFromIntent(intent);
        return errorDetails;
    }

    /**
     * Gets restart activity class from intent.
     *
     * @param intent the intent
     * @return the restart activity class from intent
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Activity> getRestartActivityClassFromIntent(Intent intent) {
        Serializable serializedClass = intent.getSerializableExtra(CrashManager.EXTRA_RESTART_ACTIVITY_CLASS);

        if (serializedClass != null && serializedClass instanceof Class) {
            return (Class<? extends Activity>) serializedClass;
        } else {
            return null;
        }
    }

    /**
     * Restart application with intent.
     *
     * @param activity the activity
     * @param intent   the intent
     */
    public static void restartApplicationWithIntent(Activity activity, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.finish();
        activity.startActivity(intent);
        killCurrentProcess();
    }

    /**
     * Close application.
     *
     * @param activity the activity
     */
    public static void closeApplication(Activity activity) {
        activity.finish();
        killCurrentProcess();
    }


    /// SETTERS AND GETTERS FOR THE CUSTOMIZABLE PROPERTIES

    /**
     * Is launch error activity when in background boolean.
     *
     * @return the boolean
     */
    public static boolean isLaunchErrorActivityWhenInBackground() {
        return launchErrorActivityWhenInBackground;
    }

    /**
     * Sets launch error activity when in background.
     *
     * @param launchErrorActivityWhenInBackground the launch error activity when in background
     */
    public static void setLaunchErrorActivityWhenInBackground(boolean launchErrorActivityWhenInBackground) {
        CrashManager.launchErrorActivityWhenInBackground = launchErrorActivityWhenInBackground;
    }

    /**
     * Is show error details boolean.
     *
     * @return the boolean
     */
    public static boolean isShowErrorDetails() {
        return showErrorDetails;
    }

    /**
     * Sets show error details.
     *
     * @param showErrorDetails the show error details
     */
    public static void setShowErrorDetails(boolean showErrorDetails) {
        CrashManager.showErrorDetails = showErrorDetails;
    }

    /**
     * Gets default error activity drawable.
     *
     * @return the default error activity drawable
     */
    public static int getDefaultErrorActivityDrawable() {
        return defaultErrorActivityDrawableId;
    }

    /**
     * Sets default error activity drawable.
     *
     * @param defaultErrorActivityDrawableId the default error activity drawable id
     */
    public static void setDefaultErrorActivityDrawable(int defaultErrorActivityDrawableId) {
        CrashManager.defaultErrorActivityDrawableId = defaultErrorActivityDrawableId;
    }

    /**
     * Is enable app restart boolean.
     *
     * @return the boolean
     */
    public static boolean isEnableAppRestart() {
        return enableAppRestart;
    }

    /**
     * Sets enable app restart.
     *
     * @param enableAppRestart the enable app restart
     */
    public static void setEnableAppRestart(boolean enableAppRestart) {
        CrashManager.enableAppRestart = enableAppRestart;
    }

    /**
     * Gets error activity class.
     *
     * @return the error activity class
     */
    public static Class<? extends Activity> getErrorActivityClass() {
        return errorActivityClass;
    }

    /**
     * Sets error activity class.
     *
     * @param errorActivityClass the error activity class
     */
    public static void setErrorActivityClass(Class<? extends Activity> errorActivityClass) {
        CrashManager.errorActivityClass = errorActivityClass;
    }

    /**
     * Gets restart activity class.
     *
     * @return the restart activity class
     */
    public static Class<? extends Activity> getRestartActivityClass() {
        return restartActivityClass;
    }

    /**
     * Sets restart activity class.
     *
     * @param restartActivityClass the restart activity class
     */
    public static void setRestartActivityClass(Class<? extends Activity> restartActivityClass) {
        CrashManager.restartActivityClass = restartActivityClass;
    }


    /// INTERNAL METHODS NOT TO BE USED BY THIRD PARTIES

    private static boolean isStackTraceLikelyConflictive(Throwable throwable, Class<? extends Activity> activityClass) {
        do {
            StackTraceElement[] stackTrace = throwable.getStackTrace();
            for (StackTraceElement element : stackTrace) {
                if ((element.getClassName().equals("android.app.ActivityThread") && element.getMethodName().equals("handleBindApplication")) || element.getClassName().equals(activityClass.getName())) {
                    return true;
                }
            }
        } while ((throwable = throwable.getCause()) != null);
        return false;
    }

    private static String getBuildDateAsString(Context context, DateFormat dateFormat) {
        String buildDate;
        try {
            ApplicationInfo ai = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
            ZipFile zf = new ZipFile(ai.sourceDir);
            ZipEntry ze = zf.getEntry("classes.dex");
            long time = ze.getTime();
            buildDate = dateFormat.format(new Date(time));
            zf.close();
        } catch (Exception e) {
            buildDate = "Unknown";
        }
        return buildDate;
    }

    private static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private static String getDeviceModelName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }

    private static String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

    private static Class<? extends Activity> guessRestartActivityClass(Context context) {
        Class<? extends Activity> resolvedActivityClass;

        //If action is defined, use that
        resolvedActivityClass = CrashManager.getRestartActivityClassWithIntentFilter(context);

        //Else, get the default launcher activity
        if (resolvedActivityClass == null) {
            resolvedActivityClass = getLauncherActivity(context);
        }

        return resolvedActivityClass;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Activity> getRestartActivityClassWithIntentFilter(Context context) {
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(
                new Intent().setAction(INTENT_ACTION_RESTART_ACTIVITY),
                PackageManager.GET_RESOLVED_FILTER);

        if (resolveInfos != null && resolveInfos.size() > 0) {
            ResolveInfo resolveInfo = resolveInfos.get(0);
            try {
                return (Class<? extends Activity>) Class.forName(resolveInfo.activityInfo.name);
            } catch (ClassNotFoundException e) {
                //Should not happen, print it to the log!
                Log.e(TAG, "Failed when resolving the restart activity class via intent filter, stack trace follows!", e);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Activity> getLauncherActivity(Context context) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (intent != null) {
            try {
                return (Class<? extends Activity>) Class.forName(intent.getComponent().getClassName());
            } catch (ClassNotFoundException e) {
                //Should not happen, print it to the log!
                Log.e(TAG, "Failed when resolving the restart activity class via getLaunchIntentForPackage, stack trace follows!", e);
            }
        }

        return null;
    }

    private static Class<? extends Activity> guessErrorActivityClass(Context context) {
        Class<? extends Activity> resolvedActivityClass;

        //If action is defined, use that
        resolvedActivityClass = CrashManager.getErrorActivityClassWithIntentFilter(context);

        //Else, get the default launcher activity
        if (resolvedActivityClass == null) {
            resolvedActivityClass = DefaultErrorActivity.class;
        }

        return resolvedActivityClass;
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Activity> getErrorActivityClassWithIntentFilter(Context context) {
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(
                new Intent().setAction(INTENT_ACTION_ERROR_ACTIVITY),
                PackageManager.GET_RESOLVED_FILTER);

        if (resolveInfos != null && resolveInfos.size() > 0) {
            ResolveInfo resolveInfo = resolveInfos.get(0);
            try {
                return (Class<? extends Activity>) Class.forName(resolveInfo.activityInfo.name);
            } catch (ClassNotFoundException e) {
                //Should not happen, print it to the log!
                Log.e(TAG, "Failed when resolving the error activity class via intent filter, stack trace follows!", e);
            }
        }

        return null;
    }

    public static void killCurrentProcess() {
        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(10);
    }

    public boolean isAutoSendMail() {
        return bAutoSendMail;
    }

    public static void setAutoSendMail(boolean b) {
        bAutoSendMail = b;
    }

    public static void setbWriteFile(boolean writeFile) {
        bWriteFile = writeFile;
    }

    public static void setMail(Object mail) {
        mEmail = mail;
    }

    public static void send(Context context, String error) {
        sendMail(context, error, true, mEmail);
    }

    public static void createEmail(EmailBean emailBean) {
        Logc.e("==================>CrashManager.createEmail1 " + emailBean);
        if (emailBean != null && mEmail != null) {
            Class clazz = mEmail.getClass();
            try {
                Method setCallback = clazz.getDeclaredMethod(MethondName.setCallback, Object.class);
                setCallback.invoke(mEmail, emailBean.getOnEmailEvents());

                Method createEmail = clazz.getDeclaredMethod(MethondName.createEmail, String.class, String.class, String.class);
                createEmail.invoke(mEmail, emailBean.getToAddr(), emailBean.getFromAddr(), emailBean.getPassword());
                Logc.d("==================>CrashManager.createEmail " + emailBean.toString());
            } catch (NoSuchMethodException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void sendMail(Context context, String error, boolean bSend, Object mail) {
        if (mail != null && (bAutoSendMail || bSend)) {
            String packageName = context.getPackageName();
            sendEmail(packageName, error, null, mail);
        }
    }

    private static void sendEmail(final String title, final String error, final String filepath, final Object mail) {
        if (mail == null) {
            return;
        }
        new Thread(new Runnable() {

            @Override
            public void run() {
                Class clazz = mail.getClass();
                try {
                    Date currentDate = new Date();
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                    String subjectString = "Program Crashed：" + title + " " + dateFormat.format(currentDate);
                    Method subJect = clazz.getDeclaredMethod(MethondName.setSubject, String.class);
                    Method conTent = clazz.getDeclaredMethod(MethondName.setContent, String.class);
                    Method attachFile = clazz.getDeclaredMethod(MethondName.setAttachFile, String.class);
                    Method sendtextMail = clazz.getDeclaredMethod(MethondName.sendTextMail);
                    subJect.invoke(mail, subjectString);
                    conTent.invoke(mail, error);
                    attachFile.invoke(mail, filepath);
                    sendtextMail.invoke(mail);
                } catch (NoSuchMethodException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * The type Methond name.
     */
    static class MethondName {
        /**
         * The constant setSubject.
         */
        public static String setSubject = "setSubject";
        /**
         * The constant setContent.
         */
        public static String setContent = "setContent";
        /**
         * The constant setAttachFile.
         */
        public static String setAttachFile = "setAttachFile";
        /**
         * The constant sendTextMail.
         */
        public static String sendTextMail = "sendTextMail";

        public static String setCallback = "setEmailCallBack";
        public static String createEmail = "createEmail";
    }
}
