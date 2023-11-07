package ir.ac.aut.ceit.cn.Logic;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;

import ir.ac.aut.ceit.cn.Message.FileMessage;
import ir.ac.aut.ceit.cn.Message.IHaveMessage;
import ir.ac.aut.ceit.cn.Message.Message;
import ir.ac.aut.ceit.cn.Message.MessageTypes;
import ir.ac.aut.ceit.cn.Model.FileUtils;

public class Client extends NetworkPeer implements Runnable {
	
	private long fileSize;
	private String fileName;
	private DatagramSocket datagramSocket;
	
	public Client() {
        
        try {
            datagramSocket = new DatagramSocket(12345);
            datagramSocket.setSendBufferSize(100000);
            datagramSocket.setReceiveBufferSize(100000);
            datagramSocket.setBroadcast(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
	@Override
	public void run() {
		boolean requestDownload = false;
		requestDownload = waitForAnswer();
		if(requestDownload == true) {          
            downloadFile();
        }
		
	}
	
	private void downloadFile() {
        int numberOfChunks = FileMessage.getChunkSize(fileSize);
        
        boolean[] chunksReceived = new boolean[numberOfChunks];
        System.out.println(chunksReceived.length);
        for (int i = 0; i < chunksReceived.length; i++) {
            chunksReceived[i] = false;
            System.out.println(chunksReceived[i]);
        }
        byte[][] chunks = new byte[numberOfChunks][];
        while (downloadIsNotComplete(chunksReceived)) {
        	System.out.println(downloadIsNotComplete(chunksReceived));
            byte[] data = new byte[FileMessage.MAX_PACKET_SIZE + 2000];
            DatagramPacket datagramPacket = new DatagramPacket(data, data.length);
            try {
                datagramSocket.setSoTimeout(0);
                datagramSocket.receive(datagramPacket);
                System.out.println("1");
                ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(datagramPacket.getData()));
                Message message = (Message) objectInputStream.readObject();
                objectInputStream.close();
                System.out.println(isFileMessage(message));
                if(isFileMessage(message)) {
                	
                    FileMessage fileMessage = (FileMessage)message;
                    chunks[fileMessage.getOffset()] = fileMessage.getData();
                    chunksReceived[fileMessage.getOffset()] = true;
                    printLog("I got a chunk! ->" + String.valueOf(fileMessage.getOffset()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        System.out.println("i am out");
        int totalSize = 0;
        for (byte[] chunk : chunks) {
            totalSize += chunk.length;
        }
        ByteBuffer byteBuffer = ByteBuffer.allocate(totalSize);
        for (byte[] chunk : chunks) {
            byteBuffer.put(chunk);
        }
        FileUtils.writeFile(byteBuffer.array(),System.getProperty("user.dir").toString() + "/receiver/" + fileName);
        printLog("Complete");
    }

    private boolean isFileMessage(Message message) {
        return message.getType() == MessageTypes.FILE_MESSAGE;
    }
    
    private boolean downloadIsNotComplete(boolean[] chunksReceived) {
        for (boolean b : chunksReceived) {
            if(b == false) {
                return true;
            }
        }
        return false;
    }
    
    private boolean waitForAnswer() {
        try {
            while (true) {
                byte[] answer = new byte[10000];
                DatagramPacket responsePacket = new DatagramPacket(answer, answer.length);
                datagramSocket.setSoTimeout(20000);
                datagramSocket.receive(responsePacket);
                ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(responsePacket.getData()));
                Message message = (Message) objectInputStream.readObject();
                objectInputStream.close();
                if (isIHaveMessage(message)) {
                    IHaveMessage iHaveMessage = (IHaveMessage) message;
                    printLog("received " + iHaveMessage.toString());
                    fileSize = iHaveMessage.getFileSize();
                    fileName = iHaveMessage.getFileName();
                    break;
                }
            }
            datagramSocket.setBroadcast(false);
            return true;

        } catch (SocketTimeoutException e) {
            printLog("time out :( it's seems no one has this file.");
            return false;
        }
        catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private boolean isIHaveMessage(Message message) {
        return message.getType() == 1;
    }
    
	@Override
	public void printLog(String text) {
		System.out.println("[Client:] " + text);
		
	}
	
	public static void main(String[] args) {
		System.out.println("wait for ..");
		Thread client = new Thread(new Client());
	    client.start();
	}
}
