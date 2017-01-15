package com.kitchen.timer;

import java.util.Iterator;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.QueryOutcome;
import com.amazonaws.services.dynamodbv2.document.RangeKeyCondition;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

/**
 * This class is dedicated the AmazonSQS service as the daemon for the timer
 * @author cody
 *
 */
public class AmazonSQS {
	
	private final static String COOKING_TIMER_TABLE = "Cooking_Timer";
	private final static String CUSTOMER_ID = "Customer_ID";
	private final static String FOOD_ID = "Food_Item";
	private final static String DURATION_TIME_IN_MILIS = "DURATION_TIME_IN_MILIS";
	
	// Must run in eclipse first, does not matter if pass or fail. Most likely
	// will fail because you don't the credentials, that is okay.
	public static void main(String[] args) {
		
		while(true){
			AmazonSQSClient sqsClient = new AmazonSQSClient();
			final String SQS_URL = "https://sqs.us-east-1.amazonaws.com/920558739301/CookingTimerQueue";
			ReceiveMessageResult sqsRMR = sqsClient.receiveMessage(SQS_URL);
			for (Message rmrMessage : sqsRMR.getMessages()) {
				
				// Loop over the incomming message, get the customer id and food id, then use those to query the db then call the timer.
				String message[] = rmrMessage.getBody().split(",");
				AmazonDynamoDBClient cookingTimerDb = new AmazonDynamoDBClient();
				DynamoDB dynamoDb = new DynamoDB(cookingTimerDb);
				Table dbTable = dynamoDb.getTable(COOKING_TIMER_TABLE);
				ItemCollection<QueryOutcome> itemResults = dbTable.query(CUSTOMER_ID, message[0], new RangeKeyCondition(FOOD_ID).eq(message[1]));
				Iterator<Item> itemResultsIterator = itemResults.iterator();
				Item queryResult = null;
				while(itemResultsIterator.hasNext()){
					queryResult = itemResultsIterator.next();	
				}
				KitchenTimerScheduler kitchenScheduler = new KitchenTimerScheduler(queryResult.getString(FOOD_ID), queryResult.getString(DURATION_TIME_IN_MILIS), CUSTOMER_ID, message[0], FOOD_ID, message[1]);
				sqsClient.deleteMessage(SQS_URL, rmrMessage.getReceiptHandle());
			}
		}
	}
}