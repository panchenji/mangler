package org.mangler;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class EventService extends Service {

	private final IBinder binder = new EventBinder();
	private boolean running = false;
	
	public class EventBinder extends Binder {
		EventService getService() {
			return EventService.this;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
    @Override
    public void onCreate() {
    	running = true;
    	(new Thread(eventRunnable)).start();
    }

    @Override
    public void onDestroy() {
    	running = false;
    }

    private Runnable eventRunnable = new Runnable() {
    	
    	private Intent broadcastIntent;
    	
        private String StringFromBytes(byte[] bytes) {
        	return new String(bytes, 0, (new String(bytes).indexOf(0)));
        }
    	
		public void run() {
			
	    	VentriloEventData data = new VentriloEventData();
	    	
	    	while(running) {
	    		
	    		VentriloInterface.getevent(data);
	    		
	    		switch(data.type) {
	    			case VentriloEvents.V3_EVENT_CHAT_MESSAGE:
	    				VentriloInterface.getuser(data, data.user.id);
	    				broadcastIntent = new Intent(ReceiverIntents.CHATVIEW_ACTION);
	    				broadcastIntent.putExtra("event", ServerView.EVENT_CHAT_MSG);
	    				broadcastIntent.putExtra("username", StringFromBytes(data.text.name));
	    				broadcastIntent.putExtra("message", StringFromBytes(data.data.chatmessage));
	    			    sendBroadcast(broadcastIntent);
	    				break;
	    				
	    			case VentriloEvents.V3_EVENT_CHAT_JOIN:
	    				VentriloInterface.getuser(data, data.user.id);
	    				broadcastIntent = new Intent(ReceiverIntents.CHATVIEW_ACTION);
	    				broadcastIntent.putExtra("event", ServerView.EVENT_CHAT_JOIN);
	    				broadcastIntent.putExtra("username", StringFromBytes(data.text.name));
	    			    sendBroadcast(broadcastIntent);
	    				break;
	    				
	    			case VentriloEvents.V3_EVENT_CHAT_LEAVE:
	    				VentriloInterface.getuser(data, data.user.id);
	    				broadcastIntent = new Intent(ReceiverIntents.CHATVIEW_ACTION);
	    				broadcastIntent.putExtra("event", ServerView.EVENT_CHAT_LEAVE);
	    				broadcastIntent.putExtra("username", StringFromBytes(data.text.name));
	    			    sendBroadcast(broadcastIntent);
	    				break;
	    				
	    			case VentriloEvents.V3_EVENT_USER_CHAN_MOVE:
    					if(data.user.id == VentriloInterface.getuserid()) {
    					//	int channel_rate = VentriloInterface.getchannelrate(data.channel.id);
    						
    						/*
    						player.rate(channel_rate);
    						recorder.stop();
    						recorder = new Recorder(channel_rate);*/
    					}
	    				break;
	    				
	    			case VentriloEvents.V3_EVENT_USER_LOGIN:
	    				if(data.user.id != 0) {
	    					Log.w("mangler", "sending broadcast");
		    				VentriloInterface.getuser(data, data.user.id);
		    				broadcastIntent = new Intent(ReceiverIntents.USERLIST_ACTION);
		    				broadcastIntent.putExtra("event", ServerView.EVENT_USER_ADD);
		    				broadcastIntent.putExtra("userid", data.user.id);
		    				broadcastIntent.putExtra("username", StringFromBytes(data.text.name));
		    			    sendBroadcast(broadcastIntent);
	    				}
	    				break;
	    				
	    			case VentriloEvents.V3_EVENT_USER_LOGOUT:
	    				broadcastIntent = new Intent(ReceiverIntents.USERLIST_ACTION);
	    				broadcastIntent.putExtra("event", ServerView.EVENT_USER_DEL);
	    				broadcastIntent.putExtra("userid", data.user.id);
	    			    sendBroadcast(broadcastIntent);
	    				break;
	    				
	    			case VentriloEvents.V3_EVENT_LOGIN_COMPLETE:
	    				//int lobby_rate = VentriloInterface.getchannelrate((short)0);
	    				/*
	    				player = new Player(lobby_rate);
	    				recorder = new Recorder(lobby_rate);*/
	    				break;
	    				
	    			case VentriloEvents.V3_EVENT_PLAY_AUDIO:
	    				//player.write(data.data.sample, data.pcm.length);
	    				break;
	
	    			case VentriloEvents.V3_EVENT_CHAN_ADD:
	    				VentriloInterface.getchannel(data, data.channel.id);
	    				broadcastIntent = new Intent(ReceiverIntents.CHANNELLIST_ACTION);
	    				broadcastIntent.putExtra("event", ServerView.EVENT_CHANNEL_ADD);
	    				broadcastIntent.putExtra("channelid", data.channel.id);
	    				broadcastIntent.putExtra("channelname", StringFromBytes(data.text.name));
	    			    sendBroadcast(broadcastIntent);
	    				break;
	    				
	    			default:
	    				Log.w("mangler", "Unhandled event of type: " + Integer.toString(data.type));
	    		}
	    	}
		}
    };

}