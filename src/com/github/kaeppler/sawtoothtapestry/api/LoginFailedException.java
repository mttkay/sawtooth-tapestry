package com.github.kaeppler.sawtoothtapestry.api;

import com.github.ignition.core.exceptions.ResourceMessageException;
import com.github.kaeppler.sawtoothtapestry.R;

@SuppressWarnings("serial")
public class LoginFailedException extends Exception implements ResourceMessageException {

    @Override
    public int getClientMessageResourceId() {
        return R.string.error_login_failed;
    }

}
