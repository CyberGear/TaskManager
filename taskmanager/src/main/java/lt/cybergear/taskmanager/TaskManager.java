package lt.cybergear.taskmanager;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static android.os.AsyncTask.Status.FINISHED;

/**
 * Created by nk on 13.12.11.
 */
public class TaskManager {
    private static final String TAG = TaskManager.class.getSimpleName();

    private static final Map<String, Task> runningTasks = new HashMap<>();
    static private final Map<String, AsyncTaskToRun> runningAsync = new HashMap<>();
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
        return saveStartMethod(object.getClass().getCanonicalName(), getIdAndMethod(object), task);
    }

    private static String saveStartMethod(String aClass, Map<String, Method> methodOfId, Task<?> task) {
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
        AsyncTaskToRun taskToRun = runningAsync.get(taskId);
        if (runningTask == null || (taskToRun != null && taskToRun.getStatus() == FINISHED)) {
            runningTasks.put(taskId, task);
            task.setCallback(new ResumedCallback(new TaskRemover(taskId, task.getCallback())));
            runningAsync.put(taskId, new AsyncTaskToRun(taskId).exec());
        } else {
            runningTask.setCallback(new ResumedCallback(new TaskRemover(taskId, task.getCallback())));
            if (runningTask.hasResult()) {
                runningTask.onResponseReceived();
            }
        }
        return taskId;
    }

    public static void stopTask(String taskId) {
        Task runningTask = runningTasks.get(taskId);
        AsyncTaskToRun taskToRun = runningAsync.get(taskId);
        if (runningTask != null) {
            runningTask.cancel();
            runningTasks.remove(taskId);
        }
        if (taskToRun != null) {
            taskToRun.cancel(true);
            runningAsync.remove(taskId);
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

        public AsyncTaskToRun exec() {
            execute();
            return this;
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
            if (callback != null) {
                remove();
                callback.onResponseReceived(result, serviceError);
            } else {
                // FIXME: could be bad idea
                new Thread() {
                    @Override
                    public void run() {
                        try { sleep(1000); } catch (InterruptedException e) { /* ignore */ }
                        if (callback == null && runningTasks.containsKey(taskId)) {
                            remove();
                        }
                    }
                }.start();
            }
        }

        public void remove() {
            if (runningAsync.containsKey(taskId)) {
                runningAsync.get(taskId).cancel(true);
            }
            runningTasks.remove(taskId);
            runningAsync.remove(taskId);
        }
    }

    static class ResumedCallback<T> implements Callback<T> {

        private Callback<T> callback;

        ResumedCallback(Callback<T> callback) {
            this.callback = callback;
        }

        @Override
        public void onResponseReceived(T result, Exception serviceError) {
            if (callback != null) {
                callback.onResponseReceived(result, serviceError);
            }
        }
    }

}
