
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

/* @author anderson.caye */
public class Main extends Thread{
    
    private final int PORTA;
    private final int TAM_BUFFER;
    private final String NETWORKINTERFACE;
    
    private InetAddress lider = null;
    private DatagramSocket CLIENTSOCKET;

    private ArrayList<InetAddress> servers = new ArrayList<>();

    
    
    public Main() {
        PORTA = 5000;
        TAM_BUFFER = 4096;
        NETWORKINTERFACE = getNetworkInterface();

        try {            
            CLIENTSOCKET = new DatagramSocket(PORTA);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        validaLider();
        agendar();

        try {
            byte[] bufferEntrada;
            byte[] bufferSaida;
            
            bufferEntrada = new byte[TAM_BUFFER];
            DatagramPacket receivePacket = new DatagramPacket(bufferEntrada, bufferEntrada.length);
            InetAddress ipCliente = receivePacket.getAddress();
            servers.add(InetAddress.getByName(ipCliente.getHostAddress()));

            while (true) {
                

                CLIENTSOCKET.setSoTimeout(0);
                CLIENTSOCKET.receive(receivePacket);

                ipCliente = receivePacket.getAddress();
                int portaCliente = receivePacket.getPort();

                String recebido = new String(receivePacket.getData()).trim();
                String mensagem = "";
                
                System.out.println(new String(receivePacket.getData()).trim()+" recebido do cliente");
                
                if (recebido.contains("elect")) {
                    lider = InetAddress.getByName(recebido.split("_")[1]);
                } else {
                    switch (recebido.toLowerCase()) {
                        case "leader":
                            
                            System.out.println(this.lider.getHostAddress()+ " o que é eth");
                            mensagem = this.lider.getHostAddress().replace("%eth0", "");
                            break;
                        case "processors":
                            mensagem = "" + Runtime.getRuntime().availableProcessors();
                            break;
                    }

                    bufferSaida = new byte[TAM_BUFFER];
                    bufferSaida = mensagem.getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(bufferSaida, bufferSaida.length, ipCliente, portaCliente);
                    
                    System.out.println(new String(sendPacket.getData()).trim()+ " ip enviado 1");
                    
                    CLIENTSOCKET.send(sendPacket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CLIENTSOCKET.close();
        }
    }

    private void validaLider() {
        System.out.println("Validando o lider...");
        boolean euAqui = false;

        if (lider == null) {
            getLeader();

            if (lider == null) {
                try {
                    this.lider = getInterface();
                    euAqui = true;
                } catch (Exception e) {
                    System.out.println("Algo saiu erado ao validar o lider...");
                    e.printStackTrace();
                }
            }
        }

        if (euAqui || isMyIp(this.lider.getHostAddress())) {
            System.out.println("Eu sou o líder.");
            return;
        }

        try {
            InetAddress liderr = InetAddress.getByName(this.lider.getHostAddress());

            if (liderr.isReachable(2000)) {
                String sentence = "leader";

                byte[] bufferEntrada = new byte[TAM_BUFFER];
                byte[] bufferSaida = sentence.getBytes();

                DatagramPacket sendPacket = new DatagramPacket(bufferSaida, bufferSaida.length, liderr, PORTA);
                
                System.out.println(new String(sendPacket.getData()).trim()+ " enviou isso");
                
                CLIENTSOCKET.send(sendPacket);
                
                System.out.println("receber 1");

                DatagramPacket receivePacket = new DatagramPacket(bufferEntrada, bufferEntrada.length);
                
                 System.out.println(new String(receivePacket.getData()).trim()+ " recebeu isso");
                
                //CLIENTSOCKET.setSoTimeout(5000);
                
                while (this.lider != null) {
                    try {
                        CLIENTSOCKET.receive(receivePacket);
                        System.out.println(new String(receivePacket.getData()).trim()+ " ip recebido 1");
                        String ip = new String(receivePacket.getData()).trim();
                        //System.out.println(ip+" ip recebido 2");
                        //this.leader = InetAddress.getByName(ip);
                        
                        System.out.println("receber 2);
                        
                        break;
                    } catch (SocketTimeoutException e) {
                        System.out.println("Socket timeout.");
                        
                        break;
                    }
                }
                
                if (this.lider == null) {
                    System.out.println("Leader É NULO OOOO");
                    electLeader();
                }
            } else {
                System.out.println("Leader trying: " + liderr.getHostAddress());
                electLeader();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("O líder é: " + this.lider.getHostAddress());
    }

    private void getLeader() {
        for (InetAddress server : servers) {
            try {
                if (isMyIp(server.getHostAddress())) {
                    continue;
                }

                if (server.isReachable(2000)) {
                    String sentence = "leader";

                    byte[] bufferEntrada = new byte[TAM_BUFFER];
                    byte[] bufferSaida = sentence.getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(bufferSaida, bufferSaida.length, server, PORTA);
                    
                    System.out.println(new String(sendPacket.getData()).trim()+ " enviando para o líder...");
                    
                    CLIENTSOCKET.send(sendPacket);

                    //CLIENTSOCKET.setSoTimeout(10000);
                    DatagramPacket receivePacket = new DatagramPacket(bufferEntrada, bufferEntrada.length);

                    try {
                        
                        System.out.println(new String(receivePacket.getData()).trim()+ " recebendo do líder...");
                        CLIENTSOCKET.receive(receivePacket);
                        this.lider = InetAddress.getByName(new String(receivePacket.getData()).trim());
                    } catch (SocketTimeoutException e) {
                        System.out.println(e.getMessage());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void electLeader() {
        System.out.println("Escolhendo um novo líder");

        InetAddress highest = null;
        int processors = 0;

        for (InetAddress server : servers) {
            try {
                if (server.isReachable(5000)) {

                    String sentence = "processors";

                    byte[] bufferEntrada = new byte[TAM_BUFFER];
                    byte[] bufferSaida = sentence.getBytes();

                    DatagramPacket sendPacket = new DatagramPacket(bufferSaida, bufferSaida.length, server, PORTA);
                    
                    System.out.println(new String(sendPacket.getData()).trim()+ " enviando processos");
                    
                    CLIENTSOCKET.send(sendPacket);
                    
                    CLIENTSOCKET.setSoTimeout(10000);
                    DatagramPacket receivePacket = new DatagramPacket(bufferEntrada, bufferEntrada.length);

                    try {
                        CLIENTSOCKET.receive(receivePacket);

                        int currentProcessors = Integer.parseInt(new String(receivePacket.getData()).trim());

                        if (currentProcessors > processors) {
                            highest = server;
                            processors = currentProcessors;
                        }
                    } catch (SocketTimeoutException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }

        if (highest != null) {
            for (InetAddress server : servers) {
                try {
                    if (server.isReachable(5000)) {
                        String sentence = "elect_" + highest.getHostAddress();

                        byte[] bufferSaida = sentence.getBytes();

                        DatagramPacket sendPacket = new DatagramPacket(bufferSaida, bufferSaida.length, server, PORTA);
                        
                        System.out.println(new String(sendPacket.getData()).trim()+ " enviando elec");
                                                
                        CLIENTSOCKET.send(sendPacket);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private InetAddress getInterface() {
        try {
            NetworkInterface byName = NetworkInterface.getByName(NETWORKINTERFACE);
            Enumeration<InetAddress> inetAddresses = byName.getInetAddresses();

            if (inetAddresses.hasMoreElements()) {
                return inetAddresses.nextElement();
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isMyIp(String ip) {

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
            e.printStackTrace();
        }

        return false;
    }

    private static String getNetworkInterface() {
        try {
            String desired = "Realtek PCIe GBE Family Controller";

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (iface.isLoopback() || !iface.isUp()) {
                    continue;
                }

                if (desired.equals(iface.getDisplayName())) {
                    return iface.getName();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void agendar() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                validaLider();
                agendar();
            }
        }, 1 * 1000);
    }
}
