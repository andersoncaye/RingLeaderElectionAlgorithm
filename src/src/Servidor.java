/**
 * GRUPO: Anderson Caye, Renan Kist, Filipe Santos
 */

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

public class Servidor extends Thread {

    private final int PORT;
    private final int BUFFER_TAMANHO;
    private InetAddress lider = null;
    
    //private InetAddress MINHACONEXAO = null;

    private ArrayList<InetAddress> servidores = new ArrayList<>();

    public Servidor() {
        PORT = 5000;
        BUFFER_TAMANHO = 4096;
        
        

        try {
            //MINHACONEXAO = InetAddress.getByName("10.3.15.20");
            servidores.add(InetAddress.getByName("10.3.15.20"));
            servidores.add(InetAddress.getByName("10.3.15.21"));
            servidores.add(InetAddress.getByName("10.3.15.30"));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void run() {
        validaLider();
       // verifica();

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
                //System.out.println (msg);
                String mensagem = "";
                if (msg.trim().contains("elect")) {
                    lider = InetAddress.getByName(msg.trim().split("_")[1]);
                    System.out.println("sadsad" +lider);
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
                    System.out.println(mensagem);
                    DatagramPacket sendPacket = new DatagramPacket(bufferSaida, bufferSaida.length, ipCliente, portaCliente);
                    socketServidores.send(sendPacket);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    synchronized private void validaLider() {
        boolean myself = false;

        if (lider == null) {
            getLider();
            if (lider == null) {
                try {
                    this.lider = InetAddress.getByName(InetAddress.getLocalHost().getHostAddress());
                    //this.lider = InetAddress.getByName(MINHACONEXAO.getHostAddress());
                    myself = true;
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }

        if (myself) {
            System.out.println("Eu sou o lider");
            return;
        }

        try {
            InetAddress leader = InetAddress.getByName(this.lider.getHostAddress());
            System.out.println("Possivel lider " + leader.getHostAddress());
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
                    elegerLider();
                }
                clientSocket.close();
            } else {
                elegerLider();
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            
        }
    }

    synchronized private void getLider() {
        for (InetAddress server : servidores) {
            try {
                if (meuIp(server.getHostAddress())) {
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

    synchronized private void elegerLider() {
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

    synchronized private boolean meuIp(String ip) {
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
    
//    private static String getNetworkInterface() {
//        try {
//            String desired = "Intel(R) 82579LM Gigabit Network";
//
//            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
//            while (interfaces.hasMoreElements()) {
//                NetworkInterface iface = interfaces.nextElement();
//
//                if (iface.isLoopback() || !iface.isUp()) {
//                    continue;
//                }
//
//                if (desired.equals(iface.getDisplayName())) {
//                    return iface.getName();
//                }
//            }
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }
//
//        return null;
//    }
    
//    private InetAddress getInterface() {
//        try {
//            NetworkInterface byName = NetworkInterface.getByName(MINHACONEXAO);
//            Enumeration<InetAddress> inetAddresses = byName.getInetAddresses();
//
//            if (inetAddresses.hasMoreElements()) {
//                return inetAddresses.nextElement();
//            } else {
//                return null;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
    
    private void verifica() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                validaLider();
                verifica();
            }
        }, 1 * 1000);
    }
    
}
