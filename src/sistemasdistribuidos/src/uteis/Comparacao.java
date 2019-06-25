package uteis;

import java.util.Comparator;
import model.Processo;

public class Comparacao implements Comparator<Processo> {

    @Override
    public int compare(Processo first, Processo second) {
        if (first.getPidId() > second.getPidId()) {
            return 1;
        }
        if (first.getPidId() < second.getPidId()) {
            return -1;
        }
        return 0;
    }
}
