/**
 * GRUPO: Anderson Caye, Renan Kist, Filipe Santos
 */

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;

public class Servidor extends Thread {

    private final int PORT;
    private final int BUFFER_TAMANHO;
    private InetAddress lider = null;

    private ArrayList<InetAddress> servidores = new ArrayList<>();

    public Servidor() {
        PORT = 9000;
        BUFFER_TAMANHO = 4096;

        try {
            servidores.add(InetAddress.getByName("10.3.15.20"));
            servidores.add(InetAddress.getByName("10.3.15.21"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void run() {
        validaLider();

        try {
            DatagramSocket socketServidores = new DatagramSocket(PORT);
            byte[] bufferEntrada;
            byte[] bufferSaida;
            while (true) {
                bufferEntrada = new byte[BUFFER_TAMANHO];

                DatagramPacket receivePacket = new DatagramPacket(bufferEntrada, bufferEntrada.length);
                socketServidores.receive(receivePacket);
                InetAddress ipCliente = receivePacket.getAddress();
                int portaCliente = receivePacket.getPort();

                String msg = new String(receivePacket.getData());
                System.out.println("Received: " + msg);
                String mensagem = "";
                if (msg.trim().contains("elect")) {
                    lider = InetAddress.getByName(msg.trim().split("_")[1]);
                } else {
                    switch (msg.trim().toLowerCase()) {
                        case "leader":
                            mensagem = this.lider.getHostAddress();
                            break;
                        case "processors":
                            mensagem = "" + Runtime.getRuntime().availableProcessors();
                            break;
                    }

                    bufferSaida = new byte[BUFFER_TAMANHO];
                    bufferSaida = mensagem.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(bufferSaida, bufferSaida.length, ipCliente, portaCliente);
                    socketServidores.send(sendPacket);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void validaLider() {
        boolean myself = false;

        if (lider == null) {
            getLeader();
            if (lider == null) {
                try {
                    this.lider = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress());
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
            InetAddress leader = InetAddress.getByName(this.lider.getHostAddress());
            if (leader.isReachable(PORT)) {
                DatagramSocket clientSocket = new DatagramSocket(PORT);
                String sentence = "leader";

                byte[] bufferIn = new byte[BUFFER_TAMANHO];
                byte[] bufferOut = sentence.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(bufferOut, bufferOut.length, leader, PORT);
                clientSocket.send(sendPacket);
                clientSocket.setSoTimeout(10000);
                DatagramPacket receivePacket = new DatagramPacket(bufferIn, bufferIn.length);
                try {
                    clientSocket.receive(receivePacket);
                    String ip = new String(receivePacket.getData()).trim();
                    this.lider = InetAddress.getByName(ip);
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
        for (InetAddress server : servidores) {
            try {
                if (myIp(server.getHostAddress())) {
                    continue;
                }
                if (server.isReachable(PORT)) {
                    DatagramSocket clientSocket = new DatagramSocket(PORT);
                    String sentence = "leader";
                    byte[] bufferIn = new byte[BUFFER_TAMANHO];
                    byte[] bufferOut = sentence.getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(bufferOut, bufferOut.length, server, PORT);
                    clientSocket.send(sendPacket);
                    clientSocket.setSoTimeout(10000);
                    DatagramPacket receivePacket = new DatagramPacket(bufferIn, bufferIn.length);
                    try {
                        clientSocket.receive(receivePacket);
                        this.lider = InetAddress.getByName(new String(receivePacket.getData()).trim());
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
        for (InetAddress server : servidores) {
            try {
                if (server.isReachable(PORT)) {
                    DatagramSocket clientSocket = new DatagramSocket(PORT);
                    String sentence = "processors";
                    byte[] bufferIn = new byte[BUFFER_TAMANHO];
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
            for (InetAddress server : servidores) {
                try {
                    if (server.isReachable(PORT)) {
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
