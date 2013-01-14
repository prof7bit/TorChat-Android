package prof7bit.torchat.android.service;
import java.io.IOException;
import java.nio.ByteBuffer;

import prof7bit.reactor.TCPHandler;
import prof7bit.reactor.ListenPort;
import prof7bit.reactor.ListenPortHandler;
import prof7bit.reactor.Reactor;
import prof7bit.reactor.TCP;
import prof7bit.torchat.android.R;
import prof7bit.torchat.android.gui.TorChat;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;


public class Backend extends Service implements ListenPortHandler{
	
	private NotificationManager nMgr;
	private int NOTIFICATION = 10429; //Any unique number for this notification

	private Reactor reactor;
	private ListenPort listenPort;
	
	@SuppressWarnings("deprecation")
	private void showNotification() {
	    CharSequence title = getText(R.string.service_running);
	    CharSequence title2 = getText(R.string.service_running_detail);

	    // Set the icon, scrolling text and timestamp
	    Notification notification = new Notification(R.drawable.ic_stat_service_running, title, System.currentTimeMillis());
	    notification.flags = Notification.FLAG_ONGOING_EVENT;
	    
	    // The PendingIntent to launch our activity if the user selects this notification
	    Intent toLaunch = new Intent(this, TorChat.class);
	    toLaunch.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
	    toLaunch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
	    PendingIntent contentIntent = PendingIntent.getActivity(this, 0, toLaunch , 0);

	    // Set the info for the views that show in the notification panel.
	    notification.setLatestEventInfo(this, title, title2, contentIntent);
	    
	    // Send the notification.
	    nMgr.notify(NOTIFICATION, notification);
	}	
	
	private void removeNotification(){
		nMgr.cancel(NOTIFICATION);
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onCreate() {
		nMgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		PrintlnRedirect.Install("TorChat");
		
		try {
			reactor = new Reactor();
			listenPort = new ListenPort(reactor, this);
			listenPort.listen(11009);
			showNotification();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	@Override	
	public void onDestroy() {	
		try {
			reactor.close();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		removeNotification();
		Toast.makeText(this, R.string.toast_service_stopped, Toast.LENGTH_LONG).show();
	}	
	
	@Override
	public void onStart(Intent intent, int startid) {
		// nothing
	}

	@Override
	public TCPHandler onAccept(TCP tcp) {
		System.out.println("onAccept");
		return new TCPHandler() {

			@Override
			public void onConnect() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onDisconnect(Exception e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onReceive(ByteBuffer buf) {
				// TODO Auto-generated method stub
				
			}
		};
	}
}
