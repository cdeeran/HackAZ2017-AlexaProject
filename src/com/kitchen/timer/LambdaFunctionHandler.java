package com.kitchen.timer;

import java.util.HashSet;
import java.util.Set;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;


/**
 * This class could be the handler for an AWS Lambda function powering an Alexa Skills Kit
 * experience.
 */
public final class LambdaFunctionHandler extends SpeechletRequestStreamHandler {
    private static final Set<String> supportedApplicationIds = new HashSet<String>();
    static {
        /*
         * This Id can be found on https://developer.amazon.com/edw/home.html#/ "Edit" the relevant
         * Alexa Skill and put the relevant Application Ids in this Set.
         */
        supportedApplicationIds.add("amzn1.ask.skill.47b0bf96-70bd-4bd1-b763-c1b18c27ea38");
    }

    public LambdaFunctionHandler() {
        super(new KitchenTimerSpeechlet(), supportedApplicationIds);
    }
}

