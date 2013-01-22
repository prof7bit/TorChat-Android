package prof7bit.torchat.android.gui;

import prof7bit.torchat.android.R;
import prof7bit.torchat.android.service.Backend;
import prof7bit.torchat.android.service.PrintlnRedirect;
import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * Main activity that is visible on start of application.
 * It mainly shows the roster (buddy list) and has an options
 * menu where all global settings can be set. It will start the
 * service if it is not already running and it is also the only 
 * place from where the service can be stopped.
 *  
 * @author Bernd Kreuss <prof7bit@gmail.com>
 */
public class TorChat extends SherlockActivity {
		
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		PrintlnRedirect.Install("TorChat");
		System.out.println("onCreate");
		setContentView(R.layout.l_roster);
		doStartService();
		doRegisterEventListeners();
	}
	
	@Override
	public void onSaveInstanceState(Bundle savedInstanceState) {
		super.onSaveInstanceState(savedInstanceState);
		System.out.println("onSaveInstanceState");
	}	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getSupportMenuInflater();
	    inflater.inflate(R.menu.m_roster, menu);
	    return true;
	}

	private void doRegisterEventListeners(){
//		findViewById(R.id.menu_quit).setOnClickListener(new OnClickListener() {
//			public void onClick(View v) {
//				doQuit();
//			}
//		});
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    switch (item.getItemId()) {
	        case R.id.menu_quit:
	            doQuit();
	            return true;
	        case R.id.menu_settings:
	            doShowSettings();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
	
	/**
	 * Start the Background service.
	 */
	private void doStartService(){
		System.out.println("doStartService");
		startService(new Intent(this, Backend.class));
	}

	/**
	 * Stop the Background service.
	 */
	private void doStopService(){
		System.out.println("doStopService");
		stopService(new Intent(this, Backend.class));
	}
	
	/**
	 * Quit entirely (stop service and close the main activity).
	 */
	private void doQuit(){
		System.out.println("doQuit");
		doStopService();
		finish();
	}
	
	/**
	 * Show the setting dialog
	 */
	private void doShowSettings(){
		System.out.println("doShowSettings");
		// nothing yet
	}
}
