package model;

import manager.Buffer;

public class Produtor extends Thread {

    private final int idProdutor;
    private final Buffer pilha;
    private final int producaoTotal;

    public Produtor(int id, Buffer p, int producaoTotal) {
        idProdutor = id;
        pilha = p;
        this.producaoTotal = producaoTotal;
    }

    public void run() {
        for (int i = 0; i < producaoTotal; i++) {
            pilha.set(idProdutor, i);
        }
        System.out.println("-----Produtor #" + idProdutor + " concluido!");
    }
}
