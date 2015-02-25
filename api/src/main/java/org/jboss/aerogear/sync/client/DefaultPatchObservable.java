package org.jboss.aerogear.sync.client;

import org.jboss.aerogear.sync.ClientDocument;

import java.util.Observable;
import java.util.Observer;

public class DefaultPatchObservable<T> extends Observable implements PatchObservable<T> {

    @Override
    public void addPatchListener(PatchListener<T> listener) {
        addObserver(new PatchListenerAdapter<T>(listener));
    }

    @Override
    public void removePatchListener(PatchListener<T> listener) {
        deleteObserver(new PatchListenerAdapter<T>(listener));
    }

    @Override
    public void removePatchListeners() {
        deleteObservers();
    }

    @Override
    public void notifyPatched(ClientDocument<T> patchedDocument) {
        notifyObservers(patchedDocument);
    }

    @Override
    public int countPatchListeners() {
        return countObservers();
    }

    @Override
    public void changed() {
        setChanged();
    }

    private static class PatchListenerAdapter<T> implements Observer {

        private final PatchListener<T> listener;

        PatchListenerAdapter(final PatchListener<T> listener) {
            this.listener = listener;
        }

        @Override
        @SuppressWarnings("rawtypes")
        public void update(final Observable ignored, final Object arg) {
            if (arg instanceof ClientDocument) {
                listener.patched((ClientDocument) arg);
            }
        }
    }
}
