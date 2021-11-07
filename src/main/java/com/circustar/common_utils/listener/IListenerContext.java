package com.circustar.common_utils.listener;

import java.util.List;

public interface IListenerContext<T> {
    void init(T target);
    T getListenTarget();
    List<IListener<T>> getListenerList();
    default boolean skipAllListener(IListenerTiming eventTiming) {
        return !getListenerList().stream().filter(x -> !x.skipListener(eventTiming)).findAny().isPresent();
    }
    default void execListeners(IListenerTiming eventTiming) {
        List<IListener<T>> listenerList = getListenerList();
        if(listenerList == null || listenerList.isEmpty()) {
            return;
        }
        T target = getListenTarget();
        for(IListener<T> listener : listenerList) {
            if(!listener.skipListener(eventTiming) && listener.matchExecuteTiming(eventTiming)) {
                listener.listenerExec(target, eventTiming);
            }
        }
    }
    default void dispose() {};
}
