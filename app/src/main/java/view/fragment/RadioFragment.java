package view.fragment;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.oucho.radio.R;
import org.oucho.radio.domain.player.GetAudioFocusTask;
import org.oucho.radio.domain.player.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import view.activities.HomeActivity;

import static android.content.Context.MODE_PRIVATE;
import static android.content.Context.NOTIFICATION_SERVICE;

/**
 * A placeholder fragment containing a simple view.
 */
public class RadioFragment extends Fragment implements
        View.OnClickListener {

    public static boolean running;

    private static ScheduledFuture mTask;

    private static final int USER_SETTINGS_REQUEST = 10;
    private static final int NOTIF_ID = 15;

    private static final String fichier_préférence = "org.oucho.radio_preferences";
    private static SharedPreferences préférences = null;

    private static final String PLAY = "play";
    private static final String PAUSE = "pause";
    private static final String RESTART = "restart";
    private static final String STOP = "stop";
    private static final String STATE = "org.oucho.radio.STATE";

    private static String nom_radio = "";
    private static final String nom_radio_pref = "";
    private static String action_lecteur = "";

    private static String etat_lecture = "";
    private static final String etat_lecture_pref = "";

    private static final int couleur_radio_pref = 0;
    private static int couleur_radio = 0;
    private static final int couleur_radio_text_pref = 0;
    public static int couleur_radio_text = 0;

    private RadioFragment.Etat_player Etat_player_Receiver;
    private NotificationManager notificationManager;
    private Context context;

    private final int[] bouton_ID = {
            R.id.button1,
            R.id.play,
            R.id.stop,
            R.id.off,
    };

    private final int[] bouton_radio_text_ID = {
            R.id.n_button1,
            R.id.button1_txt,
    };

    private static String nom1;
    private static String nom2;
    private static String nom3;
    private static String nom4;
    private static String nom5;

    private static String url1;
    private static String url2;
    private static String url3;
    private static String url4;
    private static String url5;

    private final String url_pref = "";
    private View rootView;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_home, container, false);

        context = getActivity();

        String files_path = (context.getFilesDir().getPath());
        String pref_path = files_path.replace("files", "shared_prefs");

        File f = new File(pref_path + "/" + fichier_préférence + ".xml");
        if (!f.exists())
            createPref();

        Etat_player_Receiver = new RadioFragment.Etat_player();

        RadioFragment.Control_Volume niveau_Volume = new RadioFragment.Control_Volume(getActivity(), new Handler());
        getActivity().getContentResolver().registerContentObserver(android.provider.Settings.System.CONTENT_URI, true, niveau_Volume);

        préférences = getActivity().getSharedPreferences(fichier_préférence, MODE_PRIVATE);

        Typeface fonte = Typeface.createFromAsset(getActivity().getAssets(), "fonts/ds_digii.ttf");
        TextClock heure = (TextClock) rootView.findViewById(R.id.time);
//        heure.setTypeface(fonte);


        for (int ID : bouton_ID) {
            rootView.findViewById(ID).setOnClickListener(this);
        }

        setIFace();

        getBitRate();
        return rootView;
    }


    /* *********************************************************************************************
     * Réactivation de l'application
     * ********************************************************************************************/

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter(STATE);
        getActivity().registerReceiver(Etat_player_Receiver, filter);

        setIFace();

        TextView nomStation = (TextView) rootView.findViewById(R.id.station);
        nomStation.setText(nom_radio);

        Typeface fonte = Typeface.createFromAsset(getActivity().getAssets(), "fonts/ds_digii.ttf");
        TextClock heure = (TextClock) rootView.findViewById(R.id.time);
//        heure.setTypeface(fonte);

        etat_lecture = préférences.getString("etat", etat_lecture_pref);

        TextView myAwesomeTextView = (TextView) rootView.findViewById(R.id.etat);
        myAwesomeTextView.setText(action_lecteur);

    }

    private void setIFace() {

        couleur_radio = préférences.getInt("colorRADIO", couleur_radio_pref);

        FrameLayout theme_color = (FrameLayout) rootView.findViewById(R.id.theme);
        theme_color.setBackgroundColor(couleur_radio);

        couleur_radio_text = préférences.getInt("colorTXT", couleur_radio_text_pref);

        for (int txtID : bouton_radio_text_ID) {
            TextView txt_color = (TextView) rootView.findViewById(txtID);
            txt_color.setTextColor(couleur_radio_text);
        }

        String button1 = préférences.getString("radio1a", nom_radio_pref);
        String button2 = préférences.getString("radio2a", nom_radio_pref);
        String button3 = préférences.getString("radio3a", nom_radio_pref);
        String button4 = préférences.getString("radio4a", nom_radio_pref);
        String button5 = préférences.getString("radio5a", nom_radio_pref);
        TextView nomStation10 = (TextView) rootView.findViewById(R.id.button1_txt);
        nomStation10.setText(button1);

        nom_radio = préférences.getString("name", nom_radio_pref);
        action_lecteur = préférences.getString("action", etat_lecture_pref);

        createNotification(nom_radio, action_lecteur);

        updatePlayStatus();

        volume();

    }



    /* *********************************************************************************************
     * Passage en arrière plan de l'application
     * ********************************************************************************************/

    @Override
    public void onPause() {
        super.onPause();

        SharedPreferences.Editor editor = préférences.edit();
        editor.putString("etat", etat_lecture);
        editor.apply();

        if (!"play".equals(etat_lecture))
            killNotif();
    }



    /* *********************************************************************************************
     * Gestion des clicks
     * ********************************************************************************************/

    @Override
    public void onClick(View v) {
        Vibrator vib = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        setPref();

        Intent player = new Intent(getActivity(), Player.class);

        switch (v.getId()) {
            case R.id.play:
                vib.vibrate(20);
                switch (etat_lecture) {
                    case "stop":
                        etat_lecture = "play";
                        player.putExtra("action", PLAY);
                        getActivity().startService(player);
                        updatePlayStatus();
                        break;
                    case "play":
                        etat_lecture = "pause";
                        player.putExtra("action", PAUSE);
                        getActivity().startService(player);
                        updatePlayStatus();
                        break;
                    case "pause":
                        etat_lecture = "play";
                        player.putExtra("action", RESTART);
                        getActivity().startService(player);
                        updatePlayStatus();
                        break;

                    default: //do nothing
                        break;
                }
                break;
            case R.id.stop:
                vib.vibrate(20);
                etat_lecture = "stop";
                player.putExtra("action", STOP);
                getActivity().startService(player);
                updatePlayStatus();
                break;


            case R.id.button1:
                playButton(url1, nom1);
                break;

            case R.id.off:
                vib.vibrate(20);
                etat_lecture = "stop";
                player.putExtra("action", STOP);
                getActivity().startService(player);

                SharedPreferences.Editor editor = préférences.edit();
                editor.putString("etat", "stop");
                editor.putString("action", "");
                editor.apply();

                stopTimer();

                killNotif();

                getActivity().finish();

                break;

            default: //do nothing
                break;
        }
    }

    private void setPref() {

        nom1 = préférences.getString("radio1a", nom_radio_pref);
        nom2 = préférences.getString("radio2a", nom_radio_pref);
        nom3 = préférences.getString("radio3a", nom_radio_pref);
        nom4 = préférences.getString("radio4a", nom_radio_pref);
        nom5 = préférences.getString("radio5a", nom_radio_pref);

        url1 = préférences.getString("radio1b", url_pref);
        url2 = préférences.getString("radio2b", url_pref);
        url3 = préférences.getString("radio3b", url_pref);
        url4 = préférences.getString("radio4b", url_pref);
        url5 = préférences.getString("radio5b", url_pref);
    }


    private void playButton(String url, String nom) {
        Vibrator vib = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        Intent player = new Intent(getActivity(), Player.class);

        vib.vibrate(20);

        player.putExtra("action", PLAY);
        player.putExtra("url", url);
        player.putExtra("name", nom);
        getActivity().startService(player);
        etat_lecture = "play";
        updatePlayStatus();
    }

    private void killNotif() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @SuppressLint("SetTextI18n")
            public void run() {

                notificationManager.cancel(NOTIF_ID);

            }
        }, 500);
    }

    /* *********************************************************************************************
     * Gestion des clicks
     * ********************************************************************************************/

    private void showDatePicker() {

        final String start = getString(R.string.start);
        final String cancel = getString(R.string.cancel);

        @SuppressLint("InflateParams") View view = getActivity().getLayoutInflater().inflate(R.layout.date_picker_dialog, null);

        final TimePicker picker = (TimePicker) view.findViewById(R.id.time_picker);
        final Calendar cal = Calendar.getInstance();

        picker.setIs24HourView(true);

        picker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setPositiveButton(start, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int hours;
                int mins;

                int hour = picker.getCurrentHour();
                int minute = picker.getCurrentMinute();
                int curHour = cal.get(Calendar.HOUR_OF_DAY);
                int curMin = cal.get(Calendar.MINUTE);

                if (hour < curHour) hours = (24 - curHour) + (hour);
                else hours = hour - curHour;

                if (minute < curMin) {
                    hours--;
                    mins = (60 - curMin) + (minute);
                } else mins = minute - curMin;

                startTimer(hours, mins);
            }
        });

        builder.setNegativeButton(cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // This constructor is intentionally empty, pourquoi ? parce que !
            }
        });

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showTimerInfo() {

        final String continuer = getString(R.string.continuer);
        final String cancelTimer = getString(R.string.cancel_timer);


        if (mTask.getDelay(TimeUnit.MILLISECONDS) < 0) {
            stopTimer();
            return;
        }
        @SuppressLint("InflateParams") View view = getActivity().getLayoutInflater().inflate(R.layout.timer_info_dialog, null);
        final TextView timeLeft = ((TextView) view.findViewById(R.id.time_left));


        final String stopTimer = getString(R.string.stop_timer);

        final AlertDialog dialog = new AlertDialog.Builder(getActivity()).setPositiveButton(continuer, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        }).setNegativeButton(cancelTimer, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopTimer();

                Context context = getActivity();

                Toast.makeText(context, stopTimer, Toast.LENGTH_LONG).show();
            }
        }).setView(view).create();

        new CountDownTimer(mTask.getDelay(TimeUnit.MILLISECONDS), 1000) {
            @Override
            public void onTick(long seconds) {

                long secondes = seconds;

                secondes = secondes / 1000;
                timeLeft.setText(String.format(getString(R.string.timer_info), (secondes / 3600), ((secondes % 3600) / 60), ((secondes % 3600) % 60)));
            }

            @Override
            public void onFinish() {
                dialog.dismiss();
            }
        }.start();

        dialog.show();
    }

    private void startTimer(final int hours, final int minutes) {

        final String impossible = getString(R.string.impossible);

        final String heureSingulier = getString(R.string.heure_singulier);
        final String heurePluriel = getString(R.string.heure_pluriel);

        final String minuteSingulier = getString(R.string.minute_singulier);
        final String minutePluriel = getString(R.string.minute_pluriel);

        final String arret = getString(R.string.arret);
        final String et = getString(R.string.et);

        final String heureTxt;
        final String minuteTxt;

        final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        final int delay = ((hours * 3600) + (minutes * 60)) * 1000;

        if (delay == 0) {
            Toast.makeText(getActivity(), impossible, Toast.LENGTH_LONG).show();
            return;
        }

        if (hours == 1) {
            heureTxt = heureSingulier;
        } else {
            heureTxt = heurePluriel;
        }

        if (minutes == 1) {
            minuteTxt = minuteSingulier;
        } else {
            minuteTxt = minutePluriel;
        }
        mTask = scheduler.schedule(new GetAudioFocusTask((HomeActivity) getActivity()), delay, TimeUnit.MILLISECONDS);

        if (hours == 0) {
            Toast.makeText(getActivity(), arret + " " + minutes + " " + minuteTxt, Toast.LENGTH_LONG).show();
        } else if (minutes == 0) {
            Toast.makeText(getActivity(), arret + " " + hours + " " + heureTxt, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getActivity(), arret + " " + hours + " " + heureTxt + " " + et + " " + minutes + " " + minuteTxt, Toast.LENGTH_LONG).show();
        }

        running = true;
    }


    public static void stopTimer() {

        running = false;

    }

    public static void stop(Context context) {
        Intent player = new Intent(context, Player.class);
        player.putExtra("action", "stop");
        context.startService(player);

        running = false;
    }
    /* *********************************************************************************************
     * Notification
     * ********************************************************************************************/

    private void createNotification(String nom, String action) {

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity());

        Intent i = new Intent(getActivity(), RadioFragment.class);
        i.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent intent = PendingIntent.getActivity(getActivity(), 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(intent);
        builder.setSmallIcon(R.drawable.notification);
        builder.setOngoing(true);

        Boolean unlock;
        if ("play".equals(etat_lecture)) {
            unlock = true;
        } else {
            unlock = false;
        }
        builder.setOngoing(unlock);

        Notification notification = builder.build();
        RemoteViews contentView = new RemoteViews(getActivity().getPackageName(), R.layout.notification);

        contentView.setTextViewText(R.id.notif_name, nom);
        contentView.setTextViewText(R.id.notif_text, action);

        couleur_radio = préférences.getInt("colorRADIO", couleur_radio_pref);

        contentView.setInt(R.id.notif_fond, "setBackgroundColor", couleur_radio);

        notification.contentView = contentView;

        notificationManager = (NotificationManager) getActivity().getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIF_ID, notification);
    }



    /* *********************************************************************************************
     * Fcontionnalités sur l'afficheur
     * ********************************************************************************************/

    private void updatePlayStatus() {
//        ImageView play = (ImageView) rootView.findViewById(R.id.icon_play);

        if ("stop".equals(etat_lecture) || "pause".equals(etat_lecture)) {
//            play.setBackground(getActivity().getDrawable(R.drawable.ic_equalizer0));
        } else {
//            play.setBackground(getActivity().getDrawable(R.drawable.ic_equalizer));
        }
    }

    private void volume() {

        AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

//        ImageView play = (ImageView) rootView.findViewById(R.id.icon_volume);

        if (currentVolume == 0) {
//            play.setBackground(getActivity().getDrawable(R.drawable.volume0));
        } else if (currentVolume < 4) {
//            play.setBackground(getActivity().getDrawable(R.drawable.volume1));
        } else if (currentVolume < 7) {
//            play.setBackground(getActivity().getDrawable(R.drawable.volume2));
        } else if (currentVolume < 10) {
//            play.setBackground(getActivity().getDrawable(R.drawable.volume3));
        } else if (currentVolume < 13) {
//            play.setBackground(getActivity().getDrawable(R.drawable.volume4));
        } else if (currentVolume < 16) {
//            play.setBackground(getActivity().getDrawable(R.drawable.volume5));
        }
    }


    public class Control_Volume extends ContentObserver {
        private int previousVolume;
        private final Context context;

        public Control_Volume(Context c, Handler handler) {
            super(handler);
            context = c;

            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            previousVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            volume();

            AudioManager audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            int currentVolume = audio.getStreamVolume(AudioManager.STREAM_MUSIC);

            int delta = previousVolume - currentVolume;

            if (delta > 0) {
                previousVolume = currentVolume;
            } else if (delta < 0) {
                previousVolume = currentVolume;
            }
        }
    }


    private void getBitRate() {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            public void run() {
                bitRate();
                handler.postDelayed(this, 2000);
            }
        }, 1);
    }

    private void bitRate() {
        final int uid = android.os.Process.myUid();
        final long received = TrafficStats.getUidRxBytes(uid) / 1024;

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {

            @SuppressLint("SetTextI18n")
            public void run() {

                long current = TrafficStats.getUidRxBytes(uid) / 1024;
                long total = current - received;

                long ByteToBit = total * 8;
                String bitrate = String.valueOf(ByteToBit);

                TextView BitRate = (TextView) rootView.findViewById(R.id.bitrate);
                BitRate.setText(bitrate + " kb/s");
            }
        }, 1000);
    }


    private class Etat_player extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            TextView status = (TextView) rootView.findViewById(R.id.etat);
            String action_lecteur = intent.getStringExtra("state");
            status.setText(action_lecteur);

            SharedPreferences.Editor editor = préférences.edit();

            if ("Déconnecté".equals(action_lecteur)) {
                etat_lecture = "stop";
                editor.putString("etat", etat_lecture);
            }

            editor.putString("action", action_lecteur);
            editor.apply();

            TextView StationTextView = (TextView) rootView.findViewById(R.id.station);
            String lecture = intent.getStringExtra("name");
            StationTextView.setText(lecture);

            nom_radio = préférences.getString("name", nom_radio_pref);
            action_lecteur = préférences.getString("action", etat_lecture_pref);
            createNotification(nom_radio, action_lecteur);
        }
    }


    private void settings() {
        Intent intent = new Intent();
        intent.setClass(getActivity(), SettingsFragment.class);
        startActivityForResult(intent, USER_SETTINGS_REQUEST);
    }



    /* *********************************************************************************************
     * Création du fichier de préférence
     * ********************************************************************************************/

    private void createPref() {

        context = getActivity();

        String files_path = (context.getFilesDir().getPath());
        String pref_path = files_path.replace("files", "shared_prefs");


        File newRep = new File(pref_path);
        if (!newRep.exists()) {
            //noinspection ResultOfMethodCallIgnored
            newRep.mkdir();
        }

        String fichier_pref1 = "<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n" +
                "<map>\n" +
                "    <string name=\"radio1a\">BG Alternative</string>\n" +
                "    <string name=\"radio1b\">http://87.121.166.208:8000/_a</string>\n" +
                "    <string name=\"radio2a\">RTL</string>\n" +
                "    <string name=\"radio2b\">http://streaming.radio.rtl.fr/rtl-1-48-192</string>\n" +
                "    <string name=\"radio3a\">RMC</string>\n" +
                "    <string name=\"radio3b\">http://rmc.scdn.arkena.com/rmc.mp3</string>\n" +
                "    <string name=\"radio4a\">Fun Radio</string>\n" +
                "    <string name=\"radio4b\">http://streaming.radio.funradio.fr/fun-1-48-192</string>\n" +
                "    <string name=\"radio5a\">Virgin Radio</string>\n" +
                "    <string name=\"radio5b\">http://vr-live-mp3-128.scdn.arkena.com/virginradio.mp3</string>\n" +
                "    <string name=\"name\">BG Alternative</string>\n" +
                "    <string name=\"action\"></string>\n" +
                "    <string name=\"etat\">stop</string>\n" +
                "    <string name=\"url\">http://87.121.166.208:8000/_a</string>\n" +
                "    <int name=\"colorRADIO\" value=\"-1107296256\" />\n" +
                "    <int name=\"colorTXT\" value=\"16777215\" />" +
                "</map>\n";

        File file = new File(pref_path + "/" + fichier_préférence + ".xml");

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(fichier_pref1.getBytes());
            fos.close();
        } catch (Exception ignored) {
        }
    }


    /**
     * The fragment argument representing the section number for this
     * fragment.
     */
    private static final String ARG_SECTION_NUMBER = "section_number";

    public RadioFragment() {
    }


}