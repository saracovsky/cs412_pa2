//Cansu Yıldırım 21502891
//Mert Saraç 21401480

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.*;
import java.io.*;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.net.SocketException;


public class Sender{

	private static String file_path;
	private static int receiver_port;
	private static int window_size_N;
	private static int timeout;
	private static int sequence;
	private static DatagramSocket socket;
	private static Timer timer;
	private static int no_of_segs;
	private static int last_segment;
	private static FileInputStream input_stream = null;
	private static byte[] packet;
	private static int baseSeqNum;
	private static int curSeqNum;
	private static byte[] b;

	public static void main(String[] args) throws Exception{

		file_path = args[0];
		receiver_port =  Integer.parseInt(args[1]);
		window_size_N =  Integer.parseInt(args[2]);
		timeout =  Integer.parseInt(args[3]);
		sequence= 1;

		try {
			socket = new DatagramSocket();
			socket.connect(InetAddress.getByName("127.0.0.1"), receiver_port);
		}
		catch (SocketException e) {
			e.printStackTrace();
		}

		//read file and store into array
		File file = new File(file_path);
		b = new byte[(int) file.length()];
		int size = b.length;

		try {
			input_stream = new FileInputStream(file);
			input_stream.read(b);
        	input_stream.close();
		}
		catch (Exception e)
      	{
        	e.printStackTrace();
      	}

      	no_of_segs = (int)(size/1022);
      	//due to the number of data bytes in the last segment may be smaller than 1022
		last_segment = size - (no_of_segs*1022);

      	baseSeqNum = 1;
		curSeqNum = 1;
		//.................................................
    	int totalACK = 0;
    	Thread[] threadArr = new Thread[no_of_segs+1];
    	try{
	    	while(sequence<=no_of_segs+1 && totalACK<no_of_segs+1){
    		boolean flag = false;
	            while(!flag){
	            	if(sequence>=curSeqNum && sequence<=baseSeqNum+window_size_N-1){
						packet = null;
		    			packet = createPacket();
		    			SenderThread st = new SenderThread(packet,receiver_port,socket, timeout);    
		    			threadArr[sequence-1] = new Thread(st);
		    			threadArr[sequence-1].start();
		    			sequence++;
		    			curSeqNum++;
	            	}
	            	else{
	            		flag = true;
	            	}
	            }
	            int ackk = checkACK();
				if(ackk>=baseSeqNum && ackk<=baseSeqNum+window_size_N-1){
					if(!threadArr[ackk-1].isInterrupted()){
						threadArr[ackk-1].interrupt();
						baseSeqNum++;
						totalACK++;
					}
				}
			}
		    
		    byte[] finalb  = new byte[2];
		    finalb [0] = 0;
		    finalb [1] = 0;
		    SenderThread stf = new SenderThread(finalb,receiver_port,socket, timeout);    
		    Thread thr2 =new Thread(stf);       
		    thr2.start();       	

		}
		catch (Exception e)
      	{
        	e.printStackTrace();
      	}
     }

    public static int checkACK(){
    	try{
	    	byte[]answer = new byte[2];
	      	DatagramPacket packetReceive = new DatagramPacket(answer, answer.length); 
	      	socket.receive(packetReceive);
	      	byte[] res = new byte[4];
	      	res[0] = 0;
	      	res[1] = 0;
	      	res[2] =  packetReceive.getData()[0];
	      	res[3] =  packetReceive.getData()[1];
	      	int ackno = ByteBuffer.wrap(res).getInt();
            
            return ackno;
	      		
      	}
      	catch (Exception e)
      	{
        	e.printStackTrace();
      	}
      	return 0;
    } 

  	public static byte[] createPacket() {

		if(sequence == no_of_segs+1){
			packet = new byte[last_segment+2];
		}
		else{
			packet = new byte[1024];
		}
	  	//arrange sequence numbers
		byte[] data = new byte[2];

    	data[0] = (byte) (sequence & 0xFF);
    	data[1] = (byte) ((sequence >> 8) & 0xFF);

    	int high = data[1] >= 0 ? data[1] : 256 + data[1];
    	int low = data[0] >= 0 ? data[0] : 256 + data[0];

		//create packet
		packet[0] = (byte) high;
		packet[1] = (byte) low;

		int i = (sequence-1)*1022; 
		for(int j=2; j<packet.length; j++){
			packet[j] = b[i];
			i++;
		}

		return packet;	
		
	}
}

class SenderThread implements Runnable {
	byte[] packet = null;
	int recPort;
	DatagramSocket socket;
	int tmout;
  
	SenderThread(byte[] pkt, int receiver_port, DatagramSocket sckt, int timeout){
    	packet = pkt;
    	recPort = receiver_port;
    	socket = sckt;
    	tmout = timeout;
    

	}
/*
	public void sendPacket(byte[] packet, int recPort, DatagramSocket socket) {
		try{
    		DatagramPacket pkt = new DatagramPacket(packet, packet.length, InetAddress.getByName("127.0.0.1"), recPort);
	    	socket.send(pkt);
		}
		catch (Exception e)
      	{
        	e.printStackTrace();
      	}
	    
    }
*/
	public void run()  {
 		try {
     		while(true){
     			try{
    				DatagramPacket pkt = new DatagramPacket(packet, packet.length, InetAddress.getByName("127.0.0.1"), recPort);
	    			socket.send(pkt);
					}
				catch (Exception e) {
        			e.printStackTrace();
      			}
     			Thread.sleep(tmout);
     		}
		}
		catch (InterruptedException e) {
			return;
  		}
  	}
}
