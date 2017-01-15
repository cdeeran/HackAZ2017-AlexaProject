package com.kitchen.timer;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

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
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.sqs.AmazonSQSClient;

public class KitchenTimerSpeechlet implements Speechlet {

	@Override
	public void onSessionStarted(final SessionStartedRequest request, final Session session) throws SpeechletException {
		// any initialization logic goes here

	}
	
	/**
	 * This is part of the Speechlet Interface. Where Alex will determine what intent to run based on her natural lang. processing.
	 */
	@Override
	public SpeechletResponse onIntent(final IntentRequest request, final Session session) throws SpeechletException {

		Intent intent = request.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;

		if ("Cook".equals(intentName)) {
			return createTimer(intent.getSlot("Food"), intent.getSlot("Time"),
					session.getUser().getUserId());
		} else if ("Query".equals(intentName)) {
			return queryDbForTimers(session.getUser().getUserId());
		} else if ("Delete".equals(intentName)) {
			return deleteTimer(session.getUser().getUserId(), intent.getSlot("Food"));
		} else {
			throw new SpeechletException("Invalid Intent");
		}
	}

	@Override
	public void onSessionEnded(final SessionEndedRequest request, final Session session) throws SpeechletException {
		// any cleanup logic goes here
	}

	/**
	 * This method will create a new timer and add it to the Dynamo DB. Then send a message the queque that will start the timer process in the background.
	 * @param food - the type of timer
	 * @param time - how long with the timer run
	 * @param userId - the session id with the echo
	 * @return - a confirmation if the timer has been created
	 */
	private SpeechletResponse createTimer(Slot food, Slot time, String userId) {

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
		foodItem.withPrimaryKey(CUSTOMER_ID, userId, FOOD_ID, food.getValue())
				.withString(DURATION_TIME_IN_MILIS, durationAsMilis)
				.withString("DURATION_IN_YRS:HRS:MINS:SECS", timeConverted).with("START TIME", System.nanoTime())
				.withLong("PROJECTED ENDTIME", System.currentTimeMillis() + timePeriod.toStandardDuration().getMillis())
				.withString(PROGRESS, "RUNNING");
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

	/**
	 * This intent will query for timers that are currently "in progress"
	 * @param userId - the session id for the user
	 * @return the list of running timers or say there are none
	 */
	private SpeechletResponse queryDbForTimers(String userId) {

		final String CUSTOMER_ID = "Customer_ID";
		final String FOOD_ID = "Food_Item";
		final String PROGRESS = "PROGRESS";
		final String COOKING_TIMER_TABLE = "Cooking_Timer";

		AmazonDynamoDBClient timerDb = new AmazonDynamoDBClient();
		DynamoDB dynamoDb = new DynamoDB(timerDb);
		Table dbTable = dynamoDb.getTable(COOKING_TIMER_TABLE);
		ItemCollection<QueryOutcome> itemResults = dbTable.query(CUSTOMER_ID, userId);
		Iterator<Item> itemResultsIterator = itemResults.iterator();
		Item queryResult = null;

		if (itemResultsIterator.hasNext() == false) {

			String noTimersRunning = "Sorry, there appear to be no timers running.";

			// Create the Simple card content.
			SimpleCard card = new SimpleCard();
			card.setTitle("No Timers Running");
			card.setContent(noTimersRunning);

			// Create the plain text output.
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
			speech.setText(noTimersRunning);

			return SpeechletResponse.newTellResponse(speech);

		} else {
			String timerInProgress = "The following timers are running. ";
			while (itemResultsIterator.hasNext()) {
				queryResult = itemResultsIterator.next();
				long timeLeft = TimeUnit.MILLISECONDS
						.toSeconds((queryResult.getLong("PROJECTED ENDTIME") - System.currentTimeMillis()));
				Period ptl = new Period(timeLeft);
				ptl = ptl.normalizedStandard();
				String tl = PeriodFormat.wordBased().print(ptl);
				if ("RUNNING".equals(queryResult.getString(PROGRESS)) && timeLeft > 0) {
					timerInProgress += queryResult.getString(FOOD_ID) + "with about " + tl + " left. ";
				}
			}

			// Create the Simple card content.
			SimpleCard card = new SimpleCard();
			card.setTitle("Total Running Timers");
			card.setContent(timerInProgress);

			// Create the plain text output.
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
			speech.setText(timerInProgress);

			return SpeechletResponse.newTellResponse(speech, card);
		}
	}

	/**
	 * This will method is still in beta, it is designed to delete a timer.
	 * @param userId - for the echo session
	 * @param foodItem - the type of timer to delete
	 * @return a confirmation if the timer has been deleted
	 */
	private SpeechletResponse deleteTimer(String userId, Slot foodItem) {

		final String CUSTOMER_ID = "Customer_ID";
		final String FOOD_ID = "Food_Item";
		final String COOKING_TIMER_TABLE = "Cooking_Timer";

		// Search the Dynamo DB
		AmazonDynamoDBClient timerDb = new AmazonDynamoDBClient();
		DynamoDB dynamoDb = new DynamoDB(timerDb);
		Table dbTable = dynamoDb.getTable(COOKING_TIMER_TABLE);
		DeleteItemOutcome deleteResults = dbTable.deleteItem(CUSTOMER_ID, userId, FOOD_ID, foodItem);

		if (deleteResults.getDeleteItemResult() != null) {
			String timerCouldBeDeleted = foodItem + " timer was successfully deleted.";

			// Create the Simple card content.
			SimpleCard card = new SimpleCard();
			card.setTitle("Timer deleted");
			card.setContent(timerCouldBeDeleted);

			// Create the plain text output.
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
			speech.setText(timerCouldBeDeleted);

			return SpeechletResponse.newTellResponse(speech);

		} else {

			String timerCouldBeDeleted = "Sorry, there was no timer to be deleted";

			// Create the Simple card content.
			SimpleCard card = new SimpleCard();
			card.setTitle("No Timers Running");
			card.setContent(timerCouldBeDeleted);

			// Create the plain text output.
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
			speech.setText(timerCouldBeDeleted);

			return SpeechletResponse.newTellResponse(speech);

		}
	}

	@Override
	public SpeechletResponse onLaunch(LaunchRequest arg0, Session arg1) throws SpeechletException {
		// TODO Auto-generated method stub
		return null;
	}
}