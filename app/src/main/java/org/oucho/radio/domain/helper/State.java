/**
 *  Radio for android, internet radio.
 *
 * Copyright (C) 2016 Old Geek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.oucho.radio.domain.helper;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.oucho.radio.domain.player.Player;

public class State
{
   private static final String intent_state = "org.oucho.radio.STATE";
   static SharedPreferences preferences;

    public static final String STATE_STOP         = "Stopped";
    public static final String STATE_ERROR        = "Error";
    public static final String STATE_COMPLETE     = "Complete";;
    public static final String STATE_PAUSE        = "Paused";;
    public static final String STATE_PLAY         = "Playing...";
    public static final String STATE_BUFFER       = "Buffering...";
    public static final String STATE_DISCONNECTED = "Disconnected!";


    /*   public static final String STATE_STOP         = preferences.getString("state_stop", "Stopped");
   public static final String STATE_ERROR        = preferences.getString("state_error", "Error");
   public static final String STATE_COMPLETE     = preferences.getString("state_complete", "Complete");;
   public static final String STATE_PAUSE        = preferences.getString("state_pause", "Paused");;
   public static final String STATE_PLAY         = preferences.getString("state_play", "Playing...");;
   public static final String STATE_BUFFER       = preferences.getString("state_buffer", "Buffering...");;
   public static final String STATE_DISCONNECTED = preferences.getString("state_disconnected", "Disconnected!");;
*/
   private static String current_state = STATE_STOP;

   //private static boolean current_isNetworkUrl = false;

   public static void setState(Context context, String s)
   {
      if ( s == null )
         return;

      current_state = s;

      //current_isNetworkUrl = isNetworkUrl;

      Intent intent = new Intent(intent_state);
      intent.putExtra("state", current_state);
      intent.putExtra("url", Player.url);
      intent.putExtra("name", Player.name);

      context.sendBroadcast(intent);
   }


   public static boolean is(String s)
      { return current_state.equals(s); }

   public static String text()
   {
      if (is(STATE_STOP))
          return "Stop";

       if (is(STATE_PLAY))
           return "Lecture";

       if (is(STATE_PAUSE))
           return "Pause";

       if (is(STATE_BUFFER))
           return "Chargement...";

       if (is(STATE_COMPLETE))
           return "Complété";

      if (is(STATE_ERROR))
          return "Erreur";

      if (is(STATE_DISCONNECTED))
          return "Pas de connexion réseau";

      // Should not happen.
      return "Unknown";
   }

   // Paused is not in any of the following classes.
   //
   public static boolean isPlaying()
      { return is(STATE_PLAY) || is(STATE_BUFFER); }

   public static boolean isStopped()
      { return State.is(STATE_STOP) || State.is(STATE_ERROR) || State.is(STATE_COMPLETE); }

   public static boolean isWantPlaying()
      { return isPlaying() || is(STATE_ERROR); }
}

