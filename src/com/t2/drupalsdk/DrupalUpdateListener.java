package com.t2.drupalsdk;

public interface DrupalUpdateListener {
     void drupalReadComplete();
     void drupalCreateUpdateComplete(String msg);
     void drupalDeleteComplete(String msg);

}
