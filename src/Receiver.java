import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;


public class Receiver {
	//Private members for network code and file output
	private int OutPort;
	private int InPort;
	private DatagramSocket InputSocket;
	private DatagramSocket OutputSocket;
	private InetAddress Host;
	private String Filename;
	private PrintWriter ArrLog;
	private PrintWriter FileOut;
	
	//Constructor
	public Receiver(String Hostname, int OutPort, int InPort, String FileOutputName){
		this.OutPort = OutPort;
		this.InPort = InPort;
		Filename = FileOutputName;
		try {
			ArrLog = new PrintWriter("arrival.log","UTF-8");
			Host = InetAddress.getByName(Hostname);
			InputSocket = new DatagramSocket(InPort);
			OutputSocket = new DatagramSocket(OutPort);
			FileOut = new PrintWriter(FileOutputName);
		} catch (UnknownHostException e) {
			System.out.println("Invalid Host Name");
			System.exit(-1);
		} catch (SocketException e) {
			System.out.println("One or more port numbers invalid or in use");
			System.exit(-1);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//Receive a file
	public void receive(){
		//Start out with nothing
		//This way if 0 is missed we don't retransmit that we have it already
		//Expected seq will be this + 1
		int LastSeq = -1;
		try{
			while(true){
				//Setup to receive a packet and receive it
				byte[] ReceivePacket = new byte[512];
				DatagramPacket Receiver = new DatagramPacket(ReceivePacket,512);
				InputSocket.receive(Receiver);
				packet InputPacket = packet.parseUDPdata(ReceivePacket);
				
				//If this is the packet we wanted look inside it
				if (InputPacket.getSeqNum() == (LastSeq+1)%32){
					//Increment since we got what we wanted
					LastSeq++;
					
					//If EOT create an EOT packet, send it and complete reception of file
					if (InputPacket.getType() == 2){
						packet EOT = packet.createEOT(LastSeq);
						DatagramPacket Send = new DatagramPacket(EOT.getUDPdata(),512,Host,OutPort);
						OutputSocket.send(Send);
						FileOut.close();
						ArrLog.close();
						OutputSocket.close();
						InputSocket.close();
						System.exit(-1);
					}else {
						//Else its a data packet
						//Write its data to the file and send an ACK
						ArrLog.println(InputPacket.getSeqNum());
						FileOut.print(new String(InputPacket.getData()));
						packet ACK = packet.createACK(InputPacket.getSeqNum());
						DatagramPacket Send = new DatagramPacket(ACK.getUDPdata(),512,Host,OutPort);
						OutputSocket.send(Send);
						
					}
				} else if (LastSeq >= 0) {
					//Else its something out of order
					//If LastSeq >= 0 we have recieved something so we can send an ACK for that
					//Otherwise do nothing since we dont have anything received. 
					ArrLog.println(InputPacket.getSeqNum());
					packet ACK = packet.createACK(LastSeq%32);
					DatagramPacket Send = new DatagramPacket(ACK.getUDPdata(),512,Host,OutPort);
					OutputSocket.send(Send);
					
				}
			}
		}catch (IOException e){
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
	}
	
	public static void main(String args[]) throws Exception{
		//Parse input, create receiver and receive file
		if (args.length != 4){
			System.out.println("Invalid number of command line params");
			System.out.println("Please refer to README");
		}
		Receiver ReceiveFile = new Receiver (args[0],Integer.parseInt(args[1]),Integer.parseInt(args[2]),args[3]);
		ReceiveFile.receive();
	}
	
}
