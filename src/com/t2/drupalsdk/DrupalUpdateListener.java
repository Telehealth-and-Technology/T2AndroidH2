package com.t2.drupalsdk;

public interface DrupalUpdateListener {
    void drupalFailure(String msg);
    void drupalReadComplete();
     void drupalCreateUpdateComplete(String msg);
     void drupalDeleteComplete(String msg);

}
