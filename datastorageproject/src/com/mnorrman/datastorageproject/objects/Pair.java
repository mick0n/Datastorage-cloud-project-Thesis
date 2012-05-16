package com.mnorrman.datastorageproject.objects;

/**
 *
 * @author Mikael Norrman
 */
public class Pair<S, T> {
    
    public S key;
    public T value;
    
    public Pair(S key, T value){
        this.key = key;
        this.value = value;
    }

    public S getKey() {
        return key;
    }

    public void setKey(S key) {
        this.key = key;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }    
}
