import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import javax.swing.Timer;
import java.util.Vector;


public class Sender {
	//Private members to handle transfer and file reading/creation
	private PrintWriter SeqLog;
	private PrintWriter AckLog;
	private FileInputStream FileIn;
	private InputStreamReader Reader;
	private BufferedReader Input;
	private DatagramSocket SocketIn;
	private DatagramSocket SocketOut;
	private int WINDOW_SIZE = 10;
	private int SendPort;
	private int RecPort;
	private Vector<packet> Window = new Vector<packet>();
	private InetAddress Host;
	
	//This is executed when we timeout
	private ActionListener timeOut = new ActionListener() {
		public void actionPerformed(ActionEvent e){
			for (packet p : Window){
				DatagramPacket UDPSend = new DatagramPacket(p.getUDPdata(),512,Host,SendPort);
				try {
					SocketOut.send(UDPSend);
					SeqLog.println(p.getSeqNum());
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}
	};
	private Timer TimeOutTimer;
	
	//Constructor
	public Sender(String EmuAdd,int SendPort,int RecPort,String Filename){
		try {
			SeqLog = new PrintWriter("seqnum.log","UTF-8");
			AckLog = new PrintWriter("ack.log","UTF-8");
			this.SendPort = SendPort;
			this.RecPort = RecPort;
		    SocketOut = new DatagramSocket(SendPort);
			SocketIn = new DatagramSocket(RecPort);
			FileIn = new FileInputStream(Filename);
			Reader = new InputStreamReader(FileIn,"UTF-8");
			Input = new BufferedReader(Reader);
			Host = InetAddress.getByName(EmuAdd);
			TimeOutTimer = new Timer(500,timeOut);
		} catch (SocketException e) {
			System.out.println("One of the selected sockets is not available!");
			System.exit(-1);
		} catch (FileNotFoundException e) {
			System.out.println("File could not be found");
			System.exit(-1);
		} catch (UnsupportedEncodingException e) {
			//Unreachable
			e.printStackTrace();
			System.exit(-1);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//Send the file
	public void SendFile(){
		try {
			//Start with SeqNum 0
			int SeqNum = 0;
			while (true){
				//While there is something to send we fill up the window
				while (Input.ready() && Window.size() < WINDOW_SIZE){
					//Create a string builder and grab 500 chars of our file
					StringBuilder FileChunk = new StringBuilder();
					int i = 0;
					while (i < 500 && Input.ready()){
						FileChunk.append((char)Input.read());
						i++;
					}
					//Create a packet with our chunk, add it to the window
					packet SendPacket = packet.createPacket(SeqNum%32, FileChunk.toString());
					Window.add(SendPacket);
					
					//Send out the packet we have created
					DatagramPacket UDPSend = new DatagramPacket(SendPacket.getUDPdata(),512,Host,SendPort);
					SocketOut.send(UDPSend);
					SeqLog.println(SendPacket.getSeqNum());
					
					//Increment sending Seqnumber and adjust timer
					//restart() ensures timer starts in a fresh cycle
					SeqNum++;
					if (!TimeOutTimer.isRunning()) TimeOutTimer.restart(); 
				}
				//Now know that our input is done or the window is full
				//If there is an ack to wait for wait
				//Otherwise we know our window is empty and nothing to send so we're done
				if (Window.size() > 0){
					//Setup to receieve a packet and read it
					byte[] ReceiveData = new byte[512];
					DatagramPacket ACKReceive = new DatagramPacket(ReceiveData,512);
					SocketIn.receive(ACKReceive);
					packet ACKPacket = packet.parseUDPdata(ReceiveData);
					AckLog.println(ACKPacket.getSeqNum());
					
					//Now we need to alter our window according to the above ACK
					int ACKIndex = -1;
					//For loop to find where in the window this ACK belongs
					for (int i = 0; i < Window.size(); i++){
						if (ACKPacket.getSeqNum() == Window.elementAt(i).getSeqNum()){
							ACKIndex = i;
							break;
						}
					}
					//If the ACK is one we should receive adjust window
					if (ACKIndex != -1){
						//Note the window is in order, so we can remove everything before this ACK
						for (int i = 0; i <= ACKIndex; i++) Window.remove(0);
					}
					//If window is empty stop the timer
					if (Window.size() == 0) TimeOutTimer.stop();
					else TimeOutTimer.restart();
					
				} else {
					//We know we are done sending and acknowledging all data
					//Now we must do our EOT transmissions
					packet EOT = packet.createEOT(SeqNum);
					DatagramPacket EOTPacket = new DatagramPacket(EOT.getUDPdata(),512,Host,SendPort);
					SocketOut.send(EOTPacket);
					
					//Wait to recieve an EOT back from the Receiver
					while (true){
						byte[] ReceiveEOT = new byte[512];
						DatagramPacket ACKReceive = new DatagramPacket(ReceiveEOT,512);
						SocketIn.receive(ACKReceive);
						packet Response = packet.parseUDPdata(ReceiveEOT);
						if (Response.getType() == 2) break;
						else AckLog.println(Response.getSeqNum());
					}
					//Break out
					break;
				}
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(-1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//Close file output and sockets. Transmission is now complete
		SeqLog.close();
		AckLog.close();
		SocketOut.close();
		SocketIn.close();
	}
	public static void main(String[] args){
		//Parse our input and create the sender then send our file
		if (args.length != 4){
			System.out.println("Invalid number of parameters, should have 4");
			System.out.println("Please refer to README for proper parameters");
			System.exit(-1);
		}
		int OutPort = Integer.parseInt(args[1]);
		int InPort = Integer.parseInt(args[2]);
		Sender fileSender = new Sender(args[0],OutPort,InPort,args[3]);
		fileSender.SendFile();

		
	}

}
