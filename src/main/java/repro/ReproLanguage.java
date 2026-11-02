package repro;

import com.oracle.truffle.api.TruffleLanguage;

@TruffleLanguage.Registration(id = "repro", name = "Repro")
public final class ReproLanguage extends TruffleLanguage<Object> {
    @Override
    protected Object createContext(Env env) {
        return new Object();
    }
}
