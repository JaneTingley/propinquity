import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import processing.core.PApplet;
import processing.serial.Serial;
import xbee.XBeeDataFrame;
import xbee.XBeeReader;

// extra class to use multithreading:
// stop thread to wait for xbee response

public class XBeeManager implements Runnable {

  final int XBEE_BAUDRATE = 115200;
  final int XBEE_SERIAL_RESPONSE_TIMEOUT = 5 * 1000; // 5 sec
  
  Thread thread;
  String nodeID;
  boolean hasNI;
  boolean hasAllNIs;
  
  HashMap<String, String> nodeIDAndSerialPort;
 
  private static XBeeManager inst;
  public static XBeeManager instance() {
	  if (inst == null) inst = new XBeeManager();
	  return inst;
  }

  public XBeeManager() { }

  public void init() {
    nodeIDAndSerialPort = new HashMap<String, String>();
    
    if (thread != null) 
      return;
    thread = new Thread(this);
    thread.start();
  }
  
  public void run() {

    String[] serialPortList = Serial.list();
    String osName = System.getProperty("os.name");
    
    // for all serial ports
    for (int i=0; i<serialPortList.length; i++) {
      
      // on a Mac, filter out the ports that don't start by tty.usbserial
      if ((osName.indexOf("Mac") != -1) && (serialPortList[i].indexOf("tty.usbserial") == -1))
        continue;
      readSerialPort(serialPortList[i]);

    }
    hasAllNIs = true;
    
    DiscoverTest.game.println("Local XBees found: " + getNodeIDs() + ".");

    //clear thread
    thread = null; 
  }
  
  public boolean hasAllNIs() {
    return hasAllNIs;
  }
  
  public void readSerialPort(String port) {
	  DiscoverTest.game.println(" Connecting to port: " + port);
      Serial serial = new Serial(DiscoverTest.game, port, XBEE_BAUDRATE); 
      
      // get node identifier from local xbee
      hasNI = false;  
      XBeeReader localXbee = new XBeeReader(DiscoverTest.game, serial);
      // the following lines give us debugging output of the xbee library - TODO
      localXbee.startXBee();      
      localXbee.getNI();
      
      // wait for xbee event until timeout, break if we got it
      int start = DiscoverTest.game.millis();
      while (!hasNI && DiscoverTest.game.millis() < start+XBEE_SERIAL_RESPONSE_TIMEOUT) { 
        try { 
          Thread.sleep(1);
          //print(".");
        }
        catch(InterruptedException ie) {
          ie.printStackTrace(); 
        }  
        if (hasNI) {
        	DiscoverTest.game.println(nodeID);
          nodeIDAndSerialPort.put(nodeID, port);
          break;
        }
      }
      
      //clean up      
      localXbee.stopXBee();
      
      // Stop program if we still has no xbee after timeout
      if (!hasNI) {
    	  DiscoverTest.game.println("Serial timeout and no local XBee found.");
        System.exit(1);
      }
  }  
  
  //Get a XBeeReader of the XBee with the matching NodeIdentifier (NI)
  public XBeeReader reader(String ni) {
    String serialPort = (String) nodeIDAndSerialPort.get(ni);
    if (serialPort == null) 
      return null;
    XBeeReader xbee = new XBeeReader(DiscoverTest.game, new Serial(DiscoverTest.game, serialPort, XBEE_BAUDRATE));
    return xbee;
  }
  
  public void xBeeEvent(XBeeReader xbee) { 
    XBeeDataFrame data = xbee.getXBeeReading();
    data.parseXBeeRX16Frame();
    
    int[] buffer = data.getBytes();
    nodeID = "";
    for(int i = 0; i < buffer.length; i++) {
      nodeID += (char) buffer[i];
    }
    
    hasNI = true;
  }  
   
  public String getNodeIDs() {
    Set nodeIDs = nodeIDAndSerialPort.keySet();
    Iterator it = nodeIDs.iterator();
    String nodeIDString = "";
    
    while (it.hasNext())    
      nodeIDString += (String)it.next() + ", ";
    
    if (nodeIDString.length() < 2) 
      return "";
    return nodeIDString.substring(0, nodeIDString.length()-2);
  }
    
}
