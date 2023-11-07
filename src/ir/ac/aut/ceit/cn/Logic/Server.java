package ir.ac.aut.ceit.cn.Logic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import ir.ac.aut.ceit.cn.Message.FileMessage;
import ir.ac.aut.ceit.cn.Message.IHaveMessage;
import ir.ac.aut.ceit.cn.Model.FileUtils;

public class Server extends NetworkPeer implements Runnable {
	public final static int PORT = 1234;
    private String filePath;
    private String fileName;
    private long fileSize;
    private DatagramSocket datagramSocket;
    private InetAddress clientPeerIP;
    private int clientPeerPort;
//    private boolean mustRespond = true;

    public byte[] dataToSend;
    
    public Server(String fileName,String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
        updateDataBytes();


        try {
        
				try {
					clientPeerIP = InetAddress.getByName("192.168.102.5");
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
           datagramSocket = new DatagramSocket(PORT);
        } catch (SocketException e) {
            printLog("Can't start a new server. => " + e.getMessage().toString());

        }
    }
    
    private void updateDataBytes() {
        dataToSend = FileUtils.readFile(filePath);
        this.fileSize = dataToSend.length;
        System.out.println(fileSize);
    }
    
    public void run() {
            updateDataBytes();
            try {
				sendIHaveMessage(datagramSocket, 12345, clientPeerIP);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            int numberOfChunks = FileMessage.getChunkSize(fileSize);
            
            for(int chunkIndex = 0; chunkIndex < numberOfChunks; chunkIndex++) {
                int start = chunkIndex * FileMessage.MAX_PACKET_SIZE;
                byte[] chunk = new byte[Math.min((int)(fileSize-start),FileMessage.MAX_PACKET_SIZE)];
                for (int i = 0; i < chunk.length; i++) {
                    chunk[i] = dataToSend[start + i];
                }
                printLog("I sent a file chunk [" + String.valueOf(start) + ":" + String.valueOf(chunk.length - 1 + start) + "]");
                uploadFile(chunkIndex,chunk);
                try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            printLog("File sent completely. I'm going to listen for future requests.");
      

    }
    private void uploadFile(int offset,byte[] chunk) {
        byte[] data;
        try {
            data = getFileMessageBytes(offset,chunk);
            DatagramPacket filePacket = new DatagramPacket(data, data.length, clientPeerIP, 12345);
            //System.out.println("size---->" + String.valueOf(filePacket.getData().length));
            datagramSocket.send(filePacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private byte[] getFileMessageBytes(int offset, byte[] chunk) throws IOException {
        byte[] data;
        FileMessage fileMessage = new FileMessage(offset, chunk);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(byteArrayOutputStream);
        oo.writeObject(fileMessage);
        data = byteArrayOutputStream.toByteArray();
        oo.close();
        return data;
    }
    
    private void sendIHaveMessage(DatagramSocket datagramSocket, int port, InetAddress inetAddress) throws IOException {
//      DatagramSocket iHaveSocket = new DatagramSocket(port, inetAddress);
      byte[] data = getIHaveBytes();
      DatagramPacket iHavePacket = new DatagramPacket(data, data.length, inetAddress, port);
      datagramSocket.send(iHavePacket);
  }
    
    private byte[] getIHaveBytes() throws IOException {
        byte[] data;
        IHaveMessage iHaveMessage = new IHaveMessage(fileName, fileSize);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(byteArrayOutputStream);
        oo.writeObject(iHaveMessage);
        data = byteArrayOutputStream.toByteArray();
        oo.close();
        return data;
    }

	public static void main(String[] args) {
		System.out.println("I am serving");
		Thread server = new Thread(new Server("21IT164.pdf", "F:\\Java\\Java-UDP-P2P\\src\\ir\\ac\\aut\\ceit\\cn\\21IT164.pdf"));
        server.start();
        
	}

	@Override
	public void printLog(String text) {
		System.out.println("[Server:] " + text);
		
	}
}
