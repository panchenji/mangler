/*
 * Copyright 2010 Daniel Sloof <daniel@danslo.org>
 *
 * This file is part of Mangler.
 *
 * $LastChangedDate: 2010-07-06 17:50:28 +0200 (Tue, 06 Jul 2010) $
 * $Revision: 958 $
 * $LastChangedBy: killy $
 * $URL: http://svn.mangler.org/mangler/trunk/android/src/org/mangler/ServerView.java $
 *
 * Mangler is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Mangler is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mangler.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mangler.android;

import java.util.HashMap;
import java.util.Iterator;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SimpleAdapter;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.nullwire.trace.ExceptionHandler;

public class ServerView extends TabActivity {
	// Server ID that we're connected to
	private static int serverid;

	// Database connection
	private ManglerDBAdapter dbHelper;

	// Actions.
	public static final String CHANNELLIST_ACTION		= "org.mangler.android.ChannelListAction";
	public static final String USERLIST_ACTION			= "org.mangler.android.UserListAction";
	public static final String CHATVIEW_ACTION			= "org.mangler.android.ChatViewAction";
	public static final String NOTIFY_ACTION			= "org.mangler.android.NotifyAction";
	public static final String TTS_NOTIFY_ACTION		= "org.mangler.android.TtsNotifyAction";

	// Events.
	public static final int EVENT_CHAT_JOIN	  = 1;
	public static final int EVENT_CHAT_LEAVE  = 2;
	public static final int EVENT_CHAT_MSG	  = 3;

	// Menu options.
	private final int OPTION_JOIN_CHAT  = 1;
	private final int OPTION_DISCONNECT = 3;
	private final int OPTION_SETTINGS = 4;
	
	// List adapters.
	private SimpleAdapter channelAdapter;
	private SimpleAdapter userAdapter;
	
	// Text to Speech
	TextToSpeech tts = null;

	// State variables.
	private boolean userInChat = false;
	
	// WakeLock
	private PowerManager.WakeLock wl;

    @Override
    public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.server_view);
        
        // Get the server id that we're connected to and set up the database adapter
        serverid = getIntent().getIntExtra("serverid", 0);
        dbHelper = new ManglerDBAdapter(this);
        dbHelper.open();
        
        // Send crash reports to server
        ExceptionHandler.register(this, "http://www.mangler.org/errors/upload.php");
        
        // Volume controls.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
        // Text to speech init
        if (tts == null) {
        	tts = new TextToSpeech(this, null);
        }

        // Add tabs.
        TabHost tabhost = getTabHost();
        tabhost.addTab(tabhost.newTabSpec("talk").setContent(R.id.talkView).setIndicator("Talk"));
        tabhost.addTab(tabhost.newTabSpec("channel").setContent(R.id.channelView).setIndicator("Channels"));
        tabhost.addTab(tabhost.newTabSpec("user").setContent(R.id.userView).setIndicator("Users"));
    	tabhost.addTab(tabhost.newTabSpec("chat").setContent(R.id.chatView).setIndicator("Chat"));

        // Create adapters.
	    channelAdapter 	= new SimpleAdapter(this, ChannelList.data, R.layout.channel_row, new String[] { "channelname", "passworded" }, new int[] { R.id.crowtext, R.id.crowpass } );
	    userAdapter 	= new SimpleAdapter(this, UserList.data, R.layout.user_row, new String[] { "userstatus", "username", "channelname" }, new int[] { R.id.urowimg, R.id.urowtext, R.id.urowid } );

	    // Set adapters.
	    ((ListView)findViewById(R.id.channelList)).setAdapter(channelAdapter);
	    ((ListView)findViewById(R.id.userList)).setAdapter(userAdapter);

	    // List item clicks.
	    ((ListView)findViewById(R.id.channelList)).setOnItemClickListener(onListClick);
	    ((ListView)findViewById(R.id.userList)).setOnItemClickListener(onListClick);
	    ((ListView)findViewById(R.id.userList)).setOnItemLongClickListener(onLongListClick);

	    // Register receivers.
        registerReceiver(chatReceiver, new IntentFilter(CHATVIEW_ACTION));
        registerReceiver(channelReceiver, new IntentFilter(CHANNELLIST_ACTION));
        registerReceiver(userReceiver, new IntentFilter(USERLIST_ACTION));
        registerReceiver(notifyReceiver, new IntentFilter(NOTIFY_ACTION));
        registerReceiver(ttsNotifyReceiver, new IntentFilter(TTS_NOTIFY_ACTION));

        // Control listeners.
	    ((EditText)findViewById(R.id.message)).setOnKeyListener(onChatMessageEnter);
	    ((Button)findViewById(R.id.talkButton)).setOnTouchListener(onTalkPress);
    
	    // Restore state.
	    if(savedInstanceState != null) {
	    	userInChat = savedInstanceState.getBoolean("chatopen");
	    	((TextView)findViewById(R.id.messages)).setText(savedInstanceState.getString("chatmessages"));
	    	((EditText)findViewById(R.id.message)).setEnabled(userInChat);
	    }
	
	    ((EditText)findViewById(R.id.message)).setVisibility(userInChat ? TextView.VISIBLE : TextView.GONE);
	    
	    // Get a wakelock to prevent sleeping and register an onchange preference callback
	    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Mangler");
		boolean prevent_sleep = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("prevent_sleep", false);
		if (prevent_sleep) {
			if (!wl.isHeld()) {
				wl.acquire();
			}
		}
		
		// Set our xmit volume level
		VentriloEventData userRet = new VentriloEventData();
		VentriloInterface.getuser(userRet, VentriloInterface.getuserid());
		int level = dbHelper.getVolume(serverid, new String(userRet.text.name, 0, (new String(userRet.text.name).indexOf(0))));
		Log.e("mangler", "setting xmit volume to " + level);
		VentriloInterface.setxmitvolume(level);
		
		// Set everyone else's volume
		Log.e("mangler", "running tasks after login");
		// loop through all connected users and set their volume from the database
		for (Iterator<HashMap<String, Object>> iterator = UserList.data.iterator(); iterator.hasNext();) {
			short userid = (Short)iterator.next().get("userid");
			VentriloEventData evdata = new VentriloEventData();
			VentriloInterface.getuser(evdata, userid);
			String username = new String(evdata.text.name, 0, (new String(evdata.text.name).indexOf(0)));
			level = dbHelper.getVolume(serverid, new String(evdata.text.name, 0, (new String(evdata.text.name).indexOf(0))));
			VentriloInterface.setuservolume((short) userid, level);
			Log.e("mangler", "setting " + username + " (id: " + userid + ") to volume " + level);
		}
    }
    
    @Override
    protected void onResume() {
		boolean prevent_sleep = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("prevent_sleep", false);
    	super.onResume();
		if (prevent_sleep) {
			if (!wl.isHeld()) {
				wl.acquire();
			}
		} else {
			if (wl.isHeld()) {
				wl.release();
			}
		}
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    	outState.putString("chatmessages", ((TextView)findViewById(R.id.messages)).getText().toString());
    	outState.putBoolean("chatopen", userInChat);
    	super.onSaveInstanceState(outState);
    }
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_CAMERA) {
			startPtt();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_CAMERA) {
			stopPtt();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

    @Override
    public void onDestroy() {
    	super.onDestroy();

    	if (Recorder.recording()) {			
    		Recorder.stop();
    	}
    	
    	// release a wakelock if we have one
    	if (wl.isHeld()) {
    		wl.release();
    	}
    	if (tts != null) {
    		tts.shutdown();
    	}
    	
    	dbHelper.close();
    	
    	// Unregister receivers.
		unregisterReceiver(chatReceiver);
        unregisterReceiver(channelReceiver);
        unregisterReceiver(userReceiver);
        unregisterReceiver(notifyReceiver);
        unregisterReceiver(ttsNotifyReceiver);
    }
    

    public boolean onCreateOptionsMenu(Menu menu) {
    	 // Create our menu buttons.
    	menu.add(0, OPTION_JOIN_CHAT, 0, "Join chat").setIcon(R.drawable.menu_join_chat);
        menu.add(0, OPTION_SETTINGS, 0, "Settings").setIcon(R.drawable.menu_settings);
        menu.add(0, OPTION_DISCONNECT, 0, "Disconnect").setIcon(R.drawable.menu_disconnect);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
    	// Handle menu buttons.
    	final EditText message = (EditText)findViewById(R.id.message);
        switch(item.getItemId()) {
        	case OPTION_JOIN_CHAT:
        		if (!userInChat) {
        			VentriloInterface.joinchat();
        			message.setEnabled(true);
        			message.setVisibility(TextView.VISIBLE);
        			userInChat = true;
        			item.setIcon(R.drawable.menu_leave_chat);
        			item.setTitle("Leave chat");
        		} else {
        			VentriloInterface.leavechat();
        			message.setEnabled(false);
        			message.setVisibility(TextView.GONE);
        			userInChat = false;
        			item.setIcon(R.drawable.menu_join_chat);
        			item.setTitle("Join chat");
        		}
        		break;

        	case OPTION_DISCONNECT:
        		VentriloInterface.logout();
        		finish();
        		return true;
        		
        	case OPTION_SETTINGS:
				Intent intent = new Intent(ServerView.this, Settings.class);
				startActivity(intent);
        		return true;

        	default:
        		return false;
        }
        return true;
    }

	private BroadcastReceiver chatReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			final TextView messages = (TextView)findViewById(R.id.messages);
			switch(intent.getIntExtra("event", -1)) {
				case EVENT_CHAT_JOIN:
					messages.append("\n* " + intent.getStringExtra("username") + " has joined the chat.");
					break;

				case EVENT_CHAT_LEAVE:
					messages.append("\n* " + intent.getStringExtra("username") + " has left the chat.");
					break;

			 	case EVENT_CHAT_MSG:
			 		messages.append("\n" + intent.getStringExtra("username") + ": " + intent.getStringExtra("message"));
			 		break;
			 }

			// Scroll to bottom.
			final ScrollView chatscroll = (ScrollView)findViewById(R.id.chatScroll);
			chatscroll.post(new Runnable() {
				public void run() {
					chatscroll.fullScroll(ScrollView.FOCUS_DOWN);
				}
			});
		}
	};
	
	private BroadcastReceiver userReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String username = intent.getStringExtra("username");
			int id = intent.getIntExtra("id", 0);
			if (username != null) {
				int level = dbHelper.getVolume(serverid, username);
				VentriloInterface.setuservolume((short)id, level);
				Log.e("mangler", "setting " + username + " (id: " + id + ") to volume " + level);
			}
			userAdapter.notifyDataSetChanged();
		}
	};

	
	private BroadcastReceiver channelReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			channelAdapter.notifyDataSetChanged();
		}
	};
	
	private BroadcastReceiver notifyReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String message = intent.getExtras().getString("message");
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
		}
	};
	
	private BroadcastReceiver ttsNotifyReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			boolean enable_tts = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("enable_tts", false);
			if (enable_tts) {
				String message = intent.getExtras().getString("message");
				tts.speak(message, TextToSpeech.QUEUE_ADD, null);
			}
		}
	};

	private void changeChannel(final short channelid) {
		if(VentriloInterface.getuserchannel(VentriloInterface.getuserid()) != channelid) {
			if(VentriloInterface.channelrequirespassword(channelid) > 0) {
				final EditText input = new EditText(this);
				// Create dialog box for password.
				AlertDialog.Builder alert = new AlertDialog.Builder(this)
					.setTitle("Channel is password protected")
					.setMessage("Please insert a password to join this channel.")
					.setView(input)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							VentriloInterface.changechannel(channelid, input.getText().toString());
						}
					})
					.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// No password entered.
						}
					});
				alert.show();
			}
			else {
				// No password required.
				VentriloInterface.changechannel(channelid, "");
			}
		}
	}

	private OnItemClickListener onListClick = new OnItemClickListener() {
		@SuppressWarnings("unchecked")
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			short channelid = (Short)((HashMap<String, Object>)(parent.getItemAtPosition(position))).get("channelid");
			changeChannel(channelid);
		}
	};
	
	private void setUserVolume(short id) {
		final CharSequence[] items = {"5 - Loudest", "4", "3", "2", "1 - Muted"};
		final short userid = id;
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		if (id == VentriloInterface.getuserid()) {
			alert.setTitle("Set Transmit Level");
		} else {
			alert.setTitle("Set User Volume Level");
		}
		VentriloEventData evdata = new VentriloEventData();
		VentriloInterface.getuser(evdata, id);
		final String username = new String(evdata.text.name, 0, (new String(evdata.text.name).indexOf(0)));
		alert.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				short[] levelList = { 0, 0, 39, 79, 118, 148 };
				int level = levelList[Integer.parseInt(items[item].toString().substring(0, 1))];
				if (userid == VentriloInterface.getuserid()) {
					Log.d("mangler", "setting xmit volume for me (" + username + ") to volume level " + level);
					dbHelper.setVolume(serverid, username, level);
					VentriloInterface.setxmitvolume(level);
				} else {
					Log.d("mangler", "setting volume for " + username + " to volume level " + level);
					dbHelper.setVolume(serverid, username, level);
					VentriloInterface.setuservolume(userid, level);
				} 
				
			}
		});		
		alert.show();
	}
	
	private OnItemLongClickListener onLongListClick = new OnItemLongClickListener() {
		@SuppressWarnings("unchecked")
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			short userid = (Short)((HashMap<String, Object>)(parent.getItemAtPosition(position))).get("userid");
			setUserVolume(userid);
			return true;
		}
	};

	private OnTouchListener onTalkPress = new OnTouchListener() {
		public boolean onTouch(View v, MotionEvent m) {
			boolean ptt_toggle = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("ptt_toggle", false);
			
			switch (m.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (!Recorder.recording()) {
						startPtt();
					} else if (ptt_toggle) {
						stopPtt();
					}
					break;
				case MotionEvent.ACTION_UP:
					if (! ptt_toggle) {
						stopPtt();
					}
					break;
			}
			return true;
		}
	};
	
	private void startPtt() {
		boolean force_8khz = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean("force_8khz", false);
		Recorder.setForce_8khz(force_8khz);
		if (!Recorder.start()) {
			Intent broadcastIntent = new Intent(ServerView.NOTIFY_ACTION);
			broadcastIntent.putExtra("message", "Unsupported recording rate for hardware: " + Integer.toString(Recorder.rate()) + "Hz");
		    sendBroadcast(broadcastIntent);
		    return;
		}
		((ImageView)findViewById(R.id.transmitStatus)).setImageResource(R.drawable.transmit_on);
	}
	
	private void stopPtt() {
		((TextView)findViewById(R.id.recorderInfo)).setText(
				"Last Xmit Info\n\n" +
				"Channel Rate: " + VentriloInterface.getchannelrate(VentriloInterface.getuserchannel(VentriloInterface.getuserid())) + "\n" +
				"Record Rate: " + Recorder.rate() + "\n" +
				"Buffer Size: " + Recorder.buflen() + "\n");
		Recorder.stop();
		((ImageView)findViewById(R.id.transmitStatus)).setImageResource(R.drawable.transmit_off);
	}

	private OnKeyListener onChatMessageEnter = new OnKeyListener() {
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
				// Send chat message.
				final EditText message = (EditText)findViewById(R.id.message);
				VentriloInterface.sendchatmessage(message.getText().toString());

				// Clear message field.
				message.setText("");

				// Hide keyboard.
				((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(message.getWindowToken(), 0);
				return true;
			}
			return false;
		}
	};
}
