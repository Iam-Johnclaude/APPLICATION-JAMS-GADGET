package com.jamsgadget.inventory.util;

import android.os.AsyncTask;
import android.util.Log;

import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import android.content.Context;
import android.widget.Toast;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailUtil {

    private static final String TAG = "EmailUtil";
    
    // SMTP Configuration
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final String SMTP_PORT = "465"; // SSL Port
    
    // Sender Credentials
    private static final String SENDER_EMAIL = "johnclaudewarlord@gmail.com";
    private static final String SENDER_PASSWORD = "ehus qsvx qytj qjbu"; 

    public static void sendDueDateReminder(Context context, String customerEmail, String customerName, String itemName, double amount, String dueDate) {
        String subject = "Payment Reminder: Installment Due Soon";
        String body = String.format("Dear %s,\n\nThis is a friendly reminder that your installment payment for '%s' is due on %s.\n\n" +
                "Amount Due: ₱%,.2f\n\n" +
                "Please ensure that your payment is settled on or before the due date.\n\n" +
                "Thank you for choosing JamsGadget!",
                customerName, itemName, dueDate, amount);

        new SendEmailTask(context, customerEmail, subject, body).execute();
    }

    private static class SendEmailTask extends AsyncTask<Void, Void, Boolean> {
        private final Context context;
        private final String recipient;
        private final String subject;
        private final String body;
        private Exception lastError;

        SendEmailTask(Context context, String recipient, String subject, String body) {
            this.context = context;
            this.recipient = recipient;
            this.subject = subject;
            this.body = body;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (SENDER_EMAIL.equals("your-email@gmail.com")) {
                Log.e(TAG, "Error: You must set SENDER_EMAIL and SENDER_PASSWORD in EmailUtil.java");
                return false;
            }

            Properties props = new Properties();
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.socketFactory.port", SMTP_PORT);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", SMTP_PORT);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD);
                }
            });

            try {
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SENDER_EMAIL));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
                message.setSubject(subject);
                message.setText(body);

                Transport.send(message);
                Log.d(TAG, "Email sent successfully to " + recipient);
                return true;
            } catch (Exception e) {
                lastError = e;
                Log.e(TAG, "SMTP Send Failed: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                if (context != null) Toast.makeText(context, "Reminder email sent!", Toast.LENGTH_SHORT).show();
            } else {
                String errorMsg = "Failed to send email.";
                if (lastError != null) {
                    if (lastError.getMessage().contains("authentication failed")) {
                        errorMsg += " Invalid login or App Password.";
                    } else if (lastError.getMessage().contains("UnknownHostException")) {
                        errorMsg += " No internet connection.";
                    } else {
                        errorMsg += " " + lastError.getMessage();
                    }
                }
                if (context != null) Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
            }
        }
    }
}
