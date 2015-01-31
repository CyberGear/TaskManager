package lt.cybergear.taskmanager.test;

import lt.cybergear.taskmanager.TaskManager;
import lt.cybergear.taskmanager.Callback;
import lt.cybergear.taskmanager.Task;

class Testas {

            void runTask() {
                TaskManager.startTask(Testas.this, new Task<String>(new Callback<String>() {
                    @Override
                    public void onResponseReceived(String response, Exception serviceError) {
//                        Log.e("result", response);
                    }
                }) {
                    @Override
                    protected String doInBackground() throws Exception {
                        Thread.sleep(1);
                        return "Kazkokia interneto uzklausa";
                    }
                });
            }

        }