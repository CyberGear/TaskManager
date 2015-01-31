package lt.cybergear.taskmanager;

/**
 * Created by nk on 13.12.11.
 */
public abstract class Task<Result> {
    private static final String TAG = Task.class.getSimpleName();

    private Callback<Result> callback;
    private ServiceResult serviceResult;
    private Exception exception;
    private boolean canceled;
    private Boolean isTokenRenewed = null;

    public Task(Callback<Result> callback) {
        this.callback = callback;
    }

    abstract protected Result doInBackground() throws Exception;

    protected Result doInBackground(Object... strings) {
        exception = null;
        try {
            return doInBackground() ;
        } catch (Exception e) {
            exception = e;
        }
        boolean isTokenExpired;
        if (isTokenExpired = TaskManager.getTaskListener().isTokenExpired(exception)) {
            isTokenRenewed = TaskManager.getTaskListener().isTokenRenewed();
        }
        if (isTokenExpired && isTokenRenewed) {
            try {
                exception = null;
                return doInBackground();
            } catch (Exception e) {
                exception = e;
            }
        }
        return null;
    }

    protected void onPostExecute(Result result) {
        if (canceled) {
            return;
        }
        boolean firstResult = serviceResult == null;
        serviceResult = new ServiceResult(result, exception);
        if (exception != null && isTokenRenewed != null && !isTokenRenewed) {
            TaskManager.getTaskListener().onRenewFailed();
        } else if (callback != null) {
            if (exception == null || firstResult) {
                callback.onResponseReceived(result, exception);
            }
        }
    }

    public Callback<Result> getCallback() {
        return callback;
    }

    public void setCallback(Callback<Result> callback) {
        this.callback = callback;
    }

    public void onResponseReceived() {
        if (callback != null && serviceResult != null) {
            callback.onResponseReceived(serviceResult.getResult(), serviceResult.getServiceError());
        }
    }

    public boolean hasResult() {
        return serviceResult != null;
    }

    public void cancel() {
        canceled = true;
    }

    class ServiceResult {
        private Result result;
        private Exception serviceError;

        ServiceResult(Result result, Exception serviceError) {
            this.result = result;
            this.serviceError = serviceError;
        }

        public Result getResult() {
            return result;
        }

        public Exception getServiceError() {
            return serviceError;
        }
    }
}
