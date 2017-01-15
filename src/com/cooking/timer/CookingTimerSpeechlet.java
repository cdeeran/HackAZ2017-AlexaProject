package com.cooking.timer;

import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SimpleCard;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.sqs.AmazonSQSClient;

public class CookingTimerSpeechlet implements Speechlet {

	@Override
	public void onSessionStarted(final SessionStartedRequest request, final Session session) throws SpeechletException {
		// any initialization logic goes here

	}

	@Override
	public SpeechletResponse onIntent(final IntentRequest request, final Session session) throws SpeechletException {

		Intent intent = request.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;

		if ("Cook".equals(intentName)) {
			return getCookingTimerResponse(intent.getSlot("Food"), intent.getSlot("Time"),
					session.getUser().getUserId());
		} else if("Query".equals(intentName)) {
			return queryDbForTimers();
		}
		else {
			throw new SpeechletException("Invalid Intent");
		}
	}

	@Override
	public void onSessionEnded(final SessionEndedRequest request, final Session session) throws SpeechletException {
		// any cleanup logic goes here
	}

	private SpeechletResponse getCookingTimerResponse(Slot food, Slot time, String userId) {

		final String CUSTOMER_ID = "Customer_ID";
		final String FOOD_ID = "Food_Item";
		final String DURATION_TIME_IN_MILIS = "DURATION_TIME_IN_MILIS";
		final String PROGRESS = "PROGRESS";
		final String COOKING_TIMER_TABLE = "Cooking_Timer";
		final Period timePeriod = Period.parse(time.getValue());
		final String durationAsMilis = Long.toString(timePeriod.toStandardDuration().getMillis());

		String timeConverted = PeriodFormat.wordBased().print(timePeriod);
		String response = "Okay, a timer for " + food.getValue() + "has been set for " + timeConverted;

		AmazonDynamoDBClient timerDb = new AmazonDynamoDBClient();
		DynamoDB dynamoDb = new DynamoDB(timerDb);
		Table dbTable = dynamoDb.getTable(COOKING_TIMER_TABLE);
		Item foodItem = new Item();
		foodItem.withPrimaryKey(CUSTOMER_ID, userId, FOOD_ID, food.getValue()).withString(DURATION_TIME_IN_MILIS,
				durationAsMilis).with(PROGRESS, "RUNNING");
		dbTable.putItem(foodItem);

		AmazonSQSClient sqsClient = new AmazonSQSClient();
		final String SQS_URL = "https://sqs.us-east-1.amazonaws.com/920558739301/CookingTimerQueue";
		sqsClient.sendMessage(SQS_URL, userId + "," + food.getValue());

		// Intialize a simple card
		SimpleCard card = new SimpleCard();
		card.setTitle(food.getValue());
		card.setContent(response);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(response);

		return SpeechletResponse.newTellResponse(speech);
	}

	
	private SpeechletResponse queryDbForTimers(){
		
		
		
		
		
		
		
		
		
		return null;
	}
	
	
	@Override
	public SpeechletResponse onLaunch(LaunchRequest arg0, Session arg1) throws SpeechletException {
		// TODO Auto-generated method stub
		return null;
	}

	// private SpeechletResponse getHelpResponse() {
	// // TODO Auto-generated method stub
	// return null;
	// }
}
