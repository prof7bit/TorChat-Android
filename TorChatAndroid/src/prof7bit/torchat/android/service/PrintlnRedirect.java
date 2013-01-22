package prof7bit.torchat.android.service;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import android.util.Log;

public class PrintlnRedirect extends OutputStream {
	
	public enum LogLevel{
		DEBUG,
		ERROR
	}

	private String buffer = "";
	private String TAG;
	private LogLevel level = LogLevel.DEBUG;

	public PrintlnRedirect(String tag, LogLevel lvl){
		TAG = tag;
		level = lvl;
	}

	/**
	 * Install redirection of stdout and stderr to android log
	 * @param tag the text of the tag column in the android log
	 */
	public static void Install(String tag){
		System.setOut(new PrintStream(new PrintlnRedirect(tag, LogLevel.DEBUG)));
		System.setErr(new PrintStream(new PrintlnRedirect(tag, LogLevel.ERROR)));
	}
	
	@Override
	public void write(int oneByte) throws IOException {
		
		if (oneByte == 0x0a){
			switch(level){
				case DEBUG:
					Log.d(TAG, buffer);
					break;
				case ERROR:
					Log.e(TAG, buffer);
					break;
			}
			buffer = "";
		}else{
			buffer += (char)oneByte;
		}
	}
}
