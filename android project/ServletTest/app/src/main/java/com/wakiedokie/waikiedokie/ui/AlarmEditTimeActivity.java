package com.wakiedokie.waikiedokie.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TimePicker;
import android.widget.Toast;

import com.wakiedokie.waikiedokie.R;
import com.wakiedokie.waikiedokie.util.database.DBHelper;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * AlarmEditTimeActivity - Page to edit alarm time
 *
 * Created by chaovictorshin-deh on 4/14/16.
 */
public class AlarmEditTimeActivity extends Activity {
    private static final String TAG = "AlarmEditActivity";
    private static final int PENDING_CODE_OFFSET = 990000;
    private TimePicker alarmTimePicker;
    private DBHelper dbHelper;
    private int alarmID;
    private AlarmManager am;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_alarm_time);
        dbHelper = new DBHelper(AlarmEditTimeActivity.this);
        am = (AlarmManager)getSystemService(Activity.ALARM_SERVICE);

        // set alarmID to -1 if not passed in from intent (New alarm)
        Intent thisIntent = getIntent();
        alarmID = thisIntent.getIntExtra("alarmID", -1);closeContextMenu();


        Calendar cal = Calendar.getInstance();
        if (alarmID != -1) {
            Cursor cursor = dbHelper.getAlarm(alarmID);
            cursor.moveToFirst();
            String alarmTime = cursor.getString(cursor.getColumnIndex("alarm_time"));
            cal.setTimeInMillis(Long.parseLong(alarmTime));
        }

        alarmTimePicker = (TimePicker) findViewById(R.id.alarmTimePicker);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmTimePicker.setHour(cal.get(Calendar.HOUR_OF_DAY));
            alarmTimePicker.setMinute(cal.get(Calendar.MINUTE));
        }
        else {
            alarmTimePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
            alarmTimePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
        }

        Button btn_select_buddy = (Button) findViewById(R.id.btn_to_select_buddy);
        btn_select_buddy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(AlarmEditTimeActivity.this, AlarmSelectBuddyActivity.class);
                intent.putExtra("alarmID", alarmID);
                startActivity(intent);
            }
        });

        Button btn_save = (Button) findViewById(R.id.btn_save_alarm);
        btn_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Calendar now = Calendar.getInstance();
                Calendar cal = Calendar.getInstance();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cal.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getHour());
                    cal.set(Calendar.MINUTE, alarmTimePicker.getMinute());
                }
                else {
                    cal.set(Calendar.HOUR_OF_DAY, alarmTimePicker.getCurrentHour());
                    cal.set(Calendar.MINUTE, alarmTimePicker.getCurrentMinute());
                }

                // Set alarm for next day if time is before current time
                if (cal.before(now)) {
                    cal.add(Calendar.DATE, 1);
                }

                int requestCode = PENDING_CODE_OFFSET + alarmID;
                Intent alarmRingIntent = new Intent(AlarmEditTimeActivity.this, AlarmStatusActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(AlarmEditTimeActivity.this,
                        requestCode, alarmRingIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                am.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(),
                        pendingIntent);

                // Logging stuff
                Log.d(TAG, "Alarm is set");
                Long hours = TimeUnit.MILLISECONDS.toHours(
                        cal.getTimeInMillis()-now.getTimeInMillis());
                Long minutes = TimeUnit.MILLISECONDS.toMinutes(
                        cal.getTimeInMillis() - now.getTimeInMillis() - TimeUnit.HOURS.toMillis(hours));
                String hoursStr = Long.toString(hours);
                String minutesStr = Long.toString(minutes);
                String toastStr = "Alarm will go off in " + hoursStr + " hrs " + minutesStr + " mins";
                Toast.makeText(getApplicationContext(), toastStr, Toast.LENGTH_SHORT).show();

                if (alarmID != -1) {
                    dbHelper.updateAlarm(alarmID, Long.toString(cal.getTimeInMillis()), "", 1);
                }
                else {
                    // Save alarm to SQlite if new alarm
                    dbHelper.addAlarm(Long.toString(cal.getTimeInMillis()), "", 1);

                }

                // debugging logs
                Cursor cursor = dbHelper.getAllAlarms();
                cursor.moveToFirst();
                String alarmTime = cursor.getString(cursor.getColumnIndex("alarm_time"));
                System.out.println("Alarm time = " + alarmTime);
                while (cursor.moveToNext()) {
                    alarmTime = cursor.getString(cursor.getColumnIndex("alarm_time"));
                    Log.d(TAG, "Alarm time = " + alarmTime);
                    Calendar calCheck = Calendar.getInstance();
                    calCheck.setTimeInMillis(Long.parseLong(alarmTime));
                    Log.d(TAG, "Hour  : " + calCheck.get(Calendar.HOUR));
                    Log.d(TAG, "Minute : " + calCheck.get(Calendar.MINUTE));
                    Log.d(TAG, "AM PM : " + calCheck.get(Calendar.AM_PM));
                }

                Intent mainIntent = new Intent(AlarmEditTimeActivity.this, AlarmMainActivity.class);
                startActivity(mainIntent);
            }
        });

        Button btn_delete = (Button) findViewById(R.id.btn_delete_alarm);
        btn_delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dbHelper.deleteAlarm(alarmID);

                int requestCode = PENDING_CODE_OFFSET + alarmID;
                Intent alarmRingIntent = new Intent(AlarmEditTimeActivity.this, AlarmStatusActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(AlarmEditTimeActivity.this,
                        requestCode, alarmRingIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                pendingIntent.cancel();
                am.cancel(pendingIntent);

                Toast.makeText(getApplicationContext(), "Deleted alarm!", Toast.LENGTH_SHORT).show();

                Intent mainIntent = new Intent(AlarmEditTimeActivity.this, AlarmMainActivity.class);
                startActivity(mainIntent);
            }
        });
    }
}
