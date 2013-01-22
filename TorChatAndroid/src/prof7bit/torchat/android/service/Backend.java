package prof7bit.torchat.android.service;
import java.io.IOException;
import prof7bit.torchat.android.R;
import prof7bit.torchat.android.gui.TorChat;
import prof7bit.torchat.core.Client;
import prof7bit.torchat.core.ClientHandler;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.widget.Toast;


public class Backend extends Service implements ClientHandler {
	
	private NotificationManager nMgr;
	private int NOTIFICATION = 10429; //Any unique number for this notification

	private Client client;
	
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
			client = new Client(this, 11009);
			showNotification();
		} catch (IOException e) {
			// TODO what to do now?
			e.printStackTrace();
		}
	}	
	
	@Override	
	public void onDestroy() {	
		try {
			client.close();
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
}
