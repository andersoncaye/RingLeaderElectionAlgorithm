package manager;

import java.io.Serializable;
import java.util.Timer;
import java.util.TimerTask;
import model.*;
import uteis.Randomize;

public class Gerenciador implements Serializable {

    private static final long serialVersionUID = 7757529236684909222L;
    private final Eleicao eleicao;
    private final ListaProcessos listaProcessos;
    private final Timer timerEliminarProcesso;
    private final Timer timerConsultarCoordenador;
    private final Timer timerConsultarNovoProcesso;
    private final Timer timerDesativarCoordenador;
    private final Buffer bufferCompartilhado;

    public Gerenciador() {
        super();
        eleicao = new Eleicao();
        listaProcessos = new ListaProcessos();
        timerConsultarCoordenador = new Timer();
        timerConsultarNovoProcesso = new Timer();
        timerEliminarProcesso = new Timer();
        timerDesativarCoordenador = new Timer();
        bufferCompartilhado = new Buffer();
    }

    public void criar() {
        Processo coordenador = new Processo();
        coordenador.setPidId(Processo.getIID());
        eleicao.setCoordenador(coordenador);
        listaProcessos.add(coordenador);
    }

    public void inicializar() {
        TimerTask timerTaskConsultarCoordenador = new ConsultarCoordenador();
        timerConsultarCoordenador.scheduleAtFixedRate(
                timerTaskConsultarCoordenador, 0, 2000);
        TimerTask timerTaskNovoProcesso = new NovoProcesso();
        timerConsultarNovoProcesso.scheduleAtFixedRate(timerTaskNovoProcesso,
                0, 3000);
        TimerTask timerTaskEliminarProcesso = new EliminarProcesso();
        timerEliminarProcesso.scheduleAtFixedRate(timerTaskEliminarProcesso, 0,
                5000);
        TimerTask timerTaskDesativarCoordenador = new DesativarCoordenador();
        timerDesativarCoordenador.schedule(timerTaskDesativarCoordenador,
                10000, 10000);
    }

    public void encerrar() {
        try {
            Thread.sleep(20000);
            timerDesativarCoordenador.cancel();
            timerConsultarCoordenador.cancel();
            timerConsultarNovoProcesso.cancel();
            timerEliminarProcesso.cancel();

            for (int i = 0; i < listaProcessos.size(); i++) {
                listaProcessos.get(i).getT().stop();
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private class ConsultarCoordenador extends TimerTask {

        @Override
        public void run() {
            int position = Randomize.random(listaProcessos.size());
            Processo processo = listaProcessos.get(position);
            Processo coordenador = eleicao.getCoordenador();
            if (processo != null) {
                if (!processo.equals(coordenador)) {
                    eleicao.notificar();
                }
            }
            System.out.println(listaProcessos.toString());
            if (coordenador == null) {
                eleicao.eleger(listaProcessos);
            }
        }
    }

    private class NovoProcesso extends TimerTask {

        @Override
        public void run() {
            Processo processo = new Processo();
            processo.setPidId(Processo.getIID());

            /**
             * Start Produto / Consumidor
             */
            Thread t;
            if ((processo.getIID() % 2) == 0) {
                t = new Produtor(processo.getIID(), bufferCompartilhado, 2);
            } else {
                t = new Consumidor(processo.getIID(), bufferCompartilhado, 2);
            }

            processo.setT(t);
            listaProcessos.add(processo);

            processo.getT().start();

            System.out.println("Criando Novo Processo:" + processo);
        }
    }

    private class EliminarProcesso extends TimerTask {

        @Override
        public void run() {
            int position = Randomize.random(listaProcessos.size());
            Processo coordenador = eleicao.getCoordenador();
            Processo eliminado = listaProcessos.get(position);
            if ((eliminado != null) && (coordenador != null)) {
                if (!coordenador.equals(eliminado)) {
                    listaProcessos.remove(position);
                }
            }
        }
    }

    private class DesativarCoordenador extends TimerTask {

        @Override
        public void run() {
            Processo coordenador = eleicao.getCoordenador();
            System.out.println("Desativando Coordenador:" + coordenador);
            listaProcessos.remove(coordenador);
            eleicao.remover();
        }
    }
}
