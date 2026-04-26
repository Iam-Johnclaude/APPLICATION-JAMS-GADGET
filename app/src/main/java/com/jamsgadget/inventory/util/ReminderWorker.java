package com.jamsgadget.inventory.util;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ReminderWorker extends Worker {

    private static final String TAG = "ReminderWorker";

    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Starting periodic due date check...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        try {
            // Get all active installments
            QuerySnapshot querySnapshot = Tasks.await(db.collection("installments")
                    .whereEqualTo("status", "Active")
                    .get());

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, 3); // Check for dues in the next 3 days
            Date threeDaysFromNow = cal.getTime();

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

            for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                Timestamp nextDueTs = doc.getTimestamp("nextDueDate");
                if (nextDueTs != null) {
                    Date nextDueDate = nextDueTs.toDate();
                    
                    // If due date is between now and 3 days from now
                    if (nextDueDate.after(new Date()) && nextDueDate.before(threeDaysFromNow)) {
                        String email = doc.getString("email");
                        String name = doc.getString("customerName");
                        String item = doc.getString("itemName");
                        Double amount = doc.getDouble("monthlyPayment");

                        if (email != null && !email.isEmpty() && amount != null) {
                            Log.d(TAG, "Sending auto-reminder to: " + email);
                            EmailUtil.sendDueDateReminder(getApplicationContext(), email, name, item, amount, sdf.format(nextDueDate));
                        }
                    }
                }
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Error in ReminderWorker", e);
            return Result.retry();
        }
    }
}
