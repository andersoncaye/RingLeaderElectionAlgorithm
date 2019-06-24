

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;

public class Servidor extends Thread {

    private final int PORT;
    private final int BUFFER_SIZE;
    private InetAddress leader = null;

    private ArrayList<InetAddress> servers = new ArrayList<>();

    public Servidor() {
        PORT = 5000;
        BUFFER_SIZE = 4096;

        try {
            servers.add(InetAddress.getByName("192.168.0.102"));
            servers.add(InetAddress.getByName("192.168.0.2"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void run() {
        validateLeader();

        try {
            DatagramSocket serverSocket = new DatagramSocket(PORT);
            byte[] bufferIn;
            byte[] bufferOut;
            while (true) {
                bufferIn = new byte[BUFFER_SIZE];

                DatagramPacket receivePacket = new DatagramPacket(bufferIn, bufferIn.length);
                serverSocket.receive(receivePacket);
                InetAddress ipCliente = receivePacket.getAddress();
                int portaCliente = receivePacket.getPort();

                String message = new String(receivePacket.getData());
                System.out.println("Received: " + message);
                String mensagem = "";
                if (message.trim().contains("elect")) {
                    leader = InetAddress.getByName(message.trim().split("_")[1]);
                } else {
                    switch (message.trim().toLowerCase()) {
                        case "leader":
                            mensagem = this.leader.getHostAddress();
                            break;
                        case "processors":
                            mensagem = "" + Runtime.getRuntime().availableProcessors();
                            break;
                    }

                    bufferOut = new byte[BUFFER_SIZE];
                    bufferOut = mensagem.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(bufferOut, bufferOut.length, ipCliente, portaCliente);
                    serverSocket.send(sendPacket);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void validateLeader() {
        boolean myself = false;

        if (leader == null) {
            getLeader();
            if (leader == null) {
                try {
                    this.leader = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress());
                    myself = true;
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        if (myself) {
            return;
        }

        try {
            InetAddress leader = InetAddress.getByName(this.leader.getHostAddress());
            if (leader.isReachable(5000)) {
                DatagramSocket clientSocket = new DatagramSocket(PORT);
                String sentence = "leader";

                byte[] bufferIn = new byte[BUFFER_SIZE];
                byte[] bufferOut = sentence.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(bufferOut, bufferOut.length, leader, PORT);
                clientSocket.send(sendPacket);
                clientSocket.setSoTimeout(10000);
                DatagramPacket receivePacket = new DatagramPacket(bufferIn, bufferIn.length);
                try {
                    clientSocket.receive(receivePacket);
                    String ip = new String(receivePacket.getData()).trim();
                    this.leader = InetAddress.getByName(ip);
                } catch (SocketTimeoutException e) {
                    System.out.println(e.getMessage());
                    electLeader();
                }
                clientSocket.close();
            } else {
                electLeader();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void getLeader() {
        for (InetAddress server : servers) {
            try {
                if (myIp(server.getHostAddress())) {
                    continue;
                }
                if (server.isReachable(5000)) {
                    DatagramSocket clientSocket = new DatagramSocket(PORT);
                    String sentence = "leader";
                    byte[] bufferIn = new byte[BUFFER_SIZE];
                    byte[] bufferOut = sentence.getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(bufferOut, bufferOut.length, server, PORT);
                    clientSocket.send(sendPacket);
                    clientSocket.setSoTimeout(10000);
                    DatagramPacket receivePacket = new DatagramPacket(bufferIn, bufferIn.length);
                    try {
                        clientSocket.receive(receivePacket);
                        this.leader = InetAddress.getByName(new String(receivePacket.getData()).trim());
                    } catch (SocketTimeoutException e) {
                        System.out.println(e.getMessage());
                    }
                    clientSocket.close();
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void electLeader() {
        InetAddress highest = null;
        int processors = 0;
        for (InetAddress server : servers) {
            try {
                if (server.isReachable(5000)) {
                    DatagramSocket clientSocket = new DatagramSocket(PORT);
                    String sentence = "processors";
                    byte[] bufferIn = new byte[BUFFER_SIZE];
                    byte[] bufferOut = sentence.getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(bufferOut, bufferOut.length, server, PORT);
                    clientSocket.send(sendPacket);
                    clientSocket.setSoTimeout(10000);
                    DatagramPacket receivePacket = new DatagramPacket(bufferIn, bufferIn.length);
                    try {
                        clientSocket.receive(receivePacket);
                        int currentProcessors = Integer.parseInt(new String(receivePacket.getData()).trim());
                        if (currentProcessors > processors) {
                            highest = server;
                            processors = currentProcessors;
                        }
                    } catch (SocketTimeoutException e) {
                        System.out.println(e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

        if (highest != null) {
            for (InetAddress server : servers) {
                try {
                    if (server.isReachable(5000)) {
                        DatagramSocket clientSocket = new DatagramSocket(PORT);
                        String sentence = "elect_" + highest.getHostAddress();

                        byte[] bufferOut = sentence.getBytes();

                        DatagramPacket sendPacket = new DatagramPacket(bufferOut, bufferOut.length, server, PORT);
                        clientSocket.send(sendPacket);
                    }
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    private boolean myIp(String ip) {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();

                    if (ip.equals(addr.getHostAddress())) {
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
