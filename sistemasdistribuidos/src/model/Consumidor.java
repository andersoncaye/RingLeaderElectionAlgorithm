package model;

import manager.Buffer;

public class Consumidor extends Thread {

    private final int idConsumidor;
    private final Buffer pilha;
    private final int totalConsumir;

    public Consumidor(int id, Buffer p, int totalConsumir) {
        idConsumidor = id;
        pilha = p;
        this.totalConsumir = totalConsumir;
    }

    @Override
    public void run() {
        for (int i = 0; i < totalConsumir; i++) {
            pilha.get(idConsumidor);
        }
        System.out.println("-----Consumidor #" + idConsumidor + " concluido!");
    }
}
