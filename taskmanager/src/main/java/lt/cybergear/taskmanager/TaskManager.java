package lt.cybergear.taskmanager;

import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by nk on 13.12.11.
 */
public class TaskManager {
    private static final String TAG = TaskManager.class.getSimpleName();

    private static final Map<String, Task> runningTasks = new HashMap<>();
    private static final Map<String, Map<String, Method>> methodsOfClasses = new HashMap<>();
    private static TaskListener taskListener = new EmptyTaskListener();

    public static void relinkIfNecessary(Object o) {
        Class<?> aClass = o.getClass();
        for (String key:runningTasks.keySet()){
            if (!methodsOfClasses.containsKey(aClass.getCanonicalName())) {
                break;
            }
            if (methodsOfClasses.get(aClass.getCanonicalName()).containsKey(key)) {
                try {
                    Method method = methodsOfClasses.get(aClass.getCanonicalName()).get(key);
                    method.setAccessible(true);
                    method.invoke(o);
                } catch (Exception e) {
                    Log.w(TAG, "relinking callbacks: " + e.getMessage(), e);
                }
            }
        }
    }

    public static String startTask(Object object, Task<?> task) {
        return startTask(object.getClass().getCanonicalName(), getIdAndMethod(object), task);
    }

    private static String startTask(String aClass, Map<String, Method> methodOfId, Task<?> task) {
        if (!methodsOfClasses.containsKey(aClass)) {
            methodsOfClasses.put(aClass, new HashMap<String, Method>());
        }
        methodsOfClasses.get(aClass).putAll(methodOfId);
        String taskId = methodOfId.keySet().toArray(new String[1])[0];
        return startTask(taskId, task);
    }

    private static Map<String, Method> getIdAndMethod(Object object) {
        Map<String, Method> methodOfTask = new HashMap<>();
        Class<?> aClass = object.getClass();
        Method method;
        for (StackTraceElement traceElement:Thread.currentThread().getStackTrace()) {
            if ((method = getTaskStartMethod(aClass, traceElement)) != null) {
                methodOfTask.put(method.getName(), method);
                return methodOfTask;
            }
        }
        throw new IllegalStateException(
                "Method marked as '@" +
                TaskStartMethod.class.getSimpleName() +
                "' not found in class '" +
                aClass.getCanonicalName() +
                "' "
        );
    }

    private static Method getTaskStartMethod(Class<?> aClass, StackTraceElement traceElement) {
        String methodName = traceElement.getMethodName();
        try {
            if (aClass.getCanonicalName().equals(traceElement.getClassName())) {
                Method method = aClass.getDeclaredMethod(methodName);
                if (method.isAnnotationPresent(TaskStartMethod.class)) {
                    return method;
                }
            }
        } catch (NoSuchMethodException e) {
            /* Skip for now */
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static String startTask(String taskId, Task<?> task) {
        Task<?> runningTask = runningTasks.get(taskId);
        if (runningTask == null) {
            runningTasks.put(taskId, task);
            task.setCallback(new ResumedCallback(taskId, new TaskRemover(taskId, task.getCallback())));
            new AsyncTaskToRun(taskId).execute();
        } else {
            runningTask.setCallback(new ResumedCallback(taskId, new TaskRemover(taskId, task.getCallback())));
            if (runningTask.hasResult()) {
                runningTask.onResponseReceived();
            }
        }
        return taskId;
    }

    public static void stopTask(String taskId) {
        Task runningTask = runningTasks.get(taskId);
        if (runningTask != null) {
            runningTask.cancel();
            runningTasks.remove(taskId);
        }
    }

    public static void setTaskListener(TaskListener exceptionListener) {
        if (exceptionListener == null) {
            TaskManager.taskListener = new EmptyTaskListener();
        } else {
            TaskManager.taskListener = exceptionListener;
        }
    }

    protected static TaskListener getTaskListener() {
        return taskListener;
    }

    /**
     *
     */

    public static interface TaskListener {
        public boolean isTokenExpired(Exception e);
        public boolean isTokenRenewed();
        public void onRenewFailed();
    }

    private static class EmptyTaskListener implements TaskListener {
        @Override
        public boolean isTokenExpired(Exception e) { return false; }

        @Override
        public boolean isTokenRenewed() { return false; }

        @Override
        public void onRenewFailed() {}
    }

    public static class AsyncTaskToRun extends android.os.AsyncTask {
        private String taskId;

        AsyncTaskToRun(String taskId) {
            this.taskId = taskId;
        }

        @Override
        protected Object doInBackground(Object[] params) {
            if (runningTasks.containsKey(taskId)) {
                return runningTasks.get(taskId).doInBackground(params);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (runningTasks.containsKey(taskId)){
                runningTasks.get(taskId).onPostExecute(o);
            }
        }
    }

    static class TaskRemover<T> implements Callback<T> {

        private String taskId;
        private Callback<T> callback;

        TaskRemover(String taskId, Callback<T> callback) {
            this.taskId = taskId;
            this.callback = callback;
        }

        @Override
        public void onResponseReceived(T result, Exception serviceError) {
            runningTasks.remove(taskId);
            if (callback != null) {
                callback.onResponseReceived(result, serviceError);
            }
        }
    }

    static class ResumedCallback<T> implements Callback<T> {

        private String taskId;
        private Callback<T> callback;

        ResumedCallback(String taskId, Callback<T> callback) {
            this.taskId = taskId;
            this.callback = callback;
        }

        @Override
        public void onResponseReceived(T result, Exception serviceError) {
            if (callback != null) {
                Task value = runningTasks.get(taskId);
                callback.onResponseReceived(result, serviceError);
            }
        }
    }

}
